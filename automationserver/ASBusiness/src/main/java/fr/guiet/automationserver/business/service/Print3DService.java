package fr.guiet.automationserver.business.service;

import java.util.ArrayList;
import java.util.Date;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import fr.guiet.automationserver.dto.SMSDto;

public class Print3DService implements IMqttable {
	
	private static Logger _logger = LogManager.getLogger(Print3DService.class);
	private SMSGammuService _smsGammuService = null;
	private static String MQTT_TOPIC_PRINT_STARTED="octoprint/event/PrintStarted";
	private static String MQTT_TOPIC_PRINT_DONE="octoprint/event/PrintDone";
	private static String MQTT_TOPIC_TURN_PRINTER_OFF="cmnd/sonoff-CR10S/power";
	private static String MQTT_MESSAGE_TURN_PRINTER_OFF="off";
	private Date _startTime;
	//private static String MQTT_CLIENT_ID = "print3DServiceCliendId";
	private MqttService _mqttService = null;
	private ArrayList<String> _mqttTopics = new ArrayList<String>();
	
	public Print3DService (SMSGammuService smsGammuService, MqttService mqttService) {

		//_mqttClient = new MqttClientHelper(MQTT_CLIENT_ID);
	 	_smsGammuService = smsGammuService;
	 	_mqttService = mqttService;
	 	
	 	//add topics processed by this service
	 	_mqttTopics.add(MQTT_TOPIC_PRINT_STARTED);
	 	_mqttTopics.add(MQTT_TOPIC_PRINT_DONE);
	 	
     	_logger.info("Starting 3D print service...");
    }
	
	@Override
	public ArrayList<String> getTopics() {
		return _mqttTopics;
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
			
			SMSDto sms = new SMSDto("1ad22dfa-918f-4acc-af84-3187da0352be");
			sms.setMessage("Impression 3D terminée en "+GetPrintDuration()+" minutes");
			_smsGammuService.sendMessage(sms);
			
			_logger.info("Sending message to turn off 3D printer now...Bye bye...");
			
			_mqttService.SendMsg(MQTT_TOPIC_TURN_PRINTER_OFF, MQTT_MESSAGE_TURN_PRINTER_OFF);
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