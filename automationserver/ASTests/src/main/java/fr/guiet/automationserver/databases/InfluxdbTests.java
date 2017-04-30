package fr.guiet.automationserver.databases;

import java.util.Calendar;

import fr.guiet.automationserver.dataaccess.DbManager;

public class InfluxdbTests {
	public static void main(String args[]) {
		
		
		DbManager dbManager = new DbManager();

		Calendar cal = Calendar.getInstance(); 
		cal.add(Calendar.MONTH, -2);
		java.util.Date dt = cal.getTime();
		
		//dbManager.GetElectriciyConsumption(dt);
		

	}
}