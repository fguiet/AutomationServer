package fr.guiet.automationserver.business;

import org.apache.log4j.Logger;

import com.pi4j.io.gpio.RaspiPin;

public class AlarmService {
	
	private final String PIN_ALARM_NAME = "PIN_ALARM";
	private static Logger _logger = Logger.getLogger(AlarmService.class);
	
	public void SetOn() {
	
		try {
			String logMessage = "Turning house alarm ON";
			GpioHelper.provisionGpioPin(RaspiPin.GPIO_25, fr.guiet.automationserver.business.PinState.HIGH,
					PIN_ALARM_NAME, logMessage);
			
			Thread.sleep(1000);
			
			GpioHelper.provisionGpioPin(RaspiPin.GPIO_25, fr.guiet.automationserver.business.PinState.LOW,
					PIN_ALARM_NAME, logMessage);
		}
		catch(Exception e) {
			_logger.error("Error occured setting alarm on", e);
		}		
	}
	
	public void SetOff() {
		
		try {
			String logMessage = "Turning house alarm OFF";
			GpioHelper.provisionGpioPin(RaspiPin.GPIO_26, fr.guiet.automationserver.business.PinState.HIGH,
					PIN_ALARM_NAME, logMessage);
			
			Thread.sleep(1000);
			
			GpioHelper.provisionGpioPin(RaspiPin.GPIO_26, fr.guiet.automationserver.business.PinState.LOW,
					PIN_ALARM_NAME, logMessage);
		}
		catch(Exception e) {
			_logger.error("Error occured setting alarm off", e);
		}	
		
	}
	
}
