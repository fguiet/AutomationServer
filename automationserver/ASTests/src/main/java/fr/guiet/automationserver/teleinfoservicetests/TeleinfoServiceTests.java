package fr.guiet.automationserver.teleinfoservicetests;

import fr.guiet.automationserver.business.service.MqttService;
import fr.guiet.automationserver.business.service.SMSGammuService;
import fr.guiet.automationserver.business.service.TeleInfoService;

public class TeleinfoServiceTests {
	public static void main(String args[]) {
		
		SMSGammuService gammuService = new SMSGammuService();
		
		MqttService mqttService = new MqttService(gammuService);
		
		TeleInfoService teleInfoService = new TeleInfoService(gammuService, mqttService);
		Thread teleInfoServiceThread = new Thread(teleInfoService);
		teleInfoServiceThread.start();
		
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