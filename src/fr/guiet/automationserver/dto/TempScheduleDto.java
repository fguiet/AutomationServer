package fr.guiet.automationserver.dto;

import java.util.Date;

public class TempScheduleDto
{
	private Date _hourBegin;
	private Date _hourEnd;
	private float _defaultTempNeeded;
	private int _dayOfWeek;
	
	public TempScheduleDto(Date hourBegin, Date hourEnd, float defaultTempNeeded, int dayOfWeek)  {
		_hourBegin = hourBegin;
		_hourEnd = hourEnd;
		_defaultTempNeeded = defaultTempNeeded;
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
	
	public int getDayOfWeek() {
		return _dayOfWeek;
	}
}