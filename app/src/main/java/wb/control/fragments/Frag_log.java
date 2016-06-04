package wb.control.fragments;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.content.LocalBroadcastManager;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.Button;
import android.widget.ListView;
import android.widget.TextView;

import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.GregorianCalendar;

import wb.control.Basis;
import wb.control.OnHideMnuItemListener;
import wb.control.R;
import wb.control.WBFragID;
import wb.control.WBlog;
import wb.control.activities.FAct_control;

public class Frag_log extends Fragment
implements View.OnClickListener, WBFragID {

	private static final int FRAGMENT_ID = FAct_control.FRAGMENT_LOG;

	View fragview;	// Root-View für das Fragment
	ViewGroup fcontainer;
	// OnFragReplaceListener fragReplListener;
	LayoutInflater layoutInflater;
	OnHideMnuItemListener hideMnuItemListener;
	BroadcastReceiver locBcReceiver;
	IntentFilter logifilter;
	
	Button Button_test_logclear;
	ListView listView_test_log;
	TextView textView_test_log_info;
    LogAdapter ladapter;
	DecimalFormat zweistellig;	// format für Datum: zweistelliger Tag, Monat


    public int getFragmentID() { return FRAGMENT_ID; }

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		zweistellig  = new DecimalFormat("00");	// oder "###"
		// lokales Broadcasting einrichten - für WLAN Aktivierung/Deaktivierung
		logifilter = new IntentFilter();
		logifilter.addAction(Basis.ACTION_NEW_LOGDATA);
		locBcReceiver = new BroadcastReceiver() {
			@Override
			public void onReceive(Context context, Intent intent) {
				if (intent.getAction().equals(Basis.ACTION_NEW_LOGDATA)) { ladapter.addNewData(); }
			}
		};

		ladapter = new LogAdapter(getActivity());
	}
	


	@Override
	public void onResume() {

		super.onResume();
        fresume();
	}
	

	@Override
	public void onPause() {

        fpause();
		super.onPause();
	}
	
	
	
	@Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
		
		fcontainer = container;
		layoutInflater = inflater;
		fragview = inflater.inflate(R.layout.test_log, container, false);

		//container.removeAllViewsInLayout(); //TODO: Test für


		Button_test_logclear  = (Button)fragview.findViewById(R.id.Button_test_logclear);
		Button_test_logclear.setOnClickListener(this);
		listView_test_log = (ListView)fragview.findViewById(R.id.listView_test_log);
		listView_test_log.setAdapter(ladapter);
		
		textView_test_log_info = (TextView) fragview.findViewById(R.id.textView_test_log_info);
		textView_test_log_info.setText("0 " + this.getString(R.string.test_log_info));

		return fragview;
    }	// end onCreateView



    private void fresume() {

        LocalBroadcastManager.getInstance(getActivity()).registerReceiver(locBcReceiver, logifilter);
        ladapter.addNewData();
    }


    private void fpause() {

        LocalBroadcastManager.getInstance(getActivity()).unregisterReceiver(locBcReceiver);	// localBraodcast-Empfang stoppen (für Device-loader)
    }


	public void onClick(View v) {

		int id = v.getId();
		if (id == R.id.Button_test_logclear) { ladapter.clearLog(); }
	}	// end onClick
	


	private class LogAdapter extends BaseAdapter {

		private ArrayList<WBlog> textlist;
		private final Context context;
        private int lastknownindex;     // index des letzten Eintrages, der schon übernommen wurde

		public LogAdapter(Context c) {
			context = c;
			textlist = new ArrayList<WBlog>();

            textlist.addAll(Basis.getLogNewerThan(-1));
            lastknownindex = textlist.size()-1;
            notifyDataSetChanged();
		}

		@Override
		public int getCount() {
			
			if (textView_test_log_info != null)	// hier frech einhängen liefert immer die aktuelle Anzahl!!
			{
				textView_test_log_info.setText(textlist.size() + " " + getActivity().getString(R.string.test_log_info));
			}
			return textlist.size();
		}

		@Override
		public Object getItem(int position) {
			return textlist.get(position);
		}

		@Override
		public long getItemId(int position) {
			return position;
		}

		@Override
		public View getView(int position, View convertView, ViewGroup parent) {

			TextView tv;
			WBlog logentry = textlist.get(position);
			
			Calendar c = new GregorianCalendar();  // This creates a Calendar instance with the current time
			c.setTimeInMillis(logentry.getTime());
			String date = zweistellig.format(c.get(Calendar.DAY_OF_MONTH)) + "." + zweistellig.format(c.get(Calendar.MONTH)+1) + ".";
			String time = zweistellig.format(c.get(Calendar.HOUR_OF_DAY)) + ":" + zweistellig.format(c.get(Calendar.MINUTE)) + ":" + zweistellig.format(c.get(Calendar.SECOND));
			String line = date + " " + time + " " + logentry.getTag() + " " + logentry.getText();
			//TODO: umbauen auf String.format (siehe basis log speichern)

			if (convertView == null) { tv = new TextView(context); }	// if it's not recycled
			else { tv = (TextView)convertView; }

			tv.setText(line);
			tv.setTextAppearance(context, R.style.txt_log_standard);
			return tv;
		}

        public void addNewData()    // neue Logeinträge übernehmen
        {
			int reallogsize = Basis.getLogSize();

            if (reallogsize < lastknownindex) { textlist.clear(); } // different, new data in log

            textlist.addAll(Basis.getLogNewerThan(lastknownindex));
            lastknownindex = textlist.size()-1;
            notifyDataSetChanged();
        }

        public void clearLog()    // Log leeren
        {
            lastknownindex = -1;
            textlist.clear();
            Basis.clearLog();
            notifyDataSetChanged();
        }

	}	// end class LogAdapter
	
	
}	// end class Frag_log
