package rpc;

import groupMembership.Server;

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
import java.net.SocketException;
import java.net.URLDecoder;
import java.net.URLEncoder;
import java.util.UUID;

import session.Session;
import session.SessionManager;

public class RPCClient {
	
	public final static int TIMEOUT = 1500;
	
	public final static int OPERATION_NOOP = 0;
	public final static int OPERATION_SESSIONREAD = 1;
	public final static int OPERATION_SESSIONWRITE = 2;
	public final static int OPERATION_SESSIONDELETE = 3;
	
	public final static int MAX_PACKET_SIZE = 4096;
	//private final static int OPERATION_SESSIONREAD = 1;

	
	public static boolean noop(Server s){

	    DatagramSocket rpcSocket;
	    
	    try {
	    	
	      rpcSocket = new DatagramSocket();
	      rpcSocket.setSoTimeout(TIMEOUT); 
	   
	      //generate unique id for call
	      String callID = UUID.randomUUID().toString();
	     
	      String outstr = (callID + "_" + OPERATION_NOOP + ",0,0");
	      byte[] outBuf = RPCClient.marshal(outstr);

	      //String newstr = RPCClient.unmarshal(outBuf);
	      DatagramPacket sendPkt;
	      try {
	        sendPkt = new DatagramPacket(outBuf, outBuf.length, s.ip, s.port);
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
	
	
	
	public static Session sessionRead(String sID, int version){
		
		try{
			Session s = SessionManager.sessionRead(sID, version);
			
			DatagramSocket socket = new DatagramSocket();
			socket.setSoTimeout(TIMEOUT);
			
			//generate unique id for call
			String callID = UUID.randomUUID().toString();
			
			//fill outBuf with [ callID, operationSESSIONREAD, sessionID, sessionVersionNum ]
			String temp = callID + "_" + OPERATION_SESSIONREAD + "_" + sID + "_" + version;
			byte[] outBuf = marshal(temp);
			
			//for all the servers in the group membership
			for( Server serv : s.getIPP().gm.getMbrSet() ) {
			    DatagramPacket sendPkt = new DatagramPacket(outBuf, outBuf.length, serv.ip, serv.port);
			    try{
			    	socket.send(sendPkt);
			    } catch (Exception e){
			    	e.printStackTrace();
			    }
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
				s.setMessage(URLDecoder.decode(responses[2],"UTF-8"));
				s.setChangecount(Integer.parseInt(URLDecoder.decode(responses[1], "UTF-8")));
				
			} catch(IOException ioe) {
				
			    ioe.printStackTrace();
			
			}
			//----------------------------------------------------------
			
			return s;
					  
		} catch (Exception e) {
			e.printStackTrace();
			return null;
		}
		
		
	}

	
	
	
	public static boolean sessionWrite(String sID, int version, long discard_time){
		try {
			Session s = SessionManager.sessionRead(sID, version);
			

			DatagramSocket rpcSocket = new DatagramSocket();
			rpcSocket.setSoTimeout(TIMEOUT);
			
			//generate unique id for call
			String callID = UUID.randomUUID().toString();
			
			//fill outBuf with [ callID, operationSESSIONREAD, sessionID, sessionVersionNum, discardtime ]
			String temp = callID + "_" + OPERATION_SESSIONWRITE + "_" + sID + "_" + version + "_" + URLEncoder.encode(s.getMessage(), "UTF-8") + "_" + discard_time;
			byte[] outBuf = marshal(temp);
			
			
			//send the data packets
			//for all the servers in the group membership
			for( Server serv : s.getIPP().gm.getMbrSet() ) {
			    DatagramPacket sendPkt = new DatagramPacket(outBuf, outBuf.length, serv.ip, serv.port);
			    try{
			    	rpcSocket.send(sendPkt);
			    } catch (Exception e){
			    	e.printStackTrace();
			    }
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

	public static boolean sessionDelete(Server s, String sID, int version){
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
			  sendPkt = new DatagramPacket(outBuf, outBuf.length, s.ip, s.port);
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
