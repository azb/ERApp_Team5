package com.team5.erapp;

import java.io.IOException;
import java.util.LinkedList;
import java.util.List;

import com.google.cloud.backend.core.CloudBackend;
import com.google.cloud.backend.core.CloudBackendFragment;
import com.google.cloud.backend.core.CloudCallbackHandler;
import com.google.cloud.backend.core.CloudEntity;
import com.google.cloud.backend.core.CloudQuery;
import com.google.cloud.backend.core.CloudQuery.Order;
import com.google.cloud.backend.core.CloudQuery.Scope;
import com.google.cloud.backend.core.Filter;
import com.team5.erapp.R;

import android.app.Activity;
import android.app.FragmentManager;
import android.app.FragmentTransaction;
import android.content.pm.ActivityInfo;
import android.net.Uri;
import android.os.Bundle;
import android.view.View;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ExpandableListView;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.Spinner;
import android.widget.Toast;

public class CorrectExpensesActivity extends Activity {

	private ExpandableListView expenseView;
	private TouchImageView photoImage = null;
	private EditText price;
	private EditText merchant;
	private EditText description;
	private EditText date;
	private EditText comment;
	private Spinner currency;
	private Spinner category;
	private Spinner payment;
	private Uri fileUri = null;
	private LinearLayout img;
	private Button submit;

	private CloudBackendFragment mProcessingFragment;
	private FragmentManager mFragmentManager;
	private List<CloudEntity> expensesList = new LinkedList<CloudEntity>();
	private static final String PROCESSING_FRAGMENT_TAG = "BACKEND_FRAGMENT";

	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_correct_expenses);
		this.setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);
		this.getWindow().setSoftInputMode(
				WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_HIDDEN);

		// initialize variables
		expenseView = (ExpandableListView) findViewById(R.id.expandableListView);
		submit = (Button) findViewById(R.id.button_save);
		photoImage = (TouchImageView) findViewById(R.id.imageView1);
		price = (EditText) findViewById(R.id.addExpensePrice);
		merchant = (EditText) findViewById(R.id.addExpenseMerchant);
		description = (EditText) findViewById(R.id.addExpenseDescription);
		date = (EditText) findViewById(R.id.addExpenseDate);
		comment = (EditText) findViewById(R.id.addExpenseComments);
		currency = (Spinner) findViewById(R.id.addExpenseCurrency);
		category = (Spinner) findViewById(R.id.addExpenseCategory);
		payment = (Spinner) findViewById(R.id.addExpensePayment);
		img = (LinearLayout) findViewById(R.id.AddExpensesImageBackground);
		mFragmentManager = getFragmentManager();

	}

	private void handleEndpointException(IOException e) {
		Toast.makeText(this, e.toString(), Toast.LENGTH_LONG).show();
	}

	/**
     * Method called via OnListener in {@link CloudBackendFragment}.
     */
    public void onCreateFinished() {
        listExpenses();
    }
    
    private void listExpenses() {
        // create a response handler that will receive the result or an error
        CloudCallbackHandler<List<CloudEntity>> handler =
                new CloudCallbackHandler<List<CloudEntity>>() {
                    @Override
                    public void onComplete(List<CloudEntity> results) {
                        expensesList = results;
                    }

                    @Override
                    public void onError(IOException exception) {
                        handleEndpointException(exception);
                    }
                };

        // execute the query with the handler
        mProcessingFragment.getCloudBackend().listByKind(
                "ERApp", CloudEntity.PROP_CREATED_AT, Order.DESC, 50,
                Scope.FUTURE_AND_PAST, handler);
    }
    
    private void initiateFragments() {
        FragmentTransaction fragmentTransaction = mFragmentManager.beginTransaction();

        // Check to see if we have retained the fragment which handles
        // asynchronous backend calls
        mProcessingFragment = (CloudBackendFragment) mFragmentManager.
                findFragmentByTag(PROCESSING_FRAGMENT_TAG);
        // If not retained (or first time running), create a new one
        if (mProcessingFragment == null) {
            mProcessingFragment = new CloudBackendFragment();
            mProcessingFragment.setRetainInstance(true);
            fragmentTransaction.add(mProcessingFragment, PROCESSING_FRAGMENT_TAG);
        }
    }
}
