package fr.guiet.automationserver.business.sensor;

import java.util.ArrayList;
import java.util.Date;

import fr.guiet.automationserver.business.service.SMSGammuService;
import fr.guiet.automationserver.dto.SensorDto;

public class HDC1080_Sensor extends EnvironmentalSensor {
	
	//Constructor
	public HDC1080_Sensor(long id, String name, String mqtt_topic, SMSGammuService smsGammuService) {
		super(id, name, mqtt_topic, smsGammuService);
	}
	
	private Float _humidity = null;
	private Float _batteryVoltage = null;
	private Float _rssi = null;
	
	/*public void update(float temperature, float humidity, float batteryVoltage, int rssi) {
		
		_temperature = temperature;
		_humidity = humidity;
		_batteryVoltage = batteryVoltage;
		_rssi = rssi;
	}*/
	
	public static HDC1080_Sensor LoadFromDto(SensorDto dto, SMSGammuService gammuService) {

		return new HDC1080_Sensor(dto.sensorId, dto.name, dto.mqtt_topic, gammuService);
	}
	
	public Float getHumidity() {
		
		if (this.isOperational())
			return _humidity;
		else
			return null;
	}
	
	public Float getBatteryVoltage() {
		
		if (this.isOperational())
			return _batteryVoltage;
		else
			return null;
	}
	
	public Float getRssi() {
		
		if (this.isOperational())
			return _rssi;
		else
			return null;
	}

	@Override
	public boolean ProcessMqttMessage(String topic, String message) {
		
		boolean messageProcessed = false;
		
		//TODO : Change to JSON please!
		String[] messageContent = message.split(";");
		
		//At the moment HDC1080 process only one mqtt topic
		if (topic.equals(_mqttTopics.get(0))) {
			
			try {
				//long sensorId = Long.parseLong(messageContent[1]);
				float temperature = Float.parseFloat(messageContent[2]);
				float humidity = Float.parseFloat(messageContent[3]);
				
				//100% per default if power operated
				float battery = 100;
				
				if (messageContent[4] != null) 
					battery = Float.parseFloat(messageContent[4]);
				
				//Rssi 0 per default if power operated
				float rssi = 0;
				
				if (messageContent.length >= 6)  {																
				   rssi = Float.parseFloat(messageContent[5]);
				}
				
				_temperature = temperature;
				_humidity = humidity;
				_batteryVoltage = battery;
				_rssi = rssi;
				
				//Update last sensor update date
				_lastSensorUpdate = new Date();
				
				messageProcessed = true;
				
			} catch (Exception e) {
				_logger.error("Sensor : " + this.getName() + " (id : " + this.getId()+ ") - Could not process mqtt message : " + message, e);
			}			
		}
	
		return messageProcessed;	
	}

	@Override
	public ArrayList<String> getTopics() {
		return _mqttTopics;
	}

}