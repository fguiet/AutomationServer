package fr.guiet.automationserver.business.sensor;

import fr.guiet.automationserver.business.service.SMSGammuService;

import fr.guiet.automationserver.business.service.IMqttable;

public abstract class EnvironmentalSensor extends Sensor implements IMqttable {

	/*
	 * Environmental Sensor handles at least one property : temperature
	 */
	protected Float _temperature = null;

	/*
	 * Constructor
	 */
	public EnvironmentalSensor(long id, String name, String mqtt_topic, String influxDbMeasurement,
			SMSGammuService smsGammuService) {
		super(id, name, mqtt_topic, influxDbMeasurement, smsGammuService);
	}

	public Float getTemperature() {
		if (this.isOperational())
			return _temperature;
		else
			return null;
	}

}
