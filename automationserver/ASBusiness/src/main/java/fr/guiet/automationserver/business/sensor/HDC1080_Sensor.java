package fr.guiet.automationserver.business.sensor;

import java.math.BigDecimal;
import java.util.Date;
import java.util.HashMap;
import java.util.Random;
import java.util.Timer;
import java.util.TimerTask;

import fr.guiet.automationserver.business.helper.ParseUtils;
import fr.guiet.automationserver.business.service.SMSGammuService;
import fr.guiet.automationserver.dto.SensorDto;

public class HDC1080_Sensor extends EnvironmentalSensor {
	
	//Constructor
	public HDC1080_Sensor(long id, String name, String mqtt_topic, String influxDbMeasurement, SMSGammuService smsGammuService) {
		super(id, name, mqtt_topic, influxDbMeasurement, smsGammuService);
	}
	
	private Float _humidity = null;
	private Float _batteryVoltage = null;
	private Float _rssi = null;
	
	public static HDC1080_Sensor LoadFromDto(SensorDto dto, SMSGammuService gammuService) {

		return new HDC1080_Sensor(dto.sensorId, dto.name, dto.mqtt_topic, dto.influxDbMeasurement, gammuService);
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
				
				HashMap<String, String> hm = new HashMap<String, String>();
				
				hm.put("temperature", messageContent[2]);
				hm.put("humidity", messageContent[3]);
				hm.put("battery", messageContent[4]);
				hm.put("rssi", messageContent[5]);
				
				//return message process, but do not update sensor value!
				if (!sanityCheck(hm)) return true;
				
				//long sensorId = Long.parseLong(messageContent[1]);
				float temperature = Float.parseFloat(messageContent[2]);
				float humidity = Float.parseFloat(messageContent[3]);
				float battery = Float.parseFloat(messageContent[4]);
				float rssi = Float.parseFloat(messageContent[5]);
				
				_temperature = temperature;
				_humidity = humidity;
				_batteryVoltage = battery;
				_rssi = rssi;
				
				//Update last sensor update date
				_lastSensorUpdate = new Date();
				
				messageProcessed = true;
				
			} catch (Exception e) {
				_logger.error("Sensor : " + getName() + " (id : " + getId() + ") - Could not process mqtt message : " + message, e);
			}			
		}
	
		return messageProcessed;	
	}

	@Override
	public boolean sanityCheck(HashMap<String, String> values) {
		
		boolean isOk = true;
		String mess;
		
		String temperature = values.get("temperature");
		//Check whether it is a float or not
		Float retVal = ParseUtils.tryFloatParse(temperature);
		if (retVal != null) {
			BigDecimal a = new BigDecimal(temperature);
			BigDecimal b = new BigDecimal("0.00");
			
		    //a.compareTo(b);
		    //Method returns:
		    //-1 – if a < b)
		    //0 – if a == b
		    //1 – if a > b
			
			//Check temp = 0 or temp < 0
			if (a.compareTo(b) == 0 || a.compareTo(b) == -1) {
			    
				mess = "Sanity check failed for sensor : " + getName() + " (id : " + getId() + "), incorrect temperature : "+temperature;
				
				_logger.info(mess);
				
				/*SMSDto sms = new SMSDto();
				sms.setMessage(mess);
				_smsGammuService.sendMessage(sms, true);*/
				
				isOk = false;
			}
			
			//Check temp > 50
			a = new BigDecimal(temperature);
			b = new BigDecimal("50.00");
			if (a.compareTo(b) == 1) {
				
				mess = "Sanity check failed for sensor : " + getName() + " (id : " + getId() + "), incorrect temperature : "+temperature;
				
				_logger.info(mess);
				
				/*SMSDto sms = new SMSDto();
				sms.setMessage(mess);
				_smsGammuService.sendMessage(sms, true);*/
				
				isOk = false;
			}
		}
		
		String humidity = values.get("humidity");
		//Check whether it is a float or not
		retVal = ParseUtils.tryFloatParse(humidity);
		if (retVal != null) {
			BigDecimal a = new BigDecimal(humidity);
			BigDecimal b = new BigDecimal("0.00");
			
		    //a.compareTo(b);
		    //Method returns:
		    //-1 – if a < b)
		    //0 – if a == b
		    //1 – if a > b
			
			//Check humidity = 0 or humidity < 0
			if (a.compareTo(b) == 0 || a.compareTo(b) == -1) {
				
				mess = "Sanity check failed for sensor : " + getName() + " (id : " + getId() + "), incorrect humidity : "+humidity;
				
				_logger.info(mess);
				
				/*SMSDto sms = new SMSDto();
				sms.setMessage(mess);
				_smsGammuService.sendMessage(sms, true);*/
				
				isOk = false;
			}
			
			//Check humidity > 100
			a = new BigDecimal(temperature);
			b = new BigDecimal("100.00");
			if (a.compareTo(b) == 1) {
				
				mess = "Sanity check failed for sensor : " + getName() + " (id : " + getId() + "), incorrect humidity : "+humidity;
				
				_logger.info(mess);
				
				/*SMSDto sms = new SMSDto();
				sms.setMessage(mess);
				_smsGammuService.sendMessage(sms, true);*/
				
				isOk = false;
			}
		}
		
		String battery = values.get("battery");
		// Check whether it is a float or not
		retVal = ParseUtils.tryFloatParse(battery);
		if (retVal == null) {
			mess = "Sanity check failed for sensor : " + getName() + " (id : " + getId() + "), incorrect battery voltage : "
					+ battery;

			_logger.info(mess);

			isOk = false;
		}
		
		String rssi = values.get("rssi");
		// Check whether it is a float or not
		retVal = ParseUtils.tryFloatParse(rssi);
		if (retVal == null) {
			mess = "Sanity check failed for sensor : " + getName() + " (id : " + getId() + "), incorrect rssi : "
					+ rssi;

			_logger.info(mess);

			isOk = false;
		}
		
		return isOk;
		
	}

	@Override
	protected void createSaveToDBTask() {
		
		_logger.info("Creating save to db sensor info task (Sensor : "+getName()+")");

		TimerTask sensorSavingToDbTask = new TimerTask() {
			@Override
			public void run() {

				try {
					
					if (isOperational()) {
						
						_logger.info("Saving sensor info : " + getName() + " (id : " + getId() + ") to "+getInfluxDbMeasurement()+" InfluxDB measurement"); 
						
						//No timeout detected and correct values sets to sensor in here...
						_dbManager.saveSensorInfoInfluxDB(getInfluxDbMeasurement(), _temperature, _humidity, _batteryVoltage);
						
					}
				} catch (Exception e) {
					_logger.error("Error occured in sensorSavingToDbTask", e);
				}
			}
		};
		
		Random rand = new Random(); 
		int value = rand.nextInt(10000);

		_saveToDbTaskTimer = new Timer(true);
		_saveToDbTaskTimer.schedule(sensorSavingToDbTask, 5000 + value, 60000);

		_logger.info("Save to db sensor info task has been created.");
		
	}

}