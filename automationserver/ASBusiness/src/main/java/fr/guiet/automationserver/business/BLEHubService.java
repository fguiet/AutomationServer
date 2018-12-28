package fr.guiet.automationserver.business;

import java.util.Date;
import org.apache.log4j.Logger;
import fr.guiet.automationserver.dto.SMSDto;

public class BLEHubService implements Runnable {
	
	private static Logger _logger = Logger.getLogger(BLEHubService.class);
	private SMSGammuService _smsGammuService = null;
	
	private static String MQTT_TOPIC_HUB_UPSTAIRS = "/guiet/upstairs/hub";
	private Date _lastAliveUpstairsHubReception;
	private boolean _hasUpstairsHubNotificationSent = false;
	
	private static String MQTT_TOPIC_HUB_DOWNSTAIRS = "/guiet/downstairs/hub";
	private Date _lastAliveDownstairsHubReception;
	private boolean _hasDownstairsHubNotificationSent = false;
	
	private boolean _isStopped = false; // Service arrete?
	
	public BLEHubService (SMSGammuService smsGammuService) {
		
	 	_smsGammuService = smsGammuService;
    }
	
	@Override
	public void run() {
		// TODO Auto-generated method stub
		_logger.info("Starting BLE Hub service...");
		
		//Initialisation
		_lastAliveUpstairsHubReception = new Date();
		_lastAliveDownstairsHubReception = new Date();
     	
     	while (!_isStopped) {

			try {

				//Check upstairs hub alive
				if (GetDuration(_lastAliveUpstairsHubReception) >= 2) {
					
					if (_hasUpstairsHubNotificationSent) {
						_hasUpstairsHubNotificationSent = true;
						
						String message = "Warning! No news from upstairs hub...last alive message reception : " + DateUtils.getDateToString(_lastAliveUpstairsHubReception);
						_logger.info(message);
						
						SMSDto sms = new SMSDto();
						sms.setMessage(message);
						_smsGammuService.sendMessage(sms, true);
					}
				}
				
				//Check downstairs hub alive
				if (GetDuration(_lastAliveDownstairsHubReception) >= 2) {
					
					if (_hasDownstairsHubNotificationSent) {
						_hasDownstairsHubNotificationSent = true;
						
						String message = "Warning! No news from downstairs hub...last alive message reception : " + DateUtils.getDateToString(_lastAliveUpstairsHubReception);
						_logger.info(message);
						
						SMSDto sms = new SMSDto();
						sms.setMessage(message);
						_smsGammuService.sendMessage(sms, true);
					}
				}

				Thread.sleep(10000);

			} catch (Exception e) {
				_logger.error("Error occured in BLE Hub Service...", e);

				SMSDto sms = new SMSDto();
				sms.setMessage("Error occured in BLE Hub Service, review error log for more details : "+ e.getMessage());
				_smsGammuService.sendMessage(sms, true);
			}
     	}
		
	}
	
	public boolean ProcessMqttMessage(String topic, String payload) {
		
		boolean messageProcessed = false;
		
		if (topic.equals(MQTT_TOPIC_HUB_UPSTAIRS)) {
			
			messageProcessed = true;			
			_lastAliveUpstairsHubReception = new Date();
			_hasUpstairsHubNotificationSent = false;
		}
		
		if (topic.equals(MQTT_TOPIC_HUB_DOWNSTAIRS)) {
			
			messageProcessed = true;			
			_lastAliveDownstairsHubReception = new Date();
			_hasDownstairsHubNotificationSent = false;
		}
		
		return messageProcessed;
	}
	
	public void StopService() {

		_logger.info("Stopping BLE Hub Service...");

		_isStopped = true;
	}
	
	private Long GetDuration(Date lastUpdate) {
		
		Date currentDate = new Date();
		long diffMinutes = 0;
		
		if (lastUpdate != null) {
			// occurs at launch of service
			long diff = currentDate.getTime() - lastUpdate.getTime();
			diffMinutes = diff / (60 * 1000);
		}

		return diffMinutes;
	}
}