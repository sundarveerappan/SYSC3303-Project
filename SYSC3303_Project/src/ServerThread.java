import java.io.IOException;
import java.net.*;


public class ServerThread extends Thread{
	public static enum Request {ERROR, READ, WRITE};
	
	private DatagramPacket request;
	private DatagramSocket socket;
	private InetAddress ip;
	private int port;
	private String file;
	private String mode;
	private Request requestType;
	
	public ServerThread(DatagramPacket request) {
		this.request = request;
		processRequest();
	}
	
	private void parseRequest() {
		int length  = this.request.getLength();
		byte data[] = this.request.getData();
		this.ip = this.request.getAddress();
		this.port = this.request.getPort();
		
		if (data[0]!=0) requestType = Request.ERROR;
		else if (data[1]==1) requestType = Request.READ;
		else if (data[1]==2) requestType = Request.WRITE;
		else requestType = Request.ERROR;
		
		if (requestType!=Request.ERROR) {
			//find filename
			int fileCount;
			for(fileCount = 2; fileCount < length; fileCount++) {
				if (data[fileCount] == 0) break;
			}
			if (fileCount==length) requestType=Request.ERROR;
			else file = new String(data,2,fileCount-2);
			
			//find mode
			int modeCount;
			for(modeCount = fileCount+1; modeCount < length; modeCount++) {
				if (data[modeCount] == 0) break;
			}
			if (fileCount==length) requestType=Request.ERROR;
			else mode = new String(data,fileCount,modeCount-fileCount-1);
			
			if(modeCount!=length-1) requestType=Request.ERROR;
		}
	}
	
	private void sendData(byte data[]) {
		DatagramPacket temp = new DatagramPacket(data,data.length,ip,port);
		try {
			socket.send(temp);
		} catch (IOException e) {
			e.printStackTrace();
			System.exit(1);
		}
	}
	
	private void handleRead() {
		//TODO: Implement real read method
		/*NOTE: This is a test method filler simply
		 * replying with the appropriate request
		 * as per SYSC 3303 assignment 1
		 */
		byte data[] = {0, 3, 0, 1};
		sendData(data);
	}
	
	private void handleWrite() {
		//TODO: Implement real write method
		/*NOTE: This is a test method filler simply
		 * replying with the appropriate request
		 * as per SYSC 3303 assignment 1
		 */
		byte data[] = {0, 4, 0, 0};
		sendData(data);
	}
	
	private void handleError() {
		//TODO: Implement real error method
		/*NOTE: This is a test method filler simply
		 * replying with the appropriate request
		 * as per SYSC 3303 assignment 1
		 */
		byte data[] = {0, 5};
		sendData(data);
	}
	
	public void processRequest() {
		parseRequest();
		
		try {
			socket = new DatagramSocket();
		} catch (SocketException e) {
			e.printStackTrace();
			System.exit(1);
		}
		
		if (requestType==Request.READ) {
			//handle read request
			handleRead();
		} else if (requestType==Request.WRITE) {
			//submit write request
			handleWrite();
		} else {
			//submit invalid request
			handleError();
		}
	}
}
