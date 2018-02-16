import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;


public class ScheduledJob {
	public int id;
	public int clientId;
	public int providerId;
	public int serviceRequestId;
	public int rangeId;
	public int serviceOfferId;
	public int customerId;
	public String start;
	public float duration;
	public String status;
	public float fee;
	public String created;
	public float onroutelat;
	public float onroutelong;
	public String prvdrarrived;
	public int clientPassword;
	public int prvdrPassword;
	public String prvdrauth;
	public String clientauth;
	
	public String endTime;
	public String beginTime;
	public double pausedTime;
	public double timeRemaining;
	public String transactionReason;
	public int pushCounter;
	public String cancelationType;

	public Float latitude;
	public Float longitude;
	public String address;
	public String city;
	public String state;
	public float providerCompensation;
	public String prvdrCharged;
	public String clientCharged;
	public String prvdrReviewed;
	public String clientReviewed;
	public int prvdrActive;
	public int clientActive;

	public String reason;
	public String jobAuthentication;
	public String firstname;
	public String lastname;
	public String imageURL;

	public String name;
	public float amount;
	public int reviewCount;
	public float ratingSum;
	
	public ScheduledJob(ResultSet rs) throws SQLException {
    	id = rs.getInt("id");
    	clientId = rs.getInt("clientId");
    	providerId = rs.getInt("providerId");
    	serviceRequestId = rs.getInt("serviceRequestId");
    	rangeId = rs.getInt("rangeId");
    	serviceOfferId = rs.getInt("serviceOfferId");
    	start = rs.getString("start");
    	duration = rs.getFloat("duration");
    	status = rs.getString("status");
    	fee = rs.getFloat("fee");
    	created = rs.getString("created");
    	onroutelat = rs.getFloat("onroutelat");
    	onroutelong = rs.getFloat("onroutelong");
    	prvdrarrived = rs.getString("prvdrarrived");
    	clientPassword = rs.getInt("clientPassword");
    	prvdrPassword = rs.getInt("prvdrPassword");
    	prvdrauth = rs.getString("prvdrauth");
    	clientauth = rs.getString("clientauth");
    	endTime = rs.getString("endTime");
    	beginTime = rs.getString("beginTime");
    	pausedTime = rs.getDouble("pausedTime");
    	transactionReason = rs.getString("transactionReason");
    	pushCounter = rs.getInt("pushCounter");
    	cancelationType = rs.getString("cancelationType");
	    customerId = rs.getInt("customerId");
	    latitude = rs.getFloat("latitude");
	    longitude = rs.getFloat("longitude");
	    address = rs.getString("address");
	    city = rs.getString("city");
	    state = rs.getString("state");
	    providerCompensation = rs.getFloat("providerCompensation");
	    timeRemaining = rs.getDouble("timeRemaining");
		reviewCount = rs.getInt("reviewCount");
		ratingSum = rs.getFloat("ratingSum");
		prvdrCharged = rs.getString("prvdrCharged");
		clientCharged = rs.getString("clientCharged");
		prvdrReviewed = rs.getString("prvdrReviewed");
		clientReviewed = rs.getString("clientReviewed");
		prvdrActive = rs.getInt("prvdrActive");
		clientActive = rs.getInt("clientActive");
		
    	if(hasColumn(rs, "name")){
    		name = rs.getString("name");
    	}
	    	if(hasColumn(rs, "amount")){
	    		amount = rs.getFloat("amount");
	    	}
	    	if(hasColumn(rs, "reason")){
	    		reason = rs.getString("reason");
	    	}
	    if(hasColumn(rs, "jobAuthentication")){
	    	jobAuthentication = rs.getString("jobAuthentication");
	    }
	    if(hasColumn(rs, "firstname")){
	    	firstname = rs.getString("firstname");
	    }
	    if(hasColumn(rs, "lastname")){
	    	lastname = rs.getString("lastname");
	    }
	    if(hasColumn(rs, "imageURL")){
	    	imageURL = rs.getString("imageURL");
	    }
	}
	
	public static boolean hasColumn(ResultSet rs, String columnName) throws SQLException {
	    ResultSetMetaData rsmd = rs.getMetaData();
	    int columns = rsmd.getColumnCount();
	    for (int x = 1; x <= columns; x++) {
	        if (columnName.equals(rsmd.getColumnName(x))) {
	            return true;
	        }
	    }
	    return false;
	}

}
