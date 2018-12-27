package fr.guiet.automationserver.business;

import java.util.Date;

import org.apache.log4j.Logger;

import fr.guiet.automationserver.dto.SMSDto;

public class Print3DService {
	
	private static Logger _logger = Logger.getLogger(Print3DService.class);
	private SMSGammuService _smsGammuService = null;
	private static String MQTT_TOPIC_PRINT_STARTED="octoprint/event/PrintStarted";
	private static String MQTT_TOPIC_PRINT_DONE="octoprint/event/PrintDone";
	private static String MQTT_TOPIC_TURN_PRINTER_OFF="cmnd/sonoff-CR10S/power";
	private static String MQTT_MESSAGE_TURN_PRINTER_OFF="off";
	private Date _startTime;
	private static String MQTT_CLIENT_ID = "print3DServiceCliendId";
	private MqttClientMgt _mqttClient = null;
	
	public Print3DService (SMSGammuService smsGammuService) {

		_mqttClient = new MqttClientMgt(MQTT_CLIENT_ID);
	 	_smsGammuService = smsGammuService;
     	_logger.info("Starting 3D print service...");
    }
	
	public boolean ProcessMqttMessage(String topic, String payload) {
		
		boolean messageProcessed = false;
		
		if (topic.equals(MQTT_TOPIC_PRINT_STARTED)) {
			
			messageProcessed = true;
			_logger.info("3D Print has started...");
			
			_startTime = new Date();
		}
		
		if (topic.equals(MQTT_TOPIC_PRINT_DONE)) {
			messageProcessed = true;
			_logger.info("3D Print done !...yeah !...Print duration in minutes : "+GetPrintDuration());
			
			SMSDto sms = new SMSDto();
			sms.setMessage("Impression 3D terminée en "+GetPrintDuration()+" minutes");
			_smsGammuService.sendMessage(sms, true);
			
			_logger.info("Sending message to turn off 3D printer now...Bye bye...");
			
			_mqttClient.SendMsg(MQTT_TOPIC_TURN_PRINTER_OFF, MQTT_MESSAGE_TURN_PRINTER_OFF);
		}
		
		return messageProcessed;
	}
	
	private Long GetPrintDuration() {
		
		Date currentDate = new Date();
		long diffMinutes = 0;
		
		if (_startTime != null) {
			// occurs at launch of service
			long diff = currentDate.getTime() - _startTime.getTime();
			diffMinutes = diff / (60 * 1000);
		}

		return diffMinutes;
	}
}