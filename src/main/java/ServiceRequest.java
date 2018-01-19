import java.sql.ResultSet;
import java.sql.SQLException;


public class ServiceRequest {
	public int clientId;
	public int providerId;
	public int serviceRequestId;
	public String address;
	public String city;
	public String state;
	public float latitude;
	public float longitude;
	public String name;
	public int ratingSum;
	public int reviewCount;
	

	public ServiceRequest(ResultSet rs) throws SQLException {
		this.clientId = rs.getInt("clientId");
		this.providerId = rs.getInt("providerId");
		this.serviceRequestId = rs.getInt("serviceRequestId");
		this.address = rs.getString("address");
		this.city = rs.getString("city");
		this.state = rs.getString("state");
		this.latitude = rs.getFloat("latitude");
		this.longitude = rs.getFloat("longitude");
		this.name = rs.getString("name");
		this.ratingSum = rs.getInt("ratingSum");
		this.reviewCount = rs.getInt("reviewCount");
	}

}
