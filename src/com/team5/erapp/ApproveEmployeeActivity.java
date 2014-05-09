package com.team5.erapp;

import java.io.IOException;
import java.util.LinkedList;
import java.util.List;

import com.google.cloud.backend.core.CloudBackendFragment.OnListener;
import com.google.cloud.backend.core.CloudQuery;
import com.google.cloud.backend.core.CloudQuery.Scope;
import com.google.cloud.backend.core.Filter;
import com.google.cloud.backend.core.CloudBackendFragment;
import com.google.cloud.backend.core.CloudCallbackHandler;
import com.google.cloud.backend.core.CloudEntity;
import com.team5.erapp.R;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.FragmentManager;
import android.app.FragmentTransaction;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ListView;
import android.widget.TextView;

public class ApproveEmployeeActivity extends Activity implements OnListener {

	private static final String PROCESSING_FRAGMENT_TAG = "BACKEND_FRAGMENT";

	/*
	 * UI components
	 */
	private ListView mEmployeesView;
	private FragmentManager mFragmentManager;
	private CloudBackendFragment mProcessingFragment;
	private TextView emptyView;
	private CloudEntity ce;
	
	public static final String PREFS_NAME = "MyPrefsFile";
	private SharedPreferences settings;

	/**
	 * A list of expenses to be displayed
	 */
	private List<CloudEntity> mEmployees = new LinkedList<CloudEntity>();

	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_add_employee);
		emptyView = (TextView) findViewById(R.id.no_employees);
		emptyView.setText("Loading...");
		emptyView.setVisibility(View.VISIBLE);
		settings = getSharedPreferences(PREFS_NAME, 0);
		initializeView();
		mFragmentManager = getFragmentManager();
		initiateFragments();
	}

	@Override
	public void onCreateFinished() {
		listEmployees();
	}

	private void initializeView() {
		mEmployeesView = (ListView) findViewById(R.id.employees_list);
		mEmployeesView.setOnItemClickListener(new ListView.OnItemClickListener() {

			@Override
			public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
				ce = (CloudEntity) mEmployeesView.getItemAtPosition(position);
				new AlertDialog.Builder(ApproveEmployeeActivity.this).setMessage("Approve user?")
						.setNegativeButton(android.R.string.no, null)
						.setPositiveButton(android.R.string.yes, new DialogInterface.OnClickListener() {

							public void onClick(DialogInterface arg0, int arg1) {
								ce.put("approved", true);
								CloudCallbackHandler<CloudEntity> handler = new CloudCallbackHandler<CloudEntity>() {
									@Override
									public void onComplete(final CloudEntity result) {
										Intent i = getIntent();
										finish();
										overridePendingTransition(0, 0);
										startActivity(i);
									}

									@Override
									public void onError(final IOException exception) {
									}
								};
								mProcessingFragment.getCloudBackend().update(ce,
										handler);
							}
						}).create().show();
				return;
			}
		});
	}

	private void listEmployees() {
		// create a response handler that will receive the result or an error
		CloudCallbackHandler<List<CloudEntity>> handler = new CloudCallbackHandler<List<CloudEntity>>() {
			@Override
			public void onComplete(List<CloudEntity> results) {
				for (int i = 0; i < results.size(); i++) {
					mEmployees.add(results.get(i));
				}
				updateEmployeesView();
			}

			@Override
			public void onError(IOException exception) {
				emptyView.setText("Unable to connect to server");
			}
		};
		CloudQuery cq = new CloudQuery("ERAppAccounts");
		cq.setFilter(Filter.and(Filter.eq("approved", false), Filter.eq("company", settings.getString("company", ""))));
		cq.setScope(Scope.PAST);
		mProcessingFragment.getCloudBackend().list(cq, handler);
	}

	private void updateEmployeesView() {
		if (!mEmployees.isEmpty()) {
			emptyView.setVisibility(View.GONE);
			mEmployeesView.setAdapter(new EmployeesListAdapter(this, android.R.layout.simple_list_item_1, mEmployees));
		} else {
			emptyView.setText("No pending approvals");
			emptyView.setVisibility(View.VISIBLE);
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

	@Override
	public void onBroadcastMessageReceived(List<CloudEntity> message) {
	}
}
