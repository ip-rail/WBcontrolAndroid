package wb.control;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.SocketException;
import java.net.SocketTimeoutException;
import java.net.UnknownHostException;
import java.util.Arrays;

import wb.control.Basis.WBprotokoll;
import wb.control.Device.DeviceType;
import wb.control.WBlog.wblogtype;
import wb.control.fragments.Frag_control;
import android.app.Service;
import android.content.Intent;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;

public class UDPWaiter extends Service {

	private Thread UDPwaiterThread;
	public Handler UDPhandler;
	private Boolean end;
	private DatagramSocket UDPsocket = null;
	private InetAddress broadcastAdr = null;
	final static String LOGTAG = "udpwaiter";
	
	@Override
	public IBinder onBind(Intent intent) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public void onCreate() {
		super.onCreate();
		
			StartUDP();
	}



	@Override
	public void onDestroy() {

		end = true;		// dem thread das Beenden signalisieren!

		super.onDestroy();
		Basis.AddLogLine(getString(R.string.udpw_svc_end), LOGTAG, wblogtype.Info);
		
	}
	
	//########################################################################
	
	
	private void StartUDP() {
		
		UDPwaiterThread = new Thread(new Runnable() { 
			public void run() {

				boolean connected = false;
				try {
					connected = connectUDP();
					if (connected) { doUDPwork(); }
					else { onDestroy(); }

				} catch (IOException e) {
					Basis.AddLogLine(e.toString(), LOGTAG, wblogtype.Error);
				}

			} 
		}, "UDPwaiter"); 

		UDPwaiterThread.start();
		Basis.setUDPwaiterThread(UDPwaiterThread);	// in Basis speichern
		Basis.AddLogLine(getString(R.string.udpw_thread_start), LOGTAG, wblogtype.Info);

	}	// end StartUDP()
	
	
	private boolean connectUDP()
	{
		boolean success = true;
		//String eigeneIP = Basis.getEigeneIP();		// nicht threadsafer Aufruf
		int udpport = Basis.getUdpPort();		// nicht threadsafer Aufruf
		String controllername = Basis.getName();	// nicht threadsafer Aufruf

		try {
			String bip = Basis.getBroadcastip();	// Proadcast-IP wurde beim Start von Basis-Service ermittelt
			if ((bip == null) || (bip.equals(""))) { bip = "10.0.0.255"; }	// im Fehlerfall mit meiner Standard broadcast-ip weitermachen
			broadcastAdr = InetAddress.getByName(bip);

			Basis.AddLogLine(getString(R.string.gen_connecting), "udp", wblogtype.Info);

			UDPsocket = new DatagramSocket(udpport);	// UDPsocket
			if (UDPsocket == null) { Basis.AddLogLine(getString(R.string.err_socket_creation), LOGTAG, wblogtype.Warning); }
			Basis.setUDPsocket(UDPsocket);	// in Basis speichern für Basis.SendUDP(String data)
			UDPsocket.setBroadcast(true);
			UDPsocket.setSoTimeout(1000);		// Timeout in [ms] for blocking accept or read/receive operations (but not write/send operations). A timeout of 0 means no timeout.

			String data = "<iamcontrol:" + controllername + ">";
			DatagramPacket packet = new DatagramPacket(data.getBytes(), data.length(), broadcastAdr, udpport);
			UDPsocket.send(packet);		// i-am-new Medung an alle, dass der Controller seinen Betrieb aufgenommen hat
		}
		catch (UnknownHostException e1) {	Basis.AddLogLine(e1.toString(), LOGTAG, wblogtype.Error); success = false;	}
		catch (SocketTimeoutException e1) {	Basis.AddLogLine(e1.toString(), LOGTAG, wblogtype.Error); success = false;	}
		catch (SocketException e1) { Basis.AddLogLine(e1.toString(), LOGTAG, wblogtype.Error);  success = false;	}
		catch (IOException e1) {	Basis.AddLogLine(e1.toString(), LOGTAG, wblogtype.Error);  success = false;	}

		return success;
	}
			
	
	public void SendUDP(String data) {

		if (UDPsocket != null)
		{
			DatagramPacket packet = new DatagramPacket(data.getBytes(), data.length(), broadcastAdr, Basis.getUdpPort());

			try {
				UDPsocket.send(packet);
			} catch (IOException e) {
				Basis.AddLogLine(getString(R.string.err_sending) + e.toString(), LOGTAG, wblogtype.Error);
			}
		}
	}
	
	
	private void doUDPwork() throws IOException {

		end = false;
		byte[] buf = new byte[8192];	// Empfangspuffer
		String test = "";	// empfangener, auswertbarer Text
		String senderIP = "";
		Boolean timeout = false;

		while (!end && (UDPsocket != null) && (!UDPsocket.isClosed()))
		{
			Arrays.fill(buf, (byte) 0);	// buffer leeren
			timeout = false;
			DatagramPacket packet = new DatagramPacket(buf, buf.length);
			try
			{
				UDPsocket.receive(packet);
			}
			catch (SocketTimeoutException e) // timeout abfangen, falls nichts gesendet wird
			{
				timeout = true;
			}

			if (!timeout)
			{
				senderIP = packet.getAddress().getHostAddress();	// packet.getAddress().toString();
				String newtxt = new String(buf, 0, packet.getLength());
				//String newtxt3 = new String(packet.getData());		// liefert den gesamten buffer als String zurück (mit 8000 Leerzeichen)
				Boolean worktodo = true;

				if (!senderIP.equals(Basis.getEigeneIP()))		// UDP-Meldungen vom eigenen Gerät ignorieren
				{
					test += newtxt;
					// String test_raw = new String(test);	// den aktuellen buffer incl. <comand> aufheben für ev. spezial-auswerung
					//Basis.AddLogLine("waiterthread: string geholt: " + test, "udp", wblogtype.Info);
				}
				else { worktodo = false; }	// Befehle vom eigenen Gerät ignorieren
				
				while (worktodo && !end)
				{
					int startpos = test.indexOf("<");
					int endpos = test.indexOf(">");
					
					if (endpos < startpos) { Basis.AddLogLine(getString(R.string.udpw_combuffer_crap) + test, LOGTAG, wblogtype.Warning); }
					if (test.length() > 512) { Basis.AddLogLine(getString(R.string.udpw_combuffer_full), LOGTAG, wblogtype.Warning); }
					
					String command = "";
					
					if ((startpos >= 0) && (endpos > 0) && (startpos < endpos)) // gültiger WBcontrol-Befehl ist vorhanden
					{
						// Protokoll setzen
						if (startpos >= 0)  // Wenn Zeichen "<" enthalten ist -> Protokoll = WBcontrol für das Sender-Device setzen
						{
							Device founddev = null;
							for (Device ding : Basis.getDevicelist())  // checken, ob bereits vorhanden
							{
								if (ding.getIP().equals(senderIP)) { founddev = ding; break; }
							}
							if (founddev != null)
							{ 
								if (founddev.getDev_protocol() == WBprotokoll.unbekannt)
								{
									founddev.setDev_protocol(WBprotokoll.WBcontrol); 
								}
							}	
							Basis.AddLogLine(getString(R.string.proto_wbc), LOGTAG, wblogtype.Info);
						}
						
						command = test.substring(startpos + 1, endpos);	// substring von start to end-1 !!!!!
						if (test.length() > endpos+1) { test = test.substring(endpos+1); }
						else { test = ""; }
						WBcontrol_BefehlAuswerten(command, senderIP);
					}
					else  // wenn kein kompletter Command mehr im string "test" enthalten ist
					{
						worktodo = false;

					}
				}
			} // end if (!timeout)

		}	// end while (!end)
		Basis.AddLogLine(getString(R.string.udpw_wloop_exit), LOGTAG, wblogtype.Info);
		// Aufräumarbeiten
		if (UDPsocket != null) { UDPsocket.close(); }
		Basis.AddLogLine(getString(R.string.udpw_thread_exit), LOGTAG, wblogtype.Info);

		// msg an endhandler, dass thread beendet ist
		
	}	// end doUDPwork()
	
	private void WBcontrol_BefehlAuswerten(String command, String sender_ip) throws IOException  // wertet den übergebenen Befehl aus
    {
		Basis.AddLogLine(getString(R.string.gen_cmd_check) + command, LOGTAG, wblogtype.Info);

        if (command.startsWith("iam:"))  // c[0]: "iam", c[1]: typ (int) (1 für Lok) c[2]: Name der Lok
        {
            String[] c = command.split(":");

			if ((c.length == 3) && c[1].equals("1"))	// Lok
			{
				Boolean found = false;
				for (Device ding : Basis.getDevicelist())  // checken, ob bereits vorhanden
				{
					if ((ding.getTyp() == DeviceType.Lok) && (ding.getName().equals(c[2]))) {
						found = true;
					}
				}
				if (!found) {
					// zuest noch noch nach unbekannt+ip suchen, und ggf. dort Namen anpassen!
					for (Device ding : Basis.getDevicelist())  // checken, ob bereits vorhanden
					{
						if ((ding.getTyp() == DeviceType.Lok) && (ding.getName().equals("unbekannt " + sender_ip))) {
							found = true;
							ding.setName(c[2]);   // Namen korrigieren
						}
					}

					if (!found) // wenn auch kein passender unbekannter gefunden wurde -> neues Gerät hinzufügen
					{
						Device newdev = new Device(c[2], DeviceType.Lok, sender_ip);
						newdev.setDev_protocol(WBprotokoll.WBcontrol);    // wenn's über "iamlok" gekommen ist, muss es das WBcontrol-Protokoll sein
						Basis.AddDevice(newdev);

						Intent dcIntent = new Intent(Basis.ACTION_DEVICELIST_CHANGED);
						Basis.getLocBcManager().sendBroadcast(dcIntent);
					}
				}
				Basis.AddLogLine(getString(R.string.msg_from_loco) + c[2], LOGTAG, wblogtype.Info);
			}
        }

        else if (command.startsWith("iamserver:"))  // c[1] = servername	// TODO: ändern -> bei iam: integrieren!!
        {
            String[] c = command.split(":");
            Boolean found = false;
            for (Device ding : Basis.getDevicelist())  // checken, ob bereits vorhanden
            {
                if ((ding.getTyp() == DeviceType.Server) && (ding.getName().equals(c[1]))) { found = true; }
            }
            if (!found)
            {
            	Device dev = new Device(c[1], DeviceType.Server, sender_ip);
                Basis.AddDevice(dev);
                if ((Basis.getUseServer()) && (Basis.getServer() == null))  // es soll ein Server verwendet werden, aber noch keiner ausgewählt
                {
                	Basis.setServer(dev);
                	dev.Connect();	// tcp-Verbindung mit Server herstellen
                }
            }
        }

            else if (command.equals("pauseall"))   // alle Geräte sollen stehenbleiben
            {
                if (!Basis.getPauseAll())
                {
                    Basis.setPauseAll(true);

                    for (Device ding : Basis.getDevicelist()) // sender ermitteln
                    {
                        if (ding.getIP().equals(sender_ip))
                        {
                            Basis.setPauseAllSender(ding.getName());  // den Sender des Befehls merken, nur er darf ihn aufheben
                        }
                    }

                    for (Device d : Basis.getDevicelist())
                    {
                        if (d.getTyp() == DeviceType.Lok) { d.setPause(true); }     // status Pause für Device setzen
                    }
                }
            }

            else if (command.equals("pauseall:aus"))   // Pause aufgehoben -> Geräte fahren weiter
            {
                if (Basis.getPauseAll())
                {
                    String sendername = "";
                    for (Device ding : Basis.getDevicelist())
                    {
                        if (ding.getIP().equals(sender_ip)) { sendername = ding.getName(); }  // den Sender des Befehls merken, nur er darf ihn aufheben
                    }

                    if (Basis.getPauseAllSender().equals(sendername))
                    {
                    	Basis.setPauseAll(false);

                        for (Device d : Basis.getDevicelist())
                        {
                            if (d.getTyp() == DeviceType.Lok) { d.setPause(false); }   // status Pause für Device setzen
                        }
                    }
                    else
                    {
                    	Basis.AddLogLine(String.format(getString(R.string.err_pauseall_illegal_end), sendername), LOGTAG, wblogtype.Warning);
                    }
                }
                else
                {
                	// Ein Gerät wollte einen Befehl <pauseall> aufheben, der nicht empfangen wurde!
                	Basis.AddLogLine(getString(R.string.err_pauseall_end_wo_start), LOGTAG, wblogtype.Warning);
                }
            }

            else if (command.equals("stopall"))   // alle Geräte sollen stehenbleiben und sich wenn möglich ausschalten
            {
                if (!Basis.getStopAll())	{ Basis.setStopAll(true); }
            }
    
            else  // Befehl unbekannt
            {
                Basis.AddLogLine(getString(R.string.err_cmd_unknown) + command, LOGTAG, wblogtype.Warning);
            }
    }
	

}	// end class UDPWaiter
