package wb.control.views;


import wb.control.R;
import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.Rect;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.ClipDrawable;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.LayerDrawable;
import android.graphics.drawable.ShapeDrawable;
import android.graphics.drawable.StateListDrawable;
import android.graphics.drawable.shapes.RoundRectShape;
import android.graphics.drawable.shapes.Shape;
import android.util.AttributeSet;
import android.view.Gravity;
import android.view.View;
import android.view.ViewDebug;
import android.view.ViewParent;
import android.widget.RemoteViews.RemoteView;
import android.os.Parcel;
import android.os.Parcelable;




@RemoteView
public class VerticalProgressBar extends View {
    private static final int MAX_LEVEL = 10000;

    int mMinWidth;
    int mMaxWidth;
    int mMinHeight;
    int mMaxHeight;
    
    // meine Änderungen für Textanzeige
    private String speedText;  
    private int tsProgress;	// trainspeed für roten Balken, statt secondaryProgress verwenden!!!
    private String speedmode1txt;
    private Paint tsPaint, rampPaint, rampTextPaint, speedTextPaint;
    private Path rampPath;
    private Rect speedtxtBounds;

    private int mProgress;
    private int mSecondaryProgress;
    private int mMax;

    private Drawable mProgressDrawable;
    private Drawable mCurrentDrawable;
    Bitmap mSampleTile;
    private boolean mNoInvalidate;
    private RefreshProgressRunnable mRefreshProgressRunnable;
    private long mUiThreadId;

    private boolean mInDrawing;

    protected int mScrollX;
	protected int mScrollY;
	protected int mPaddingLeft;
	protected int mPaddingRight;
	protected int mPaddingTop;
	protected int mPaddingBottom;
	protected ViewParent mParent;

    /**
     * Create a new progress bar with range 0...100 and initial progress of 0.
     * @param context the application environment
     */
    public VerticalProgressBar(Context context) {
    	this(context, null);
    }

    public VerticalProgressBar(Context context, AttributeSet attrs) {
        this(context, attrs, android.R.attr.progressBarStyle);
    }

    public VerticalProgressBar(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
        mUiThreadId = Thread.currentThread().getId();
        initProgressBar();

        TypedArray a =
            context.obtainStyledAttributes(attrs, R.styleable.ProgressBar, defStyle, 0);

        mNoInvalidate = true;

        Drawable drawable = a.getDrawable(R.styleable.ProgressBar_android_progressDrawable);
        if (drawable != null) {
            drawable = tileify(drawable, false);
            // Calling this method can set mMaxHeight, make sure the corresponding
            // XML attribute for mMaxHeight is read after calling this method
            setProgressDrawable(drawable);
        }


        mMinWidth = a.getDimensionPixelSize(R.styleable.ProgressBar_android_minWidth, mMinWidth);
        mMaxWidth = a.getDimensionPixelSize(R.styleable.ProgressBar_android_maxWidth, mMaxWidth);
        mMinHeight = a.getDimensionPixelSize(R.styleable.ProgressBar_android_minHeight, mMinHeight);
        mMaxHeight = a.getDimensionPixelSize(R.styleable.ProgressBar_android_maxHeight, mMaxHeight);

        setMax(a.getInt(R.styleable.ProgressBar_android_max, mMax));

        setProgress(a.getInt(R.styleable.ProgressBar_android_progress, mProgress));

        setSecondaryProgress(
                a.getInt(R.styleable.ProgressBar_android_secondaryProgress, mSecondaryProgress));

        a.recycle();
        
        // Änderungen für Text, Vorbereitungen
        int scaledFontSize = getResources().getDimensionPixelSize(R.dimen.font_size_small);
        speedText = "0";
        speedTextPaint = new Paint(); 
        speedTextPaint.setColor(Color.BLACK);
        speedTextPaint.setTextSize(scaledFontSize);
        speedTextPaint.setAntiAlias(true);
        mNoInvalidate = false;
        speedmode1txt = getResources().getString(R.string.control_kmh);

        tsPaint = new Paint(); 
        tsPaint.setColor(Color.RED);
        //tsPaint.setAntiAlias(true);
        tsPaint.setStyle(Paint.Style.FILL);
        
        speedtxtBounds = new Rect();
        
        rampPaint = new Paint(); 
        rampPaint.setColor(getResources().getColor(R.color.RampGrey));	// R.color.RampGrey
        //rampPaint.setColor(Color.RED);
        rampPaint.setAntiAlias(true);
        rampPaint.setStyle(Paint.Style.FILL);
        
        rampTextPaint = new Paint();
        rampTextPaint.setColor(Color.BLACK);
        rampTextPaint.setTextSize(scaledFontSize);
        rampTextPaint.setAntiAlias(true);
        
        rampPath = new Path();
    }

    /**
     * Converts a drawable to a tiled version of itself. It will recursively
     * traverse layer and state list drawables.
     */
    private Drawable tileify(Drawable drawable, boolean clip) {

        if (drawable instanceof LayerDrawable) {
        	LayerDrawable background = (LayerDrawable) drawable;
            final int N = background.getNumberOfLayers();
            Drawable[] outDrawables = new Drawable[N];

            for (int i = 0; i < N; i++) {
                int id = background.getId(i);
                outDrawables[i] = tileify(background.getDrawable(i),
                        (id == android.R.id.progress || id == android.R.id.secondaryProgress));
            }

            LayerDrawable newBg = new LayerDrawable(outDrawables);

            for (int i = 0; i < N; i++) {
                newBg.setId(i, background.getId(i));
            }

            return newBg;

        } else if (drawable instanceof StateListDrawable) {
            //StateListDrawable in = (StateListDrawable) drawable;
            StateListDrawable out = new StateListDrawable();
            /*int numStates = in.getStateCount();
            for (int i = 0; i < numStates; i++) {
                out.addState(in.getStateSet(i), tileify(in.getStateDrawable(i), clip));
            }*/
            return out;

        } else if (drawable instanceof BitmapDrawable) {
            final Bitmap tileBitmap = ((BitmapDrawable) drawable).getBitmap();
            if (mSampleTile == null) {
                mSampleTile = tileBitmap;
            }

            final ShapeDrawable shapeDrawable = new ShapeDrawable(getDrawableShape());
            return (clip) ? new ClipDrawable(shapeDrawable, Gravity.LEFT,
                    ClipDrawable.HORIZONTAL) : shapeDrawable;
        }

        return drawable;
    }

    Shape getDrawableShape() {
        final float[] roundedCorners = new float[] { 5, 5, 5, 5, 5, 5, 5, 5 };
        return new RoundRectShape(roundedCorners, null, null);
    }

    /**
     * <p>
     * Initialize the progress bar's default values:
     * </p>
     * <ul>
     * <li>progress = 0</li>
     * <li>max = 100</li>
     * </ul>
     */
    private void initProgressBar() {
        mMax = 100;
        mProgress = 0;
        mSecondaryProgress = 0;
        mMinWidth = 24;
        mMaxWidth = 48;
        mMinHeight = 24;
        mMaxHeight = 48;
    }

    /**
     * <p>Get the drawable used to draw the progress bar in
     * progress mode.</p>
     *
     * @return a {@link android.graphics.drawable.Drawable} instance
     *
     * @see #setProgressDrawable(android.graphics.drawable.Drawable)
     */
    public Drawable getProgressDrawable() {
        return mProgressDrawable;
    }

    /**
     * <p>Define the drawable used to draw the progress bar in
     * progress mode.</p>
     *
     * @param d the new drawable
     *
     * @see #getProgressDrawable()
     */
    public void setProgressDrawable(Drawable d) {
        if (d != null) {
            d.setCallback(this);
            // Make sure the ProgressBar is always tall enough
            int drawableHeight = d.getMinimumHeight();
            if (mMaxHeight < drawableHeight) {
                mMaxHeight = drawableHeight;
                requestLayout();
            }
        }
        mProgressDrawable = d;
        mCurrentDrawable = d;
        postInvalidate();
    }

    /**
     * @return The drawable currently used to draw the progress bar
     */
    Drawable getCurrentDrawable() {
        return mCurrentDrawable;
    }

    @Override
    protected boolean verifyDrawable(Drawable who) {
        return who == mProgressDrawable || super.verifyDrawable(who);
    }

    @Override
    public void postInvalidate() {
        if (!mNoInvalidate) {
            super.postInvalidate();
        }
    }

    private class RefreshProgressRunnable implements Runnable {

        private int mId;
        private int mProgress;
        private boolean mFromUser;

        RefreshProgressRunnable(int id, int progress, boolean fromUser) {
            mId = id;
            mProgress = progress;
            mFromUser = fromUser;
        }

        public void run() {
            doRefreshProgress(mId, mProgress, mFromUser);
            // Put ourselves back in the cache when we are done
            mRefreshProgressRunnable = this;
        }

        public void setup(int id, int progress, boolean fromUser) {
            mId = id;
            mProgress = progress;
            mFromUser = fromUser;
        }

    }

    private synchronized void doRefreshProgress(int id, int progress, boolean fromUser) {
        float scale = mMax > 0 ? (float) progress / (float) mMax : 0;
        final Drawable d = mCurrentDrawable;
        if (d != null) {
            Drawable progressDrawable = null;

            if (d instanceof LayerDrawable) {
                progressDrawable = ((LayerDrawable) d).findDrawableByLayerId(id);
            }

            final int level = (int) (scale * MAX_LEVEL);
            (progressDrawable != null ? progressDrawable : d).setLevel(level);
        } else {
            invalidate();
        }

        if (id == android.R.id.progress) {
            onProgressRefresh(scale, fromUser);
        }
    }

    void onProgressRefresh(float scale, boolean fromUser) {
    }

    private synchronized void refreshProgress(int id, int progress, boolean fromUser) {
        if (mUiThreadId == Thread.currentThread().getId()) {
            doRefreshProgress(id, progress, fromUser);
        } else {
            RefreshProgressRunnable r;
            if (mRefreshProgressRunnable != null) {
                // Use cached RefreshProgressRunnable if available
                r = mRefreshProgressRunnable;
                // Uncache it
                mRefreshProgressRunnable = null;
                r.setup(id, progress, fromUser);
            } else {
                // Make a new one
                r = new RefreshProgressRunnable(id, progress, fromUser);
            }
            post(r);
        }
    }

    /**
     * <p>Set the current progress to the specified value.</p>
     *
     * @param progress the new progress, between 0 and {@link #getMax()}
     *
     * @see #getProgress()
     * @see #incrementProgressBy(int)
     */
    public synchronized void setProgress(int progress) {
        setProgress(progress, false);
    }

    synchronized void setProgress(int progress, boolean fromUser) {
        if (progress < 0) {
            progress = 0;
        }

        if (progress > mMax) {
            progress = mMax;
        }

        if (progress != mProgress) {
            mProgress = progress;
            refreshProgress(android.R.id.progress, mProgress, fromUser);
        }
    }
    
    public synchronized void refreshAll() {		// zum Aktualisieren ohne dass sich der Progress geändert hat (zB: für Rangieren, Rampenänderung)
    	invalidate();
    }
    
    /*
    public synchronized void setAlpha(float a)
    {
    	this.setAlpha(a);	// ab APIlevel 11
    } */

    // Getter/setter für tsProgress
    public synchronized void setTsProgress(int ts) {
    	if (tsProgress < 0) { tsProgress = 0; }
        if (tsProgress > mMax) { tsProgress = mMax; }

        if (ts !=  tsProgress)
        {
            tsProgress = ts;
            invalidate();
        }
    }
    
    public synchronized int getTSProgress() {
        return tsProgress;
    }
    
    /**
     * <p>
     * Set the current secondary progress to the specified value.
     * </p>
     *
     * @param secondaryProgress the new secondary progress, between 0 and {@link #getMax()}
     * @see #getSecondaryProgress()
     * @see #incrementSecondaryProgressBy(int)
     */
    public synchronized void setSecondaryProgress(int secondaryProgress) {
        if (secondaryProgress < 0) {
            secondaryProgress = 0;
        }

        if (secondaryProgress > mMax) {
            secondaryProgress = mMax;
        }

        if (secondaryProgress != mSecondaryProgress) {
            mSecondaryProgress = secondaryProgress;
            refreshProgress(android.R.id.secondaryProgress, mSecondaryProgress, false);
        }
    }
    
    

    /**
     * <p>Get the progress bar's current level of progress.</p>
     *
     * @return the current progress, between 0 and {@link #getMax()}
     *
     * @see #setProgress(int)
     * @see #setMax(int)
     * @see #getMax()
     */
    @ViewDebug.ExportedProperty
    public synchronized int getProgress() {
        return mProgress;
    }

    /**
     * <p>Get the progress bar's current level of secondary progress.</p>
     *
     * @return the current secondary progress, between 0 and {@link #getMax()}
     *
     * @see #setSecondaryProgress(int)
     * @see #setMax(int)
     * @see #getMax()
     */
    @ViewDebug.ExportedProperty
    public synchronized int getSecondaryProgress() {
        return mSecondaryProgress;
    }

    /**
     * <p>Return the upper limit of this progress bar's range.</p>
     *
     * @return a positive integer
     *
     * @see #setMax(int)
     * @see #getProgress()
     * @see #getSecondaryProgress()
     */
    @ViewDebug.ExportedProperty
    public synchronized int getMax() {
        return mMax;
    }

    /**
     * <p>Set the range of the progress bar to 0...<tt>max</tt>.</p>
     *
     * @param max the upper range of this progress bar
     *
     * @see #getMax()
     * @see #setProgress(int)
     * @see #setSecondaryProgress(int)
     */
    public synchronized void setMax(int max) {
        if (max < 0) { max = 0; }
        
        if (max != mMax) {
            mMax = max;
            postInvalidate();

            if (mProgress > max) { mProgress = max; }
            
            // refreshProgress muss immer gemacht werden, nicht nur bei mProgress > max !!!!!!!!!
            refreshProgress(android.R.id.progress, mProgress, false);
            refreshAll();
            
        }
    }

    /**
     * <p>Increase the progress bar's progress by the specified amount.</p>
     *
     * @param diff the amount by which the progress must be increased
     *
     * @see #setProgress(int)
     */
    public synchronized final void incrementProgressBy(int diff) {
        setProgress(mProgress + diff);
    }

    /**
     * <p>Increase the progress bar's secondary progress by the specified amount.</p>
     *
     * @param diff the amount by which the secondary progress must be increased
     *
     * @see #setSecondaryProgress(int)
     */
    public synchronized final void incrementSecondaryProgressBy(int diff) {
        setSecondaryProgress(mSecondaryProgress + diff);
    }

    @Override
    public void setVisibility(int v) {
        if (getVisibility() != v) {
            super.setVisibility(v);
        }
    }

    @Override
    public void invalidateDrawable(Drawable dr) {
        if (!mInDrawing) {
            if (verifyDrawable(dr)) {
                final Rect dirty = dr.getBounds();
                final int scrollX = mScrollX + mPaddingLeft;
                final int scrollY = mScrollY + mPaddingTop;

                invalidate(dirty.left + scrollX, dirty.top + scrollY,
                        dirty.right + scrollX, dirty.bottom + scrollY);
            } else {
                super.invalidateDrawable(dr);
            }
        }
    }

    @Override
    protected void onSizeChanged(int w, int h, int oldw, int oldh) {
        // onDraw will translate the canvas so we draw starting at 0,0
        int right = w - mPaddingRight - mPaddingLeft;
        int bottom = h - mPaddingBottom - mPaddingTop;

        if (mProgressDrawable != null) {
            mProgressDrawable.setBounds(0, 0, right, bottom);
        }
    }

    @Override
    protected synchronized void onDraw(Canvas canvas) {
        super.onDraw(canvas);

        Drawable d = mCurrentDrawable;
        
        if (d != null) {
            // Translate canvas so a indeterminate circular progress bar with padding
            // rotates properly in its animation
            canvas.save();
            canvas.translate(mPaddingLeft, mPaddingTop);
            d.draw(canvas);
            
            // Änderungen für trainspeed (echten speed)
            if (tsProgress != mProgress)	// roten Balken nur anzeigen, wenn gewählter und zurückgemeldeter Speed nicht übereinstimmen
            {
            	int viewheight = getHeight();
            	float tswidth = viewheight / 80;	// Breite des roten Balkens 
            	if (tswidth < 2) { tswidth = 2; }
            	float tsheight =  (float)tsProgress / (float)mMax * (float)viewheight;
            	canvas.drawRect(5, getHeight() - tsheight, 5+tswidth, getHeight(), tsPaint);	// Breite ändern: im Verhältnis zur Gesamtbreite!
            }

            // Änderungen für eigenen Text

            speedText = String.valueOf(mProgress);
            // TODO: speedmode einbauen, muss übergeben werden
            /*
            if (dev.getSpeedmode() == 1) {
                speedText += speedmode1txt;
            } */
            speedTextPaint.getTextBounds(speedText, 0, speedText.length(), speedtxtBounds);
            int x = getWidth() / 2 - speedtxtBounds.centerX();
            //int y = getHeight() / 2 - bounds.centerY();
            canvas.drawText(speedText, x, 10 + speedtxtBounds.height(), speedTextPaint);
            drawRampIcons(canvas);


            
            //-----------------------------------------------------------
            
            canvas.restore();
        }
    }
    
    void drawRampIcons(Canvas canvas)	// Icons für speedramp in Speedbar einzeichnen
    {
    	float leftRampX;
    	float leftTextX;
    	//float rightRampX;
    	//float rightTextX;
    	float Y;
    	
    	int rampW;	// Ramp Breite
    	int rampH;  // Ramp Höhe
    	
    	int viewWith = getWidth();
    	rampW = getWidth() / 3;
    	rampH = rampW / 2;
    	
    	Y = getHeight() - (10 + rampH);		// 10 = Abstand vom unteren Rand
    	leftRampX = (viewWith / 3) - (rampW / 2);
    	//rightRampX = ((viewWith / 3) * 2) - (rampW / 2);
    	
    	// TODO Rampenbild anpassen
    	/*
    	if ((rightRampX + rampW + 5) < viewWith) 
    	{ rightRampX += 5;
    		if (leftRampX > 5) { leftRampX -= 5; }
    	} */
    	
    	// linke Ramp
    	rampPath.reset();
    	rampPath.moveTo(leftRampX, Y + rampH);
    	rampPath.lineTo(leftRampX + rampW, Y + rampH);
    	rampPath.lineTo(leftRampX + rampW, Y);
    	rampPath.close();
    	canvas.drawPath(rampPath, rampPaint);
    	
    	
    	// Text Ramp
    	// TODO: Bounds des Textes noch beachten wg. großer Schriftarten??
    	//Rect boundsRampTxt = new Rect();
        String RampText = "";
        //String RampText = String.valueOf(Basis.getCCDevice().getSpeedramp());
        // TODO rampe ein ooder ausbauen
        //TextPaint.getTextBounds(leftRampText, 0, leftRampText.length(), boundsRampTxt);   
        leftTextX = leftRampX + rampW / 2;
        float TextY = (int) (Y + rampH - 2);
        canvas.drawText(RampText, leftTextX , TextY, rampTextPaint);
        
    }
    

    @Override
    protected synchronized void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        Drawable d = mCurrentDrawable;

        int dw = 0;
        int dh = 0;
        if (d != null) {
            dw = Math.max(mMinWidth, Math.min(mMaxWidth, d.getIntrinsicWidth()));
            dh = Math.max(mMinHeight, Math.min(mMaxHeight, d.getIntrinsicHeight()));
        }
        dw += mPaddingLeft + mPaddingRight;
        dh += mPaddingTop + mPaddingBottom;

        setMeasuredDimension(resolveSize(dw, widthMeasureSpec),
                resolveSize(dh, heightMeasureSpec));
    }

    @Override
    protected void drawableStateChanged() {
        super.drawableStateChanged();

        int[] state = getDrawableState();

        if (mProgressDrawable != null && mProgressDrawable.isStateful()) {
            mProgressDrawable.setState(state);
        }
    }

    static class SavedState extends BaseSavedState {
        int progress;
        int secondaryProgress;

        /**
         * Constructor called from {@link ProgressBar#onSaveInstanceState()}
         */
        SavedState(Parcelable superState) {
            super(superState);
        }

        /**
         * Constructor called from {@link #CREATOR}
         */
        private SavedState(Parcel in) {
            super(in);
            progress = in.readInt();
            secondaryProgress = in.readInt();
        }

        @Override
        public void writeToParcel(Parcel out, int flags) {
            super.writeToParcel(out, flags);
            out.writeInt(progress);
            out.writeInt(secondaryProgress);
        }

        public static final Parcelable.Creator<SavedState> CREATOR
                = new Parcelable.Creator<SavedState>() {
            public SavedState createFromParcel(Parcel in) {
                return new SavedState(in);
            }

            public SavedState[] newArray(int size) {
                return new SavedState[size];
            }
        };
    }

    @Override
    public Parcelable onSaveInstanceState() {
        // Force our ancestor class to save its state
        Parcelable superState = super.onSaveInstanceState();
        SavedState ss = new SavedState(superState);

        ss.progress = mProgress;
        ss.secondaryProgress = mSecondaryProgress;

        return ss;
    }

    @Override
    public void onRestoreInstanceState(Parcelable state) {
        SavedState ss = (SavedState) state;
        super.onRestoreInstanceState(ss.getSuperState());

        setProgress(ss.progress);
        setSecondaryProgress(ss.secondaryProgress);
    }
}
