package wb.control.fragments;

import java.io.IOException;
import java.net.URI;

import android.media.AudioManager;
import android.media.MediaPlayer;
import android.media.MediaPlayer.OnBufferingUpdateListener;
import android.media.MediaPlayer.OnCompletionListener;
import android.media.MediaPlayer.OnErrorListener;
import android.media.MediaPlayer.OnInfoListener;
import android.media.MediaPlayer.OnPreparedListener;
import android.media.MediaPlayer.OnVideoSizeChangedListener;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.View;
import android.view.ViewGroup;
import android.webkit.WebView;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.Spinner;
import android.widget.TabHost;
import android.widget.TabHost.OnTabChangeListener;
import android.widget.ToggleButton;
import android.widget.VideoView;

import org.apache.http.HttpResponse;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.DefaultHttpClient;

import wb.control.Basis;
import wb.control.MjpegInputStream;
import wb.control.R;
import wb.control.WBFragID;
import wb.control.activities.FAct_control;
import wb.control.views.MjpegView;


public class Frag_VideoTest extends Fragment
implements View.OnClickListener, AdapterView.OnItemSelectedListener, OnTabChangeListener, SurfaceHolder.Callback, OnPreparedListener,
        OnCompletionListener, OnVideoSizeChangedListener, OnBufferingUpdateListener, OnErrorListener, OnInfoListener, WBFragID
{
    private static final int FRAGMENT_ID = FAct_control.FRAGMENT_VIDEO_TEST;

	private View fragview;	// Root-View für das Fragment
	private SurfaceView surfaceView_video;
	private ToggleButton toggleButton_video;
	private EditText editText_videosource;
	private Spinner spinner_videosource;
	
	private WebView webView_video;
	private LinearLayout tab_mp, tab_web, tab_v, tab_mj;
	private VideoView videoView_v;
	private MjpegView mjview;

	private TabHost tabHost;
	
	public static final String TAB_MP = "mp";
	public static final String TAB_WEB = "web";
	public static final String TAB_VID = "vid";
	public static final String TAB_MJPG = "mjpg";
	
	private MediaPlayer mplayer;
	private SurfaceHolder sholder;
	private String mp_datasource;
	
	ArrayAdapter<String> sa;			// für Stream-Spinner
	String[] streamsources;				// für Stream-Auswahl
	
	private int videoWidth;
	private int videoHeight;
	private boolean isVideoSizeKnown = false;
	private boolean isVideoReadyToBePlayed = false;
	
	private Boolean mp_on, web_on, vid_on, mjpg_on;
	private int currentTab = 0;	// 0=MediaPlayer, 1= WebView, 2=VideoView, 3=MJPG


	public int getFragmentID() { return FRAGMENT_ID; }


    /*
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

    } */



	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {

		fragview = inflater.inflate(R.layout.f_videotest, container, false);
		
		surfaceView_video = (SurfaceView)fragview.findViewById(R.id.surfaceView_video);
		sholder = surfaceView_video.getHolder();
		sholder.addCallback(this);

		toggleButton_video = (ToggleButton)fragview.findViewById(R.id.toggleButton_video);
		toggleButton_video.setOnClickListener(this);
		//toggleButton_video.setEnabled(false);
		editText_videosource = (EditText)fragview.findViewById(R.id.editText_videosource);
		//editText_videosource.setText("http://10.0.0.7:8080/a1");
		//editText_videosource.setText("/mnt/sdcard/DCIM/Camera/20120211_172211.mp4");
		
		spinner_videosource = (Spinner)fragview.findViewById(R.id.spinner_videosource);
		streamsources = getResources().getStringArray(R.array.streamsources);
		sa=new ArrayAdapter<String>(getActivity(), android.R.layout.simple_spinner_item, streamsources); 
		spinner_videosource.setAdapter(sa);
		spinner_videosource.setOnItemSelectedListener(this);
		
		tabHost = (TabHost) fragview.findViewById(android.R.id.tabhost);
		tabHost.setup();
		//tabHost.addTab(newTab(TAB_WORDS, R.string.tab_words, R.id.tab_1));
		//tabHost.addTab(newTab(TAB_NUMBERS, R.string.tab_numbers, R.id.tab_2));
		
		webView_video = (WebView)fragview.findViewById(R.id.webView_video);
		webView_video.getSettings().setJavaScriptEnabled(false);
		
		videoView_v = (VideoView)fragview.findViewById(R.id.videoView_v);

		mjview = (MjpegView)fragview.findViewById(R.id.mjpegView_stream);
		
		tab_mp = (LinearLayout)fragview.findViewById(R.id.tab_surface);
		tab_web = (LinearLayout)fragview.findViewById(R.id.tab_web);
		tab_v =  (LinearLayout)fragview.findViewById(R.id.tab_vid);
		tab_mj =  (LinearLayout)fragview.findViewById(R.id.tab_mjpg);
		
		TabHost.TabSpec tspec = tabHost.newTabSpec(TAB_MP);
		tspec.setContent(R.id.tab_surface);
		tspec.setIndicator("MediaPlayer");
		tabHost.addTab(tspec);
		
		tspec = tabHost.newTabSpec(TAB_WEB);
		tspec.setContent(R.id.tab_web);
		tspec.setIndicator("WebBrowser");
		tabHost.addTab(tspec);
		
		tspec = tabHost.newTabSpec(TAB_VID);
		tspec.setContent(R.id.tab_vid);
		tspec.setIndicator("videoView");
		tabHost.addTab(tspec);
		
		tspec = tabHost.newTabSpec(TAB_MJPG);
		tspec.setContent(R.id.tab_vid);
		tspec.setIndicator("MJPGView");
		tabHost.addTab(tspec);
		
		tabHost.setOnTabChangedListener(this);
		//tabHost.setCurrentTab(2);
		
		mp_on = false;
		web_on = false;
		vid_on = false;
		mjpg_on = false;
		
		return fragview;

	}	// end onCreateView

	
	/*
	@Override
	public void onResume() {
		super.onResume();
		
	} */
	
	@Override
	public void onPause() {

        fpause();
		super.onPause();
	}

    /*
	private void fresume() {

	} */


	private void fpause() {

        if (toggleButton_video.isChecked()) { toggleButton_video.setChecked(false); } // mplayer wird dabei beendet
        releaseMediaPlayer();
        mjpg_off();
	}

	
	@Override
	public void onClick(View v) {
		
		int id = v.getId();
		if (id == R.id.toggleButton_video) {
			switch (currentTab) {
			
			case 0:	// mp
				if  (((ToggleButton) v).isChecked()) { mp_on(); }
				else { mp_off(); }
				break;
				
			case 1:	// web
				if  (((ToggleButton) v).isChecked()) { webView_video.loadUrl(editText_videosource.getText().toString()); }
				else { webView_video.stopLoading(); }
				break;
				
			case 2:	//vid
				if  (((ToggleButton) v).isChecked()) { video_on(); }
				else { video_off(); }
				break;
				
			case 3:	//mjpg
				if  (((ToggleButton) v).isChecked()) { video_on(); }
				else { video_off(); }
				break;
			
			}
		}
		
	}
	
	
	
	public void mp_off()
	{
		if (mplayer != null)
		{
			if (mplayer.isPlaying()) { mplayer.stop(); }
			mplayer.reset();
			doCleanUp();
		}
	}
	
	public void mp_on()
	{
		if (mplayer == null) { prepare_new_mp(); }
		else { prepare_old_mp(); } 
	}
	
	
	public void video_off()
	{
		videoView_v.stopPlayback();
	}
	
	public void video_on()
	{
		if (mplayer == null) { prepare_new_mp(); }
		else { prepare_old_mp(); } 
		
		videoView_v.setVideoURI(Uri.parse(editText_videosource.getText().toString()));
		//videoView_v.setMediaController(new MediaController(getActivity()));
		videoView_v.requestFocus();
		videoView_v.start();
	} 
	
	public void mjpg_on()
	{
		new MJPGTask().execute(editText_videosource.getText().toString());

	}
	
	public void mjpg_off()
	{
		if (mjview != null) { mjview.stopPlayback(); }

	}
	
	
    private void startVideoPlayback() {
    	
        //sholder.setFixedSize(videoWidth, videoHeight);
        mplayer.start();
    }
    
    private void releaseMediaPlayer() {
        if (mplayer != null) {
        	mplayer.release();
        	mplayer = null;
        }
    }

    private void doCleanUp() {
        videoWidth = 0;
        videoHeight = 0;
        isVideoReadyToBePlayed = false;
        isVideoSizeKnown = false;
    }


    public void prepare_old_mp()
    {
    	mp_datasource = editText_videosource.getText().toString();

    	if ((mp_datasource != null) && (!mp_datasource.equals("")))
    	{
    		try {
    			mplayer.setDataSource(getActivity(), Uri.parse(mp_datasource));

    		} catch (IllegalArgumentException e) {
    			// TODO Auto-generated catch block
    			e.printStackTrace();
    		} catch (IllegalStateException e) {
    			// TODO Auto-generated catch block
    			e.printStackTrace();
    		} catch (IOException e) {
    			// TODO Auto-generated catch block
    			e.printStackTrace();
    		}
    		mplayer.prepareAsync();	// wenn bereit, wird er von onPrepared() wieder gestartet
    	}
    }
	
	public void prepare_new_mp()
	{
		mp_datasource = editText_videosource.getText().toString();

		if ((mp_datasource != null) && (!mp_datasource.equals("")))
		{
			mplayer = new MediaPlayer();

			try {
				mplayer.setDataSource(getActivity(), Uri.parse(mp_datasource));
				
			} catch (IllegalArgumentException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			} catch (IllegalStateException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			mplayer.setDisplay(sholder);
			mplayer.setAudioStreamType(AudioManager.STREAM_MUSIC);	// wird in Beispiel-Activity so angegeben!
			mplayer.setScreenOnWhilePlaying(true);
			mplayer.setOnInfoListener(this);
			mplayer.setOnErrorListener(this);
			mplayer.setOnVideoSizeChangedListener(this);
			mplayer.setOnBufferingUpdateListener(this);
			mplayer.setOnPreparedListener(this);
			mplayer.setOnCompletionListener(this);
			mplayer.prepareAsync();

		}
	}


	//SurfaceHolder callbacks
	@Override
	public void surfaceCreated(SurfaceHolder holder) {
		// MediaPlayer vorbereiten
		
		//prepare_new_mp();	// sonst wird automatisch losgespielt
		toggleButton_video.setEnabled(true);
	}

	@Override
	public void surfaceChanged(SurfaceHolder holder, int format, int width,	int height) {
		// TODO Auto-generated method stub
		
		
	}


	@Override
	public void surfaceDestroyed(SurfaceHolder holder) {
		// TODO Auto-generated method stub
		
	}


	// MediaPlayer callbacks
	
	@Override
	public void onPrepared(MediaPlayer mp) {
		
		isVideoReadyToBePlayed = true;
        //if (isVideoReadyToBePlayed && isVideoSizeKnown) { startVideoPlayback(); }
		startVideoPlayback();	// bei HLS kommt onVideoSizeChanged erst später!!
	}


	@Override
	public void onCompletion(MediaPlayer mp) {

		//mplayer.stop();
		if (toggleButton_video.isChecked()) { toggleButton_video.setChecked(false); }

	}


	@Override
	public void onVideoSizeChanged(MediaPlayer mp, int width, int height) {
		if (width == 0 || height == 0) { return; }	// Logeintrag machen?
		
        isVideoSizeKnown = true;
        videoWidth = width;
        videoHeight = height;
        
    	Boolean wasRunning = mplayer.isPlaying();
    	if (wasRunning) { mplayer.stop(); }
        sholder.setFixedSize(videoWidth, videoHeight);
        if (wasRunning) 
        { 
        	mplayer.prepareAsync();
        	isVideoReadyToBePlayed = false;
        }
        
        if (isVideoReadyToBePlayed && isVideoSizeKnown) { startVideoPlayback(); }
	}


	@Override
	public void onBufferingUpdate(MediaPlayer mp, int percent) {
		// TODO Auto-generated method stub
		
	}


	@Override
	public boolean onError(MediaPlayer mp, int what, int extra) {

		//mplayer.reset();
		//prepare_old_mp();
		
		return false;	// True if the method handled the error
	}


	@Override
	public boolean onInfo(MediaPlayer mp, int what, int extra) {
		// TODO Auto-generated method stub
		return false;
	} 


	@Override
	public void onItemSelected(AdapterView<?> parent, View v, int position, long itemid) {
		int id = parent.getId();
		if (id == R.id.spinner_videosource) {
			editText_videosource.setText(streamsources[position]);
		}
		
	}


	@Override
	public void onNothingSelected(AdapterView<?> arg0) {
		// TODO Auto-generated method stub
		
	}


	@Override
	public void onTabChanged(String tabId) {

		//int oldTab = currentTab;
		
		switch (currentTab) {	// Zustand sichern

		case 0:	// mp
			mp_on = toggleButton_video.isChecked();
			break;

		case 1:	// web
			web_on = toggleButton_video.isChecked();	
			break;

		case 2:	// vid
			vid_on = toggleButton_video.isChecked();	
			break;
			
		case 3:	// mjpg
			mjpg_on = toggleButton_video.isChecked();	
			break;

		}
		
		if (tabId.equals(TAB_MP))
		{
			currentTab = 0;
			// if (toggleButton_video.isChecked()) { toggleButton_video.setChecked(false); }
			toggleButton_video.setChecked(mp_on);		// Zustand restoren
		}
		else if (tabId.equals(TAB_WEB))
		{
			currentTab = 1;
			toggleButton_video.setChecked(web_on);		// Zustand restoren
		}
		
		else if (tabId.equals(TAB_VID))
		{
			currentTab = 2;
			toggleButton_video.setChecked(vid_on);		// Zustand restoren
		}
		
		else if (tabId.equals(TAB_MJPG))
		{
			currentTab = 3;
			toggleButton_video.setChecked(mjpg_on);		// Zustand restoren
		}
	}


	public class MJPGTask extends AsyncTask<String, Void, MjpegInputStream> {
        protected MjpegInputStream doInBackground(String... url) {
            //TODO: if camera has authentication deal with it and don't just not work
            HttpResponse res;
            DefaultHttpClient httpclient = new DefaultHttpClient();     
            //Log.d(TAG, "1. Sending http request");
            try {
                res = httpclient.execute(new HttpGet(URI.create(url[0])));
                //Log.d(TAG, "2. Request finished, status = " + res.getStatusLine().getStatusCode());
                if(res.getStatusLine().getStatusCode()==401){
                    //You must turn off camera User Access Control before this will work
                    return null;
                }
                return new MjpegInputStream(res.getEntity().getContent());  
            } catch (ClientProtocolException e) {
                e.printStackTrace();
                //Log.d(TAG, "Request failed-ClientProtocolException", e);
                //Error connecting to camera
            } catch (IOException e) {
                e.printStackTrace();
                //Log.d(TAG, "Request failed-IOException", e);
                //Error connecting to camera
            }

            return null;
        }

        protected void onPostExecute(MjpegInputStream result) {
        	mjview.setSource(result);
        	mjview.setDisplayMode(MjpegView.SIZE_BEST_FIT);
        	mjview.showFps(true);
        }
    }



	
}	// end Class Frag_VideoTest
