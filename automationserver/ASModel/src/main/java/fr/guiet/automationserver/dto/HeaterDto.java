package fr.guiet.automationserver.dto;

/**
 * Heater DTO 
 * Represents information about a heater
 * 
 * @author guiet
 *
 */
public class HeaterDto {

	public long heaterId;
	public int currentConsumption;
	public int phase;
	public int raspberryPin;
	public String name;
	//TODO : refactor dto (suppress direct property access)
}