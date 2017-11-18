import java.sql.ResultSet;
import java.sql.SQLException;


public class FullOffer {
	public int offerId;
	public int clientId;
	public int providerId;
	public int jobId;
	public int rangeId;
	public String status;
	public String starttime;
	public float fee;
	public float duration;
	public int ratingSum;
	public int reviewCount;
	public int pushCounter;
	public float latitude;
	public float longitude;
	public String address;
	public String city;
	public String state;
	public String name;
	
	public FullOffer(ResultSet rs) throws SQLException {
		this.offerId = rs.getInt("id");
		this.clientId = rs.getInt("clientId");
		this.providerId = rs.getInt("providerId");
		this.jobId = rs.getInt("jobId");
		this.rangeId = rs.getInt("rangeId");
		this.status = rs.getString("status");
		this.starttime = rs.getString("starttime");
		this.fee = rs.getFloat("fee");
		this.duration = rs.getFloat("duration");
		this.ratingSum = rs.getInt("ratingSum");
		this.reviewCount = rs.getInt("reviewCount");
		this.pushCounter = rs.getInt("pushCounter");
		this.latitude = rs.getFloat("latitude");
		this.longitude = rs.getFloat("longitude");
		this.address = rs.getString("address");
		this.city = rs.getString("city");
		this.state = rs.getString("state");
		this.name = rs.getString("name");
	}

}
