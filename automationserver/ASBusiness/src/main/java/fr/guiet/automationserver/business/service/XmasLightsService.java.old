package fr.guiet.automationserver.business;

import org.apache.log4j.Logger;

public class XmasLightsService {

	// Logger
	private static Logger _logger = Logger.getLogger(XmasLightsService.class);
	
	private static String _topic = "/guiet/automationserver/xmaslights";
	private static String _sensorId = "10";
	private static String _mqttCliendId = "xmasLightsServiceClientId";
	
	public void TurnXmasLightsOn() {
		
		_logger.info("Turning XmasLights ON");
		String message = "SETXMASLIGHTSON;" + _sensorId;
		
		MqttClientMgt rtt = new MqttClientMgt(_mqttCliendId);
		rtt.SendMsg(_topic, message);
	}
	
	public void TurnXmasLightsOff() {
		
		_logger.info("Turning XmasLights OFF");
		String message = "SETXMASLIGHTSOFF;" + _sensorId;
		
		MqttClientMgt rtt = new MqttClientMgt(_mqttCliendId);
		rtt.SendMsg(_topic, message);
	}
	
}
