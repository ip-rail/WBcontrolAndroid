package wb.control;

import java.util.ArrayList;

public class Commandset implements Device.OnStopListener
{	// jetzt statt Macrocommands

	private String devicename;
	private ArrayList<String> commandlist;
	private Boolean waitfor_stop;
	private Boolean waitfor_start_vw;
	private Boolean waitfor_start_rw;
    private String waitfor_rfidcode = null; // null bedeutet nicht gesetzt, sonst auf den gesetzten String als RFID-Code warten

    private final static String LOGTAG = "Commandset";

	//TODO: die events werden wieder benötigt! stop/start_vw / start_rw (ev. weitere).. checken, wie das gelöst werden soll

	//XXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXX
	// Konstruktor
	//XXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXX

	public Commandset() // Konstruktor
	{
		setDevicename("");
		commandlist = new ArrayList<String>();
		waitfor_stop = false;
		waitfor_start_vw = false;
		waitfor_start_rw = false;
	}

	public Commandset(String devname) // Konstruktor mit Devicename
	{
		setDevicename(devname);
		commandlist = new ArrayList<String>();
		waitfor_stop = false;
		waitfor_start_vw = false;
		waitfor_start_rw = false;
	}

	//XXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXX
	// get/set
	//XXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXX  

	public void setDevicename(String devicename) {
		this.devicename = devicename;
	}

	public String getDevicename() {
		return devicename;
	}

	public void setCommandlist(ArrayList<String> l) {
		this.commandlist = l;
	}

	public ArrayList<String> getCommandlist() {
		return commandlist;
	}


	//XXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXX
	// andere Methoden
	//XXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXX


	public String getCommandsString()
	{
		String cmdtxt = "";

		for (String cmd : commandlist)
		{
			cmdtxt += cmd;  // TODO ändern auf Stringbuffer oä statt +=
		}
		//cmdtxt.Remove(cmdtxt.Length - 1, 1);    // letzten Beistrich entfernen!
		return cmdtxt; 
	}

	public void AddCommand(String commandtxt)
	{
		commandlist.add(commandtxt);
	}

	public void ImportCommandString(String cmdtext) // string "<a><b>" -> List. List wird vor Import geleert!!
	{
		Boolean doWork = true;
		commandlist.clear();

		while (doWork)
		{
			int startpos = cmdtext.indexOf("<");
			int endpos = cmdtext.indexOf(">");

			if ((startpos >= 0) && (endpos > 0) && (startpos < endpos)) // gültiger WBcontrol-Befehl ist vorhanden
			{
				//String command2 = new String();
				//command2 = cmdtext.substring(startpos, endpos - startpos + 1);
				String command = new String(cmdtext.substring(startpos, endpos - startpos + 1)); 
				if (!command.equals("")) { commandlist.add(command); }
				if (cmdtext.length() > endpos) {cmdtext = cmdtext.substring(endpos+1); }	// aktuellen command aus test entfernen
			}
			else { doWork = false; }	// Ende - nix mehr zu tun!!!
			
		}
	}
	
	/*
	public void ImportRawCommandString(String cmdtext) // string "a, b" -> List (Beistrich = Trennzeichen). List wird vor Import geleert!!
	{
		// wird nur zum Testen verwendet!

		Boolean doWork = true;

		commandlist.clear();
		
		String[] Test; 
		Test = cmdtext.split(",");
		for (String s : Test)
		{
			s.compareTo("");
			commandlist.add(s);
		}
		
	}
	*/

	public void Start(final String devname)     // Macro-Ausführung starten (immer in eigenem thread)
	{
		// Macro-Thread starten -> besser: per AsyncTask       
		Thread MacroThread = new Thread( new Runnable(){ 
			public void run() { 
				MacroWork(devname);
			} 
		}); 
		MacroThread.start();

	}

	private void MacroWork(String devname)     // Arbeit für Macro-Thread
	{
		Device macrodevice = null;   // das Device, zu dem das macro gehört (bei wbevent, rfid)
		Device macroziel;     // das aktuelle macroziel-device (veränderbar, es kann ja mal etwas an ein anderes device geschickt gehören..)
        Device zielsic = null;         // Zieldevice sichern, falls Ziel zwischendurch geändert werden soll (damit man es wieder zurückstellen kann)


		if (!this.devicename.equals("")) {
			macrodevice = Basis.getDevicelistObjectByName(devname);
		}
		if ((macrodevice == null) && (Basis.getCCDevice() != null)) {
			macroziel = Basis.getCCDevice();
		}
		else
        {
			macroziel = macrodevice;    // Ziel-Device standardmäßig setzen, wenn die Commands nicht schon zu einem Device gehören (wbevent, rfid)
		}

        //macroziel darf nicht mehr null sein
        if (macroziel == null)
        {
            Basis.AddLogLine(Basis.getBcontext().getString(R.string.err_cmdset_dev_undefined), LOGTAG, WBlog.wblogtype.Error);
            return;
        }  // sonst Bearbeitung abbrechen

        // TODO: groß/kleinschreibung??

		for (String command : this.commandlist) {
			int startpos = command.indexOf("<");
			int endpos = command.indexOf(">");
			String txt = command.substring(startpos + 1, endpos);
			String[] cmdtxt = txt.split(":");

			if (command.startsWith("<#"))   // Macro behandeln
			{
				if (cmdtxt[0].equals("#Ziel"))  // Macro Ziel setzen
				{
                    zielsic = macroziel;    // macroziel sichern
					Device zieldev = Basis.getDevicelistObjectByName(cmdtxt[1]);
					if (zieldev != null) {
						macroziel = zieldev;
					} // wenn Devicename bekannt ist
				}
                else if (cmdtxt[0].equals("#ZielReset"))  // Macro Ziel wieder auf Anfangswert setzen
				{

                    if (zielsic != null) { macroziel = zielsic; }
                    else
                    {
                        //TODO: sonst error ausgeben, abbrechen - sollte aber nicht passieren
                        return;
                    }

                    /* weg damit
					if (macrodevice == null) {
						macroziel = Basis.getCCDevice();
					}  // Deviceindex für Ziel standardmäßig setzen, wenn die Commands nicht schon zu einem Device gehören (wbevent, rfid)
					else {
						macroziel = macrodevice;
					}*/
				}
                // TODO: <#macro

                else if (cmdtxt[0].equals("#wait")) {

                    if (cmdtxt[1].equals("s"))    // cmdtxt[2] Sekunden warten
                    {
                        int seks = 0;
                        try {
                            seks = Integer.parseInt(cmdtxt[2]);
                        } catch (NumberFormatException nfe1) {
                            seks = 1;
                        }
                        try {
                            Thread.sleep(seks * 1000);
                        } catch (InterruptedException e) {
                            e.printStackTrace();
                        }

                    }
                    else if (cmdtxt[1].equals("ms"))    // cmdtxt[2] Milisekunden warten
                    {
                        int milliseks = 0;
                        try {
                            milliseks = Integer.parseInt(cmdtxt[2]);
                        } catch (NumberFormatException nfe2) {
                            milliseks = 1;
                        }
                        try {
                            Thread.sleep(milliseks);
                        } catch (InterruptedException e) {
                            e.printStackTrace();
                        }

                    }
                    else if (cmdtxt[1].equals("stop"))    // auf "stop"-Ereignis warten
                    {
                        waitfor_stop = true;
                        macroziel.setOnStopListener(this);

                        while (waitfor_stop) {
                            try {
                                Thread.sleep(100);  // 100ms warten
                            } catch (InterruptedException e) {
                                e.printStackTrace();
                            }
                        }
                        macroziel.removeStopListener(this);
                    }
                    else if (cmdtxt[1].equals("svw"))    // auf "start vorwärts"-Ereignis warten
                    {
                        waitfor_start_vw = true;
                        while (waitfor_start_vw) {
                            try {
                                Thread.sleep(200);  // 100ms warten
                            } catch (InterruptedException e) {
                                e.printStackTrace();
                            }
                        }
                    }
                    else if (cmdtxt[1].equals("srw"))    // auf "start rückwärts"-Ereignis warten
                    {
                        waitfor_start_rw = true;
                        while (waitfor_start_rw) {
                            try {
                                Thread.sleep(200);  // 100ms warten
                            } catch (InterruptedException e) {
                                e.printStackTrace();
                            }
                        }
                    }
                    else if (cmdtxt[1].equals("RFID"))    // auf "RFID"-Ereignis warten
                    {
                        waitfor_rfidcode = cmdtxt[2];
                        while (waitfor_rfidcode != null) {
                            try {
                                Thread.sleep(100);  // 100ms warten
                            } catch (InterruptedException e) {
                                e.printStackTrace();
                            }
                        }
                    }
                }
            }

            else  // Befehl direkt an Ziel-Device ausgeben
            {
                if (macroziel != null) {
                    macroziel.Netwrite(command);
                }
            }
        }

    }

    @Override
    public void onStop(Device d) {
        waitfor_stop = false;   // Flag zurücksetzen
    }



	/*
    private void HandleWBEvent(WBEventArgs e) // Event-Handler für config-WBEvents
    {

        if (e.Source.Name == this.DeviceName)   // nur durchführen, wenn's vom richtigen Device stammt
        {
            switch (e.Typ)
            {
                case WBeventType.Stop:
                    waitfor_stop = false;
                    break;

                case WBeventType.Start_vw:
                    waitfor_start_vw = false;
                    break;

                case WBeventType.Start_rw:
                    waitfor_start_rw = false;
                    break;
            }
        }

    }*/

}
