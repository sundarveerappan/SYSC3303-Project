import java.net.*;


public class ServerThread extends Thread{
	private DatagramPacket request;
	private DatagramSocket socket;
	private byte requestType;
	
	public ServerThread(DatagramPacket request) {
		this.request = request;
	}
	
	public void handleRequest() {
		
	}
}
