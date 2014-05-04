package com.team5.erapp;

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
import android.widget.ListView;
import android.widget.Toast;

import com.team5.erapp.R;
import com.google.cloud.backend.core.CloudBackendFragment;
import com.google.cloud.backend.core.CloudBackendFragment.OnListener;
import com.google.cloud.backend.core.CloudCallbackHandler;
import com.google.cloud.backend.core.CloudEntity;
import com.google.cloud.backend.core.CloudQuery.Order;
import com.google.cloud.backend.core.CloudQuery.Scope;

/**
 * Shows a list of expenses.
 */
@SuppressLint("CommitTransaction")
public class ViewExpensesActivity extends Activity implements OnListener {

	private static final String PROCESSING_FRAGMENT_TAG = "BACKEND_FRAGMENT";

	/*
	 * UI components
	 */
	private ListView mPostsView;
	private CloudEntity ce;

	private FragmentManager mFragmentManager;
	private CloudBackendFragment mProcessingFragment;

	public static final String PREFS_NAME = "MyPrefsFile";

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

		// Create the view
		mPostsView = (ListView) findViewById(R.id.posts_list);
		mPostsView.setOnItemClickListener(new ListView.OnItemClickListener() {

			@Override
			public void onItemClick(AdapterView<?> parent, View view,
					int position, long id) {
				ce = (CloudEntity) mPostsView.getItemAtPosition(position);
				Intent i = new Intent(getBaseContext(),
						ViewIndvExpenseActivity.class);
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
		mFragmentManager = getFragmentManager();

		initiateFragments();
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		getMenuInflater().inflate(R.menu.sort, menu);
		SharedPreferences settings = getSharedPreferences(PREFS_NAME, 0);
		if(settings.getString("sort", "date").equals("date")) {
			menu.getItem(0).setVisible(false);
			menu.getItem(1).setVisible(true);
		} else if(settings.getString("sort", "date").equals("price")) {
			menu.getItem(0).setVisible(true);
			menu.getItem(1).setVisible(false);
		}
		return true;
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		SharedPreferences settings = getSharedPreferences(PREFS_NAME, 0);
		SharedPreferences.Editor editor = settings.edit();
		switch (item.getItemId()) {
		case R.id.action_sortDate:
			editor.putString("sort", "date");
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
		default:
			return super.onOptionsItemSelected(item);
		}
	}

	/**
	 * Method called via OnListener in {@link CloudBackendFragment}.
	 */
	@Override
	public void onCreateFinished() {
		listPosts();
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
	 * Retrieves the list of all posts from the backend and updates the UI. For
	 * demonstration in this sample, the query that is executed is:
	 * "SELECT * FROM ERApp ORDER BY _createdAt DESC LIMIT 50" This query will
	 * be re-executed when matching entity is updated.
	 */
	private void listPosts() {
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
		
		SharedPreferences settings = getSharedPreferences(PREFS_NAME, 0);
		if(settings.getString("sort", "date").equals("date")) {
			mProcessingFragment.getCloudBackend().listByKind("ERApp",
					CloudEntity.PROP_CREATED_AT, Order.DESC, 50, Scope.PAST,
					handler);
		}
		//need to retrieve price to sort
		else if(settings.getString("sort", "date").equals("price")) {
			mProcessingFragment.getCloudBackend().listByKind("ERApp",
					CloudEntity.PROP_UPDATED_BY, Order.ASC, 50, Scope.PAST,
					handler);
		}
	}

	private void handleEndpointException(IOException e) {
		Toast.makeText(this, e.toString(), Toast.LENGTH_LONG).show();
	}

	private void updateExpenseView() {
		if (!mPosts.isEmpty()) {
			mPostsView.setAdapter(new ExpensesListAdapter(this,
					android.R.layout.simple_list_item_1, mPosts));
		}
	}
}
