package com.zing.imagetest;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.OutputStream;

import org.libjpegturbo.turbojpeg.TJ;
import org.libjpegturbo.turbojpeg.TJCompressor;
import org.libjpegturbo.turbojpeg.TJDecompressor;
import org.libjpegturbo.turbojpeg.TJScalingFactor;

import android.app.Activity;
import android.app.ProgressDialog;
import android.content.DialogInterface;
import android.content.DialogInterface.OnCancelListener;
import android.content.Intent;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.Bitmap.CompressFormat;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Matrix;
import android.graphics.Rect;
import android.media.ExifInterface;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.provider.MediaStore;
import android.provider.MediaStore.Images;
import android.text.TextUtils;

public class ImageTest extends Activity {
	private final static String sampName[] = { "4:4:4", "4:2:2", "4:2:0",
			"Grayscale", "4:4:0" };

	private static final String TAG = ImageTest.class.getSimpleName();
	private static final String mImageUri = Images.Media.getContentUri(
			"external").toString();
	private ProgressDialog getPhotoDialog;
	private Uri localUri;
	private String mZingCacheFolderName;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		 setContentView(R.layout.activity_image_test);

//		TextView tv = new TextView(this);
//		tv.setText(stringFromJNI1());
//		setContentView(tv);

		handleSendIntent(getIntent());
		mZingCacheFolderName = getCacheExternalStorageDirectory();
		getPhotoDialog = new ProgressDialog(this);
		getPhotoDialog.setCancelable(false);
		getPhotoDialog.setCanceledOnTouchOutside(false);
		getPhotoDialog.setMessage("Loading...");
		getPhotoDialog.setProgressStyle(ProgressDialog.STYLE_SPINNER);
		getPhotoDialog.setOnCancelListener(new OnCancelListener() {
			@Override
			public void onCancel(DialogInterface dialog) {
				if (getPhotoFromStream != null)
					getPhotoFromStream.interrupt();
			}
		});
		if (localUri != null && localUri.toString().length() > 0) {
			downloadFromUri(localUri);
		}
	}

	public native String stringFromJNI1();

	public native String stringFromJNI2();

	private boolean handleSendIntent(Intent intent) {
		Bundle extras = intent.getExtras();
		if (extras == null) {
			return false;
		}

		final String mimeType = intent.getType();// text/plain -
		String action = intent.getAction();
		if (Intent.ACTION_SEND.equals(action)) {
			if (extras.containsKey(Intent.EXTRA_STREAM)) {
				Uri uri = (Uri) extras.getParcelable(Intent.EXTRA_STREAM);
				addAttachment(mimeType, uri, false);
				return true;
			}
		}

		return false;
	}

	private Thread getPhotoFromStream = null;

	private void downloadFromUri(final Uri uri) {
		try {

			if (getPhotoDialog != null && !this.isFinishing())
				getPhotoDialog.show();

			if (getPhotoFromStream != null)
				getPhotoFromStream.interrupt();

			getPhotoFromStream = new Thread(new TestRotateJPEG(uri));

			getPhotoFromStream.start();
		} catch (Exception e) {
			e.printStackTrace();
			finish();
		}
	}

	private class TestTurboCompressJPEG implements Runnable {
		private Uri uriImage;

		public TestTurboCompressJPEG(Uri uriImage) {
			this.uriImage = uriImage;
		}

		@Override
		public void run() {
			try {
				String fileName = uriImage.getLastPathSegment();

				BitmapFactory.Options options = new BitmapFactory.Options();
				options.inJustDecodeBounds = true;
				InputStream is = getContentResolver().openInputStream(uriImage);
				BitmapFactory.decodeStream(is, null, options);
				is.close();
				is = getContentResolver().openInputStream(uriImage);

				byte[] buff = new byte[1024];
				ByteArrayOutputStream arrayOutputStream = new ByteArrayOutputStream();
				int i = 0;
				while ((i = is.read(buff)) > 0) {
					arrayOutputStream.write(buff, 0, i);
				}
				is.close();
				
				TJScalingFactor[] sf = TJ.getScalingFactors();
				
				TJScalingFactor scaleFactor = new TJScalingFactor(1, 2);
				
				

				TJDecompressor decompressor = new TJDecompressor(
						arrayOutputStream.toByteArray());

				int width = decompressor.getWidth();
				int height = decompressor.getHeight();
				
		        width = scaleFactor.getScaled(width);
		        height = scaleFactor.getScaled(height);
		        
				int inSubsamp = decompressor.getSubsamp();
				System.out.println("Source Image: " + width + " x " + height
						+ " pixels, " + sampName[inSubsamp] + " subsampling");

				byte[] bmpBuf = decompressor.decompress(width, 0, height, TJ.PF_BGRX, 0);
				decompressor.close();
				
				TJCompressor tjc = new TJCompressor();
		        int jpegSize;
		        byte[] jpegBuf;

		        tjc.setSubsamp(inSubsamp);
		        tjc.setJPEGQuality(80);
		          tjc.setSourceImage(bmpBuf, width, 0, height, TJ.PF_BGRX);
		          jpegBuf = tjc.compress(0);
		        jpegSize = tjc.getCompressedSize();
		        tjc.close();

				File file = new File(mZingCacheFolderName, fileName + ".jpg");
				OutputStream fOut = new FileOutputStream(file);
				fOut.write(jpegBuf, 0, jpegSize);
				fOut.close();
				
				is = getContentResolver().openInputStream(uriImage);
				options.inJustDecodeBounds = false;
				options.inSampleSize = 2;
				Bitmap bitmap = BitmapFactory.decodeStream(is, null, options);
				file = new File(mZingCacheFolderName, fileName + "_a.jpg");
				fOut = new FileOutputStream(file);
				bitmap.compress(CompressFormat.JPEG, 80, fOut);
				fOut.flush();
				fOut.close();

//				TJCompressor compressor = new TJCompressor(
//						arrayOutputStream.toByteArray(), options.outWidth, 0,
//						options.outHeight,
//						TJCompressor.getPixelFormat(Config.ARGB_8888));
//				compressor.setJPEGQuality(50);
//				compressor.setSubsamp(TJ.SAMP_GRAY);
//				byte[] outImage = compressor.compress(TJ.FLAG_FASTUPSAMPLE);
//				File file = new File(mZingCacheFolderName, fileName + ".jpg");
//				OutputStream fOut = new FileOutputStream(file);
//				fOut.write(outImage);
//				fOut.flush();
//				fOut.close();
				arrayOutputStream.close();
			} catch (Exception e) {
				e.printStackTrace();
			} finally {
				ImageTest.this.runOnUiThread(new Runnable() {
					@Override
					public void run() {
						if (getPhotoDialog != null
								&& !ImageTest.this.isFinishing()) {
							getPhotoDialog.dismiss();
							finish();
						}
					}
				});
			}
		}
	}

	private class TestCompressJPEG implements Runnable {
		private Uri uriImage;

		public TestCompressJPEG(Uri uriImage) {
			this.uriImage = uriImage;
		}

		@Override
		public void run() {
			try {
				String fileName = uriImage.getLastPathSegment();

				BitmapFactory.Options options = new BitmapFactory.Options();
				final InputStream is = getContentResolver().openInputStream(
						uriImage);
				Bitmap bitmap = BitmapFactory.decodeStream(is, null, options);
				is.close();
				ByteArrayOutputStream bOut = new ByteArrayOutputStream();
				bitmap.compress(Bitmap.CompressFormat.JPEG, 100, bOut);
				bitmap.recycle();
				for (int i = 0; i < 10; i++) {
					File file = new File(mZingCacheFolderName, fileName
							+ String.format("_%2d", i) + ".jpg");
					OutputStream fOut = new FileOutputStream(file);

					bOut.writeTo(fOut);
					fOut.flush();
					fOut.close();

					byte[] byteArr = bOut.toByteArray();
					bitmap = BitmapFactory.decodeByteArray(byteArr, 0,
							byteArr.length);					
					{
						Bitmap croppedImage = Bitmap.createBitmap(bitmap.getWidth(), bitmap.getHeight(), Bitmap.Config.RGB_565);
						Canvas canvas = new Canvas(croppedImage);
						Rect dstRect = new Rect(0, 0, bitmap.getWidth(), bitmap.getHeight());
						canvas.drawBitmap(bitmap, dstRect, dstRect, null);
						bitmap.recycle();
						bitmap = croppedImage;
					}					
					bitmap.compress(Bitmap.CompressFormat.JPEG, 80, bOut);
				}
			} catch (Exception e) {
				e.printStackTrace();
			} finally {
				ImageTest.this.runOnUiThread(new Runnable() {
					@Override
					public void run() {
						if (getPhotoDialog != null
								&& !ImageTest.this.isFinishing()) {
							getPhotoDialog.dismiss();
							finish();
						}
					}
				});
			}
		}
	}
	
	public static int getExifRotation(String imgPath) {
		try {
			ExifInterface exif = new ExifInterface(imgPath);
			String rotationAmount = exif
					.getAttribute(ExifInterface.TAG_ORIENTATION);
			if (!TextUtils.isEmpty(rotationAmount)) {
				int rotationParam = Integer.parseInt(rotationAmount);
				switch (rotationParam) {
				case ExifInterface.ORIENTATION_NORMAL:
					return 0;
				case ExifInterface.ORIENTATION_ROTATE_90:
					return 90;
				case ExifInterface.ORIENTATION_ROTATE_180:
					return 180;
				case ExifInterface.ORIENTATION_ROTATE_270:
					return 270;
				default:
					return 0;
				}
			} else {
				return 0;
			}
		} catch (Exception ex) {
			return 0;
		}
	}
	
	private class TestRotateJPEG implements Runnable {
		private Uri uriImage;

		public TestRotateJPEG(Uri uriImage) {
			this.uriImage = uriImage;
		}

		@Override
		public void run() {
			try {

				String fileName = uriImage.getLastPathSegment();

				BitmapFactory.Options options = new BitmapFactory.Options();
				options.inSampleSize = 2; 
				final InputStream is = getContentResolver().openInputStream(
						uriImage);
				Bitmap bitmap = BitmapFactory.decodeStream(is, null, options);
				is.close();
				File file = new File(mZingCacheFolderName, fileName
						+ String.format("_%2d", 0) + ".jpg");
				OutputStream fOut = new FileOutputStream(file);
				
				bitmap.compress(Bitmap.CompressFormat.JPEG, 80, fOut);				
				fOut.close();
				
				Matrix m = new Matrix();
				int rotation = getExifRotation(getRealPathFromURI(uriImage));
				if (rotation != 0) {
					m.postRotate(rotation);
					Bitmap bitmapNew =  Bitmap.createBitmap(bitmap, 0, 0, bitmap.getWidth(),
							bitmap.getHeight(), m, true);
					bitmap.recycle();
					
					File fileN = new File(mZingCacheFolderName, fileName
							+ String.format("_%2d", 1) + ".jpg");
					OutputStream fOutN = new FileOutputStream(fileN);
					bitmapNew.compress(Bitmap.CompressFormat.JPEG, 80, fOutN);
					fOutN.close();
				}
				
			} catch (Exception e) {
				e.printStackTrace();
			} finally {
				ImageTest.this.runOnUiThread(new Runnable() {
					@Override
					public void run() {
						if (getPhotoDialog != null
								&& !ImageTest.this.isFinishing()) {
							getPhotoDialog.dismiss();
							finish();
						}
					}
				});
			}
		}
	}
	
	public String getRealPathFromURI(Uri contentUri) {
        String[] proj = { MediaStore.Images.Media.DATA };
        Cursor cursor = managedQuery(contentUri, proj, null, null, null);
        int column_index = cursor.getColumnIndexOrThrow(MediaStore.Images.Media.DATA);
        cursor.moveToFirst();
        return cursor.getString(column_index);
    }

	private void addAttachment(String type, Uri uri, boolean append) {
		if (uri != null) {
			// When we're handling Intent.ACTION_SEND_MULTIPLE, the passed in
			// items can be
			// videos, and/or images, and/or some other unknown types we don't
			// handle. When
			// a single attachment is "shared" the type will specify an image or
			// video. When
			// there are multiple types, the type passed in is "*/*". In that
			// case, we've got
			// to look at the uri to figure out if it is an image or video.
			boolean wildcard = "*/*".equals(type);
			if (type.startsWith("image/")
					|| (wildcard && uri.toString().startsWith(mImageUri))) {
				localUri = uri;
			}
			;
		}
	}

	public static String getCacheExternalStorageDirectory() {
		String zaloFolder = "/imagetest/";
		String extStorageDirectory = Environment.getExternalStorageDirectory()
				.toString();
		File myNewFolder = new File(extStorageDirectory + zaloFolder);
		myNewFolder.mkdir();
		return extStorageDirectory + zaloFolder;
	}

//	static {
//		System.loadLibrary("turbo-jpeg");
//	}
}
