package fr.guiet.automationserver.business;

import org.apache.log4j.Logger;

import com.pi4j.io.gpio.RaspiPin;

public class AlarmService {
	
	private final String PIN_ALARM_NAME = "PIN_ALARM";
	private static Logger _logger = Logger.getLogger(AlarmService.class);
	private RollerShutterService _rollerShutterService = null;
	
	public AlarmService (RollerShutterService rollerShutterService) {
		_rollerShutterService = rollerShutterService;
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
