import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.Map;


public class Range {
	public int id;
	public int serviceRequestId;
	public String date;
	public String minstart;
	public String maxstart;
	public Float minduration;
	public Float maxduration;
	public Float bid;
	public String bidtype;
	public String scheduleType;
	public int useFeeSchedule;
	public int repeatForDays;
	
	public Range(ResultSet rs) throws SQLException {
		serviceRequestId = rs.getInt("serviceRequestId");
    	minduration = rs.getFloat("mindur");
    	maxduration = rs.getFloat("maxdur");
    	bid = rs.getFloat("bid");
    	bidtype = rs.getString("bidtype");
    	useFeeSchedule = rs.getInt("useFeeSchedule");
    	scheduleType = rs.getString("scheduleType");
    	if (scheduleType.equals("Advanced")) {
    		date = rs.getString("date");
    		minstart = rs.getString("minstart");
    		maxstart = rs.getString("maxstart");
    		repeatForDays = rs.getInt("repeatForDays");
    	}
	}
	
	public Map<String, Object> toArray() {
		Map<String, Object> map = new HashMap<String, Object>();
		map.put("serviceRequestId", this.serviceRequestId);
		map.put("date", this.date);
		map.put("minstart", this.minstart);
		map.put("maxstart", this.maxstart);
		map.put("minduration", this.minduration);
		map.put("maxduration", this.maxduration);
		map.put("bid", this.bid);
		map.put("bidtype", this.bidtype);
		map.put("scheduleType", this.scheduleType);
		map.put("useFeeSchedule", this.useFeeSchedule);
		map.put("repeatForDays", this.repeatForDays);
		return map;
	}

}
