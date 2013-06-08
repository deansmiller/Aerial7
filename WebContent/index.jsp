<%@ page language="java" contentType="text/html; charset=utf-8" pageEncoding="utf-8"%>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%@ page import="com.mongodb.DB" %>
<%@ page import="com.mongodb.DBCollection" %>
<%@ page import="com.mongodb.MongoClient" %>
<%@ page import="com.mongodb.MongoClientURI" %>
<%@ page import="com.mongodb.DBCursor" %>
<%@ page import="com.mongodb.DBObject" %>
<%@ page import="java.util.ArrayList" %>
<%@ page import="java.util.Collections" %>

<%

	MongoClient mongoClient = new MongoClient(new MongoClientURI("mongodb://nodejitsu:7b9b1cc97d6ad36ad901b426d425e436@linus.mongohq.com:10063/nodejitsudb6132056633"));
	DB db = mongoClient.getDB("nodejitsudb6132056633");
	DBCollection jobs = db.getCollection("aerial7jobs");
	DBCursor cursor = jobs.find();
	ArrayList<DBObject> jobObjects = new ArrayList<DBObject>();
	while(cursor.hasNext()){
		jobObjects.add(cursor.next());
	}
	Collections.reverse(jobObjects);
%>

<!DOCTYPE html>
<html>

	<head>
	    <meta http-equiv="Content-type" content="text/html; charset=utf-8">
	    <title>Aerial7 Digital Media Processor</title>
	    <link rel="stylesheet" href="styles/bootstrap.min.css" type="text/css" media="screen">
	    <link rel="stylesheet" href="styles/styles.css" type="text/css" media="screen">
	</head>
	
	<body>
	    <div class="container">
	    
			<div class="page-header">
				<h1>Aerial7 <small>Digital Media Processor</small></h1>
			</div>
			
			<div class="job-container">
				<table class="table">
					<thead>
						<tr>
							<th>Item ID</th>
							<th>Filename</th>
							<th>Date Created</th>
						</tr>
					</thead>
					
					<tbody>
						<% for(int i = 0; i < jobObjects.size(); i++){ %>
						<tr>
							<td class="itemId"><%= jobObjects.get(i).get("itemId") %></td>
							<td class="filename"><%= jobObjects.get(i).get("filename") %></td>
							<td class="date"><%= jobObjects.get(i).get("date") %></td>
						<%} %>
						</tr>
					</tbody>
				</table>
			</div>
			
	    </div>
	</body>
	
	
</html>