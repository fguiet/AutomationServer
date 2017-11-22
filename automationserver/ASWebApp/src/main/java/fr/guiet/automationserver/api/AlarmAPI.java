package fr.guiet.automationserver.api;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriInfo;

import fr.guiet.automationserver.business.MqttClientMgt;
import org.apache.log4j.Logger;

@Path("/alarm")
public class AlarmAPI {
	
	private static Logger _logger = Logger.getLogger(AlarmAPI.class);
	private static String _topic = "/guiet/api/alarm";
	private static String _mqttClientId = "AlarmAPIClientId";
	
	@GET	
	public Response sendMsg(@Context UriInfo info) {
		
		
		String action = info.getQueryParameters().getFirst("action");
				
		//_logger.info("Received Google Assistant Message : "+msg);
		
		//String [] messageContent = msg.split(";");
		
		//if (messageContent != null && messageContent.length > 0) {
			//String action = messageContent[0];
			
			switch(action) {
			case "SETALARM":
									
				String mode = info.getQueryParameters().getFirst("mode");								
				
				_logger.info("AlarmAPI receives action request : " + action + ", mode : "+mode);
										
				String message = action + ";" + mode;
			
				MqttClientMgt rtt = new MqttClientMgt(_mqttClientId);
				rtt.SendMsg(_topic, message);
				
				break;
			}
		
				

		return Response.ok().build();

	}


}
