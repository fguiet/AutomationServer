package fr.guiet.automationserver.business;

import com.rapplogic.xbee.api.XBee;
import com.rapplogic.xbee.api.XBeeAddress64;
import com.rapplogic.xbee.api.wpan.RxResponse64;
import com.rapplogic.xbee.api.wpan.TxRequest64;
import com.rapplogic.xbee.api.XBeeException;
import com.rapplogic.xbee.api.PacketListener;
import com.rapplogic.xbee.api.XBeeResponse;
import com.rapplogic.xbee.api.ApiId;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Properties;

import org.apache.log4j.Logger;

public class XBeeService implements PacketListener {
	
	//TODO : xbee-api-0.9.jar use RXTXComm-1.0.jar ... use Digicom xbee library instead
	
	// Logger
	private static Logger _logger = Logger.getLogger(XBeeService.class);

	private int DEFAULT_BAUD_RATE = 9600;
	private String _xbeeUsbDevice = "";
	private Integer _xbeeBaudRate = null;
	private XBee _xbee = null;

	private ArrayList<IXBeeListener> _sensorList = new ArrayList<IXBeeListener>();
	private boolean _isStopped = true; // Service arrete?

	public XBeeService() {
		
		InputStream is = null;
        try {
        	Properties prop = new Properties();
            is = this.getClass().getResourceAsStream("/config/automationserver.properties");
            prop.load(is);
            
            _xbeeUsbDevice = prop.getProperty("xbee.usbdevice");
            
            try {
            
            	_xbeeBaudRate =  Integer.parseInt(prop.getProperty("xbee.baudrate"));
            }
            catch (NumberFormatException nfe) {
            	_xbeeBaudRate = DEFAULT_BAUD_RATE;
            	_logger.error("La propriété xbee.baudrate doit être un entier, utilisation du baudrate par défaut : "+ DEFAULT_BAUD_RATE, nfe);            	
            }
            
        } catch (FileNotFoundException e) {
        	_logger.error("Impossible de trouver le fichier de configuration classpath_folder/config/automationserver.properties", e);
        } catch (IOException e) {
        	_logger.error("Impossible de trouver le fichier de configuration classpath_folder/config/automationserver.properties", e);
        } 
		
		try {
			_xbee = new XBee();
			_xbee.open(_xbeeUsbDevice, _xbeeBaudRate);
			_xbee.addPacketListener(this);

			_isStopped = false;

			_logger.info("Connexion avec le XBee central réussi...");
		} catch (XBeeException e) {
			_logger.error("Impossible d'ouvrir une connection avec le XBee sur le port " + _xbeeUsbDevice, e);
		}
	}

	// Arret du service XBee
	public void StopService() {

		_xbee.removePacketListener(this);
		_xbee.close();
		_xbee = null;

		_isStopped = true;

		_logger.info("Arrêt du service XBeeService...");

	}

	public boolean isStopped() {
		return _isStopped;
	}

	public void addXBeeListener(IXBeeListener xbeeListener) {
		_sensorList.add(xbeeListener);
		// _xbeeListener = xbeeListener;
	}

	public void processResponse(XBeeResponse response) {
		// _queue.offer(response);
		// if (_xbeeListener != null) {

		if (response.getApiId() == ApiId.RX_64_RESPONSE) {

			RxResponse64 rx = (RxResponse64) response;
			int[] payloadReceived = rx.getData();

			StringBuilder sb = new StringBuilder();

			for (int i = 0; i < payloadReceived.length; i++) {
				char c = (char) payloadReceived[i];
				sb.append(c);
			}

			if (_sensorList != null) {
				for (IXBeeListener xbeeListener : _sensorList) {
					if (xbeeListener.sensorAddress().equals(rx.getRemoteAddress().toString())) {
						xbeeListener.processResponse(sb.toString());
					}
				}
			}
		}
		// }
	}	

	// Envoie d'un message synchrone
	public void SendAsynchronousMessage(XBeeAddress64 sensorAddress, String message) {

		try {
			TxRequest64 request = new TxRequest64(sensorAddress, ConvertMessageToIntArray(message));
			_xbee.sendAsynchronous(request);

			// _logger.info("Envoi du message : "+message+" au module :
			// "+sensorAddress.toString());
			// RxResponse64 rx = (RxResponse64) _xbee.sendSynchronous(request,
			// 1000);
			// TxStatusResponse response = (TxStatusResponse)
			// _xbee.sendSynchronous(request, 1000);
		}
		/*
		 * catch (XBeeTimeoutException te) { _logger.
		 * error("Timeout lors de l'envoi du message. Le XBee distant est allumé?"
		 * , te); } catch(XBeeException e) { _logger.
		 * error("Impossible d'ouvrir une connection avec le XBee sur le port : "
		 * + DEFAULT_COM_PORT, e); }
		 */
		catch (Exception ex) {
			_logger.error("Erreur lors de l'envoi du message.", ex);
		}

		
	}

	private int[] ConvertMessageToIntArray(String message) {

		int[] arr = null;

		try {
			byte[] payloadSent = (message).getBytes("ASCII");
			arr = new int[payloadSent.length];
			for (int i = 0; i < payloadSent.length; i++)
				arr[i] = (int) payloadSent[i];

		} catch (java.io.UnsupportedEncodingException uee) {
			arr = null;
			_logger.error("Erreur lors de la conversion du message a envoyer : " + message, uee);
		}

		return arr;
	}

}
