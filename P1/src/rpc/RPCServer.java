package rpc;

import groupMembership.Server;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.SocketException;
import java.net.URLDecoder;
import java.net.URLEncoder;

import session.Session;

public class RPCServer implements Runnable{

	private DatagramSocket rpcSocket;
	private int serverPort;
	
	public RPCServer() {
		try{
			rpcSocket = new DatagramSocket();
			serverPort = rpcSocket.getLocalPort();
		} catch (SocketException e) {
			e.printStackTrace();
		}
	}
	
	@Override
	public void run() {
		
		while(true) {
			byte[] inBuf = new byte[RPCClient.MAX_PACKET_SIZE];
			byte[] outBuf = new byte[RPCClient.MAX_PACKET_SIZE];
			
			DatagramPacket recvPkt = new DatagramPacket(inBuf, inBuf.length);
			
			try{
				rpcSocket.receive(recvPkt);
				InetAddress returnAddr = recvPkt.getAddress();
				int returnPort = recvPkt.getPort();
				// here inBuf contains the callID and operationCode

				//compute response
				byte [] data = recvPkt.getData();
				int datalength = recvPkt.getLength();
				
				//parse packet data
				String[] requests = RPCClient.unmarshal(data).split(",");
				
				String callID = requests[0];
				int operationCode = Integer.parseInt(requests[1]);
				String sID = requests[2];
				String sVersion = requests[3];
				String response = null;
				Session session = null;
				
				switch( operationCode ) {
					case RPCClient.OPERATION_PROBE:
						response = callID;
					break;
					case RPCClient.OPERATION_SESSIONREAD:
						//get session by id
						session = Server.getHash().get(sID);
						if (session == null){
							response = null;
						} else {
							response = callID;
							try {
					            response += "," + URLEncoder.encode(session.getChangecount().toString(),"UTF-8");
					            response += "," + URLEncoder.encode(session.getMessage(),"UTF-8");
					          } catch (UnsupportedEncodingException e) {
					            // TODO Auto-generated catch block
					            e.printStackTrace();
					          }
							
						}
					break;
					case RPCClient.OPERATION_SESSIONPUT:
						String count = null;
				        String message = null;
				        try {
				          count = URLDecoder.decode(requests[4],"UTF-8");
				          message = URLDecoder.decode(requests[5],"UTF-8");
				        } catch (UnsupportedEncodingException e) {
				          // TODO Auto-generated catch block
				          e.printStackTrace();
				        }
				        //put session (sessionid,sessionversion,count,message)
						response = callID;
				 	break;
				 
				}
				
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
	
