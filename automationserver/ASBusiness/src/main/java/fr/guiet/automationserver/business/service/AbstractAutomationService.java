package fr.guiet.automationserver.business.service;

import java.util.ArrayList;

import org.apache.log4j.Logger;

abstract class AbstractAutomationService implements Runnable {

	protected static Logger _logger = Logger.getLogger(AbstractAutomationService.class);

	private String _name;

	public AbstractAutomationService(String name) {
		_name = name;

		_logger.info("Starting " + _name + " service...");

	}

	// abstract void stop();
	abstract ArrayList<IMqttable> getMqttableClients();

	/*
	 * public String getName() { return _name; }
	 */

}