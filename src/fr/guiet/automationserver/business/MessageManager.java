package fr.guiet.automationserver.business;

//MessageManager (au lieu de XBeeManager)
//Procedure Brodcast message to sensor

import org.apache.log4j.Logger;

public class MessageManager  implements Runnable {
	
	private static Logger _logger = Logger.getLogger(MessageManager.class);
	
	@Override
	public void run() {
		
		//Creation d'un serveur de reception des messages ici 
		
		
	}
}