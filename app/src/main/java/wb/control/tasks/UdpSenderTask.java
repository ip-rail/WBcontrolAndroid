package wb.control.tasks;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.UnknownHostException;

import wb.control.Basis;
import wb.control.WBlog.wblogtype;
import android.os.AsyncTask;

public class UdpSenderTask extends AsyncTask<String, Void, String> {
	
	protected String doInBackground(String... sendtxt) {
		
		String udperror = "";

		DatagramSocket UDPsocket;

		UDPsocket = Basis.getUDPsocket(); // TODO: threadsafe zugreifen


		if (UDPsocket != null)
		{
			InetAddress broadcastAdr = null;
			try {
				broadcastAdr = InetAddress.getByName(Basis.getBroadcastip());
			} catch (UnknownHostException e1) {
				udperror = "Fehler bei SendUDP: Ermitteln der Broadcast-Adresse: " + e1.toString();
			}
			DatagramPacket packet = new DatagramPacket(sendtxt[0].getBytes(), sendtxt[0].length(), broadcastAdr, Basis.getUdpPort());

			try {
				UDPsocket.send(packet);
			} catch (IOException e) {
				udperror = "Fehler beim Senden: " + e.toString();
			}
		}


		return udperror;	// im Erfolgsfall ""
	}

	protected void onPostExecute(String error) {
		
		if (!error.equals(""))
		{
			Basis.AddLogLine(error, "SendUDP", wblogtype.Error);
		}
		
	}
}
