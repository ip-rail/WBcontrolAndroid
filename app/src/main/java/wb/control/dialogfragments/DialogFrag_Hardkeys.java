package wb.control.dialogfragments;

import java.util.ArrayList;

import wb.control.Basis;
import wb.control.HardKey;
import wb.control.Macro;
import wb.control.OnHardkeyChangedListener;
import wb.control.R;
import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.Dialog;
import android.os.Bundle;
import android.support.v4.app.DialogFragment;
import android.view.KeyEvent;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.Spinner;
import android.widget.TextView;

public class DialogFrag_Hardkeys extends DialogFragment implements AdapterView.OnItemSelectedListener {

	
	Button button_dialog_hk_save, button_dialog_hk_cancel;
	EditText editText_customname;
	TextView textView_keycodename, textView_customname, textView_key_function, textView_key_function2, textView_key_macro1, textView_key_macro2, textView_keytype;
	Spinner spinner_keytype, spinner_key_function, spinner_key_function2, spinner_key_macro1, spinner_key_macro2;
	LinearLayout LinearLayout_key1;
	HardKey hk;
	String keycode_name;
	int keycode;
	int dialogstep;
	boolean step2_needed, step2_need_function2, step2_need_macro, step2_need_macro2;
	
	String[] keytype, keytypeinfo, function;
	ArrayList<Macro> macros;
	ArrayAdapter<Macro> ma;
	ArrayAdapter<String> kta, fa;
	
	OnHardkeyChangedListener HKChangedListener;

	public DialogFrag_Hardkeys() {
		// Empty constructor required for DialogFragment
	}


	
	@Override
	public void onAttach(Activity activity) {
		super.onAttach(activity);

		try {
			HKChangedListener = (OnHardkeyChangedListener) activity;
		} catch (ClassCastException e) {
			throw new ClassCastException(activity.toString() + " must implement OnChangeDisplayModeListener");
		}
	}
		
	
		

	@SuppressLint("NewApi")
	@Override
	public Dialog onCreateDialog(Bundle savedInstanceState) {

		final Dialog dialog = new Dialog(getActivity());
		//setRetainInstance(true);
		keycode = getArguments().getInt("keycode");
		dialogstep = 1;
		step2_need_function2 = false;
		step2_need_macro = false;
		step2_need_macro2 = false;
		
		hk = Basis.DBloadHardkey(keycode);
		
		// nach OrientationChange: gesicherten daten wieder laden
		if (savedInstanceState != null) 
		{
			dialogstep = savedInstanceState.getInt("dialogstep", 1);
			step2_need_function2 = savedInstanceState.getBoolean("step2_f2", false);
			step2_need_macro = savedInstanceState.getBoolean("step2_m", false);
			step2_need_macro2 = savedInstanceState.getBoolean("step2_m2", false);
			hk.keytype = savedInstanceState.getInt("keytype", 0);
			hk.function1 = savedInstanceState.getInt("func1", 0);
			hk.function2 = savedInstanceState.getInt("func2", 0);
			hk.macro1 = savedInstanceState.getString("macro1", "");
			hk.macro2 = savedInstanceState.getString("macro2", "");
			hk.custom_name = savedInstanceState.getString("cname", "");
		}

		
		// TODO was anderes machen -> dialog.dismiss() funktioniert hier noch nicht!!
		//if (hk == null) { dialog.dismiss(); }	// wenn keine Daten vorhanden sind, Dialog beenden // TODO log error

		dialog.setContentView(R.layout.dialog_hardkeys);
		dialog.setTitle(R.string.dialog_hkey_title);
		
		LinearLayout_key1 = (LinearLayout) dialog.findViewById(R.id.LinearLayout_key1);
		textView_keycodename = (TextView) dialog.findViewById(R.id.textView_keycodename);
		textView_keytype = (TextView) dialog.findViewById(R.id.textView_keytype);
		textView_customname = (TextView) dialog.findViewById(R.id.textView_customname);
		textView_key_function = (TextView) dialog.findViewById(R.id.textView_key_function);
		textView_key_function2 = (TextView) dialog.findViewById(R.id.textView_key_function2);
		textView_key_macro1 = (TextView) dialog.findViewById(R.id.textView_key_macro1);
		textView_key_macro2 = (TextView) dialog.findViewById(R.id.textView_key_macro2);
		
		editText_customname = (EditText) dialog.findViewById(R.id.editText_customname);
		spinner_keytype = (Spinner) dialog.findViewById(R.id.spinner_keytype);
		spinner_key_function = (Spinner) dialog.findViewById(R.id.spinner_key_function);
		spinner_key_function2  = (Spinner) dialog.findViewById(R.id.spinner_key_function2);
		spinner_key_macro1 = (Spinner) dialog.findViewById(R.id.spinner_key_macro1);
		spinner_key_macro2 = (Spinner) dialog.findViewById(R.id.spinner_key_macro2);
		
		textView_keycodename.setVisibility(View.VISIBLE);
		
		if (Basis.getApiLevel() >=12) // unter Apilevel 12 ist dieser Name nicht über diese Funktion verfügbar
		{
			textView_keycodename.setText(KeyEvent.keyCodeToString(keycode));
		}
		else { textView_keycodename.setVisibility(View.GONE); }
 		
		
		keytype = getResources().getStringArray(R.array.hardkey_type);
		keytypeinfo = getResources().getStringArray(R.array.hardkey_type_info);	// TODO: custom adapter, sonst wird nur keytype angezeigt
		function = getResources().getStringArray(R.array.hardkey_function);
		
		macros = new ArrayList<Macro>();
		macros.addAll(Basis.getMacrolist());
		
		kta = new ArrayAdapter<String>(getActivity(), android.R.layout.simple_spinner_item, keytype); 
		fa = new ArrayAdapter<String>(getActivity(), android.R.layout.simple_spinner_item, function); 
		ma = new ArrayAdapter<Macro>(getActivity(), android.R.layout.simple_spinner_item, macros);
		spinner_keytype.setAdapter(kta);
		spinner_keytype.setOnItemSelectedListener(this);
		spinner_keytype.setSelection(hk.keytype);
		
		spinner_key_function.setAdapter(fa);
		spinner_key_function.setOnItemSelectedListener(this);
		spinner_key_function.setSelection(hk.function1);
		
		spinner_key_function2.setAdapter(fa);
		spinner_key_function2.setOnItemSelectedListener(this);
		spinner_key_function2.setSelection(hk.function2);
		
		spinner_key_macro1.setAdapter(ma);
		spinner_key_macro1.setOnItemSelectedListener(this);
		spinner_key_macro1.setSelection(getMacroIndexByName(hk.macro1));
		spinner_key_macro2.setAdapter(ma);
		spinner_key_macro2.setOnItemSelectedListener(this);
		spinner_key_macro2.setSelection(getMacroIndexByName(hk.macro2));
		
		editText_customname.setText(hk.custom_name);
		
		// Elemente von Schritt2 vestecken
		textView_key_function2.setVisibility(View.GONE);
		spinner_key_function2.setVisibility(View.GONE);

		textView_key_macro1.setVisibility(View.GONE);
		textView_key_macro2.setVisibility(View.GONE);
		spinner_key_macro1.setVisibility(View.GONE);
		spinner_key_macro2.setVisibility(View.GONE);
		
	
		dialog.setCancelable(true);

		button_dialog_hk_save = (Button) dialog.findViewById(R.id.button_dialog_hk_save);
		button_dialog_hk_save.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {

				if (dialogstep == 2)
				{
					saveHardKeyToDB();
					dialog.dismiss();
				}
				else	// step1
				{
					if (step2_needed)
					{
						dialogstep = 2;
						activateStep2();
					}
					else	// step1 -> kein step2 -> save
					{
						saveHardKeyToDB();
						dialog.dismiss();
					}
				}
				
				

			}
		});

		button_dialog_hk_cancel = (Button) dialog.findViewById(R.id.button_dialog_hk_cancel);

		button_dialog_hk_cancel.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {

				dialog.dismiss();
			}
		});

		
		// falls der Dialog neu gestartet werden muss und schon bei step 2 war
		if (dialogstep == 2) { activateStep2(); }
		
		return dialog;
	}

	@Override	// wg. Problemen bei ScreenRotation
	public void onDestroyView() {
		if (getDialog() != null && getRetainInstance())
			getDialog().setOnDismissListener(null);
		super.onDestroyView();
	}

	
	// dialogstep bei Orientationchange sichern
	@Override
	public void onSaveInstanceState(Bundle savedInstanceState) {

		super.onSaveInstanceState(savedInstanceState);	// muss als ersts aufgerufen werden!!
		savedInstanceState.putInt("dialogstep", dialogstep);
		savedInstanceState.putBoolean("step2_f2", step2_need_function2);
		savedInstanceState.putBoolean("step2_m", step2_need_macro);
		savedInstanceState.putBoolean("step2_m2", step2_need_macro2);
		savedInstanceState.putInt("keytype", hk.keytype);
		savedInstanceState.putInt("func1", hk.function1);
		savedInstanceState.putInt("func2", hk.function2);
		savedInstanceState.putString("macro1", hk.macro1);
		savedInstanceState.putString("macro2", hk.macro2);
		savedInstanceState.putString("cname", editText_customname.getText().toString());
	}

	
	

	// Spinner Behandlung
	@Override
	public void onItemSelected(AdapterView<?> parent, View v, int position, long itemid) {

		int id = parent.getId();

		switch (id) {

		case R.id.spinner_keytype:
			checkSpinner_keytype(position);
			break;

		case R.id.spinner_key_function:
			checkSpinner_function(position);
			break;

		case R.id.spinner_key_function2:
			checkSpinner_function2(position);
			break;

		case R.id.spinner_key_macro1:
			checkSpinner_macro1(position);
			break;

		case R.id.spinner_key_macro2:
			checkSpinner_macro2(position);
			break;

		}

	}
	
	@Override
	public void onNothingSelected(AdapterView<?> arg0) {
		// TODO Auto-generated method stub
		
	}


	//Spinner Auswertung
	private void checkSpinner_keytype(int index)
	{
		hk.keytype = index;
		
		switch (index) {
		
		case HardKey.HARDKEY_TYPE_KLICK_LONG:
		case HardKey.HARDKEY_TYPE_ON_OFF:
			step2_need_function2 = true;
			prepareStep2();
			break;
			
		default:
			removeStep2();
		}
	}
	
	
	private void checkSpinner_function(int index)
	{
		hk.function1 = index;
		if (index == HardKey.HARDKEY_FUNCTION_MACRO) { step2_need_macro = true; }
	}
	
	private void checkSpinner_function2(int index)
	{
		hk.function2 = index;
		// läuft im step2
		if (index == HardKey.HARDKEY_FUNCTION_MACRO) 
		{ 
			step2_need_macro2 = true;
			if (dialogstep == 2)
			{
				textView_key_macro2.setVisibility(View.VISIBLE);
				spinner_key_macro2.setVisibility(View.VISIBLE);
			}
			
		}
	}

	
	private void checkSpinner_macro1(int index)
	{
		hk.macro1 = macros.get(index).getName();
	}
	
	private void checkSpinner_macro2(int index)
	{
		hk.macro2 = macros.get(index).getName();
	}
	
	
	private void prepareStep2()
	{
		step2_needed = true;
		button_dialog_hk_save.setText(R.string.gen_next);
	}
	
	private void removeStep2()
	{
		step2_needed = false;
		button_dialog_hk_save.setText(R.string.save);
	}
	
	private void hideStep1()
	{
		
		textView_customname.setVisibility(View.GONE);
		editText_customname.setVisibility(View.GONE);
		LinearLayout_key1.setVisibility(View.GONE);
		//textView_keytype.setVisibility(View.GONE);
		//spinner_keytype.setVisibility(View.GONE);
		textView_key_function.setVisibility(View.GONE);
		spinner_key_function.setVisibility(View.GONE);
		button_dialog_hk_save.setText(R.string.save);
	}
	
	private void activateStep2()
	{
		hideStep1();
		button_dialog_hk_save.setText(R.string.save);

		if (step2_need_function2)
		{
			textView_key_function2.setVisibility(View.VISIBLE);
			spinner_key_function2.setVisibility(View.VISIBLE);
		}

		if (step2_need_macro)
		{
			textView_key_macro1.setVisibility(View.VISIBLE);
			spinner_key_macro1.setVisibility(View.VISIBLE);
		}

		if (step2_need_macro2)
		{
			textView_key_macro2.setVisibility(View.VISIBLE);
			spinner_key_macro2.setVisibility(View.VISIBLE);
		}
	}



	private void saveHardKeyToDB()
	{
		hk.custom_name = editText_customname.getText().toString();
		Basis.DBsaveHardkey(keycode, hk);
		HKChangedListener.OnHardkeyChanged(keycode);	// Info an die Activity
	}

	
	private int getMacroIndexByName(String mname)	// Objekt nach Name suchen und Index zurückgeben
    {
    	int index = -1;		// -1 = not found
    	for (Macro m : macros)
    	{
    		if (m.getName().equals(mname)) { index = macros.indexOf(m); break; }
    	}
		return index;
    }

}
