package com.team5.erapp;

import java.io.IOException;
import java.io.File;
import java.net.URL;
import java.text.DecimalFormat;
import java.text.SimpleDateFormat;
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
import android.app.FragmentManager;
import android.app.FragmentTransaction;
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
import android.provider.MediaStore;
import android.util.Log;
import android.view.View;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.Spinner;
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
	private String imagePath;
	
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
		setContentView(R.layout.activity_add_expenses);
		this.setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);
		this.getWindow().setSoftInputMode(
				WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_HIDDEN);

		submit = (Button) findViewById(R.id.button_submit);
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
		settings = getSharedPreferences(PREFS_NAME, 0);

		currency.setSelection(settings.getInt("index", 7));
		img.setBackgroundColor(Color.GRAY);

		final Calendar c = Calendar.getInstance();
		int yy = c.get(Calendar.YEAR);
		int mm = c.get(Calendar.MONTH);
		int dd = c.get(Calendar.DAY_OF_MONTH);

		date.setText(new StringBuilder().append(mm + 1).append("/").append(dd)
				.append("/").append(yy));

		submit.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				onSendButtonPressed(v);
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
				Intent i = new Intent(
						Intent.ACTION_PICK,
						android.provider.MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
				startActivityForResult(i, SELECT_IMAGE);
			}
		});

		Bundle data = getIntent().getExtras();
		if (data.getBoolean("correct")) {
			setTitle("Correct Expense");
			setInputs();
		}
		initiateFragments();
	}

	protected void onActivityResult(int requestCode, int resultCode, Intent data) {
		if (requestCode == CAPTURE_IMAGE_ACTIVITY_REQ) {
			if (resultCode == RESULT_OK) {
				Uri photoUri = null;
				if (data == null) {
					Toast.makeText(this, "Image saved successfully",
							Toast.LENGTH_LONG).show();
					photoUri = fileUri;
				} else {
					photoUri = data.getData();
				}
				imagePath = photoUri.getPath();
				showPhoto(photoUri.getPath());
			} else if (resultCode == RESULT_CANCELED) {
				Toast.makeText(this, "Cancelled", Toast.LENGTH_SHORT).show();
			}
		}
		if (requestCode == SELECT_IMAGE && resultCode == RESULT_OK
				&& null != data) {
			fileUri = data.getData();
			String[] filePathColumn = { MediaStore.Images.Media.DATA };
			Cursor cursor = getContentResolver().query(fileUri, filePathColumn,
					null, null, null);
			cursor.moveToFirst();
			int columnIndex = cursor.getColumnIndex(filePathColumn[0]);
			String picturePath = cursor.getString(columnIndex);
			cursor.close();
			Bitmap bitmap = BitmapFactory.decodeFile(picturePath);
			try {
				ExifInterface exif = new ExifInterface(picturePath);
				int orientation = exif.getAttributeInt(
						ExifInterface.TAG_ORIENTATION, 1);
				if (orientation == 6) {
					Matrix matrix = new Matrix();
					matrix.postRotate(90);
					bitmap = Bitmap
							.createBitmap(bitmap, 0, 0, bitmap.getWidth(),
									bitmap.getHeight(), matrix, true);
				}
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			BitmapDrawable drawable = new BitmapDrawable(this.getResources(),
					bitmap);
			if (bitmap.getHeight() > 4096 || bitmap.getWidth() > 4096) {
				int nh = (int) (bitmap.getHeight() * (2048.0 / bitmap
						.getWidth()));
				Bitmap scaled = Bitmap.createScaledBitmap(bitmap, 2048, nh,
						true);
				drawable = new BitmapDrawable(this.getResources(), scaled);
			} else {
				drawable = new BitmapDrawable(this.getResources(), bitmap);
			}
			photoImage.setScaleType(ImageView.ScaleType.MATRIX);
			photoImage.setImageDrawable(drawable);
			img.setBackgroundColor(Color.TRANSPARENT);
		} else if (resultCode == RESULT_CANCELED) {
			Toast.makeText(this, "Cancelled", Toast.LENGTH_SHORT).show();
		}
	}

	private File getOutputPhotoFile() {
		File directory = new File(
				Environment
						.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES),
				getPackageName());
		if (!directory.exists()) {
			if (!directory.mkdirs()) {
				Log.e(TAG, "Failed to create storage directory.");
				return null;
			}
		}
		String timeStamp = new SimpleDateFormat("yyyMMdd_HHmmss", Locale.US)
				.format(new Date());
		return new File(directory.getPath() + File.separator + "IMG_"
				+ timeStamp + ".jpg");
	}

	private void showPhoto(String photoUri) {
		File imageFile = new File(photoUri);
		if (imageFile.exists()) {
			Bitmap bitmap = BitmapFactory.decodeFile(imageFile
					.getAbsolutePath());
			try {
				ExifInterface exif = new ExifInterface(photoUri);
				int orientation = exif.getAttributeInt(
						ExifInterface.TAG_ORIENTATION, 1);
				if (orientation == 6) {
					Matrix matrix = new Matrix();
					matrix.postRotate(90);
					bitmap = Bitmap
							.createBitmap(bitmap, 0, 0, bitmap.getWidth(),
									bitmap.getHeight(), matrix, true);
				}
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			BitmapDrawable drawable = new BitmapDrawable(this.getResources(),
					bitmap);
			if (bitmap.getHeight() > 4096 || bitmap.getWidth() > 4096) {
				int nh = (int) (bitmap.getHeight() * (2048.0 / bitmap
						.getWidth()));
				Bitmap scaled = Bitmap.createScaledBitmap(bitmap, 2048, nh,
						true);
				drawable = new BitmapDrawable(this.getResources(), scaled);
			} else {
				drawable = new BitmapDrawable(this.getResources(), bitmap);
			}

			photoImage.setScaleType(ImageView.ScaleType.MATRIX);
			photoImage.setImageDrawable(drawable);
			img.setBackgroundColor(Color.TRANSPARENT);
		}
	}

	private void initiateFragments() {
		FragmentTransaction fragmentTransaction = mFragmentManager
				.beginTransaction();
		mProcessingFragment = (CloudBackendFragment) mFragmentManager
				.findFragmentByTag(PROCESSING_FRAGMENT_TAG);
		if (mProcessingFragment == null) {
			mProcessingFragment = new CloudBackendFragment();
			mProcessingFragment.setRetainInstance(true);
			fragmentTransaction.add(mProcessingFragment,
					PROCESSING_FRAGMENT_TAG);
		}
		fragmentTransaction.commit();
	}

	/**
	 * onClick method. Sends input in text fields to datastore.
	 */
	public void onSendButtonPressed(View view) {
		if (price.getText().toString().isEmpty()
				&& merchant.getText().toString().isEmpty()
				&& description.getText().toString().isEmpty()
				&& comment.getText().toString().isEmpty()) {
			Toast.makeText(this, "Nothing to submit", Toast.LENGTH_SHORT)
					.show();
			return;
		}

		// change CloudEntity Object to include user's name/email or company's
		// name/email
		CloudEntity expense = new CloudEntity("ERApp");

		// use selected CloudEntity if correcting
		Bundle data = getIntent().getExtras();
		if (data.getBoolean("correct") == true) {
			expense = data.getParcelable("expense");
		}

		Boolean incomplete = false;
		if (price.getText().toString().isEmpty()) {
			incomplete = true;
			expense.put("price", -1);
		} else {
			expense.put("price", Double.parseDouble(price.getText().toString()));
		}
		if (merchant.getText().toString().isEmpty()) {
			incomplete = true;
			expense.put("merchant", "");
		} else {
			expense.put("merchant", merchant.getText().toString());
		}
		if (description.getText().toString().isEmpty()) {
			incomplete = true;
			expense.put("description", "");
		} else {
			expense.put("description", description.getText().toString());
		}
		if (date.getText().toString().isEmpty()) {
			incomplete = true;
			expense.put("date", "");
		} else {
			expense.put("date", date.getText().toString());
		}
		if (category.getSelectedItem().toString().equals("Category")) {
			incomplete = true;
		}
		expense.put("incomplete", incomplete);
		expense.setCreatedBy("Name");
		expense.setUpdatedBy("Name");
		expense.put("comment", comment.getText().toString());
		expense.put("currency", currency.getSelectedItem().toString());
		expense.put("currencyPos", currency.getSelectedItemPosition());
		expense.put("payment", payment.getSelectedItem().toString());
		expense.put("paymentPos", payment.getSelectedItemPosition());
		expense.put("category", category.getSelectedItem().toString());
		expense.put("categoryPos", category.getSelectedItemPosition());

		// save currency
		SharedPreferences.Editor editor = settings.edit();
		editor.putInt("index", currency.getSelectedItemPosition());
		editor.commit();
		
		CloudCallbackHandler<CloudEntity> handler = new CloudCallbackHandler<CloudEntity>() {
			@Override
			public void onComplete(final CloudEntity result) {
			}

			@Override
			public void onError(final IOException exception) {
				handleEndpointException(exception);
			}
		};

		// insert or update cloud entity
		// if (data.getBoolean("correct") == true && !incomplete) {
		// mProcessingFragment.getCloudBackend().update(expense, handler);
		// } else if (data.getBoolean("correct") == true && incomplete) {
		// Toast.makeText(this, "Please complete all entries",
		// Toast.LENGTH_LONG).show();
		// return;
		// }
		if (data.getBoolean("correct") == true && !incomplete) {
			mProcessingFragment.getCloudBackend().update(expense, handler);
		} else {
			mProcessingFragment.getCloudBackend().insert(expense, handler);
		}

		// return to HomeActivity
		Intent intent = new Intent(this, HomeActivity.class);
		intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK
				| Intent.FLAG_ACTIVITY_NEW_TASK);
		startActivity(intent);
		Toast.makeText(this, "Submitted", Toast.LENGTH_SHORT).show();
	}

	private void handleEndpointException(IOException e) {
		Toast.makeText(this, e.toString(), Toast.LENGTH_LONG).show();
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

	/**
	 * Sets existing data into fields if correcting an expense.
	 */
	public void setInputs() {
		Bundle data = getIntent().getExtras();
		if (data.get("price").toString().equals("-1")) {
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
		currency.setSelection(data.getInt("currency"));
		category.setSelection(data.getInt("category"));
		payment.setSelection(data.getInt("payment"));
	}
}
