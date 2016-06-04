package wb.control;


public class TrackElement {
	public int type;			// type ist gleichzeitig die PictureID (der index im Picture Array te_pics)
	public Boolean frei;		// true = frei / false = belegt (nur Verkehrsdaten) // ändern auf int (damit man definieren kann, wovon belegt (Lok usw..)
	public int trackswitch;	// Stellung der Weiche (0=Standard= geradeaus) (nur Verkehrsdaten)
	public String name;		// frei wählbare Bezeichnung
	
	public TrackElement(int trackelementType)   // Konstruktor
	{
		type = trackelementType;
		frei = true;
		trackswitch = 0;
		name = "";
	}
	
	public TrackElement(int trackelementType, String ename)   // Konstruktor
	{
		type = trackelementType;
		frei = true;
		trackswitch = 0;
		name = ename;
	}
	
	public String toString()	// für Speicherung in DB
	{
		//final Resources r = Resources.getSystem();		
		//if (name.equals("")) { name = Basis.getBcontext().getString(R.string.trackview_trackelement_unknown); }
		if (name.equals("")) { name = " "; }
		return String.format("%s,%s;", String.valueOf(type), name);
		//die anderen Daten sind nur Verkehrsdaten, die nicht gespeichert werden müssen
	}
	
}	// end class TrackElement
