import java.sql.ResultSet;
import java.sql.SQLException;


public class ArrivedStatus {
	public int userId;
	public int offerId;
	public String status;
	public int prvdrPassword;
	public int clientPassword;
	
	public ArrivedStatus(ResultSet rs) throws SQLException {
		this.userId = rs.getInt("userId");
		this.offerId = rs.getInt("offerId");
		this.status = rs.getString("status");
		this.prvdrPassword = rs.getInt("prvdrPassword");
		this.clientPassword = rs.getInt("clientPassword");
	}

}
