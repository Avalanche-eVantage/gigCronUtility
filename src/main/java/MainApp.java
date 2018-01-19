import java.sql.SQLException;


import java.text.SimpleDateFormat;

//import com.google.gson.Gson;
import com.google.gson.JsonSyntaxException;

public class MainApp {

	public static void main( String[] args )
    {
		
		APNS apns = new APNS();
        try {

        	do {
        		String timeStampString = apns.getLastRunTimeStamp();
            	System.out.println("timeStampString: " + timeStampString);
            	java.text.DateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
    	   	    
    	   	    java.util.Date date = dateFormat.parse(timeStampString);
    	   	    java.sql.Timestamp sqlTimeStamp = new java.sql.Timestamp(date.getTime());

            	apns.sendServiceRequestPushNotifications(sqlTimeStamp);
            	apns.sendServiceOfferPushNotifications(sqlTimeStamp);
            	apns.sendInvalidatedServiceRequestPushNotifications(sqlTimeStamp);
            	apns.sendScheduledJobPushNotifications(sqlTimeStamp);
            	apns.sendAcceptedOfferStatusPushNotifications();
            	apns.sendConfirmedOfferStatusPushNotifications();
            	apns.sendOfferDeclinedStatusPushNotifications();
            	apns.sendOnRouteOfferStatusPushNotifications();
            	apns.sendClientCanceledJobsPushNotifications();
            	apns.sendPrvdrCanceledJobNotifications();
            	apns.sendArrivedOfferStatusPushNotifications();
            	apns.sendPrvdrAuthStatusPushNotifications();
            	apns.sendClientAuthStatusPushNotifications();
            	apns.sendJobStartNotifications();
            	apns.sendJobPauseNotifications();
            	apns.sendJobResumeNotifications();
            	apns.sendJobCompleteNotifications();
            	//apns.sendCompletedOfferStatusPushNotifications();
            	//apns.sendClientCanceledOfferStatusPushNotifications();
            	//apns.sendPrvdrCanceledOfferStatusPushNotifications();

            	System.out.println("isActive: " + apns.isActive());
            	
        	} while(apns.isActive());    
        	
        } catch (ClassNotFoundException e) {
	    	e.printStackTrace();
			System.out.println("\n'Class.forName' exception: " + e);
        } catch (SQLException e) {
	    	e.printStackTrace();
			System.out.println("\n'SQL' exception: " + e);
        } catch (JsonSyntaxException e) {
	    	e.printStackTrace();
			System.out.println("\n'JSON' exception: " + e);
        } catch (Exception e) {
			e.printStackTrace();
			System.out.println("\n'GET' exception: " + e);
	    }
	}

}
