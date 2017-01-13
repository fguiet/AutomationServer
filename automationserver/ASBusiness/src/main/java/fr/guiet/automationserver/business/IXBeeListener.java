package fr.guiet.automationserver.business;

/**
 * @author guiet
 *
 */
public interface IXBeeListener {

	void processResponse(String message);

	String sensorAddress();

}