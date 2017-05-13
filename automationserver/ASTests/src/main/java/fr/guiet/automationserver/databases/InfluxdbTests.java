package fr.guiet.automationserver.databases;

import java.util.Calendar;
import java.util.List;

import org.json.JSONArray;

import fr.guiet.automationserver.dataaccess.DbManager;
import fr.guiet.automationserver.dto.TempScheduleDto;

public class InfluxdbTests {
	public static void main(String args[]) {
		
		
		DbManager dbManager = new DbManager();
		
		List<TempScheduleDto> dtoList = dbManager.getTempSchedule();
		
		
		
		//System.out.println(ja);
		

	}
}