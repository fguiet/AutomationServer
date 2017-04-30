package fr.guiet.automationserver.teleinfoservicetests;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;

import fr.guiet.automationserver.business.DateUtils;
import fr.guiet.automationserver.business.SMSGammuService;
import fr.guiet.automationserver.business.TeleInfoService;

public class TeleinfoServiceTests {
	public static void main(String args[]) {
		
		SMSGammuService gammuService = new SMSGammuService();
		
		TeleInfoService teleInfoService = new TeleInfoService(gammuService);
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
		Date date = teleInfoService.getLastBillDate();
		Date dateTo = DateUtils.addDays(date,6);
		
		System.out.println(teleInfoService.GetNextElectricityBillCost());
		
	}
}