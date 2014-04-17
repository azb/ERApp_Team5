package com.team5.erapp;

import com.team5.erapp.R;

import android.app.Activity;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.View;
import android.widget.CheckBox;

public class SignupActivity extends Activity{
	
	private static final String PREFS_NAME = "MyPrefsFile";
	
	@Override
    protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_signup);
	}
	
	public void viewHome(View view) {
		SharedPreferences settings = getSharedPreferences(PREFS_NAME, 0);
    	SharedPreferences.Editor editor = settings.edit();
    	editor.putBoolean("logged", true);
    	editor.commit();
    	Intent intent = new Intent(getApplicationContext(), HomeActivity.class);
    	intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK|Intent.FLAG_ACTIVITY_NEW_TASK);
    	startActivity(intent);
    }
}