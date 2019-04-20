package fr.guiet.automationserver.business.service;

import java.util.ArrayList;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import fr.guiet.automationserver.business.sensor.Sensor;

public abstract class AbstractAutomationService implements Runnable {

	//Logger
	protected Logger _logger;
	
	//Service name
	private String _name;
	
	private boolean _isThreadCompliant = false;
	
	//Sensors
	private ArrayList<Sensor> _sensorList = new  ArrayList<Sensor>();

	//Constructor
	public AbstractAutomationService(String name, boolean isThreadCompliant, Logger logger) {
		
		_name = name;
		_isThreadCompliant = isThreadCompliant;
		_logger = logger;
		
		_logger.info("Constructing " + _name + " service...");
	}
	
	protected void addSensor(Sensor sensor) {
		_sensorList.add(sensor);
	}
	
	protected ArrayList<Sensor> getSensors() {
		return _sensorList;
	}

	//Mqtt client (ie Sensors manage by this service)
	abstract ArrayList<IMqttable> getMqttableClients();
	
	public boolean IsThreadCompliant() {
		return _isThreadCompliant;
	}
	
	public void run() {
		_logger.info("Starting " + _name + " service...");
	}
	
	//Stop service properly
	public void StopService() {
		
		for(Sensor s : _sensorList) {
			s.stop();
		}
	}

}