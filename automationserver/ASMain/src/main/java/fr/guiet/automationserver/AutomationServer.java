package fr.guiet.automationserver;

import org.apache.commons.daemon.Daemon;
import org.apache.commons.daemon.DaemonContext;
import org.apache.commons.daemon.DaemonInitException;
import java.util.Locale;
import org.apache.log4j.Logger;
import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import fr.guiet.automationserver.business.*;

/***
 * 
 * Classe principale de gestion de l'IoT de la maison Creation : 201602
 *
 */
public class AutomationServer implements Daemon {

	private Thread _mainThread = null; // Thread principal
	private TeleInfoService _teleInfoService = null; // service de teleinfo
	private RoomService _roomService = null; // service de room service
	private WaterHeater _waterHeater = null; // service de gestion du
												// chauffe-eau
	private boolean _isStopped = false;
	private Thread _roomServiceThread = null;
	private Thread _teleInfoServiceThread = null;
	private Thread _waterHeaterServiceThread = null;

	// Logger
	private static Logger _logger = Logger.getLogger(AutomationServer.class);

	// Initialisation du daemon
	@Override
	public void init(DaemonContext daemonContext) throws DaemonInitException, Exception {
		/*
		 * Construct objects and initialize variables here. You can access the
		 * command line arguments that would normally be passed to your main()
		 * method as follows:
		 */
		// String[] args = daemonContext.getArguments();

		// Initialisation du Thread principal
		_mainThread = new Thread() {

			// Méthode de démarrage du Thread principale
			@Override
			public synchronized void start() {
				AutomationServer.this._isStopped = false;
				super.start(); // Démarrage du Thread principal (Appel de la
								// méthode run)
			}

			// Méthode principale du Thread principal (celle qui boucle)
			@Override
			public void run() {
				try {
					_logger.info("Démarrage du serveur Automation...");
					/*
					 * _logger.info("Wait one minute before starting...");
					 * 
					 * try { Thread.sleep(60000); } catch(InterruptedException
					 * e) {
					 * 
					 * }
					 * 
					 * _logger.info("Running...");
					 */

					// Définit de la locale
					Locale.setDefault(new Locale("en", "GB"));

					// Démarrage du service de teleinfo
					_teleInfoService = new TeleInfoService();
					_teleInfoServiceThread = new Thread(_teleInfoService);
					_teleInfoServiceThread.start();

					// Démarrage du service de gestion des pièces
					_roomService = new RoomService(_teleInfoService);
					_roomServiceThread = new Thread(_roomService);
					_roomServiceThread.start();

					// Démarrage du service de gestion du chauffe-eau
					_waterHeater = new WaterHeater(_teleInfoService);
					_waterHeaterServiceThread = new Thread(_waterHeater);
					_waterHeaterServiceThread.start();

					// Creation du serveur de reception des messages de la
					// console web
					ServerSocket socket = new ServerSocket(4310);
					_logger.info("Serveur d'écoute des messages opérationnels...");

					// CreateCheckThreadActivityTask();

					while (!_isStopped) {

						Socket connection = socket.accept();
						AutomationServerThread ast = new AutomationServerThread(connection, _roomService,
								_teleInfoService);
						ast.start();

					}

					try {
						socket.close();
					} catch (IOException e) {
						_logger.error("Une erreur est apparue dans lors de l'arrêt du serveur Automation...", e);
						socket = null;
					}
				}
				// catch(IOException ioe) {
				// _logger.error("Une erreur est apparue dans le serveur
				// Automation...",ioe);
				// }
				catch (Exception e) {
					_logger.error("Une erreur est apparue dans le serveur Automation...", e);
				}
			}
		};
	}

	// Création de la tache de sauvegarde en bdd
	/*
	 * private void CreateCheckThreadActivityTask() {
	 * 
	 * TimerTask checkThreadActivityTask = new TimerTask() {
	 * 
	 * @Override public void run() { if (_roomServiceThread.isAlive()) {
	 * _logger.info("Thread RoomService Alive"); } else {
	 * _logger.info("Thread RoomService Dead"); }
	 * 
	 * if (_teleInfoServiceThread.isAlive()) {
	 * _logger.info("Thread TeleInfoService Alive"); } else {
	 * _logger.info("Thread TeleInfoService Dead"); }
	 * 
	 * if (_waterHeaterServiceThread.isAlive()) {
	 * _logger.info("Thread WaterHeaterService Alive"); } else {
	 * _logger.info("Thread WaterHeaterService Dead"); } } };
	 * 
	 * Timer timer = new Timer(true); //Toutes les minutes on enregistre une
	 * trame timer.schedule(checkThreadActivityTask, 5000, 60000);
	 * 
	 * }
	 */

	// Méthode start de jsvc (Classe Deamon)
	@Override
	public void start() throws Exception {
		_mainThread.start(); // Démarrage du thread principal
	}

	// Arret du serveur
	@Override
	public void stop() throws Exception {

		// Arret des services
		_teleInfoService.StopService();
		_roomService.StopService();
		_waterHeater.StopService();

		_isStopped = true;

		try {
			_mainThread.join(5000); // Attend la fin de l'exécution du Thread
									// principal (5 secondes)

			_logger.info("Arrêt du serveur Automation...");
		} catch (InterruptedException e) {
			System.err.println(e.getMessage());
			_logger.error("Erreur lors de l'arrêt du serveur Automation ", e);
			throw e;
		}
	}

	// Destruction propre des objets
	@Override
	public void destroy() {
		_mainThread = null;
		_teleInfoService = null;
		_roomService = null;
		_waterHeater = null;
	}

}