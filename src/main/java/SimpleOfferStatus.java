import java.sql.ResultSet;
import java.sql.SQLException;


public class SimpleOfferStatus {
	public int userId;
	public int offerId;
	public String status;
	
	public SimpleOfferStatus(ResultSet rs) throws SQLException {
		this.userId = rs.getInt("userId");
		this.offerId = rs.getInt("offerId");
		this.status = rs.getString("status");
	}

}
