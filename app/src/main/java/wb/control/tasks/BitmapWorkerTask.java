package wb.control.tasks;

import java.lang.ref.WeakReference;

import wb.control.Basis;
import android.app.Activity;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.AsyncTask;
import android.util.DisplayMetrics;
import android.widget.ImageView;

public class BitmapWorkerTask extends AsyncTask<String, Void, Bitmap> {
	private final WeakReference<ImageView> imageViewReference;
	private String picpath;
	private String lokname;
	private Activity act;
	private int ivwidth, ivheight;

	public BitmapWorkerTask(ImageView imageView, Activity a) {
		// Use a WeakReference to ensure the ImageView can be garbage collected
		imageViewReference = new WeakReference<ImageView>(imageView);
		lokname = (String)imageView.getTag();
		act = a;

		// funkt nicht - Größe ist noch 0
		//ivwidth = imageView.getWidth();
		//ivheight = imageView.getHeight();
		DisplayMetrics metrics = new DisplayMetrics();
		act.getWindowManager().getDefaultDisplay().getMetrics(metrics);
		ivwidth = (int)(200 * (metrics.densityDpi / 160));
		ivheight = (int)(ivwidth / 1.83);
	}

	// Decode image in background.
	@Override
	protected Bitmap doInBackground(String... params) {
		picpath = params[0];
		
		final Bitmap bitmap = decodeSampledBitmapFromPath(picpath, ivwidth, ivheight);
        Basis.addBitmapToMemoryCache(picpath, bitmap);
        return bitmap;
	}


	private int calculateInSampleSize(BitmapFactory.Options options, int reqWidth, int reqHeight) {
		// Raw height and width of image
		final int height = options.outHeight;
		final int width = options.outWidth;
		int inSampleSize = 1;

		if (height > reqHeight || width > reqWidth) {

			final int halfHeight = height / 2;
			final int halfWidth = width / 2;

			// Calculate the largest inSampleSize value that is a power of 2 and keeps both
			// height and width larger than the requested height and width.
			while ((halfHeight / inSampleSize) > reqHeight
					&& (halfWidth / inSampleSize) > reqWidth) {
				inSampleSize *= 2;
			}
		}
		return inSampleSize;
	}

	private Bitmap decodeSampledBitmapFromPath(String path, int reqWidth, int reqHeight) {

		// First decode with inJustDecodeBounds=true to check dimensions
		final BitmapFactory.Options options = new BitmapFactory.Options();
		options.inJustDecodeBounds = true;
		BitmapFactory.decodeFile(path, options);

		// Calculate inSampleSize
		options.inSampleSize = calculateInSampleSize(options, reqWidth, reqHeight);

		// Decode bitmap with inSampleSize set
		options.inJustDecodeBounds = false;
		return BitmapFactory.decodeFile(path, options);
	}


	// Once complete, see if ImageView is still around and set bitmap.
	@Override
	protected void onPostExecute(Bitmap bitmap) {
		if (imageViewReference != null && bitmap != null) {
			final ImageView imageView = imageViewReference.get();
			if (imageView != null) 
			{
				if (imageView.getTag() != null)
				{
					// checken, ob der imageView immer noch für die ursprüngliche Lok verwendet wird (oder ob er beeits für eine andere lok recyclet wurde)
					if (lokname.equals(imageView.getTag())) { imageView.setImageBitmap(bitmap); }
				}
				
			}
		}
	}
}	// end BitmapWorkerTask
