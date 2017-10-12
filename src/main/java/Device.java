import java.sql.ResultSet;
import java.sql.SQLException;

public class Device {
	public int id;
	public String token;
	public String uid;
	public int userId;
	

	public Device(ResultSet rs) throws SQLException {
		token = rs.getString("token");
		id = rs.getInt("id");
		uid = rs.getString("uid");
		userId = rs.getInt("userId");
	}

}
