package fr.guiet.automationserver.business;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;
import java.util.TimeZone;

import org.apache.log4j.Logger;

import com.pi4j.io.gpio.RaspiPin;

import it.sauronsoftware.cron4j.Scheduler;

//TODO : automatic stop at 6AM during week
public class AlarmService {
	
	private final String PIN_ALARM_NAME = "PIN_ALARM";
	private static Logger _logger = Logger.getLogger(AlarmService.class);
	private RollerShutterService _rollerShutterService = null;
	private Scheduler _alarmScheduler = null;
	private String _cronNightAlarmOn = null;
	private String _cronMorningAlarmOff = null;
	private boolean _automaticModeStatus = true; //activated by default
	
	public AlarmService (RollerShutterService rollerShutterService) {
		
		InputStream is = null;
		try {

			String configPath = System.getProperty("automationserver.config.path");
			is = new FileInputStream(configPath);

			Properties prop = new Properties();
			prop.load(is);
			
			String cronNightAlarmOn = prop.getProperty("alarm.schedule.weeknighton");
			if (cronNightAlarmOn != null)
				_cronNightAlarmOn = cronNightAlarmOn;
			else
				_cronNightAlarmOn = "00 22 * * 1,2,3,4,5";
			
			String cronMorningAlarmOff = prop.getProperty("alarm.schedule.weekmorningoff");
			if (cronMorningAlarmOff != null)
				_cronMorningAlarmOff = cronMorningAlarmOff;
			else
				_cronMorningAlarmOff = "00 6 * * 1,2,3,4,5";
											
		} catch (FileNotFoundException e) {
			_logger.error(
					"Impossible de trouver le fichier de configuration classpath_folder/config/automationserver.properties",
					e);
		} catch (IOException e) {
			_logger.error(
					"Erreur lors de la lecture du fichier de configuration classpath_folder/config/automationserver.properties",
					e);
		}
		
		_rollerShutterService = rollerShutterService;
				
		_logger.info("Starting Alarm service...");
		
		_logger.info("Starting automatic alarm ON scheduler at : "+_cronNightAlarmOn);
		_alarmScheduler = new Scheduler();
		//Get Europe/Paris TimeZone
		TimeZone timeZone = TimeZone.getTimeZone("Europe/Paris");
		_alarmScheduler.setTimeZone(timeZone);
		_alarmScheduler.schedule(_cronNightAlarmOn, new Runnable() {
		public void run() {	
			if (_automaticModeStatus)
				SetOn();
		}
		});
		_logger.info("Starting automatic alarm OFF scheduler at : "+_cronMorningAlarmOff);
		_alarmScheduler.schedule(_cronMorningAlarmOff, new Runnable() {
			public void run() {	
				if (_automaticModeStatus)
					SetOff();
			}
			});
		
		_alarmScheduler.start();
	}
	
	public String GetAutomaticModeStatus() {
		if (_automaticModeStatus) {
			return "ON";
		} else {
			return "OFF";
		}
	}
	
	public void SetAutomaticModeOn() {
		_automaticModeStatus = true;
		_logger.info("Setting automatic alarm mode ON");
	}
	
	public void StopService() {
		_alarmScheduler.stop();
		
		_logger.info("Stopping AlarmService service...");

		//_isStopped = true;
	}
	
	public void SetAutomaticModeOff() {
		_automaticModeStatus = false;
		_logger.info("Setting automatic alarm mode OFF");
	}
	
	public void SetOn() {
	
		//GPIO_06 (notation Pi4J) = GPIO_25 (notation raspberry) = PIN22 
		try {
			String logMessage = "Turning house alarm ON";
			GpioHelper.provisionGpioPin(RaspiPin.GPIO_06, fr.guiet.automationserver.business.PinState.HIGH,
					PIN_ALARM_NAME, logMessage);
			
			Thread.sleep(1000);
			
			GpioHelper.provisionGpioPin(RaspiPin.GPIO_06, fr.guiet.automationserver.business.PinState.LOW,
					PIN_ALARM_NAME, null);
			
			//Close all rollers shutters if not allready closed
			_rollerShutterService.CloseAllRollerShutters();
		}
		catch(Exception e) {
			_logger.error("Error occured setting alarm on", e);
		}		
	}
	
	public void SetOff() {
		
		//GPIO_25 (notation Pi4J) = GPIO_26 (notation raspberry) = PIN37
		try {
			String logMessage = "Turning house alarm OFF";
			GpioHelper.provisionGpioPin(RaspiPin.GPIO_25, fr.guiet.automationserver.business.PinState.HIGH,
					PIN_ALARM_NAME, logMessage);
			
			Thread.sleep(1000);
			
			GpioHelper.provisionGpioPin(RaspiPin.GPIO_25, fr.guiet.automationserver.business.PinState.LOW,
					PIN_ALARM_NAME, null);
		}
		catch(Exception e) {
			_logger.error("Error occured setting alarm off", e);
		}	
		
	}
	
}
