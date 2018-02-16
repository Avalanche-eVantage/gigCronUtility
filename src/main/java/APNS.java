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

	   	    cStmt = con.prepareCall("{ CALL getNewServiceRequests(?) }");
	   	    cStmt.setTimestamp(1, timestamp);
	   	    cStmt.execute();
	   	    rs = cStmt.getResultSet();
	   	    
	   	    ArrayList<ServiceRequest> serviceRequests = new ArrayList<ServiceRequest>();

	   	    while(rs.next()) {
	   	    	ServiceRequest serviceRequest = new ServiceRequest(rs);
	   	    	serviceRequests.add(serviceRequest);
	   	    	LOGGER.info("sendServiceRequestPushNotifications - getNewServiceRequests: " + serviceRequest.serviceRequestId);
	   	    }
	   	    
	   	    if (serviceRequests.size() == 0){
	   	    	return;
	   	    }
	   	    
	   	    final ApnsClient apnsClient = new ApnsClientBuilder()
	        .setClientCredentials(new File("/home/ec2-user/ProviderAPNSDeveloperCertificates.p12"), "ABCD3fgh!")
	        .build();
			final Future<Void> connectFuture = apnsClient.connect(ApnsClient.DEVELOPMENT_APNS_HOST);
			connectFuture.await();

	   	    for (ServiceRequest serviceRequest : serviceRequests) {
	   	    	cStmt.close();
	   	    	cStmt = con.prepareCall("{ CALL getDeviceForUser(?) }");
		   	    cStmt.setInt(1, serviceRequest.providerId);

		   	    cStmt.execute();
		   	    rs = cStmt.getResultSet();
		   	    
		   	    ArrayList<Device> devicesList = new ArrayList<Device>();
		   	    while(rs.next())
		   	    { 
		        	Device device = new Device(rs);
				    devicesList.add(device); 
		   	    }
		   	    
		   	    cStmt.close();
	   	    	cStmt = con.prepareCall("{ CALL getRangesForServiceRequest(?) }");
		   	    cStmt.setInt(1, serviceRequest.serviceRequestId);

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
			    	    
			    	    if ((rangeList.size() == 1) && (!rangeList.get(0).isAmbiguous())) {
			    	    	payloadBuilder.setCategoryName("fullServiceRequest.category");
			    	    	Range range = rangeList.get(0);
			    	    	payloadBuilder.addCustomProperty("date", range.date);
			    	    	LOGGER.info("range.date: " + range.date);
			    	    	payloadBuilder.addCustomProperty("start", range.minstart);
			    	    	payloadBuilder.addCustomProperty("duration", range.minduration);
			    	    	payloadBuilder.addCustomProperty("useFeeSchedule", range.useFeeSchedule);
			    	    	payloadBuilder.addCustomProperty("bidtype", range.bidtype);
			    	    	payloadBuilder.addCustomProperty("bid", range.bid);
			    	    	payloadBuilder.addCustomProperty("rangeId", range.id);
			    	    } else {
			    	    	payloadBuilder.setCategoryName("serviceRequest.category");
			    	    }
			    	    
			    	    
			    	    payloadBuilder.setAlertTitle(serviceRequest.name);
			    	    payloadBuilder.setAlertSubtitle("Client service request");
			    	    float avgRating = 0;
			    	    if(serviceRequest.reviewCount > 0) {
			    	    	avgRating = (float)serviceRequest.ratingSum/(float)serviceRequest.reviewCount;
			    	    }
			    	    payloadBuilder.setAlertBody(serviceRequest.address + "\n" + serviceRequest.city + "\n" + String.format("%1.1f", avgRating) + " avg. stars, " + serviceRequest.reviewCount + " total reviews");
			    	    payloadBuilder.setSoundFileName("default");
			    	    payloadBuilder.addCustomProperty("userId", device.userId);
			    	    payloadBuilder.addCustomProperty("serviceRequestId", serviceRequest.serviceRequestId);
			    	    payloadBuilder.addCustomProperty("clientId", serviceRequest.clientId);
			    	    payloadBuilder.addCustomProperty("providerId", serviceRequest.providerId);
			    	    payloadBuilder.addCustomProperty("latitude", serviceRequest.latitude);
			    	    payloadBuilder.addCustomProperty("longitude", serviceRequest.longitude);
			    	    payloadBuilder.addCustomProperty("address", serviceRequest.address);
			    	    payloadBuilder.addCustomProperty("city", serviceRequest.city);
			    	    payloadBuilder.addCustomProperty("state", serviceRequest.state);
			    	    payloadBuilder.addCustomProperty("avgRating", avgRating);
			    	    payloadBuilder.addCustomProperty("reviewCount", serviceRequest.reviewCount);
			    	    
			    	    //Map<String, ArrayList<Map<String, Object>>> rangesMap = new HashMap<String, ArrayList<Map<String, Object>>>();
			    	    ArrayList<Map<String, Object>> ranges = new ArrayList<Map<String, Object>>();

			    	    for(Range range : rangeList) {
			    	    	ranges.add(range.toArray());
			    	    }
			    	    payloadBuilder.addCustomProperty("ranges", ranges);
			    	    final String payload = payloadBuilder.buildWithDefaultMaximumLength();
			    	    LOGGER.info("payload: " + payload);
						sendPushNotification(device, apnsClient, "Com.AvalancheEvantage.ProviderApp", payload);
						LOGGER.info("pid after: " + device.id);
						
			    	    final ApnsPayloadBuilder simplePayloadBuilder = new ApnsPayloadBuilder();
			    	    simplePayloadBuilder.setContentAvailable(true);
			    	    
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

		public void sendScheduledJobPushNotifications(java.sql.Timestamp timestamp) throws ClassNotFoundException, SQLException, JsonSyntaxException, Exception {
			//Statement stmt = null;
			ResultSet rs = null;
			Logger LOGGER = Logger.getLogger("InfoLogging");
			try {
				Class.forName("com.mysql.jdbc.Driver");
		    	
				
		   	    con = DriverManager.getConnection(ServletConstants.dbURL, ServletConstants.dbUsername, ServletConstants.dbPassword);
		   	    CallableStatement cStmt = null;

		   	    cStmt = con.prepareCall("{ CALL getNewScheduledJobs(?) }");
		   	    cStmt.setTimestamp(1, timestamp);
		   	    cStmt.execute();
		   	    rs = cStmt.getResultSet();
		   	    
		   	 ArrayList<ScheduledJob> scheduledJobs = new ArrayList<ScheduledJob>();
		 	
		   	    while(rs.next()) {
		   	    	ScheduledJob scheduledJob = new ScheduledJob(rs);
		   	    	scheduledJobs.add(scheduledJob);
		   	    }
		   	    
		   	    if (scheduledJobs.size() == 0) {
		   	    	return;
		   	    }
		   	    
		   	    final ApnsClient apnsClient = new ApnsClientBuilder()
		        .setClientCredentials(new File("/home/ec2-user/ProviderAPNSDeveloperCertificates.p12"), "ABCD3fgh!")
		        .build();
				final Future<Void> connectFuture = apnsClient.connect(ApnsClient.DEVELOPMENT_APNS_HOST);
				connectFuture.await();

		   	    java.text.DateFormat df1 = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss.s");
		   	    java.text.DateFormat df2 = new SimpleDateFormat("EEE, MMM dd");
				
				for (ScheduledJob scheduledJob : scheduledJobs) {
		   	    	cStmt.close();
		   	    	cStmt = con.prepareCall("{ CALL getDeviceForUser(?) }");
			   	    cStmt.setInt(1, scheduledJob.providerId);

			   	    cStmt.execute();
			   	    rs = cStmt.getResultSet();
			   	    
			   	    ArrayList<Device> devicesList = new ArrayList<Device>();
			   	    while(rs.next())
			   	    { 
			        	Device device = new Device(rs);
					    devicesList.add(device); 
			   	    }
			   	    
		    	    final ApnsPayloadBuilder payloadBuilder = new ApnsPayloadBuilder();
		    	    payloadBuilder.setContentAvailable(true);
		    	    payloadBuilder.setCategoryName("newScheduledJob.category");
		    	    if(scheduledJob.pushCounter == 0) {
		    	    	java.util.Date start = df1.parse(scheduledJob.start);
		    	    	payloadBuilder.setAlertTitle(scheduledJob.name);
		    	    	payloadBuilder.setAlertSubtitle("Fully confirmed job");
			        	payloadBuilder.setAlertBody(df2.format(start));
		    	    	payloadBuilder.setSoundFileName("default");
		    	    }
		    	    payloadBuilder.addCustomProperty("address", scheduledJob.address);
		    	    payloadBuilder.addCustomProperty("city", scheduledJob.city);
		    	    payloadBuilder.addCustomProperty("state", scheduledJob.state);
		    	    payloadBuilder.addCustomProperty("start", scheduledJob.start);
		    	    payloadBuilder.addCustomProperty("duration", scheduledJob.duration);
		    	    payloadBuilder.addCustomProperty("providerCompensation", scheduledJob.providerCompensation);
		    	    payloadBuilder.addCustomProperty("fee", scheduledJob.fee);
		    	    payloadBuilder.addCustomProperty("scheduledJobId", scheduledJob.id);
		    	    payloadBuilder.addCustomProperty("userId", scheduledJob.providerId);
		    	    payloadBuilder.addCustomProperty("status", scheduledJob.status);
				   	 for (Device device : devicesList) {				   	    

				    	    
				    	    final String simplePayload = payloadBuilder.buildWithDefaultMaximumLength();
				    	    LOGGER.info("simplePayload: " + simplePayload);
							sendPushNotification(device, apnsClient, "Com.AvalancheEvantage.ProviderApp", simplePayload);
							LOGGER.info("pid after: " + device.id);
							

							
						}
			   	    	cStmt.close();
			   	    	cStmt = con.prepareCall("{ CALL updateScheduledJobPushCounter(?) }");
				   	    cStmt.setInt(1, scheduledJob.id);
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
		    	    payloadBuilder.setCategoryName("invalidatedServiceRequests.category");
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

	   	    cStmt = con.prepareCall("{ CALL getAcceptedServiceOffers() }");
	   	    cStmt.execute();
	   	    rs = cStmt.getResultSet();
	   	    
	   	    ArrayList<ServiceOffer> serviceOffers = new ArrayList<ServiceOffer>();

	   	    while(rs.next()) {
	   	    	serviceOffers.add(new ServiceOffer(rs));
	   	    }
	   	    
	   	    LOGGER.info("sendServiceOfferPushNotifications serviceOffers.size(): " + serviceOffers.size());
	   	    
	   	    if (serviceOffers.size() == 0) {
	   	    	return;
	   	    }
	   	    
	   	    final ApnsClient apnsClient = new ApnsClientBuilder()
	        .setClientCredentials(new File("/home/ec2-user/ProviderAPNSDeveloperCertificates.p12"), "ABCD3fgh!")
	        .build();
			
	   	    java.text.DateFormat df1 = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss.s");
	   	    java.text.DateFormat df2 = new SimpleDateFormat("EEE, MMM dd");

	   	    
			final Future<Void> connectFuture = apnsClient.connect(ApnsClient.DEVELOPMENT_APNS_HOST);
			connectFuture.await();

	   	 	//ArrayList<Device> devicesList = new ArrayList<Device>();
	   	    for (ServiceOffer serviceOffer : serviceOffers) {
	   	    	cStmt.close();
	   	    	cStmt = con.prepareCall("{ CALL getDeviceForUser(?) }");
		   	    cStmt.setInt(1, serviceOffer.providerId);

		   	    cStmt.execute();
		   	    rs = cStmt.getResultSet();
		   	    
		   	    final ApnsPayloadBuilder payloadBuilder = new ApnsPayloadBuilder();
		   	    payloadBuilder.setContentAvailable(true);
		   	    payloadBuilder.setCategoryName("acceptOffer.category");
		   	    if(serviceOffer.pushCounter == 0) {
	    	    	java.util.Date starttime = df1.parse(serviceOffer.starttime);
	    	    	payloadBuilder.setAlertTitle(serviceOffer.name);
	    	    	payloadBuilder.setAlertSubtitle("Client accepted offer");
	    	    	payloadBuilder.setAlertBody(df2.format(starttime) + "\n" + serviceOffer.address + "\n" 
		    	    		+ serviceOffer.city);
		    	    payloadBuilder.setSoundFileName("default");
	    	    }
	    	    payloadBuilder.addCustomProperty("serviceOfferId", serviceOffer.serviceOfferId);
	    	    payloadBuilder.addCustomProperty("userId", serviceOffer.providerId);
	    	    payloadBuilder.addCustomProperty("status", serviceOffer.status);
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
	   	    	cStmt = con.prepareCall("{ CALL updateServiceOfferPushCounter(?) }");
		   	    cStmt.setInt(1, serviceOffer.serviceOfferId);
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
	
	   	    cStmt = con.prepareCall("{ CALL getConfirmedServiceOffers() }");
	   	    cStmt.execute();
	   	    rs = cStmt.getResultSet();
	   	    
	   	 ArrayList<ServiceOffer> serviceOffers = new ArrayList<ServiceOffer>();

	   	    while(rs.next()) {
	   	    	serviceOffers.add(new ServiceOffer(rs));
	   	    }
	   	    
	   	    LOGGER.info("sendConfirmedOfferStatusPushNotifications serviceOffers.size(): " + serviceOffers.size());
	   	    
	   	    if (serviceOffers.size() == 0) {
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
			for (ServiceOffer serviceOffer : serviceOffers) {
	   	    	cStmt = con.prepareCall("{ CALL getDeviceForUser(?) }");
		   	    cStmt.setInt(1, serviceOffer.clientId);
		   	    //cStmt.setInt(2, newAcceptOffer.get(1));
	
		   	    cStmt.execute();
		   	    rs = cStmt.getResultSet();
		   	    
		   	    final ApnsPayloadBuilder payloadBuilder = new ApnsPayloadBuilder();
		   	    payloadBuilder.setContentAvailable(true);
	    	    payloadBuilder.setCategoryName("confirmedOffer.category");
	    	    if(serviceOffer.pushCounter == 0) {
	    	    	java.util.Date starttime = df1.parse(serviceOffer.starttime);
	    	    	payloadBuilder.setAlertTitle(serviceOffer.name);
	    	    	payloadBuilder.setAlertSubtitle("Provider confirmed job");
		    	    int hours = (int)serviceOffer.duration; 
		    	    int minutes = (int)((serviceOffer.duration - hours) * 60);
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
		    	    body += "\nFee: " + String.format("$%3.2f", serviceOffer.fee);
		    	    
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
	    	    payloadBuilder.addCustomProperty("serviceOfferId", serviceOffer.serviceOfferId);
	    	    payloadBuilder.addCustomProperty("status", serviceOffer.status);
	    	    payloadBuilder.addCustomProperty("clientId", serviceOffer.clientId);
	    	    payloadBuilder.addCustomProperty("userId", serviceOffer.clientId);
	    	    payloadBuilder.addCustomProperty("providerId", serviceOffer.providerId);
	    	    payloadBuilder.addCustomProperty("serviceRequestId", serviceOffer.serviceRequestId);
	    	    payloadBuilder.addCustomProperty("starttime", serviceOffer.starttime);
	    	    payloadBuilder.addCustomProperty("fee", serviceOffer.fee);
	    	    payloadBuilder.addCustomProperty("duration", serviceOffer.duration);
	    	    payloadBuilder.addCustomProperty("latitude", serviceOffer.latitude);
	    	    payloadBuilder.addCustomProperty("longitude", serviceOffer.longitude);
	    	    payloadBuilder.addCustomProperty("address", serviceOffer.address);
	    	    payloadBuilder.addCustomProperty("city", serviceOffer.city);
	    	    payloadBuilder.addCustomProperty("state", serviceOffer.state);
	    	    
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
		   	    cStmt = con.prepareCall("{ CALL updateServiceOfferPushCounter(?) }");
		   	    cStmt.setInt(1, serviceOffer.serviceOfferId);
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

	/*
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
		    	    payloadBuilder.setCategoryName("completedJob.category");
	    	    }
	    	    payloadBuilder.addCustomProperty("type", "OfferStatus");
	    	    payloadBuilder.addCustomProperty("offerId", completedOffer.offerId);
	    	    payloadBuilder.addCustomProperty("userId", completedOffer.userId);
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
	*/

	
	void sendOnRouteJobStatusPushNotifications() throws ClassNotFoundException, SQLException, JsonSyntaxException, Exception {
		ResultSet rs = null;
		CallableStatement cStmt = null;
		Logger LOGGER = Logger.getLogger("InfoLogging");
		try {
			Class.forName("com.mysql.jdbc.Driver");
	    	
	   	    con = DriverManager.getConnection(ServletConstants.dbURL, ServletConstants.dbUsername, ServletConstants.dbPassword);
	
	   	    cStmt = con.prepareCall("{ CALL getOnRouteScheduledJobs() }");
	   	    cStmt.execute();
	   	    rs = cStmt.getResultSet();
	   	    
	   	    ArrayList<ScheduledJob> scheduledJobs = new ArrayList<ScheduledJob>();
	
	   	    while(rs.next()) {
	   	    	ScheduledJob scheduledJob = new ScheduledJob(rs);
	   	    	scheduledJobs.add(scheduledJob);
	   	    }
	   	    
	   	    if (scheduledJobs.size() == 0) {
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
	   	    for (ScheduledJob scheduledJob : scheduledJobs) {
	   	    	cStmt.close();
	   	    	cStmt = con.prepareCall("{ CALL getDeviceForUser(?) }");
		   	    cStmt.setInt(1, scheduledJob.clientId);
		   	    //cStmt.setInt(2, newAcceptOffer.get(1));
	
		   	    cStmt.execute();
		   	    rs = cStmt.getResultSet();
		   	    
		   	    final ApnsPayloadBuilder payloadBuilder = new ApnsPayloadBuilder();
	    	    payloadBuilder.setContentAvailable(true);
	    	    payloadBuilder.setCategoryName("onRoute.category");
	    	    if(scheduledJob.status.equals("onroute")) {
	    	    	java.util.Date starttime = df1.parse(scheduledJob.start);
	    	    	payloadBuilder.setAlertTitle(scheduledJob.name);
	    	    	payloadBuilder.setAlertSubtitle("Provider on route");
	    	    	payloadBuilder.setAlertBody(df2.format(starttime));
	    	    	payloadBuilder.setSoundFileName("default");
	    	    }
	    	    payloadBuilder.addCustomProperty("scheduledJobId", scheduledJob.id);
	    	    payloadBuilder.addCustomProperty("status", scheduledJob.status);
	    	    payloadBuilder.addCustomProperty("userId", scheduledJob.clientId);
	    	    payloadBuilder.addCustomProperty("status", scheduledJob.status);
	    	    payloadBuilder.addCustomProperty("latitude", scheduledJob.latitude);
	    	    payloadBuilder.addCustomProperty("longitude", scheduledJob.longitude);
	    	    payloadBuilder.addCustomProperty("starttime", scheduledJob.start);
	    	    payloadBuilder.addCustomProperty("duration", scheduledJob.duration);
	    	    payloadBuilder.addCustomProperty("fee", scheduledJob.fee);
	    	    payloadBuilder.addCustomProperty("onroutelat", scheduledJob.onroutelat);
	    	    payloadBuilder.addCustomProperty("onroutelong", scheduledJob.onroutelong);
	    	    final String payload = payloadBuilder.buildWithDefaultMaximumLength();
	    	    LOGGER.info("payload: " + payload);
		   	    
		   	    while(rs.next())
		   	    { 
		        	Device device = new Device(rs);
		        	LOGGER.info("token: " + device.token);
					sendPushNotification(device, apnsClient, "Com.AvalancheEvantage.ClientApp", payload);
					LOGGER.info("pid after: " + device.id);
		   	    }
		   	    
		   	    /*
	   	    	cStmt.close();
	   	    	cStmt = con.prepareCall("{ CALL updateScheduledJobPushCounter(?) }");
		   	    cStmt.setInt(1, scheduledJob.id);
		   	    cStmt.execute();
		   	    */
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

	
	public void sendArrivedJobStatusPushNotifications() throws ClassNotFoundException, SQLException, JsonSyntaxException, Exception {
		ResultSet rs = null;
		CallableStatement cStmt = null;
		Logger LOGGER = Logger.getLogger("InfoLogging");
		try {
			Class.forName("com.mysql.jdbc.Driver");
	    	
	   	    con = DriverManager.getConnection(ServletConstants.dbURL, ServletConstants.dbUsername, ServletConstants.dbPassword);
	   	    
	
	   	    cStmt = con.prepareCall("{ CALL getPrvdrArrived() }");
	   	    cStmt.execute();
	   	    rs = cStmt.getResultSet();
	   	    
	   	    ArrayList<ScheduledJob> scheduledJobs = new ArrayList<ScheduledJob>();
		 	
	   	    while(rs.next()) {
	   	    	ScheduledJob scheduledJob = new ScheduledJob(rs);
	   	    	scheduledJobs.add(scheduledJob);
	   	    }
	   	    
	   	    if (scheduledJobs.size() == 0) {
	   	    	return;
	   	    }

	   	    
	   	    final ApnsClient apnsClient = new ApnsClientBuilder()
	        .setClientCredentials(new File("/home/ec2-user/ClientAPNSDeveloperCertificate.p12"), "ABCD3fgh!")
	        .build();
			
			final Future<Void> connectFuture = apnsClient.connect(ApnsClient.DEVELOPMENT_APNS_HOST);
			connectFuture.await();

	   	    //java.text.DateFormat df1 = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss.s");
	   	    //java.text.DateFormat df2 = new SimpleDateFormat("EEE, MMM dd");
	   	    
			for (ScheduledJob scheduledJob : scheduledJobs) {
	   	    	cStmt.close();
	   	    	cStmt = con.prepareCall("{ CALL getDeviceForUser(?) }");
		   	    cStmt.setInt(1, scheduledJob.clientId);
	
		   	    cStmt.execute();
		   	    rs = cStmt.getResultSet();
		   	    
		   	    final ApnsPayloadBuilder payloadBuilder = new ApnsPayloadBuilder();
		   	    payloadBuilder.setContentAvailable(true);
	    	    
	    	    payloadBuilder.setCategoryName("clientAuth.category");
	    	    if(scheduledJob.pushCounter == 0) {
	    	    	payloadBuilder.setSoundFileName("default");
	    	    	payloadBuilder.setAlertTitle(scheduledJob.name);
	    	    	payloadBuilder.setAlertSubtitle("Provider arrived");
	    	    	
	    	    	if(scheduledJob.jobAuthentication.equals("both")) {	    	
		    	    	payloadBuilder.setCategoryName("authBoth.category");
		    	    	payloadBuilder.addCustomProperty("imageURL", scheduledJob.imageURL);
		    	    	payloadBuilder.setAlertBody("Exchange passwords and compare portraits to authenticate");
		    	    	payloadBuilder.setAlertSubtitle("Client password: " + scheduledJob.clientPassword);
		    	    	payloadBuilder.setMutableContent(true);
		    	    } else if (scheduledJob.jobAuthentication.equals("portrait")) {
		    	    	payloadBuilder.setCategoryName("authPortrait.category");
		    	    	payloadBuilder.addCustomProperty("imageURL", scheduledJob.imageURL);
		    	    	payloadBuilder.setAlertBody("Compare portraits to authenticate");
		    	    	payloadBuilder.setMutableContent(true);
		    	    } else {
		    	    	payloadBuilder.setCategoryName("authPassword.category");
		    	    	payloadBuilder.setAlertBody("Exchange passwords to authenticate");
		    	    	payloadBuilder.setAlertSubtitle("Client password: " + scheduledJob.clientPassword);
		    	    }
	    	    }
	    	    payloadBuilder.addCustomProperty("address", scheduledJob.address);
	    	    payloadBuilder.addCustomProperty("city", scheduledJob.city);
	    	    payloadBuilder.addCustomProperty("state", scheduledJob.state);
	    	    payloadBuilder.addCustomProperty("start", scheduledJob.start);
	    	    payloadBuilder.addCustomProperty("duration", scheduledJob.duration);
	    	    payloadBuilder.addCustomProperty("providerCompensation", scheduledJob.providerCompensation);
	    	    payloadBuilder.addCustomProperty("fee", scheduledJob.fee);
	    	    payloadBuilder.addCustomProperty("scheduledJobId", scheduledJob.id);
	    	    payloadBuilder.addCustomProperty("userId", scheduledJob.clientId);
	    	    payloadBuilder.addCustomProperty("providerId", scheduledJob.providerId);
	    	    payloadBuilder.addCustomProperty("status", scheduledJob.status);
	    	    payloadBuilder.addCustomProperty("prvdrPassword", scheduledJob.prvdrPassword);
	    	    payloadBuilder.addCustomProperty("clientPassword", scheduledJob.clientPassword);
	    	    payloadBuilder.addCustomProperty("firstname", scheduledJob.firstname);
	    	    payloadBuilder.addCustomProperty("lastname", scheduledJob.lastname);
	    	    payloadBuilder.addCustomProperty("jobAuthentication", scheduledJob.jobAuthentication);
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
	   	    	
	   	    	cStmt = con.prepareCall("{ CALL updateScheduledJobPushCounter(?) }");
		   	    cStmt.setInt(1, scheduledJob.id);
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
	
	   	    cStmt = con.prepareCall("{ CALL getPrvdrAuth() }");
	   	    cStmt.execute();
	   	    rs = cStmt.getResultSet();
	   	    
	   	    ArrayList<ScheduledJob> scheduledJobs = new ArrayList<ScheduledJob>();
		 	
	   	    while(rs.next()) {
	   	    	ScheduledJob scheduledJob = new ScheduledJob(rs);
	   	    	scheduledJobs.add(scheduledJob);
	   	    }
	   	    
	   	    if (scheduledJobs.size() == 0) {
	   	    	return;
	   	    }

	  	   	    
	   	    final ApnsClient apnsClient = new ApnsClientBuilder()
	        .setClientCredentials(new File("/home/ec2-user/ClientAPNSDeveloperCertificate.p12"), "ABCD3fgh!")
	        .build();
			
			final Future<Void> connectFuture = apnsClient.connect(ApnsClient.DEVELOPMENT_APNS_HOST);
			connectFuture.await();
	
	   	    java.text.DateFormat df1 = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss.s");
	   	    java.text.DateFormat df2 = new SimpleDateFormat("EEE, MMM dd");
	   	    
			for (ScheduledJob scheduledJob : scheduledJobs) {
	   	    	cStmt.close();
	   	    	cStmt = con.prepareCall("{ CALL getDeviceForUser(?) }");
		   	    cStmt.setInt(1, scheduledJob.clientId);
	
		   	    cStmt.execute();
		   	    rs = cStmt.getResultSet();
		   	    
		   	    final ApnsPayloadBuilder payloadBuilder = new ApnsPayloadBuilder();
		   	    payloadBuilder.setContentAvailable(true);
	    	    payloadBuilder.setCategoryName("prvdrAuth.category");
	    	    //if(scheduledJob.pushCounter == 0) {
	    	    	java.util.Date start = df1.parse(scheduledJob.start);
	    	    	payloadBuilder.setAlertTitle(scheduledJob.name);
	    	    	payloadBuilder.setAlertSubtitle("Provider authenticated");
		        	payloadBuilder.setAlertBody(df2.format(start));
	    	    	payloadBuilder.setSoundFileName("default");
	    	    //}
	    	    payloadBuilder.addCustomProperty("address", scheduledJob.address);
	    	    payloadBuilder.addCustomProperty("city", scheduledJob.city);
	    	    payloadBuilder.addCustomProperty("state", scheduledJob.state);
	    	    payloadBuilder.addCustomProperty("start", scheduledJob.start);
	    	    payloadBuilder.addCustomProperty("duration", scheduledJob.duration);
	    	    payloadBuilder.addCustomProperty("providerCompensation", scheduledJob.providerCompensation);
	    	    payloadBuilder.addCustomProperty("fee", scheduledJob.fee);
	    	    payloadBuilder.addCustomProperty("scheduledJobId", scheduledJob.id);
	    	    payloadBuilder.addCustomProperty("userId", scheduledJob.providerId);
	    	    payloadBuilder.addCustomProperty("status", scheduledJob.status);
	    	    payloadBuilder.addCustomProperty("clientauth", scheduledJob.clientauth);
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
	   	    	/*
	   	    	cStmt = con.prepareCall("{ CALL updateScheduledJobPushCounter(?) }");
		   	    cStmt.setInt(1, scheduledJob.id);
		   	    cStmt.execute();
		   	    */
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
	
	   	    cStmt = con.prepareCall("{ CALL getClientAuth() }");
	   	    cStmt.execute();
	   	    rs = cStmt.getResultSet();
	   	    
	   	    ArrayList<ScheduledJob> scheduledJobs = new ArrayList<ScheduledJob>();
		 	
	   	    while(rs.next()) {
	   	    	ScheduledJob scheduledJob = new ScheduledJob(rs);
	   	    	scheduledJobs.add(scheduledJob);
	   	    }
	   	    
	   	    if (scheduledJobs.size() == 0) {
	   	    	return;
	   	    }

	  	   	    
	   	    final ApnsClient apnsClient = new ApnsClientBuilder()
	        .setClientCredentials(new File("/home/ec2-user/ProviderAPNSDeveloperCertificates.p12"), "ABCD3fgh!")
	        .build();
			
			final Future<Void> connectFuture = apnsClient.connect(ApnsClient.DEVELOPMENT_APNS_HOST);
			connectFuture.await();
	
	   	    java.text.DateFormat df1 = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss.s");
	   	    java.text.DateFormat df2 = new SimpleDateFormat("EEE, MMM dd");
			
			for (ScheduledJob scheduledJob : scheduledJobs) {
	   	    	cStmt.close();
	   	    	cStmt = con.prepareCall("{ CALL getDeviceForUser(?) }");
		   	    cStmt.setInt(1, scheduledJob.providerId);
	
		   	    cStmt.execute();
		   	    rs = cStmt.getResultSet();
		   	    
		   	    final ApnsPayloadBuilder payloadBuilder = new ApnsPayloadBuilder();
		   	    payloadBuilder.setContentAvailable(true);
	    	    payloadBuilder.setCategoryName("clientAuth.category");
	    	    //if(scheduledJob.pushCounter == 0) {
	    	    	java.util.Date start = df1.parse(scheduledJob.start);
	    	    	payloadBuilder.setAlertTitle(scheduledJob.name);
	    	    	payloadBuilder.setAlertSubtitle("Client authenticated");
		        	payloadBuilder.setAlertBody(df2.format(start));
	    	    	payloadBuilder.setSoundFileName("default");
	    	    //}
	    	    payloadBuilder.addCustomProperty("address", scheduledJob.address);
	    	    payloadBuilder.addCustomProperty("city", scheduledJob.city);
	    	    payloadBuilder.addCustomProperty("state", scheduledJob.state);
	    	    payloadBuilder.addCustomProperty("start", scheduledJob.start);
	    	    payloadBuilder.addCustomProperty("duration", scheduledJob.duration);
	    	    payloadBuilder.addCustomProperty("providerCompensation", scheduledJob.providerCompensation);
	    	    payloadBuilder.addCustomProperty("fee", scheduledJob.fee);
	    	    payloadBuilder.addCustomProperty("scheduledJobId", scheduledJob.id);
	    	    payloadBuilder.addCustomProperty("userId", scheduledJob.providerId);
	    	    payloadBuilder.addCustomProperty("status", scheduledJob.status);
	    	    payloadBuilder.addCustomProperty("clientauth", scheduledJob.clientauth);
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
	   	    	//cStmt = con.prepareCall("{ CALL updateScheduledJobPushCounter(?) }");
		   	    //cStmt.setInt(1, scheduledJob.id);
		   	    //cStmt.execute();
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
	
	   	    cStmt = con.prepareCall("{ CALL getDeclinedServiceOffers() }");
	   	    cStmt.execute();
	   	    rs = cStmt.getResultSet();
	   	    
	   	    ArrayList<ServiceOffer> serviceOffers = new ArrayList<ServiceOffer>();

	   	    while(rs.next()) {
	   	    	serviceOffers.add(new ServiceOffer(rs));
	   	    }
	   	    
	   	    LOGGER.info("sendOfferDeclinedStatusPushNotifications serviceOffers.size(): " + serviceOffers.size());
	   	    
	   	    if (serviceOffers.size() == 0) {
	   	    	return;
	   	    }

	  	   	    
	   	    final ApnsClient apnsClient = new ApnsClientBuilder()
	        .setClientCredentials(new File("/home/ec2-user/ProviderAPNSDeveloperCertificates.p12"), "ABCD3fgh!")
	        .build();
			
	   	    java.text.DateFormat df1 = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss.s");
	   	    java.text.DateFormat df2 = new SimpleDateFormat("EEE, MMM dd");
	   	    
			final Future<Void> connectFuture = apnsClient.connect(ApnsClient.DEVELOPMENT_APNS_HOST);
			connectFuture.await();
	
	   	 	//ArrayList<Device> devicesList = new ArrayList<Device>();
			for (ServiceOffer serviceOffer : serviceOffers) {
	   	    	cStmt.close();
	   	    	cStmt = con.prepareCall("{ CALL getDeviceForUser(?) }");
		   	    cStmt.setInt(1, serviceOffer.providerId);
		   	    //cStmt.setInt(2, newAcceptOffer.get(1));
	
		   	    cStmt.execute();
		   	    rs = cStmt.getResultSet();
		   	    
		   	    final ApnsPayloadBuilder payloadBuilder = new ApnsPayloadBuilder();
		   	    payloadBuilder.setCategoryName("offerDeclined.category");
		   	    payloadBuilder.setContentAvailable(true);
    	    	java.util.Date starttime = df1.parse(serviceOffer.starttime);
    	    	if(serviceOffer.pushCounter == 0) {
    	    		payloadBuilder.setAlertTitle(serviceOffer.name);
    		   	    payloadBuilder.setAlertSubtitle("Client declined offer");
    		   	    payloadBuilder.setAlertBody(df2.format(starttime));
    	    	    payloadBuilder.setSoundFileName("default");
    	    	}

	    	    payloadBuilder.addCustomProperty("serviceOfferId", serviceOffer.serviceOfferId);
	    	    payloadBuilder.addCustomProperty("status", serviceOffer.status);
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
	   	    	cStmt = con.prepareCall("{ CALL updateServiceOfferPushCounter(?) }");
		   	    cStmt.setInt(1, serviceOffer.serviceOfferId);
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


	
	public void sendServiceOfferPushNotifications(java.sql.Timestamp timestamp) throws ClassNotFoundException, SQLException, JsonSyntaxException, Exception {
		ResultSet rs = null;
		Logger LOGGER = Logger.getLogger("InfoLogging");
		LOGGER.info("sendServiceOfferPushNotifications timestamp: " + timestamp);
		try {
			Class.forName("com.mysql.jdbc.Driver");
	    	
	   	    con = DriverManager.getConnection(ServletConstants.dbURL, ServletConstants.dbUsername, ServletConstants.dbPassword);
	   	    CallableStatement cStmt = null;

	   	    cStmt = con.prepareCall("{ CALL getNewServiceOffers(?) }");
	   	    cStmt.setTimestamp(1, timestamp);
	   	    cStmt.execute();
	   	    rs = cStmt.getResultSet();
	   	    
	   	    ArrayList<ServiceOffer> serviceOffers = new ArrayList<ServiceOffer>();

	   	    while(rs.next()) {
	   	    	serviceOffers.add(new ServiceOffer(rs));
	   	    }
	   	    
	   	    LOGGER.info("sendServiceOfferPushNotifications serviceOffers.size(): " + serviceOffers.size());
	   	    
	   	    if (serviceOffers.size() == 0) {
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
	   	    for (ServiceOffer serviceOffer : serviceOffers) {
	   	    	cStmt.close();
	   	    	cStmt = con.prepareCall("{ CALL getDeviceForUser(?) }");
		   	    cStmt.setInt(1, serviceOffer.clientId);

		   	    cStmt.execute();
		   	    rs = cStmt.getResultSet();
		   	    
	        	final ApnsPayloadBuilder payloadBuilder = new ApnsPayloadBuilder();
	        	java.util.Date starttime = df1.parse(serviceOffer.starttime);
	        	payloadBuilder.setAlertTitle(serviceOffer.name);
	        	payloadBuilder.setAlertSubtitle("Provider Service Offer");
	    	    int hours = (int)serviceOffer.duration; 
	    	    int minutes = (int)((serviceOffer.duration - hours) * 60);
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
	    	    body += "\nFee: " + String.format("$%3.2f", serviceOffer.fee);
	    	    
	    	    float avgRating = 0;
	    	    if(serviceOffer.reviewCount > 0) {
	    	    	avgRating = (float)serviceOffer.ratingSum/(float)serviceOffer.reviewCount;
	    	    }
	    	    body += "\nProvider average rating: " + String.format("%1.1f", avgRating) + ", total reviews: " + serviceOffer.reviewCount;
	    	    payloadBuilder.setAlertBody(body);
	    	    payloadBuilder.setSoundFileName("default");
	    	    payloadBuilder.setCategoryName("serviceOffer.category");
	    	    payloadBuilder.setContentAvailable(true);

	    	    payloadBuilder.addCustomProperty("serviceOfferId", serviceOffer.serviceOfferId);
	    	    payloadBuilder.addCustomProperty("userId", serviceOffer.clientId);
	    	    payloadBuilder.addCustomProperty("clientId", serviceOffer.clientId);
	    	    payloadBuilder.addCustomProperty("providerId", serviceOffer.providerId);
	    	    payloadBuilder.addCustomProperty("rangeId", serviceOffer.rangeId);
	    	    payloadBuilder.addCustomProperty("serviceRequestId", serviceOffer.serviceRequestId);
	    	    payloadBuilder.addCustomProperty("start", serviceOffer.starttime);
	    	    payloadBuilder.addCustomProperty("duration", serviceOffer.duration);
	    	    payloadBuilder.addCustomProperty("fee", serviceOffer.fee);
	    	    payloadBuilder.addCustomProperty("latitude", serviceOffer.latitude);
	    	    payloadBuilder.addCustomProperty("longitude", serviceOffer.longitude);
	    	    payloadBuilder.addCustomProperty("address", serviceOffer.address);
	    	    payloadBuilder.addCustomProperty("city", serviceOffer.city);
	    	    payloadBuilder.addCustomProperty("state", serviceOffer.state);
	    	    payloadBuilder.addCustomProperty("ratingSum", serviceOffer.ratingSum);
	    	    payloadBuilder.addCustomProperty("reviewCount", serviceOffer.reviewCount);
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
	
	   	    // deprecated the following procedure
	   	    cStmt = con.prepareCall("{ CALL getPrvdrIdsForClientCanceledOffers() }");
	   	    cStmt.execute();
	   	    rs = cStmt.getResultSet();
	   	    
	   	    ArrayList<CancelledOffer> cancelledOffers = new ArrayList<CancelledOffer>();

	   	    while(rs.next()) {
	   	    	cancelledOffers.add(new CancelledOffer(rs));
	   	    }
	   	    
	   	    final ApnsClient apnsClient = new ApnsClientBuilder()
	        .setClientCredentials(new File("/home/ec2-user/ProviderAPNSDeveloperCertificates.p12"), "ABCD3fgh!")
	        .build();
			
	   	    java.text.DateFormat df1 = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss.s");
	   	    java.text.DateFormat df2 = new SimpleDateFormat("EEE, MMM dd");
	   	    
			final Future<Void> connectFuture = apnsClient.connect(ApnsClient.DEVELOPMENT_APNS_HOST);
			connectFuture.await();
	
	   	 	//ArrayList<Device> devicesList = new ArrayList<Device>();
			for (CancelledOffer cancelledOffer : cancelledOffers) {
	   	    	cStmt.close();
	   	    	cStmt = con.prepareCall("{ CALL getDeviceForUser(?) }");
		   	    cStmt.setInt(1, cancelledOffer.userId);
		   	    //cStmt.setInt(2, newAcceptOffer.get(1));
	
		   	    cStmt.execute();
		   	    rs = cStmt.getResultSet();
		   	    
		   	    final ApnsPayloadBuilder payloadBuilder = new ApnsPayloadBuilder();
		   	    java.util.Date starttime = df1.parse(cancelledOffer.starttime);
		   	    payloadBuilder.setContentAvailable(true);
		   	    payloadBuilder.setAlertTitle(cancelledOffer.name);
		   	    payloadBuilder.setAlertSubtitle("Client canceled job");
	    	    payloadBuilder.setAlertBody(df2.format(starttime) + "\n" + cancelledOffer.address + "\n" 
	    	    		+ cancelledOffer.city);
	    	    payloadBuilder.setSoundFileName("default");
	    	    payloadBuilder.addCustomProperty("type", "OfferStatus");
	    	    payloadBuilder.addCustomProperty("offerId", cancelledOffer.offerId);
	    	    payloadBuilder.addCustomProperty("status", cancelledOffer.status);
	    	    payloadBuilder.addCustomProperty("cancelationType", cancelledOffer.cancelationType);
	    	    
	    	    if(!cancelledOffer.cancelationType.equals("early")) {
	    	    	payloadBuilder.addCustomProperty("amount", cancelledOffer.amount);
	    	    	payloadBuilder.addCustomProperty("reason", cancelledOffer.reason);
	    	    }
	    	    
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

	
	public void sendClientNoShowNotifications() throws ClassNotFoundException, SQLException, JsonSyntaxException, Exception {
		ResultSet rs = null;
		Logger LOGGER = Logger.getLogger("InfoLogging");
		try {
			Class.forName("com.mysql.jdbc.Driver");
	    	
	   	    con = DriverManager.getConnection(ServletConstants.dbURL, ServletConstants.dbUsername, ServletConstants.dbPassword);
	   	    CallableStatement cStmt = null;
	
	   	    cStmt = con.prepareCall("{ CALL getClientNoShowJobs() }");
	   	    cStmt.execute();
	   	    rs = cStmt.getResultSet();
	   	    
	   	    ArrayList<ScheduledJob> scheduledJobs = new ArrayList<ScheduledJob>();
	 	
	   	    while(rs.next()) {
	   	    	ScheduledJob scheduledJob = new ScheduledJob(rs);
	   	    	scheduledJobs.add(scheduledJob);
	   	    }
	   	    
	   	    if (scheduledJobs.size() == 0) {
	   	    	return;
	   	    }
	   	    
	   	    final ApnsClient apnsClient = new ApnsClientBuilder()
	        .setClientCredentials(new File("/home/ec2-user/ClientAPNSDeveloperCertificate.p12"), "ABCD3fgh!")
	        .build();
			
	   	    java.text.DateFormat df1 = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss.s");
	   	    java.text.DateFormat df2 = new SimpleDateFormat("EEE, MMM dd");
	   	    
			final Future<Void> connectFuture = apnsClient.connect(ApnsClient.DEVELOPMENT_APNS_HOST);
			connectFuture.await();

			for (ScheduledJob scheduledJob : scheduledJobs) {
	   	    	cStmt.close();
	   	    	cStmt = con.prepareCall("{ CALL getDeviceForUser(?) }");
		   	    cStmt.setInt(1, scheduledJob.clientId);

		   	    cStmt.execute();
		   	    rs = cStmt.getResultSet();
		   	    
		   	    final ApnsPayloadBuilder payloadBuilder = new ApnsPayloadBuilder();
		   	    payloadBuilder.setContentAvailable(true);

	    	    payloadBuilder.setCategoryName("clientnoshow.category");
	    	    if(scheduledJob.pushCounter == 0) {
	    	    	java.util.Date start = df1.parse(scheduledJob.start);
	    	    	payloadBuilder.setAlertTitle(scheduledJob.name);
	    	    	payloadBuilder.setAlertSubtitle("Provider reported 'No Show' for scheduled job");
		        	payloadBuilder.setAlertBody(df2.format(start));
	    	    	payloadBuilder.setSoundFileName("default");
	    	    }
	    	    payloadBuilder.addCustomProperty("address", scheduledJob.address);
	    	    payloadBuilder.addCustomProperty("city", scheduledJob.city);
	    	    payloadBuilder.addCustomProperty("state", scheduledJob.state);
	    	    payloadBuilder.addCustomProperty("start", scheduledJob.start);
	    	    payloadBuilder.addCustomProperty("duration", scheduledJob.duration);
	    	    payloadBuilder.addCustomProperty("providerCompensation", scheduledJob.providerCompensation);
	    	    payloadBuilder.addCustomProperty("fee", scheduledJob.fee);
	    	    payloadBuilder.addCustomProperty("scheduledJobId", scheduledJob.id);
	    	    payloadBuilder.addCustomProperty("userId", scheduledJob.providerId);
	    	    payloadBuilder.addCustomProperty("status", scheduledJob.status);
	    	    payloadBuilder.addCustomProperty("cancelationType", scheduledJob.cancelationType);
	    	    payloadBuilder.addCustomProperty("amount", scheduledJob.amount);

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
	   	    	cStmt = con.prepareCall("{ CALL updateScheduledJobPushCounter(?) }");
		   	    cStmt.setInt(1, scheduledJob.id);
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

	public void sendClientCanceledJobNotifications() throws ClassNotFoundException, SQLException, JsonSyntaxException, Exception {
		ResultSet rs = null;
		Logger LOGGER = Logger.getLogger("InfoLogging");
		try {
			Class.forName("com.mysql.jdbc.Driver");
	    	
	   	    con = DriverManager.getConnection(ServletConstants.dbURL, ServletConstants.dbUsername, ServletConstants.dbPassword);
	   	    CallableStatement cStmt = null;
	
	   	    cStmt = con.prepareCall("{ CALL getClientCanceledJobs() }");
	   	    cStmt.execute();
	   	    rs = cStmt.getResultSet();
	   	    
	   	    ArrayList<ScheduledJob> scheduledJobs = new ArrayList<ScheduledJob>();
	 	
	   	    while(rs.next()) {
	   	    	ScheduledJob scheduledJob = new ScheduledJob(rs);
	   	    	scheduledJobs.add(scheduledJob);
	   	    }
	   	    
	   	    LOGGER.info("sendClientCanceledJobNotifications() scheduledJobs.size(): " + scheduledJobs.size());
	   	    
	   	    if (scheduledJobs.size() == 0) {
	   	    	return;
	   	    }
	   	    
	   	    
	   	 	final ApnsClient apnsClient = new ApnsClientBuilder()
	        .setClientCredentials(new File("/home/ec2-user/ProviderAPNSDeveloperCertificates.p12"), "ABCD3fgh!")
	        .build();
			
	   	    java.text.DateFormat df1 = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss.s");
	   	    java.text.DateFormat df2 = new SimpleDateFormat("EEE, MMM dd");
	   	    
			final Future<Void> connectFuture = apnsClient.connect(ApnsClient.DEVELOPMENT_APNS_HOST);
			connectFuture.await();
	
	   	 	//ArrayList<Device> devicesList = new ArrayList<Device>();
			for (ScheduledJob scheduledJob : scheduledJobs) {
	   	    	cStmt.close();
	   	    	cStmt = con.prepareCall("{ CALL getDeviceForUser(?) }");
		   	    cStmt.setInt(1, scheduledJob.providerId);
		   	    //cStmt.setInt(2, newAcceptOffer.get(1));
	
		   	    cStmt.execute();
		   	    rs = cStmt.getResultSet();
		   	    
		   	    final ApnsPayloadBuilder payloadBuilder = new ApnsPayloadBuilder();
		   	    payloadBuilder.setContentAvailable(true);

	    	    payloadBuilder.setCategoryName("clientcanceled.category");
	    	    if(scheduledJob.pushCounter == 0) {
	    	    	java.util.Date start = df1.parse(scheduledJob.start);
	    	    	payloadBuilder.setAlertTitle(scheduledJob.name);
	    	    	payloadBuilder.setAlertSubtitle("Client canceled scheduled job");
		        	payloadBuilder.setAlertBody(df2.format(start));
	    	    	payloadBuilder.setSoundFileName("default");
	    	    }

	    	    payloadBuilder.addCustomProperty("scheduledJobId", scheduledJob.id);
	    	    payloadBuilder.addCustomProperty("userId", scheduledJob.clientId);
	    	    payloadBuilder.addCustomProperty("status", scheduledJob.status);
	    	    payloadBuilder.addCustomProperty("cancelationType", scheduledJob.cancelationType);
	    	    
	    	    if(!scheduledJob.cancelationType.equals("early")) {
	    	    	payloadBuilder.addCustomProperty("amount", scheduledJob.amount);
	    	    	payloadBuilder.addCustomProperty("reason", scheduledJob.reason);
	    	    }
	    	    
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
	   	    	cStmt = con.prepareCall("{ CALL updateScheduledJobPushCounter(?) }");
		   	    cStmt.setInt(1, scheduledJob.id);
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
	
	public void sendPrvdrCanceledJobNotifications() throws ClassNotFoundException, SQLException, JsonSyntaxException, Exception {
		ResultSet rs = null;
		Logger LOGGER = Logger.getLogger("InfoLogging");
		try {
			Class.forName("com.mysql.jdbc.Driver");
	    	
	   	    con = DriverManager.getConnection(ServletConstants.dbURL, ServletConstants.dbUsername, ServletConstants.dbPassword);
	   	    CallableStatement cStmt = null;
	
	   	    cStmt = con.prepareCall("{ CALL getPrvdrCanceledJobs() }");
	   	    cStmt.execute();
	   	    rs = cStmt.getResultSet();
	   	    
	   	    ArrayList<ScheduledJob> scheduledJobs = new ArrayList<ScheduledJob>();
	 	
	   	    while(rs.next()) {
	   	    	ScheduledJob scheduledJob = new ScheduledJob(rs);
	   	    	scheduledJobs.add(scheduledJob);
	   	    }
	   	    
	   	    LOGGER.info("sendPrvdrCanceledJobNotifications() scheduledJobs.size(): " + scheduledJobs.size());
	   	    
	   	    if (scheduledJobs.size() == 0) {
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
			for (ScheduledJob scheduledJob : scheduledJobs) {
	   	    	cStmt.close();
	   	    	cStmt = con.prepareCall("{ CALL getDeviceForUser(?) }");
		   	    cStmt.setInt(1, scheduledJob.clientId);
		   	    //cStmt.setInt(2, newAcceptOffer.get(1));
	
		   	    cStmt.execute();
		   	    rs = cStmt.getResultSet();
		   	    
		   	    final ApnsPayloadBuilder payloadBuilder = new ApnsPayloadBuilder();
		   	    payloadBuilder.setContentAvailable(true);

	    	    payloadBuilder.setCategoryName("prvdrcanceled.category");
	    	    if(scheduledJob.pushCounter == 0) {
	    	    	java.util.Date start = df1.parse(scheduledJob.start);
	    	    	payloadBuilder.setAlertTitle(scheduledJob.name);
	    	    	payloadBuilder.setAlertSubtitle("Provider canceled scheduled job");
		        	payloadBuilder.setAlertBody(df2.format(start));
	    	    	payloadBuilder.setSoundFileName("default");
	    	    }
	    	    payloadBuilder.addCustomProperty("type", "JobStatus");
	    	    payloadBuilder.addCustomProperty("scheduledJobId", scheduledJob.id);
	    	    payloadBuilder.addCustomProperty("userId", scheduledJob.clientId);
	    	    payloadBuilder.addCustomProperty("status", scheduledJob.status);
	    	    payloadBuilder.addCustomProperty("cancelationType", scheduledJob.cancelationType);
	    	    
	    	    if(!scheduledJob.cancelationType.equals("early")) {
	    	    	payloadBuilder.addCustomProperty("amount", scheduledJob.amount);
	    	    	payloadBuilder.addCustomProperty("reason", scheduledJob.reason);
	    	    }
	    	    
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
	   	    	cStmt = con.prepareCall("{ CALL updateScheduledJobPushCounter(?) }");
		   	    cStmt.setInt(1, scheduledJob.id);
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
	
	public void sendJobStartNotifications() throws ClassNotFoundException, SQLException, JsonSyntaxException, Exception {
		ResultSet rs = null;
		Logger LOGGER = Logger.getLogger("InfoLogging");
		try {
			Class.forName("com.mysql.jdbc.Driver");
	    	
	   	    con = DriverManager.getConnection(ServletConstants.dbURL, ServletConstants.dbUsername, ServletConstants.dbPassword);
	   	    CallableStatement cStmt = null;
	
	   	    cStmt = con.prepareCall("{ CALL getStartedJobs() }");
	   	    cStmt.execute();
	   	    rs = cStmt.getResultSet();
	   	    
	   	    ArrayList<ScheduledJob> scheduledJobs = new ArrayList<ScheduledJob>();
	 	
	   	    while(rs.next()) {
	   	    	ScheduledJob scheduledJob = new ScheduledJob(rs);
	   	    	scheduledJobs.add(scheduledJob);
	   	    }
	   	    
	   	    LOGGER.info("sendJobStartNotifications() scheduledJobs.size(): " + scheduledJobs.size());
	   	    
	   	    if (scheduledJobs.size() == 0) {
	   	    	return;
	   	    }
	   	    
	   	    final ApnsClient apnsClient = new ApnsClientBuilder()
	        .setClientCredentials(new File("/home/ec2-user/ClientAPNSDeveloperCertificate.p12"), "ABCD3fgh!")
	        .build();
			
	   	    java.text.DateFormat df1 = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss.s");
	   	    java.text.DateFormat df2 = new SimpleDateFormat("hh:mm a");
	   	    
			final Future<Void> connectFuture = apnsClient.connect(ApnsClient.DEVELOPMENT_APNS_HOST);
			connectFuture.await();
	
	   	 	//ArrayList<Device> devicesList = new ArrayList<Device>();
			for (ScheduledJob scheduledJob : scheduledJobs) {
	   	    	cStmt.close();
	   	    	cStmt = con.prepareCall("{ CALL getDeviceForUser(?) }");
		   	    cStmt.setInt(1, scheduledJob.clientId);
		   	    //cStmt.setInt(2, newAcceptOffer.get(1));
	
		   	    cStmt.execute();
		   	    rs = cStmt.getResultSet();
		   	    
		   	    final ApnsPayloadBuilder payloadBuilder = new ApnsPayloadBuilder();
		   	    payloadBuilder.setContentAvailable(true);
		   	    payloadBuilder.setCategoryName("jobstarted.category");
	    	    if(scheduledJob.pushCounter == 0) {
		    	    payloadBuilder.setSoundFileName("default");
	    	    	java.util.Date beginTime = df1.parse(scheduledJob.beginTime);
	    	    	payloadBuilder.setAlertTitle(scheduledJob.name);
	    	    	payloadBuilder.setAlertSubtitle(scheduledJob.firstname + " " + scheduledJob.lastname + " started the job");
		        	payloadBuilder.setAlertBody("Start time: " + df2.format(beginTime));
	    	    }

	    	    payloadBuilder.addCustomProperty("scheduledJobId", scheduledJob.id);
	    	    payloadBuilder.addCustomProperty("userId", scheduledJob.clientId);
	    	    payloadBuilder.addCustomProperty("status", scheduledJob.status);
	    	    payloadBuilder.addCustomProperty("cancelationType", scheduledJob.cancelationType);
	    	    payloadBuilder.addCustomProperty("beginTime", scheduledJob.beginTime);
	    	    payloadBuilder.addCustomProperty("endTime", scheduledJob.endTime);
	    	    payloadBuilder.addCustomProperty("pausedTime", scheduledJob.pausedTime);
	    	    payloadBuilder.addCustomProperty("timeRemaining", scheduledJob.timeRemaining);
	    	    
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
	   	    	cStmt = con.prepareCall("{ CALL updateScheduledJobPushCounter(?) }");
		   	    cStmt.setInt(1, scheduledJob.id);
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

	
	public void sendJobCompleteNotifications() throws ClassNotFoundException, SQLException, JsonSyntaxException, Exception {
		ResultSet rs = null;
		Logger LOGGER = Logger.getLogger("InfoLogging");
		try {
			Class.forName("com.mysql.jdbc.Driver");
	    	
	   	    con = DriverManager.getConnection(ServletConstants.dbURL, ServletConstants.dbUsername, ServletConstants.dbPassword);
	   	    CallableStatement cStmt = null;
	
	   	    cStmt = con.prepareCall("{ CALL getCompletedJobs() }");
	   	    cStmt.execute();
	   	    rs = cStmt.getResultSet();
	   	    
	   	    ArrayList<ScheduledJob> scheduledJobs = new ArrayList<ScheduledJob>();
	 	
	   	    while(rs.next()) {
	   	    	ScheduledJob scheduledJob = new ScheduledJob(rs);
	   	    	scheduledJobs.add(scheduledJob);
	   	    }
	   	    
	   	    LOGGER.info("sendJobCompleteNotifications() scheduledJobs.size(): " + scheduledJobs.size());
	   	    
	   	    if (scheduledJobs.size() == 0) {
	   	    	return;
	   	    }
	   	    
	   	    final ApnsClient apnsClient = new ApnsClientBuilder()
	        .setClientCredentials(new File("/home/ec2-user/ClientAPNSDeveloperCertificate.p12"), "ABCD3fgh!")
	        .build();
			
	   	    java.text.DateFormat df1 = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss.s");
	   	    java.text.DateFormat df2 = new SimpleDateFormat("hh:mm a");
	   	    
			final Future<Void> connectFuture = apnsClient.connect(ApnsClient.DEVELOPMENT_APNS_HOST);
			connectFuture.await();
	
	   	 	//ArrayList<Device> devicesList = new ArrayList<Device>();
			for (ScheduledJob scheduledJob : scheduledJobs) {
	   	    	cStmt.close();
	   	    	cStmt = con.prepareCall("{ CALL getDeviceForUser(?) }");
		   	    cStmt.setInt(1, scheduledJob.clientId);
		   	    //cStmt.setInt(2, newAcceptOffer.get(1));
	
		   	    cStmt.execute();
		   	    rs = cStmt.getResultSet();
		   	    
		   	    final ApnsPayloadBuilder payloadBuilder = new ApnsPayloadBuilder();
		   	    payloadBuilder.setContentAvailable(true);
		   	    payloadBuilder.setCategoryName("jobcompleted.category");
	    	    if(scheduledJob.pushCounter == 0) {
		    	    payloadBuilder.setSoundFileName("default");
	    	    	java.util.Date endTime = df1.parse(scheduledJob.endTime);
	    	    	payloadBuilder.setAlertTitle(scheduledJob.name);
	    	    	payloadBuilder.setAlertSubtitle(scheduledJob.firstname + " " + scheduledJob.lastname + " completed the job");
		        	payloadBuilder.setAlertBody("End time: " + df2.format(endTime));
	    	    }
	    	    payloadBuilder.addCustomProperty("scheduledJobId", scheduledJob.id);
	    	    payloadBuilder.addCustomProperty("userId", scheduledJob.clientId);
	    	    payloadBuilder.addCustomProperty("providerId", scheduledJob.providerId);
	    	    payloadBuilder.addCustomProperty("status", scheduledJob.status);
	    	    payloadBuilder.addCustomProperty("cancelationType", scheduledJob.cancelationType);
	    	    payloadBuilder.addCustomProperty("beginTime", scheduledJob.beginTime);
	    	    payloadBuilder.addCustomProperty("endTime", scheduledJob.endTime);
	    	    payloadBuilder.addCustomProperty("pausedTime", scheduledJob.pausedTime);
	    	    payloadBuilder.addCustomProperty("timeRemaining", scheduledJob.timeRemaining);
	    	    payloadBuilder.addCustomProperty("firstname", scheduledJob.firstname);
	    	    payloadBuilder.addCustomProperty("lastname", scheduledJob.lastname);
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
	   	    	cStmt = con.prepareCall("{ CALL updateScheduledJobPushCounter(?) }");
		   	    cStmt.setInt(1, scheduledJob.id);
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

	
	public void sendJobPauseNotifications() throws ClassNotFoundException, SQLException, JsonSyntaxException, Exception {
		ResultSet rs = null;
		Logger LOGGER = Logger.getLogger("InfoLogging");
		try {
			Class.forName("com.mysql.jdbc.Driver");
	    	
	   	    con = DriverManager.getConnection(ServletConstants.dbURL, ServletConstants.dbUsername, ServletConstants.dbPassword);
	   	    CallableStatement cStmt = null;
	
	   	    cStmt = con.prepareCall("{ CALL getPausedJobs() }");
	   	    cStmt.execute();
	   	    rs = cStmt.getResultSet();
	   	    
	   	    ArrayList<ScheduledJob> scheduledJobs = new ArrayList<ScheduledJob>();
	 	
	   	    while(rs.next()) {
	   	    	ScheduledJob scheduledJob = new ScheduledJob(rs);
	   	    	scheduledJobs.add(scheduledJob);
	   	    }
	   	    
	   	    LOGGER.info("sendJobPauseNotifications() scheduledJobs.size(): " + scheduledJobs.size());
	   	    
	   	    if (scheduledJobs.size() == 0) {
	   	    	return;
	   	    }
	   	    
	   	    final ApnsClient apnsClient = new ApnsClientBuilder()
	        .setClientCredentials(new File("/home/ec2-user/ClientAPNSDeveloperCertificate.p12"), "ABCD3fgh!")
	        .build();
			
	   	    //java.text.DateFormat df1 = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss.s");
	   	    //java.text.DateFormat df2 = new SimpleDateFormat("EEE, MMM dd");
	   	    
			final Future<Void> connectFuture = apnsClient.connect(ApnsClient.DEVELOPMENT_APNS_HOST);
			connectFuture.await();
	
	   	 	//ArrayList<Device> devicesList = new ArrayList<Device>();
			for (ScheduledJob scheduledJob : scheduledJobs) {
	   	    	cStmt.close();
	   	    	cStmt = con.prepareCall("{ CALL getDeviceForUser(?) }");
		   	    cStmt.setInt(1, scheduledJob.clientId);
		   	    //cStmt.setInt(2, newAcceptOffer.get(1));
	
		   	    cStmt.execute();
		   	    rs = cStmt.getResultSet();
		   	    
		   	    final ApnsPayloadBuilder payloadBuilder = new ApnsPayloadBuilder();
		   	    payloadBuilder.setContentAvailable(true);
		   	    payloadBuilder.setCategoryName("jobpaused.category");
	    	    if(scheduledJob.pushCounter == 0) {
	    	    	//java.util.Date beginTime = df1.parse(scheduledJob.beginTime);
	    	    	payloadBuilder.setAlertTitle(scheduledJob.name);
	    	    	payloadBuilder.setAlertSubtitle(scheduledJob.firstname + " " + scheduledJob.lastname + " paused the job");
	    	    	Double timeRemaining = scheduledJob.timeRemaining;
	    	    	int totalMinutes = (int) (timeRemaining/60);
	    	    	int hours = totalMinutes/60;
	    	    	int minutes = totalMinutes%60;
		        	payloadBuilder.setAlertBody("Time remaining: " + hours + " hours " + minutes + " minutes");
		    	    payloadBuilder.setSoundFileName("default");  
	    	    }

	    	    payloadBuilder.addCustomProperty("scheduledJobId", scheduledJob.id);
	    	    payloadBuilder.addCustomProperty("userId", scheduledJob.clientId);
	    	    payloadBuilder.addCustomProperty("status", scheduledJob.status);
	    	    payloadBuilder.addCustomProperty("cancelationType", scheduledJob.cancelationType);
	    	    payloadBuilder.addCustomProperty("beginTime", scheduledJob.beginTime);
	    	    payloadBuilder.addCustomProperty("endTime", scheduledJob.endTime);
	    	    payloadBuilder.addCustomProperty("pausedTime", scheduledJob.pausedTime);
	    	    payloadBuilder.addCustomProperty("timeRemaining", scheduledJob.timeRemaining);
	    	    
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
	   	    	cStmt = con.prepareCall("{ CALL updateScheduledJobPushCounter(?) }");
		   	    cStmt.setInt(1, scheduledJob.id);
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

	
	public void sendJobResumeNotifications() throws ClassNotFoundException, SQLException, JsonSyntaxException, Exception {
		ResultSet rs = null;
		Logger LOGGER = Logger.getLogger("InfoLogging");
		try {
			Class.forName("com.mysql.jdbc.Driver");
	    	
	   	    con = DriverManager.getConnection(ServletConstants.dbURL, ServletConstants.dbUsername, ServletConstants.dbPassword);
	   	    CallableStatement cStmt = null;
	
	   	    cStmt = con.prepareCall("{ CALL getResumedJobs() }");
	   	    cStmt.execute();
	   	    rs = cStmt.getResultSet();
	   	    
	   	    ArrayList<ScheduledJob> scheduledJobs = new ArrayList<ScheduledJob>();
	 	
	   	    while(rs.next()) {
	   	    	ScheduledJob scheduledJob = new ScheduledJob(rs);
	   	    	scheduledJobs.add(scheduledJob);
	   	    }
	   	    
	   	    LOGGER.info("sendJobResumeNotifications() scheduledJobs.size(): " + scheduledJobs.size());
	   	    
	   	    if (scheduledJobs.size() == 0) {
	   	    	return;
	   	    }
	   	    
	   	    final ApnsClient apnsClient = new ApnsClientBuilder()
	        .setClientCredentials(new File("/home/ec2-user/ClientAPNSDeveloperCertificate.p12"), "ABCD3fgh!")
	        .build();
			
	   	    java.text.DateFormat df1 = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss.s");
	   	    java.text.DateFormat df2 = new SimpleDateFormat("hh:mm a");
	   	    
			final Future<Void> connectFuture = apnsClient.connect(ApnsClient.DEVELOPMENT_APNS_HOST);
			connectFuture.await();
	
	   	 	//ArrayList<Device> devicesList = new ArrayList<Device>();
			for (ScheduledJob scheduledJob : scheduledJobs) {
	   	    	cStmt.close();
	   	    	cStmt = con.prepareCall("{ CALL getDeviceForUser(?) }");
		   	    cStmt.setInt(1, scheduledJob.clientId);
		   	    //cStmt.setInt(2, newAcceptOffer.get(1));
	
		   	    cStmt.execute();
		   	    rs = cStmt.getResultSet();
		   	    
		   	    final ApnsPayloadBuilder payloadBuilder = new ApnsPayloadBuilder();
		   	    payloadBuilder.setContentAvailable(true);
		   	    payloadBuilder.setCategoryName("jobresumed.category");
	    	    if(scheduledJob.pushCounter == 0) {
	    	    	java.util.Date endTime = df1.parse(scheduledJob.endTime);
	    	    	payloadBuilder.setAlertTitle(scheduledJob.name);
	    	    	payloadBuilder.setAlertSubtitle(scheduledJob.firstname + " " + scheduledJob.lastname + " resumed the job");
	    	    	payloadBuilder.setAlertBody("Estimated completion: " + df2.format(endTime));
		    	    payloadBuilder.setSoundFileName("default");
		    	    
	    	    }

	    	    payloadBuilder.addCustomProperty("scheduledJobId", scheduledJob.id);
	    	    payloadBuilder.addCustomProperty("userId", scheduledJob.clientId);
	    	    payloadBuilder.addCustomProperty("status", scheduledJob.status);
	    	    payloadBuilder.addCustomProperty("cancelationType", scheduledJob.cancelationType);
	    	    payloadBuilder.addCustomProperty("beginTime", scheduledJob.beginTime);
	    	    payloadBuilder.addCustomProperty("endTime", scheduledJob.endTime);
	    	    payloadBuilder.addCustomProperty("pausedTime", scheduledJob.pausedTime);
	    	    payloadBuilder.addCustomProperty("timeRemaining", scheduledJob.timeRemaining);
	    	    
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
	   	    	cStmt = con.prepareCall("{ CALL updateScheduledJobPushCounter(?) }");
		   	    cStmt.setInt(1, scheduledJob.id);
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

	
	
	public void sendPrvdrNoShowNotifications() throws ClassNotFoundException, SQLException, JsonSyntaxException, Exception {
		ResultSet rs = null;
		Logger LOGGER = Logger.getLogger("InfoLogging");
		try {
			Class.forName("com.mysql.jdbc.Driver");
	    	
	   	    con = DriverManager.getConnection(ServletConstants.dbURL, ServletConstants.dbUsername, ServletConstants.dbPassword);
	   	    CallableStatement cStmt = null;
	
	   	    cStmt = con.prepareCall("{ CALL getPrvdrNoShowJobs() }");
	   	    cStmt.execute();
	   	    rs = cStmt.getResultSet();
	   	    
	   	    ArrayList<ScheduledJob> scheduledJobs = new ArrayList<ScheduledJob>();
	 	
	   	    while(rs.next()) {
	   	    	ScheduledJob scheduledJob = new ScheduledJob(rs);
	   	    	scheduledJobs.add(scheduledJob);
	   	    }
	   	    
	   	    if (scheduledJobs.size() == 0) {
	   	    	return;
	   	    }
	   	    
	   	 	final ApnsClient apnsClient = new ApnsClientBuilder()
	        .setClientCredentials(new File("/home/ec2-user/ProviderAPNSDeveloperCertificates.p12"), "ABCD3fgh!")
	        .build();
			
	   	    java.text.DateFormat df1 = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss.s");
	   	    java.text.DateFormat df2 = new SimpleDateFormat("EEE, MMM dd");
	   	    
			final Future<Void> connectFuture = apnsClient.connect(ApnsClient.DEVELOPMENT_APNS_HOST);
			connectFuture.await();

			for (ScheduledJob scheduledJob : scheduledJobs) {
	   	    	cStmt.close();
	   	    	cStmt = con.prepareCall("{ CALL getDeviceForUser(?) }");
		   	    cStmt.setInt(1, scheduledJob.providerId);

		   	    cStmt.execute();
		   	    rs = cStmt.getResultSet();
		   	    
		   	    final ApnsPayloadBuilder payloadBuilder = new ApnsPayloadBuilder();
		   	    payloadBuilder.setContentAvailable(true);

	    	    
	    	    payloadBuilder.setCategoryName("prvdrnoshow.category");
	    	    if(scheduledJob.pushCounter == 0) {
	    	    	java.util.Date start = df1.parse(scheduledJob.start);
	    	    	payloadBuilder.setAlertTitle(scheduledJob.name);
	    	    	payloadBuilder.setAlertSubtitle("Client reported 'No Show' for scheduled job");
		        	payloadBuilder.setAlertBody(df2.format(start));
	    	    	payloadBuilder.setSoundFileName("default");
	    	    }
	    	    payloadBuilder.addCustomProperty("address", scheduledJob.address);
	    	    payloadBuilder.addCustomProperty("city", scheduledJob.city);
	    	    payloadBuilder.addCustomProperty("state", scheduledJob.state);
	    	    payloadBuilder.addCustomProperty("start", scheduledJob.start);
	    	    payloadBuilder.addCustomProperty("duration", scheduledJob.duration);
	    	    payloadBuilder.addCustomProperty("providerCompensation", scheduledJob.providerCompensation);
	    	    payloadBuilder.addCustomProperty("fee", scheduledJob.fee);
	    	    payloadBuilder.addCustomProperty("scheduledJobId", scheduledJob.id);
	    	    payloadBuilder.addCustomProperty("userId", scheduledJob.providerId);
	    	    payloadBuilder.addCustomProperty("status", scheduledJob.status);
	    	    payloadBuilder.addCustomProperty("amount", scheduledJob.amount);

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
	   	    	cStmt = con.prepareCall("{ CALL updateScheduledJobPushCounter(?) }");
		   	    cStmt.setInt(1, scheduledJob.id);
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
