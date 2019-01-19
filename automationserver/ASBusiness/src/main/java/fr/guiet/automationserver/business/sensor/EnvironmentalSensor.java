package fr.guiet.automationserver.business.sensor;

import fr.guiet.automationserver.business.service.SMSGammuService;
import fr.guiet.automationserver.business.service.IMqttable;

public abstract class EnvironmentalSensor extends Sensor implements IEnvironmentalSensor, IMqttable {
	
	protected Float _temperature = null;
	
	//Constructeur
	public EnvironmentalSensor(long id, String name, String mqtt_topic, SMSGammuService smsGammuService) {
		super(id, name, mqtt_topic, smsGammuService);
	}

	public Float getTemperature() {
		
		if (this.isOperational())
			return _temperature;
		else
			return null;
	}
	
	public long getId() {
		return super.getId();
	}
}
