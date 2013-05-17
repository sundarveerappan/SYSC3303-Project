import java.io.IOException;
import java.net.*;

//import javax.swing.*;


public class Server {
	public static final int WELL_KNOWN_PORT = 69;
	public static final int BUFFER_SIZE = 512+4;
	//public static final byte DEFAULT = 0;//Default Server will be type 0

	
	private DatagramSocket wellKnown;

	//private byte threadMode;
	
	public Server() {
		try {
			wellKnown = new DatagramSocket(WELL_KNOWN_PORT);
		} catch (SocketException e) {
			e.printStackTrace();
			System.exit(1);
		}
	}
	
	
	public DatagramPacket recieveTFTP() {
		byte data[] = new byte[BUFFER_SIZE];
		DatagramPacket incomingPacket = new DatagramPacket(data, data.length);
		
		try {
			wellKnown.receive(incomingPacket);
		} catch (IOException e) {
			e.printStackTrace();
			System.exit(1);
		}
		return incomingPacket;
	}
	
	public static void main(String [] args) {
		Server s = new Server();
		for(;;) {
			Thread st = new Thread(new ServerThread(s.recieveTFTP()));
			st.start();
		}
	}
} 