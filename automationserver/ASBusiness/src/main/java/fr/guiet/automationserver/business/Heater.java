package fr.guiet.automationserver.business;

import com.pi4j.io.gpio.GpioController;
import com.pi4j.io.gpio.GpioFactory;
import com.pi4j.io.gpio.GpioPinDigitalOutput;
import com.pi4j.io.gpio.PinState;
import com.pi4j.io.gpio.Pin;
import org.apache.log4j.Logger;
import com.pi4j.io.gpio.RaspiPin;
import fr.guiet.automationserver.dto.*;
import fr.guiet.automationserver.dataaccess.DbManager;

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

	private static Logger _logger = Logger.getLogger(Heater.class);

	// Retourne la priorite du radiateur en fonction du jour de la semaine et de
	// l'heure
	public Integer GetCurrentPriority() {
		DbManager dbManager = new DbManager();
		return dbManager.GetCurrentPriorityByHeaterId(_heaterId);
	}

	@Override
	public int compareTo(Heater heater) {

		final int BEFORE = -1;
		final int EQUAL = 0;
		final int AFTER = 1;

		Integer priority = GetCurrentPriority();
		Integer heaterPriority = heater.GetCurrentPriority();

		if (priority == null) {
			_logger.error("Impossible de déterminer la priorité pour le radiateur : " + this._name);
			return EQUAL;
		}

		// 20160222 - Correction du bug null pointer exception
		if (heaterPriority == null) {
			_logger.error("Impossible de déterminer la priorité pour le radiateur : " + heater.getName());
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

		_logger.error("Impossible de comparer la priorite de deux radiateurs");
		return EQUAL;
	}

	public String getName() {
		return _name;
	}

	private Heater(HeaterDto dto, Room room) {

		_room = room;
		_heaterId = dto.heaterId;
		_currentConsumption = dto.currentConsumption;
		_phase = dto.phase;
		_raspberryPin = dto.raspberryPin;
		_name = dto.name;

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
		default:
			// TODO : utiliser un throw ici!
			_logger.error("Pin Raspberry non geree");
			break;
		// throw new Exception("Pin Raspberry non geree");
		}

		SetOff(); // On eteint le radiateur par defaut;
	}

	// retourne la piece associee a ce radiateur
	public Room getRoom() {
		return _room;
	}

	public int getPhase() {
		return _phase;
	}

	public boolean isOn() {
		return _isOn;
	}

	public int getCurrentConsumption() {
		return _currentConsumption;
	}

	public static Heater LoadFromDto(HeaterDto dto, Room room) {

		return new Heater(dto, room);
	}

	// Allume le radiateur
	public void SetOn() {

		_isOn = true;

		// create gpio controller
		final GpioController gpio = GpioFactory.getInstance();

		// provision gpio pin #01 as an output pin and turn on
		final GpioPinDigitalOutput pin = gpio.provisionDigitalOutputPin(_pin, "PIN_CHAUFFAGE_ID" + _heaterId);

		// set shutdown state for this pin
		pin.setShutdownOptions(true, PinState.LOW);

		// turn on gpio pin #01
		pin.low();

		gpio.unprovisionPin(pin);
		gpio.shutdown();

		if (_room != null)
			_logger.info("Allumage du radiateur : " + _name + " de la piece : " + _room.getName());

	}

	// Active l'arret force du radiateur
	public void ActivateOffForced() {

		if (isOn())
			SetOff();

		_logger.info("Activation de l'arrêt forcé du radiateur : " + _name + " de la piece : " + _room.getName());
		_isOffForced = true;
	}

	public void DesactivateOffForced() {
		_logger.info("Déactivation de l'arrêt forcé du radiateur : " + _name + " de la piece : " + _room.getName());
		_isOffForced = false;
	}

	// Le radiateur est t-il en arret force par l'utilisateur
	public boolean IsOffForced() {
		return _isOffForced;
	}

	// Get french string indicated heater state (on/off)
	public String getEtatLabel() {
		if (_isOn)
			return "ALLUME";
		else
			return "ETEINT";
	}

	public void SetOff() {

		_isOn = false;

		// create gpio controller
		final GpioController gpio = GpioFactory.getInstance();

		// provision gpio pin #01 as an output pin and turn on
		final GpioPinDigitalOutput pin = gpio.provisionDigitalOutputPin(_pin, "PIN_CHAUFFAGE_ID" + _heaterId);

		// set shutdown state for this pin
		pin.setShutdownOptions(true, PinState.LOW);

		// turn on gpio pin #01
		pin.high();

		gpio.unprovisionPin(pin);
		gpio.shutdown();

		if (_room != null)
			_logger.info("Extinction du radiateur : " + _name + " de la piece : " + _room.getName());
	}

}