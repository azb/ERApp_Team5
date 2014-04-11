package com.example.app;

import android.app.Activity;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.ActivityInfo;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.CheckBox;

public class MainActivity extends Activity {

	public final static String EXTRA_MESSAGE = "com.example.app.MESSAGE";
	public static final String PREFS_NAME = "MyPrefsFile";

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		if (!logCheck()) {
			Intent intent = new Intent(this, LoginActivity.class);
			startActivity(intent);
		}
		super.onCreate(savedInstanceState);
		this.setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);
		setContentView(R.layout.activity_main_corp);
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
			SharedPreferences settings = getSharedPreferences(PREFS_NAME, 0);
			SharedPreferences.Editor editor = settings.edit();
			editor.putBoolean("logged", false);
			editor.commit();
			Intent intent = new Intent(this, LoginActivity.class);
			startActivity(intent);
			return true;

		default:
			return super.onOptionsItemSelected(item);
		}
	}

	public boolean logCheck() {
		SharedPreferences settings = getSharedPreferences(PREFS_NAME, 0);
		return (settings.getBoolean("logged", false));
	}

	public void addExpense(View view) {
		Intent intent = new Intent(this, AddExpenseActivity.class);
		startActivity(intent);
	}

	public void viewExpense(View view) {
		Intent intent = new Intent(this, ViewExpensesActivity.class);
		startActivity(intent);
	}

	public void correctExpense(View view) {
		Intent intent = new Intent(this, CorrectExpensesActivity.class);
		startActivity(intent);
	}

	public void addEmployee(View view) {
		Intent intent = new Intent(this, AddEmployeeActivity.class);
		startActivity(intent);
	}
}
