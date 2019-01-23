package fr.guiet.automationserver.business.service;

import java.util.ArrayList;

abstract class AbstractService {
	
	abstract void stop();
	abstract ArrayList<IMqttable> getMqttableClients();

}