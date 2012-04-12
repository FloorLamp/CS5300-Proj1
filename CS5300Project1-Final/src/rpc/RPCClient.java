package rpc;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InterruptedIOException;
import java.io.ObjectInput;
import java.io.ObjectInputStream;
import java.io.ObjectOutput;
import java.io.ObjectOutputStream;
import java.io.UnsupportedEncodingException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.SocketException;
import java.net.URLDecoder;
import java.net.URLEncoder;
import java.util.UUID;

import cs5300.servlets.SessionManager;

public class RPCClient {
	
	public final static int TIMEOUT = 300;
	
	public final static int OPERATION_NOOP = 0;
	public final static int OPERATION_SESSIONREAD = 1;
	public final static int OPERATION_SESSIONWRITE = 2;
	public final static int OPERATION_SESSIONDELETE = 3;
	
	public final static int MAX_PACKET_SIZE = 4096;
	//private final static int OPERATION_SESSIONREAD = 1;

	
	// Noop request, to check if targer is up. Returns true if a response is received
	public static boolean noop(String ip, int port){

	    DatagramSocket rpcSocket;
	    
	    try {
	    	
	      rpcSocket = new DatagramSocket();
	      rpcSocket.setSoTimeout(TIMEOUT); 
	   
	      //generate unique id for call
	      String callID = UUID.randomUUID().toString();
	     
	      String outstr = (callID + "_" + OPERATION_NOOP + "_0_0");
	      byte[] outBuf = RPCClient.marshal(outstr);

	      //String newstr = RPCClient.unmarshal(outBuf);
	      DatagramPacket sendPkt;
	      try {
	        sendPkt = new DatagramPacket(outBuf, outBuf.length, InetAddress.getByName(ip), port);
	        rpcSocket.send(sendPkt);
	      } catch (IOException e) {
	        e.printStackTrace();
	      }
	      
	      //-----------------------------------------------------
		  //waiting for response
		      
	      byte[] inBuf = new byte[MAX_PACKET_SIZE];
	      DatagramPacket recvPkt = new DatagramPacket(inBuf, inBuf.length);
	      
	      try {
	    	  
	        do {
	          recvPkt.setLength(inBuf.length);
	          rpcSocket.receive(recvPkt);
	        } while ( !(RPCClient.unmarshal(recvPkt.getData())).split("_")[0].equals(callID));
	      } catch (IOException e) {
	        recvPkt = null;
	        return false;
	      }
	      
	      } catch (SocketException e) {
	    	  e.printStackTrace();
	    	  return false;
	      }

	    return true;		
		
		
	}
	
	// Read request. Returns session if a response is received.
	// Returns null if the call failed.
	public static SessionManager.SessionState sessionRead(String sID, int version, String ip, int port){
		
		try{
			SessionManager.SessionState session = null;
			
			DatagramSocket socket = new DatagramSocket();
			socket.setSoTimeout(TIMEOUT);
			
			//generate unique id for call
			String callID = UUID.randomUUID().toString();
			
			//fill outBuf with [ callID, operationSESSIONREAD, sessionID, sessionVersionNum ]
			String temp = callID + "_" + OPERATION_SESSIONREAD + "_" + sID + "_" + version;
			byte[] outBuf = marshal(temp);
			
			DatagramPacket sendPkt = new DatagramPacket(outBuf, outBuf.length, InetAddress.getByName(ip), port);
		    try{
		    	socket.send(sendPkt);
		    } catch (Exception e){
		    	e.printStackTrace();
		    }
			
			//-----------------------------------------------------
			//waiting for response
			
			byte [] inBuf = new byte[MAX_PACKET_SIZE];
			DatagramPacket recvPkt = new DatagramPacket(inBuf, inBuf.length);
			recvPkt.setLength(inBuf.length);
			String response = null;
			
			try {
				
				do {
					try{
						socket.receive(recvPkt);
						response = RPCClient.unmarshal(inBuf);
						
					} catch (IOException e){
						e.printStackTrace();
						return null;
					}
			    } while( response == null || !(response.split("_")[0].equals(callID)) );
			
				String[] responses = response.split("_");
				session = new SessionManager.SessionState(sID, null, version);
				session.version = Integer.parseInt(URLDecoder.decode(responses[1], "UTF-8"));
				session.message = URLDecoder.decode(responses[2],"UTF-8");
				session.expirationTime = Long.parseLong(URLDecoder.decode(responses[3], "UTF-8"));
				
			} catch(IOException ioe) {
				
			    ioe.printStackTrace();
			
			}
			//----------------------------------------------------------
			
			return session;
					  
		} catch (Exception e) {
			e.printStackTrace();
			return null;
		}
		
		
	}

	// Write request. Returns true if a response is received, false otherwise.
	public static boolean sessionWrite(SessionManager.SessionState session, String ip, int port){
		try {
			DatagramSocket rpcSocket = new DatagramSocket();
			rpcSocket.setSoTimeout(TIMEOUT);
			
			//generate unique id for call
			String callID = UUID.randomUUID().toString();
			
			//fill outBuf with [ callID, operationSESSIONREAD, sessionID, sessionVersionNum, discardtime ]
			String temp = callID + "_" + OPERATION_SESSIONWRITE + "_" + session.sessionID + "_" + session.version + "_"
							+ URLEncoder.encode(session.message, "UTF-8") + "_" + Long.toString(session.expirationTime);
			byte[] outBuf = marshal(temp);
			
			
		    try{
		    	DatagramPacket sendPkt = new DatagramPacket(outBuf, outBuf.length, InetAddress.getByName(ip), port);
		    	rpcSocket.send(sendPkt);
		    } catch (Exception e){
		    	e.printStackTrace();
		    }
			
			//-----------------------------------------------------
			//waiting for response
			
		      byte[] inBuf = new byte[MAX_PACKET_SIZE];
		      DatagramPacket recvPkt = new DatagramPacket(inBuf, inBuf.length);
		      
		      try {
		    	  
		        do {
		          recvPkt.setLength(inBuf.length);
		          rpcSocket.receive(recvPkt);
		        } while ( !(RPCClient.unmarshal(recvPkt.getData())).split("_")[0].equals(callID));
		      } catch (IOException e) {
		        recvPkt = null;
		        return false;
		      }
		      
		      } catch (SocketException e) {
		    	  e.printStackTrace();
		    	  return false;
		      } catch (UnsupportedEncodingException e) {
		    	  e.printStackTrace();
			  }
		
			//----------------------------------------------------------
			
			return true;	
	}

	// Delete request. Returns true if a response is received, false otherwise.
	public static boolean sessionDelete(String sID, int version, String ip, int port){
		try {

			DatagramSocket rpcSocket = new DatagramSocket();
			rpcSocket.setSoTimeout(TIMEOUT);
			
			//generate unique id for call
			String callID = UUID.randomUUID().toString();
			
			//fill outBuf with [ callID, operationSESSIONREAD, sessionID, sessionVersionNum ]
			String temp = callID + "_" + OPERATION_SESSIONDELETE + "_" + sID + "_" + version;
			byte[] outBuf = marshal(temp);
			

			//String newstr = RPCClient.unmarshal(outBuf);
			DatagramPacket sendPkt;
			try {
			  sendPkt = new DatagramPacket(outBuf, outBuf.length, InetAddress.getByName(ip), port);
			  rpcSocket.send(sendPkt);
			} catch (IOException e) {
			  e.printStackTrace();
			}
		      
			//-----------------------------------------------------
			//waiting for response
			
		      byte[] inBuf = new byte[MAX_PACKET_SIZE];
		      DatagramPacket recvPkt = new DatagramPacket(inBuf, inBuf.length);
		      
		      try {
		    	  
		        do {
		          recvPkt.setLength(inBuf.length);
		          rpcSocket.receive(recvPkt);
		        } while ( !(RPCClient.unmarshal(recvPkt.getData())).split("_")[0].equals(callID));
		      } catch (IOException e) {
		        recvPkt = null;
		        return false;
		      }
		      
		      } catch (SocketException e) {
		    	  e.printStackTrace();
		    	  return false;
		      }
		
			//----------------------------------------------------------
		return true;
	}
	
	// Convert call/response strings into bytes for transmission
	public static byte[] marshal(String s){
	
	    try {
	        ByteArrayOutputStream bos = new ByteArrayOutputStream();
	        ObjectOutput out = new ObjectOutputStream(bos);
	        out.writeObject(s);
	        byte[] output = bos.toByteArray();
	        return output;
	      } catch (Exception e) {
	        e.printStackTrace();
	        return null;
	      }
	    
	}

	// Convert transmitted bytes back into call/response string
	public static String unmarshal(byte[] b) {
	   try {
		      ByteArrayInputStream bis = new ByteArrayInputStream(b);
		      ObjectInput in = new ObjectInputStream(bis);
		      String output = (String) in.readObject();
		      return output;
		    } catch (Exception e) {
		      e.printStackTrace();
		      return null;
		    }
	}	
}