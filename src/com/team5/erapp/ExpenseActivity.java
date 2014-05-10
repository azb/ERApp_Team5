package com.team5.erapp;

import java.io.IOException;
import java.io.File;
import java.net.URL;
import java.text.DecimalFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Locale;

import com.google.cloud.backend.android.mobilebackend.Mobilebackend.BlobEndpoint;
import com.google.cloud.backend.core.CloudBackendFragment;
import com.google.cloud.backend.core.CloudBackendFragment.OnListener;
import com.google.cloud.backend.core.CloudCallbackHandler;
import com.google.cloud.backend.core.CloudEntity;
import com.team5.erapp.R;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.AlertDialog;
import android.app.DatePickerDialog;
import android.app.FragmentManager;
import android.app.FragmentTransaction;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.ActivityInfo;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.graphics.Matrix;
import android.graphics.drawable.BitmapDrawable;
import android.media.ExifInterface;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.provider.MediaStore;
import android.util.Log;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.WindowManager;
import android.view.inputmethod.InputMethodManager;
import android.widget.Button;
import android.widget.DatePicker;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

public class ExpenseActivity extends Activity implements OnListener {

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
	private SharedPreferences settings;
	private String toast;
	private int length;
	private boolean incomplete;
	private int yy, mm, dd;

	private CloudBackendFragment mProcessingFragment;
	private FragmentManager mFragmentManager;

	public static final String PREFS_NAME = "MyPrefsFile";
	private static final String PROCESSING_FRAGMENT_TAG = "BACKEND_FRAGMENT";
	private static final String TAG = "CallCamera";
	private static final int CAPTURE_IMAGE_ACTIVITY_REQ = 0;
	private static final int SELECT_IMAGE = 1;

	@SuppressLint("NewApi")
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_expenses);
		setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);
		getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_ADJUST_PAN);
//		getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_HIDDEN);

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
		submit = (Button) findViewById(R.id.button_submit);

		mFragmentManager = getFragmentManager();
		settings = getSharedPreferences(PREFS_NAME, 0);

		currency.setSelection(settings.getInt("index", 7));
		img.setBackgroundColor(Color.GRAY);

//		OnClickListener hideKey = new OnClickListener() {
//
//			@Override
//			public void onClick(View v) {
//				getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_ADJUST_PAN);
//			}
//		};
//		price.setOnClickListener(hideKey);
//		merchant.setOnClickListener(hideKey);
//		description.setOnClickListener(hideKey);
//		comment.setOnClickListener(hideKey);
		
		final Calendar c = Calendar.getInstance();
		yy = c.get(Calendar.YEAR);
		mm = c.get(Calendar.MONTH);
		dd = c.get(Calendar.DAY_OF_MONTH);

		date.setText(new StringBuilder().append(mm + 1).append("/").append(dd).append("/").append(yy));
		date.setFocusable(false);
		date.setOnClickListener(new OnClickListener() {

			@Override
			public void onClick(View v) {
				DatePickerDialog dpd = new DatePickerDialog(ExpenseActivity.this, new DatePickerDialog.OnDateSetListener() {

					@Override
					public void onDateSet(DatePicker view, int year, int monthOfYear, int dayOfMonth) {
						yy = year;
						mm = monthOfYear;
						dd = dayOfMonth;
						date.setText(new StringBuilder().append(mm + 1).append("/").append(dd).append("/").append(yy));
					}
				}, yy, mm, dd);
				dpd.show();
			}
		});

		Button callCameraButton = (Button) findViewById(R.id.button_camera);
		callCameraButton.setOnClickListener(new View.OnClickListener() {
			public void onClick(View view) {
				Intent i = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
				fileUri = Uri.fromFile(getOutputPhotoFile());
				i.putExtra(MediaStore.EXTRA_OUTPUT, fileUri);
				startActivityForResult(i, CAPTURE_IMAGE_ACTIVITY_REQ);
			}
		});

		Button callGalleryButton = (Button) findViewById(R.id.button_gallery);
		callGalleryButton.setOnClickListener(new View.OnClickListener() {
			public void onClick(View view) {
				Intent i = new Intent(Intent.ACTION_PICK, android.provider.MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
				startActivityForResult(i, SELECT_IMAGE);
			}
		});

		Bundle data = getIntent().getExtras();
		if (data.get("display").equals("view") || data.get("display").equals("correct")) {
			setTitle("Correct Expense");
			setInputs();
		}
		initiateFragments();
	}

	@Override
	public void onBackPressed() {
		Bundle data = getIntent().getExtras();
		if ((!price.getText().toString().isEmpty() || !merchant.getText().toString().isEmpty()
				|| !description.getText().toString().isEmpty() || !comment.getText().toString().isEmpty())
				&& !data.get("display").equals("view") && !data.get("display").equals("correct")) {
			new AlertDialog.Builder(this).setMessage("Discard changes?").setNegativeButton(android.R.string.no, null)
					.setPositiveButton(android.R.string.yes, new DialogInterface.OnClickListener() {

						public void onClick(DialogInterface arg0, int arg1) {
							finish();
						}
					}).create().show();
			return;
		} else {
			finish();
		}
	}

	protected void onActivityResult(int requestCode, int resultCode, Intent data) {
		if (requestCode == CAPTURE_IMAGE_ACTIVITY_REQ) {
			if (resultCode == RESULT_OK) {
				Uri photoUri = null;
				if (data == null) {
					photoUri = fileUri;
				} else {
					photoUri = data.getData();
				}
				showPhoto(photoUri.getPath());
			} else if (resultCode == RESULT_CANCELED) {
			}
		}
		if (requestCode == SELECT_IMAGE && resultCode == RESULT_OK && null != data) {
			fileUri = data.getData();
			String[] filePathColumn = { MediaStore.Images.Media.DATA };
			Cursor cursor = getContentResolver().query(fileUri, filePathColumn, null, null, null);
			cursor.moveToFirst();
			int columnIndex = cursor.getColumnIndex(filePathColumn[0]);
			String picturePath = cursor.getString(columnIndex);
			cursor.close();
			Bitmap bitmap = BitmapFactory.decodeFile(picturePath);
			try {
				ExifInterface exif = new ExifInterface(picturePath);
				int orientation = exif.getAttributeInt(ExifInterface.TAG_ORIENTATION, 1);
				if (orientation == 6) {
					Matrix matrix = new Matrix();
					matrix.postRotate(90);
					bitmap = Bitmap.createBitmap(bitmap, 0, 0, bitmap.getWidth(), bitmap.getHeight(), matrix, true);
				}
			} catch (IOException e) {
				e.printStackTrace();
			}
			BitmapDrawable drawable = new BitmapDrawable(this.getResources(), bitmap);
			if (bitmap.getHeight() > 4096 || bitmap.getWidth() > 4096) {
				int nh = (int) (bitmap.getHeight() * (2048.0 / bitmap.getWidth()));
				Bitmap scaled = Bitmap.createScaledBitmap(bitmap, 2048, nh, true);
				drawable = new BitmapDrawable(this.getResources(), scaled);
			} else {
				drawable = new BitmapDrawable(this.getResources(), bitmap);
			}
			photoImage.setScaleType(ImageView.ScaleType.MATRIX);
			photoImage.setImageDrawable(drawable);
			img.setBackgroundColor(Color.TRANSPARENT);
		}
	}

	/**
	 * onClick method. Sends input in text fields to datastore.
	 */
	public void onSendButtonPressed(View view) {
		if (isEmpty(price) && isEmpty(merchant) && isEmpty(description) && isEmpty(comment)) {
			Toast.makeText(this, "Nothing to submit.", Toast.LENGTH_SHORT).show();
			return;
		}

		String acc = settings.getString("emailFormatted", "");
		if (settings.getBoolean("employee", false)) {
			acc = "Co_" + settings.getString("company", "").replaceAll(" ", "_");
		}
		CloudEntity expense = new CloudEntity("ERApp_" + acc);
		expense = addData(expense);

		toast = "Submitted";
		length = 0;
		CloudCallbackHandler<CloudEntity> handler = new CloudCallbackHandler<CloudEntity>() {
			@Override
			public void onComplete(final CloudEntity result) {
			}

			@Override
			public void onError(final IOException exception) {
				toast = "Unable to connect to server";
				length = 1;
			}
		};
		Bundle data = getIntent().getExtras();
		if (data.getBoolean("correct") && !incomplete) {
			mProcessingFragment.getCloudBackend().update(expense, handler);
		} else {
			mProcessingFragment.getCloudBackend().insert(expense, handler);
		}

		// return to previous activity
		finish();
		if (data.get("display").equals("correct")) {
			Intent intent = new Intent(this, ViewExpensesActivity.class);
			intent.putExtra("display", "correct");
			intent.putExtra("delay", true);
			intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
			startActivity(intent);
		}
		Handler toaster = new Handler();
		Runnable run = new Runnable() {
			@Override
			public void run() {
				Toast.makeText(ExpenseActivity.this, toast, length).show();
			}
		};
		toaster.postDelayed(run, 500);
	}

	private CloudEntity addData(CloudEntity expense) {
		// use selected CloudEntity if correcting
		Bundle data = getIntent().getExtras();
		expense.put("correctable", true);
		if (data.get("display").equals("correct")) {
			expense = data.getParcelable("expense");
		}
		List<Object> list = new ArrayList<Object>();
		incomplete = false;
		if (isEmpty(price)) {
			incomplete = true;
			list.add(-1);
			expense.put("price", -1);
		} else {
			expense.put("price", Double.parseDouble(price.getText().toString().replaceAll("[^\\d.]", "")));
			list.add(Double.parseDouble(price.getText().toString().replaceAll("[^\\d.]", "")));
		}
		if (isEmpty(merchant)) {
			incomplete = true;
			list.add("");
		} else {
			list.add(merchant.getText().toString());
		}
		if (isEmpty(description)) {
			incomplete = true;
			list.add("");
		} else {
			list.add(description.getText().toString());
		}
		if (isEmpty(date)) {
			incomplete = true;
			list.add("");
		} else {
			list.add(date.getText().toString());
		}
		if (isEmpty(comment)) {
			list.add("");
		} else {
			list.add(comment.getText().toString());
		}
		if (category.getSelectedItem().toString().equals("Category")) {
			incomplete = true;
		}
		list.add(currency.getSelectedItem().toString());
		list.add(currency.getSelectedItemPosition());
		list.add(payment.getSelectedItem().toString());
		list.add(payment.getSelectedItemPosition());
		list.add(category.getSelectedItem().toString());
		list.add(category.getSelectedItemPosition());
		expense.setCreatedBy(settings.getString("email", ""));
		expense.put("ex", list);
		expense.put("name", settings.getString("name", ""));
		if (incomplete) {
			expense.put("correctable", true);
		} else if (data.get("display").equals("correct")) {
			expense.put("correctable", false);
		}

		// save currency
		SharedPreferences.Editor editor = settings.edit();
		editor.putInt("index", currency.getSelectedItemPosition());
		editor.commit();

		return expense;
	}

	/**
	 * Sets existing data into fields if correcting or viewing an expense.
	 */
	private void setInputs() {
		Bundle data = getIntent().getExtras();
		if (data.get("price").toString().equals("-1.0")) {
			price.setText("");
		} else {
			DecimalFormat format = new DecimalFormat("#");
			format.setMinimumFractionDigits(2);
			double amount = Double.parseDouble(data.get("price").toString());
			price.setText(format.format(amount));
		}
		merchant.setText(data.get("merchant").toString());
		description.setText(data.get("description").toString());
		date.setText(data.get("date").toString());
		comment.setText(data.get("comment").toString());
		currency.setSelection((int) Double.parseDouble(data.get("currency").toString()));
		category.setSelection((int) Double.parseDouble(data.get("category").toString()));
		payment.setSelection((int) Double.parseDouble(data.get("payment").toString()));

		if (data.get("display").equals("view")) {
			setTitle("Expense");
			if (settings.getBoolean("employee", false)) {
				TextView name = (TextView) findViewById(R.id.view_name);
				name.setText(settings.getString("name", ""));
				name.setVisibility(View.VISIBLE);
				name.setPadding(0, 5, 0, 5);
			} else {
				TextView priceText = (TextView) findViewById(R.id.addExpense_price);
				priceText.setPadding(0, 15, 0, 0);
			}
			LinearLayout layout = (LinearLayout) findViewById(R.id.addExpense_imageSelect);
			layout.setVisibility(View.GONE);
			price.setFocusable(false);
			merchant.setFocusable(false);
			description.setFocusable(false);
			date.setOnClickListener(null);
			comment.setFocusable(false);
			currency.setClickable(false);
			category.setClickable(false);
			payment.setClickable(false);
			submit.setVisibility(View.GONE);
		}
	}

	private boolean isEmpty(EditText etText) {
		return etText.getText().toString().trim().length() == 0;
	}

	private void initiateFragments() {
		FragmentTransaction fragmentTransaction = mFragmentManager.beginTransaction();
		mProcessingFragment = (CloudBackendFragment) mFragmentManager.findFragmentByTag(PROCESSING_FRAGMENT_TAG);
		if (mProcessingFragment == null) {
			mProcessingFragment = new CloudBackendFragment();
			mProcessingFragment.setRetainInstance(true);
			fragmentTransaction.add(mProcessingFragment, PROCESSING_FRAGMENT_TAG);
		}
		fragmentTransaction.commit();
	}

	private File getOutputPhotoFile() {
		File directory = new File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES), getPackageName());
		if (!directory.exists()) {
			if (!directory.mkdirs()) {
				Log.e(TAG, "Failed to create storage directory.");
				return null;
			}
		}
		String timeStamp = new SimpleDateFormat("yyyMMdd_HHmmss", Locale.US).format(new Date());
		return new File(directory.getPath() + File.separator + "IMG_" + timeStamp + ".jpg");
	}

	private void showPhoto(String photoUri) {
		File imageFile = new File(photoUri);
		if (imageFile.exists()) {
			Bitmap bitmap = BitmapFactory.decodeFile(imageFile.getAbsolutePath());
			try {
				ExifInterface exif = new ExifInterface(photoUri);
				int orientation = exif.getAttributeInt(ExifInterface.TAG_ORIENTATION, 1);
				if (orientation == 6) {
					Matrix matrix = new Matrix();
					matrix.postRotate(90);
					bitmap = Bitmap.createBitmap(bitmap, 0, 0, bitmap.getWidth(), bitmap.getHeight(), matrix, true);
				}
			} catch (IOException e) {
				e.printStackTrace();
			}
			BitmapDrawable drawable = new BitmapDrawable(this.getResources(), bitmap);
			if (bitmap.getHeight() > 4096 || bitmap.getWidth() > 4096) {
				int nh = (int) (bitmap.getHeight() * (2048.0 / bitmap.getWidth()));
				Bitmap scaled = Bitmap.createScaledBitmap(bitmap, 2048, nh, true);
				drawable = new BitmapDrawable(this.getResources(), scaled);
			} else {
				drawable = new BitmapDrawable(this.getResources(), bitmap);
			}
			photoImage.setScaleType(ImageView.ScaleType.MATRIX);
			photoImage.setImageDrawable(drawable);
			img.setBackgroundColor(Color.TRANSPARENT);
		}
	}

	/**
	 * Method called via OnListener in {@link CloudBackendFragment}.
	 */
	@Override
	public void onCreateFinished() {
	}

	/**
	 * Method called via OnListener in {@link CloudBackendFragment}.
	 */
	@Override
	public void onBroadcastMessageReceived(List<CloudEntity> l) {
	}
}
