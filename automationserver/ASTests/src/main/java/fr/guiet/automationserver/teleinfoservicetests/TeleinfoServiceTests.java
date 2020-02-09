package fr.guiet.automationserver.teleinfoservicetests;

import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;

import fr.guiet.automationserver.business.helper.DateUtils;
import fr.guiet.automationserver.business.service.MqttService;
import fr.guiet.automationserver.business.service.SMSGammuService;
import fr.guiet.automationserver.business.service.TeleInfoService;
import fr.guiet.automationserver.dataaccess.DbManager;

public class TeleinfoServiceTests {
	public static void main(String args[]) {
		
		SMSGammuService gammuService = new SMSGammuService();
		
		MqttService mqttService = new MqttService(gammuService);
		
		TeleInfoService teleInfoService = new TeleInfoService(gammuService, mqttService);
		//Thread teleInfoServiceThread = new Thread(teleInfoService);
		//teleInfoServiceThread.start();
		
		Date fromDate = DateUtils.parseDate("2020-02-01");
		Date toDate = DateUtils.parseDate("2020-02-28");
		
		//HashMap<String, Float> test = teleInfoService.GetElectricityBillInfo(fromDate, toDate);
		float test = teleInfoService.getElectricityCostPerCurrentDay();
		
		
			
		/*DbManager dbManager = new DbManager();
		HashMap<String, Integer> map = null;
		try {
			map = dbManager.GetElectriciyConsumption(fromDate, toDate);
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

		Integer hcConso = map.get("hcConsuption");
		Integer hpConso = map.get("hpConsuption");*/
		
		//Test d'une minute
		
		/*try {
			Thread.sleep(60000);
		} catch (InterruptedException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}*/
		
		//Date date = DateUtils.getDateWithoutTime(new Date());
		//Date date = teleInfoService.getLastBillDate();
		//Date dateTo = DateUtils.addDays(date,6);
		
		//System.out.println(teleInfoService.getElectricityCostPerCurrentMonth());
		
	}
}