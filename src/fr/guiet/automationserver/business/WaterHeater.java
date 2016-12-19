package fr.guiet.automationserver.business;

import com.pi4j.io.gpio.GpioController;
import com.pi4j.io.gpio.GpioFactory;
import com.pi4j.io.gpio.GpioPinDigitalOutput;
import com.pi4j.io.gpio.PinState;
import com.pi4j.io.gpio.Pin;
import com.pi4j.io.gpio.RaspiPin;
import org.apache.log4j.Logger;
import java.util.Date;

public class WaterHeater implements Runnable { 
	
	private static Logger _logger = Logger.getLogger(WaterHeater.class);	

	private boolean _isOn = false;
    private Date _startTime;
	private boolean _isCheckedOk = false;
	private int _retryStart =0;
	private TeleInfoService _teleInfoService = null;
	private boolean _isStopped = false;  //Service arrete?	
	
	//Constructeur
	public WaterHeater(TeleInfoService teleInfoService) {		
		_teleInfoService = teleInfoService; 
		
	}
	
	@Override
	public void run() {
		
		_logger.info("Démarrage de la gestion du chauffe-eau...");
		
		//Extinction des radiateurs par defaut
		
		while(!_isStopped) {
			
			try {				
				
				ManageWaterHeater();
				
				//Toutes les minutes
				Thread.sleep(60000);
				
			}		
			catch(Exception e) {
				_logger.error("Erreur dans la gestion du chauffe-eau", e);
			}
			
		}
	}
	
	//Stop WaterHeater Service
    public void StopService() {
						
		_logger.info("Arrêt du service WaterHeater...");
		
        _isStopped = true;
    }

	public boolean isOn() {
		return _isOn;
	}
	
	private void ManageWaterHeater() {
		
		//*** Gestion du chauffe eau
		//20160221 - Probleme avec le contacteur qui deconne
		//On considere qu'il y a un probleme si le chauffe est allume et que l'intensite est inferieur a 3000wh
		if (_teleInfoService.IsHeureCreuse() != null && _teleInfoService.IsHeureCreuse() && this.isOn()) {
			//Verifie que le chauffe est bien demarre
			CheckAndRestartIfNecessary(_teleInfoService.GetLastTrame().PAPP);
		}

		if (_teleInfoService.IsHeureCreuse() != null && _teleInfoService.IsHeureCreuse() && !this.isOn()) {
			this.SetOn();
		}
		
		if (_teleInfoService.IsHeureCreuse() != null && !_teleInfoService.IsHeureCreuse() && this.isOn()) {
			this.SetOff();
		}
		//*** Fin gestion du chauffe-eau
		
	}

	//20160221 - Si l'intensite consommée n'est pas correcte on relance c'est que le contacteur n'a pas fonctionné
	private void CheckAndRestartIfNecessary(int puissanceApparente) {
		
		//Test deja execute inutile de relancer..
		if (_isCheckedOk) return;	

		//1 minute ecoulée depuis le demarrage?
//		long elapsed = ((System.currentTimeMillis() - _startTime) / 1000);
		Date currentDate = new Date();
			
		long diff = currentDate.getTime() - _startTime.getTime();
		long diffMinutes = diff / (60 * 1000);	

	    if (diffMinutes >= 1) {
			if (puissanceApparente < 3000) {
				_retryStart ++;
				//On relance!!
				SetOff();
				SetOn();
				_logger.warn("Le chauffe eau a été lancé mais la puissance consommée est de : "+puissanceApparente+". Ceci est incohérent. On relance le chauffe eau (probleme de contacteur?). Tentative de relance : "+ _retryStart);
			}
			else {
				_logger.info("Contrôle du lancement du chauffe eau ok après : "+ _retryStart + " tentative(s) de relance.");
				_retryStart = 0;
				_isCheckedOk = true;
			}
		}
	}
	
	private void SetOn() {
		
		_isCheckedOk = false;	

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
		
		_logger.info("Mise en marche du chauffe-eau");

	    _startTime = new Date();
		_isOn = true;				
			
	}		

	private void SetOff() {		

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

		_logger.info("Extinction du chauffe-eau, durée de chauffe : "+diffMinutes+" minutes");

		_isOn = false;		
		
	}


}
