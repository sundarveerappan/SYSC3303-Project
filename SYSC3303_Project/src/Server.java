import java.io.IOException;
import java.net.*;

//import javax.swing.*;


public class Server {
	public static final int WELL_KNOWN_PORT = 69;
	public static final int BUFFER_SIZE = 512+4;
	//public static final byte DEFAULT = 0;//Default Server will be type 0
	public static final byte READ = 1;
	public static final byte WRITE = 2;
	
	private DatagramSocket wellKnown;
	private DatagramPacket incomingPacket;
	//private byte threadMode;
	
	public Server() {
		try {
			wellKnown = new DatagramSocket(WELL_KNOWN_PORT);
		} catch (SocketException e) {
			e.printStackTrace();
			System.exit(1);
		}
	}
	
	
	public void recieveTFTP() {
		byte data[] = new byte[BUFFER_SIZE];
		DatagramPacket incomingPacket = new DatagramPacket(data, data.length);
		
		try {
			wellKnown.receive(incomingPacket);
		} catch (IOException e) {
			e.printStackTrace();
			System.exit(1);
		}
		//return incomingPacket;
		
		//Create ServerThread()
	}
} 