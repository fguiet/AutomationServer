package fr.guiet.automationserver.dto;

/**
 * Sensor DTO
 * 
 * @author guiet
 *
 */
public class SensorDto {

	public long sensorId;
	public String sensorAddress;
	public String name;
	public int firmware_version;
	public String mqtt_topic;
	public String influxDbMeasurement;
}