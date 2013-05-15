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
	}
	
	public void parseRequest() {
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
}
