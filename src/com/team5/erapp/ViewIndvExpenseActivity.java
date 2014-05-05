package com.team5.erapp;

import java.text.DecimalFormat;

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
import android.widget.TextView;

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

		TouchImageView photoImage = (TouchImageView) findViewById(R.id.imageView1);
		TextView priceTitle = (TextView) findViewById(R.id.addExpense_price);
		EditText price = (EditText) findViewById(R.id.addExpensePrice);
		EditText merchant = (EditText) findViewById(R.id.addExpenseMerchant);
		EditText description = (EditText) findViewById(R.id.addExpenseDescription);
		EditText date = (EditText) findViewById(R.id.addExpenseDate);
		EditText comment = (EditText) findViewById(R.id.addExpenseComments);
		Spinner currency = (Spinner) findViewById(R.id.addExpenseCurrency);
		Spinner category = (Spinner) findViewById(R.id.addExpenseCategory);
		Spinner payment = (Spinner) findViewById(R.id.addExpensePayment);
		Button submit = (Button) findViewById(R.id.button_submit);
		LinearLayout img = (LinearLayout) findViewById(R.id.AddExpensesImageBackground);
		LinearLayout layout = (LinearLayout) findViewById(R.id.addExpense_imageSelect);

		priceTitle.setPadding(0, 10, 0, 0);
		layout.setVisibility(View.GONE);
		price.setFocusable(false);
		merchant.setFocusable(false);
		description.setFocusable(false);
		date.setFocusable(false);
		comment.setFocusable(false);
		currency.setClickable(false);
		category.setClickable(false);
		payment.setClickable(false);
		submit.setVisibility(View.GONE);

		Bundle data = getIntent().getExtras();
		CloudEntity ce = (CloudEntity) data.getParcelable("expense");

		if (data.get("price").toString().equals("-1")) {
			price.setText("");
		} else {
			DecimalFormat format = new DecimalFormat("#");
			format.setMinimumFractionDigits(2);
			double amount = Double.parseDouble(data.get("price").toString());
			price.setText(format.format(amount));
			// price.setText(data.get("price").toString());
		}
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
