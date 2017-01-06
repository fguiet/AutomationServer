package fr.guiet.automationserver;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.Socket;
import java.io.DataOutputStream;
import org.apache.log4j.Logger;

import fr.guiet.automationserver.business.*;

//Thread de gestion des connections clients
public class AutomationServerThread extends Thread {

	private Socket _socket = null;
	private RoomService _roomService = null;
	private TeleInfoService _teleInfoService = null; // service de teleinfo
	// Logger
	private static Logger _logger = Logger.getLogger(AutomationServerThread.class);

	public AutomationServerThread(Socket socket, RoomService roomService, TeleInfoService teleInfoService) {
		_socket = socket;
		_roomService = roomService;
		_teleInfoService = teleInfoService;
	}

	// Traite le message recu du serveur web
	private String ProcessMessage(String message) {

		String[] messageArray = message.split(";");
		String answer = null;
		// Room r = null;

		long roomId = Long.parseLong(messageArray[1]);
		// r = _roomService.getRoomById(roomId);

		switch (messageArray[0]) {

		// Eteint de manière forcé le radiateur
		case "SETOFF":
			_roomService.ForceExtinctionRadiateurs(roomId);
			break;

		// Allume de manière forcé le radiateur
		case "SETON":
			_roomService.DesactiveExtinctionForceRadiateurs(roomId);
			break;

		// Change la temperature désirée pour la pièce
		case "SETWANTEDTEMP":
			try {
				float wantedTempFloat = Float.parseFloat(messageArray[2]);
				_roomService.SetWantedTemp(roomId, wantedTempFloat);
			} catch (Exception e) {
				_logger.error("Erreur de conversion dans la temp désirée par l'utilisateur");
			}
			break;

		case "GETROOMINFO":
			String actualTemp = "NA";
			if (_roomService.GetActualTemp(roomId) != null) {
				actualTemp = String.format("%.2f", _roomService.GetActualTemp(roomId));
			}

			String wantedTemp = "NA";
			if (_roomService.GetWantedTemp(roomId) != null) {
				wantedTemp = String.format("%.2f", _roomService.GetWantedTemp(roomId));
			}

			String actualHumidity = "NA";
			if (_roomService.GetActualHumidity(roomId) != null) {
				actualHumidity = String.format("%.2f", _roomService.GetActualHumidity(roomId));
			}

			String nextDefaultTemp = _roomService.NextChangeDefaultTemp(roomId);

			String hasHeaterOn = "HEATEROFF";
			if (_roomService.AtLeastOneHeaterOn(roomId)) {
				hasHeaterOn = "HEATERON";
			}

			String progTemp = "NA";
			Float tempProg = _roomService.GetTempProg(roomId);
			if (tempProg != null) {
				progTemp = String.format("%.2f", tempProg);
			}

			String offForced = "FORCEDHEATEROFF";
			if (_roomService.IsOffForced(roomId)) {
				offForced = "FORCEDHEATERON";
			}

			String sensorKO = "SENSORKO";
			if (_roomService.IsSensorResponding(roomId)) {
				sensorKO = "SENSOROK";
			}

			String papp = "NA";
			String hchc = "NA";
			String hchp = "NA";

			if (_teleInfoService.GetLastTrame() != null) {
				hchc = Integer.toString(_teleInfoService.GetLastTrame().HCHC);
				hchp = Integer.toString(_teleInfoService.GetLastTrame().HCHP);
				papp = Integer.toString(_teleInfoService.GetLastTrame().PAPP);
			}

			answer = "SETROOMINFO;" + messageArray[1] + ";" + actualTemp + ";" + actualHumidity + ";" + progTemp + ";"
					+ nextDefaultTemp + ";" + hasHeaterOn + ";" + offForced + ";" + hchc + ";" + hchp + ";" + papp + ";"
					+ sensorKO + ";" + wantedTemp;
			break;
		}

		return answer;
	}

	// Convertir le message en JSON
	private String ConvertToJSON(String message) {

		String[] messageArray = message.split(";");

		String messageJSON = "{\"order\":\"" + messageArray[0] + "\"";

		boolean first = true;
		int cpt = 1;
		for (String value : messageArray) {
			if (first) {
				first = false;
				continue;
			}

			messageJSON = messageJSON + ", \"value" + cpt + "\":\"" + value + "\"";
			cpt++;
		}

		messageJSON = messageJSON + "}";

		return messageJSON;
	}

	// Démarrage du thread
	public void run() {

		try {
			InputStreamReader inputStream = null;
			DataOutputStream response = null;
			BufferedReader input = null;

			// Process client message until socket is closed...
			// while(!_socket.isClosed()) {

			inputStream = new InputStreamReader(_socket.getInputStream());
			response = new DataOutputStream(_socket.getOutputStream());
			input = new BufferedReader(inputStream);

			String command = input.readLine();
			// String command = input.read();
			// _logger.info("Automation Server a recu la commande : "+command);

			String answer = ProcessMessage(command);

			if (answer != null) {
				response.writeUTF(ConvertToJSON(answer) + "\r\n");
			} else {
				response.writeUTF("\r\n");
			}

			response.flush();

			// Thread.sleep(2000);

			// response.close();
			// inputStream.close();
			// input.close();
			// _socket.close();

			// }

			if (response != null)
				response.close();

			if (inputStream != null)
				inputStream.close();

			if (input != null)
				input.close();
			response = null;
			inputStream = null;
			input = null;
			_socket.close();
		}
		// catch(InterruptedException ie) {
		// _logger.error("Une erreur est apparue dans le thread
		// AutomationServer...",ie);
		// }
		catch (IOException ioe) {
			_logger.error("Une erreur est apparue dans le thread AutomationServer...", ioe);
		}
	}

}
