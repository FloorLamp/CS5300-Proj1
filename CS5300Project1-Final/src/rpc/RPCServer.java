package rpc;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.SocketException;
import java.net.URLDecoder;
import java.net.URLEncoder;

import cs5300.servlets.SessionManager;

public class RPCServer implements Runnable{
	public boolean running = true;

	private DatagramSocket rpcSocket;
	private int serverPort;
	private SessionManager sMgr;
	
	// Server instantiation
	public RPCServer(SessionManager sessMgr) {
		try{
			rpcSocket = new DatagramSocket();
			serverPort = rpcSocket.getLocalPort();
			sMgr = sessMgr;
		} catch (SocketException e) {
			e.printStackTrace();
		}
	}
	
	public void setRunning(boolean running) {
		this.running = running;
	}
	
	@Override
	// Loop to receive requests from clients
	public void run() {
		
		while(running) {
			byte[] inBuf = new byte[RPCClient.MAX_PACKET_SIZE];
			byte[] outBuf = new byte[RPCClient.MAX_PACKET_SIZE];
			
			// Packet to receive commands
			DatagramPacket recvPkt = new DatagramPacket(inBuf, inBuf.length);
			
			try{
				rpcSocket.receive(recvPkt);
				InetAddress returnAddr = recvPkt.getAddress();
				int returnPort = recvPkt.getPort();
				// here inBuf contains the callID and operationCode

				//compute response
				byte [] data = recvPkt.getData();
				//int datalength = recvPkt.getLength();
				
				//parse packet data
				String[] requests = RPCClient.unmarshal(data).split("_");
				
				String callID = requests[0];
				int operationCode = Integer.parseInt(requests[1]);
				String sID = requests[2];
				String sVersion = requests[3];
				//String discard_time = requests[4];
				String response = null;
				SessionManager.SessionState session = null;
				
				sMgr.table_writelock.lock();
				switch( operationCode ) {
				
					case RPCClient.OPERATION_NOOP:
						response = callID;
					break;
					
					case RPCClient.OPERATION_SESSIONREAD:
						//get session by id
						session = sMgr.sessionMap.get(sID);
						if (session == null || session.version != Integer.parseInt(sVersion)){
							response = null;
						} else {
							response = callID;
							try {
					            response += "_" + URLEncoder.encode(Integer.toString(session.version),"UTF-8");
					            response += "_" + URLEncoder.encode(session.message,"UTF-8");
					            response += "_" + URLEncoder.encode(Long.toString(session.expirationTime),"UTF-8");
					          } catch (UnsupportedEncodingException e) {
					            // TODO Auto-generated catch block
					            e.printStackTrace();
					          }
							
						}
					break;
					
					case RPCClient.OPERATION_SESSIONWRITE:
				        String message = null;
				        String discard_time = null;
				        try {
				          message = URLDecoder.decode(requests[4],"UTF-8");
				          discard_time = requests[5];
				        } catch (UnsupportedEncodingException e) {
				          // TODO Auto-generated catch block
				          e.printStackTrace();
				        }
				        //put session (sessionid,sessionversion,count,message)
				        session = new SessionManager.SessionState(sID, message, Integer.parseInt(sVersion));
						session.expirationTime = Long.parseLong(discard_time);
				        sMgr.sessionMap.put(sID, session);
				        response = callID;
				 	break;
				 	
					case RPCClient.OPERATION_SESSIONDELETE:
						sMgr.sessionMap.remove(sID);
						response = callID;
					break;
				 
				}
				sMgr.table_writelock.unlock();
				
				outBuf = RPCClient.marshal(response);
				  // here outBuf should contain the callID and results of the call
				DatagramPacket sendPkt = new DatagramPacket(outBuf, outBuf.length, returnAddr, returnPort);
				rpcSocket.send(sendPkt);
				
			} catch(IOException e){
				e.printStackTrace();
			}
		
		}
	
		}
	
    public int getServerPort() {
	      return serverPort;
	}	
}