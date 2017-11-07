package fr.guiet.automationserver.api;

import java.util.List;

import javax.ws.rs.Consumes;
import javax.ws.rs.DELETE;
import javax.ws.rs.POST;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import org.apache.log4j.Logger;

import com.sun.jersey.api.client.ClientResponse.Status;

import fr.guiet.automationserver.dataaccess.DbManager;
import fr.guiet.automationserver.dto.TempScheduleDto;

@Path("/schedule")
public class ScheduleAPI {

	private static Logger _logger = Logger.getLogger(ScheduleAPI.class);

	// URI:
	// /contextPath/servletPath/employees
	@POST
	@Produces({ MediaType.APPLICATION_JSON })
	public List<TempScheduleDto> getTempSchedule_JSON() {
		try {
			DbManager dbManager = new DbManager();
			List<TempScheduleDto> listOfTempSchedule = dbManager.getTempSchedule();
			return listOfTempSchedule;
			
		} catch (Exception e) {
			_logger.error("Error lors de la récupération des températures", e);
			return null;
		}
	}

	@POST
	@Produces({ MediaType.APPLICATION_JSON })
	@Consumes("application/json")
	@Path("/create")
	public TempScheduleDto createTempSchedule(TempScheduleDto ts) {
		try {
			DbManager dbManager = new DbManager();
			return dbManager.createTempScheduleById(ts);
		} catch (Exception e) {
			_logger.error("Error lors de la création de température", e);
			return null;
		}
	}

	@DELETE
	@Path("/delete/{id}")
	@Produces({ MediaType.APPLICATION_JSON })
	@Consumes("application/json")
	public Response deleteTempSchedule(@PathParam("id") int id) {
		try {
			DbManager dbManager = new DbManager();
			dbManager.deleteTempScheduleById(id);
			return Response.ok().build();
		} catch (Exception e) {
			_logger.error("Error lors de la suppression de température", e);
			return Response.status(Status.INTERNAL_SERVER_ERROR).build();
		}
	}

	@PUT
	@Path("/update")
	@Produces({ MediaType.APPLICATION_JSON })
	@Consumes("application/json")
	public Response updateTempSchedule(TempScheduleDto ts) {
		try {
			DbManager dbManager = new DbManager();
			dbManager.updateTempScheduleById(ts);

			return Response.ok().build();
		} catch (Exception e) {
			_logger.error("Error lors de la mise à jour de température", e);
			return Response.status(Status.INTERNAL_SERVER_ERROR).build();
		}
	}
}