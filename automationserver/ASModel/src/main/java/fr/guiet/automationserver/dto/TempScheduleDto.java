package fr.guiet.automationserver.dto;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;

import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * TempSchedule DTO
 * 
 * @author guiet
 *
 */
//@XmlRootElement(name = "TempSchedule")
//@XmlAccessorType(XmlAccessType.FIELD)
public class TempScheduleDto {

	private Date _hourBegin;	
	private Date _hourEnd;
	private float _defaultTempNeeded;
	private int _dayOfWeek;
	private long _id;
	private int _roomId;
	
	public TempScheduleDto()
	{
		
	}
	
	public static String getDateISOFormat(Date date) {
		DateFormat df = new SimpleDateFormat("yyyy/MM/dd HH:mm:ss");
		return df.format(date);
	}
	
	public static String getHourString(Date date) {
		DateFormat df = new SimpleDateFormat("HH:mm:ss");
		return df.format(date);
	}

	public TempScheduleDto(long id, int roomId, Date hourBegin, Date hourEnd, float defaultTempNeeded, int dayOfWeek) {
		_hourBegin = hourBegin;
		_hourEnd = hourEnd;
		_defaultTempNeeded = defaultTempNeeded;
		_dayOfWeek = dayOfWeek;
		_id = id;
		_roomId = roomId;
	}
		
	@JsonProperty("start")
	public void setHourBegin(Date hourBegin) {
		_hourBegin = hourBegin;
	}
	
	@JsonProperty("end")
	public void setHourEnd(Date hourEnd) {
		_hourEnd = hourEnd;
	}
	
	public void setTemp(float temp) {
		_defaultTempNeeded = temp;
	}
	
	@JsonProperty("text")
	public void setTemp(String temp) {
		_defaultTempNeeded = Float.parseFloat(temp);
	}
	
	@JsonProperty("dayofweek")
	public void setDayOfWeek(int dayOfWeek) {
		_dayOfWeek = dayOfWeek;
	}	
	
	public Date getHourBegin() {
		return _hourBegin;
	}

	public Date getHourEnd() {
		return _hourEnd;
	}

	public float getDefaultTempNeeded() {
		return _defaultTempNeeded;
	}

	@JsonProperty("id")
	public long getId() {
		return _id;
	}
	
	@JsonProperty("id")
	public void setId(long id) {
		_id = id;
	}
	
	@JsonProperty("resource")
	public int getRoomId() {
		return _roomId;
	}
	
	@JsonProperty("resource")
	public void setRoomId(int roomId) {
		_roomId = roomId;
	}
		
	@JsonProperty("dayofweek")
	public int getDayOfWeek() {
		return _dayOfWeek;
	}
	
	@JsonProperty("start")
	public String getISODateStart() {
		return getDateISOFormat(_hourBegin);
	}
	
	@JsonProperty("end")
	public String getISODateEnd() {
		return getDateISOFormat(_hourEnd);
	}
	
	@JsonProperty("text")
	public String getTempInString() {
		return String.valueOf(_defaultTempNeeded);
	}
	
	public String getHourBeginString() {
		return getHourString(_hourBegin);
	}
	
	public String getHourEndString() {
		return getHourString(_hourEnd);
	}
}