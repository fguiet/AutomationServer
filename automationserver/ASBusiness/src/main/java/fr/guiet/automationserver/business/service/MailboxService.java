package fr.guiet.automationserver.business.service;

import java.util.ArrayList;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.json.JSONObject;

import fr.guiet.automationserver.dataaccess.DbManager;
import fr.guiet.automationserver.dto.SMSDto;

public class MailboxService implements IMqttable {

	private static Logger _logger = LogManager.getLogger(MailboxService.class);
	private SMSGammuService _smsGammuService = null;
	private static String MQTT_TOPIC = "guiet/mailbox/sensor/6";
	private ArrayList<String> _mqttTopics = new ArrayList<String>();
	private DbManager _dbManager = null;
	
	public MailboxService(SMSGammuService smsGammuService) {
		_smsGammuService = smsGammuService;		
		_mqttTopics.add(MQTT_TOPIC);
		_dbManager = new DbManager();
		
		_logger.info("Starting Mailbox service...");
	}
	
	@Override
	public boolean ProcessMqttMessage(String topic, String message) {
		
		boolean messageProcessed = false;
		
		if (topic.equals(MQTT_TOPIC)) {
			
			JSONObject json = new JSONObject(message);
			String battery = json.getString("battery");
			String externalWakeUp = json.getString("externalwakeup");
			String firmware = json.getString("firmware");
			
			SMSDto sms = new SMSDto("45eab206-21de-41c4-8598-759d1bfe198b");
			String mess = "You got mail ! (Battery voltage : " + battery + "v, Firmware : "+firmware+")";
			
			try {
				float vcc = Float.parseFloat(battery);
				//Save info to InfluxDb
				_dbManager.SaveMailboxSensorInfoInfluxDB(vcc);
			}
			catch (NumberFormatException nfe) {
				_logger.error("Could not convert mailbox battery voltage : "+battery+" into a float", nfe);
			}
			
			sms.setMessage(mess);
			_smsGammuService.sendMessage(sms);
			
			_logger.info("Receveid message from mailbox. Battery voltage : " + battery+ "v, Firmware : "+ firmware +", External wakeup : " + externalWakeUp);
			
			messageProcessed = true;
		}
		
		return messageProcessed;
		
	}

	@Override
	public ArrayList<String> getTopics() {
		return _mqttTopics;
	}
	
}