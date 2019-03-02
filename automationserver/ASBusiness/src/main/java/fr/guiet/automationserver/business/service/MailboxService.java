package fr.guiet.automationserver.business.service;

import java.util.ArrayList;

import org.apache.log4j.Logger;
import org.json.JSONObject;

import fr.guiet.automationserver.dto.SMSDto;

public class MailboxService implements IMqttable {

	private static Logger _logger = Logger.getLogger(MailboxService.class);
	private SMSGammuService _smsGammuService = null;
	private static String MQTT_TOPIC = "guiet/mailbox/sensor/6";
	private ArrayList<String> _mqttTopics = new ArrayList<String>();
	
	public MailboxService(SMSGammuService smsGammuService) {
		_smsGammuService = smsGammuService;
		
		_mqttTopics.add(MQTT_TOPIC);
		
		_logger.info("Starting Mailbox service...");
	}
	
	@Override
	public boolean ProcessMqttMessage(String topic, String message) {
		
		boolean messageProcessed = false;
		
		if (topic.equals(MQTT_TOPIC)) {
			
			JSONObject json = new JSONObject(message);
			String battery = json.getString("battery");
			String externalWakeUp = json.getString("externalwakeup");
			
			SMSDto sms = new SMSDto("45eab206-21de-41c4-8598-759d1bfe198b");
			sms.setMessage("Receveid message from mailbox. Battery voltage : " + battery+ "v. External wakeup : " + externalWakeUp);
			_smsGammuService.sendMessage(sms, true);
			
			_logger.info("Receveid message from mailbox. Battery voltage : " + battery+ "v. External wakeup : " + externalWakeUp);
			
			messageProcessed = true;
		}
		
		return messageProcessed;
		
	}

	@Override
	public ArrayList<String> getTopics() {
		return _mqttTopics;
	}
	
}