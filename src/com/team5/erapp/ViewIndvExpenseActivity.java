package com.team5.erapp;

import android.app.Activity;
import android.content.pm.ActivityInfo;
import android.graphics.Color;
import android.os.Bundle;
import android.view.View;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.Spinner;

import com.google.cloud.backend.core.CloudEntity;
import com.team5.erapp.R;

public class ViewIndvExpenseActivity extends Activity {
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_add_expenses);
		this.setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);
		this.getWindow().setSoftInputMode(
				WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_HIDDEN);

		Button submit = (Button) findViewById(R.id.button_save);
		Button camera = (Button) findViewById(R.id.button_camera);
		Button gallery = (Button) findViewById(R.id.button_gallery);
		TouchImageView photoImage = (TouchImageView) findViewById(R.id.imageView1);
		EditText price = (EditText) findViewById(R.id.addExpensePrice);
		EditText merchant = (EditText) findViewById(R.id.addExpenseMerchant);
		EditText description = (EditText) findViewById(R.id.addExpenseDescription);
		EditText date = (EditText) findViewById(R.id.addExpenseDate);
		EditText comment = (EditText) findViewById(R.id.addExpenseComments);
		Spinner currency = (Spinner) findViewById(R.id.addExpenseCurrency);
		Spinner category = (Spinner) findViewById(R.id.addExpenseCategory);
		Spinner payment = (Spinner) findViewById(R.id.addExpensePayment);
		LinearLayout img = (LinearLayout) findViewById(R.id.AddExpensesImageBackground);

		submit.setVisibility(View.GONE);
		camera.setVisibility(View.GONE);
		gallery.setVisibility(View.GONE);
		price.setFocusable(false);
		merchant.setFocusable(false);
		description.setFocusable(false);
		date.setFocusable(false);
		comment.setFocusable(false);
		currency.setClickable(false);
		category.setClickable(false);
		payment.setClickable(false);
		
		Bundle data = getIntent().getExtras();
		CloudEntity ce = (CloudEntity) data.getParcelable("expense");
		
		price.setText(data.get("price").toString());
		merchant.setText(data.get("merchant").toString());
		description.setText(data.get("description").toString());
		date.setText(data.get("date").toString());
		comment.setText(data.get("comment").toString());
		currency.setSelection(data.getInt("currency"));
		category.setSelection(data.getInt("category"));
		payment.setSelection(data.getInt("payment"));
		
		img.setBackgroundColor(Color.GRAY);
	}
}
