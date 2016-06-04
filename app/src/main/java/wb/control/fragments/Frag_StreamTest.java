package wb.control.fragments;

import java.util.ArrayList;

import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import wb.control.AVStream;
import wb.control.Basis;
import wb.control.R;
import wb.control.WBFragID;
import wb.control.activities.FAct_control;
import wb.control.dialogfragments.DialogFrag_ChooseFromList;
import wb.control.views.MjpegView;


public class Frag_StreamTest extends Fragment
implements View.OnClickListener, DialogFrag_ChooseFromList.OnTextFromDialogListListener, WBFragID
{

    private static final int FRAGMENT_ID = FAct_control.FRAGMENT_STREAM_TEST;
	private static final String TAG = "MjpegStream";

	private View fragview;	// Root-View für das Fragment
	private MjpegView mjview;
	private ArrayList<AVStream> streamlist;	// Liste der verfügbaren Streams
	private int selectedAVstramIndex;		// ausgewählter AVStream - Index in streamlist. -1: nichts ausgewählt
	private ArrayAdapter<String> sa;			// für Stream-Spinner


    public int getFragmentID() { return FRAGMENT_ID; }

    /*
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);


		//streamlist = new ArrayList<AVStream>();
		
	} */
	
	

	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {

		fragview = inflater.inflate(R.layout.f_streamtest, container, false);

		streamlist = Basis.getAvStreamlist();
		mjview = (MjpegView)fragview.findViewById(R.id.mjpegView_stream);
		mjview.setOnClickListener(this);

		return fragview;

	}	// end onCreateView


	
	
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


    private void fresume() {

        selectedAVstramIndex = Basis.getSelectedAVstream();
        if (selectedAVstramIndex > -1) { mjpg_on(); }
        //mjpg_on();
    }


    private void fpause() {

        Basis.setSelectedAVstream(selectedAVstramIndex);
        mjpg_off();
    }



	@Override
	public void onClick(View v) {

		int id = v.getId();
		if (id == R.id.mjpegView_stream) {
			DialogFrag_ChooseFromList dialog = new DialogFrag_ChooseFromList();
			Bundle args = new Bundle();
			args.putInt("dialogtype", DialogFrag_ChooseFromList.DIALOG_CFL_AV_STREAMS);
			args.putString("name", getResources().getString(R.string.dialog_ac_stream_title));
			ArrayList<String> dialogstreams = new ArrayList<String>();
			for (AVStream s : streamlist)
			{
				dialogstreams.add(s.name + " " + s.source);
			}
			args.putStringArrayList("list", dialogstreams);
			dialog.setArguments(args);
			dialog.setOnTextFromDialogListListener(this);
			dialog.show(getFragmentManager(), "cfldialog");
		}
	}




	public void mjpg_on()
	{		
		if (selectedAVstramIndex > -1)
		{
			AVStream av = streamlist.get(selectedAVstramIndex);
			if (av != null)
			{
				mjview.startAVstream(av.source);
			}
		} 
	}

	public void mjpg_off()
	{
		if (mjview != null) { mjview.stopPlayback(); }
	}


	@Override
	public void OnTextFromDialogList(String text, int nummer, int typ) {
		
		// zurückgegeben nummer ist der Array-Index des ausgewählten Streams
		
		if (mjview != null) { mjview.stopPlayback(); }
		selectedAVstramIndex = nummer;
		mjpg_on();	// Auswahl abspielen

	}




}
