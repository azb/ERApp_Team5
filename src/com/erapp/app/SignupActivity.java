package com.erapp.app;

import com.erapp.app.R;

import android.app.Activity;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.View;
import android.widget.CheckBox;

public class SignupActivity extends Activity{
	
	public static final String PREFS_NAME = "MyPrefsFile";
	
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
    	Intent intent = new Intent(this, HomeActivity.class);
    	startActivity(intent);
    }
}