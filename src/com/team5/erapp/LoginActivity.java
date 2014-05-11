package com.team5.erapp;

import java.io.IOException;
import java.security.NoSuchAlgorithmException;
import java.security.spec.InvalidKeySpecException;
import java.util.List;
import java.util.Locale;

import com.google.cloud.backend.core.CloudBackendFragment;
import com.google.cloud.backend.core.CloudCallbackHandler;
import com.google.cloud.backend.core.CloudQuery;
import com.google.cloud.backend.core.Filter;
import com.google.cloud.backend.core.CloudBackendFragment.OnListener;
import com.google.cloud.backend.core.CloudQuery.Scope;
import com.google.cloud.backend.core.CloudEntity;
import com.team5.erapp.R;

import android.os.Bundle;
import android.app.Activity;
import android.app.FragmentManager;
import android.app.FragmentTransaction;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.ActivityInfo;
import android.view.KeyEvent;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.View.OnKeyListener;
import android.view.inputmethod.InputMethodManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

public class LoginActivity extends Activity implements OnListener {

	private Button btnLogin;
	private EditText inputEmail;
	private EditText inputPassword;
	private Boolean approved;
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
		setContentView(R.layout.activity_login);
		PRNGFixes.apply();
		
		mFragmentManager = getFragmentManager();
		settings = getSharedPreferences(PREFS_NAME, 0);

		SharedPreferences.Editor editor = settings.edit();
		editor.clear();
		editor.commit();

		inputEmail = (EditText) findViewById(R.id.emailLogin);
		inputPassword = (EditText) findViewById(R.id.passLogin);
		btnLogin = (Button) findViewById(R.id.buttonLogin);
		btnLogin.setOnClickListener(new OnClickListener() {

			@Override
			public void onClick(View v) {
				InputMethodManager inputManager = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
				inputManager.hideSoftInputFromWindow(getCurrentFocus().getWindowToken(), InputMethodManager.HIDE_NOT_ALWAYS);
				if (isEmpty(inputEmail) || isEmpty(inputPassword)) {
					Toast.makeText(LoginActivity.this, "Please input email and password.", Toast.LENGTH_SHORT).show();
					return;
				}
				progress = new ProgressDialog(LoginActivity.this);
				progress.setMessage("Logging in...");
				progress.show();
				final String pass = inputPassword.getText().toString();
				checkCredentials(inputEmail.getText().toString(), pass);
			}
		});
		inputPassword.setImeActionLabel("Log in", KeyEvent.KEYCODE_ENTER);
		inputPassword.setOnKeyListener(new OnKeyListener() {
			public boolean onKey(View v, int keyCode, KeyEvent event) {
				// If the event is a key-down event on the "enter" button
				if ((event.getAction() == KeyEvent.ACTION_DOWN) && (keyCode == KeyEvent.KEYCODE_ENTER)) {
					if (isEmpty(inputEmail) || isEmpty(inputPassword)) {
						Toast.makeText(LoginActivity.this, "Please input email and password.", Toast.LENGTH_SHORT).show();
						return false;
					}
					progress = new ProgressDialog(LoginActivity.this);
					progress.setMessage("Logging in...");
					progress.show();
					final String pass = inputPassword.getText().toString();
					checkCredentials((inputEmail.getText().toString()), pass);
					return true;
				}
				return false;
			}
		});
		initiateFragments();
	}

	private void checkCredentials(String email, final String pass) {
		CloudQuery cq = new CloudQuery("ERAppAccounts");
		cq.setFilter(Filter.eq("email", email.toLowerCase(Locale.getDefault())));
		cq.setScope(Scope.PAST);
		mProcessingFragment.getCloudBackend().list(cq, new CloudCallbackHandler<List<CloudEntity>>() {
			@Override
			public void onComplete(List<CloudEntity> results) {
				if (!results.isEmpty()) {
					boolean matched = false;
					String storedPass = (String) results.get(0).get("pass").toString();
					try {
						matched = PassHash.validatePassword(pass, storedPass);
					} catch (NoSuchAlgorithmException e) {
						e.printStackTrace();
					} catch (InvalidKeySpecException e) {
						e.printStackTrace();
					}
					if (matched) {
						login(results.get(0));
					} else {
						progress.dismiss();
						Toast.makeText(LoginActivity.this, "There was an error with your email/password combination.",
								Toast.LENGTH_SHORT).show();
					}
				} else {
					progress.dismiss();
					Toast.makeText(LoginActivity.this, "There was an error with your email/password combination.",
							Toast.LENGTH_SHORT).show();
				}
			}

			@Override
			public void onError(IOException exception) {
			}
		});
	}

	private void login(CloudEntity ce) {
		approved = true;
		if (ce.get("company") != null) {
			SharedPreferences.Editor editor = settings.edit();
			editor.putString("company", ce.get("company").toString());
			editor.putBoolean("employee", true);
			editor.putString("name", ce.get("name").toString());
			if (ce.get("approved").equals(false)) {
				approved = false;
			}
			if (ce.get("admin").equals(true)) {
				editor.putBoolean("admin", true);
			}
			editor.commit();
		}
		goHome();
	}

	private boolean isEmpty(EditText etText) {
		return etText.getText().toString().trim().length() == 0;
	}

	private void goHome() {
		SharedPreferences.Editor editor = settings.edit();
		String email = inputEmail.getText().toString();
		editor.putString("email", email);
		int at = email.indexOf("@");
		int dot = email.indexOf(".");
		email = email.substring(0, at) + "_" + email.substring(at + 1, dot) + "_" + email.substring(dot + 1);
		editor.putString("emailFormatted", email);
		Intent intent = new Intent(this, HomeActivity.class);
		if (!approved) {
			progress.dismiss();
			Toast.makeText(this, "Please wait for approval from company admin.", Toast.LENGTH_LONG).show();
			editor.commit();
			return;
		} else {
			editor.putBoolean("logged", true);
		}
		editor.commit();
		progress.dismiss();
		startActivity(intent);
		finish();
	}

	public void signUp(View view) {
		Intent intent = new Intent(this, SignupActivity.class);
		startActivity(intent);
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

	@Override
	public void onCreateFinished() {
	}

	@Override
	public void onBroadcastMessageReceived(List<CloudEntity> message) {

	}
}