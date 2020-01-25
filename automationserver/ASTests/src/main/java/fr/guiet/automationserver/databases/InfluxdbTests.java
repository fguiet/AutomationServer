package fr.guiet.automationserver.databases;

import org.json.JSONObject;

import fr.guiet.automationserver.dataaccess.DbManager;

public class InfluxdbTests {
	public static void main(String args[]) {
		
		
		DbManager dbManager = new DbManager();
		JSONObject json = dbManager.GetWaterMeterInfo();
		
		//List<TempScheduleDto> dtoList = dbManager.getTempSchedule();
		
		
		
		//System.out.println(ja);
		

	}
}