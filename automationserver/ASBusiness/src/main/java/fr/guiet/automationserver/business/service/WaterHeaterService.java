package fr.guiet.automationserver.business.service;

import com.pi4j.io.gpio.Pin;
import com.pi4j.io.gpio.RaspiPin;

import fr.guiet.automationserver.business.helper.DateUtils;
import fr.guiet.automationserver.business.helper.GpioHelper;
import fr.guiet.automationserver.dataaccess.DbManager;
import fr.guiet.automationserver.dto.SMSDto;
import fr.guiet.automationserver.dto.TeleInfoTrameDto;

import org.apache.log4j.Logger;
import java.util.Date;

public class WaterHeaterService implements Runnable {

	private static Logger _logger = Logger.getLogger(WaterHeaterService.class);

	private boolean _isOn = false;
	private Date _startTime;
	private boolean _isCheckedOk = false;
	private int _retryStart = 0;
	private TeleInfoService _teleInfoService = null;
	private boolean _isStopped = false; // Service arrete?
	private SMSGammuService _smsGammuService = null;	
	private final Pin _pinWaterHeater = RaspiPin.GPIO_00;
	private boolean _awayModeStatus = false;
	private DbManager _dbManager = null;
	private Date _lastInfoReceived = null;
	private Long _lastOnDuration = null;
	private Float _actualTemp = null;
	

	/**
	 * @return Returns whether heater is on or off
	 */
	public boolean isOn() {
		return _isOn;
	}
	
	public String getState() {
		if (_isOn) {
			return "ON";
		}
		else {
			return "OFF";
		}
	}
	
	public Float getActualTemp() {
		return _actualTemp;
	}


	/**
	 * Constructor
	 * 
	 * @param teleInfoService
	 */
	public WaterHeaterService(TeleInfoService teleInfoService, SMSGammuService smsGammuService) {
		_teleInfoService = teleInfoService;
		// _teleInfoService.addListener(this);
		_smsGammuService = smsGammuService;
		
		_dbManager = new DbManager();
	}
	
	public void SetAwayModeOn() {
		_awayModeStatus = true;
		_logger.info("Setting away mode ON");
	}

	public void SetAwayModeOff() {
		_awayModeStatus = false;
		_logger.info("Setting away mode OFF");
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see java.lang.Runnable#run()
	 */
	@Override
	public void run() {

		_logger.info("Starting water heater management...");

		GpioHelper.provisionGpioPin(_pinWaterHeater,
				 "Provisioning Pin address : "+_pinWaterHeater.getAddress()+" for water heater", com.pi4j.io.gpio.PinState.LOW, _smsGammuService);
		
		// By Default set off
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
	
	public String LastInfoReceived() {
		
		if (_lastInfoReceived != null) {
			return DateUtils.getDateToString(_lastInfoReceived);
		}
		else {
			return "NA";
		}
	}
	
	public String LastOnDuration() {
		
		if (_lastOnDuration != null) {
			return _lastOnDuration.toString();
		}
		else {
			return "NA";
		}
		
	}

	/*
	 * Saving boiler temp in InfluxDb database
	 */
	public void SaveTemp(float temp){
		_actualTemp = temp;
		_lastInfoReceived = new Date();
		
		_dbManager.SaveBoilerTemp(temp);
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
		if (_isCheckedOk || !(DateUtils.getCurrentHour() >= 0 && DateUtils.getCurrentHour() <= 5))
			return;
		
		//Ne faire la vérification le jour car il se peut que le chauffe soit deja chaud et qu'il ne consomme pas de courant
		//donc c'est normal
		if (DateUtils.getCurrentHour() >= 7 && DateUtils.getCurrentHour() <= 22) {
			return;
		}

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
		
		//Pas de demarrage du boiler si away mode est on
		if (_awayModeStatus) 
			return;
					
		_isCheckedOk = false;

		String logMessage = "Turning ON water heater";
		
		GpioHelper.changeGpioPinState(_pinWaterHeater,fr.guiet.automationserver.business.helper.PinState.HIGH, logMessage, _smsGammuService);
		
		_startTime = new Date();

		_isOn = true;

	}


	/**
	 * Turns water heater OFF
	 */
	private void SetOff() {		
		
		Date currentDate = new Date();

		long diffMinutes = 0;
		if (_startTime != null) {
			// occurs at launch of service
			long diff = currentDate.getTime() - _startTime.getTime();
			diffMinutes = diff / (60 * 1000);
		}

		_lastOnDuration = diffMinutes;
		
		String logMessage = "Turning OFF water heater. Water heater was ON during : " + diffMinutes + " minutes";
		
		GpioHelper.changeGpioPinState(_pinWaterHeater,fr.guiet.automationserver.business.helper.PinState.LOW, logMessage, _smsGammuService);
		
		/*GpioHelper.provisionGpioPin(RaspiPin.GPIO_00, fr.guiet.automationserver.business.PinState.LOW,
				PIN_WATER_HEATER_NAME, logMessage, com.pi4j.io.gpio.PinState.LOW);*/
		
		_isOn = false;
	}
}
