package fr.guiet.automationserver.business.sensor;

import fr.guiet.automationserver.business.service.SMSGammuService;
import fr.guiet.automationserver.dto.SMSDto;
import fr.guiet.automationserver.dto.SensorDto;

import java.util.Date;
import java.util.HashMap;
import java.util.UUID;

import org.json.JSONObject;

import fr.guiet.automationserver.business.service.IMqttable;

public class ReedswitchSensor extends Sensor implements IMqttable {

	/*
	 * Reedswitch Sensor handles at least one property : state
	 */
	protected ReedswitchState _state = ReedswitchState.VOID;
	
	private Float _batteryVoltage = null;
	private Float _rssi = null;
	private final String OPEN_STATE = "0";
	private final String CLOSE_STATE = "1";

	/*
	 * Constructor
	 */
	public ReedswitchSensor(long id, String name, String mqtt_topic, String influxDbMeasurement,
			SMSGammuService smsGammuService) {
		super(id, name, mqtt_topic, influxDbMeasurement, smsGammuService);
	}
	
	public static ReedswitchSensor LoadFromDto(SensorDto dto, SMSGammuService gammuService) {

		return new ReedswitchSensor(dto.sensorId, dto.name, dto.mqtt_topic, dto.influxDbMeasurement, gammuService);
	}

	public ReedswitchState getState() {
		if (this.isOperational())
			return _state;
		else
			return null;
	}

	@Override
	public boolean ProcessMqttMessage(String topic, String message) {
		
		boolean messageProcessed = false;

		if (topic.equals(_mqttTopics.get(0))) {

			try {
				
				HashMap<String, String> hm = new HashMap<String, String>();

				JSONObject json = new JSONObject(message);
				String rssi = json.getString("rssi");
				String batteryVoltage = json.getString("battery");
				String state = json.getString("state");

				hm.put("rssi", rssi);
				hm.put("battery", batteryVoltage);
				hm.put("state", state);

				// return message process, but do not update sensor value!
				if (!sanityCheck(hm))
					return true;

				_rssi = Float.parseFloat(rssi);
				_batteryVoltage = Float.parseFloat(batteryVoltage);
				
				ReedswitchState updateState = ReedswitchState.VOID;
				
				if (state.equals(CLOSE_STATE))
					updateState = ReedswitchState.CLOSE;
				
				if (state.equals(OPEN_STATE))
					updateState = ReedswitchState.OPEN;
				
				if (updateState != _state) {
					
					String mess = "Sensor (id : " + getId() + ") : " + getName() + ". State changed from : " + _state.name() + " to " + updateState.name() + ". Rssi : " + Float.toString(_rssi) + ". Battery : " + Float.toString(_batteryVoltage);
					
					_logger.info(mess);
					
					//Wanna be alerted each time, state change !
					SMSDto sms = new SMSDto(UUID.randomUUID().toString());					
					sms.setMessage(mess);
					_smsGammuService.sendMessage(sms);
				}
				
				_state = updateState;
				
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
	protected void createSaveToDBTask() {
		// TODO Auto-generated method stub
		
	}

	@Override
	protected boolean sanityCheck(HashMap<String, String> values) {
		
		boolean isOk = true;
		String mess;
		
		String state = values.get("state");
		
		if (!state.equals(CLOSE_STATE) && !state.equals(OPEN_STATE)) {
			mess = "Sanity check failed for sensor : " + getName() + " (id : " + getId() + "), incorrect state : "+state;
			
			_logger.info(mess);
			
			isOk = false;
		}
		
		return isOk;
		
	}

}
