package com.team5.erapp;

import java.io.IOException;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.google.cloud.backend.core.CloudBackendFragment;
import com.google.cloud.backend.core.CloudCallbackHandler;
import com.google.cloud.backend.core.CloudEntity;
import com.google.cloud.backend.core.CloudBackendFragment.OnListener;
import com.google.cloud.backend.core.CloudQuery.Order;
import com.google.cloud.backend.core.CloudQuery.Scope;
import com.team5.erapp.R;

import android.app.Activity;
import android.app.FragmentManager;
import android.app.FragmentTransaction;
import android.content.Context;
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
	private List<CloudEntity> accountsList;

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
				if (isEmpty(name) || isEmpty(email) || isEmpty(pass) || isEmpty(verify)) {
					Toast.makeText(SignupActivity.this, "One or more fields are empty", Toast.LENGTH_SHORT).show();
					inputManager.hideSoftInputFromWindow(getCurrentFocus().getWindowToken(), InputMethodManager.HIDE_NOT_ALWAYS);
					return;
				} else if (!isEmailValid(email.getText().toString())) {
					Toast.makeText(SignupActivity.this, "Invalid email", Toast.LENGTH_SHORT).show();
					inputManager.hideSoftInputFromWindow(getCurrentFocus().getWindowToken(), InputMethodManager.HIDE_NOT_ALWAYS);
					return;
				} else if (!pass.getText().toString().equals(verify.getText().toString())) {
					Toast.makeText(SignupActivity.this, "Passwords do not match", Toast.LENGTH_SHORT).show();
					inputManager.hideSoftInputFromWindow(getCurrentFocus().getWindowToken(), InputMethodManager.HIDE_NOT_ALWAYS);
					return;
				} else if (pass.getText().toString().length() < 6) {
					Toast.makeText(SignupActivity.this, "Password must be at least 6 characters", Toast.LENGTH_SHORT).show();
					inputManager.hideSoftInputFromWindow(getCurrentFocus().getWindowToken(), InputMethodManager.HIDE_NOT_ALWAYS);
					return;
				}
				createAcc();
			}
		});
		initiateFragments();
	}

	/**
	 * Method called via OnListener in {@link CloudBackendFragment}.
	 */
	@Override
	public void onCreateFinished() {
		getAccounts();
	}

	private void createAcc() {
		CloudEntity account = new CloudEntity("ERAppAccounts");
		account.put("name", name.getText().toString());
		SharedPreferences.Editor editor = settings.edit();
		String email = this.email.getText().toString();
		editor.putString("email", email);
		for (CloudEntity ce : accountsList) {
			if (ce.get("email").toString().equalsIgnoreCase(email)) {
				InputMethodManager inputManager = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
				inputManager.hideSoftInputFromWindow(getCurrentFocus().getWindowToken(), InputMethodManager.HIDE_NOT_ALWAYS);
				Toast.makeText(SignupActivity.this, "Email already in use", Toast.LENGTH_SHORT).show();
				return;
			}
		}
		account.put("email", email);
		account.put("pass", pass.getText().toString());
		approved = true;

		int at = email.indexOf("@");
		int dot = email.indexOf(".");
		String eFormat = email.substring(0, at) + "_" + email.substring(at + 1, dot) + "_" + email.substring(dot + 1);
		editor.putString("emailFormatted", eFormat);

		if (checked && (company.getText().toString().trim().length() != 0)) {
			account.put("company", company.getText().toString());
			account.put("admin", true);
			account.put("approved", true);
			editor.putString("company", company.getText().toString());
			editor.putString("name", name.getText().toString());
			editor.putBoolean("employee", true);
			editor.putBoolean("admin", true);
			editor.putBoolean("approved", true);
			for (CloudEntity ce : accountsList) {
				if (ce.get("company") != null) {
					if (ce.get("company").equals(company.getText().toString())) {
						account.put("admin", false);
						editor.putBoolean("admin", false);
						account.put("approved", false);
						approved = false;
						break;
					}
				}
			}
		}
		editor.commit();

		CloudCallbackHandler<CloudEntity> handler = new CloudCallbackHandler<CloudEntity>() {
			@Override
			public void onComplete(final CloudEntity result) {
				getAccounts();
			}

			@Override
			public void onError(final IOException exception) {
				Toast.makeText(SignupActivity.this, "Unable to connect to server", Toast.LENGTH_SHORT).show();
			}
		};
		mProcessingFragment.getCloudBackend().insert(account, handler);
		goHome();
	}

	private void getAccounts() {
		CloudCallbackHandler<List<CloudEntity>> handler = new CloudCallbackHandler<List<CloudEntity>>() {
			@Override
			public void onComplete(List<CloudEntity> results) {
				accountsList = results;
			}

			@Override
			public void onError(IOException exception) {
			}
		};
		mProcessingFragment.getCloudBackend().listByKind("ERAppAccounts", CloudEntity.PROP_CREATED_AT, Order.DESC, 10000,
				Scope.PAST, handler);
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

	public void goHome() {
		SharedPreferences.Editor editor = settings.edit();
		editor.putBoolean("logged", true);
		editor.commit();
		Intent intent = new Intent(getApplicationContext(), HomeActivity.class);
		if (settings.getBoolean("employee", false) && !approved) {
			intent = new Intent(this, LoginActivity.class);
			InputMethodManager inputManager = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
			inputManager.hideSoftInputFromWindow(getCurrentFocus().getWindowToken(), InputMethodManager.HIDE_NOT_ALWAYS);
			Toast.makeText(this, "Please wait for approval from company administrator", Toast.LENGTH_LONG).show();
		}
		intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK | Intent.FLAG_ACTIVITY_NEW_TASK);
		startActivity(intent);
	}

	@Override
	public void onBroadcastMessageReceived(List<CloudEntity> message) {
	}
}