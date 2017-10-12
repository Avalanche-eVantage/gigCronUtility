import java.sql.ResultSet;
import java.sql.SQLException;


public class CompletedOffer {
	public int userId;
	public int offerId;
	public String status;
	public String beginTime;
	public String endTime;
	public Float pausedTime;
	
	public CompletedOffer(ResultSet rs) throws SQLException {
		this.userId = rs.getInt("userId");
		this.offerId = rs.getInt("offerId");
		this.status = rs.getString("status");
		this.beginTime = rs.getString("beginTime");
		this.endTime = rs.getString("endTime");
		this.pausedTime = rs.getFloat("pausedTime");
	}


}
