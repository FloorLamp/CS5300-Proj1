package groupMembership;

import java.io.IOException;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Random;
import java.util.Set;

import com.amazonaws.*;
import com.amazonaws.auth.*;
import com.amazonaws.services.simpledb.*;
import com.amazonaws.services.simpledb.model.*;

public class GroupMembershipManager extends Thread {
	protected boolean running = true;

	protected static final String MBR_LIST_DOMAIN = "CS5300PROJECT1BSDBMbrList";
	protected static final String MBR_LIST_ITEM = "members";
	protected static final String MBR_LIST_ATTR = "ipps";
	protected static final int NO_OP_ITERATIONS = 2;
	protected static final int ROUND_TIME = 10; // seconds

	private Server server;
	private AmazonSimpleDB sdb;
	private Set<Server> MbrSet = new HashSet<Server>();
	private static Random rand = new Random();
	
	public GroupMembershipManager(Server server) {
		this.server = server;
		
		try {
			sdb = new AmazonSimpleDBClient(new PropertiesCredentials(
                Server.class.getResourceAsStream("AwsCredentials.properties")));
			
			sdb.createDomain(new CreateDomainRequest(MBR_LIST_DOMAIN));
		} catch (IOException e) {
			e.printStackTrace();
		}
		
		refreshMembers();
		setDaemon(true);
	}
	
	public Set<Server> getMbrSet() {
		return MbrSet;
	}
	
	public void run() {
		while(running) {
			try {
				Thread.sleep((int) ((rand.nextDouble() + 0.5) * ROUND_TIME * 1000));
				refreshMembers();
			} catch (InterruptedException e) {
		        e.printStackTrace();
			}
		}
	}
	
	private List<Server> parseMembers(String mbrList) {
		List<Server> members = new ArrayList<Server>();
		
		if (mbrList.length() > 0) {
			for (String s : mbrList.split("_")) {
				members.add(new Server(s));
			}
		}
		return members;
	}
	
	private String encodeMemberList() {
		String mbrList = "";
		
		for (Server member : MbrSet) {
			mbrList += (member.toString() + "_");
		}
		return mbrList.substring(0, mbrList.length() - 1);
	}
	
	protected void refreshMembers() {
		MbrSet.clear();
		
		try {
			List<Attribute> SDBMbrList = sdb.getAttributes(
					new GetAttributesRequest(MBR_LIST_DOMAIN, MBR_LIST_ITEM)).getAttributes();
			boolean hasResults = (SDBMbrList.size() != 0);
			System.out.println("server " + server + " sdbmbr: " + SDBMbrList + " has results: " + hasResults);
			String mbrs = hasResults ? SDBMbrList.get(0).getValue() :  ""; 
			
			for (Server mbr : parseMembers(mbrs)) {
				for (int i = 0; i < NO_OP_ITERATIONS; i++) {
					if (!mbr.equals(server) && rpc.RPCClient.noop(mbr)) {
						MbrSet.add(mbr);
					}
				}
			}			
			MbrSet.add(server);
			
			List<ReplaceableAttribute> replaceableAttributes = new ArrayList<ReplaceableAttribute>();
            replaceableAttributes.add(new ReplaceableAttribute(MBR_LIST_ATTR, encodeMemberList(), true));
            PutAttributesRequest putAttributesRequest = new PutAttributesRequest(MBR_LIST_DOMAIN, MBR_LIST_ITEM, replaceableAttributes);

            // If there are results in db, then set expected value. Otherwise, just write
            if (hasResults) {
            	putAttributesRequest.setExpected(new UpdateCondition(MBR_LIST_ATTR, mbrs, true));
            }
            sdb.putAttributes(putAttributesRequest);
			
		} catch (AmazonServiceException ase) {
			System.out.println("Caught an AmazonServiceException, which means your request made it "
		            + "to Amazon SimpleDB, but was rejected with an error response for some reason.");
			System.out.println("Error Message:    " + ase.getMessage());
			System.out.println("HTTP Status Code: " + ase.getStatusCode());
			System.out.println("AWS Error Code:   " + ase.getErrorCode());
			System.out.println("Error Type:       " + ase.getErrorType());
			System.out.println("Request ID:       " + ase.getRequestId());
		} catch (AmazonClientException ace) {
			System.out.println("Caught an AmazonClientException, which means the client encountered "
		            + "a serious internal problem while trying to communicate with SimpleDB, "
		            + "such as not being able to access the network.");
		    System.out.println("Error Message: " + ace.getMessage());
		}
	}
}
