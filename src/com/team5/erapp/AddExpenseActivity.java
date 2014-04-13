package com.team5.erapp;

import java.io.IOException;
import java.io.File;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

import com.google.cloud.backend.core.CloudBackendFragment;
import com.google.cloud.backend.core.CloudBackendFragment.OnListener;
import com.google.cloud.backend.core.CloudCallbackHandler;
import com.google.cloud.backend.core.CloudEntity;
import com.google.cloud.backend.core.CloudQuery.Order;
import com.google.cloud.backend.core.CloudQuery.Scope;
import com.google.cloud.backend.core.Consts;
import com.team5.erapp.R;

import android.animation.Animator;
import android.annotation.SuppressLint;
import android.annotation.TargetApi;
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
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.provider.MediaStore;
import android.support.v4.app.NavUtils;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

public class AddExpenseActivity extends Activity {

	private TouchImageView photoImage = null;
	private EditText price;
	private EditText merchant;
	private EditText description;
	private EditText date;
	private EditText comments;
	private Spinner currency;
	private Spinner category;
	private Spinner payment;
	private Uri fileUri = null;
	private LinearLayout img;
	private Button submit;
	
    private CloudBackendFragment mProcessingFragment;
    private FragmentManager mFragmentManager;
	
	private static final String TAG = "CallCamera";
	private static final int CAPTURE_IMAGE_ACTIVITY_REQ = 0;
	private static final int SELECT_IMAGE = 1;
	private static final int SUBMIT = 2;
	public static final String PREFS_NAME = "MyPrefsFile";
	private static final String PROCESSING_FRAGMENT_TAG = "BACKEND_FRAGMENT";

	@SuppressLint("NewApi")
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_add_expenses);
		this.setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);
		this.getWindow().setSoftInputMode(
				WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_HIDDEN);
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
		submit = (Button) findViewById(R.id.button_save);
		submit.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				onSendButtonPressed(v);
			}
		});

		//initialize variables
		photoImage = (TouchImageView) findViewById(R.id.imageView1);
		price = (EditText) findViewById(R.id.addExpensePrice);
		merchant = (EditText) findViewById(R.id.addExpenseMerchant);
		description = (EditText) findViewById(R.id.addExpenseDescription);
		date = (EditText) findViewById(R.id.addExpenseDate);
		comments = (EditText) findViewById(R.id.addExpenseComments);
		currency = (Spinner) findViewById(R.id.addExpenseCurrency);
		currency.setSelection(7);
		category = (Spinner) findViewById(R.id.addExpenseCategory);
		payment = (Spinner) findViewById(R.id.addExpensePayment);
		img = (LinearLayout) findViewById(R.id.AddExpensesImageBackground);
		img.setBackgroundColor(Color.GRAY);
		mFragmentManager = getFragmentManager();
		
		initiateFragments();
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		getMenuInflater().inflate(R.menu.main, menu);
		return true;
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		switch (item.getItemId()) {
			case R.id.action_logout:
				SharedPreferences settings = getSharedPreferences(PREFS_NAME, 0);
				SharedPreferences.Editor editor = settings.edit();
				editor.putBoolean("logged", false);
				editor.commit();
				Intent intent = new Intent(this, LoginActivity.class);
				startActivity(intent);
				return true;
			default:
				return super.onOptionsItemSelected(item);
		}
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
			Uri selectedImage = data.getData();
			String[] filePathColumn = { MediaStore.Images.Media.DATA };
			Cursor cursor = getContentResolver().query(selectedImage,
					filePathColumn, null, null, null);
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
//        fragmentTransaction.commit();
    }
	
	/**
     * onClick method.
     */
    public void onSendButtonPressed(View view) {

        // create a CloudEntity with the new post
        CloudEntity newPost = new CloudEntity("Guestbook");
        newPost.put("message", price.getText().toString());

        // create a response handler that will receive the result or an error
        CloudCallbackHandler<CloudEntity> handler = new CloudCallbackHandler<CloudEntity>() {
            @Override
            public void onComplete(final CloudEntity result) {
                //mPosts.add(0, result);
                //updateGuestbookView();
            }

            @Override
            public void onError(final IOException exception) {
                handleEndpointException(exception);
            }
        };

        // execute the insertion with the handler
        mProcessingFragment.getCloudBackend().insert(newPost, handler);
    }
    
    private void handleEndpointException(IOException e) {
        Toast.makeText(this, e.toString(), Toast.LENGTH_LONG).show();
    }
    
}
