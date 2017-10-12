import java.sql.ResultSet;
import java.sql.SQLException;

public class NewOffer {
	public int id;
	public int newOfferCount;
	
	public NewOffer(ResultSet rs) throws SQLException {
		id = rs.getInt("id");
		newOfferCount = rs.getInt("newOfferCount");
	}

}
