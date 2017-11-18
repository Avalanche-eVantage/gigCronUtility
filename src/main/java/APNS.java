import io.netty.util.concurrent.Future;

import java.io.File;
import java.sql.CallableStatement;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.text.SimpleDateFormat;
//import java.sql.Statement;
//import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.Map;
//import java.util.TimeZone;
import java.util.concurrent.ExecutionException;
import java.util.logging.Logger;

import com.google.gson.JsonSyntaxException;
import com.relayrides.pushy.apns.ClientNotConnectedException;
import com.relayrides.pushy.apns.PushNotificationResponse;
import com.relayrides.pushy.apns.util.ApnsPayloadBuilder;
import com.relayrides.pushy.apns.util.SimpleApnsPushNotification;
import com.relayrides.pushy.apns.ApnsClient;
import com.relayrides.pushy.apns.ApnsClientBuilder;


public class APNS {
	private static Connection con = null;
	
	public APNS() {
		// TODO Auto-generated constructor stub
	}
 
		public boolean isActive() throws ClassNotFoundException, SQLException, Exception {
			ResultSet rs = null;
			Logger LOGGER = Logger.getLogger("InfoLogging");
			try {
				Class.forName("com.mysql.jdbc.Driver");
		    	
				
		   	    con = DriverManager.getConnection(ServletConstants.dbURL, ServletConstants.dbUsername, ServletConstants.dbPassword);
		   	    CallableStatement cStmt = null;

		   	    LOGGER.info("Before CALL getLastRunTimeStamp()");
		   	    cStmt = con.prepareCall("{ CALL getStatus() }");
		   	    cStmt.execute();
		   	    rs = cStmt.getResultSet();

		   	    if(rs.next()) {
		   	    	String status = rs.getString("status");
		   	    	System.out.println("status: " + status);
		   	    	if (status.equals("active")){
		   	    		System.out.println("status == active");
		   	    		return(true);
		   	    	}
		   	    } 
		   	    System.out.println("end is near");
		   	    return(false);
			} finally {
				 if (rs != null) {
					 rs.close();
				 }
		         if (con != null) {
		        	 con.close();
		         }
			}
		}
	
		public String getLastRunTimeStamp() throws ClassNotFoundException, SQLException, Exception {
			ResultSet rs = null;
			Logger LOGGER = Logger.getLogger("InfoLogging");
			try {
				Class.forName("com.mysql.jdbc.Driver");
		    	
				
		   	    con = DriverManager.getConnection(ServletConstants.dbURL, ServletConstants.dbUsername, ServletConstants.dbPassword);
		   	    CallableStatement cStmt = null;

		   	    LOGGER.info("Before CALL getLastRunTimeStamp()");
		   	    cStmt = con.prepareCall("{ CALL getLastRunTimeStamp() }");
		   	    cStmt.execute();
		   	    rs = cStmt.getResultSet();

		   	    if(rs.next()) {
		   	    	String temp = rs.getString("lastRun");
		   	    	LOGGER.info("lastRun: " + temp);
		   	    	return(rs.getString("lastRun"));
		   	    } else {
		   	    	LOGGER.info("lastRun: Failure");
		   	    	return("Failure");
		   	    }
			} finally {
				 if (rs != null) {
					 rs.close();
				 }
		         if (con != null) {
		        	 con.close();
		         }
			}
		}

		 
		public void setLastRunTimeStamp() throws ClassNotFoundException, SQLException, Exception {

			//Logger LOGGER = Logger.getLogger("InfoLogging");
			try {
				Class.forName("com.mysql.jdbc.Driver");
		    	
		   	    con = DriverManager.getConnection(ServletConstants.dbURL, ServletConstants.dbUsername, ServletConstants.dbPassword);
		   	    CallableStatement cStmt = null;

		   	    cStmt = con.prepareCall("{ CALL setLastRunTimeStamp() }");
		   	    cStmt.execute();
			} finally {
		         if (con != null) {
		        	 con.close();
		         }
			}
		}


		
		public void sendServiceRequestPushNotifications(java.sql.Timestamp timestamp) throws ClassNotFoundException, SQLException, JsonSyntaxException, Exception {
		//Statement stmt = null;
		ResultSet rs = null;
		Logger LOGGER = Logger.getLogger("InfoLogging");
		try {
			Class.forName("com.mysql.jdbc.Driver");
	    	
			
	   	    con = DriverManager.getConnection(ServletConstants.dbURL, ServletConstants.dbUsername, ServletConstants.dbPassword);
	   	    CallableStatement cStmt = null;

	   	    cStmt = con.prepareCall("{ CALL getNewJobs(?) }");
	   	    cStmt.setTimestamp(1, timestamp);
	   	    cStmt.execute();
	   	    rs = cStmt.getResultSet();
	   	    
	   	    ArrayList<Job> jobs = new ArrayList<Job>();

	   	    while(rs.next()) {
	   	    	Job job = new Job(rs);
	   	    	jobs.add(job);
	   	    	LOGGER.info("sendServiceRequestPushNotifications - getNewJobs: " + job.jobId);
	   	    }
	   	    
	   	    if (jobs.size() == 0){
	   	    	return;
	   	    }
	   	    
	   	    final ApnsClient apnsClient = new ApnsClientBuilder()
	        .setClientCredentials(new File("/home/ec2-user/ProviderAPNSDeveloperCertificates.p12"), "ABCD3fgh!")
	        .build();
			final Future<Void> connectFuture = apnsClient.connect(ApnsClient.DEVELOPMENT_APNS_HOST);
			connectFuture.await();

	   	    for (Job job : jobs) {
	   	    	cStmt.close();
	   	    	cStmt = con.prepareCall("{ CALL getDeviceForUser(?) }");
		   	    cStmt.setInt(1, job.providerId);

		   	    cStmt.execute();
		   	    rs = cStmt.getResultSet();
		   	    
		   	    ArrayList<Device> devicesList = new ArrayList<Device>();
		   	    while(rs.next())
		   	    { 
		        	Device device = new Device(rs);
				    devicesList.add(device); 
		   	    }
		   	    
		   	    cStmt.close();
	   	    	cStmt = con.prepareCall("{ CALL getRangesForJobId(?) }");
		   	    cStmt.setInt(1, job.jobId);

		   	    cStmt.execute();
		   	    rs = cStmt.getResultSet();
		   	    
		   	    ArrayList<Range> rangeList = new ArrayList<Range>();
		   	    while(rs.next())
		   	    { 
		        	Range range = new Range(rs);
		        	rangeList.add(range); 
		   	    }
		   	    
		   	    
			   	 for (Device device : devicesList) {				   	    
			    	    final ApnsPayloadBuilder payloadBuilder = new ApnsPayloadBuilder();
			    	    //payloadBuilder.setContentAvailable(true);
			    	    //payloadBuilder.setMutableContent(true);
			    	    payloadBuilder.setCategoryName("serviceRequest.category");
			    	    payloadBuilder.setAlertTitle(job.name);
			    	    payloadBuilder.setAlertSubtitle("Client service request");
			    	    float avgRating = 0;
			    	    if(job.reviewCount > 0) {
			    	    	avgRating = (float)job.ratingSum/(float)job.reviewCount;
			    	    }
			    	    payloadBuilder.setAlertBody(job.address + "\n" + job.city + "\n" + String.format("%1.1f", avgRating) + " avg. stars, " + job.reviewCount + " total reviews");
			    	    payloadBuilder.setSoundFileName("default");
			    	    payloadBuilder.addCustomProperty("type", "NewJobs");
			    	    payloadBuilder.addCustomProperty("jobId", job.jobId);
			    	    payloadBuilder.addCustomProperty("clientId", job.clientId);
			    	    payloadBuilder.addCustomProperty("providerId", job.providerId);
			    	    payloadBuilder.addCustomProperty("latitude", job.latitude);
			    	    payloadBuilder.addCustomProperty("longitude", job.longitude);
			    	    payloadBuilder.addCustomProperty("address", job.address);
			    	    payloadBuilder.addCustomProperty("city", job.city);
			    	    payloadBuilder.addCustomProperty("state", job.state);
			    	    payloadBuilder.addCustomProperty("avgRating", avgRating);
			    	    payloadBuilder.addCustomProperty("reviewCount", job.reviewCount);
			    	    
			    	    //Map<String, ArrayList<Map<String, Object>>> rangesMap = new HashMap<String, ArrayList<Map<String, Object>>>();
			    	    ArrayList<Map<String, Object>> ranges = new ArrayList<Map<String, Object>>();

			    	    for(Range range : rangeList) {
			    	    	ranges.add(range.toArray());
			    	    }
			    	    //rangesMap.put("ranges", ranges);
			    	    payloadBuilder.addCustomProperty("ranges", ranges);
			    	    
			    	    final String payload = payloadBuilder.buildWithDefaultMaximumLength();
			    	    LOGGER.info("payload: " + payload);
						sendPushNotification(device, apnsClient, "Com.AvalancheEvantage.ProviderApp", payload);
						LOGGER.info("pid after: " + device.id);
						
			    	    final ApnsPayloadBuilder simplePayloadBuilder = new ApnsPayloadBuilder();
			    	    simplePayloadBuilder.setContentAvailable(true);
			    	    simplePayloadBuilder.addCustomProperty("type", "NewJobs");
			    	    
			    	    final String simplePayload = simplePayloadBuilder.buildWithDefaultMaximumLength();
			    	    LOGGER.info("simplePayload: " + simplePayload);
						sendPushNotification(device, apnsClient, "Com.AvalancheEvantage.ProviderApp", simplePayload);
						LOGGER.info("pid after: " + device.id);
						
					}
	   	    }    
		        
	        final Future<Void> disconnectFuture = apnsClient.disconnect();
	        disconnectFuture.await();

		} finally {
			 if (rs != null) {
				 rs.close();
			 }
	         if (con != null) {
	        	 con.close();
	         }
		}
	}

		
		public void sendInvalidatedServiceRequestPushNotifications(java.sql.Timestamp timestamp) throws ClassNotFoundException, SQLException, JsonSyntaxException, Exception {
		//Statement stmt = null;
		ResultSet rs = null;
		Logger LOGGER = Logger.getLogger("InfoLogging");
		LOGGER.info("sendInvalidatedServiceRequestPushNotifications - timestamp: " + timestamp);
		try {
			Class.forName("com.mysql.jdbc.Driver");
	    	
			
	   	    con = DriverManager.getConnection(ServletConstants.dbURL, ServletConstants.dbUsername, ServletConstants.dbPassword);
	   	    CallableStatement cStmt = null;

	   	    cStmt = con.prepareCall("{ CALL getPrvdrsWithInvalidatedServiceRequests(?) }");
	   	    cStmt.setTimestamp(1, timestamp);
	   	    cStmt.execute();
	   	    rs = cStmt.getResultSet();
	   	    
	   	    ArrayList<Integer> providerIds = new ArrayList<Integer>();

	   	    while(rs.next()) {
	   	    	providerIds.add(rs.getInt("id"));
	   	    	LOGGER.info("sendInvalidatedServiceRequestPushNotifications - getPrvdrsWithInvalidatedServiceRequests: " + rs.getInt("id"));
	   	    }
	   	    
	   	    
	   	 	ArrayList<Device> devicesList = new ArrayList<Device>();
	   	    for (Integer providerId : providerIds) {
	   	    	cStmt = con.prepareCall("{ CALL getDeviceForUser(?) }");
		   	    cStmt.setInt(1, providerId);

		   	    cStmt.execute();
		   	    rs = cStmt.getResultSet();
		   	    
		   	    while(rs.next())
		   	    { 
		        	Device device = new Device(rs);
				    devicesList.add(device); 
		   	    }
	   	    }
	   	    
	        LOGGER.info("sendInvalidatedServiceRequestPushNotifications -  devicesList.size(): " + devicesList.size());
	        
	        if (devicesList.size() > 0) {

	    	    
		        final ApnsClient apnsClient = new ApnsClientBuilder()
		        .setClientCredentials(new File("/home/ec2-user/ProviderAPNSDeveloperCertificates.p12"), "ABCD3fgh!")
		        .build();
				
				final Future<Void> connectFuture = apnsClient.connect(ApnsClient.DEVELOPMENT_APNS_HOST);
				connectFuture.await();
		        
				Date now = new Date();
		        for (Device device : devicesList) {
			   	    
		    	    final ApnsPayloadBuilder payloadBuilder = new ApnsPayloadBuilder();
		    	    payloadBuilder.setContentAvailable(true);
		    	    payloadBuilder.addCustomProperty("type", "InvalidatedJobs");
		    	    

		    	    final String payload = payloadBuilder.buildWithDefaultMaximumLength();
		    	    LOGGER.info("payload: " + payload);

		        	LOGGER.info("timestamp: " + now.getTime() + " pid: " + device.id);
					sendPushNotification(device, apnsClient, "Com.AvalancheEvantage.ProviderApp", payload);
					LOGGER.info("pid after: " + device.id);
				}
		        
		        final Future<Void> disconnectFuture = apnsClient.disconnect();
		        disconnectFuture.await();
	        }
		} finally {
			 if (rs != null) {
				 rs.close();
			 }
	         if (con != null) {
	        	 con.close();
	         }
		}
	}


		
	public void sendAcceptedOfferStatusPushNotifications() throws ClassNotFoundException, SQLException, JsonSyntaxException, Exception {
		ResultSet rs = null;
		Logger LOGGER = Logger.getLogger("InfoLogging");
		try {
			Class.forName("com.mysql.jdbc.Driver");
	    	
	   	    con = DriverManager.getConnection(ServletConstants.dbURL, ServletConstants.dbUsername, ServletConstants.dbPassword);
	   	    CallableStatement cStmt = null;

	   	    cStmt = con.prepareCall("{ CALL getPrvdrIdsForAcceptedOffers() }");
	   	    cStmt.execute();
	   	    rs = cStmt.getResultSet();
	   	    
	   	    ArrayList<OfferStatusWithAddress> offerStatusWithAddresses = new ArrayList<OfferStatusWithAddress>();

	   	    while(rs.next()) {
	   	    	offerStatusWithAddresses.add(new OfferStatusWithAddress(rs));
	   	    }
	   	    
	   	    final ApnsClient apnsClient = new ApnsClientBuilder()
	        .setClientCredentials(new File("/home/ec2-user/ProviderAPNSDeveloperCertificates.p12"), "ABCD3fgh!")
	        .build();
			
	   	    java.text.DateFormat df1 = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss.s");
	   	    java.text.DateFormat df2 = new SimpleDateFormat("EEE, MMM dd");

	   	    
			final Future<Void> connectFuture = apnsClient.connect(ApnsClient.DEVELOPMENT_APNS_HOST);
			connectFuture.await();

	   	 	//ArrayList<Device> devicesList = new ArrayList<Device>();
	   	    for (OfferStatusWithAddress offerStatusWithAddress : offerStatusWithAddresses) {
	   	    	cStmt.close();
	   	    	cStmt = con.prepareCall("{ CALL getDeviceForUser(?) }");
		   	    cStmt.setInt(1, offerStatusWithAddress.userId);

		   	    cStmt.execute();
		   	    rs = cStmt.getResultSet();
		   	    
		   	    final ApnsPayloadBuilder payloadBuilder = new ApnsPayloadBuilder();
		   	    if(offerStatusWithAddress.pushCounter == 0) {
	    	    	java.util.Date starttime = df1.parse(offerStatusWithAddress.starttime);
	    	    	payloadBuilder.setAlertTitle(offerStatusWithAddress.name);
	    	    	payloadBuilder.setAlertSubtitle("Client accepted job");
	    	    	payloadBuilder.setAlertBody(df2.format(starttime) + "\n" + offerStatusWithAddress.address + "\n" 
		    	    		+ offerStatusWithAddress.city);
		    	    payloadBuilder.setSoundFileName("default");
	    	    }
		   	    payloadBuilder.setContentAvailable(true);
		   	    payloadBuilder.setCategoryName("acceptOffer.category");
	    	    payloadBuilder.addCustomProperty("type", "OfferStatus");
	    	    payloadBuilder.addCustomProperty("offerId", offerStatusWithAddress.offerId);
	    	    payloadBuilder.addCustomProperty("status", offerStatusWithAddress.status);
	    	    final String payload = payloadBuilder.buildWithDefaultMaximumLength();
	    	    LOGGER.info("payload: " + payload);
	    	    
		   	    while(rs.next())
		   	    { 
		        	Device device = new Device(rs);
		        	LOGGER.info("token: " + device.token);
					sendPushNotification(device, apnsClient, "Com.AvalancheEvantage.ProviderApp", payload);
					LOGGER.info("pid after: " + device.id);
		   	    }
		   	    cStmt.close();
	   	    	cStmt = con.prepareCall("{ CALL updatePushCounterForOfferId(?) }");
		   	    cStmt.setInt(1, offerStatusWithAddress.offerId);
		   	    cStmt.execute();
	   	    }
	   	    final Future<Void> disconnectFuture = apnsClient.disconnect();
	        disconnectFuture.await();
		} finally {
			 if (rs != null) {
				 rs.close();
			 }
	         if (con != null) {
	        	 con.close();
	         }
		}
	}

	
	public void sendConfirmedOfferStatusPushNotifications() throws ClassNotFoundException, SQLException, JsonSyntaxException, Exception {
		ResultSet rs = null;
		Logger LOGGER = Logger.getLogger("InfoLogging");
		try {
			Class.forName("com.mysql.jdbc.Driver");
	    	
	   	    con = DriverManager.getConnection(ServletConstants.dbURL, ServletConstants.dbUsername, ServletConstants.dbPassword);
	   	    CallableStatement cStmt = null;
	
	   	    cStmt = con.prepareCall("{ CALL getClientIdsForConfirmedOffers() }");
	   	    cStmt.execute();
	   	    rs = cStmt.getResultSet();
	   	    
	   	    ArrayList<FullOffer> fullOffers = new ArrayList<FullOffer>();

	   	    while(rs.next()) {
	   	    	fullOffers.add(new FullOffer(rs));
	   	    }
	   	    
	   	    final ApnsClient apnsClient = new ApnsClientBuilder()
	        .setClientCredentials(new File("/home/ec2-user/ClientAPNSDeveloperCertificate.p12"), "ABCD3fgh!")
	        .build();
			
	   	    java.text.DateFormat df1 = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss.s");
	   	    java.text.DateFormat df2 = new SimpleDateFormat("EEE, MMM dd");
	   	    
			final Future<Void> connectFuture = apnsClient.connect(ApnsClient.DEVELOPMENT_APNS_HOST);
			connectFuture.await();
	
	   	 	//ArrayList<Device> devicesList = new ArrayList<Device>();
	   	    for (FullOffer fullOffer : fullOffers) {
	   	    	cStmt = con.prepareCall("{ CALL getDeviceForUser(?) }");
		   	    cStmt.setInt(1, fullOffer.clientId);
		   	    //cStmt.setInt(2, newAcceptOffer.get(1));
	
		   	    cStmt.execute();
		   	    rs = cStmt.getResultSet();
		   	    
		   	    final ApnsPayloadBuilder payloadBuilder = new ApnsPayloadBuilder();
		   	    payloadBuilder.setContentAvailable(true);
	    	    payloadBuilder.setCategoryName("confirmedOffer.category");
	    	    if(fullOffer.pushCounter == 0) {
	    	    	java.util.Date starttime = df1.parse(fullOffer.starttime);
	    	    	payloadBuilder.setAlertTitle(fullOffer.name);
	    	    	payloadBuilder.setAlertSubtitle("Provider confirmed job");
		    	    int hours = (int)fullOffer.duration; 
		    	    int minutes = (int)(((fullOffer.duration - hours) * 10) * 60);
		    	    String body = df2.format(starttime) + "\nDuration: ";
		    	    if (hours > 0) {
		    	    	if (hours == 1) {
		    	    		body += "1 hour ";
		    	    	} else {
		    	    		body += hours + " hours ";
		    	    	} 
		    	    }
		    	    if (minutes > 0) {
		    	    	if (minutes == 1) {
		    	    		body += "1 minute";
		    	    	} else {
		    	    		body += minutes + " minutes";
		    	    	}
		    	    }
		    	    body += "\nFee: " + String.format("$%3.2f", fullOffer.fee);
		    	    
		    	    /*
		    	    float avgRating = 0;
		    	    if(fullOffer.reviewCount > 0) {
		    	    	avgRating = (float)fullOffer.ratingSum/(float)fullOffer.reviewCount;
		    	    }
		    	    body += "\nProvider average rating: " + String.format("%1.1f", avgRating) + ", total reviews: " + fullOffer.reviewCount;
		    	    */
		    	    
		    	    payloadBuilder.setAlertBody(body);
		    	    payloadBuilder.setSoundFileName("default");
	    	    }

	    	    payloadBuilder.addCustomProperty("type", "OfferStatus");
	    	    payloadBuilder.addCustomProperty("offerId", fullOffer.offerId);
	    	    payloadBuilder.addCustomProperty("status", fullOffer.status);
	    	    payloadBuilder.addCustomProperty("clientId", fullOffer.clientId);
	    	    payloadBuilder.addCustomProperty("providerId", fullOffer.providerId);
	    	    payloadBuilder.addCustomProperty("jobId", fullOffer.jobId);
	    	    payloadBuilder.addCustomProperty("starttime", fullOffer.starttime);
	    	    payloadBuilder.addCustomProperty("fee", fullOffer.fee);
	    	    payloadBuilder.addCustomProperty("duration", fullOffer.duration);
	    	    payloadBuilder.addCustomProperty("latitude", fullOffer.latitude);
	    	    payloadBuilder.addCustomProperty("longitude", fullOffer.longitude);
	    	    payloadBuilder.addCustomProperty("address", fullOffer.address);
	    	    payloadBuilder.addCustomProperty("city", fullOffer.city);
	    	    payloadBuilder.addCustomProperty("state", fullOffer.state);
	    	    
	    	    final String payload = payloadBuilder.buildWithDefaultMaximumLength();
	    	    LOGGER.info("payload: " + payload);
		   	    
		   	    while(rs.next())
		   	    { 
		        	Device device = new Device(rs);
		        	LOGGER.info("token: " + device.token);
					sendPushNotification(device, apnsClient, "Com.AvalancheEvantage.ClientApp", payload);
					LOGGER.info("pid after: " + device.id);
		   	    }
		   	    cStmt.close();
	   	    	cStmt = con.prepareCall("{ CALL updatePushCounterForOfferId(?) }");
		   	    cStmt.setInt(1, fullOffer.offerId);
		   	    cStmt.execute();
	   	    }
	   	    final Future<Void> disconnectFuture = apnsClient.disconnect();
	        disconnectFuture.await();
		} finally {
			 if (rs != null) {
				 rs.close();
			 }
	         if (con != null) {
	        	 con.close();
	         }
		}
	}

	
	public void sendCompletedOfferStatusPushNotifications() throws ClassNotFoundException, SQLException, JsonSyntaxException, Exception {
		ResultSet rs = null;
		Logger LOGGER = Logger.getLogger("InfoLogging");
		try {
			Class.forName("com.mysql.jdbc.Driver");
	    	
	   	    con = DriverManager.getConnection(ServletConstants.dbURL, ServletConstants.dbUsername, ServletConstants.dbPassword);
	   	    CallableStatement cStmt = null;
	
	   	    cStmt = con.prepareCall("{ CALL getClientIdsForCompletedOffers() }");
	   	    cStmt.execute();
	   	    rs = cStmt.getResultSet();
	   	    
	   	    ArrayList<CompletedOffer> completedOfferStatuses = new ArrayList<CompletedOffer>();
	
	   	    while(rs.next()) {
	   	    	CompletedOffer newCompletedOffer = new CompletedOffer(rs);
	   	    	completedOfferStatuses.add(newCompletedOffer);
	   	    }
	   	    
	   	    final ApnsClient apnsClient = new ApnsClientBuilder()
	        .setClientCredentials(new File("/home/ec2-user/ClientAPNSDeveloperCertificate.p12"), "ABCD3fgh!")
	        .build();
			
	   	    java.text.DateFormat df1 = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
	   	    java.text.DateFormat df2 = new SimpleDateFormat("EEE, MMM dd");
	   	    java.text.DateFormat df3 = new SimpleDateFormat("hh:mm a");
	   	 
			final Future<Void> connectFuture = apnsClient.connect(ApnsClient.DEVELOPMENT_APNS_HOST);
			connectFuture.await();
	
	   	 	//ArrayList<Device> devicesList = new ArrayList<Device>();
	   	    for (CompletedOffer completedOffer : completedOfferStatuses) {
	   	    	cStmt.close();
	   	    	cStmt = con.prepareCall("{ CALL getDeviceForUser(?) }");
		   	    cStmt.setInt(1, completedOffer.userId);
		   	    //cStmt.setInt(2, newAcceptOffer.get(1));
	
		   	    cStmt.execute();
		   	    rs = cStmt.getResultSet();
		   	    
		   	    final ApnsPayloadBuilder payloadBuilder = new ApnsPayloadBuilder();
	    	    payloadBuilder.setContentAvailable(true);
	    	    if(completedOffer.pushCounter == 0) {
	    	    	java.util.Date beginTime = df1.parse(completedOffer.beginTime);
	    	    	java.util.Date endTime = df1.parse(completedOffer.endTime);
	    	    	payloadBuilder.setAlertTitle(completedOffer.name);
	    	    	payloadBuilder.setAlertSubtitle("Provider completed job");
		    	    int hours = (int)completedOffer.pausedTime; 
		    	    int minutes = (int)(((completedOffer.pausedTime - hours) * 10) * 60);
		    	    String body = df2.format(beginTime) + "\nDuration: " + df3.format(beginTime) + " - " + df3.format(endTime) + "\nPaused Time: ";
	    	    	if (hours == 1) {
	    	    		body += "1 hour ";
	    	    	} else {
	    	    		body += hours + " hours ";
	    	    	} 
	    	    	if (minutes == 1) {
	    	    		body += "1 minute";
	    	    	} else {
	    	    		body += minutes + " minutes";
	    	    	} 
	    	    	payloadBuilder.setAlertBody(body);
		    	    
		    	    payloadBuilder.setSoundFileName("default");
		    	    payloadBuilder.setCategoryName("completedOffer.category");
	    	    }
	    	    payloadBuilder.addCustomProperty("type", "OfferStatus");
	    	    payloadBuilder.addCustomProperty("offerId", completedOffer.offerId);
	    	    payloadBuilder.addCustomProperty("status", completedOffer.status);
	    	    
	    	    payloadBuilder.addCustomProperty("endTime", completedOffer.endTime);
	    	    payloadBuilder.addCustomProperty("beginTime", completedOffer.beginTime);
	    	    payloadBuilder.addCustomProperty("pausedTime", completedOffer.pausedTime);
	    	    final String payload = payloadBuilder.buildWithDefaultMaximumLength();
	    	    LOGGER.info("payload: " + payload);
		   	    
		   	    while(rs.next())
		   	    { 
		        	Device device = new Device(rs);
		        	LOGGER.info("token: " + device.token);
					sendPushNotification(device, apnsClient, "Com.AvalancheEvantage.ClientApp", payload);
					LOGGER.info("pid after: " + device.id);
		   	    }
		   	    
		   	    cStmt.close();
	   	    	cStmt = con.prepareCall("{ CALL updatePushCounterForOfferId(?) }");
		   	    cStmt.setInt(1, completedOffer.offerId);
		   	    cStmt.execute();
	   	    }
	   	    final Future<Void> disconnectFuture = apnsClient.disconnect();
	        disconnectFuture.await();
		} finally {
			 if (rs != null) {
				 rs.close();
			 }
	         if (con != null) {
	        	 con.close();
	         }
		}
	}


	
	void sendOnRouteOfferStatusPushNotifications() throws ClassNotFoundException, SQLException, JsonSyntaxException, Exception {
		ResultSet rs = null;
		CallableStatement cStmt = null;
		Logger LOGGER = Logger.getLogger("InfoLogging");
		try {
			Class.forName("com.mysql.jdbc.Driver");
	    	
	   	    con = DriverManager.getConnection(ServletConstants.dbURL, ServletConstants.dbUsername, ServletConstants.dbPassword);
	
	   	    cStmt = con.prepareCall("{ CALL getClientIdsForOnRouteOffers() }");
	   	    cStmt.execute();
	   	    rs = cStmt.getResultSet();
	   	    
	   	    ArrayList<OnRouteOffer> newOfferStatuses = new ArrayList<OnRouteOffer>();
	
	   	    while(rs.next()) {
	   	    	OnRouteOffer newOfferStatus = new OnRouteOffer(rs);
	   	    	newOfferStatuses.add(newOfferStatus);
	   	    }
	   	    
	   	    final ApnsClient apnsClient = new ApnsClientBuilder()
	        .setClientCredentials(new File("/home/ec2-user/ClientAPNSDeveloperCertificate.p12"), "ABCD3fgh!")
	        .build();
			
	   	    java.text.DateFormat df1 = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss.s");
	   	    java.text.DateFormat df2 = new SimpleDateFormat("EEE, MMM dd");
	   	    
			final Future<Void> connectFuture = apnsClient.connect(ApnsClient.DEVELOPMENT_APNS_HOST);
			connectFuture.await();
	
	   	 	//ArrayList<Device> devicesList = new ArrayList<Device>();
	   	    for (OnRouteOffer newOfferStatus : newOfferStatuses) {
	   	    	cStmt.close();
	   	    	cStmt = con.prepareCall("{ CALL getDeviceForUser(?) }");
		   	    cStmt.setInt(1, newOfferStatus.clientId);
		   	    //cStmt.setInt(2, newAcceptOffer.get(1));
	
		   	    cStmt.execute();
		   	    rs = cStmt.getResultSet();
		   	    
		   	    final ApnsPayloadBuilder payloadBuilder = new ApnsPayloadBuilder();
	    	    payloadBuilder.setContentAvailable(true);
	    	    payloadBuilder.setCategoryName("onRoute.category");
	    	    if(newOfferStatus.pushCounter == 0) {
	    	    	java.util.Date starttime = df1.parse(newOfferStatus.starttime);
	    	    	payloadBuilder.setAlertTitle(newOfferStatus.name);
	    	    	payloadBuilder.setAlertSubtitle("Provider on route");
	    	    	payloadBuilder.setAlertBody(df2.format(starttime));
	    	    	payloadBuilder.setSoundFileName("default");
	    	    }
	    	    
	    	    payloadBuilder.addCustomProperty("type", "OfferStatus");
	    	    payloadBuilder.addCustomProperty("offerId", newOfferStatus.offerId);
	    	    payloadBuilder.addCustomProperty("status", newOfferStatus.status);
	    	    payloadBuilder.addCustomProperty("latitude", newOfferStatus.latitude);
	    	    payloadBuilder.addCustomProperty("longitude", newOfferStatus.longitude);
	    	    payloadBuilder.addCustomProperty("starttime", newOfferStatus.starttime);
	    	    payloadBuilder.addCustomProperty("duration", newOfferStatus.duration);
	    	    payloadBuilder.addCustomProperty("fee", newOfferStatus.fee);
	    	    payloadBuilder.addCustomProperty("jobLatitude", newOfferStatus.jobLatitude);
	    	    payloadBuilder.addCustomProperty("jobLongitude", newOfferStatus.jobLongitude);
	    	    final String payload = payloadBuilder.buildWithDefaultMaximumLength();
	    	    LOGGER.info("payload: " + payload);
		   	    
		   	    while(rs.next())
		   	    { 
		        	Device device = new Device(rs);
		        	LOGGER.info("token: " + device.token);
					sendPushNotification(device, apnsClient, "Com.AvalancheEvantage.ClientApp", payload);
					LOGGER.info("pid after: " + device.id);
		   	    }
		   	    
	   	    	cStmt.close();
	   	    	cStmt = con.prepareCall("{ CALL updatePushCounterForOfferId(?) }");
		   	    cStmt.setInt(1, newOfferStatus.offerId);
		   	    cStmt.execute();
	   	    }
	   	    final Future<Void> disconnectFuture = apnsClient.disconnect();
	        disconnectFuture.await();
		} finally {
			 if (rs != null) {
				 rs.close();
			 }
			 if (cStmt != null) {
				 cStmt.close();
			 }
	         if (con != null) {
	        	 con.close();
	         }
		}
	}

	
	public void sendArrivedOfferStatusPushNotifications() throws ClassNotFoundException, SQLException, JsonSyntaxException, Exception {
		ResultSet rs = null;
		CallableStatement cStmt = null;
		Logger LOGGER = Logger.getLogger("InfoLogging");
		try {
			Class.forName("com.mysql.jdbc.Driver");
	    	
	   	    con = DriverManager.getConnection(ServletConstants.dbURL, ServletConstants.dbUsername, ServletConstants.dbPassword);
	   	    
	
	   	    cStmt = con.prepareCall("{ CALL getClientIdsForArrivedOffers() }");
	   	    cStmt.execute();
	   	    rs = cStmt.getResultSet();
	   	    
	   	    ArrayList<ArrivedStatus> newArrivedStatuses = new ArrayList<ArrivedStatus>();
	
	   	    while(rs.next()) {
	   	    	ArrivedStatus newOfferStatus = new ArrivedStatus(rs);
	   	    	newArrivedStatuses.add(newOfferStatus);
	   	    }
	   	    
	   	    final ApnsClient apnsClient = new ApnsClientBuilder()
	        .setClientCredentials(new File("/home/ec2-user/ClientAPNSDeveloperCertificate.p12"), "ABCD3fgh!")
	        .build();
			
			final Future<Void> connectFuture = apnsClient.connect(ApnsClient.DEVELOPMENT_APNS_HOST);
			connectFuture.await();
	
	   	 	//ArrayList<Device> devicesList = new ArrayList<Device>();
	   	    for (ArrivedStatus newArrivedStatus : newArrivedStatuses) {
	   	    	cStmt.close();
	   	    	cStmt = con.prepareCall("{ CALL getDeviceForUser(?) }");
		   	    cStmt.setInt(1, newArrivedStatus.userId);
		   	    //cStmt.setInt(2, newAcceptOffer.get(1));
	
		   	    cStmt.execute();
		   	    rs = cStmt.getResultSet();
		   	    
		   	    final ApnsPayloadBuilder payloadBuilder = new ApnsPayloadBuilder();
	    	    payloadBuilder.setContentAvailable(true);
	    	    
	    	    if(newArrivedStatus.pushCounter == 0) {
	    	    	payloadBuilder.setAlertTitle(newArrivedStatus.name);
	    	    	payloadBuilder.setAlertSubtitle("Provider arrived");
	    	    	payloadBuilder.setAlertBody("Launch app or expand the notification to authenticate");
	    	    	if(newArrivedStatus.jobAuthentication.equals("both")) {	    	
		    	    	payloadBuilder.setCategoryName("authBoth.category");
		    	    	payloadBuilder.addCustomProperty("imageURL", newArrivedStatus.imageURL);
		    	    	payloadBuilder.setMutableContent(true);
		    	    } else if (newArrivedStatus.jobAuthentication.equals("portrait")) {
		    	    	payloadBuilder.setCategoryName("authPortrait.category");
		    	    	payloadBuilder.addCustomProperty("imageURL", newArrivedStatus.imageURL);
		    	    	payloadBuilder.setMutableContent(true);
		    	    } else {
		    	    	payloadBuilder.setCategoryName("authPassword.category");
		    	    	payloadBuilder.setMutableContent(true);
		    	    }
	    	    	payloadBuilder.setSoundFileName("default");
	    	    }
	    	    
	    	    payloadBuilder.addCustomProperty("type", "OfferStatus");
	    	    payloadBuilder.addCustomProperty("offerId", newArrivedStatus.offerId);
	    	    payloadBuilder.addCustomProperty("status", newArrivedStatus.status);
	    	    payloadBuilder.addCustomProperty("prvdrPassword", newArrivedStatus.prvdrPassword);
	    	    payloadBuilder.addCustomProperty("clientPassword", newArrivedStatus.clientPassword);
	    	    payloadBuilder.addCustomProperty("firstname", newArrivedStatus.firstname);
	    	    payloadBuilder.addCustomProperty("lastname", newArrivedStatus.lastname);
	    	    payloadBuilder.addCustomProperty("jobAuthentication", newArrivedStatus.jobAuthentication);
	    	    
	    	    final String payload = payloadBuilder.buildWithDefaultMaximumLength();
	    	    LOGGER.info("payload: " + payload);
		   	    
		   	    while(rs.next())
		   	    { 
		        	Device device = new Device(rs);
		        	LOGGER.info("token: " + device.token);
					sendPushNotification(device, apnsClient, "Com.AvalancheEvantage.ClientApp", payload);
					LOGGER.info("pid after: " + device.id);
		   	    }
	   	    	cStmt.close();
	   	    	cStmt = con.prepareCall("{ CALL updatePushCounterForOfferId(?) }");
		   	    cStmt.setInt(1, newArrivedStatus.offerId);
		   	    cStmt.execute();
	   	    }
	   	    final Future<Void> disconnectFuture = apnsClient.disconnect();
	        disconnectFuture.await();
		} finally {
			 if (rs != null) {
				 rs.close();
			 }
			 if (cStmt != null) {
				 cStmt.close();
			 }
	         if (con != null) {
	        	 con.close();
	         }
		}
	}


	
	public void sendPrvdrAuthStatusPushNotifications() throws ClassNotFoundException, SQLException, JsonSyntaxException, Exception {
		ResultSet rs = null;
		Logger LOGGER = Logger.getLogger("InfoLogging");
		try {
			Class.forName("com.mysql.jdbc.Driver");
	    	
	   	    con = DriverManager.getConnection(ServletConstants.dbURL, ServletConstants.dbUsername, ServletConstants.dbPassword);
	   	    CallableStatement cStmt = null;
	
	   	    cStmt = con.prepareCall("{ CALL getClientIdsForPrvdrAuthOffers() }");
	   	    cStmt.execute();
	   	    rs = cStmt.getResultSet();
	   	    
	   	    ArrayList<AuthStatus> authStatuses = new ArrayList<AuthStatus>();

	   	    while(rs.next()) {
	   	    	authStatuses.add(new AuthStatus(rs));
	   	    }

	  	   	    
	   	    final ApnsClient apnsClient = new ApnsClientBuilder()
	        .setClientCredentials(new File("/home/ec2-user/ClientAPNSDeveloperCertificate.p12"), "ABCD3fgh!")
	        .build();
			
			final Future<Void> connectFuture = apnsClient.connect(ApnsClient.DEVELOPMENT_APNS_HOST);
			connectFuture.await();
	
	   	 	//ArrayList<Device> devicesList = new ArrayList<Device>();
			for (AuthStatus authStatus : authStatuses) {
	   	    	cStmt.close();
	   	    	cStmt = con.prepareCall("{ CALL getDeviceForUser(?) }");
		   	    cStmt.setInt(1, authStatus.userId);
		   	    //cStmt.setInt(2, newAcceptOffer.get(1));
	
		   	    cStmt.execute();
		   	    rs = cStmt.getResultSet();
		   	    
		   	    final ApnsPayloadBuilder payloadBuilder = new ApnsPayloadBuilder();
	    	    payloadBuilder.setContentAvailable(true);
	    	    //payloadBuilder.setAlertBody("Offer id " + authStatus.offerId + " - provider authenticated");
	    	    //payloadBuilder.setSoundFileName("default");
	    	    payloadBuilder.addCustomProperty("type", "AuthStatus");
	    	    payloadBuilder.addCustomProperty("offerId", authStatus.offerId);
	    	    final String payload = payloadBuilder.buildWithDefaultMaximumLength();
	    	    LOGGER.info("payload: " + payload);
		   	    
		   	    while(rs.next())
		   	    { 
		        	Device device = new Device(rs);
		        	LOGGER.info("token: " + device.token);
					sendPushNotification(device, apnsClient, "Com.AvalancheEvantage.ClientApp", payload);
					LOGGER.info("pid after: " + device.id);
		   	    }
	
	   	    }
	   	    final Future<Void> disconnectFuture = apnsClient.disconnect();
	        disconnectFuture.await();
		} finally {
			 if (rs != null) {
				 rs.close();
			 }
	         if (con != null) {
	        	 con.close();
	         }
		}
	}

	
	public void sendClientAuthStatusPushNotifications() throws ClassNotFoundException, SQLException, JsonSyntaxException, Exception {
		ResultSet rs = null;
		Logger LOGGER = Logger.getLogger("InfoLogging");
		try {
			Class.forName("com.mysql.jdbc.Driver");
	    	
	   	    con = DriverManager.getConnection(ServletConstants.dbURL, ServletConstants.dbUsername, ServletConstants.dbPassword);
	   	    CallableStatement cStmt = null;
	
	   	    cStmt = con.prepareCall("{ CALL getPrvdrIdsForClientAuthOffers() }");
	   	    cStmt.execute();
	   	    rs = cStmt.getResultSet();
	   	    
	   	    ArrayList<AuthStatus> authStatuses = new ArrayList<AuthStatus>();

	   	    while(rs.next()) {
	   	    	authStatuses.add(new AuthStatus(rs));
	   	    }

	  	   	    
	   	    final ApnsClient apnsClient = new ApnsClientBuilder()
	        .setClientCredentials(new File("/home/ec2-user/ProviderAPNSDeveloperCertificates.p12"), "ABCD3fgh!")
	        .build();
			
			final Future<Void> connectFuture = apnsClient.connect(ApnsClient.DEVELOPMENT_APNS_HOST);
			connectFuture.await();
	
	   	 	//ArrayList<Device> devicesList = new ArrayList<Device>();
			for (AuthStatus authStatus : authStatuses) {
	   	    	cStmt.close();
	   	    	cStmt = con.prepareCall("{ CALL getDeviceForUser(?) }");
		   	    cStmt.setInt(1, authStatus.userId);
		   	    //cStmt.setInt(2, newAcceptOffer.get(1));
	
		   	    cStmt.execute();
		   	    rs = cStmt.getResultSet();
		   	    
		   	    final ApnsPayloadBuilder payloadBuilder = new ApnsPayloadBuilder();
		   	    payloadBuilder.setContentAvailable(true);
	    	    //payloadBuilder.setAlertBody("Offer id " + authStatus.offerId + " - client authenticated");
	    	    //payloadBuilder.setSoundFileName("default");
	    	    payloadBuilder.addCustomProperty("type", "AuthStatus");
	    	    payloadBuilder.addCustomProperty("offerId", authStatus.offerId);
	    	    final String payload = payloadBuilder.buildWithDefaultMaximumLength();
	    	    LOGGER.info("payload: " + payload);
		   	    
		   	    while(rs.next())
		   	    { 
		        	Device device = new Device(rs);
		        	LOGGER.info("token: " + device.token);
					sendPushNotification(device, apnsClient, "Com.AvalancheEvantage.ProviderApp", payload);
					LOGGER.info("pid after: " + device.id);
		   	    }
	
	   	    }
	   	    final Future<Void> disconnectFuture = apnsClient.disconnect();
	        disconnectFuture.await();
		} finally {
			 if (rs != null) {
				 rs.close();
			 }
	         if (con != null) {
	        	 con.close();
	         }
		}
	}

	
	public void sendOfferDeclinedStatusPushNotifications() throws ClassNotFoundException, SQLException, JsonSyntaxException, Exception {
		ResultSet rs = null;
		Logger LOGGER = Logger.getLogger("InfoLogging");
		try {
			Class.forName("com.mysql.jdbc.Driver");
	    	
	   	    con = DriverManager.getConnection(ServletConstants.dbURL, ServletConstants.dbUsername, ServletConstants.dbPassword);
	   	    CallableStatement cStmt = null;
	
	   	    cStmt = con.prepareCall("{ CALL getPrvdrIdsForDeclinedOffers() }");
	   	    cStmt.execute();
	   	    rs = cStmt.getResultSet();
	   	    
	   	    ArrayList<SimpleOfferStatus> simpleOfferStatuses = new ArrayList<SimpleOfferStatus>();

	   	    while(rs.next()) {
	   	    	simpleOfferStatuses.add(new SimpleOfferStatus(rs));
	   	    }

	  	   	    
	   	    final ApnsClient apnsClient = new ApnsClientBuilder()
	        .setClientCredentials(new File("/home/ec2-user/ProviderAPNSDeveloperCertificates.p12"), "ABCD3fgh!")
	        .build();
			
	   	    java.text.DateFormat df1 = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss.s");
	   	    java.text.DateFormat df2 = new SimpleDateFormat("EEE, MMM dd");
	   	    
			final Future<Void> connectFuture = apnsClient.connect(ApnsClient.DEVELOPMENT_APNS_HOST);
			connectFuture.await();
	
	   	 	//ArrayList<Device> devicesList = new ArrayList<Device>();
			for (SimpleOfferStatus simpleOfferStatus : simpleOfferStatuses) {
				LOGGER.info("simpleOfferStatus.userId: " + simpleOfferStatus.userId);
	   	    	cStmt.close();
	   	    	cStmt = con.prepareCall("{ CALL getDeviceForUser(?) }");
		   	    cStmt.setInt(1, simpleOfferStatus.userId);
		   	    //cStmt.setInt(2, newAcceptOffer.get(1));
	
		   	    cStmt.execute();
		   	    rs = cStmt.getResultSet();
		   	    
		   	    final ApnsPayloadBuilder payloadBuilder = new ApnsPayloadBuilder();
		   	    payloadBuilder.setContentAvailable(true);
    	    	java.util.Date starttime = df1.parse(simpleOfferStatus.starttime);
    	    	
		   	    payloadBuilder.setAlertTitle(simpleOfferStatus.name);
		   	    payloadBuilder.setAlertSubtitle("Client declined offer");
		   	    payloadBuilder.setAlertBody(df2.format(starttime));
	    	    payloadBuilder.setSoundFileName("default");
	    	    payloadBuilder.addCustomProperty("type", "OfferStatus");
	    	    payloadBuilder.addCustomProperty("offerId", simpleOfferStatus.offerId);
	    	    payloadBuilder.addCustomProperty("status", simpleOfferStatus.status);
	    	    final String payload = payloadBuilder.buildWithDefaultMaximumLength();
	    	    LOGGER.info("payload: " + payload);
		   	    
		   	    while(rs.next())
		   	    { 
		        	Device device = new Device(rs);
		        	LOGGER.info("token: " + device.token);
					sendPushNotification(device, apnsClient, "Com.AvalancheEvantage.ProviderApp", payload);
					LOGGER.info("pid after: " + device.id);
		   	    }
	
	   	    }
	   	    final Future<Void> disconnectFuture = apnsClient.disconnect();
	        disconnectFuture.await();
		} finally {
			 if (rs != null) {
				 rs.close();
			 }
	         if (con != null) {
	        	 con.close();
	         }
		}
	}


	
	public void sendServiceOfferPushNotifications(java.sql.Timestamp timestamp) throws ClassNotFoundException, SQLException, JsonSyntaxException, Exception {
		ResultSet rs = null;
		Logger LOGGER = Logger.getLogger("InfoLogging");
		LOGGER.info("sendServiceOfferPushNotifications timestamp: " + timestamp);
		try {
			Class.forName("com.mysql.jdbc.Driver");
	    	
	   	    con = DriverManager.getConnection(ServletConstants.dbURL, ServletConstants.dbUsername, ServletConstants.dbPassword);
	   	    CallableStatement cStmt = null;

	   	    cStmt = con.prepareCall("{ CALL getNewOffers(?) }");
	   	    cStmt.setTimestamp(1, timestamp);
	   	    cStmt.execute();
	   	    rs = cStmt.getResultSet();
	   	    
	   	    ArrayList<FullOffer> fullOffers = new ArrayList<FullOffer>();

	   	    while(rs.next()) {
	   	    	fullOffers.add(new FullOffer(rs));
	   	    }
	   	    
	   	    LOGGER.info("sendServiceOfferPushNotifications fullOffers.size(): " + fullOffers.size());
	   	    
	   	    if (fullOffers.size() == 0) {
	   	    	return;
	   	    }
	   	    
	   	    final ApnsClient apnsClient = new ApnsClientBuilder()
	        .setClientCredentials(new File("/home/ec2-user/ClientAPNSDeveloperCertificate.p12"), "ABCD3fgh!")
	        .build();
			
	   	    java.text.DateFormat df1 = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss.s");
	   	    java.text.DateFormat df2 = new SimpleDateFormat("EEE, MMM dd");

	   	    
			final Future<Void> connectFuture = apnsClient.connect(ApnsClient.DEVELOPMENT_APNS_HOST);
			connectFuture.await();

	   	 	//ArrayList<Device> devicesList = new ArrayList<Device>();
	   	    for (FullOffer fullOffer : fullOffers) {
	   	    	cStmt.close();
	   	    	cStmt = con.prepareCall("{ CALL getDeviceForUser(?) }");
		   	    cStmt.setInt(1, fullOffer.clientId);

		   	    cStmt.execute();
		   	    rs = cStmt.getResultSet();
		   	    
	        	final ApnsPayloadBuilder payloadBuilder = new ApnsPayloadBuilder();
	        	java.util.Date starttime = df1.parse(fullOffer.starttime);
	        	payloadBuilder.setAlertTitle(fullOffer.name);
	        	payloadBuilder.setAlertSubtitle("Provider Service Offer");
	    	    int hours = (int)fullOffer.duration; 
	    	    int minutes = (int)(((fullOffer.duration - hours) * 10) * 60);
	    	    String body = df2.format(starttime) + "\nDuration: ";
	    	    if (hours > 0) {
	    	    	if (hours == 1) {
	    	    		body += "1 hour ";
	    	    	} else {
	    	    		body += hours + " hours ";
	    	    	} 
	    	    }
	    	    if (minutes > 0) {
	    	    	if (minutes == 1) {
	    	    		body += "1 minute";
	    	    	} else {
	    	    		body += minutes + " minutes";
	    	    	}
	    	    }
	    	    body += "\nFee: " + String.format("$%3.2f", fullOffer.fee);
	    	    
	    	    float avgRating = 0;
	    	    if(fullOffer.reviewCount > 0) {
	    	    	avgRating = (float)fullOffer.ratingSum/(float)fullOffer.reviewCount;
	    	    }
	    	    body += "\nProvider average rating: " + String.format("%1.1f", avgRating) + ", total reviews: " + fullOffer.reviewCount;
	    	    payloadBuilder.setAlertBody(body);
	    	    payloadBuilder.setSoundFileName("default");
	    	    payloadBuilder.setCategoryName("serviceOffer.category");
	    	    payloadBuilder.setContentAvailable(true);
	    	    payloadBuilder.addCustomProperty("type", "NewServiceOffers");
	    	    payloadBuilder.addCustomProperty("offerId", fullOffer.offerId);
	    	    payloadBuilder.addCustomProperty("clientId", fullOffer.clientId);
	    	    payloadBuilder.addCustomProperty("providerId", fullOffer.providerId);
	    	    payloadBuilder.addCustomProperty("rangeId", fullOffer.rangeId);
	    	    payloadBuilder.addCustomProperty("jobId", fullOffer.jobId);
	    	    payloadBuilder.addCustomProperty("starttime", fullOffer.starttime);
	    	    payloadBuilder.addCustomProperty("duration", fullOffer.duration);
	    	    payloadBuilder.addCustomProperty("ratingSum", fullOffer.ratingSum);
	    	    payloadBuilder.addCustomProperty("reviewCount", fullOffer.reviewCount);
	    	    final String payload = payloadBuilder.buildWithDefaultMaximumLength();
	    	    LOGGER.info("payload: " + payload);
		   	    
		   	    while(rs.next())
		   	    { 
		        	Device device = new Device(rs);

					sendPushNotification(device, apnsClient, "Com.AvalancheEvantage.ClientApp", payload);
					LOGGER.info("pid after: " + device.id);
		   	    }

	   	    }
	   	    final Future<Void> disconnectFuture = apnsClient.disconnect();
	        disconnectFuture.await();
		} finally {
			 if (rs != null) {
				 rs.close();
			 }
	         if (con != null) {
	        	 con.close();
	         }
		}
	}

	public void sendClientCanceledOfferStatusPushNotifications() throws ClassNotFoundException, SQLException, JsonSyntaxException, Exception {
		ResultSet rs = null;
		Logger LOGGER = Logger.getLogger("InfoLogging");
		try {
			Class.forName("com.mysql.jdbc.Driver");
	    	
	   	    con = DriverManager.getConnection(ServletConstants.dbURL, ServletConstants.dbUsername, ServletConstants.dbPassword);
	   	    CallableStatement cStmt = null;
	
	   	    cStmt = con.prepareCall("{ CALL getPrvdrIdsForCanceledOffers() }");
	   	    cStmt.execute();
	   	    rs = cStmt.getResultSet();
	   	    
	   	    ArrayList<OfferStatusWithAddress> offerStatusWithAddresses = new ArrayList<OfferStatusWithAddress>();

	   	    while(rs.next()) {
	   	    	offerStatusWithAddresses.add(new OfferStatusWithAddress(rs));
	   	    }
	   	    
	   	    final ApnsClient apnsClient = new ApnsClientBuilder()
	        .setClientCredentials(new File("/home/ec2-user/ProviderAPNSDeveloperCertificates.p12"), "ABCD3fgh!")
	        .build();
			
	   	    java.text.DateFormat df1 = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss.s");
	   	    java.text.DateFormat df2 = new SimpleDateFormat("EEE, MMM dd");
	   	    
			final Future<Void> connectFuture = apnsClient.connect(ApnsClient.DEVELOPMENT_APNS_HOST);
			connectFuture.await();
	
	   	 	//ArrayList<Device> devicesList = new ArrayList<Device>();
			for (OfferStatusWithAddress offerStatusWithAddress : offerStatusWithAddresses) {
	   	    	cStmt.close();
	   	    	cStmt = con.prepareCall("{ CALL getDeviceForUser(?) }");
		   	    cStmt.setInt(1, offerStatusWithAddress.userId);
		   	    //cStmt.setInt(2, newAcceptOffer.get(1));
	
		   	    cStmt.execute();
		   	    rs = cStmt.getResultSet();
		   	    
		   	    final ApnsPayloadBuilder payloadBuilder = new ApnsPayloadBuilder();
		   	    java.util.Date starttime = df1.parse(offerStatusWithAddress.starttime);
		   	    payloadBuilder.setContentAvailable(true);
		   	    payloadBuilder.setAlertTitle(offerStatusWithAddress.name);
		   	    payloadBuilder.setAlertSubtitle("Client canceled job");
	    	    payloadBuilder.setAlertBody(df2.format(starttime) + "\n" + offerStatusWithAddress.address + "\n" 
	    	    		+ offerStatusWithAddress.city);
	    	    payloadBuilder.setSoundFileName("default");
	    	    payloadBuilder.addCustomProperty("type", "OfferStatus");
	    	    payloadBuilder.addCustomProperty("offerId", offerStatusWithAddress.offerId);
	    	    payloadBuilder.addCustomProperty("status", offerStatusWithAddress.status);
	    	    final String payload = payloadBuilder.buildWithDefaultMaximumLength();
	    	    LOGGER.info("payload: " + payload);
		   	    
		   	    while(rs.next())
		   	    { 
		        	Device device = new Device(rs);
		        	LOGGER.info("token: " + device.token);
					sendPushNotification(device, apnsClient, "Com.AvalancheEvantage.ProviderApp", payload);
					LOGGER.info("pid after: " + device.id);
		   	    }
	
	   	    }
			
	   	  	final Future<Void> disconnectFuture = apnsClient.disconnect();
	        disconnectFuture.await();
		} finally {
			 if (rs != null) {
				 rs.close();
			 }
	         if (con != null) {
	        	 con.close();
	         }
		}
	}

	public void sendPrvdrCanceledOfferStatusPushNotifications() throws ClassNotFoundException, SQLException, JsonSyntaxException, Exception {
		ResultSet rs = null;
		Logger LOGGER = Logger.getLogger("InfoLogging");
		try {
			Class.forName("com.mysql.jdbc.Driver");
	    	
	   	    con = DriverManager.getConnection(ServletConstants.dbURL, ServletConstants.dbUsername, ServletConstants.dbPassword);
	   	    CallableStatement cStmt = null;
	
	   	    cStmt = con.prepareCall("{ CALL getClientIdsForPrvdrCanceledOffers() }");
	   	    cStmt.execute();
	   	    rs = cStmt.getResultSet();
	   	    
	   	    ArrayList<SimpleOfferStatus> simpleClientOfferStatuses = new ArrayList<SimpleOfferStatus>();

	   	    while(rs.next()) {
	   	    	simpleClientOfferStatuses.add(new SimpleOfferStatus(rs));
	   	    }
	   	    
	   	    final ApnsClient apnsClient = new ApnsClientBuilder()
	        .setClientCredentials(new File("/home/ec2-user/ClientAPNSDeveloperCertificate.p12"), "ABCD3fgh!")
	        .build();
			
	   	    java.text.DateFormat df1 = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss.s");
	   	    java.text.DateFormat df2 = new SimpleDateFormat("EEE, MMM dd");
	   	    
			final Future<Void> connectFuture = apnsClient.connect(ApnsClient.DEVELOPMENT_APNS_HOST);
			connectFuture.await();
	
	   	 	//ArrayList<Device> devicesList = new ArrayList<Device>();
			for (SimpleOfferStatus simpleOfferStatus : simpleClientOfferStatuses) {
	   	    	cStmt.close();
	   	    	cStmt = con.prepareCall("{ CALL getDeviceForUser(?) }");
		   	    cStmt.setInt(1, simpleOfferStatus.userId);
		   	    //cStmt.setInt(2, newAcceptOffer.get(1));
	
		   	    cStmt.execute();
		   	    rs = cStmt.getResultSet();
		   	    
		   	    final ApnsPayloadBuilder payloadBuilder = new ApnsPayloadBuilder();
		   	    payloadBuilder.setContentAvailable(true);
		   	    payloadBuilder.setAlertTitle(simpleOfferStatus.name);
	        	java.util.Date starttime = df1.parse(simpleOfferStatus.starttime);
	        	payloadBuilder.setAlertSubtitle("Provider canceled service offer");
	        	payloadBuilder.setAlertBody(df2.format(starttime));
	    	    payloadBuilder.setSoundFileName("default");
	    	    payloadBuilder.addCustomProperty("type", "OfferStatus");
	    	    payloadBuilder.addCustomProperty("offerId", simpleOfferStatus.offerId);
	    	    payloadBuilder.addCustomProperty("status", simpleOfferStatus.status);
	    	    final String payload = payloadBuilder.buildWithDefaultMaximumLength();
	    	    LOGGER.info("payload: " + payload);
		   	    
		   	    while(rs.next())
		   	    { 
		        	Device device = new Device(rs);
		        	LOGGER.info("token: " + device.token);
					sendPushNotification(device, apnsClient, "Com.AvalancheEvantage.ClientApp", payload);
					LOGGER.info("pid after: " + device.id);
		   	    }
	
	   	    }
			
	   	  	final Future<Void> disconnectFuture = apnsClient.disconnect();
	        disconnectFuture.await();
		} finally {
			 if (rs != null) {
				 rs.close();
			 }
	         if (con != null) {
	        	 con.close();
	         }
		}
	}
	
	private void unregisterDevice(Device device) throws Exception {
		Class.forName("com.mysql.jdbc.Driver");
    	
   	    con = DriverManager.getConnection(ServletConstants.dbURL, ServletConstants.dbUsername, ServletConstants.dbPassword);
   	    CallableStatement cStmt = null;

   	    cStmt = con.prepareCall("{ CALL unregisterDevice(?) }");
   	    cStmt.setInt(1, device.id);
   	    cStmt.execute();
	}
	
	private void sendPushNotification(Device device, ApnsClient apnsClient, String appName, String payload) throws Exception {
		Logger LOGGER = Logger.getLogger("InfoLogging");
	    final SimpleApnsPushNotification pushNotification = new SimpleApnsPushNotification(device.token, appName, payload);
	    
	    final Future<PushNotificationResponse<SimpleApnsPushNotification>> sendNotificationFuture =
	            apnsClient.sendNotification(pushNotification);
	    
	    try {
	        final PushNotificationResponse<SimpleApnsPushNotification> pushNotificationResponse =
	                sendNotificationFuture.get();

	        if (pushNotificationResponse.isAccepted()) {
	        	LOGGER.info("Push notification accepted by APNs gateway.");
	        } else {
	        	LOGGER.info("Notification rejected by the APNs gateway: " +
	                    pushNotificationResponse.getRejectionReason());

	            if (pushNotificationResponse.getTokenInvalidationTimestamp() != null) {
	            	LOGGER.info("\t…and the token is invalid as of " +
	                    pushNotificationResponse.getTokenInvalidationTimestamp());
	            }
	            
	            if(pushNotificationResponse.getRejectionReason().equalsIgnoreCase("Unregistered")) {
	            	LOGGER.info("Unregistered...?" + device);
	            	unregisterDevice(device);
	            }
	        }

	    } catch (final ExecutionException e) {
	        System.err.println("Failed to send push notification.");
	        e.printStackTrace();

	        if (e.getCause() instanceof ClientNotConnectedException) {
	        	LOGGER.info("Waiting for client to reconnect…");
	            apnsClient.getReconnectionFuture().await();
	            LOGGER.info("Reconnected.");
	        }
	    }
	    
	}
	
}
