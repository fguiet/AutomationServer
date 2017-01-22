package fr.guiet.automationserver.business;

import com.pi4j.io.gpio.GpioController;
import com.pi4j.io.gpio.GpioFactory;
import com.pi4j.io.gpio.GpioPinDigitalOutput;
import com.pi4j.io.gpio.PinState;
import com.pi4j.io.gpio.RaspiPin;

import fr.guiet.automationserver.dto.SMSDto;
import fr.guiet.automationserver.dto.TeleInfoTrameDto;

import org.apache.log4j.Logger;
import java.util.Date;

public class WaterHeater implements Runnable, ICollectInfoStopListener  {

	private static Logger _logger = Logger.getLogger(WaterHeater.class);

	private boolean _isOn = false;
	private Date _startTime;
	private boolean _isCheckedOk = false;
	private int _retryStart = 0;
	private TeleInfoService _teleInfoService = null;
	private boolean _isStopped = false; // Service arrete?
	private SMSGammuService _smsGammuService = null;
	private boolean _waitForOn = false;
	private boolean _waitForOff = false;

	@Override
	public void OnCollectInfoStopped() {

		
		//_logger.info("Hello from water heater");
		
		if (_waitForOn) {
			_waitForOn = false;
			TurnOn();
			StartTeleInfoService();
		}

		if (_waitForOff) {
			_waitForOff = false;
			TurnOff();
			StartTeleInfoService();
		}
	}

	/**
	 * @return Returns whether heater is on or off
	 */
	public boolean isOn() {
		return _isOn;
	}

	/**
	 * Constructor
	 * 
	 * @param teleInfoService
	 */
	public WaterHeater(TeleInfoService teleInfoService, SMSGammuService smsGammuService) {
		_teleInfoService = teleInfoService;
		_teleInfoService.addListener(this);
		_smsGammuService = smsGammuService;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see java.lang.Runnable#run()
	 */
	@Override
	public void run() {

		_logger.info("Starting water heater management...");
		
		//By Default set off
		SetOff();

		while (!_isStopped) {

			try {

				ManageWaterHeater();

				// Sleep for 1 minutes
				Thread.sleep(60000);

			} catch (Exception e) {
				_logger.error("Error occured in water heater management service", e);

				SMSDto sms = new SMSDto();
				sms.setMessage("Error occured in water heater management service, review error log for more details");
				_smsGammuService.sendMessage(sms, true);
			}
		}
	}

	/**
	 * Stops water heater properly
	 */
	public void StopService() {

		_logger.info("Stopping water heater service...");

		_isStopped = true;
	}

	/**
	 * Water heater main management method
	 */
	private void ManageWaterHeater() {

		// *** Gestion du chauffe eau
		// 20160221 - Probleme avec le contacteur qui deconne
		// On considere qu'il y a un probleme si le chauffe est allume et que
		// l'intensite est inferieur a 3000wh
		if (_teleInfoService.IsHeureCreuse() != null && _teleInfoService.IsHeureCreuse() && this.isOn()) {
			// Verifie que le chauffe est bien demarre
			TeleInfoTrameDto teleInfoTrame = _teleInfoService.GetLastTrame();

			if (teleInfoTrame != null)
				CheckAndRestartIfNecessary(teleInfoTrame.PAPP);
			else
				_logger.error(
						"Could not check and restart water heater boiler because no teleinfotrame is available...");
		}

		if (_teleInfoService.IsHeureCreuse() != null && _teleInfoService.IsHeureCreuse() && !this.isOn()) {
			this.SetOn();
		}

		if (_teleInfoService.IsHeureCreuse() != null && !_teleInfoService.IsHeureCreuse() && this.isOn()) {
			this.SetOff();
		}
		// *** Fin gestion du chauffe-eau

	}

	// 20160221 - Si l'intensite consommée n'est pas correcte on relance c'est
	// que le contacteur n'a pas fonctionné
	private void CheckAndRestartIfNecessary(int puissanceApparente) {

		// Test deja execute inutile de relancer..
		if (_isCheckedOk)
			return;

		// 1 minute ecoulée depuis le demarrage?
		// long elapsed = ((System.currentTimeMillis() - _startTime) / 1000);
		Date currentDate = new Date();

		long diff = currentDate.getTime() - _startTime.getTime();
		long diffMinutes = diff / (60 * 1000);

		if (diffMinutes >= 1) {
			if (puissanceApparente < 3000) {
				_retryStart++;
				// On relance!!
				SetOff();
				SetOn();
				_logger.warn("Le chauffe eau a été lancé mais la puissance consommée est de : " + puissanceApparente
						+ ". Ceci est incohérent. On relance le chauffe eau (probleme de contacteur?). Tentative de relance : "
						+ _retryStart);
			} else {
				_logger.info(
						"Contrôle du lancement du chauffe eau ok après : " + _retryStart + " tentative(s) de relance.");
				_retryStart = 0;
				_isCheckedOk = true;
			}
		}
	}

	/**
	 * Turns water heater ON
	 */
	private void SetOn() {

		StopTeleInfoService();
		_waitForOn = true;
	}

	private void StopTeleInfoService() {
		_teleInfoService.StopCollectingTeleinfo("WaterHeater");
	}

	private void TurnOff() {

		// create gpio controller
		final GpioController gpio = GpioFactory.getInstance();

		// provision gpio pin #01 as an output pin and turn on
		final GpioPinDigitalOutput pin = gpio.provisionDigitalOutputPin(RaspiPin.GPIO_00, "PIN_CHAUFFE_EAU");

		// set shutdown state for this pin
		pin.setShutdownOptions(true, PinState.LOW);

		// turn on gpio pin #00
		pin.low();

		gpio.unprovisionPin(pin);
		gpio.shutdown();

		Date currentDate = new Date();

		long diff = currentDate.getTime() - _startTime.getTime();
		long diffMinutes = diff / (60 * 1000);

		_logger.info("Turning OFF water heater. Water heater was ON during : " + diffMinutes + " minutes");

		_isOn = false;

	}

	private void TurnOn() {
		_isCheckedOk = false;

		// TODO : faire une classe métier pour la gestion des PINS
		// create gpio controller
		final GpioController gpio = GpioFactory.getInstance();

		// provision gpio pin #01 as an output pin and turn on
		final GpioPinDigitalOutput pin = gpio.provisionDigitalOutputPin(RaspiPin.GPIO_00, "PIN_CHAUFFE_EAU");

		// set shutdown state for this pin
		pin.setShutdownOptions(true, PinState.LOW);

		// turn on gpio pin #00
		pin.high();

		gpio.unprovisionPin(pin);
		gpio.shutdown();

		_logger.info("Turning ON water heater");

		_startTime = new Date();
		_isOn = true;
	}

	/**
	 * Turns water heater OFF
	 */
	private void SetOff() {

		StopTeleInfoService();
		_waitForOff=true;	

	}

	private void StartTeleInfoService() {
		_teleInfoService.StartCollectingTeleinfo("WaterHeater");
	}
}
