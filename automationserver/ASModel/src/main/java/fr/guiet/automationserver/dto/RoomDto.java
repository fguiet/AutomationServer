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
	
	//Sensor linked to the room
	public long idSensor;
	public String mqttTopic;
	//public String influxdbMeasurement;
}