package wb.control;

import android.content.Intent;
import android.util.Log;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.net.SocketAddress;
import java.net.SocketTimeoutException;
import java.net.UnknownHostException;

import wb.control.Basis.WBprotokoll;
import wb.control.Device.DeviceType;
import wb.control.WBlog.wblogtype;

public class NetWaiter implements Runnable {
	
	private Device dev;
	private Socket TCPsocket;
	private BufferedReader input;
	private Thread netwriterthread;
	
	final static String LOGTAG = "netwaiter";
	
	
	public NetWaiter(Device d)	//Konstruktor zur Übergabe des Devices
	{
		dev = d;
	}

	@Override
	public void run() {	// run wird beim Starten des threads ausgeführt
				
		boolean success = connect();
		
		if (success)
		{
			//sendMsgToUIthread(wb.control.fragments.Frag_control.MSG_DEVICE_CONNECTED);
            dev.sendStdLocBroadcast(Basis.ACTION_DEVICE_CONNECTED);
			doTCPwork();	// Haupt-Arbeitsschleife
		}
		else	// Msg senden, dass Verbindung fehlgeschlagen ist
		{
			//sendMsgToUIthread(wb.control.fragments.Frag_control.MSG_DEVICE_CONNECING_FAILED);
            dev.sendStdLocBroadcast(Basis.ACTION_DEVICE_CONNECING_FAILED);
		}
		
	}

	
	private boolean connect()
	{
		int tcpport = Basis.getTcpPort();
		if (dev.getTyp() == DeviceType.Server) { tcpport = Basis.getServerTcpPort(); }
		
		Basis.AddLogLine(String.format(Basis.getBcontext().getString(R.string.netw_connecting), dev.getIP(), tcpport), LOGTAG, wblogtype.Info);

		//this.tcpClient = new TcpClient(new IPEndPoint(Config.eigeneIP, tcpport));
		try
		{
			TCPsocket = new Socket();
			TCPsocket.setSoTimeout(1000);	// 1 Sekunden Read-Timeout
			//TCPsocket.setSoTimeout(0);	// no Read-Timeout -> blockt, bis etwas empfangen wurde -> darf nicht sein, sonst wird der NetWaiter bei Netz-Trennung nie beendet 
			SocketAddress localadr = new InetSocketAddress(Basis.getEigeneIP(),0);	// port = 0 -> freien lokalen Port wählen
			TCPsocket.bind(localadr);
			SocketAddress devadr = new InetSocketAddress(dev.getIP(),tcpport);
			TCPsocket.connect(devadr, 5000);	// 5 Sekunden Connect-Timeout
		}
		catch (IOException e)
		{
			Basis.AddLogLine(Basis.getBcontext().getString(R.string.netw_conn_failed) + e.toString(), LOGTAG, wblogtype.Error);
			return false;
		}
		catch (SecurityException e)
		{
			Basis.AddLogLine(Basis.getBcontext().getString(R.string.netw_conn_refused) + e.toString(), LOGTAG, wblogtype.Error);
			return false;
		}
		catch (Exception e)
		{
			Basis.AddLogLine(Basis.getBcontext().getString(R.string.netw_conn_unknown_err) + e.toString(), LOGTAG, wblogtype.Error);
			return false;
		}

		if (TCPsocket != null)	// wenn der Socket nicht erstellt werden konnte
		{

			if (!TCPsocket.isConnected())
			{
				try { TCPsocket.close(); } 
				catch (IOException e) { Basis.AddLogLine(e.toString(), LOGTAG, wblogtype.Error); }
				finally { TCPsocket = null; }
			}

			if (TCPsocket == null) { return false; }

			try
			{
				input = new BufferedReader(new InputStreamReader(TCPsocket.getInputStream()));
			}
			catch(UnknownHostException e) 
			{
				Basis.AddLogLine(Basis.getBcontext().getString(R.string.netw_err_tcp_io_host) + e.toString(), LOGTAG, wblogtype.Error);
				return false;
			}
			catch (IOException e)
			{
				Basis.AddLogLine(Basis.getBcontext().getString(R.string.netw_err_tcp_io) + e.toString(), LOGTAG, wblogtype.Error);
				return false;
			}

			netwriterthread = new Thread(new NetWriter(dev, TCPsocket), "NetWriter_" + dev.getName());	// NetWriter-Thread erzeugen (übernimmt alles tcp-senden)
			netwriterthread.start();
			dev.setConnected(true);	// Verbindungstatus im Device vermerken

			// Infos von Lok anfordern
			//if (dev.getDev_protocol() == WBprotokoll.WBcontrol)
			//{
				if (dev.getTyp() == DeviceType.Lok)
				{
					dev.cmdSendInit();		// Konfiguration abfragen (<hwget><swget><fpwmget>)
				}
			//}

				/*
			else // derzeit nur WLAN_DL
			{
				dev.Netwrite("i19");         // sonst geht nichts
				dev.Netwrite("*");         // umschalten auf mehr Output
			} */
		}
			
			return true;
	}
	
		
	
	public void Disconnect() throws IOException 
	{		
		
		if (dev.isTryreconnect())
		{
			//sendMsgToUIthread(wb.control.fragments.Frag_control.MSG_DEVICE_TRY_RECONNECT);
            dev.sendStdLocBroadcast(Basis.ACTION_DEVICE_TRY_RECONNECT);
		}
		else
		{
			// Msg an Control, dass Device getrennt wurde (bzw. wird) (für den Fall, dass die Verbindung unterbrochen oder die Lok ausgeschalten wurde)
			//sendMsgToUIthread(wb.control.fragments.Frag_control.MSG_DEVICE_DISCONNECTED);
            dev.sendStdLocBroadcast(Basis.ACTION_DEVICE_DISCONNECTED);
			dev.setConnected(false);	// Verbindungstatus im Device vermerken
		}
		
		
		try {
			netwriterthread.join();	// warten, bis NetWriter beendet wurde
		} catch (InterruptedException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
		
		try
		{
			if (TCPsocket != null)
			{			
				if (input != null) { input.close(); }
				TCPsocket.close();
				TCPsocket = null;	// socket kann nicht wiederverwendet werden -> neue Instanz muss erstellt werden
			}
		}
		catch (IOException e)
		{
			Basis.AddLogLine(Basis.getBcontext().getString(R.string.netw_err_closing_tcp) + e.toString(), LOGTAG, wblogtype.Error);
		}
	}
	
	
	// falls sich Device länger nicht meldet, wird ein reconnect versucht
	private void reconnect()
	{
		try {
			Disconnect();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
		boolean time_over = false;
		int counter = 0;
		int waittime = 200;	// mit 200ms starten
		
		do
		{
			if (connect()) { time_over = true; }
			else // if connect fails
			{ 
				try { Thread.sleep(100); } // 100ms warten 
				catch (InterruptedException e) { /* ignore error */ }
			}
			counter++;
			if (counter > 10)
			{
				waittime = 500; // TODO: was soll mit der waittime gemacht werden?
			}
			
			if (counter > 20) 
			{ 
				time_over = true; // nach 20 Versuchen aufgeben!!	// TODO: könnte zu wenig sein
				//sendMsgToUIthread(wb.control.fragments.Frag_control.MSG_DEVICE_DISCONNECTED);
                dev.sendStdLocBroadcast(Basis.ACTION_DEVICE_DISCONNECTED);
				dev.setConnected(false);	// Verbindungstatus im Device vermerken
				dev.setTryreconnect(false);
			}	
			
		} while (!time_over);

	
	}
	
	
	private void doTCPwork()
	{
		Boolean end = false;
		Boolean worktodo = false;
		int receive_count = 512;    // Anzahl der Zeichen pro Lesevorgang
		char[] buf = new char[receive_count];	// Empfangspuffer
		// bool protocol_check = true; // wird später benötigt
		String test = "";	// empfangener, auswertbarer Text
		
		// Verbindung checken?
		if (dev == null) { end = true; }

		while (!end)
        {
			if (dev.isTryreconnect()) { reconnect(); }
			worktodo = false;
			end = dev.getExitThread();	// Thread-Exit wird durch Variable im Device signalisiert

			Boolean connected = true;
			try
			{
				connected = TCPsocket.isConnected();
			}
			catch (Exception e)
			{
				connected = false;
				end = true;
			}
			
			if (connected && !end)	// ende prüfen (Netwaiter wird bei device.connect() gestartet
			{										// und soll beendet werden, wenn die Verbindung getrennt wurde
				// buf leeren ?
				int gelesen = 0;
				try {
					gelesen = input.read(buf, 0, receive_count);

				} catch(SocketTimeoutException e) {
					// ignorieren -> passiert sekündlich

				} catch (IOException e) {
					// nur wenn's keine SocketTimeoutException ist
					Basis.AddLogLine(Basis.getBcontext().getString(R.string.netw_err_reading) + e.toString(), LOGTAG, wblogtype.Error);
				}	// returns the # of chars read, or -1 if nothing to read


				if (gelesen > 0)
				{
					dev.setLastlokstatus(System.currentTimeMillis());	// aktuelle Zeit speichern, damit überprüft werden kann, ob Meldungen von der lok ausbleiben
					String newtxt = new String(buf, 0, gelesen);
					test += newtxt;
					// Basis.AddLogLine(String.format(Basis.getBcontext().getString(R.string.netw_dev_data), dev.getName(), test), LOGTAG, wblogtype.Info);
				}
				
				if (test.length() > 0) { worktodo = true; }
			}
			else { end = true; }	// aus Schleife aussteigen, wenn Device getrennt ist
			
			while (worktodo)
			{
				int startpos = test.indexOf("<");
				int endpos = test.indexOf(">");

				// Prüfung auf WBprotokoll.WBcontrol
				if ((startpos >= 0) && (endpos > 0) && (startpos < endpos))  // gültiger WBcontrol-Befehl ist enthalten -> Protokoll = WBcontrol
				{
					if ((dev.getDev_protocol() == WBprotokoll.unbekannt)|| (dev.getDev_protocol() == WBprotokoll.WLAN_DL))
					{ dev.setDev_protocol(WBprotokoll.WBcontrol); }
					//protocol_check = false;
					// Basis.AddLogLine(dev.getName() + ": " + Basis.getBcontext().getString(R.string.proto_wbc), LOGTAG, wblogtype.Info);
				}
				else  // wenn kein kompletter Command mehr im string "test" enthalten ist
				{
					if (dev.getDev_protocol() == WBprotokoll.WBcontrol) { worktodo = false; }
					
					if ((startpos == -1) && (endpos == -1))	//kein < oder > -> WLAN-DL
					{
						/*
						if (dev.getDev_protocol() == WBprotokoll.unbekannt) 
						{ 
							// hier die Initialisierung
							dev.setDev_protocol(WBprotokoll.WLAN_DL);
							dev.Netwrite("i19");         // sonst geht nichts
							// dev.Netwrite("*");         // umschalten auf mehr Output
						} */
					}
				}
                
                String command = "";
                
                if (worktodo)
                {
                	if (dev.getDev_protocol() == WBprotokoll.WBcontrol)  // bei win noch Basis.Protokoll
                	{
                		command = test.substring(startpos + 1, endpos);	// substring von start to end-1 !!!!!
                		test = test.substring(endpos+1);	//.Remove(startpos, endpos - startpos + 1);   // aktuellen command aus test entfernen Error: Count bei Remove um 1 zu klein!!
                		WBcontrol_BefehlAuswerten(command);
                	}

                	if (dev.getDev_protocol() == WBprotokoll.WLAN_DL)
                	{
                		dev.setStatustxt(new String(test));
                		test = "";
                		worktodo = false;	// nichts mehr zu tun

                	}

                	if (dev.getDev_protocol() == WBprotokoll.unbekannt)  // bei win noch Basis.Protokoll
                	{
                		worktodo = false;	// derzeit keine auswertung
                	}
                }
            }
        }	// end while (!end)

		Basis.AddLogLine(dev.getName() + Basis.getBcontext().getString(R.string.netw_loop_exit), LOGTAG, wblogtype.Info);
		try {
				Disconnect();
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
	}
	

	private void WBcontrol_BefehlAuswerten(String command) {
		//Basis.AddLogLine(Basis.getBcontext().getString(R.string.gen_cmd_check) + command, LOGTAG, wblogtype.Info);
        String[] cmdparts = command.split(":"); // wenn kein : enthalten ist, dann ist cmdparts[0] = command, cmdparts.length=1

        if (cmdparts.length == 0) { return; }
        
        if (cmdparts[0].equals("sd"))   // Speed-Meldung  0: "speed", 1: "nnn" Speedzahl
        {
            int newts = 0;
            try
			{
				newts = Integer.parseInt(cmdparts[1]);
			}
            catch(NumberFormatException nfe)
            {
            	Basis.AddLogLine(Basis.getBcontext().getString(R.string.netw_err_speed_conversion) + "String=" + cmdparts[1], LOGTAG, wblogtype.Error);
            }

            if (dev.getTrainspeed() != newts) { dev.setTrainspeed(newts);  }
        }


        else if ((cmdparts.length == 3) && (cmdparts[0].equals("ui")))  // Meldung eines Spannungswertes
        {
            int index = 0xFFFF; // ungültiger Wert
            int value = 0xFFFF; // ungültiger Wert

            try { index = Integer.parseInt(cmdparts[1]); }
            catch(NumberFormatException nfe)
            {
                Basis.AddLogLine(String.format(Basis.getBcontext().getString(R.string.netw_err_adc_conversion1), cmdparts[1]), LOGTAG, wblogtype.Error);
            }
            try { value = Integer.parseInt(cmdparts[2]); }
            catch(NumberFormatException nfe)
            {
                Basis.AddLogLine(String.format(Basis.getBcontext().getString(R.string.netw_err_adc_conversion2), cmdparts[1]), LOGTAG, wblogtype.Error);
            }

            if (index != 0xFFFF) { dev.setU(index, value); } // TODO: Lese/Schreib-Zugriff noch absichern? im Device?
        }


        else if ((cmdparts.length == 3) && (cmdparts[0].equals("iam")))   // Identifikationsmeldung (der Lok)  0: "iam", 1: typ (int) (1 für Lok) 2: Name der Lok
        {
            if (!cmdparts[2].equals(dev.getName()))	// Namens-Check
            {
                if (dev.getName().startsWith("unbekannt"))    // bei manuell verbundenem Device ist der Name "unbekannt"
                {
                    String oldname = dev.getName();
                    dev.setName(cmdparts[2]);  // den richtigen Namen jetzt setzen
					// TODO: checken, ob's den neuen Loknamen schon gibt -> auf ander Lok umswitchen?
                    Intent dcIntent = new Intent(Basis.ACTION_DEVICE_NAME_CHANGED); // TODO: überall einbauen control, loklist, lokdetails
                    dcIntent.putExtra("device", dev.getName());
                    dcIntent.putExtra("oldname", oldname);
                    Basis.getLocBcManager().sendBroadcast(dcIntent);
                }
				// TODO: Typ überprüfen??

                else // wenn schon ein Name existiert -> Problem melden
                {
                    dev.setNameForChange(cmdparts[2]);
                    dev.sendStdLocBroadcast(Basis.ACTION_DEVICE_NEW_NAME);  // Aktionen zur Lösung des Namenskonflikts -> User fragen (im control
                    // Meldung ausgeben, dass sich das Device mit einem anderen Namen gemeldet hat
                    Basis.AddLogLine(String.format(Basis.getBcontext().getString(R.string.netw_err_devname_dup), dev.getName(), cmdparts[2]), LOGTAG, wblogtype.Warning);
                }
            }
        }

        else if (cmdparts[0].equals("pauseall"))   // alle Geräte sollen stehenbleiben/weiterfahren
        {
            if (cmdparts.length == 1)   // stehenbleiben
            {
                if (!Basis.getPauseAll())
                {
                    Basis.setPauseAll(true);
	                    Basis.setPauseAllSender(dev.getName());	// den Sender des Befehls merken, nur er darf ihn aufheben

                    for (Device d : Basis.getDevicelist())
                    {
                        if (d.getTyp() == DeviceType.Lok) { d.setPause(true); }    // status Pause für Device setzen
                    }
                }
            }
            else if ((cmdparts.length == 2) && (cmdparts[1].equals("aus")))    // Pause aufgehoben -> Geräte fahren weiter
            {
                if (Basis.getPauseAll())
                {
                    if (Basis.getPauseAllSender().equals(dev.getName()))
                    {
                        Basis.setPauseAll(false);

                        for (Device d : Basis.getDevicelist())
                        {
                            if (d.getTyp() == DeviceType.Lok)  { d.setPause(false); }  // status Pause für Device setzen
                        }
                    }
                    else
                    {
                        //Verweigert: Gerät xy wollte einen Befehl <pauseall> aufheben, der nicht von ihm stammt!
                        Basis.AddLogLine(String.format(Basis.getBcontext().getString(R.string.err_pauseall_illegal_end),dev.getName()), LOGTAG, wblogtype.Warning);
                    }
                }
                else
                {
                	Basis.AddLogLine(Basis.getBcontext().getString(R.string.err_pauseall_end_wo_start) + "(" + dev.getName() + ")", LOGTAG, wblogtype.Warning);
                }
            }
        }

        else if (cmdparts[0].equals("stopall"))   // alle Geräte sollen stehenbleiben und sich wenn möglich ausschalten
        {
            if (!Basis.getStopAll())
            {
            	Basis.setStopAll(true);
            }
        }

        else if (cmdparts[0].equals("pong"))   // Rückmeldung zum ping
        {
            long PingLaufzeit = System.currentTimeMillis() - dev.getPingstart();

            if (dev.getIscd())  // wenn des das Device für die Speedbar ist
            {
            	Basis.setStatusText("Ping: " + String.valueOf(PingLaufzeit) + "ms");
                Basis.AddLogLine(String.format(Basis.getBcontext().getString(R.string.netw_log_ping), dev.getName(), PingLaufzeit), LOGTAG, wblogtype.Info);
            }
        }

		else if ((cmdparts.length == 3) && (cmdparts[0].equals("error")))    // Error Rückmeldung
		{
			if (cmdparts[1].equals("motor"))
			{
				int error;
				try	{ error = Integer.parseInt(cmdparts[2]); }
				catch (NumberFormatException e) { error = 0; }

				int error_m1,error_m2;
				error_m1 = error & 3;	// bit 1,2
				error_m2 = error & 12;	// bit 3,4

				String msgtxt = String.format(Basis.getBcontext().getString(R.string.netw_msg_motorerror), error, error_m1, error_m2);
				if (error > 0)
				{
                    dev.setSpeed(0);
                    Basis.AddLogLine(msgtxt, LOGTAG, wblogtype.Info);
                }
				dev.setMotorerror(error);

                Intent dcIntent = new Intent(Basis.ACTION_DEVICE_MOTORERROR);
                dcIntent.putExtra("device", dev.getName());
                dcIntent.putExtra("error", error);
                Basis.getLocBcManager().sendBroadcast(dcIntent);
			}
		}


        else if ((cmdparts.length == 2) && (cmdparts[0].equals("rfid")))   // RFID-Meldung  0: "rfid", 1: rfid-code
        {
            //string[] rfid = command.Split(':');    // 
            int tagindex = Basis.getWbeventlistIndexByName(cmdparts[1]);
            
            if (tagindex != -1) // wenn tag bereits bekannt ist
            {
                if (!dev.getName().equals(""))
                {
                    int csindex = Basis.getWbeventlist().get(tagindex).getCommandlistIndexByDeviceName(dev.getName());
                    if (csindex != -1)
                    {
                        // string comtxt = Basis.WBeventlist[tagindex].commandlist[csindex].Commands.ToString();
                    	String comtxt = Basis.getWbeventlist().get(tagindex).getCommandlist().get(csindex).getCommandsString();
                        String[] commands = comtxt.split(">");  //zur Sicherheit trennen -> Achtung: abschließendes ">" wieder anhängen
                        // unter win wurde hier noch mal die Netzverbindung gecheckt // if (this.tcpClient.Client.Connected)    // ?
                        
                            for (String c : commands)      // alle vorhandenen commands an Lok senden
                            {
                               dev.Netwrite(c + ">");
                            }
                    }
                }
            }
            else // tag ist noch nicht bekannt -> tag merken
            {
                Basis.AddRFID("*" + cmdparts[1], cmdparts[1]);
            }

            if ((Basis.getUseServer()) && (Basis.getServer() != null))    // wenn Server benutzt werden soll & einer verbunden ist
            {
                Basis.getServer().Netwrite("<" + cmdparts[0] + ":" + dev.getName() + ":" + cmdparts[1] + ">");
            }

            Basis.AddLogLine(String.format(Basis.getBcontext().getString(R.string.netw_rec_rfid), dev.getName(), cmdparts[1]), LOGTAG, wblogtype.Info);
        }

        else if ((cmdparts.length == 2) && (cmdparts[0].equals("swi")))    // <swi:*text*> Loksoftware Name von einem Device 
        {
            dev.setDev_swname(cmdparts[1]);
        }

        else if ((cmdparts.length == 2) && (cmdparts[0].equals("swvi")))    // <swvi:*text*> Loksoftware Version von einem Device 
        {
            dev.setDev_swversion(cmdparts[1]);
        }

        else if ((cmdparts.length == 2) && (cmdparts[0].equals("owneri")))    // <owneri:*name*> Device meldet seinen eingetragenen Besitzer 
        {
            dev.setDev_owner(cmdparts[1]);
        }

		else if ((cmdparts.length == 2) && (cmdparts[0].equals("ntypi")))    // <ntypi:*n*> Lok meldet ihren Netzwerk-Typ (siehe Device: Definitionen)
		{
            int nettype = Integer.parseInt(cmdparts[1]);

            if ((nettype >= 1) && (nettype <= 2)) { dev.setLok_nettype(nettype); }
            else
            {
                Basis.AddLogLine(String.format(Basis.getBcontext().getString(R.string.netw_ntype_wrong), dev.getName(), nettype), LOGTAG, wblogtype.Error);
            }
		}

        else if ((cmdparts.length == 2) && (cmdparts[0].equals("typi")))    // <typi:*text*> Lok meldet ihren Lok-Typ (standardisierter Name des Typs
        {
            dev.setDev_modelname(cmdparts[1]);
        }

        else if ((cmdparts.length == 2) && (cmdparts[0].equals("fpwmi")))    // <fpwmi:*n*> 1: n = 1-9
        {
            dev.setLok_pwmfrequenz(Integer.parseInt(cmdparts[1]));
        }

        // TODO: später: <fpwmri:*>  Lok meldet den möglichen PWM-Frequenz Range [Hz].
        // Entweder als Werte zur auswahl: <fpwmri:wert1:wert2:..>
        // oder als Range: <fpwmri:wert1-wert2> Wert1 = anfangswert, wert2 = endwert = int Frequenz in [Hz].

        /*
        else if ((cmdparts.length == 2) && (cmdparts[0].equals("fpwmri")))
        {

            String[] fs;    // Array zum Auswerten
            ArrayList<int> fsint = new ArrayList<int>();    // List mit den Ergebnissen
            dev.LokPWMfRange.clear();  // alte Werte löschen

            if (cmdparts[1].contains(':'))  // Aufzählung
            {
                fs = cmdparts[1].split(':');
                for (String f : fs)  { dev.LokPWMfRange.Add(Int32.Parse(f));  }
            }
            else if (cmdparts[1].contains('-')) // Range: Werte in [Hz] zur Auswahl oder Bereich Wert[0] = Anfangswert, Wert[1] = 0 (=Kennzeichnung), Wert[2] = Endwert 
            {
                fs = cmdparts[1].split('-');
                if (fs.Length == 2)
                {
                    dev.LokPWMfRange.Add(Integer.parseInt(fs[0]));  // Anfangswert
                    dev.LokPWMfRange.Add(0);                   // Wert 0 ist die Kennung für Range
                    dev.LokPWMfRange.Add(Integer.parseInt(fs[1]));  // Anfangswert
                }
            }
        } */

		else if ((cmdparts.length == 5) && (cmdparts[0].equals("gpioi"))) // <gpioi:port:mögliche:verwendete:werte>	char:byte-mask:byte-mask:byte-mask
		{
			int usable = 0;
			int used = 0;
			int error = 0;
			int values = 0;

            Log.d("CMD : ", command);   //TODO: test wieder weg

			try { usable = Integer.parseInt(cmdparts[2]); }
			catch(NumberFormatException nfe)
			{
				Basis.AddLogLine(String.format(Basis.getBcontext().getString(R.string.netw_err_gpio_conversion), cmdparts[2]), LOGTAG, wblogtype.Error);
				error = 1;
			}

			try { used = Integer.parseInt(cmdparts[3]); }
			catch(NumberFormatException nfe)
			{
				Basis.AddLogLine(String.format(Basis.getBcontext().getString(R.string.netw_err_gpio_conversion), cmdparts[3]), LOGTAG, wblogtype.Error);
				error = 1;
			}

            try { values = Integer.parseInt(cmdparts[4]); }
            catch(NumberFormatException nfe)
            {
                Basis.AddLogLine(String.format(Basis.getBcontext().getString(R.string.netw_err_gpio_conversion), cmdparts[4]), LOGTAG, wblogtype.Error);
                error = 1;
            }

			if ((usable < 1) || (usable > 255)) { error = 1; }	// es müssen usable vorhanden sein
			if ((used < 0) || (used > 255)) { error = 1; }
            if ((values < 0) || (values > 255)) { error = 1; }

			if (error == 0) { dev.checkATMegaGpioPort(cmdparts[1], usable, used, values); }	// Port nur erstellen, wenn alle Daten gültig sind
		}

		else if ((cmdparts.length == 2) && (cmdparts[0].equals("hwi"))) // <hwi:n> Device gibt ein Hardware bekannt
		{
            int id = 0;
            boolean error = false;
			try { id = Integer.parseInt(cmdparts[1]); }
			catch(NumberFormatException nfe)
			{
				Basis.AddLogLine(String.format(Basis.getBcontext().getString(R.string.netw_err_hwid_conversion), cmdparts[4]), LOGTAG, wblogtype.Error);
                error= true;
			}

            if (!error) { dev.addHardware(id); }    // TODO: erromeldung bei returnwert false = id ungültig
		}

        else if (cmdparts[0].equals("log")) // TODO: sinnvoll anzeigen
        {
            Log.d("Log von Lok: ", cmdparts[1]);
            dev.setStatustxt(cmdparts[1]);
            Intent dcIntent = new Intent(Basis.ACTION_UPDATE_STATUSTXT);
            Basis.getLocBcManager().sendBroadcast(dcIntent);
        }

		/*
        else if ((cmdparts.length == 2) && (cmdparts[0].equals("have"))) // <have:> Device gibt ein optionales Property bekannt. 1: Propertykennung
		{
			dev.getOpt_deviceproperties().put(cmdparts[1], "");		// Key=Kennung, Value = leer
		} */


        else // unbekannter Befehl
        {
            Basis.AddLogLine(dev.getName() + ": " + Basis.getBcontext().getString(R.string.err_cmd_unknown) + command, LOGTAG, wblogtype.Warning);
        }
		
	}

}
