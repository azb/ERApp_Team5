package com.team5.erapp;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.LinkedList;
import java.util.List;
import java.util.Locale;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.FragmentManager;
import android.app.FragmentTransaction;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.ActivityInfo;
import android.os.Bundle;
import android.os.Environment;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.WindowManager;
import android.widget.AdapterView;
import android.widget.ListView;
import android.widget.Toast;

import com.team5.erapp.R;
import com.google.cloud.backend.core.CloudBackendFragment;
import com.google.cloud.backend.core.CloudBackendFragment.OnListener;
import com.google.cloud.backend.core.CloudCallbackHandler;
import com.google.cloud.backend.core.CloudEntity;
import com.google.cloud.backend.core.CloudQuery.Order;
import com.google.cloud.backend.core.CloudQuery.Scope;
import com.google.cloud.backend.core.Filter.Op;

/**
 * Shows a list of expenses.
 */
public class ViewExpensesActivity extends Activity implements OnListener {

	private static final String PROCESSING_FRAGMENT_TAG = "BACKEND_FRAGMENT";

	/*
	 * UI components
	 */
	private ListView mExpensesView;
	private ArrayList<CloudEntity> a;
	private Bundle data;
	private FragmentManager mFragmentManager;
	private CloudBackendFragment mProcessingFragment;

	public static final String PREFS_NAME = "MyPrefsFile";

	SharedPreferences settings;

	/**
	 * A list of expenses to be displayed
	 */
	private List<CloudEntity> mExpenses = new LinkedList<CloudEntity>();

	/**
	 * Override Activity lifecycle method.
	 */
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		this.setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);
		this.getWindow().setSoftInputMode(
				WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_HIDDEN);
		settings = getSharedPreferences(PREFS_NAME, 0);
		setContentView(R.layout.activity_display_expenses);
		initializePosts();
		data = getIntent().getExtras();
		if (data.get("display").equals("correct")) {
			setTitle("Incomplete Expenses");
		}
		if (!settings.getBoolean("update", true)
				&& !data.get("display").equals("correct")) {
			for (int i = 0; i < HomeActivity.a.size(); i++) {
				mExpenses.add(HomeActivity.a.get(i));
			}
			updateExpenseView();
		} else {
			mFragmentManager = getFragmentManager();
			initiateFragments();
		}
	}

	/**
	 * Method called via OnListener in {@link CloudBackendFragment}.
	 */
	@Override
	public void onCreateFinished() {
		listExpenses();
	}
	
	@Override
	public void onBackPressed() {
		if (settings.getBoolean("update", false)
				&& data.get("display").equals("view")) {
			HomeActivity.saveList(a);
			SharedPreferences.Editor editor = settings.edit();
			editor.putBoolean("update", false);
			editor.commit();
		}
		finish();
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		if (data.get("display").equals("view")) {
			getMenuInflater().inflate(R.menu.sort, menu);
			if (settings.getString("sort", "_createdAt").equals("_createdAt")) {
				menu.getItem(0).setVisible(false);
				menu.getItem(1).setVisible(true);
			} else if (settings.getString("sort", "_createdAt").equals("price")) {
				menu.getItem(0).setVisible(true);
				menu.getItem(1).setVisible(false);
			}
		}
		return true;
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		SharedPreferences.Editor editor = settings.edit();
		switch (item.getItemId()) {
		case R.id.action_sortDate:
			editor.putString("sort", "_createdAt");
			editor.putBoolean("update", true);
			editor.commit();
			finish();
			startActivity(getIntent());
			overridePendingTransition(0,0);
			return true;
		case R.id.action_sortPrice:
			editor.putString("sort", "price");
			editor.putBoolean("update", true);
			editor.commit();
			finish();
			startActivity(getIntent());
			overridePendingTransition(0,0);
			return true;
		case R.id.action_export:
			AlertDialog.Builder builder = new AlertDialog.Builder(this);
			final CharSequence[] items = { "All", "Month", "Year" };
			builder.setTitle(R.string.title_export).setItems(items,
					new DialogInterface.OnClickListener() {
						public void onClick(DialogInterface dialog, int which) {
							// The 'which' argument contains the index position
							// of the selected item
							String type = items[which].toString();
							if (type.equals("Month")) {
								final CharSequence[] itemsMonth = { "January",
										"February", "March", "April", "May",
										"June", "July", "August", "September",
										"October", "November", "December" };
								AlertDialog.Builder builder2 = new AlertDialog.Builder(
										ViewExpensesActivity.this);
								builder2.setTitle(R.string.title_month)
										.setItems(
												itemsMonth,
												new DialogInterface.OnClickListener() {
													public void onClick(
															DialogInterface dialong,
															int which) {
														String range = itemsMonth[which]
																.toString();
														createCSV(range,
																"month");
													}
												});
								builder2.create().show();
							} else if (type.equals("Year")) {
								final CharSequence[] itemsYear = { "2014",
										"2015", "2016" };
								AlertDialog.Builder builder3 = new AlertDialog.Builder(
										ViewExpensesActivity.this);
								builder3.setTitle(R.string.title_year)
										.setItems(
												itemsYear,
												new DialogInterface.OnClickListener() {
													public void onClick(
															DialogInterface dialong,
															int which) {
														String range = itemsYear[which]
																.toString();
														createCSV(range, "year");
													}
												});
								builder3.create().show();
							} else {
								createCSV(type, "all");
							}
						}
					});
			builder.create().show();
			return true;
		default:
			return super.onOptionsItemSelected(item);
		}
	}

	private void initiateFragments() {
		FragmentTransaction fragmentTransaction = mFragmentManager
				.beginTransaction();
	
		// Check to see if we have retained the fragment which handles
		// asynchronous backend calls
		mProcessingFragment = (CloudBackendFragment) mFragmentManager
				.findFragmentByTag(PROCESSING_FRAGMENT_TAG);
		// If not retained (or first time running), create a new one
		if (mProcessingFragment == null) {
			mProcessingFragment = new CloudBackendFragment();
			mProcessingFragment.setRetainInstance(true);
			fragmentTransaction.add(mProcessingFragment,
					PROCESSING_FRAGMENT_TAG);
		}
		fragmentTransaction.commit();
	}

	private void initializePosts() {
		mExpensesView = (ListView) findViewById(R.id.posts_list);
		mExpensesView
				.setOnItemClickListener(new ListView.OnItemClickListener() {

					@Override
					public void onItemClick(AdapterView<?> parent, View view,
							int position, long id) {
						CloudEntity ce = (CloudEntity) mExpensesView
								.getItemAtPosition(position);
						Intent i = new Intent(getBaseContext(),
								ExpenseActivity.class);
						if (data.get("display").equals("view")) {
							i.putExtra("display", "view");
						} else if (data.get("display").equals("correct")) {
							i.putExtra("display", "correct");
						}
						i.putExtra("expense", ce);
						i.putExtra("price", ce.get("price").toString());
						i.putExtra("merchant", ce.get("merchant").toString());
						i.putExtra("description", ce.get("description")
								.toString());
						i.putExtra("date", ce.get("date").toString());
						i.putExtra("comment", ce.get("comment").toString());
						i.putExtra("currency", Integer.parseInt(ce.get(
								"currencyPos").toString()));
						i.putExtra("category", Integer.parseInt(ce.get(
								"categoryPos").toString()));
						i.putExtra("payment", Integer.parseInt(ce.get(
								"paymentPos").toString()));
						startActivity(i);
					}
				});
	}

	/**
	 * Creates a CSV file with expense data.
	 * 
	 * @param range
	 *            Range such as January, February, 2014, etc.
	 * @param type
	 *            Type of category: all, month, or year
	 */
	public void createCSV(String range, String type) {
		String str1 = "Price,Currency,Payment,Merchant,Category,Date,Description,Comments\n";
		str1 = setRange(str1, range, type);
		final Calendar c = Calendar.getInstance();
		int yy = c.get(Calendar.YEAR);
		int mm = c.get(Calendar.MONTH);
		int dd = c.get(Calendar.DAY_OF_MONTH);
		int hh = c.get(Calendar.HOUR);
		int ii = c.get(Calendar.MINUTE);
		int ss = c.get(Calendar.SECOND);
		String curTime = String.format(Locale.getDefault(), "%02d-%02d-%02d",
				hh, ii, ss);
		String date = new StringBuilder().append(yy).append("-").append(dd)
				.append("-").append(mm + 1).append("_").append(curTime)
				.toString();
		writeFileOnSDCard(str1, getBaseContext(), "ERApp" + "_" + date + ".csv");
	}

	/**
	 * Sets the category range.
	 * 
	 * @param str1
	 *            CSV string
	 * @param range
	 *            Range such as January, February, 2014, etc.
	 * @param type
	 *            Type of category: all, month, or year
	 * @return A String containing expense data from chosen range.
	 */
	public String setRange(String str1, String range, String type) {
		for (int i = 0; i < mExpenses.size(); i++) {
			String date = mExpenses.get(i).get("date").toString();
			if (type.equals("all")) {
				str1 = appendEx(str1, i);
			} else if (type.equals("year")) {
				if (date.substring(date.length() - 4, date.length()).equals(
						range)) {
					str1 = appendEx(str1, i);
				}
			} else if (type.equals("month")) {
				String month = "";
				try {
					Date d = new SimpleDateFormat("MMM", Locale.ENGLISH)
							.parse(range);
					Calendar cal = Calendar.getInstance();
					cal.setTime(d);
					int m = cal.get(Calendar.MONTH) + 1;
					month = Integer.toString(m);
				} catch (ParseException e) {
					e.printStackTrace();
				}
				if (Integer.parseInt(date.substring(0, date.indexOf("/"))) == Integer
						.parseInt(month)) {
					str1 = appendEx(str1, i);
				}
			}
		}
		return str1;
	}

	/**
	 * Appends the expense data from chosen range to a String str1.
	 * 
	 * @param str1
	 *            The String to append to.
	 * @param i
	 *            Index of CloudEntity object satisfying the range.
	 * @return A String containing expense data from chosen range.
	 */
	public String appendEx(String str1, int i) {
		str1 += mExpenses.get(i).get("price").toString().replaceAll(",", ".")
				+ ",";
		str1 += mExpenses.get(i).get("currency").toString() + ",";
		str1 += mExpenses.get(i).get("payment").toString() + ",";
		str1 += mExpenses.get(i).get("merchant").toString()
				.replaceAll(",", ";")
				+ ",";
		str1 += mExpenses.get(i).get("category").toString() + ",";
		str1 += mExpenses.get(i).get("date").toString() + ",";
		str1 += mExpenses.get(i).get("description").toString()
				.replaceAll(",", ";")
				+ ",";
		str1 += mExpenses.get(i).get("comment").toString().replaceAll(",", ";")
				+ ",";
		str1 += "\n";
		return str1;
	}

	public static void writeFileOnSDCard(String strWrite, Context context,
			String fileName) {

		try {
			if (isSdReadable()) {
				File myFile = new File(
						Environment.getExternalStorageDirectory() + "/ERApp");
				if (!myFile.exists()) {
					File erappDirectory = new File(Environment
							.getExternalStorageDirectory().getPath()
							+ "/ERApp/");
					erappDirectory.mkdirs();
				}

				File file = new File(new File(Environment
						.getExternalStorageDirectory().getPath() + "/ERApp/"),
						fileName);
				if (file.exists()) {
					file.delete();
				}
				try {
					FileOutputStream fOut = new FileOutputStream(file);
					OutputStreamWriter myOutWriter = new OutputStreamWriter(
							fOut);
					myOutWriter.append(strWrite);
					myOutWriter.close();
					fOut.close();
					String path = myFile.getPath().substring(
							myFile.getPath().indexOf("/sd"));
					Toast.makeText(context, "Exported to folder " + path,
							Toast.LENGTH_LONG).show();
				} catch (Exception e) {
					e.printStackTrace();
				}
			}
		} catch (Exception e) {
		}
	}

	public static boolean isSdReadable() {
		boolean mExternalStorageAvailable = false;
		try {
			String state = Environment.getExternalStorageState();
			if (Environment.MEDIA_MOUNTED.equals(state)) {
				// We can read and write the media
				mExternalStorageAvailable = true;
				Log.i("isSdReadable", "External storage card is readable.");
			} else if (Environment.MEDIA_MOUNTED_READ_ONLY.equals(state)) {
				// We can only read the media
				Log.i("isSdReadable", "External storage card is readable.");
				mExternalStorageAvailable = true;
			} else {
				// Something else is wrong. It may be one of many other
				// states, but all we need to know is we can neither read nor
				// write
				mExternalStorageAvailable = false;
			}
		} catch (Exception ex) {
		}
		return mExternalStorageAvailable;
	}

	/**
	 * Retrieves the list of all expenses from the backend and updates the UI.
	 */
	private void listExpenses() {
		// create a response handler that will receive the result or an error
		CloudCallbackHandler<List<CloudEntity>> handler = new CloudCallbackHandler<List<CloudEntity>>() {
			@Override
			public void onComplete(List<CloudEntity> results) {
				mExpenses = results;
				if (settings.getBoolean("update", false)
						|| data.get("display").equals("correct")) {
					updateExpenseView();
				}
				if (!data.get("display").equals("correct")) {
					a = new ArrayList<CloudEntity>();
					for (int i = 0; i < mExpenses.size(); i++) {
						a.add(mExpenses.get(i));
					}
				}
			}

			@Override
			public void onError(IOException exception) {
				handleEndpointException(exception);
			}
		};
		if (data.get("display").equals("view")) {
			mProcessingFragment.getCloudBackend().listByKind("ERApp",
					settings.getString("sort", "_createdAt"), Order.DESC, 50,
					Scope.PAST, handler);
		} else if (data.get("display").equals("correct")) {
			mProcessingFragment.getCloudBackend().listByProperty("ERApp",
					"incomplete", Op.EQ, true, Order.DESC, 50, Scope.PAST,
					handler);
		}
	}

	private void handleEndpointException(IOException e) {
		Toast.makeText(this, e.toString(), Toast.LENGTH_LONG).show();
	}

	private void updateExpenseView() {
		if (!mExpenses.isEmpty()) {
			mExpensesView.setAdapter(new ExpensesListAdapter(this,
					android.R.layout.simple_list_item_1, mExpenses));
		} else {
			Toast.makeText(this, "No expenses to display", Toast.LENGTH_LONG)
					.show();
		}
	}

	/**
	 * Method called via OnListener in {@link CloudBackendFragment}.
	 */
	@Override
	public void onBroadcastMessageReceived(List<CloudEntity> l) {
	}
	
}
