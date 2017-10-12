import java.sql.ResultSet;
import java.sql.SQLException;


public class AuthStatus {
	public int userId;
	public int offerId;
	
	public AuthStatus(ResultSet rs) throws SQLException {
		this.userId = rs.getInt("userId");
		this.offerId = rs.getInt("offerId");
	}

}
