package fr.guiet.automationserver.business.helper;

import org.apache.log4j.Logger;

import com.pi4j.io.gpio.GpioController;
import com.pi4j.io.gpio.GpioFactory;
import com.pi4j.io.gpio.GpioPinDigitalOutput;
import com.pi4j.io.gpio.Pin;

import fr.guiet.automationserver.business.service.SMSGammuService;
import fr.guiet.automationserver.dto.SMSDto;

public class GpioHelper {

	private static Logger _logger = Logger.getLogger(GpioHelper.class);
	
	/*
	 * Try to set pin state on shutdown
	 */
	public static synchronized void shutdown() {
		
		try {
			_logger.info("Setting down Gpio controller...");
			final GpioController gpio = GpioFactory.getInstance();
			
			gpio.shutdown();
		}
		catch(Exception e) {
			_logger.error("Error when shutting down gpio controller...",e);
		}
	}
	
	public static synchronized void changeGpioPinState(Pin gpioPinNumber, PinState state, String logMessage, SMSGammuService smsService) {
		
		try {
			final GpioController gpio = GpioFactory.getInstance();
					
			GpioPinDigitalOutput pin = (GpioPinDigitalOutput)gpio.getProvisionedPin(gpioPinNumber);
						
			if (state == PinState.HIGH) 
			{
				pin.high();
			} else {
				pin.low();
			}
			
			if (logMessage != null) {
				_logger.info(logMessage);
			}
		}		
		catch (Exception e) {
			_logger.error("Error changing pin state "+gpioPinNumber.getAddress()+ ", log message : "+logMessage,e); 
			
			SMSDto sms = new SMSDto("0966fb53-a8fe-4f0f-8b55-ac5d50ce140e");
			sms.setMessage("Cannot change pin state : "+gpioPinNumber.getAddress());
			smsService.sendMessage(sms, true);
		}
	}

	public static synchronized void provisionGpioPin(Pin gpioPinNumber, String logMessage,
			com.pi4j.io.gpio.PinState pinStateOnShutdown, SMSGammuService smsService) {

		try {
			final GpioController gpio = GpioFactory.getInstance();

			// provision gpio pin #01 as an output pin and turn on
			GpioPinDigitalOutput pin = gpio.provisionDigitalOutputPin(gpioPinNumber);

			// set shutdown state for this pin
			pin.setShutdownOptions(true, pinStateOnShutdown);		

			if (logMessage != null) {
				_logger.info(logMessage);
			}
		} catch (Exception e) {
			_logger.error("Error provisionning pin "+gpioPinNumber.getAddress()+ ", log message"+logMessage,e);
			
			SMSDto sms = new SMSDto("ed1b236f-3e1c-4d96-8c9a-73283d1db7a0");
			sms.setMessage("Cannot provision pin : "+gpioPinNumber.getAddress());
			smsService.sendMessage(sms, true);
		}

	}

}
