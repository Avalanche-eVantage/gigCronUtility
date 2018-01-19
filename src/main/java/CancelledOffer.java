import java.sql.ResultSet;
import java.sql.SQLException;


public class CancelledOffer {
	public int userId;
	public int offerId;
	public String status;
	public String starttime;
	public String address;
	public String city;
	public String state;
	public String name;
	public int pushCounter;
	public String cancelationType;
	public float amount;
	public String reason;
	
	public CancelledOffer(ResultSet rs) throws SQLException {
		this.userId = rs.getInt("userId");
		this.offerId = rs.getInt("offerId");
		this.status = rs.getString("status");
		this.starttime = rs.getString("starttime");
		this.address = rs.getString("address");
		this.city = rs.getString("city");
		this.state = rs.getString("state");
		this.name = rs.getString("name");
		this.pushCounter = rs.getInt("pushCounter");
		this.cancelationType = rs.getString("cancelationType");
		this.amount = rs.getFloat("amount");
		this.reason = rs.getString("reason");
	}

}
