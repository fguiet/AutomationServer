package fr.guiet.automationserver.business;

import org.apache.log4j.Logger;

import com.pi4j.io.gpio.GpioController;
import com.pi4j.io.gpio.GpioFactory;
import com.pi4j.io.gpio.GpioPinDigitalOutput;
import com.pi4j.io.gpio.Pin;

public class GpioHelper {

	private static Logger _logger = Logger.getLogger(GpioHelper.class);

	public static void provisionGpioPin(Pin gpioPinNumber, PinState state, String pinName, String logMessage) {

		try {

			final GpioController gpio = GpioFactory.getInstance();

			// provision gpio pin #01 as an output pin and turn on
			final GpioPinDigitalOutput pin = gpio.provisionDigitalOutputPin(gpioPinNumber, pinName);

			// set shutdown state for this pin
			pin.setShutdownOptions(true, com.pi4j.io.gpio.PinState.LOW);

			if (state == PinState.HIGH) {
				pin.high();
			} else {
				pin.low();
			}

			gpio.unprovisionPin(pin);

			gpio.shutdown();

			if (logMessage != null) {
				_logger.info(logMessage);
			}
		} catch (Exception e) {
			_logger.error("Error setting pin "+pinName+ ", log message"+logMessage,e);
			// TODO : Send SMS here
		}

	}

}