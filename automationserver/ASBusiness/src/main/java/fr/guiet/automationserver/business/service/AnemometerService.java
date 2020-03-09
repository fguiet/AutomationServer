package fr.guiet.automationserver.business.service;

import java.util.ArrayList;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.json.JSONObject;

import fr.guiet.automationserver.dataaccess.DbManager;

public class AnemometerService implements IMqttable {

	private static Logger _logger = LogManager.getLogger(AnemometerService.class);
	//private SMSGammuService _smsGammuService = null;
	private static String MQTT_TOPIC = "guiet/outside/sensor/20";
	private ArrayList<String> _mqttTopics = new ArrayList<String>();
	private DbManager _dbManager = null;
	
	public AnemometerService() {
		//_smsGammuService = smsGammuService;		
		_mqttTopics.add(MQTT_TOPIC);
		_dbManager = new DbManager();
		
		_logger.info("Starting Anemometer service...");
	}
	
	@Override
	public boolean ProcessMqttMessage(String topic, String message) {
		
		boolean messageProcessed = false;
		
		if (topic.equals(MQTT_TOPIC)) {
			
			JSONObject json = new JSONObject(message);
			String battery = json.getString("battery");
			String rpm = json.getString("rpm");
			String vitesse = json.getString("vitesse");
			String firmware = json.getString("firmware");
			
			//SMSDto sms = new SMSDto("45eab206-21de-41c4-8598-759d1bfe198b");
			//String mess = "You got mail ! (Battery voltage : " + battery + "v, Firmware : "+firmware+")";
			
			try {
				float vcc = Float.parseFloat(battery);
				float vitesseFloat = Float.parseFloat(battery);
				int rpmInt = Integer.parseInt(rpm);
				
				//Save info to InfluxDb
				_dbManager.SaveAnemometerSensorInfoInfluxDB(vcc, rpmInt, vitesseFloat);
			}
			catch (NumberFormatException nfe) {
				_logger.error("Could not convert anemometer value, battery : " + battery + ", rpm : " + rpm + ", vitesse: " + vitesse, nfe);
			}
			
			//sms.setMessage(mess);
			//_smsGammuService.sendMessage(sms);
			
			_logger.info("Receveid message from anemometer. Battery voltage : " + battery+ "v, Firmware : "+ firmware +", rpm : " + rpm + ", vitesse : " + vitesse);
			
			messageProcessed = true;
		}
		
		return messageProcessed;
		
	}

	@Override
	public ArrayList<String> getTopics() {
		return _mqttTopics;
	}

	
}