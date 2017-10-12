import java.sql.ResultSet;
import java.sql.SQLException;


public class OfferStatus {
	public int userId;
	public int offerId;
	public String status;
	public float latitude;
	public float longitude;
	
	public OfferStatus(ResultSet rs) throws SQLException {
		this.userId = rs.getInt("userId");
		this.offerId = rs.getInt("offerId");
		this.status = rs.getString("status");
		this.latitude = rs.getFloat("latitude");
		this.longitude = rs.getFloat("longitude");
	}

}
