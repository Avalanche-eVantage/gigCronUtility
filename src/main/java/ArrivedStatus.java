import java.sql.ResultSet;
import java.sql.SQLException;


public class ArrivedStatus {
	public int userId;
	public int offerId;
	public String status;
	public int prvdrPassword;
	public int clientPassword;
	public int pushCounter;
	public String jobAuthentication;
	public String firstname;
	public String lastname;
	public String imageURL;
	public String name;
	
	public ArrivedStatus(ResultSet rs) throws SQLException {
		this.userId = rs.getInt("userId");
		this.offerId = rs.getInt("offerId");
		this.status = rs.getString("status");
		this.prvdrPassword = rs.getInt("prvdrPassword");
		this.clientPassword = rs.getInt("clientPassword");
		this.pushCounter = rs.getInt("pushCounter");
		this.jobAuthentication = rs.getString("jobAuthentication");
		this.firstname = rs.getString("firstname");
		this.lastname = rs.getString("lastname");
		this.imageURL = rs.getString("imageURL");
		this.name = rs.getString("name");
	}

}
