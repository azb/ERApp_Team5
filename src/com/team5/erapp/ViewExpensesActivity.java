package com.team5.erapp;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedList;
import java.util.List;
import java.util.Locale;

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
import android.os.Environment;
import android.text.InputType;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.WindowManager;
import android.widget.AdapterView;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import com.team5.erapp.R;
import com.google.cloud.backend.core.CloudBackendFragment;
import com.google.cloud.backend.core.CloudBackendFragment.OnListener;
import com.google.cloud.backend.core.CloudCallbackHandler;
import com.google.cloud.backend.core.CloudEntity;
import com.google.cloud.backend.core.CloudQuery;
import com.google.cloud.backend.core.Filter;
import com.google.cloud.backend.core.CloudQuery.Order;
import com.google.cloud.backend.core.CloudQuery.Scope;

/**
 * Shows a list of expenses.
 */
public class ViewExpensesActivity extends Activity implements OnListener {

	/*
	 * UI components
	 */
	private ListView mExpensesView;
	private Bundle data;
	private FragmentManager mFragmentManager;
	private CloudBackendFragment mProcessingFragment;
	private ProgressDialog progress;
	private CloudEntity ce;
	private Boolean filtered = false;
	private int delPos;
	private SharedPreferences.Editor editor;
	private Boolean open = true;

	private static final String PROCESSING_FRAGMENT_TAG = "BACKEND_FRAGMENT";
	public static final String PREFS_NAME = "MyPrefsFile";
	private SharedPreferences settings;

	/**
	 * A list of expenses to be displayed
	 */
	private List<CloudEntity> mExpenses = new LinkedList<CloudEntity>();
	private List<CloudEntity> filteredExpenses = new LinkedList<CloudEntity>();

	/**
	 * Override Activity lifecycle method.
	 */
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		this.setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);
		this.getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_HIDDEN);
		setContentView(R.layout.activity_display_expenses);
		settings = getSharedPreferences(PREFS_NAME, 0);
		data = getIntent().getExtras();
		editor = settings.edit();

		initializeView();

		progress = new ProgressDialog(this);
		progress.setMessage("Loading...");
		progress.show();
		if (data.get("display").equals("correct")) {
			setTitle("Correct Expenses");
		}
		mFragmentManager = getFragmentManager();
		initiateFragments();
	}

	/**
	 * Method called via OnListener in {@link CloudBackendFragment}.
	 */
	@Override
	public void onCreateFinished() {
		listExpenses();
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
		switch (item.getItemId()) {
		case R.id.action_sortDate:
			editor.putString("sort", "_createdAt");
			editor.commit();
			List<CloudEntity> dateSort = new LinkedList<CloudEntity>();
			if (filtered) {
				dateSort = filteredExpenses;
			} else {
				dateSort = mExpenses;
			}
			Collections.sort(dateSort, new Comparator<CloudEntity>() {
				@Override
				public int compare(CloudEntity ce1, CloudEntity ce2) {
					return ce1.getCreatedAt().compareTo(ce2.getCreatedAt());
				}
			});
			Collections.reverse(dateSort);
			updateExpenseView(dateSort);
			this.invalidateOptionsMenu();
			return true;
		case R.id.action_sortPrice:
			editor.putString("sort", "price");
			editor.commit();
			List<CloudEntity> priceSort = new LinkedList<CloudEntity>();
			if (filtered) {
				priceSort = filteredExpenses;
			} else {
				priceSort = mExpenses;
			}
			Collections.sort(priceSort, new Comparator<CloudEntity>() {
				@Override
				public int compare(CloudEntity ce1, CloudEntity ce2) {
					return Double.compare(Double.parseDouble(ce1.get("price").toString()),
							Double.parseDouble(ce2.get("price").toString()));
				}
			});
			Collections.reverse(priceSort);
			updateExpenseView(priceSort);
			this.invalidateOptionsMenu();
			return true;
		case R.id.action_filter:
			AlertDialog.Builder builder = new AlertDialog.Builder(this);
			final CharSequence[] items1 = { "Month", "Current year", "Previous year", "All" };
			final CharSequence[] items2 = { "Month", "Current year", "Previous year", "Employee", "All" };
			DialogInterface.OnClickListener clickListener = new DialogInterface.OnClickListener() {

				@Override
				public void onClick(DialogInterface dialog, int which) {
					String type = "";
					if (settings.getBoolean("employee", false)) {
						type = items2[which].toString();
					} else {
						type = items1[which].toString();
					}
					if (type.equals("Month")) {
						final CharSequence[] itemsMonth = { "January", "February", "March", "April", "May", "June", "July",
								"August", "September", "October", "November", "December" };
						AlertDialog.Builder builder2 = new AlertDialog.Builder(ViewExpensesActivity.this);
						builder2.setTitle(R.string.title_month).setItems(itemsMonth, new DialogInterface.OnClickListener() {
							public void onClick(DialogInterface dialong, int which) {
								filterEx("month", which, "");
								editor.putBoolean("filtered", true);
								editor.putString("filter", "month");
								editor.putInt("month", which);
								editor.commit();
							}
						});
						builder2.create().show();
					} else if (type.equals("Current year")) {
						filterEx("current year", which, "");
						editor.putBoolean("filtered", true);
						editor.putString("filter", "year");
					} else if (type.equals("Previous year")) {
						filterEx("previous year", which, "");
						editor.putBoolean("filtered", false);
					} else if (type.equals("Employee")) {
						AlertDialog.Builder builder3 = new AlertDialog.Builder(ViewExpensesActivity.this);
						builder3.setMessage("Enter employee's email:");
						final EditText input = new EditText(ViewExpensesActivity.this);
						input.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_EMAIL_ADDRESS);
						builder3.setView(input);
						builder3.setPositiveButton("Ok", new DialogInterface.OnClickListener() {

							@Override
							public void onClick(DialogInterface dialog, int whichButton) {
								filterEx("employee", 0, input.getText().toString());
							}
						});
						builder3.setNegativeButton("Cancel", null);
						builder3.show();
						editor.putBoolean("filtered", false);
					} else {
						editor.putBoolean("filtered", true);
						editor.putString("filter", "all");
						filtered = false;
						updateExpenseView(mExpenses);
					}
					editor.commit();
				}
			};
			if (settings.getBoolean("employee", false)) {
				builder.setTitle(R.string.title_filter).setItems(items2, clickListener);
			} else {
				builder.setTitle(R.string.title_filter).setItems(items1, clickListener);
			}
			builder.create().show();
			return true;
		case R.id.action_export:
			AlertDialog.Builder builder2 = new AlertDialog.Builder(this);
			final CharSequence[] items3 = { "Month", "Current year", "Previous year", "All" };
			final CharSequence[] items4 = { "Month", "Current year", "Previous year", "Employee", "All" };
			DialogInterface.OnClickListener clickListener2 = new DialogInterface.OnClickListener() {

				@Override
				public void onClick(DialogInterface dialog, int which) {
					String type = "";
					if (settings.getBoolean("admin", false)) {
						type = items4[which].toString();
					} else {
						type = items3[which].toString();
					}
					if (type.equals("Month")) {
						final CharSequence[] itemsMonth = { "January", "February", "March", "April", "May", "June", "July",
								"August", "September", "October", "November", "December" };
						AlertDialog.Builder builder2 = new AlertDialog.Builder(ViewExpensesActivity.this);
						builder2.setTitle(R.string.title_month).setItems(itemsMonth, new DialogInterface.OnClickListener() {
							public void onClick(DialogInterface dialong, int which) {
								String month = Integer.toString(which + 1);
								createCSV(month, "month");
							}
						});
						builder2.create().show();
					} else if (type.equals("Current year")) {
						int year = Calendar.getInstance().get(Calendar.YEAR);
						createCSV(Integer.toString(year), "year");
					} else if (type.equals("Previous year")) {
						int year = Calendar.getInstance().get(Calendar.YEAR);
						createCSV(Integer.toString(year - 1), "year");
					} else if (type.equals("Employee")) {
						AlertDialog.Builder builder3 = new AlertDialog.Builder(ViewExpensesActivity.this);
						builder3.setMessage("Enter employee's email:");
						final EditText input = new EditText(ViewExpensesActivity.this);
						input.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_EMAIL_ADDRESS);
						builder3.setView(input);
						builder3.setPositiveButton("Ok", new DialogInterface.OnClickListener() {
							public void onClick(DialogInterface dialog, int whichButton) {
								createCSV(input.getText().toString(), "employee");
							}
						});
						builder3.setNegativeButton("Cancel", null);
						builder3.show();
					} else {
						createCSV("", "all");
					}
				}
			};

			if (settings.getBoolean("admin", false)) {
				builder2.setTitle(R.string.title_export).setItems(items4, clickListener2);
			} else {
				builder2.setTitle(R.string.title_export).setItems(items3, clickListener2);
			}
			builder2.create().show();
			return true;
		default:
			return super.onOptionsItemSelected(item);
		}
	}

	private void initiateFragments() {
		FragmentTransaction fragmentTransaction = mFragmentManager.beginTransaction();

		// Check to see if we have retained the fragment which handles
		// asynchronous backend calls
		mProcessingFragment = (CloudBackendFragment) mFragmentManager.findFragmentByTag(PROCESSING_FRAGMENT_TAG);
		// If not retained (or first time running), create a new one
		if (mProcessingFragment == null) {
			mProcessingFragment = new CloudBackendFragment();
			mProcessingFragment.setRetainInstance(true);
			fragmentTransaction.add(mProcessingFragment, PROCESSING_FRAGMENT_TAG);
		}
		fragmentTransaction.commit();
	}

	private void initializeView() {
		mExpensesView = (ListView) findViewById(R.id.posts_list);
		mExpensesView.setOnItemClickListener(new ListView.OnItemClickListener() {

			@Override
			public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
				CloudEntity ce = (CloudEntity) mExpensesView.getItemAtPosition(position);
				Intent i = new Intent(getBaseContext(), ExpenseActivity.class);
				if (data.get("display").equals("view")) {
					i.putExtra("display", "view");
				} else if (data.get("display").equals("correct")) {
					i.putExtra("display", "correct");
				}
				@SuppressWarnings("unchecked")
				List<Object> ab = (ArrayList<Object>) ce.get("ex");
				i.putExtra("expense", ce);
				i.putExtra("price", ab.get(0).toString());
				i.putExtra("merchant", ab.get(1).toString());
				i.putExtra("description", ab.get(2).toString());
				i.putExtra("date", ab.get(3).toString());
				i.putExtra("comment", ab.get(4).toString());
				i.putExtra("currency", ab.get(6).toString());
				i.putExtra("category", ab.get(10).toString());
				i.putExtra("payment", ab.get(8).toString());
				i.putExtra("name", ce.get("name").toString());
				i.putExtra("hasPic", false);
				if (ce.get("pic") != null) {
					i.putExtra("pic", ce.get("pic").toString());
					i.putExtra("hasPic", true);
				}
				startActivity(i);
			}
		});
		if (data.get("display").equals("correct")) {
			mExpensesView.setOnItemLongClickListener(new ListView.OnItemLongClickListener() {

				@Override
				public boolean onItemLongClick(AdapterView<?> parent, View view, int position, long id) {
					ce = (CloudEntity) mExpensesView.getItemAtPosition(position);
					delPos = position;
					new AlertDialog.Builder(ViewExpensesActivity.this).setMessage("Delete expense?")
							.setNegativeButton(android.R.string.no, null)
							.setPositiveButton(android.R.string.yes, new DialogInterface.OnClickListener() {
								@Override
								public void onClick(DialogInterface arg0, int arg1) {
									progress.setMessage("Deleting...");
									progress.show();
									CloudCallbackHandler<Void> handler = new CloudCallbackHandler<Void>() {
										@Override
										public void onComplete(Void result) {
											progress.dismiss();
											mExpenses.remove(delPos);
											updateExpenseView(mExpenses);
										}

										@Override
										public void onError(final IOException exception) {
										}

									};
									mProcessingFragment.getCloudBackend().delete(ce, handler);
								}
							}).create().show();
					return true;
				}
			});
		}
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
				updateExpenseView(mExpenses);
			}

			@Override
			public void onError(IOException exception) {
				handleEndpointException(exception);
			}
		};
		String acc = settings.getString("emailFormatted", "");
		if (settings.getBoolean("employee", false)) {
			acc = "Co_" + settings.getString("company", "").replaceAll(" ", "_");
		}
		if (data.get("display").equals("view")) {
			mProcessingFragment.getCloudBackend().listByKind("ERApp_" + acc, settings.getString("sort", "_createdAt"),
					Order.DESC, 10000, Scope.PAST, handler);
		} else if (data.get("display").equals("correct")) {
			CloudQuery cq = new CloudQuery("ERApp_" + acc);
			if (!settings.getBoolean("admin", false)) {
				cq.setFilter(Filter.and(Filter.eq(CloudEntity.PROP_CREATED_BY, settings.getString("email", "")),
						Filter.eq("correctable", true)));
			} else {
				cq.setFilter(Filter.eq("correctable", true));
			}
			cq.setScope(Scope.PAST);
			mProcessingFragment.getCloudBackend().list(cq, handler);
		}
	}

	private void updateExpenseView(List<CloudEntity> expenses) {
		TextView emptyView = (TextView) findViewById(R.id.no_expenses);
		if (!expenses.isEmpty()) {
			progress.dismiss();
			emptyView.setVisibility(View.GONE);
			mExpensesView.setVisibility(View.VISIBLE);
			if (data.getString("display").equals("correct")) {
				Collections.sort(mExpenses, new Comparator<CloudEntity>() {
					@Override
					public int compare(CloudEntity ce1, CloudEntity ce2) {
						return ce1.getCreatedAt().compareTo(ce2.getCreatedAt());
					}
				});
				Collections.reverse(mExpenses);
			}
			if (data.getString("display").equals("view") && open) {
				if (settings.getBoolean("filtered", false)) {
					String fil = settings.getString("filter", "");
					if (fil.equals("month")) {
						filterEx("month", settings.getInt("month", 0), "");
						return;
					} else if (fil.equals("year")) {
						filterEx("current year", Calendar.getInstance().get(Calendar.YEAR), "");
						return;
					} else if (fil.equals("all")) {
						expenses = mExpenses;
						editor.putBoolean("filtered", false);
						editor.commit();
					}
				}
			}
			mExpensesView.setAdapter(new ExpensesListAdapter(this, android.R.layout.simple_list_item_1, expenses));
		} else if (data.get("display").equals("correct")) {
			progress.dismiss();
			mExpensesView.setVisibility(View.GONE);
			emptyView.setText("No expenses to correct");
			emptyView.setVisibility(View.VISIBLE);
		} else {
			progress.dismiss();
			mExpensesView.setVisibility(View.GONE);
			emptyView.setVisibility(View.VISIBLE);
		}
	}

	@SuppressWarnings("unchecked")
	private void filterEx(String filter, int range, String input) {
		filteredExpenses = new ArrayList<CloudEntity>();
		List<Object> a = new ArrayList<Object>();
		editor.putString("sort", "_createdAt");
		editor.commit();
		List<CloudEntity> dateSort = new LinkedList<CloudEntity>();
		if (filtered) {
			dateSort = filteredExpenses;
		} else {
			dateSort = mExpenses;
		}
		Collections.sort(dateSort, new Comparator<CloudEntity>() {
			@Override
			public int compare(CloudEntity ce1, CloudEntity ce2) {
				return ce1.getCreatedAt().compareTo(ce2.getCreatedAt());
			}
		});
		Collections.reverse(dateSort);
		this.invalidateOptionsMenu();
		for (CloudEntity ce : mExpenses) {
			a = (ArrayList<Object>) ce.get("ex");
			String date = a.get(3).toString();
			if (filter.equals("month")) {
				if (Integer.parseInt(date.substring(0, date.indexOf("/"))) == range + 1) {
					filteredExpenses.add(ce);
				}
			} else if (filter.equals("current year")) {
				if (Integer.parseInt(date.substring(date.length() - 4, date.length())) == Calendar.getInstance().get(
						Calendar.YEAR)) {
					filteredExpenses.add(ce);
				}
			} else if (filter.equals("previous year")) {
				if (Integer.parseInt(date.substring(date.length() - 4, date.length())) == Calendar.getInstance().get(
						Calendar.YEAR) - 1) {
					filteredExpenses.add(ce);
				}
			} else if (filter.equals("employee")) {
				if (input.equalsIgnoreCase(ce.getCreatedBy())) {
					filteredExpenses.add(ce);
				}
			}
		}
		open = false;
		filtered = true;
		updateExpenseView(filteredExpenses);
	}

	/**
	 * Creates a CSV file with expense data.
	 * 
	 * @param range
	 *            Range such as January, February, 2014, etc.
	 * @param type
	 *            Type of category: all, month, or year
	 */
	private void createCSV(String range, String type) {
		String str1 = "Price,Currency,Payment,Merchant,Category,Date,Description,Comments\n";
		str1 = setRange(str1, range, type);
		final Calendar c = Calendar.getInstance();
		int yy = c.get(Calendar.YEAR);
		int mm = c.get(Calendar.MONTH);
		int dd = c.get(Calendar.DAY_OF_MONTH);
		int hh = c.get(Calendar.HOUR);
		int ii = c.get(Calendar.MINUTE);
		int ss = c.get(Calendar.SECOND);
		String curTime = String.format(Locale.getDefault(), "%02d-%02d-%02d", hh, ii, ss);
		String date = new StringBuilder().append(yy).append("-").append(mm + 1).append("-").append(dd).append("_")
				.append(curTime).toString();
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
	@SuppressWarnings("unchecked")
	private String setRange(String str1, String range, String type) {
		List<Object> a = new ArrayList<Object>();
		for (CloudEntity ce : mExpenses) {
			a = (ArrayList<Object>) ce.get("ex");
			String date = a.get(3).toString();
			if (type.equals("all")) {
				if (settings.getBoolean("employee", false) && !settings.getBoolean("admin", false)) {
					if (ce.getCreatedBy().toString().equals(settings.getString("email", ""))) {
						str1 = appendEx(str1, a);
					}
				} else {
					str1 = appendEx(str1, a);
				}
			} else if (type.equals("year")) {
				if (date.substring(date.length() - 4, date.length()).equals(range)) {
					if (settings.getBoolean("employee", false) && !settings.getBoolean("admin", false)) {
						if (ce.getCreatedBy().toString().equals(settings.getString("email", ""))) {
							str1 = appendEx(str1, a);
						}
					} else {
						str1 = appendEx(str1, a);
					}
				}
			} else if (type.equals("month")) {
				if (Integer.parseInt(date.substring(0, date.indexOf("/"))) == Integer.parseInt(range)) {
					if (settings.getBoolean("employee", false) && !settings.getBoolean("admin", false)) {
						if (ce.getCreatedBy().toString().equals(settings.getString("email", ""))) {
							str1 = appendEx(str1, a);
						}
					} else {
						str1 = appendEx(str1, a);
					}
				}
			} else if (type.equals("employee")) {
				if (ce.getCreatedBy().toString().equalsIgnoreCase(range)) {
					str1 = appendEx(str1, a);
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
	private String appendEx(String str1, List<Object> a) {
		str1 += a.get(0).toString().replaceAll(",", ".") + ",";
		str1 += a.get(5).toString() + ",";
		str1 += a.get(7).toString() + ",";
		str1 += a.get(1).toString().replaceAll(",", ";") + ",";
		str1 += a.get(9).toString() + ",";
		str1 += a.get(3).toString() + ",";
		str1 += a.get(2).toString().replaceAll(",", ";") + ",";
		str1 += a.get(4).toString().replaceAll(",", ";") + ",";
		str1 += "\n";
		return str1;
	}

	private static void writeFileOnSDCard(String strWrite, Context context, String fileName) {

		try {
			if (isSdReadable()) {
				File myFile = new File(Environment.getExternalStorageDirectory() + "/ERApp");
				if (!myFile.exists()) {
					File erappDirectory = new File(Environment.getExternalStorageDirectory().getPath() + "/ERApp/");
					erappDirectory.mkdirs();
				}

				File file = new File(new File(Environment.getExternalStorageDirectory().getPath() + "/ERApp/"), fileName);
				if (file.exists()) {
					file.delete();
				}
				try {
					FileOutputStream fOut = new FileOutputStream(file);
					OutputStreamWriter myOutWriter = new OutputStreamWriter(fOut);
					myOutWriter.append(strWrite);
					myOutWriter.close();
					fOut.close();
					String path = myFile.getPath();
					if (path.contains("/storage/")) {
						path = path.substring(path.indexOf("/storage/") + 8);
					}
					Toast.makeText(context, "Exported to folder " + path, Toast.LENGTH_LONG).show();
				} catch (Exception e) {
					e.printStackTrace();
				}
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	private static boolean isSdReadable() {
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

	private void handleEndpointException(IOException e) {
		TextView emptyView = (TextView) findViewById(R.id.no_expenses);
		emptyView.setText("Unable to connect to server");
	}

	/**
	 * Method called via OnListener in {@link CloudBackendFragment}.
	 */
	@Override
	public void onBroadcastMessageReceived(List<CloudEntity> l) {
	}
}
