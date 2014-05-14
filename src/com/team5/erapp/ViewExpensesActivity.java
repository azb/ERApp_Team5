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

	private ListView mExpensesView;
	private Bundle data;
	private FragmentManager mFragmentManager;
	private CloudBackendFragment mProcessingFragment;
	private CloudEntity ce;
	private String str1;
	private SharedPreferences.Editor editor;
	private SharedPreferences settings;
	private int delPos;
	private static ProgressDialog progress;

	private static final String PROCESSING_FRAGMENT_TAG = "BACKEND_FRAGMENT";
	public static final String PREFS_NAME = "MyPrefsFile";

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
			Collections.sort(mExpenses, new Comparator<CloudEntity>() {
				@Override
				public int compare(CloudEntity ce1, CloudEntity ce2) {
					return ce1.getCreatedAt().compareTo(ce2.getCreatedAt());
				}
			});
			Collections.reverse(mExpenses);
			updateExpenseView(mExpenses);
			this.invalidateOptionsMenu();
			return true;
		case R.id.action_sortPrice:
			editor.putString("sort", "price");
			editor.commit();
			Collections.sort(mExpenses, new Comparator<CloudEntity>() {
				@Override
				public int compare(CloudEntity ce1, CloudEntity ce2) {
					return Double.compare(Double.parseDouble(ce1.get("price").toString()),
							Double.parseDouble(ce2.get("price").toString()));
				}
			});
			Collections.reverse(mExpenses);
			updateExpenseView(mExpenses);
			this.invalidateOptionsMenu();
			return true;
		case R.id.action_filter:
			AlertDialog.Builder builder = new AlertDialog.Builder(this);
			final CharSequence[] items1 = { "Previous month", "Previous year", "Recent" };
			final CharSequence[] items2 = { "Previous month", "Previous year", "Employee", "Recent" };
			DialogInterface.OnClickListener clickListener = new DialogInterface.OnClickListener() {

				@Override
				public void onClick(DialogInterface dialog, int which) {
					String type = "";
					if (settings.getBoolean("employee", false)) {
						type = items2[which].toString();
					} else {
						type = items1[which].toString();
					}
					if (type.equals("Previous month")) {
						final CharSequence[] itemsMonth = { "January", "February", "March", "April", "May", "June", "July",
								"August", "September", "October", "November", "December" };
						List<String> listMonths = new ArrayList<String>();
						for (int i = 0; i < Calendar.getInstance().get(Calendar.MONTH); i++) {
							listMonths.add(itemsMonth[i].toString());
						}
						final CharSequence[] prevMonths = listMonths.toArray(new CharSequence[listMonths.size()]);
						AlertDialog.Builder builder2 = new AlertDialog.Builder(ViewExpensesActivity.this);
						builder2.setTitle(R.string.title_month).setItems(prevMonths, new DialogInterface.OnClickListener() {
							public void onClick(DialogInterface dialong, int which) {
								progress.show();
								getFiltered(Integer.toString(which + 1), "month");
							}
						});
						builder2.create().show();
					} else if (type.equals("Previous year")) {
						final CharSequence[] itemsMonth = { "January", "February", "March", "April", "May", "June", "July",
								"August", "September", "October", "November", "December" };
						AlertDialog.Builder builder2 = new AlertDialog.Builder(ViewExpensesActivity.this);
						builder2.setTitle(R.string.title_month).setItems(itemsMonth, new DialogInterface.OnClickListener() {
							public void onClick(DialogInterface dialong, int which) {
								progress.show();
								getFiltered(Integer.toString(which + 1), "previousYearMonth");
							}
						});
						builder2.create().show();
					} else if (type.equals("Employee")) {
						AlertDialog.Builder builder3 = new AlertDialog.Builder(ViewExpensesActivity.this);
						builder3.setMessage("Enter employee's email:");
						final EditText input = new EditText(ViewExpensesActivity.this);
						input.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_EMAIL_ADDRESS);
						builder3.setView(input);
						builder3.setPositiveButton("Ok", new DialogInterface.OnClickListener() {

							@Override
							public void onClick(DialogInterface dialog, int whichButton) {
								progress.show();
								getFiltered(input.getText().toString().toLowerCase(Locale.getDefault()), "employee");
							}
						});
						builder3.setNegativeButton("Cancel", null);
						builder3.show();
					} else {
						progress.show();
						getFiltered("", "recent");
					}
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
			final CharSequence[] items3 = { "Current month", "Previous month", "Current year", "Previous year" };
			final CharSequence[] items4 = { "Current month", "Previous month", "Current year", "Previous year", "Employee" };
			DialogInterface.OnClickListener clickListener2 = new DialogInterface.OnClickListener() {

				@Override
				public void onClick(DialogInterface dialog, int which) {
					String type = "";
					if (settings.getBoolean("admin", false)) {
						type = items4[which].toString();
					} else {
						type = items3[which].toString();
					}
					if (type.equals("Current month")) {
						progress.show();
						getCSVExpenses("month", Integer.toString(Calendar.getInstance().get(Calendar.MONTH) + 1));
					} else if (type.equals("Previous month")) {
						final CharSequence[] itemsMonth = { "January", "February", "March", "April", "May", "June", "July",
								"August", "September", "October", "November", "December" };
						List<String> listMonths = new ArrayList<String>();
						for (int i = 0; i < Calendar.getInstance().get(Calendar.MONTH); i++) {
							listMonths.add(itemsMonth[i].toString());
						}
						final CharSequence[] prevMonths = listMonths.toArray(new CharSequence[listMonths.size()]);
						AlertDialog.Builder builder2 = new AlertDialog.Builder(ViewExpensesActivity.this);
						builder2.setTitle(R.string.title_month).setItems(prevMonths, new DialogInterface.OnClickListener() {
							public void onClick(DialogInterface dialong, int which) {
								progress.show();
								getCSVExpenses("month", Integer.toString(which + 1));
							}
						});
						builder2.create().show();
					} else if (type.equals("Current year")) {
						progress.show();
						getCSVExpenses("year", Integer.toString(Calendar.getInstance().get(Calendar.YEAR)));
					} else if (type.equals("Previous year")) {
						progress.show();
						getCSVExpenses("year", Integer.toString(Calendar.getInstance().get(Calendar.YEAR) - 1));
					} else if (type.equals("Employee")) {
						progress.show();
						AlertDialog.Builder builder3 = new AlertDialog.Builder(ViewExpensesActivity.this);
						builder3.setMessage("Enter employee's email:");
						final EditText input = new EditText(ViewExpensesActivity.this);
						input.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_EMAIL_ADDRESS);
						builder3.setView(input);
						builder3.setPositiveButton("Ok", new DialogInterface.OnClickListener() {
							public void onClick(DialogInterface dialog, int whichButton) {
								getCSVExpenses("employee", input.getText().toString().toLowerCase(Locale.getDefault()));
							}
						});
						builder3.setNegativeButton("Cancel", null);
						builder3.show();
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
				i.putExtra("id", ce.getId());
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
					Order.DESC, 300, Scope.PAST, handler);
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

	private void getFiltered(final String range, final String type) {
		String acc = settings.getString("emailFormatted", "");
		if (settings.getBoolean("employee", false)) {
			acc = "Co_" + settings.getString("company", "").replaceAll(" ", "_");
		}
		CloudQuery cq = new CloudQuery("ERApp_" + acc);
		if (type.equals("month")) {
			cq.setFilter(Filter.and(Filter.eq("month", range),
					Filter.in("year", Integer.toString(Calendar.getInstance().get(Calendar.YEAR)))));
		} else if (type.equals("employee")) {
			cq.setFilter(Filter.and(Filter.eq("_createdBy", range),
					Filter.in("year", Integer.toString(Calendar.getInstance().get(Calendar.YEAR)))));
		} else if (type.equals("previousYearMonth")) {
			cq.setFilter(Filter.and(Filter.eq("month", range),
					Filter.in("year", Integer.toString(Calendar.getInstance().get(Calendar.YEAR) - 1))));
		} else {
			cq.setLimit(300);
		}
		cq.setScope(Scope.PAST);
		mProcessingFragment.getCloudBackend().list(cq, new CloudCallbackHandler<List<CloudEntity>>() {
			@Override
			public void onComplete(List<CloudEntity> results) {
				mExpenses = results;
				Collections.sort(mExpenses, new Comparator<CloudEntity>() {
					@Override
					public int compare(CloudEntity ce1, CloudEntity ce2) {
						return ce1.getCreatedAt().compareTo(ce2.getCreatedAt());
					}
				});
				Collections.reverse(mExpenses);
				updateExpenseView(mExpenses);
			}

			@Override
			public void onError(IOException exception) {
				handleEndpointException(exception);
			}
		});
	}

	private void getCSVExpenses(final String type, final String range) {
		String acc = settings.getString("emailFormatted", "");
		if (settings.getBoolean("employee", false)) {
			acc = "Co_" + settings.getString("company", "").replaceAll(" ", "_");
		}
		CloudQuery cq = new CloudQuery("ERApp_" + acc);
		if (type.equals("year")) {
			if (settings.getBoolean("employee", false) && !settings.getBoolean("admin", false)) {
				cq.setFilter(Filter.and(Filter.in("year", range), Filter.eq("_createdBy", settings.getString("email", ""))));
			} else {
				cq.setFilter(Filter.in("year", range));
			}
		} else if (type.equals("month")) {
			if (settings.getBoolean("employee", false) && !settings.getBoolean("admin", false)) {
				cq.setFilter(Filter.and(Filter.eq("month", range), Filter.eq("_createdBy", settings.getString("email", "")),
						Filter.in("year", Integer.toString(Calendar.getInstance().get(Calendar.YEAR)))));
			} else {
				cq.setFilter(Filter.and(Filter.eq("month", range),
						Filter.in("year", Integer.toString(Calendar.getInstance().get(Calendar.YEAR)))));
			}
		} else if (type.equals("employee")) {
			cq.setFilter(Filter.and(Filter.eq("_createdBy", range),
					Filter.in("year", Integer.toString(Calendar.getInstance().get(Calendar.YEAR)))));
		}
		cq.setScope(Scope.PAST);
		mProcessingFragment.getCloudBackend().list(cq, new CloudCallbackHandler<List<CloudEntity>>() {
			@Override
			public void onComplete(List<CloudEntity> results) {
				str1 = "Price,Currency,Payment,Merchant,Category,Date,Description,Comments\n";
				appendEx(results);
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

			@Override
			public void onError(IOException exception) {
				handleEndpointException(exception);
			}
		});
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
	@SuppressWarnings("unchecked")
	private void appendEx(List<CloudEntity> a) {
		for (CloudEntity ce : a) {
			List<Object> ba = (ArrayList<Object>) ce.get("ex");
			str1 += ba.get(0).toString().replaceAll(",", ".") + ",";
			str1 += ba.get(5).toString() + ",";
			str1 += ba.get(7).toString() + ",";
			str1 += ba.get(1).toString().replaceAll(",", ";") + ",";
			str1 += ba.get(9).toString() + ",";
			str1 += ba.get(3).toString() + ",";
			str1 += ba.get(2).toString().replaceAll(",", ";") + ",";
			str1 += ba.get(4).toString().replaceAll(",", ";") + ",";
			str1 += "\n";
		}
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
					progress.dismiss();
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
