import java.sql.ResultSet;
import java.sql.SQLException;


public class Job {
	public int clientId;
	public int providerId;
	public int jobId;
	public String address;
	public String city;
	public String state;
	public float latitude;
	public float longitude;
	public int ratingSum;
	public int reviewCount;

	public Job(ResultSet rs) throws SQLException {
		this.clientId = rs.getInt("clientId");
		this.providerId = rs.getInt("providerId");
		this.jobId = rs.getInt("jobId");
		this.address = rs.getString("address");
		this.city = rs.getString("city");
		this.state = rs.getString("state");
		this.latitude = rs.getFloat("latitude");
		this.longitude = rs.getFloat("longitude");
		this.ratingSum = rs.getInt("ratingSum");
		this.reviewCount = rs.getInt("reviewCount");
	}

}
