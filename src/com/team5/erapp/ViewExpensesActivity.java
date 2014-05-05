package com.team5.erapp;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileWriter;
import java.io.IOException;
import java.util.LinkedList;
import java.util.List;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.FragmentManager;
import android.app.FragmentTransaction;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.ActivityInfo;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.TextView;
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
	private ListView mPostsView;
	private TextView empty;
	
	private FragmentManager mFragmentManager;
	private CloudBackendFragment mProcessingFragment;

	public static final String PREFS_NAME = "MyPrefsFile";

	SharedPreferences settings;

	/**
	 * A list of posts to be displayed
	 */
	private List<CloudEntity> mPosts = new LinkedList<CloudEntity>();

	/**
	 * Override Activity lifecycle method.
	 */
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_display_expenses);
		this.setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);
		Bundle data = getIntent().getExtras();
		if (data.get("display").equals("correct")) {
			setTitle("Incomplete Expenses");
		}
		// Create the view
//		LinearLayout display = (LinearLayout) findViewById(R.layout.activity_display_expenses);
//		empty = new TextView(this);
//		empty.setText("No expenses to display");
//		display.addView(empty);
		mPostsView = (ListView) findViewById(R.id.posts_list);
		mPostsView.setOnItemClickListener(new ListView.OnItemClickListener() {

			@Override
			public void onItemClick(AdapterView<?> parent, View view,
					int position, long id) {
				CloudEntity ce = (CloudEntity) mPostsView
						.getItemAtPosition(position);
				Bundle data = getIntent().getExtras();
				Intent i = new Intent(getBaseContext(),
						ViewIndvExpenseActivity.class);
				if (data.get("display").equals("correct")) {
					i = new Intent(getBaseContext(), ExpenseActivity.class);
					i.putExtra("correct", true);
				}
				i.putExtra("expense", ce);
				i.putExtra("price", ce.get("price").toString());
				i.putExtra("merchant", ce.get("merchant").toString());
				i.putExtra("description", ce.get("description").toString());
				i.putExtra("date", ce.get("date").toString());
				i.putExtra("comment", ce.get("comment").toString());
				i.putExtra("currency",
						Integer.parseInt(ce.get("currencyPos").toString()));
				i.putExtra("category",
						Integer.parseInt(ce.get("categoryPos").toString()));
				i.putExtra("payment",
						Integer.parseInt(ce.get("paymentPos").toString()));
				startActivity(i);
			}
		});
		settings = getSharedPreferences(PREFS_NAME, 0);
		mFragmentManager = getFragmentManager();

		initiateFragments();
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		Bundle data = getIntent().getExtras();
		if(data.get("display").equals("view")) {
			getMenuInflater().inflate(R.menu.sort, menu);
			if (settings.getString("sort", "_createdAt").equals("_createdAt")) {
				menu.getItem(0).setVisible(false);
				menu.getItem(1).setVisible(true);
			} else if (settings.getString("sort", "date").equals("price")) {
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
			editor.commit();
			finish();
			overridePendingTransition(0, 0);
			startActivity(getIntent());
			return true;
		case R.id.action_sortPrice:
			editor.putString("sort", "price");
			editor.commit();
			finish();
			overridePendingTransition(0, 0);
			startActivity(getIntent());
			return true;
		case R.id.action_export:
			
			String str1 = "Merchant,Price,Spent By,Date,\n";
			for (int i = 0; i < mPosts.size(); i++) {
				str1 += mPosts.get(i).get("price").toString() + ",";
				str1 += mPosts.get(i).get("merchant").toString() + ",";
				str1 += mPosts.get(i).get("comment").toString() + ",";
				str1 += mPosts.get(i).get("currency").toString() + ",";
				str1 += mPosts.get(i).get("category").toString() + ",";
				str1 += mPosts.get(i).get("payment").toString() + ",";
				str1 += mPosts.get(i).get("date").toString() + ",";
				str1 += mPosts.get(i).get("description").toString() + ",";
				str1 += "\n";
				//Toast.makeText(this, mPosts.get(i).get("price").toString(), Toast.LENGTH_SHORT).show();
			}
			//FileBrowser
			File newCSVFile = new File("test.csv");
			try {
				FileWriter out = new FileWriter(newCSVFile);
				out.write(str1);
				out.close();
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			
			
			
			return true;
		default:
			return super.onOptionsItemSelected(item);
		}
	}

	/**
	 * Method called via OnListener in {@link CloudBackendFragment}.
	 */
	@Override
	public void onCreateFinished() {
		listExpenses();
	}

	/**
	 * Method called via OnListener in {@link CloudBackendFragment}.
	 */
	@Override
	public void onBroadcastMessageReceived(List<CloudEntity> l) {
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

	/**
	 * Retrieves the list of all expenses from the backend and updates the UI.
	 */
	private void listExpenses() {
		// create a response handler that will receive the result or an error
		CloudCallbackHandler<List<CloudEntity>> handler = new CloudCallbackHandler<List<CloudEntity>>() {
			@Override
			public void onComplete(List<CloudEntity> results) {
				mPosts = results;
				updateExpenseView();
			}

			@Override
			public void onError(IOException exception) {
				handleEndpointException(exception);
			}
		};
		Bundle data = getIntent().getExtras();
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
		if (!mPosts.isEmpty()) {
//			empty.setVisibility(View.GONE);
			mPostsView.setAdapter(new ExpensesListAdapter(this,
					android.R.layout.simple_list_item_1, mPosts));
		} else {
			Toast.makeText(this, "No expenses to display", Toast.LENGTH_LONG).show();
		}
	}
}
