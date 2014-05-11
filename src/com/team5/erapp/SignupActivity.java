package com.team5.erapp;

import java.io.IOException;
import java.security.NoSuchAlgorithmException;
import java.security.NoSuchProviderException;
import java.security.spec.InvalidKeySpecException;
import java.util.List;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.google.cloud.backend.core.CloudBackendFragment;
import com.google.cloud.backend.core.CloudCallbackHandler;
import com.google.cloud.backend.core.CloudEntity;
import com.google.cloud.backend.core.CloudQuery;
import com.google.cloud.backend.core.Filter;
import com.google.cloud.backend.core.CloudBackendFragment.OnListener;
import com.google.cloud.backend.core.CloudQuery.Scope;
import com.team5.erapp.R;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.FragmentManager;
import android.app.FragmentTransaction;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.ActivityInfo;
import android.os.Bundle;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.inputmethod.InputMethodManager;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.Toast;
import android.widget.CompoundButton.OnCheckedChangeListener;
import android.widget.EditText;

public class SignupActivity extends Activity implements OnListener {

	private EditText name;
	private EditText email;
	private EditText pass;
	private EditText verify;
	private CheckBox checkCorp;
	private EditText company;
	private Button create;
	private boolean checked;
	private boolean approved;
	private boolean coExists;
	private ProgressDialog progress;

	private SharedPreferences settings;

	private CloudBackendFragment mProcessingFragment;
	private FragmentManager mFragmentManager;

	private static final String PREFS_NAME = "MyPrefsFile";
	private static final String PROCESSING_FRAGMENT_TAG = "BACKEND_FRAGMENT";

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		this.setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);
		setContentView(R.layout.activity_signup);
		PRNGFixes.apply();

		mFragmentManager = getFragmentManager();
		settings = getSharedPreferences(PREFS_NAME, 0);

		name = (EditText) findViewById(R.id.nameSignup);
		email = (EditText) findViewById(R.id.emailSignup);
		pass = (EditText) findViewById(R.id.passwordSignup);
		verify = (EditText) findViewById(R.id.verifyPassSignup);
		company = (EditText) findViewById(R.id.companySignup);
		checkCorp = (CheckBox) findViewById(R.id.checkBox1);
		checkCorp.setOnCheckedChangeListener(new OnCheckedChangeListener() {

			@Override
			public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
				if (isChecked) {
					checked = true;
					company.setVisibility(View.VISIBLE);
					company.requestFocus();
				} else {
					checked = false;
					company.setVisibility(View.GONE);
				}
			}
		});
		create = (Button) findViewById(R.id.buttonSignup);
		create.setOnClickListener(new OnClickListener() {

			@Override
			public void onClick(View v) {
				InputMethodManager inputManager = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
				inputManager.hideSoftInputFromWindow(getCurrentFocus().getWindowToken(), InputMethodManager.HIDE_NOT_ALWAYS);
				if (isEmpty(name) || isEmpty(email) || isEmpty(pass) || isEmpty(verify)) {
					Toast.makeText(SignupActivity.this, "One or more fields are empty.", Toast.LENGTH_SHORT).show();
					return;
				} else if (!isEmailValid(email.getText().toString())) {
					Toast.makeText(SignupActivity.this, "Invalid email.", Toast.LENGTH_SHORT).show();
					return;
				} else if (!pass.getText().toString().equals(verify.getText().toString())) {
					Toast.makeText(SignupActivity.this, "Passwords do not match.", Toast.LENGTH_SHORT).show();
					return;
				} else if (pass.getText().toString().length() < 6) {
					Toast.makeText(SignupActivity.this, "Password must be at least 6 characters.", Toast.LENGTH_SHORT).show();
					return;
				}
				progress = new ProgressDialog(SignupActivity.this);
				progress.setMessage("Creating account...");
				progress.show();
				checkEmail(email.getText().toString().toLowerCase(Locale.getDefault()));
			}
		});
		initiateFragments();
	}

	private void createAcc() throws NoSuchAlgorithmException, NoSuchProviderException, InvalidKeySpecException {
		CloudEntity account = new CloudEntity("ERAppAccounts");

		String password = PassHash.generateStrongPasswordHash(pass.getText().toString());

		SharedPreferences.Editor editor = settings.edit();
		String email = this.email.getText().toString().toLowerCase(Locale.getDefault());
		editor.putString("email", email);
		account.put("email", email);
		account.put("name", name.getText().toString());
		account.put("pass", password);

		int at = email.indexOf("@");
		int dot = email.indexOf(".");
		String eFormat = email.substring(0, at) + "_" + email.substring(at + 1, dot) + "_" + email.substring(dot + 1);
		editor.putString("emailFormatted", eFormat);

		if (checked && (company.getText().toString().trim().length() != 0)) {
			account.put("company", company.getText().toString().toLowerCase(Locale.getDefault()));
			if (coExists) {
				editor.putBoolean("employee", true);
				account.put("admin", false);
				account.put("approved", false);
				approved = false;
			} else {
				account.put("admin", true);
				account.put("approved", true);
				approved = true;
				editor.putString("company", company.getText().toString());
				editor.putString("name", name.getText().toString());
				editor.putBoolean("employee", true);
				editor.putBoolean("admin", true);
				editor.putBoolean("approved", true);
			}
		}
		editor.commit();

		mProcessingFragment.getCloudBackend().insert(account, new CloudCallbackHandler<CloudEntity>() {
			@Override
			public void onComplete(final CloudEntity result) {
			}

			@Override
			public void onError(final IOException exception) {
				Toast.makeText(SignupActivity.this, "Unable to connect to server.", Toast.LENGTH_SHORT).show();
			}
		});
		goHome();
	}

	private void checkEmail(String email) {
		CloudQuery cq = new CloudQuery("ERAppAccounts");
		cq.setFilter(Filter.eq("email", email));
		cq.setScope(Scope.PAST);
		mProcessingFragment.getCloudBackend().list(cq, new CloudCallbackHandler<List<CloudEntity>>() {
			@Override
			public void onComplete(List<CloudEntity> results) {
				if (!results.isEmpty()) {
					InputMethodManager inputManager = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
					inputManager.hideSoftInputFromWindow(getCurrentFocus().getWindowToken(), InputMethodManager.HIDE_NOT_ALWAYS);
					progress.dismiss();
					Toast.makeText(SignupActivity.this, "Email already in use.", Toast.LENGTH_SHORT).show();
				} else if (checked && (company.getText().toString().trim().length() != 0)) {
					checkCompany(company.getText().toString().toLowerCase(Locale.getDefault()));
				} else {
					try {
						createAcc();
					} catch (NoSuchAlgorithmException e) {
						e.printStackTrace();
					} catch (NoSuchProviderException e) {
						e.printStackTrace();
					} catch (InvalidKeySpecException e) {
						e.printStackTrace();
					}
				}
			}

			@Override
			public void onError(IOException exception) {
			}
		});
	}

	private void checkCompany(String company) {
		CloudQuery cq = new CloudQuery("ERAppAccounts");
		cq.setFilter(Filter.eq("company", company));
		cq.setScope(Scope.PAST);
		mProcessingFragment.getCloudBackend().list(cq, new CloudCallbackHandler<List<CloudEntity>>() {
			@Override
			public void onComplete(List<CloudEntity> results) {
				if (!results.isEmpty()) {
					coExists = true;
				}
				try {
					createAcc();
				} catch (NoSuchAlgorithmException e) {
					e.printStackTrace();
				} catch (NoSuchProviderException e) {
					e.printStackTrace();
				} catch (InvalidKeySpecException e) {
					e.printStackTrace();
				}
			}

			@Override
			public void onError(IOException exception) {
			}
		});
	}

	/**
	 * method is used for checking valid email id format.
	 * 
	 * @param email
	 * @return boolean true for valid false for invalid
	 */
	public static boolean isEmailValid(String email) {
		boolean isValid = false;

		String expression = "^[\\w\\.-]+@([\\w\\-]+\\.)+[A-Z]{2,4}$";
		CharSequence inputStr = email;

		Pattern pattern = Pattern.compile(expression, Pattern.CASE_INSENSITIVE);
		Matcher matcher = pattern.matcher(inputStr);
		if (matcher.matches()) {
			isValid = true;
		}
		return isValid;
	}

	private boolean isEmpty(EditText etText) {
		return etText.getText().toString().trim().length() == 0;
	}

	private void initiateFragments() {
		FragmentTransaction fragmentTransaction = mFragmentManager.beginTransaction();
		mProcessingFragment = (CloudBackendFragment) mFragmentManager.findFragmentByTag(PROCESSING_FRAGMENT_TAG);
		if (mProcessingFragment == null) {
			mProcessingFragment = new CloudBackendFragment();
			mProcessingFragment.setRetainInstance(true);
			fragmentTransaction.add(mProcessingFragment, PROCESSING_FRAGMENT_TAG);
		}
		fragmentTransaction.commit();
	}

	public void goHome() {
		Intent i = new Intent(getApplicationContext(), HomeActivity.class);
		if (settings.getBoolean("employee", false) && !approved) {
			i = new Intent(this, LoginActivity.class);
			new AlertDialog.Builder(this).setMessage("Account created. Please wait for approval from company admin.")
					.setPositiveButton(android.R.string.yes, new DialogInterface.OnClickListener() {
						@Override
						public void onClick(DialogInterface arg0, int arg1) {
							progress.dismiss();
							finish();
						}
					}).create().show();
			return;
		} else {
			SharedPreferences.Editor editor = settings.edit();
			editor.putBoolean("logged", true);
			editor.commit();
		}
		progress.dismiss();
		i.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK | Intent.FLAG_ACTIVITY_NEW_TASK);
		startActivity(i);
	}

	/**
	 * Method called via OnListener in {@link CloudBackendFragment}.
	 */
	@Override
	public void onCreateFinished() {
	}

	@Override
	public void onBroadcastMessageReceived(List<CloudEntity> message) {
	}
}