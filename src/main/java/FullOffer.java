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
	}

}
