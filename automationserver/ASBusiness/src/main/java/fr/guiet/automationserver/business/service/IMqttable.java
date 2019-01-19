package fr.guiet.automationserver.business.service;

import java.util.ArrayList;

public interface IMqttable {
	
	boolean ProcessMqttMessage(String topic, String message);
	ArrayList<String> getTopics();
}