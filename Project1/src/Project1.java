import java.io.IOException;
import java.io.PrintWriter;
import java.net.InetAddress;
import java.net.UnknownHostException;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import rpc.RPCClient;
import rpc.RPCServer;
import session.Session;
import session.SessionManager;

import groupMembership.GroupMembershipManager;
import groupMembership.Server;

public class Project1 extends HttpServlet {
  private static final long serialVersionUID = 8815322823956211829L;

  public static Server localServer;

  public static RPCServer rpcServer;
  public static GroupMembershipManager gm;


  /**
   * @param args
   * @throws Exception
   */
  public Project1() {
    super();
  }

  protected void doGet(HttpServletRequest request, HttpServletResponse response)
      throws ServletException, IOException {
    // Set response objects
    response.setContentType("text/html");
    PrintWriter out = response.getWriter();

    // Load session from cookie or create new one if doesn't exist
    Session session = SessionManager.getAndIncrement(request);

    String message = session.getMessage();
    Integer count = session.getChangecount();
    // Initialize message
    if (message == null) {
      message = "Hello World!";
    }
    // Initialize count or increment
    if (count == null) {
      count = 1;
    } else {
      count = new Integer(1 + count);
    }
    // Check user submission command
    String cmd = request.getParameter("cmd");
    if (cmd != null) {
      if (cmd.equals("Replace")) {
        message = request.getParameter("replace_text");
      } else if (cmd.equals("LogOut")) {
        // Do something different on logout
        SessionManager.destroyCookie(request, response);
        out.println("<!DOCTYPE html>");
        out.println("<html><head></head><body>");
        out.println("<h2>Bye!</h2>");
        out.println("</body></html>");
        return;
      } else if (cmd.equals("Refresh Membership")) {
    	  gm.refreshMembers();
      } else if (cmd.equals("Server Crash")) {
    	  gm.setRunning(false);
    	  rpcServer.setRunning(false);
      }
    }
    // Write changed variables back to session
    session.setMessage(message);
    session.setChangecount(count);

    
    RPCClient.sessionWrite(session.getSID(),session.getChangecount(),session.getExpiration());
    
    // Write back cookie
    SessionManager.putCookie(response, session);

    // Output HTML to page
    out.println("<!DOCTYPE html>");
    out.println("<html><head></head><body>");
    out.println("<h2>(" + count.toString() + ") " + message + "</h2>");
    out.println("<form method=\"post\">");
    out.println("<div><input type=\"submit\" value=\"Replace\" name=\"cmd\" /><input type=\"text\" name=\"replace_text\" /></div>");
    out.println("<div><input type=\"submit\" value=\"Refresh\" name=\"cmd\" /></div>");
    out.println("<div><input type=\"submit\" value=\"LogOut\" name=\"cmd\" /></div>");
    out.println("<div><input type=\"submit\" value=\"Refresh Membership\" name=\"cmd\" /></div>");
    out.println("<div><input type=\"submit\" value=\"Server Crash\" name=\"cmd\" /></div>");
    out.println("</form>");
    out.println("<h3>Server: " + localServer + "</h3>");
    out.println("<h3>Session: " + session + "</h3>");
    out.println("<h3>MbrSet: </h3><ul>");
    for (Server s : gm.getMbrSet()) {
    	out.println("<li>" + s.toString() + "</li>");
    }
    out.println("</ul>");
    out.println("</body></html>");
  }

  /**
   * Simply refer to doGet
   */
  protected void doPost(HttpServletRequest request, HttpServletResponse response)
      throws ServletException, IOException {
    doGet(request, response);
  }

}
