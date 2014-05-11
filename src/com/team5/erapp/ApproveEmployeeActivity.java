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
import android.app.ProgressDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.ActivityInfo;
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
	private CloudEntity ce;
	private ProgressDialog progress;
	private int delPos;

	public static final String PREFS_NAME = "MyPrefsFile";
	private SharedPreferences settings;

	/**
	 * A list of expenses to be displayed
	 */
	private List<CloudEntity> mEmployees = new LinkedList<CloudEntity>();

	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		this.setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);
		setContentView(R.layout.activity_add_employee);
		progress = new ProgressDialog(this);
		progress.setMessage("Loading...");
		progress.show();
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
				delPos = position;
				new AlertDialog.Builder(ApproveEmployeeActivity.this).setMessage("Approve " + ce.get("name").toString() + "?")
						.setNegativeButton(R.string.remove, new DialogInterface.OnClickListener() {
							@Override
							public void onClick(DialogInterface arg0, int arg1) {
								mProcessingFragment.getCloudBackend().delete(ce, new CloudCallbackHandler<Void>() {
									@Override
									public void onComplete(Void result) {
										mEmployees.remove(delPos);
										updateEmployeesView();
									}

									@Override
									public void onError(final IOException exception) {
									}

								});
							}
						}).setPositiveButton(android.R.string.yes, new DialogInterface.OnClickListener() {

							public void onClick(DialogInterface arg0, int arg1) {
								ce.put("approved", true);
								mProcessingFragment.getCloudBackend().update(ce, new CloudCallbackHandler<CloudEntity>() {
									@Override
									public void onComplete(final CloudEntity result) {
										mEmployees.remove(delPos);
										updateEmployeesView();
									}

									@Override
									public void onError(final IOException exception) {
									}
								});
							}
						}).create().show();
				return;
			}
		});
	}

	private void listEmployees() {
		CloudQuery cq = new CloudQuery("ERAppAccounts");
		cq.setFilter(Filter.and(Filter.eq("approved", false), Filter.eq("company", settings.getString("company", ""))));
		cq.setScope(Scope.PAST);
		mProcessingFragment.getCloudBackend().list(cq, new CloudCallbackHandler<List<CloudEntity>>() {
			@Override
			public void onComplete(List<CloudEntity> results) {
				for (CloudEntity ce : results) {
					mEmployees.add(ce);
				}
				updateEmployeesView();
			}

			@Override
			public void onError(IOException exception) {
				TextView emptyView = (TextView) findViewById(R.id.no_employees);
				emptyView.setText("Unable to connect to server.");
			}
		});
	}

	private void updateEmployeesView() {
		progress.dismiss();
		TextView emptyView = (TextView) findViewById(R.id.no_employees);
		if (!mEmployees.isEmpty()) {
			mEmployeesView.setVisibility(View.VISIBLE);
			mEmployeesView.setAdapter(new EmployeesListAdapter(this, android.R.layout.simple_list_item_1, mEmployees));
		} else {
			mEmployeesView.setVisibility(View.GONE);
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
