package fr.guiet.automationserver.business;

import com.rapplogic.xbee.api.XBee;
import com.rapplogic.xbee.api.XBeeAddress64;
import com.rapplogic.xbee.api.wpan.RxResponse64;
import com.rapplogic.xbee.api.wpan.TxRequest64;
import com.rapplogic.xbee.api.XBeeException;
import com.rapplogic.xbee.api.PacketListener;
import com.rapplogic.xbee.api.XBeeResponse;
import com.rapplogic.xbee.api.ApiId;
import java.util.ArrayList;

import org.apache.log4j.Logger;

public class XBeeService implements PacketListener {
	
	//TODO : xbee-api-0.9.jar use RXTXComm-1.0.jar ... use Digicom xbee library instead
	
	// Logger
	private static Logger _logger = Logger.getLogger(XBeeService.class);

	private static final String DEFAULT_COM_PORT = "/dev/ttyUSB0";
	private XBee _xbee = null;
	// private IXBeeListener _xbeeListener = null;
	// private ConcurrentLinkedQueue<XBeeResponse> _queue = new
	// ConcurrentLinkedQueue<XBeeResponse>();
	private ArrayList<IXBeeListener> _sensorList = new ArrayList<IXBeeListener>();
	private boolean _isStopped = true; // Service arrete?

	public XBeeService() {
		try {
			_xbee = new XBee();
			_xbee.open(DEFAULT_COM_PORT, 9600);
			_xbee.addPacketListener(this);

			_isStopped = false;

			_logger.info("Connexion avec le XBee central réussi...");
		} catch (XBeeException e) {
			_logger.error("Impossible d'ouvrir une connection avec le XBee sur le port " + DEFAULT_COM_PORT, e);
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

	/*
	 * public XBeeMessage GetSensorMessages() {
	 * 
	 * XBeeMessage xbeeMessage = null; XBeeResponse response; int []
	 * payloadReceived;
	 * 
	 * response = _queue.poll();
	 * 
	 * if (response != null) { try {
	 * 
	 * if (response.getApiId() == ApiId.RX_64_RESPONSE) {
	 * 
	 * RxResponse64 rx = (RxResponse64) response; payloadReceived =
	 * rx.getData();
	 * 
	 * StringBuilder sb = new StringBuilder();
	 * 
	 * for (int i=0;i<payloadReceived.length;i++) { char c =
	 * (char)payloadReceived[i]; sb.append(c); }
	 * 
	 * //_logger.info("Messsage recu"+sb.toString()); xbeeMessage = new
	 * XBeeMessage(rx.getRemoteAddress());
	 * xbeeMessage.setMessageReceived(sb.toString());
	 * 
	 * } } catch(Exception ex) { xbeeMessage = null;
	 * _logger.error("Erreur dans le traitement des trames reçues par le XBee"
	 * ,ex); } }
	 * 
	 * return xbeeMessage; }
	 */

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

		/*
		 * if (response != null) { try {
		 * 
		 * if (response.getApiId() == ApiId.RX_64_RESPONSE) {
		 * 
		 * RxResponse64 rx = (RxResponse64) response; payloadReceived =
		 * rx.getData();
		 * 
		 * StringBuilder sb = new StringBuilder();
		 * 
		 * for (int i=0;i<payloadReceived.length;i++) { char c =
		 * (char)payloadReceived[i]; sb.append(c); }
		 * 
		 * //_logger.info("Messsage recu"+sb.toString()); xbeeMessage = new
		 * XBeeMessage(rx.getRemoteAddress());
		 * xbeeMessage.setMessageReceived(sb.toString());
		 * 
		 * } } catch(Exception ex) { xbeeMessage = null; _logger.
		 * error("Erreur dans le traitement des trames reçues par le XBee",ex);
		 * } }
		 */

		/*
		 * int retryCpt = 1; boolean sentOk = false; TxStatusResponse response =
		 * null;
		 * 
		 * try { TxRequest64 request = new TxRequest64(_sensorAddress,
		 * "GETSENSORINFO");
		 * 
		 * while(retryCpt <= 3 && !sentOk) {
		 * 
		 * //_logger.info("Debut envoie message synchrone"); // send the packet
		 * and wait up to 1 seconds for the transmit status reply (you should
		 * really catch a timeout exception)
		 * _logger.info("Envoi du message : "+xbeeMessage.getMessageToSendString
		 * ()+" au module : "+xbeeMessage.getRemoteAddress().toString()+
		 * " (Tentative numero : "+retryCpt+")"); response =
		 * (TxStatusResponse)_xbee.sendSynchronous(request, 1000);
		 * //_logger.info("fin envoie message synchrone");
		 * 
		 * if (response.isSuccess()) { sentOk = true; } else { _logger.
		 * warn("Message envoyé à un module mais ACK non recu (Tentative numero : "
		 * +retryCpt+")"); retryCpt++; } } } catch (XBeeTimeoutException te) {
		 * _logger.
		 * error("Timeout lors de l'envoi du message. Le XBee distant est allumé?"
		 * , te); } catch(Exception e) { _logger.
		 * error("Erreur lors de l'envoi du message. Le XBee distant est allumé?"
		 * , e); }
		 * 
		 * return sentOk;
		 */
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
