package wb.control;

import java.util.ArrayList;

import wb.control.WBlog.wblogtype;


public class Macro {

	private String name;
	private ArrayList<Commandset> commandlist;
	private String comment; // Beschreibung
	private String rfidcode;	// für macros,wbevents: rfidcode = null. für rfids: rfidcode = "" oder text
	private int type;			// Macrotyp
	
	public static final int MACROTYP_MACRO 		= 	0;	// Macros
	public static final int MACROTYP_EVENT 		= 	1;	// Ereignisse (WBevents)
	public static final int MACROTYP_RFID 		= 	2;	// RFIDs (spezielle WBevents)

	//XXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXX
	// Konstruktor
	//XXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXX

	public Macro()   // Konstruktor 
	{
		name = "";
		comment = "";
		rfidcode = null;
		type = MACROTYP_MACRO;
		commandlist = new ArrayList<Commandset>();
	}
	
	public Macro(String mname) // Konstruktor - ev. Konfig laden (woher?)
	{
		name = mname;
		comment = "";
		commandlist = new ArrayList<Commandset>();
		commandlist.add(new Commandset(Basis.getMacroStandardDeviceName()));
		rfidcode = null;
	}
	
	public Macro(String mname, int mtyp) // Konstruktor - ev. Konfig laden (woher?)
	{
		name = mname;
		comment = "";
		type = mtyp;
		commandlist = new ArrayList<Commandset>();
		commandlist.add(new Commandset(Basis.getMacroStandardDeviceName()));
		rfidcode = null;
	}
	
	public Macro(String mname, ArrayList<Commandset> mcommands, String mbeschreibung) // Konstruktor - ev. Konfig laden (woher?)
	{
		name = mname;
		commandlist = mcommands;
		comment = mbeschreibung;
		rfidcode = null;
		
		Boolean found = false;
		
		String standarddevname = Basis.getMacroStandardDeviceName();
		
		for (Commandset cs : commandlist)
		{
			if (cs.getDevicename().equals(standarddevname)) { found = true; }
		}
		if (!found) { commandlist.add(new Commandset(standarddevname)); }
		
	}
	
	public Macro(String eventname, String codetext)   // Konstruktor mit Namen und RFID-code
	{
		name = eventname;
		rfidcode = codetext;
		type = MACROTYP_RFID;
		commandlist = new ArrayList<Commandset>();
	}


	//XXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXX
	// get/set
	//XXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXX

	public void setName(String name) {
		this.name = name;
	}

	public String getName() {
		return name;
	}

	public void setCommandlist(ArrayList<Commandset> l) {
		this.commandlist = l;
	}

	public ArrayList<Commandset> getCommandlist() {
		return commandlist;
	}    
	
	public void setComment(String comment) {
		this.comment = comment;
	}

	public String getComment() {
		return comment;
	}
	
	public void setRfidcode(String rfidcode) {
		this.rfidcode = rfidcode;
	}

	public String getRfidcode() {
		return rfidcode;
	}
	
	public int getType() {
		return type;
	}

	public void setType(int type) {
		this.type = type;
	}
	
	//XXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXX
	// andere Methoden
	//XXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXX


	@Override
    public String toString()
    {
		return this.name;
    }
	
	
	public void AddCommand(String devicename, String cmd)	// einen Befehl an das Commandset (hinten) anhängen Commandset wird neu angelegt, wenn noch nicht existent
	{
		int csindex = getCommandlistIndexByDeviceName(devicename);
		if (csindex > -1)	// Commandset für Device bereits vorhanden
		{
			Commandset cs = commandlist.get(csindex);
			cs.AddCommand(cmd);
		}
		else	// Commandset für Device muss erst angelegt werden
		{
			Commandset newcs = new Commandset(devicename);
			newcs.AddCommand(cmd);
			commandlist.add(newcs);
		}
	}
	
	
	public void AddCommandSet(String devicename)
	{
		if (getCommandlistIndexByDeviceName(devicename) == -1)	// nur anlegen, wenn noch nicht vorhanden!
		{
			Commandset newcs = new Commandset(devicename);
			commandlist.add(newcs);
		}
	}
	
	public void RemoveCommandSet(Commandset cs)
	{
		commandlist.remove(cs);
	}
	
	
	public int getCommandlistIndexByDeviceName(String dname)	// Commandset nach Devicenamen suchen und Commandlist-Index zurückgeben
	{
		int index = -1;		// -1 = not found
		for (Commandset cs : commandlist)
		{
			if (cs.getDevicename().equals(dname)) { index = commandlist.indexOf(cs); break; }
		}
		return index;
	}
	
	public Commandset getCommandSetByDeviceName(String dname)	// Commandset nach Devicenamen suchen und zurückgeben
	{
		Commandset csfound = null;		// null = not found
		for (Commandset cs : commandlist)
		{
			if (cs.getDevicename().equals(dname)) { csfound = cs; break; }
		}
		return csfound;
	}
	
	public void execute(int scope, String data)	// Macro anhand von MacroScope ausführen
	{	// data: das Scope-data (die Namen der Geräte)
		
		switch (scope) {

		case ActionElement.AE_MACROSCOPE_TYP_CCDEVICE:
			if (Basis.getCCDevice() != null) { execute(Basis.getCCDevice().getName()); }
			break;
			
		case ActionElement.AE_MACROSCOPE_TYP_NAMES:	// die Geräte mit den per data übergebenen Namen
			String[] dataparts = data.split(",");
			for (String d : dataparts) { execute(d); }
			break;
			
		case ActionElement.AE_MACROSCOPE_TYP_ALLNAMES:	// alle Geräte, die im Macro namentlich definiert sind
			for (Commandset cs : commandlist)
			{
				if (!cs.getDevicename().equals(Basis.getMacroStandardDeviceName())) { execute(cs.getDevicename()); }
			}
			break;
		}
		
		Basis.setStatusText("Macro " + this.name + " ausgeführt!");
		Basis.AddLogLine(this.name + " ausgeführt!", "Macro", wblogtype.Info);
	}
	
	public void execute(String devicename)	// Commandset für name ausführen bzw. für Standard, wenn name nicht vorhanden ist
	{
		Commandset cs;
		
		cs = getCommandSetByDeviceName(devicename);	// nach Namen suchen
		
		if (cs == null) { cs = getCommandSetByDeviceName(Basis.getMacroStandardDeviceName()); }		// wenn kein Commandset für das genannte Device vorhanden ist, dann das für Standard nehmen
		cs.Start(devicename);		// Name mitgeben, damit man im Fall "Standard" weiß, an welches Device man senden muss
	}
	
	public void copyFrom(Macro m_old)	// alles kopieren
	{
		name = String.valueOf(m_old.getName());
		comment = String.valueOf(m_old.getComment());
		rfidcode = String.valueOf(m_old.getRfidcode());
		type = m_old.type;
		
		for (Commandset cs : m_old.getCommandlist())
		{
			String newdevname = String.valueOf(cs.getDevicename());
			Commandset newcs = new Commandset(newdevname);
			for (String cmd : cs.getCommandlist())
			{
				String newcmd = String.valueOf(cmd);
				newcs.AddCommand(newcmd);
			}
			commandlist.add(newcs);
		}
	}
	
	

}	// end class Macro
