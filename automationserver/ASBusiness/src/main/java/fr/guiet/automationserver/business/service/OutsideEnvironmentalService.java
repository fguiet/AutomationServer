package fr.guiet.automationserver.business.service;

import java.util.ArrayList;

import org.apache.log4j.Logger;

import fr.guiet.automationserver.business.sensor.DS18B20_Sensor;
import fr.guiet.automationserver.business.sensor.EnvironmentalSensor;
import fr.guiet.automationserver.business.sensor.Sensor;
import fr.guiet.automationserver.dataaccess.DbManager;
import fr.guiet.automationserver.dto.SensorDto;

public class OutsideEnvironmentalService extends AbstractAutomationService {

	//private EnvironmentalSensor _ds18b20_outside_sensor;
	private DbManager _dbManager = null;
	private final long DS18B20_GARAGE_SENSOR_ID = 14;

	public ArrayList<IMqttable> getMqttableClients() {

		ArrayList<IMqttable> m = new ArrayList<IMqttable>();
		
		for(Sensor s : getSensors()) {
			if (s instanceof IMqttable) {
				_logger.info("Add IMqttable sensor");
				m.add((IMqttable) s);
			}
		}
		
		return m;
	}

	public OutsideEnvironmentalService(String name, SMSGammuService gammuService) {

		super(name, false, Logger.getLogger(OutsideEnvironmentalService.class));
		
		_dbManager = new DbManager();
		SensorDto sensorDto = _dbManager.getSensorById(DS18B20_GARAGE_SENSOR_ID);
		EnvironmentalSensor _ds18b20_outside_sensor = DS18B20_Sensor.LoadFromDto(sensorDto, gammuService);
		
		addSensor(_ds18b20_outside_sensor);
	}

	@Override
	public void run() {
		
		super.run();
		
		// TODO Auto-generated method stub
	}

}