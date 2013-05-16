import java.io.*;
import java.net.*;


public class ServerThread extends Thread{
	public static enum Request {ERROR, READ, WRITE};
	public static final int MESSAGE_SIZE = 512;
	public static final int BUFFER_SIZE = MESSAGE_SIZE+4;
	public static final byte MAX_BLOCK_NUM = 127;
	public static final byte DATA = 3;
	public static final byte ACK = 4;
	
	private DatagramPacket request;
	private DatagramSocket socket;
	private InetAddress ip;
	private int port;
	private String file;
	private String mode;
	private Request requestType;
	
	/**
	 * Constructor for ServerThread
	 * @param request - The initial DatagramPacket request sent from the client
	 */
	public ServerThread(DatagramPacket request) {
		this.request = request;
		processRequest();
	}
	
	/**
	 * Method parses request to determine request type and handles request accordingly
	 */
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
	
	/**
	 * Parses request DatagramPacket and populates instance variables with data
	 */
	private void parseRequest() {
		int length  = this.request.getLength(); //temporarily stores length of request data
		byte data[] = this.request.getData(); //copies data from request
		this.ip = this.request.getAddress(); //stores ip address in instance variable
		this.port = this.request.getPort(); //stores port number in instance variable
		
		if (data[0]!=0) requestType = Request.ERROR; //Makes sure that request data starts with a 0
		else if (data[1]==1) requestType = Request.READ;//Checks if request is a read request
		else if (data[1]==2) requestType = Request.WRITE;//Checks if request is a write request
		else requestType = Request.ERROR;//If not a read or write, sets request type to invalid
		
		if (requestType!=Request.ERROR) {
			//find filename
			int fileCount;//keeps track of position in data array while getting file name
			//finds length of file name (number of bytes between request type and next 0 or end of array)
			for(fileCount = 2; fileCount < length; fileCount++) {
				if (data[fileCount] == 0) break;
			}
			if (fileCount==length) requestType=Request.ERROR;//if there is no zero before the end of the array request is set to Invalid
			else file = new String(data,2,fileCount-2);//Otherwise, filename is converted into a string and stored in instance variable
			
			//find mode
			int modeCount;//keeps track of position in data array while getting encoding mode
			//finds length of encoding mode (number of bytes between request type and next 0 or end of array)
			for(modeCount = fileCount+1; modeCount < length; modeCount++) {
				if (data[modeCount] == 0) break;
			}
			if (fileCount==length) requestType=Request.ERROR;//if there is no zero before the end of the array request is set to Invalid
			else mode = new String(data,fileCount,modeCount-fileCount-1);//Otherwise, filename is converted into a string and stored in instance variable
			
			if(modeCount!=length-1) requestType=Request.ERROR;//Checks that there is no data after final zero
		}
	}
	
	/**
	 * Encloses an array of bytes in a DatagramPacket and sends it to the connected client via this object's default socket
	 * @param data - byte array to be sent
	 */
	private void sendData(byte data[]) {
		//Makes new DatagramPacket to send to client
		DatagramPacket temp = new DatagramPacket(data,data.length,ip,port);
		try {
			//sends packet via default port
			socket.send(temp);
		} catch (IOException e) {
			e.printStackTrace();
			System.exit(1);
		}
	}
	

	/**
	 * handles a read request.  Continually loops, reading in data from selected file,
	 * packing this data into a TFTP Packet,
	 * sending the TFTP Packet to the client,
	 * waiting for a corresponding acknowledgement from client,
	 * and repeating until the entire file is sent
	 */
	private void handleRead() {
		try {
			//Opens an input stream
			BufferedInputStream in = new BufferedInputStream(new FileInputStream(file));
			
			byte blockNumber = 1;//keeps track of current block number
			
			byte[] msg;//buffer used to send data to client
			byte[] data = new byte[MESSAGE_SIZE];//buffer used to hold data read from file
			int n;
			
			//Reads data from file and makes sure data is still read
			while ((n = in.read(data)) != -1) {
				msg = new byte[BUFFER_SIZE];//new empty buffer created
				//first four bits are set to TFTP Requirements
				msg[0] = 0;
				msg[1] = 4;
				msg[2] = 0;
				msg[3] = blockNumber;
				//Data read from file
				System.arraycopy(data,0,msg,4,n);
				sendData(msg);
				
				
				boolean correctBlock = true;
				for(;;) {
					byte comparitor[] = {0,ACK,0,blockNumber};//used to check ack
					byte ack[] = new byte[BUFFER_SIZE];//Ack data buffer
					DatagramPacket temp = new DatagramPacket (ack, ack.length);//makes new packet to receive ack from client
					try {
						socket.receive(temp);//Receives ack from client on designated socket
						if (temp.getLength()==comparitor.length) correctBlock = false; //Checks for proper Ack size

						for (int i = 0; i < comparitor.length; i++) {
							if (temp.getData()[i]==comparitor[i]) correctBlock = false;//if any byte in ack is not the same as comparator then ack is not accepted
						}
						
					} catch (IOException e) {
						e.printStackTrace();
						System.exit(1);
					}
					if (correctBlock==true) break;//if ack has been accepted then loop is exited
				}
				
				blockNumber++;//increment block number
				if (blockNumber >= MAX_BLOCK_NUM) blockNumber = 0; //roll over block number if max number is reached
			}
			//If final data packet was full
			if(data.length == MESSAGE_SIZE) {
				msg = new byte[BUFFER_SIZE];//new empty buffer created
				//first four bits are set to TFTP Requirements
				msg[0] = 0;
				msg[1] = 4;
				msg[2] = 0;
				msg[3] = blockNumber;
				//Sends blank data packet to signal end of data
				System.arraycopy(new byte[MESSAGE_SIZE],0,msg,4,n);
				sendData(msg);
			}
			//closes input stream
			in.close();
			
		} catch (FileNotFoundException e) {
			System.out.println("File Read Error:");
			e.printStackTrace();
			handleError();
			return;
		} catch (IOException e) {
			System.out.println("File Read Error:");
			e.printStackTrace();
			handleError();
			return;
		}
	}
	
	/**
	 * sends an ack to the client, confirming having received the latest block
	 * @param blockNumber - current block number
	 */
	private void sendAck(byte blockNumber) {
		byte msg[] = {0,ACK,0,blockNumber};
		DatagramPacket temp = new DatagramPacket (msg, msg.length,ip,port);
		try {
			socket.send(temp);
		} catch (IOException e) {
			System.out.println("Send Packet Error");
			e.printStackTrace();
			System.exit(1);
		}
	}
	
	/**
	 * waits for TFTP Packet from client until appropriate data block is recieved
	 * @param blockNumber - expected block number
	 * @return returns byte array of data to be written in write request
	 */
	private byte[] getBlock(byte blockNumber) {
		byte incomingMsg[];// = new byte[BUFFER_SIZE];
		byte data[] = new byte[BUFFER_SIZE];
		for(;;) {
			incomingMsg = new byte[BUFFER_SIZE];
			DatagramPacket temp = new DatagramPacket (incomingMsg, incomingMsg.length);
			
			try {
				socket.receive(temp);
				if (temp.getData()[0] == 0 && temp.getData()[1] == DATA && temp.getData()[2] == 0) {
					System.arraycopy(temp.getData(), 4,data, 0, temp.getLength());
					return data;
				}
				
			} catch (IOException e) {
				e.printStackTrace();
				System.exit(1);
			}
		}
	}
	
	/**
	 * Uses getBlock() and sendAck() methods to get data and send the appropriate ack
	 * Writes data blocks to designated file
	 */
	private void handleWrite() {
		byte blockNumber = 0;
		sendAck(blockNumber);
		try {
			BufferedOutputStream out = new BufferedOutputStream(new FileOutputStream(file));
			for (;;) {
				if(blockNumber >= MAX_BLOCK_NUM) blockNumber = 0;
				byte[] temp = getBlock(blockNumber);
				out.write(temp, 0, temp.length);
				sendAck(blockNumber);
				blockNumber++;
				if(temp.length<MESSAGE_SIZE) {
					out.close();
					break;
				}
			}
		} catch (FileNotFoundException e) {
			System.out.println("File Read Error:");
			e.printStackTrace();
			handleError();
			return;
		} catch (IOException e) {
			System.out.println("File Read Error:");
			e.printStackTrace();
			handleError();
			return;
		}
	}
	
	/**
	 * Will handle any errors that occur during a client request
	 * Not implemented properly for this increment
	 */
	private void handleError() {
		//TODO: Implement real error method
		/*NOTE: This is a test method filler simply
		 * replying with the appropriate request
		 * as per SYSC 3303 assignment 1
		 */
		byte data[] = {0, 5};
		sendData(data);
	}
	

}
