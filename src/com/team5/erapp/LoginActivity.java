package com.team5.erapp;

import java.io.IOException;
import java.util.List;

import com.google.cloud.backend.core.CloudBackendFragment;
import com.google.cloud.backend.core.CloudCallbackHandler;
import com.google.cloud.backend.core.CloudBackendFragment.OnListener;
import com.google.cloud.backend.core.CloudQuery.Order;
import com.google.cloud.backend.core.CloudQuery.Scope;
import com.google.cloud.backend.core.CloudEntity;
import com.team5.erapp.R;

import android.os.Bundle;
import android.app.Activity;
import android.app.FragmentManager;
import android.app.FragmentTransaction;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.ActivityInfo;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.inputmethod.InputMethodManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

public class LoginActivity extends Activity implements OnListener {

	private Button btnLogin;
	private EditText inputEmail;
	private EditText inputPassword;
	private List<CloudEntity> accounts;
	private String company;
	private Boolean approved;

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
		mFragmentManager = getFragmentManager();
		settings = getSharedPreferences(PREFS_NAME, 0);
		company = "";

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
				if (isEmpty(inputEmail) || isEmpty(inputPassword)) {
					Toast.makeText(LoginActivity.this, "Please input email and password", Toast.LENGTH_SHORT).show();
					inputManager.hideSoftInputFromWindow(getCurrentFocus().getWindowToken(), InputMethodManager.HIDE_NOT_ALWAYS);
					return;
				}
				login();
			}
		});
		initiateFragments();
	}

	@Override
	public void onResume() {
		super.onResume();
		getAccounts();
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

	private void login() {
		String e = inputEmail.getText().toString();
		String pass = inputPassword.getText().toString();
		Boolean exists = false;
		approved = true;
		for (CloudEntity ce : accounts) {
			if (ce.get("email").toString().equalsIgnoreCase(e)) {
				if (ce.get("pass").toString().equals(pass)) {
					exists = true;
					if (ce.get("company") != null) {
						company = ce.get("company").toString();
						SharedPreferences.Editor editor = settings.edit();
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
			}
		}
		if (!exists) {
			Toast.makeText(LoginActivity.this, "There was an error with your email/password combination", Toast.LENGTH_SHORT)
					.show();
			InputMethodManager inputManager = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
			inputManager.hideSoftInputFromWindow(getCurrentFocus().getWindowToken(), InputMethodManager.HIDE_NOT_ALWAYS);
		}
	}

	private void getAccounts() {
		CloudCallbackHandler<List<CloudEntity>> handler = new CloudCallbackHandler<List<CloudEntity>>() {
			@Override
			public void onComplete(List<CloudEntity> results) {
				accounts = results;
			}

			@Override
			public void onError(IOException exception) {
			}
		};
		mProcessingFragment.getCloudBackend().listByKind("ERAppAccounts", CloudEntity.PROP_CREATED_AT, Order.DESC, 10000,
				Scope.PAST, handler);
	}

	private boolean isEmpty(EditText etText) {
		return etText.getText().toString().trim().length() == 0;
	}

	private void goHome() {
		SharedPreferences.Editor editor = settings.edit();
		editor.putBoolean("logged", true);
		String email = inputEmail.getText().toString();
		editor.putString("email", email);
		int at = email.indexOf("@");
		int dot = email.indexOf(".");
		email = email.substring(0, at) + "_" + email.substring(at + 1, dot) + "_" + email.substring(dot + 1);
		editor.putString("emailFormatted", email);
		editor.putString("company", company);
		Intent intent = new Intent(this, HomeActivity.class);
		if (!approved) {
			InputMethodManager inputManager = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
			inputManager.hideSoftInputFromWindow(getCurrentFocus().getWindowToken(), InputMethodManager.HIDE_NOT_ALWAYS);
			Toast.makeText(this, "Please wait for approval from company administrator", Toast.LENGTH_LONG).show();
			editor.putBoolean("logged", false);
			editor.commit();
			return;
		}
		editor.commit();
		startActivity(intent);
		finish();
	}

	public void signUp(View view) {
		Intent intent = new Intent(this, SignupActivity.class);
		startActivity(intent);
	}
	
	@Override
	public void onCreateFinished() {
	}

	@Override
	public void onBroadcastMessageReceived(List<CloudEntity> message) {

	}
}