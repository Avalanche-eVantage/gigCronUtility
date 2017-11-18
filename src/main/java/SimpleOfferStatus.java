import java.sql.ResultSet;
import java.sql.SQLException;


public class SimpleOfferStatus {
	public int userId;
	public int offerId;
	public String starttime;
	public String status;
	public String name;
	
	public SimpleOfferStatus(ResultSet rs) throws SQLException {
		this.userId = rs.getInt("userId");
		this.offerId = rs.getInt("offerId");
		this.starttime = rs.getString("starttime");
		this.status = rs.getString("status");
		this.name = rs.getString("name");
	}

}
