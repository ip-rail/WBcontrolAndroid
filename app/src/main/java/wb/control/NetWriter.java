package wb.control;

import java.io.BufferedWriter;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.net.Socket;

import wb.control.Basis.WBprotokoll;
import wb.control.WBlog.wblogtype;

public class NetWriter implements Runnable {

	private Device dev;
	private Socket TCPsocket;
	private  BufferedWriter output;
	
	final static String LOGTAG = "netwriter";


	public NetWriter(Device d, Socket tcp)	//Konstruktor zur Übergabe des Devices
	{
		dev = d;
		TCPsocket = tcp;	
	}

	@Override
	public void run() 	// run wird beim Starten des Threads ausgeführt
	{
		try {

			output = new BufferedWriter(new OutputStreamWriter(TCPsocket.getOutputStream()));
		} catch (IOException e) {
			Basis.AddLogLine(Basis.getBcontext().getString(R.string.netwr_err_open_stream) + e.toString(), LOGTAG, wblogtype.Error);
		}

		doWork();	// Haupt-Arbeitsschleife
		
		cleanupBeforeExit();

		// Rest wird erst im NetWaiterThread gemacht
		// Msg an Control, dass Device getrennt wurde (bzw. wird) (für den Fall, dass die Verbindung unterbrochen oder die Lok ausgeschalten wurde)
		//sendMsgToUIthread(wb.control.Frag_control.MSG_DEVICE_DISCONNECTED);
		//dev.setConnected(false);	// Verbindungstatus im Device vermerken
	}


	private void Netwrite(String string) 
	{
		//Boolean error1, error2 = false;
		//if (Basis.getLoglevel() ==1) { Basis.AddLogLine(Basis.getBcontext().getString(R.string.netwr_to_write) + string, LOGTAG, wblogtype.Info); }
		
		// wenn gesendet werden soll und es wurde noch kein WBcontrol-Kommando empfangen -> WLAN_DL
		if (dev.getDev_protocol() == WBprotokoll.unbekannt) { dev.setDev_protocol(WBprotokoll.WLAN_DL); }	//TODO: ändern!
		
		if (dev.getDev_protocol() == WBprotokoll.WLAN_DL) { string += "\n"; }	// TODO bisher: \r\n  Karl: nur \r, bei (wegen) NetIO nur \n

		if ((TCPsocket != null) && (output != null)) 
		{ 
			try 
			{
				output.write(string);
				output.flush();
			}
			catch (IOException e) 
			{
				 Basis.AddLogLine(Basis.getBcontext().getString(R.string.err_writing) + e.toString(), LOGTAG, wblogtype.Error);
				 dev.setExitThread(true);
			}
		}
		else 
		{ 
			Basis.AddLogLine(Basis.getBcontext().getString(R.string.netwr_err_netwrite), LOGTAG, wblogtype.Error);
			dev.setExitThread(true);
		}
	}


	private void doWork()	// Haupt-Arbeits- und Warteschleife des Threads
	{
		Boolean end = false;
		String text;

		while (!end)
		{
			// text = dev.getNextCmdtoSend();
			text = dev.getNextCmdsToSend();  // TODO: Testen
			if (text == null) // kurze Pause einlegen: 50ms
			{ 
				try { Thread.sleep(50); } //TODO: konfigurierbar machen (besser: verlangsamen, wenn sich länger nichts tut)
				catch (InterruptedException e) { e.printStackTrace(); }
			}	
			else { Netwrite(text); }

			if (dev.getExitThread()) { end = true; }	// Thread-Exit wird durch Variable im Device signalisiert
			if (dev.isTryreconnect()) { end = true; }	// auch bei Reconnect muss NetWriter beendet werden!
			
		}	// end while (!end)
		Basis.AddLogLine(dev.getName() + Basis.getBcontext().getString(R.string.netwr_loop_exit), LOGTAG, wblogtype.Info);
	}
	
	
	private void cleanupBeforeExit()
	{
	// vor Ausstieg checken, ob Device gestoppt werden soll (sofern das Netwrite() nicht selbst wg. eines Fehlers den Ausstieg auslöst - dann kann natürlich nicht mehr geschrieben werden)
	
			if (Basis.isStop_on_disconnect()) 	// bei disconnect speed zuerst auf 0 stellen?? -> muss im NetWriter passieren (im Reader weiss man nicht, ob der Writer noch vorhanden ist) (die beiden werden immer zugleich beendet)
			{
				dev.cmdSendStop();
			} 

			if (TCPsocket != null)
			{

				if (output != null) 
				{ 
					try {
						output.flush();
						output.close();
						output = null;
					} catch (IOException e) {
						Basis.AddLogLine(Basis.getBcontext().getString(R.string.netwr_err_ending), LOGTAG, wblogtype.Error);
					}
				}
			}
	}

}
