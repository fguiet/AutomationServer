package fr.guiet.automationserver.business.service;

import java.util.ArrayList;

import org.apache.log4j.Logger;

import fr.guiet.automationserver.business.sensor.BMP085_Sensor;
import fr.guiet.automationserver.business.sensor.DS18B20_Sensor;
import fr.guiet.automationserver.business.sensor.EnvironmentalSensor;
import fr.guiet.automationserver.dataaccess.DbManager;
import fr.guiet.automationserver.dto.SensorDto;

public class OutsideEnvironmentalService extends AbstractService {

	private static Logger _logger = Logger.getLogger(OutsideEnvironmentalService.class);

	private EnvironmentalSensor _bmp085_garage_sensor;
	private EnvironmentalSensor _ds18b20_outside_sensor;
	private DbManager _dbManager = null;
	//private SMSGammuService _gammuService = null;
	private final long BMP085_GARAGE_SENSOR_ID = 13;
	private final long DS18B20_GARAGE_SENSOR_ID = 14;

	public ArrayList<IMqttable> getMqttableClients() {

		ArrayList<IMqttable> m = new ArrayList<IMqttable>();
		m.add(_bmp085_garage_sensor);
		m.add(_ds18b20_outside_sensor);

		return m;
	}

	public OutsideEnvironmentalService(SMSGammuService gammuService) {

		_dbManager = new DbManager();

		SensorDto sensorDto = _dbManager.getSensorById(BMP085_GARAGE_SENSOR_ID);

		_bmp085_garage_sensor = BMP085_Sensor.LoadFromDto(sensorDto, gammuService);

		sensorDto = _dbManager.getSensorById(DS18B20_GARAGE_SENSOR_ID);

		_ds18b20_outside_sensor = DS18B20_Sensor.LoadFromDto(sensorDto, gammuService);
		
     	_logger.info("Starting Outside Environmental service...");
	}

	@Override
	void stop() {
		// TODO Auto-generated method stub
		
	}

}