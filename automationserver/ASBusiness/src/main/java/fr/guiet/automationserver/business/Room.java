package fr.guiet.automationserver.business;

import java.util.List;
import java.util.ArrayList;
import java.util.Calendar;
import org.apache.log4j.Logger;
import fr.guiet.automationserver.dto.*;
import fr.guiet.automationserver.business.service.SMSGammuService;
import fr.guiet.automationserver.business.helper.DateUtils;
import fr.guiet.automationserver.business.sensor.BMP085_Sensor;
import fr.guiet.automationserver.business.sensor.DHT22_Sensor;
import fr.guiet.automationserver.business.sensor.EnvironmentalSensor;
import fr.guiet.automationserver.business.sensor.HDC1080_Sensor;
import fr.guiet.automationserver.business.sensor.ReedswitchSensor;
//import fr.guiet.automationserver.business.sensor.Sensor;
import fr.guiet.automationserver.dataaccess.DbManager;

public class Room {

	private static Logger _logger = Logger.getLogger(Room.class);

	private long _id;

	private String _name;

	private List<EnvironmentalSensor> _envSensorList = new ArrayList<EnvironmentalSensor>();
	
	private List<ReedswitchSensor> _reedswitchSensorList = new ArrayList<ReedswitchSensor>();
	
	private ArrayList<Heater> _heaterList = new ArrayList<Heater>();

	private Float _userWantedTemp = null;
	private Float _lastDefaultTemp = null;
	private String _mqttTopic;

	private DbManager _dbManager = null;
	private SMSGammuService _gammuService = null;

	// Retourne la liste des radiateurs de la piece
	public List<Heater> getHeaterList() {
		return _heaterList;
	}
	
	public List<ReedswitchSensor> getReedswitchSensorList() {
		return _reedswitchSensorList;
	}

	public String getLastEnvSensorUpdate() {
		return getEnvironmentalSensor().getLastSensorUpdate();
	}

	public EnvironmentalSensor getEnvSensor() {
		return getEnvironmentalSensor();
	}

	public long getRoomId() {
		return _id;
	}

	public String getMqttTopic() {
		return _mqttTopic;
	}

	public String getName() {
		return _name;
	}
	
	public boolean isEnvSensorOperational() {
		return getEnvironmentalSensor().isOperational();
	}

	public Float getActualHumidity() {

		if (getEnvironmentalSensor() instanceof HDC1080_Sensor) {
			HDC1080_Sensor hdc1080_sensor = (HDC1080_Sensor) getEnvironmentalSensor();
			return hdc1080_sensor.getHumidity();
		}

		return null;
	}

	public Float getBatteryVoltage() {

		if (getEnvironmentalSensor() instanceof HDC1080_Sensor) {
			HDC1080_Sensor hdc1080_sensor = (HDC1080_Sensor) getEnvironmentalSensor();
			return hdc1080_sensor.getBatteryVoltage();
		}

		return null;
	}

	public Float getRssi() {
		if (getEnvironmentalSensor() instanceof HDC1080_Sensor) {
			HDC1080_Sensor hdc1080_sensor = (HDC1080_Sensor) getEnvironmentalSensor();
			return hdc1080_sensor.getRssi();
		}

		return null;
	}

	public Float getActualTemp() {
		return getEnvironmentalSensor().getTemperature();
	}

	/**
	 * Calcul de l'heure du prochain changement de temperature
	 */
	public String NextChangeDefaultTemp() {

		String result = "NA";

		// No heater...so...no next change default temp
		if (!HasHeater())
			return result;

		try {
			// DbManager dbManager = new DbManager();

			boolean found = false;
			// Date du jour
			Calendar calendar = Calendar.getInstance();
			// Jour de la semaine
			int dayOfWeek = calendar.get(Calendar.DAY_OF_WEEK);
			int dayOfWeekTemp = calendar.get(Calendar.DAY_OF_WEEK);

			// Temporaire
			Calendar calendarHourBegin = Calendar.getInstance();

			while (!found) {

				TempScheduleDto ts = _dbManager.GetNextDefaultTempByRoom(_id, dayOfWeek);

				if (ts != null) {
					found = true;
					calendarHourBegin.setTime(ts.getHourBegin());
					String hour = String.format("%02d:%02d", calendarHourBegin.get(Calendar.HOUR_OF_DAY),
							calendarHourBegin.get(Calendar.MINUTE));
					result = DateUtils.GetDayName(dayOfWeek) + " à " + hour + " ("
							+ String.format("%.2f", ts.getDefaultTempNeeded()) + "°C)";
					break;
				}

				if (dayOfWeek == 7)
					dayOfWeek = 1;
				else
					dayOfWeek++;

				if (dayOfWeek == dayOfWeekTemp) {
					_logger.warn(
							"Impossible de déterminer le prochain changement de température par défaut pour les pièces : "
									+ _name);
					result = "NA à NA (NA °C)";
					found = true;
					break;
				}
			}
		} catch (Exception e) {
			_logger.error("Erreur lors de la récupération du prochain changement de température", e);
			SMSDto sms = new SMSDto("d3678188-8f55-4c6b-9076-bf5f56905ede");
			sms.setMessage("Error occured in room class, review error log for more details");
			_gammuService.sendMessage(sms);
		}

		return result;
	}

	public Float getWantedTemp() {
		return _userWantedTemp; // Peut etre null
	}

	public synchronized void SetWantedTemp(Float wantedTemp) {
		_logger.info("Modification de la température désirée pour la pièce : " + _name + ". Ancienne valeur : "
				+ _userWantedTemp + ". Nouvelle valeur : " + wantedTemp);
		_userWantedTemp = wantedTemp;
	}

	public boolean AtLeastOneHeaterOn() {
		for (Heater h : _heaterList) {
			if (h.isOn())
				return true;
		}

		return false;
	}

	/*
	 * public boolean IsSensorResponding() { return !_sensor.HasTimeoutOccured(); }
	 */

	public boolean IsOffForced() {
		for (Heater h : _heaterList) {
			if (h.IsOffForced())
				return true;
		}

		return false;
	}

	public void ForceExtinctionRadiateurs() {
		for (Heater h : _heaterList) {
			h.ActivateOffForced();
		}
	}

	public void DesactiveExtinctionForceRadiateurs() {
		for (Heater h : _heaterList) {
			h.DesactivateOffForced();
		}
	}

	// Retourne la t° programmée
	public Float GetTempProg() {
		Float currentDefaultTemp = GetDefaultTempByDayAndTime();

		if (currentDefaultTemp == null)
			return null;
		else
			return currentDefaultTemp;
	}

	public void ResetWantedTempToDefault() {

		Float currentDefaultTemp = GetDefaultTempByDayAndTime();
		_userWantedTemp = currentDefaultTemp;
		_lastDefaultTemp = currentDefaultTemp;

	}

	// Calcul de la temperature desiree pour la piece
	public Float ComputeWantedTemp(boolean awayMode, float awayModeTemp) {

		Float currentDefaultTemp = GetDefaultTempByDayAndTime();

		// Probleme lors de la recuperation de la temperature par defaut ou
		// utilisateur
		if (currentDefaultTemp == null || _lastDefaultTemp == null || _userWantedTemp == null) {
			// Reinitialisation des temperatures
			_lastDefaultTemp = currentDefaultTemp;
			_userWantedTemp = currentDefaultTemp;
			return null;
		}

		if (!awayMode) {

			// la temperature par defaut a changer, on reinitialise la
			// temperature
			// voulu par l'utilisateur
			if (currentDefaultTemp.compareTo(_lastDefaultTemp) != 0) {
				_logger.info(currentDefaultTemp + ", " + _lastDefaultTemp
						+ " Réinitialisation de la température désirée par l'utilisateur car il y a un changement de la température par défaut pour la pièce : "
						+ _name);
				_userWantedTemp = currentDefaultTemp;
				_lastDefaultTemp = currentDefaultTemp;
			}

			// si l'utilisateur a modifie la temperature voulue on la retourne
			if (_userWantedTemp.compareTo(currentDefaultTemp) != 0) {
				return _userWantedTemp;
			} else {
				return currentDefaultTemp;
			}
		} else
			return awayModeTemp;
	}

	/*
	 * Heater link to this room declared?
	 */
	private boolean HasHeater() {
		return !_heaterList.isEmpty();
	}

	private Float GetDefaultTempByDayAndTime() {

		Float defaultTemp = null;

		try {

			// Do not try to get default temp for this room if no heater has been defined
			// for this room!
			if (HasHeater()) {
				defaultTemp = _dbManager.GetDefaultTempByRoom(_id);

				if (defaultTemp == null) {
					_logger.warn("Impossible de déterminer la température par défaut pour les pièces : " + _name
							+ ".Le radiateur ne va pas etre controler correctement...");
				}
			}
		} catch (Exception e) {
			_logger.error("Erreur lors de la récupération de la température par défaut", e);
			SMSDto sms = new SMSDto("d72e4b20-7774-45a0-8122-0388c629e811");
			sms.setMessage("Error occured in room class, review error log for more details");
			_gammuService.sendMessage(sms);
		}

		return defaultTemp;
	}

	// Arret du service RoomService
	public void StopService() {

		// Turn heater off when exiting...
		for (Heater h : _heaterList) {
			h.SetOff();
		}

		getEnvironmentalSensor().Stop();
	}

	//At the moment...only one env sensor by room but it will change
	private EnvironmentalSensor getEnvironmentalSensor() {
		
		//TODO : may change in the future
		return _envSensorList.get(0);
	}
	
	private Room(RoomDto dto, SMSGammuService gammuService) {

		_id = dto.id;
		_name = dto.name;
		_mqttTopic = dto.mqttTopic;
		_gammuService = gammuService;

		_dbManager = new DbManager();

		try {

			//TODO : Add sensor type in database
			
			ArrayList<SensorDto> sensorList = _dbManager.getSensorsByRoomId(dto.id);
			
			//SensorDto sensorDto = _dbManager.getSensorById(dto.idSensor);

			for (SensorDto sensorDto : sensorList) {
			
				switch (Long.toString(sensorDto.sensorId)) {
				// Garage
				case "13":
					_envSensorList.add(BMP085_Sensor.LoadFromDto(sensorDto, gammuService));				
					break;
				// Cave / Basement
				case "15":
					_envSensorList.add(DHT22_Sensor.LoadFromDto(sensorDto, gammuService));
					break;
					//Reedswith porte entrée
				case "16":
					_reedswitchSensorList.add(ReedswitchSensor.LoadFromDto(sensorDto, gammuService));
					break;
					
				default:
					_envSensorList.add(HDC1080_Sensor.LoadFromDto(sensorDto, gammuService));
				}
			}

			ArrayList<HeaterDto> heaterDtoList = _dbManager.GetHeatersByRoomId(dto.id);
			for (HeaterDto heaterDto : heaterDtoList) {

				Heater heater = Heater.LoadFromDto(heaterDto, this, gammuService);
				_heaterList.add(heater);
			}

			_lastDefaultTemp = GetDefaultTempByDayAndTime();
			_userWantedTemp = _lastDefaultTemp; // temperature par defaut et
												// temperature utilisateur identique
												// au demarrage
		} catch (Exception e) {
			_logger.error("Erreur dans le constructeur de la class room", e);
			SMSDto sms = new SMSDto("16577983-79d1-4e0f-be20-80f8f731a23a");
			sms.setMessage("Error occured in room class, review error log for more details");
			_gammuService.sendMessage(sms);
		}
	}

	public static Room LoadFromDto(RoomDto dto, SMSGammuService gammuService) {

		return new Room(dto, gammuService);
	}

}
