<%@ page language="java" contentType="text/html; charset=utf-8" pageEncoding="utf-8"%>
<%@ page import="com.amazonaws.*" %>
<%@ page import="com.amazonaws.auth.*" %>
<%@ page import="com.amazonaws.services.ec2.*" %>
<%@ page import="com.amazonaws.services.ec2.model.*" %>
<%@ page import="com.amazonaws.services.s3.*" %>
<%@ page import="com.amazonaws.services.s3.model.*" %>
<%@ page import="com.amazonaws.services.simpledb.*" %>
<%@ page import="com.amazonaws.services.simpledb.model.*" %>
<%@ page import="java.util.*" %>
<%@ page session="false" %>

<!DOCTYPE html PUBLIC "-//W3C//DTD HTML 4.01 Transitional//EN" "http://www.w3.org/TR/html4/loose.dtd">
<html>
<head>
<meta http-equiv="Content-Type" content="text/html; charset=UTF-8">
<title>Project 1</title>
</head>
<body>
<%
String cmd = request.getParameter("cmd");
if( (cmd != null && cmd.equals("LogOut")) || ((String)request.getAttribute("logout")).equals("true")){
	out.println("<br>&nbsp\n<br><big><big><b>Logged out! (if you weren't expecting this, session either timed out or failed, just hit refresh!)<br>&nbsp\n<br></b></big></big>");
} else {
	out.println("<br>&nbsp;<br>");
	out.println("<big><big><b>");
	out.println((String)(request.getAttribute("displayMsg")));
	out.println("<br>&nbsp;<br>");
	out.println("</b></big></big>");
	out.println("<form method=POST action=\"\">");
	out.println("<input type=submit name=cmd value=Replace>&nbsp;&nbsp;<input type=text name=NewText size=40 maxlength=256>&nbsp;&nbsp;");
	out.println("</form>");
	out.println("<br>&nbsp;<br>");
	out.println("<form method=POST action=\"\">");
	out.println("<input type=submit name=cmd value=Refresh>");
	out.println("</form>");
	out.println("<br>&nbsp;<br>");
	out.println("<form method=POST action=\"\">");
	out.println("<input type=submit name=cmd value=LogOut>");
	out.println("</form>");
	out.println("<br>&nbsp;<br>");
	out.println("<form method=POST action=\"\">");
	out.println("<input type=submit name=cmd value=ServerCrash>");
	out.println("</form>");
	out.println("<br>&nbsp;<br>");
	out.println("<form method=POST action=\"\">");
	out.println("<input type=submit name=cmd value=RefreshMembership>");
	out.println("</form>");
	out.println("<p>");
	
	out.println("<br>&nbsp;<br>");
	out.println("Session expires: " + ((Date)request.getAttribute("expiration")).toString());
	out.println("<br>&nbsp;<br>");
	out.println("Server ID: " + ((String)request.getAttribute("serverID")));
	out.println("<br>&nbsp;<br>");
	out.println("IPP Primary: " + ((String)request.getAttribute("IPPPrimary")));
	out.println("<br>&nbsp;<br>");
	out.println("IPP Backup: " + ((String)request.getAttribute("IPPBackup")));
	out.println("<br>&nbsp;<br>");
	out.println("Found where?: " + ((String)request.getAttribute("found")));
	out.println("<br>&nbsp;<br>");
	out.println("Evicted Session: " + ((String)request.getAttribute("evicted")));
	out.println("<br>&nbsp;<br>");
	out.println("Discard time: " + ((String)request.getAttribute("discard")));
	out.println("<br>&nbsp;<br>");
	out.println("GROUP MEMBER LIST:");

	ArrayList<String> mbrList = ((ArrayList<String>) request.getAttribute("mbrList"));
	for(int i = 0; i < mbrList.size(); i++){
		out.println("<br>&nbsp;<br>");
		out.println(mbrList.get(i));
	}
}
%>
</body>
</html>