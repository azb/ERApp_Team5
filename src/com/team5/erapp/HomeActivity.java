package com.team5.erapp;

import com.team5.erapp.R;

import android.app.Activity;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.ActivityInfo;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;

public class HomeActivity extends Activity {

	public final static String EXTRA_MESSAGE = "com.example.app.MESSAGE";
	public static final String PREFS_NAME = "MyPrefsFile";
	SharedPreferences settings;
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		if (!logCheck()) {
			Intent intent = new Intent(this, LoginActivity.class);
			startActivity(intent);
			finish();
		}
		super.onCreate(savedInstanceState);
		this.setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);
		setContentView(R.layout.activity_home);
		if(!adminCheck()) {
			Button addEmployee = (Button) findViewById(R.id.button_addEmployee);
			addEmployee.setVisibility(View.GONE);
		}		
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		getMenuInflater().inflate(R.menu.main, menu);
		return true;
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		switch (item.getItemId()) {
		case R.id.action_logout:
			settings = getSharedPreferences(PREFS_NAME, 0);
			SharedPreferences.Editor editor = settings.edit();;
			editor.putBoolean("logged", false);
			editor.commit();
			Intent intent = new Intent(this, LoginActivity.class);
			startActivity(intent);
			finish();
			return true;
		default:
			return super.onOptionsItemSelected(item);
		}
	}
	
	public boolean logCheck() {
		settings = getSharedPreferences(PREFS_NAME, 0);
		return (settings.getBoolean("logged", false));
	}
	
	public boolean adminCheck() {
		settings = getSharedPreferences(PREFS_NAME, 0);
		//return true for now
		return (settings.getBoolean("admin", true));
	}

	public void addExpense(View view) {
		Intent intent = new Intent(this, ExpenseActivity.class);
		intent.putExtra("display", "add");
		startActivity(intent);
	}

	public void viewExpense(View view) {
		settings = getSharedPreferences(PREFS_NAME, 0);
		SharedPreferences.Editor editor = settings.edit();;
		editor.putString("sort", "_createdAt");
		editor.commit();
		Intent intent = new Intent(this, ViewExpensesActivity.class);
		intent.putExtra("display", "view");
		startActivity(intent);
	}

	public void correctExpense(View view) {
		settings = getSharedPreferences(PREFS_NAME, 0);
		SharedPreferences.Editor editor = settings.edit();;
		editor.putString("sort", "_createdAt");
		editor.commit();
		Intent intent = new Intent(this, ViewExpensesActivity.class);
		intent.putExtra("display", "correct");
		startActivity(intent);
	}

	public void addEmployee(View view) {
		Intent intent = new Intent(this, AddEmployeeActivity.class);
		startActivity(intent);
	}
}
