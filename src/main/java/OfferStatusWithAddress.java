import java.sql.ResultSet;
import java.sql.SQLException;


public class OfferStatusWithAddress {
	public int userId;
	public int offerId;
	public String status;
	public String starttime;
	public String address;
	public String city;
	public String state;
	

	public OfferStatusWithAddress(ResultSet rs) throws SQLException {
		this.userId = rs.getInt("userId");
		this.offerId = rs.getInt("offerId");
		this.status = rs.getString("status");
		this.starttime = rs.getString("starttime");
		this.address = rs.getString("address");
		this.city = rs.getString("city");
		this.state = rs.getString("state");
	}

}
