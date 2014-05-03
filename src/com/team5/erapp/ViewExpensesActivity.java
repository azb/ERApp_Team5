/*
 * Copyright (c) 2013 Google Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except
 * in compliance with the License. You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the License
 * is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express
 * or implied. See the License for the specific language governing permissions and limitations under
 * the License.
 */

package com.team5.erapp;

import java.io.IOException;
import java.util.LinkedList;
import java.util.List;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.FragmentManager;
import android.app.FragmentTransaction;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.os.Bundle;
import android.os.Parcel;
import android.os.Parcelable;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.EditText;
import android.widget.ImageView;
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
import com.google.cloud.backend.core.Consts;

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
	 * "SELECT * FROM Guestbook ORDER BY _createdAt DESC LIMIT 50" This query
	 * will be re-executed when matching entity is updated.
	 */
	private void listPosts() {
		// create a response handler that will receive the result or an error
		CloudCallbackHandler<List<CloudEntity>> handler = new CloudCallbackHandler<List<CloudEntity>>() {
			@Override
			public void onComplete(List<CloudEntity> results) {
				mPosts = results;
				updateGuestbookView();
			}

			@Override
			public void onError(IOException exception) {
				handleEndpointException(exception);
			}
		};

		// execute the query with the handler
		mProcessingFragment.getCloudBackend().listByKind("ERApp",
				CloudEntity.PROP_CREATED_AT, Order.DESC, 50, Scope.PAST,
				handler);
	}

	private void handleEndpointException(IOException e) {
		Toast.makeText(this, e.toString(), Toast.LENGTH_LONG).show();
	}

	private void updateGuestbookView() {
		if (!mPosts.isEmpty()) {
			mPostsView.setVisibility(View.VISIBLE);
			mPostsView.setAdapter(new ExpensesListAdapter(this,
					android.R.layout.simple_list_item_1, mPosts));
		}
	}
}
