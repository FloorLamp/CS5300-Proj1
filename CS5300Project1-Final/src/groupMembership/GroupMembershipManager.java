package groupMembership;

import java.io.IOException;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Random;
import java.util.Set;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.amazonaws.*;
import com.amazonaws.auth.*;
import com.amazonaws.services.simpledb.*;
import com.amazonaws.services.simpledb.model.*;

import cs5300.servlets.SessionManager;

public class GroupMembershipManager extends Thread {
	protected boolean running = true;

	private final static Logger LOGGER = Logger.getLogger(GroupMembershipManager.class.getName());
	
	protected static final String MBR_LIST_DOMAIN = "CS5300PROJECT1BSDBMbrList";
	protected static final String MBR_LIST_ITEM = "members";
	protected static final String MBR_LIST_ATTR = "ipps";
	protected static final int NO_OP_ITERATIONS = 2;
	protected static final int ROUND_TIME = 10; // seconds

	String server;
	
	private AmazonSimpleDB sdb;
	private Set<String> MbrSet = new HashSet<String>();
	private static Random rand = new Random();
	
	public GroupMembershipManager(String server) {
		LOGGER.info("Constructing GMM");
		this.server = server;
		
		try {
			LOGGER.info("Amazon DB initialization");
			sdb = new AmazonSimpleDBClient(
					 new BasicAWSCredentials("AKIAJ2RRJJLTIVPJPRAQ",
							 				"7qZ6PKijsifoMLVKH51Q65DdMrwGxQSiWNue/ftl"));
			
			sdb.createDomain(new CreateDomainRequest(MBR_LIST_DOMAIN));
		} catch (Exception e) {
			LOGGER.info("Except in Amazon DB initialization");
			LOGGER.info(e.toString());
			e.printStackTrace();
		}
		
		LOGGER.info("Constructing GMM -> Refreshing members");
		refreshMembers();
		setDaemon(true);
		LOGGER.info("Constructing GMM -> Size:" + Integer.toString(MbrSet.size()));
	}
	
	public Set<String> getMbrSet() {
		return MbrSet;
	}
	
	public void setRunning(boolean running) {
		this.running = running;
	}
	
	// Start this to run refresh protocol about every ROUND_TIME + epsilon seconds
	public void run() {
		while(running) {
			try {
				Thread.sleep((int) ((rand.nextDouble() + 0.5) * ROUND_TIME * 1000));
				if(rand.nextInt(MbrSet.size()) == 0){
					refreshMembers();
				}
			} catch (InterruptedException e) {
				LOGGER.info("Error refreshing members in thread");
		        e.printStackTrace();
			}
		}
	}
	
	// Takes data value stored in DB as arg, processes it into member list
	private List<String> parseMembers(String mbrList) {
		List<String> members = new ArrayList<String>();
		
		if (mbrList.length() > 0) {
			for (String s : mbrList.split("_")) {
				try {
					members.add(s);
				} catch (Exception e) {
					e.printStackTrace();
				}
			}
		}
		return members;
	}
	
	// Takes current member list and encodes it as a value for the DB
	private String encodeMemberList() {
		String mbrList = "";
		
		for (String member : MbrSet) {
			mbrList += (member + "_");
		}
		return mbrList.substring(0, mbrList.length() - 1);
	}	
	
	// Adds 's' to DB and local member set 
	public void addMember(String s) {
		try {
			List<Attribute> SDBMbrList = sdb.getAttributes(
					new GetAttributesRequest(MBR_LIST_DOMAIN, MBR_LIST_ITEM)).getAttributes();
			boolean hasResults = (SDBMbrList.size() != 0);
			String mbrs = hasResults ? SDBMbrList.get(0).getValue() :  ""; 
			
			MbrSet.add(s);
			
			List<ReplaceableAttribute> replaceableAttributes = new ArrayList<ReplaceableAttribute>();
            replaceableAttributes.add(new ReplaceableAttribute(MBR_LIST_ATTR, encodeMemberList(), true));
            PutAttributesRequest putAttributesRequest = new PutAttributesRequest(MBR_LIST_DOMAIN, MBR_LIST_ITEM, replaceableAttributes);

            // If there are results in db, then set expected value. Otherwise, just write
            if (hasResults) {
            	putAttributesRequest.setExpected(new UpdateCondition(MBR_LIST_ATTR, mbrs, true));
            }
            sdb.putAttributes(putAttributesRequest);
			
		} catch (Exception e) {
			LOGGER.info("Error adding member");
			e.printStackTrace();
		}
	}
	
	// Removes 's' from DB and local member set
	public void removeMember(String s) {
		try {
			List<Attribute> SDBMbrList = sdb.getAttributes(
					new GetAttributesRequest(MBR_LIST_DOMAIN, MBR_LIST_ITEM)).getAttributes();
			boolean hasResults = (SDBMbrList.size() != 0);
			String mbrs = hasResults ? SDBMbrList.get(0).getValue() :  ""; 
			
			MbrSet.remove(s);
			
			List<ReplaceableAttribute> replaceableAttributes = new ArrayList<ReplaceableAttribute>();
            replaceableAttributes.add(new ReplaceableAttribute(MBR_LIST_ATTR, encodeMemberList(), true));
            PutAttributesRequest putAttributesRequest = new PutAttributesRequest(MBR_LIST_DOMAIN, MBR_LIST_ITEM, replaceableAttributes);

            // If there are results in db, then set expected value. Otherwise, just write
            if (hasResults) {
            	putAttributesRequest.setExpected(new UpdateCondition(MBR_LIST_ATTR, mbrs, true));
            }
            sdb.putAttributes(putAttributesRequest);
			
		} catch (Exception e) {
			LOGGER.info("Error removing member");
			e.printStackTrace();
		}
	}
	
	// Refresh operation, checks whether each member stored on the DB is availables
	public void refreshMembers() {
		MbrSet.clear();
		LOGGER.info("Refreshing members");
		try {
			List<Attribute> SDBMbrList = sdb.getAttributes(
					new GetAttributesRequest(MBR_LIST_DOMAIN, MBR_LIST_ITEM)).getAttributes();
			boolean hasResults = (SDBMbrList.size() != 0);
			String mbrs = hasResults ? SDBMbrList.get(0).getValue() :  ""; 
			
			String mbrs_check = new String(mbrs);
			
			String[] mbrs_split = mbrs.split("_");
			
			// Check whether format is met, otherwise wipe value
			if(mbrs_split[0].split(":").length != 2 || mbrs_split[0].split("\\.").length != 4){
				mbrs = "";
			}
			
			LOGGER.info("Refreshing members: db access");
			for (String mbr : parseMembers(mbrs)) {
				for (int i = 0; i < NO_OP_ITERATIONS; i++) {
					if (!mbr.equals(server)
							&& rpc.RPCClient.noop(mbr.split(":")[0],
									              Integer.parseInt(mbr.split(":")[1]))) {
						MbrSet.add(mbr);
					}
				}
			}
			LOGGER.info("Refreshing members: adding self");
			MbrSet.add(server);
			
			List<ReplaceableAttribute> replaceableAttributes = new ArrayList<ReplaceableAttribute>();
            replaceableAttributes.add(new ReplaceableAttribute(MBR_LIST_ATTR, encodeMemberList(), true));
            PutAttributesRequest putAttributesRequest = new PutAttributesRequest(MBR_LIST_DOMAIN, MBR_LIST_ITEM, replaceableAttributes);

            // If there are results in db, then set expected value. Otherwise, just write
            if (hasResults) {
            	putAttributesRequest.setExpected(new UpdateCondition(MBR_LIST_ATTR, mbrs_check, true));
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
			LOGGER.info("Refreshing members error:");
			LOGGER.info("Caught an AmazonServiceException, which means your request made it "
		            + "to Amazon SimpleDB, but was rejected with an error response for some reason.");
			LOGGER.info("Error Message:    " + ase.getMessage());
			LOGGER.info("HTTP Status Code: " + ase.getStatusCode());
			LOGGER.info("AWS Error Code:   " + ase.getErrorCode());
			LOGGER.info("Error Type:       " + ase.getErrorType());
			LOGGER.info("Request ID:       " + ase.getRequestId());
		} catch (AmazonClientException ace) {
			System.out.println("Caught an AmazonClientException, which means the client encountered "
		            + "a serious internal problem while trying to communicate with SimpleDB, "
		            + "such as not being able to access the network.");
		    System.out.println("Error Message: " + ace.getMessage());
		    LOGGER.info("Refreshing members error:");
		    LOGGER.info("Caught an AmazonClientException, which means the client encountered "
		            + "a serious internal problem while trying to communicate with SimpleDB, "
		            + "such as not being able to access the network.");
		    LOGGER.info("Error Message: " + ace.getMessage());
		}
	}
}