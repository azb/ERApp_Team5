package com.team5.erapp;

import java.io.IOException;
import java.io.File;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Locale;

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
import android.graphics.drawable.BitmapDrawable;
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

public class AddExpenseActivity extends Activity implements OnListener {

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
			photoImage.setScaleType(ImageView.ScaleType.MATRIX);
			photoImage.setImageBitmap(BitmapFactory.decodeFile(picturePath));
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
			BitmapDrawable drawable = new BitmapDrawable(this.getResources(),
					bitmap);
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
	 * onClick method.
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
		expense.put("incomplete", false);
		expense.setOwner("Name");
		expense.setCreatedBy("Name");
		expense.setUpdatedBy("Name");
		expense.put("price", price.getText().toString());
		expense.put("merchant", merchant.getText().toString());
		expense.put("description", description.getText().toString());
		expense.put("date", date.getText().toString());
		expense.put("comment", comment.getText().toString());
		expense.put("currency", currency.getSelectedItem().toString());
		expense.put("currencyPos", currency.getSelectedItemPosition());
		expense.put("payment", payment.getSelectedItem().toString());
		expense.put("paymentPos", payment.getSelectedItemPosition());
		expense.put("category", category.getSelectedItem().toString());
		expense.put("categoryPos", category.getSelectedItemPosition());		
		if (price.getText().toString().isEmpty()) {
			expense.put("incomplete", true);
			expense.put("price", "");
		}
		if (merchant.getText().toString().isEmpty()) {
			expense.put("incomplete", true);
			expense.put("merchant", "");
		}
		if (description.getText().toString().isEmpty()) {
			expense.put("incomplete", true);
			expense.put("description", "");
		}
		if (date.getText().toString().isEmpty()) {
			expense.put("incomplete", true);
			expense.put("date", "");
		}
		if (comment.getText().toString().isEmpty()) {
			expense.put("incomplete", true);
			expense.put("comment", "");
		}
		if (category.getSelectedItem().toString().equals("Category")) {
			expense.put("incomplete", true);
		}
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

		mProcessingFragment.getCloudBackend().insert(expense, handler);

		Intent intent = new Intent(this, HomeActivity.class);
		intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK|Intent.FLAG_ACTIVITY_NEW_TASK);
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
}
