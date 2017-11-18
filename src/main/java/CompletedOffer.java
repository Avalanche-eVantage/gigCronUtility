import java.sql.ResultSet;
import java.sql.SQLException;


public class CompletedOffer {
	public int userId;
	public int offerId;
	public String status;
	public String beginTime;
	public String endTime;
	public float pausedTime;
	public float fee;
	public int pushCounter;
	public String name;
	
	public CompletedOffer(ResultSet rs) throws SQLException {
		this.userId = rs.getInt("userId");
		this.offerId = rs.getInt("offerId");
		this.status = rs.getString("status");
		this.beginTime = rs.getString("beginTime");
		this.endTime = rs.getString("endTime");
		this.pausedTime = rs.getFloat("pausedTime");
		this.fee = rs.getFloat("fee");
		this.pushCounter = rs.getInt("pushCounter");
		this.name = rs.getString("name");
	}


}
