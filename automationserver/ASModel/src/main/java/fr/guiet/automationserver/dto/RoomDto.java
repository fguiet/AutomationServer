package fr.guiet.automationserver.dto;

/**
 * Room DTO
 * 
 * @author guiet
 *
 */
public class RoomDto {
	public long id;
	public String name;
	public long idSensor;
	public String mqttTopic;
	public String influxdbMeasurement;
}