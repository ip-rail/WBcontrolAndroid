package wb.control;

import android.content.Context;
import android.graphics.Color;
import android.support.v4.app.Fragment;
import android.support.v7.widget.GridLayout;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.GridView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.ToggleButton;

public class ActionElement {  // für Laden der Action elements

	public static final int AE_TYP_TEXT = 0;		// fixer Text
	public static final int AE_TYP_BUTTON = 1;
	public static final int AE_TYP_BILDBUTTON = 2;
	public static final int AE_TYP_BILD = 3;
	public static final int AE_TYP_ONOFF_BUTTON = 4;
	public static final int AE_TYP_Data = 5;			// Text, der systeminterne Daten (zB. Lokgeschwindigkeit) anzeigt. ae.text ist hier der Text vor den Daten (wie zB. "TS=" usw).
	
	public static final int AE_MACROSCOPE_TYP_NONE = -1;	// kein Macroscope -> Verwendung: wenn's nicht angezeigt werden soll
	public static final int AE_MACROSCOPE_TYP_CCDEVICE = 0;
	public static final int AE_MACROSCOPE_TYP_NAMES = 1;
	public static final int AE_MACROSCOPE_TYP_ALLNAMES = 2;
	public static final int AE_MACROSCOPE_TYP_ALL = 3;
	public static final int AE_MACROSCOPE_TYP_ALLCON = 4;
	public static final int AE_MACROSCOPE_TYP_ALLFROMTYPE = 5;
	public static final int AE_MACROSCOPE_TYP_ALLCONTYPE = 6;
	public static final int AE_MACROSCOPE_USED_AS_DATA_SOURCE = 7;	// für AE_TYP_Data als Datenquelle (welches Device) verwendet

	// ae.datatype für AE_TYP_Data. MUSS mit dem index von values/arrays/actionelement_datatype zusammenstimmen!!!
	public static final int AE_DATATYPE_TRAINSPEED 	= 0;		
	public static final int AE_DATATYPE_U_ADC0 	    = 1;
	public static final int AE_DATATYPE_U_ADC1 		= 2;
    public static final int AE_DATATYPE_U_ADC2 		= 3;
    public static final int AE_DATATYPE_U_ADC3 		= 4;
    public static final int AE_DATATYPE_U_ADC4 		= 5;
    public static final int AE_DATATYPE_U_ADC5 		= 6;
    public static final int AE_DATATYPE_U_ADC6 		= 7;
    public static final int AE_DATATYPE_U_ADC7 		= 8;

    // ae.ort - in welches Layout gehört das ae: 0="Action" | 1= "Ctrl links" | 2="Ctrl rechts"  -> element gehört ins Action-Tab oder ins Ctrl-Panel
    public static final int AE_LOCATION_ACTION 	    = 0;
    public static final int AE_LOCATION_CTRL_L	    = 1;
    public static final int AE_LOCATION_CTRL_R 	    = 2;

	
	public int typ;      // Text | Button | BildButton | Bild | on/off Button -> index von R.array.actionelement_typ
	public String text;		// Test für Text/Button, Bildname (? - zumindest unter Win)
	public int ort;      	// 0="Action" | 1= "Ctrl links" | 2="Ctrl rechts"  -> element gehört ins Action-Tab oder ins Ctrl-Panel
	public int posX;		// falls Position verwendet wird
	public int posY;		// falls Position verwendet wird
	public int width;		// Breite des elements - derzeit nicht verwendet
	public int height;		// Hühe des Elements - derzeit nicht verwendet
	public Macro macro;		// Standardmacro, Macro für "on" bei on/off Button
	public Macro macro2;	// Macro für "off"-Funktion bei on/off Button
	public Boolean onoffstate;		// zum Speichern für den on/off Zustand bei on/off Button
	public View view;		// der zugehörige View (nicht nur beim Edit)!!	// TODO: view sollte hier gar nicht gespeichert werden!! checken+entfernen!
	public int scope;	// Macro-Gültigkeitsbereich
	public String scopedata;		// wird für typnamen oder devicenamen für den MacroScope benütigt (bei mehreren: durch Beistrich getrennt). bei AE-Typ=AE_TYP_Data steht hier der devicename des devices, von dem die daten geholt werden sollen ("" bedeutet dann das aktuell gesteuerte Device).
	public int datatype;			// bezeichnet welche Daten angezeigt werden sollen, wenn der AE-Typ=AE_TYP_Data ist (siehe values/arrays/actionelement_datatype)
	//public String picpath;      // Pfad zur Bilddatei eines ImageButton

	public ActionElement()
	{
		init();
	}

	public ActionElement(String t)
	{
		init();
		text = t;
	}
	
	public ActionElement(String t, int aetyp)
	{
		init();
		text = t;
		typ = aetyp;
	}
	
	private void init()
	{
		typ = 1;	// Button. Standardwert für neue AEs
		text = "";
		ort = AE_LOCATION_ACTION;	    // standardmäßig in Action verwenden (nicht Ctrl)
		posX = -1;		// falls Position verwendet wird (früher war initwert = 0)
		posY = -1;		// falls Position verwendet wird (früher war initwert = 0)
		width = 0;
		height = 0;
		scope = 0;	// CCDevice
		scopedata = "";
		onoffstate = false;	// Anfangszustand off
		datatype = 0;
		// view, macro bleiben null, werden erst im ActAction_Edit mit gültigen Daten befüllt.
	}


	// Daten im Widget des AE aktualisieren (Text, Data)

	public void update(ViewGroup container)
	{
		View v = container.findViewWithTag(this);

		if (v != null)
		{
			switch (this.typ) {

				case ActionElement.AE_TYP_TEXT:
					TextView tv = (TextView)v;
					this.view = tv;
					tv.setText(this.text);
					break;

				case ActionElement.AE_TYP_Data:
					TextView tvd = (TextView)v;
					String data = "";
					Device d = Basis.getDevicelistObjectByName(this.scopedata);

					switch (this.datatype) {

						case ActionElement.AE_DATATYPE_TRAINSPEED:	// Trainspeed
							if (d != null) { data = String.valueOf(d.getTrainspeed()); }
							break;

						case ActionElement.AE_DATATYPE_U_ADC0:	// Spannung: Schiene
						case ActionElement.AE_DATATYPE_U_ADC1:
						case ActionElement.AE_DATATYPE_U_ADC2:
						case ActionElement.AE_DATATYPE_U_ADC3:
						case ActionElement.AE_DATATYPE_U_ADC4:
						case ActionElement.AE_DATATYPE_U_ADC5:
						case ActionElement.AE_DATATYPE_U_ADC6:
						case ActionElement.AE_DATATYPE_U_ADC7:
							data = d.getUString(this.datatype-1);
							break;
					}
					tvd.setText(this.text + data);
					break;

				case ActionElement.AE_TYP_BUTTON:
					Button b = (Button)v;
					b.setText(this.text);
					break;

				case ActionElement.AE_TYP_ONOFF_BUTTON:
					ToggleButton tb = (ToggleButton)v;
					tb.setText(this.text);	// wird vor dem ersten Click angezeigt
					tb.setTextOn(this.text + " ON");
					tb.setTextOff(this.text + " OFF");
					break;
			}
		}

	}	// end updateAE




    // create view from ae

    public View createView(ViewGroup container, Fragment f) {

        //AEtag tag = new AEtag(ae.macro, ae.typ);
        View aeview = null;
        ViewGroup.LayoutParams layoutparam;

        Context c = container.getContext();


        // Action: GridView
        if (container instanceof GridView) {

            // Position und Breite/Höhe im Raster auslesen und im LayoutParams definieren
            int rowsize = 1;
            if (this.width >0) { rowsize = this.width; }	// # rows wide
            int colsize = 1;
            if (this.height >0) { rowsize = this.height; }	// # columns high
            int row = this.posX;
            int col = this.posY;
            if (row == -1) { row = GridLayout.UNDEFINED; }	// Row position
            if (col == -1) { col = GridLayout.UNDEFINED; }	// column position
            GridLayout.Spec rowspec = GridLayout.spec(row, rowsize);
            GridLayout.Spec colspec = GridLayout.spec(col, colsize);
            //GridLayout.LayoutParams layoutparam = new GridLayout.LayoutParams(rowspec, colspec);
            layoutparam = new GridLayout.LayoutParams(rowspec, colspec);
        }
        else //if (container instanceof LinearLayout)
        {
            //Ctrl: LinearLayout
            //LinearLayout.LayoutParams layoutparam =  new LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
            layoutparam =  new LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
            //if (aelist == aelist_rechts) { layoutparam.gravity = 5; }	// gravity = right
            if (this.ort == ActionElement.AE_LOCATION_CTRL_R) { ((LinearLayout.LayoutParams)layoutparam).gravity = Gravity.RIGHT; }	// gravity = right
        }

        //layoutparam.setMargins(R.dimen.space_std_w, R.dimen.space_std_h, R.dimen.space_std_w, R.dimen.space_std_h);
        // TODO error: buttons nicht mehr sichtbar, sobald margin gesetzt ist
        final float scale = f.getActivity().getBaseContext().getResources().getDisplayMetrics().density;


        switch (this.typ) {

            case ActionElement.AE_TYP_TEXT:
                //TextView tv = new TextView(getActivity());
                TextView tv = new TextView(c);
                this.view = tv;
                tv.setText(this.text);
                //tv.setTextSize(TypedValue.COMPLEX_UNIT_SP, 16);
                tv.setTag(this);	// ae selbst als tag setzen!
                tv.setLayoutParams(layoutparam);
                tv.setTextColor(Color.rgb(200, 0, 0));
                //tv.setId(aelist.indexOf(this));	// als id den aelist index setzen!!
                f.registerForContextMenu(tv);
                aeview = tv;
                break;

            case ActionElement.AE_TYP_Data:
                TextView tvd = new TextView(c);
                this.view = tvd;
                tvd.setText(this.text);
                //tvd.setTextSize(TypedValue.COMPLEX_UNIT_SP, 16);
                tvd.setTag(this);	// ae selbst als tag setzen!
                tvd.setLayoutParams(layoutparam);
                tvd.setTextColor(Color.rgb(200,0,0));
                //tvd.setId(aelist.indexOf(this));	// als id den alist index setzen!!
                f.registerForContextMenu(tvd);
                aeview = tvd;
                break;

            case ActionElement.AE_TYP_BUTTON:
                Button b = new Button(c);
                this.view = b;
                b.setText(this.text);
                b.setTag(this);

                //b.setGravity(Gravity.FILL_HORIZONTAL);

                int pixelsw = (int) (100 * scale + 0.5f);	//dp in pixel konvertieren
                int pixelsh = (int) (50 * scale + 0.5f);	//dp in pixel konvertieren

                //int pixelmargin = (int) (R.dimen.space_std_w * scale + 0.5f);	//dp in pixel konvertieren
                layoutparam.width = pixelsw;
                layoutparam.height = pixelsh;
                //layoutparam.topMargin = pixelmargin;
                //layoutparam.bottomMargin = pixelmargin;
                //layoutparam.leftMargin = pixelmargin;
                //layoutparam.rightMargin = pixelmargin;

                b.setLayoutParams(layoutparam);
                //b.setId(aelist.indexOf(this));	// als id den alist index setzen!!
                f.registerForContextMenu(b);
                b.setOnClickListener(new View.OnClickListener() {
                    public void onClick(View v) {
                        // do something when the button is clicked
                        if (v.getTag() != null)
                        {
                            ActionElement ae = (ActionElement) v.getTag();
                            if (ae.macro != null)
                            {
                                ae.macro.execute(ae.scope, ae.scopedata);	// Macro ausführen
                            }
                        }
                    } } );
                aeview = b;
                break;

            case ActionElement.AE_TYP_ONOFF_BUTTON:
                ToggleButton tb = new ToggleButton(c);
                this.view = tb;
                tb.setText(this.text);	// wird vor dem ersten Click angezeigt
                tb.setTextOn(this.text + " ON");
                tb.setTextOff(this.text + " OFF");
                tb.setTag(this);
                tb.setChecked(this.onoffstate);

                int pixelsw2 = (int) (100 * scale + 0.5f);	//dp in pixel konvertieren
                int pixelsh2 = (int) (50 * scale + 0.5f);	//dp in pixel konvertieren

                //int pixelmargin2 = (int) (R.dimen.space_std_w * scale + 0.5f);	//dp in pixel konvertieren
                layoutparam.width = pixelsw2;
                layoutparam.height = pixelsh2;
                //layoutparam.topMargin = pixelmargin2;
                //layoutparam.bottomMargin = pixelmargin2;
                //layoutparam.leftMargin = pixelmargin2;
                //layoutparam.rightMargin = pixelmargin2;

                tb.setLayoutParams(layoutparam);
                //tb.setId(aelist.indexOf(this));	// als id den alist index setzen!!
                f.registerForContextMenu(tb);
                tb.setOnClickListener(new View.OnClickListener() {
                    public void onClick(View v) {
                        // do something when the button is clicked
                        if (v.getTag() != null)
                        {
                            ActionElement ae = (ActionElement) v.getTag();
                            //if (ae.macro != null)
                            if (ae.view != null)
                            {
                                ae.onoffstate = !ae.onoffstate;	// on/off Zustand speichern
                                ToggleButton t = (ToggleButton) ae.view;
                                if (t.isChecked()) { if (ae.macro != null) { ae.macro.execute(ae.scope, ae.scopedata); } }	// Macro "on" ausführen
                                else { if (ae.macro2 != null) { ae.macro2.execute(ae.scope, ae.scopedata); } }	// Macro "off" ausführen
                            }
                        }
                    } } );
                aeview = tb;
                break;
        }

        if (aeview != null) { aeview.requestLayout(); }

        return aeview;
    }	// end createView()



	
}
