// TFTPClient.java
// This class is the client side for a very simple assignment based on TFTP on
// UDP/IP. The client uses one port and sends a read or write request and gets 
// the appropriate response from the server.  No actual file transfer takes place.   

import java.io.*;
import java.net.*;
import java.util.Scanner;

public class Client {

   private DatagramPacket sendPacket, receivePacket;
   private DatagramSocket sendReceiveSocket;
   
   private String file;
   private byte reqType;
   
   public static final int PORT = 68;
   public static final int DATA_SIZE = 512;
   public static final int DATA_INFO = 4;
   public static final String MODE = "octet";
   
   public static final byte READ = 1;
   public static final byte WRITE = 2;
   public static final byte DATA = 3;
   public static final byte ACK = 4;
   
   
   // we can run in normal (send directly to server) or test
   // (send to simulator) mode
   public static enum Mode {NORMAL, TEST};

   public Client()
   {
      try {
         // Construct a datagram socket and bind it to any available
         // port on the local host machine. This socket will be used to
         // send and receive UDP Datagram packets.
         sendReceiveSocket = new DatagramSocket();
      } catch (SocketException se) {   // Can't create the socket.
         se.printStackTrace();
         System.exit(1);
      }
   }

   public void sendRequest() {
	   byte data[] = new byte[DATA_SIZE+DATA_INFO];
	   data[0] = 0;
	   data[1] = reqType;
	   System.arraycopy(file.getBytes(),0,data,2,file.getBytes().length);
	   data[2+file.getBytes().length] = 0;
	   System.arraycopy(MODE.getBytes(),0,data,3+file.getBytes().length,MODE.getBytes().length);
	   data[3+file.getBytes().length+MODE.getBytes().length] = 0;
	   
	   try {
		this.sendPacket = new DatagramPacket(data,data.length,InetAddress.getLocalHost(),PORT);
		this.sendReceiveSocket.send(sendPacket);
	} catch (UnknownHostException e) {
		// TODO Auto-generated catch block
		e.printStackTrace();
	} catch (IOException e) {
		// TODO Auto-generated catch block
		e.printStackTrace();
	}
   }
   
   public void sendAndReceive()
   {
      byte[] msg = new byte[100], // message we send
             fn, // filename as an array of bytes
             md, // mode as an array of bytes
             data; // reply as array of bytes
      String filename, mode; // filename and mode as Strings
      int j, len, sendPort;
      Mode run = Mode.TEST; // change to NORMAL to send directly to server
      
      if (run==Mode.NORMAL) 
         sendPort = 69;
      else
         sendPort = 68;
      
      // sends 10 packets -- 4 reads, 5 writes, 1 invalid
      for(int i=0; i<10; i++) {

         System.out.println("Client: creating packet " + i + ".");
         
         // Prepare a DatagramPacket and send it via sendReceiveSocket
         // to sendPort on the destination host (also on this machine).

         // if i even, it's a read; otherwise a write
         // opcode for read is 01, and for write 02

        msg[0] = 0;
        if(i%2==0) 
           msg[1]=1;
        else 
           msg[1]=2;
           
        if(i==8) 
           msg[1]=7; // if it's the 8th time, send an invalid request

        // next we have a file name -- let's just pick one
        filename = "test.txt";
        // convert to bytes
        fn = filename.getBytes();
        
        // and copy into the msg
        System.arraycopy(fn,0,msg,2,fn.length);
        
        // now add a 0 byte
        msg[fn.length+2] = 0;

        // now add "octet" (or "netascii")
        mode = "octet";
        // convert to bytes
        md = mode.getBytes();
        
        // and copy into the msg
        System.arraycopy(md,0,msg,fn.length+3,md.length);
        
        len = fn.length+md.length+4; // length of the message

        // and end with another 0 byte 
        msg[len-1] = 0;

        // Construct a datagram packet that is to be sent to a specified port
        // on a specified host.
        // The arguments are:
        //  msg - the message contained in the packet (the byte array)
        //  the length we care about - k+1
        //  InetAddress.getLocalHost() - the Internet address of the
        //     destination host.
        //     In this example, we want the destination to be the same as
        //     the source (i.e., we want to run the client and server on the
        //     same computer). InetAddress.getLocalHost() returns the Internet
        //     address of the local host.
        //  69 - the destination port number on the destination host.
        try {
           sendPacket = new DatagramPacket(msg, len,
                                         InetAddress.getLocalHost(), sendPort);
        } catch (UnknownHostException e) {
           e.printStackTrace();
           System.exit(1);
        }

        System.out.println("Client: sending packet " + i + ".");
        System.out.println("To host: " + sendPacket.getAddress());
        System.out.println("Destination host port: " + sendPacket.getPort());
        System.out.println("Length: " + sendPacket.getLength());
        System.out.println("Containing: ");
        data = sendPacket.getData();
        for (j=0;j<len;j++) {
            System.out.println("byte " + j + " " + data[j]);
        }

        // Send the datagram packet to the server via the send/receive socket.

        try {
           sendReceiveSocket.send(sendPacket);
        } catch (IOException e) {
           e.printStackTrace();
           System.exit(1);
        }

        System.out.println("Client: Packet sent.");

        // Construct a DatagramPacket for receiving packets up
        // to 100 bytes long (the length of the byte array).

        data = new byte[100];
        receivePacket = new DatagramPacket(data, data.length);

        System.out.println("Client: Waiting for packet.");
        try {
           // Block until a datagram is received via sendReceiveSocket.
           sendReceiveSocket.receive(receivePacket);
        } catch(IOException e) {
           e.printStackTrace();
           System.exit(1);
        }

        // Process the received datagram.
        System.out.println("Client: Packet received:");
        System.out.println("From host: " + receivePacket.getAddress());
        System.out.println("Host port: " + receivePacket.getPort());
        System.out.println("Length: " + receivePacket.getLength());
        System.out.println("Containing: ");

        // Get a reference to the data inside the received datagram.
        data = receivePacket.getData();
        for (j=0;j<receivePacket.getLength();j++) {
            System.out.println("byte " + j + " " + data[j]);
        }
        
        System.out.println();

      } // end of loop

      // We're finished, so close the socket.
      sendReceiveSocket.close();
   }
   
   public void askUser() {
	   Scanner scanner = new Scanner (System.in);
	   for (;;) {
		   System.out.println("Please enter a file name:");
		   file = scanner.next();
		   System.out.println("Please select a request type (by number):");
		   System.out.println("1) Read");
		   System.out.println("2) Write");
		   int temp = scanner.nextInt();
		   if(temp == 1) {
			   this.reqType = READ;
			   break;
		   } else if (temp == 2) {
			   this.reqType = WRITE;
			   break;
		   }
	   }
   }

   public static void main(String args[])
   {
      Client c = new Client();
      c.askUser();
      c.sendAndReceive();
   }
}