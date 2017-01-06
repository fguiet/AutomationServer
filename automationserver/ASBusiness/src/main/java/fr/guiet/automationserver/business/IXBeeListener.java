package fr.guiet.automationserver.business;

public interface IXBeeListener {
	void processResponse(String message);

	String sensorAddress();
}