import java.sql.ResultSet;
import java.sql.SQLException;


public class OnRouteOffer {
	public int offerId;
	public int clientId;
	public int providerId;
	public String status;
	public String starttime;
	public float fee;
	public float duration;
	public float latitude;
	public float longitude;
	public float jobLatitude;
	public float jobLongitude;
	
	OnRouteOffer(ResultSet rs) throws SQLException {
		this.offerId = rs.getInt("offerId");
		this.clientId = rs.getInt("clientId");
		this.providerId = rs.getInt("providerId");
		this.status = rs.getString("status");
		this.starttime = rs.getString("starttime");
		this.fee = rs.getFloat("fee");
		this.duration = rs.getFloat("duration");
		this.latitude = rs.getFloat("latitude");
		this.longitude = rs.getFloat("longitude");
		this.jobLatitude = rs.getFloat("jobLatitude");
		this.jobLongitude = rs.getFloat("jobLongitude");
	}

}
