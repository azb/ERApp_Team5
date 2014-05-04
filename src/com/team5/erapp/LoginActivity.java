package com.team5.erapp;

import com.team5.erapp.R;

import android.os.Bundle;
import android.app.Activity;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.ActivityInfo;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;

public class LoginActivity extends Activity {

	private Button btnLogin;
	private Button Btnregister;
	private EditText inputEmail;
	private EditText inputPassword;

	private static final String PREFS_NAME = "MyPrefsFile";

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		this.setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);
		setContentView(R.layout.activity_login);
		inputEmail = (EditText) findViewById(R.id.emailLogin);
		inputPassword = (EditText) findViewById(R.id.addExpensePrice);
		Btnregister = (Button) findViewById(R.id.buttonSignup);
		btnLogin = (Button) findViewById(R.id.buttonLogin);
	}

	// remove when account system is implemented
	public void viewHome(View view) {
		SharedPreferences settings = getSharedPreferences(PREFS_NAME, 0);
		SharedPreferences.Editor editor = settings.edit();
		editor.putBoolean("logged", true);
		editor.commit();
		Intent intent = new Intent(this, HomeActivity.class);
		startActivity(intent);
		finish();
	}

	public void signUp(View view) {
		Intent intent = new Intent(this, SignupActivity.class);
		startActivity(intent);
	}
}