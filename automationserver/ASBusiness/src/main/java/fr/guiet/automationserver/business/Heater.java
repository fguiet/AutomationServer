package fr.guiet.automationserver.business;

import com.pi4j.io.gpio.Pin;
import org.apache.log4j.Logger;
import com.pi4j.io.gpio.RaspiPin;
import fr.guiet.automationserver.dto.*;
import fr.guiet.automationserver.dataaccess.DbManager;

/**
 * Handles heater management
 * 
 * @author guiet
 * 
 *         TODO : Creer une classe GpioHelper
 *
 */
public class Heater implements Comparable<Heater> {

	private long _heaterId;
	private int _currentConsumption;
	private int _phase;
	private int _raspberryPin;
	private String _name;
	private boolean _isOn = false;
	private Room _room = null;
	private Pin _pin;
	private boolean _isOffForced = false;
	// private TeleInfoService _teleInfoService = null;
	private static Logger _logger = Logger.getLogger(Heater.class);
	// private boolean _waitForOn = false;
	// private boolean _waitForOff = false;
	//private final String PIN_CHAUFFAGE_NAME = "PIN_CHAUFFAGE";
	private DbManager _dbManager = null;
	private SMSGammuService _smsGammuService = null;

	/**
	 * @return Returns Heater name (value stored in PostgreSQL database)
	 */
	public String getName() {
		return _name;
	}

	/**
	 * @return Returns room object associated with this heater
	 */
	public Room getRoom() {
		return _room;
	}

	/**
	 * @return Returns electrical phase associated with this heater
	 */
	public int getPhase() {
		return _phase;
	}

	/**
	 * @return Returns whether heater is on or off
	 */
	public boolean isOn() {
		return _isOn;
	}

	/**
	 * @return Returns heater max. current consumption
	 */
	public int getCurrentConsumption() {
		return _currentConsumption;
	}

	/**
	 * @return Returns whether heater is automatically regulated or if user
	 *         decided to stop it !
	 */
	public boolean IsOffForced() {
		return _isOffForced;
	}

	/**
	 * Create a new heater
	 * 
	 * @param dto
	 * @param room
	 */
	private Heater(HeaterDto dto, Room room, SMSGammuService smsGammuService) {

		_room = room;
		_heaterId = dto.heaterId;
		_currentConsumption = dto.currentConsumption;
		_phase = dto.phase;
		_raspberryPin = dto.raspberryPin;
		_name = dto.name;
		_dbManager = new DbManager();
		// _teleInfoService = teleInfoService;
		// _teleInfoService.addListener(this);
		_smsGammuService = smsGammuService;

		switch (_raspberryPin) {
		case 1:
			_pin = RaspiPin.GPIO_01;
			break;
		case 2:
			_pin = RaspiPin.GPIO_02;
			break;
		case 3:
			_pin = RaspiPin.GPIO_03;
			break;
		case 4:
			_pin = RaspiPin.GPIO_04;
			break;
		case 5:
			_pin = RaspiPin.GPIO_05;
			break;
		case 6:
			_pin = RaspiPin.GPIO_06;
			break;
		case 7:
			_pin = RaspiPin.GPIO_07;
			break;
		case 8:
			_pin = RaspiPin.GPIO_08;
			break;
		case 9:
			_pin = RaspiPin.GPIO_09;
			break;
		case 10:
			_pin = RaspiPin.GPIO_10;
			break;
		case 11:
			_pin = RaspiPin.GPIO_11;
			break;
		case 12:
			_pin = RaspiPin.GPIO_12;
			break;
		case 13:
			_pin = RaspiPin.GPIO_13;
			break;
		case 14:
			_pin = RaspiPin.GPIO_14;
			break;
		case 15:
			_pin = RaspiPin.GPIO_15;
			break;
		case 16:
			_pin = RaspiPin.GPIO_16;
			break;
		case 17:
			_pin = RaspiPin.GPIO_17;
			break;
		case 18:
			_pin = RaspiPin.GPIO_18;
			break;
		case 19:
			_pin = RaspiPin.GPIO_19;
			break;
		case 20:
			_pin = RaspiPin.GPIO_20;
			break;
		case 21:
			_pin = RaspiPin.GPIO_21;
			break;
		case 22:
			_pin = RaspiPin.GPIO_22;
			break;
		case 23:
			_pin = RaspiPin.GPIO_23;
			break;
		case 24:
			_pin = RaspiPin.GPIO_24;
			break;
		case 25:
			_pin = RaspiPin.GPIO_25;
			break;
		case 26:
			_pin = RaspiPin.GPIO_26;
			break;
		case 27:
			_pin = RaspiPin.GPIO_27;
			break;
		case 28:
			_pin = RaspiPin.GPIO_28;
			break;
		case 29:
			_pin = RaspiPin.GPIO_29;
			break;
		/*
		 * case 30: _pin = RaspiPin.GPIO_30; break; case 31: _pin =
		 * RaspiPin.GPIO_31; break;
		 */

		default:
			// TODO : utiliser un throw ici!
			_logger.error(String.format("Pin %d not managed !", _raspberryPin));
			break;
		}
		
		GpioHelper.provisionGpioPin(_pin,
				 "Provisioning Pin address : "+_pin.getAddress()+" for heater : " + _name, com.pi4j.io.gpio.PinState.HIGH, _smsGammuService);

		// Set Heater off by default
		SetOff();
	}

	/**
	 * @return Returns French heater state (ON or OFF)
	 */
	public String getEtatLabel() {

		// TODO : Rename this method getFRStateLabel()
		if (_isOn)
			return "ALLUME";
		else
			return "ETEINT";
	}

	// Retourne la priorite du radiateur en fonction du jour de la semaine et de
	// l'heure

	/**
	 * @return Returns heater priority value regarding current datetime
	 */
	public Integer GetCurrentPriority() {

		Integer priority = null;

		try {
			priority = _dbManager.GetCurrentPriorityByHeaterId(_heaterId);
		} catch (Exception e) {
			_logger.error("Erreur lors de la récupération de la priorité du radiateur : " + _heaterId, e);
			SMSDto sms = new SMSDto();
			sms.setMessage("Error occured in heater class, review error log for more details");
			_smsGammuService.sendMessage(sms, true);
		}

		return priority;
	}

	public long getId() {
		return _heaterId;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see java.lang.Comparable#compareTo(java.lang.Object)
	 */
	@Override
	public int compareTo(Heater heater) {

		final int BEFORE = -1;
		final int EQUAL = 0;
		final int AFTER = 1;

		Integer priority = GetCurrentPriority();
		Integer heaterPriority = heater.GetCurrentPriority();

		if (priority == null) {
			_logger.error(String.format("Cannot retrieve %s heater priority", this._name));
			return EQUAL;
		}

		// 20160222 - Correction du bug null pointer exception
		if (heaterPriority == null) {
			_logger.error(String.format("Cannot retrieve %s heater priority", heater.getName()));
			return EQUAL;
		}

		if (priority < heaterPriority) {
			return BEFORE;
		}

		if (priority > heaterPriority) {
			return AFTER;
		}

		if (priority.equals(heaterPriority)) {
			return EQUAL;
		}

		_logger.error(String.format("Cannot compare %s and %s heater priority", this._name, heater.getName()));

		return EQUAL;
	}

	/**
	 * @param dto
	 * @param room
	 * @return Returns Heater object loaded from Dto
	 */
	public static Heater LoadFromDto(HeaterDto dto, Room room, SMSGammuService smsGammuService) {

		return new Heater(dto, room, smsGammuService);
	}

	/**
	 * Sets heater ON
	 */
	public void SetOn() {

		_isOn = true;
		String logMessage = "";

		if (_room != null)
			logMessage = String.format("Turning ON heater %s from room %s", _name, _room.getName());

		/*GpioHelper.provisionGpioPin(_pin, fr.guiet.automationserver.business.PinState.LOW,
				PIN_CHAUFFAGE_NAME + _heaterId, logMessage, PinState.HIGH);*/

		GpioHelper.changeGpioPinState(_pin,fr.guiet.automationserver.business.PinState.LOW, logMessage, _smsGammuService);
	}

	/*
	 * private void StartTeleInfoService() {
	 * _teleInfoService.StartCollectingTeleinfo(String.
	 * format("heater %s from room %s", _name, _room.getName())); }
	 */

	// private void StopTeleInfoService() {
	// _teleInfoService.StopCollectingTeleinfo(String.format("heater %s from
	// room %s", _name, _room.getName()));
	/*
	 * while (!_teleInfoService.IsTeleInfoCollectStopped()) { try {
	 * Thread.sleep(500); } catch (InterruptedException e) {
	 * _logger.error("Error in Heater::StopTeleInfoService method"); break; } }
	 */
	// }

	/**
	 * Sets heater OFF
	 */
	public void SetOff() {

		_isOn = false;
		String logMessage = "";

		if (_room != null)
			logMessage = String.format("Turning OFF heater %s from room %s", _name, _room.getName());

		GpioHelper.changeGpioPinState(_pin,fr.guiet.automationserver.business.PinState.HIGH, logMessage, _smsGammuService);
		
		//GpioHelper.provisionGpioPin(_pin, fr.guiet.automationserver.business.PinState.HIGH,
		//		PIN_CHAUFFAGE_NAME + _heaterId, logMessage, PinState.HIGH);		
	}

	/**
	 * Sets heater OFF even if it should be ON (useful when madam is opening
	 * windows so that fresh air can enters our chamber)
	 */
	public void ActivateOffForced() {

		_isOffForced = true;

		// If heater is ON, set it OFF...
		if (isOn())
			SetOff();

		_logger.info(
				String.format("Setting OFF automatic heater management. Heater %s from room %s is now OFF for ever...",
						_name, _room.getName()));
	}

	/**
	 * Sets heater state so that is automatically regulated
	 */
	public void DesactivateOffForced() {

		_isOffForced = false;

		_logger.info(String.format(
				"Setting ON automatic heater management. Heater %s from room %s is now regulated automatically... ",
				_name, _room.getName()));
	}
}