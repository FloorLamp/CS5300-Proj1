package groupMembership;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Random;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

import rpc.RPCClient;

import com.amazonaws.auth.PropertiesCredentials;
import com.amazonaws.services.simpledb.AmazonSimpleDB;
import com.amazonaws.services.simpledb.AmazonSimpleDBClient;
import com.amazonaws.services.simpledb.model.DeleteAttributesRequest;
import com.amazonaws.services.simpledb.model.Item;
import com.amazonaws.services.simpledb.model.PutAttributesRequest;
import com.amazonaws.services.simpledb.model.ReplaceableAttribute;
import com.amazonaws.services.simpledb.model.SelectRequest;

public class GroupMembership extends Thread {
  public static final int checkRate = 10; // Approximate number of seconds
                                          // between membership checks
  public static final String simpleDBDomain = "Project1";

  boolean setFalseToStop = true;

  AmazonSimpleDB sdb;
  Server current;
  static Random r = new Random();

  protected static final ReentrantReadWriteLock rwl = new ReentrantReadWriteLock();
  protected static final Lock readlock = rwl.readLock();
  protected static final Lock writelock = rwl.writeLock();
  protected static List<Server> servers = new ArrayList<Server>();

  public GroupMembership(Server s) throws IOException {
    current = s;
    writelock.lock();
    servers.add(current);
    writelock.unlock();
    sdb = new AmazonSimpleDBClient(new PropertiesCredentials(
        GroupMembership.class.getResourceAsStream("AwsCredentials.properties")));
    checkRound();
  }

  public void run() {
    // Check every 0.5*checkRate to 1.5*checkRate
    while (setFalseToStop) {
      try {
        Thread.sleep((int) ((r.nextDouble() + 0.5) * checkRate * 1000));
        checkRound();
      } catch (InterruptedException e) {
        e.printStackTrace();
      }
    }
  }

  public void cleanup() {
    setFalseToStop = false;
  }

  private boolean checkRound() {
    System.out.println("Start " + current);

    // Add to SimpleDB
    addMembership();

    // Build list of servers
    SelectRequest selectRequest = new SelectRequest("select * from "
        + simpleDBDomain);
    List<Server> ss = new ArrayList<Server>();
    for (Item item : sdb.select(selectRequest).getItems()) {
      ss.add(new Server(item.getName()));
    }

    if (ss.size() > 0) {
      // Probe random server and remove if inactive
      int i = r.nextInt(ss.size());
      Server s = ss.get(i);
      System.out.println("Checking " + s);
      // if(!current.equals(s)) {
      if (!probe(s)) {
        ss.remove(i);
      }
      // }
      // Replace servers with new list
      Collections.shuffle(ss, r);
      writelock.lock();
      servers = ss;
      writelock.unlock();
    }

    System.out.println("End");
    return true;
  }

  private boolean addMembership() {
    List<ReplaceableAttribute> replaceableAttributes = new ArrayList<ReplaceableAttribute>();
    replaceableAttributes.add(new ReplaceableAttribute("ip", current.ip
        .getHostAddress(), true));
    replaceableAttributes.add(new ReplaceableAttribute("port", current.port
        .toString(), true));
    sdb.putAttributes(new PutAttributesRequest(simpleDBDomain, current
        .toString(), replaceableAttributes));
    return true;
  }

  /**
   * Issue a probe request to remoteip:remoteport. If the server does not
   * respond, remove it from SimpleDB.
   * 
   * @return
   */
  private boolean probe(Server s) {
    if (RPCClient.probe(s)) {
      System.out.println(s + " Active");
      return true;
    } else {
      System.out.println(s + " Inactive");
      sdb.deleteAttributes(new DeleteAttributesRequest(simpleDBDomain, s
          .toString()));
      System.out.println(s + " Removed");
      return false;
    }
  }

  public static List<Server> getServers() {
    readlock.lock();
    @SuppressWarnings("unchecked")
    List<Server> ss = (List<Server>) ((ArrayList<Server>) servers).clone();
    readlock.unlock();
    return ss;
  }

  public static List<Server> getServers(int num) {
    writelock.lock();
    Collections.shuffle(servers,r);
    writelock.unlock();
    readlock.lock();
    List<Server> ss = new ArrayList<Server>(); 
    ss.addAll(servers.subList(0, num));
    readlock.unlock();
    return ss;
  }

  public static int numServers() {
    readlock.lock();
    int size = servers.size();
    readlock.unlock();
    return size;
  }

}
