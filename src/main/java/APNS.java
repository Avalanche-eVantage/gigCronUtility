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
			    	    payloadBuilder.setContentAvailable(true);
			    	    payloadBuilder.setCategoryName("serviceRequest.category");
			    	    payloadBuilder.setAlertTitle("New Service Request");
			    	    payloadBuilder.setAlertBody(job.address + ", " + job.city + ", " + job.state);
			    	    float avgRating = (float)job.ratingSum/(float)job.reviewCount;
			    	    payloadBuilder.setAlertSubtitle(String.format("%1.1f", avgRating) + " avg. stars, " + job.reviewCount + "total reviews");
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
		        
				//Time now = new Time(0);
				Date now = new Date();
		        for (Device device : devicesList) {
		        	//cStmt = con.prepareCall("{ CALL getNewJobIdsForPrvdr(?, ?) }");
			   	    //cStmt.setTimestamp(1, timestamp);
			   	    //cStmt.setInt(2,  device.userId);
			   	    //cStmt.execute();
			   	    //rs = cStmt.getResultSet();
			   	    
			   	    //ArrayList<Integer> jobIds = new ArrayList<Integer>();

			   	    //while(rs.next()) {
			   	    //	jobIds.add(rs.getInt("id"));
			   	    //	LOGGER.info("sendServiceRequestPushNotifications - jobId: " + rs.getInt("id"));
			   	    //}
			   	    
		    	    final ApnsPayloadBuilder payloadBuilder = new ApnsPayloadBuilder();
		    	    //payloadBuilder.setAlertBody(device.sunset.toString());
		    	    //payloadBuilder.setAlertBody("Invalidated Jobs");
		    	    //payloadBuilder.setSoundFileName("default");
		    	    //payloadBuilder.setContentAvailable(true);
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
	   	    
	   	    ArrayList<OfferStatus> newOfferStatuses = new ArrayList<OfferStatus>();

	   	    while(rs.next()) {
	   	    	OfferStatus newOfferStatus = new OfferStatus(rs);
	   	    	newOfferStatuses.add(newOfferStatus);
	   	    	LOGGER.info("newOfferStatus userId: " + newOfferStatus.userId + ", status: " + newOfferStatus.status + ", offerId: " + newOfferStatus.offerId);
	   	    }
	   	    
	   	    final ApnsClient apnsClient = new ApnsClientBuilder()
	        .setClientCredentials(new File("/home/ec2-user/ProviderAPNSDeveloperCertificates.p12"), "ABCD3fgh!")
	        .build();
			
			final Future<Void> connectFuture = apnsClient.connect(ApnsClient.DEVELOPMENT_APNS_HOST);
			connectFuture.await();

	   	 	//ArrayList<Device> devicesList = new ArrayList<Device>();
	   	    for (OfferStatus newOfferStatus : newOfferStatuses) {
	   	    	cStmt = con.prepareCall("{ CALL getDeviceForUser(?) }");
		   	    cStmt.setInt(1, newOfferStatus.userId);
		   	    //cStmt.setInt(2, newAcceptOffer.get(1));

		   	    cStmt.execute();
		   	    rs = cStmt.getResultSet();
		   	    
		   	    final ApnsPayloadBuilder payloadBuilder = new ApnsPayloadBuilder();
	    	    //payloadBuilder.setAlertBody(device.sunset.toString());
	    	    payloadBuilder.setAlertBody("Offer id " + newOfferStatus.offerId + " - " + newOfferStatus.status);
	    	    payloadBuilder.setSoundFileName("default");
	    	    payloadBuilder.addCustomProperty("type", "OfferStatus");
	    	    payloadBuilder.addCustomProperty("id", newOfferStatus.offerId);
	    	    payloadBuilder.addCustomProperty("status", newOfferStatus.status);
	    	    
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
	   	    
	   	    ArrayList<OfferStatus> newOfferStatuses = new ArrayList<OfferStatus>();
	
	   	    while(rs.next()) {
	   	    	OfferStatus newOfferStatus = new OfferStatus(rs);
	   	    	newOfferStatuses.add(newOfferStatus);
	   	    }
	   	    
	   	    final ApnsClient apnsClient = new ApnsClientBuilder()
	        .setClientCredentials(new File("/home/ec2-user/ClientAPNSDeveloperCertificate.p12"), "ABCD3fgh!")
	        .build();
			
			final Future<Void> connectFuture = apnsClient.connect(ApnsClient.DEVELOPMENT_APNS_HOST);
			connectFuture.await();
	
	   	 	//ArrayList<Device> devicesList = new ArrayList<Device>();
	   	    for (OfferStatus newOfferStatus : newOfferStatuses) {
	   	    	cStmt = con.prepareCall("{ CALL getDeviceForUser(?) }");
		   	    cStmt.setInt(1, newOfferStatus.userId);
		   	    //cStmt.setInt(2, newAcceptOffer.get(1));
	
		   	    cStmt.execute();
		   	    rs = cStmt.getResultSet();
		   	    
		   	    final ApnsPayloadBuilder payloadBuilder = new ApnsPayloadBuilder();
	    	    //payloadBuilder.setAlertBody(device.sunset.toString());
	    	    payloadBuilder.setAlertBody("Offer id " + newOfferStatus.offerId + " - " + newOfferStatus.status);
	    	    payloadBuilder.setSoundFileName("default");
	    	    payloadBuilder.addCustomProperty("type", "OfferStatus");
	    	    payloadBuilder.addCustomProperty("id", newOfferStatus.offerId);
	    	    payloadBuilder.addCustomProperty("status", newOfferStatus.status);
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
	    	    //payloadBuilder.setAlertBody(device.sunset.toString());
	    	    payloadBuilder.setAlertBody("Offer id " + completedOffer.offerId + " - " + completedOffer.status);
	    	    payloadBuilder.setSoundFileName("default");
	    	    payloadBuilder.addCustomProperty("type", "OfferStatus");
	    	    payloadBuilder.addCustomProperty("id", completedOffer.offerId);
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
		Logger LOGGER = Logger.getLogger("InfoLogging");
		try {
			Class.forName("com.mysql.jdbc.Driver");
	    	
	   	    con = DriverManager.getConnection(ServletConstants.dbURL, ServletConstants.dbUsername, ServletConstants.dbPassword);
	   	    CallableStatement cStmt = null;
	
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
	    	    payloadBuilder.setAlertBody("Offer id " + newOfferStatus.offerId + " - " + newOfferStatus.status);
	    	    payloadBuilder.setSoundFileName("default");
	    	    payloadBuilder.addCustomProperty("type", "OfferStatus");
	    	    payloadBuilder.addCustomProperty("id", newOfferStatus.offerId);
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

	
	public void sendArrivedOfferStatusPushNotifications() throws ClassNotFoundException, SQLException, JsonSyntaxException, Exception {
		ResultSet rs = null;
		Logger LOGGER = Logger.getLogger("InfoLogging");
		try {
			Class.forName("com.mysql.jdbc.Driver");
	    	
	   	    con = DriverManager.getConnection(ServletConstants.dbURL, ServletConstants.dbUsername, ServletConstants.dbPassword);
	   	    CallableStatement cStmt = null;
	
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
	    	    //payloadBuilder.setAlertBody(device.sunset.toString());
	    	    payloadBuilder.setAlertBody("Offer id " + newArrivedStatus.offerId + " - " + newArrivedStatus.status);
	    	    payloadBuilder.setSoundFileName("default");
	    	    payloadBuilder.addCustomProperty("type", "OfferStatus");
	    	    payloadBuilder.addCustomProperty("id", newArrivedStatus.offerId);
	    	    payloadBuilder.addCustomProperty("status", newArrivedStatus.status);
	    	    payloadBuilder.addCustomProperty("prvdrPassword", newArrivedStatus.prvdrPassword);
	    	    payloadBuilder.addCustomProperty("clientPassword", newArrivedStatus.clientPassword);
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
	    	    //payloadBuilder.setAlertBody(device.sunset.toString());
	    	    payloadBuilder.setAlertBody("Offer id " + authStatus.offerId + " - provider authenticated");
	    	    payloadBuilder.setSoundFileName("default");
	    	    payloadBuilder.addCustomProperty("type", "AuthStatus");
	    	    payloadBuilder.addCustomProperty("id", authStatus.offerId);
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
	    	    //payloadBuilder.setAlertBody(device.sunset.toString());
	    	    payloadBuilder.setAlertBody("Offer id " + authStatus.offerId + " - client authenticated");
	    	    payloadBuilder.setSoundFileName("default");
	    	    payloadBuilder.addCustomProperty("type", "AuthStatus");
	    	    payloadBuilder.addCustomProperty("id", authStatus.offerId);
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
	    	    //payloadBuilder.setAlertBody(device.sunset.toString());
	    	    payloadBuilder.setAlertBody("Offer id " + simpleOfferStatus.offerId + " - declined");
	    	    payloadBuilder.setSoundFileName("default");
	    	    payloadBuilder.addCustomProperty("type", "OfferStatus");
	    	    payloadBuilder.addCustomProperty("id", simpleOfferStatus.offerId);
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
	   	    java.text.DateFormat df2 = new SimpleDateFormat("EEEEE MMMMM dd, yyyy");

	   	    
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
	        	payloadBuilder.setAlertTitle("New Service Offer for " + df2.format(starttime));
	    	    
	    	    int hours = (int)fullOffer.duration; 
	    	    int minutes = (int)(((fullOffer.duration - hours) * 10) * 60);
	    	    String body = "Duration: ";
	    	    if (hours > 0) {
	    	    	if (hours == 1) {
	    	    		body += "1 hour";
	    	    	} else {
	    	    		body += hours + " hours";
	    	    	} 
	    	    }
	    	    if (minutes > 0) {
	    	    	body += minutes + " minutes";
	    	    }
	    	    body += ", Fee: " + String.format("$%3.2f", fullOffer.fee);
	    	    payloadBuilder.setAlertBody(body);
	    	    float avgRating = (float)fullOffer.ratingSum/(float)fullOffer.reviewCount;
	    	    payloadBuilder.setAlertSubtitle("Provider average rating: " + String.format("%1.1f", avgRating) + ", total reviews: " + fullOffer.reviewCount);
	    	    
	    	    payloadBuilder.setSoundFileName("default");
	    	    payloadBuilder.setCategoryName("serviceOffer.category");
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
	   	    java.text.DateFormat df2 = new SimpleDateFormat("EEEEE MMMMM dd, yyyy");
	   	    
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
	    	    payloadBuilder.setAlertBody("The client canceled the " 
	    	    		+ df2.format(starttime) + " job at " 
	    	    		+ offerStatusWithAddress.address + " " 
	    	    		+ offerStatusWithAddress.city + "," 
	    	    		+ offerStatusWithAddress.state);
	    	    payloadBuilder.setSoundFileName("default");
	    	    payloadBuilder.addCustomProperty("type", "OfferStatus");
	    	    payloadBuilder.addCustomProperty("id", offerStatusWithAddress.offerId);
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
	    	    //payloadBuilder.setAlertBody(device.sunset.toString());
	    	    payloadBuilder.setAlertBody("Offer id " + simpleOfferStatus.offerId + " - is canceled");
	    	    payloadBuilder.setSoundFileName("default");
	    	    payloadBuilder.addCustomProperty("type", "OfferStatus");
	    	    payloadBuilder.addCustomProperty("id", simpleOfferStatus.offerId);
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
