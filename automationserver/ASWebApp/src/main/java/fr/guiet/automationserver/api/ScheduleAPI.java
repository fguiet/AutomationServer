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

import fr.guiet.automationserver.dataaccess.DbManager;
import fr.guiet.automationserver.dto.TempScheduleDto;
 
 
@Path("/schedule")
public class ScheduleAPI {
 
    // URI:
    // /contextPath/servletPath/employees
    @POST
    @Produces({ MediaType.APPLICATION_JSON})
    public List<TempScheduleDto> getTempSchedule_JSON() {
    	DbManager dbManager = new DbManager();
    	
        List<TempScheduleDto> listOfTempSchedule = dbManager.getTempSchedule();
    	return listOfTempSchedule;
    }
    
    @POST
    @Produces({ MediaType.APPLICATION_JSON})
    @Consumes("application/json")
    @Path("/create")
    public TempScheduleDto createTempSchedule(TempScheduleDto ts) {
    	DbManager dbManager = new DbManager();
    	return dbManager.createTempScheduleById(ts);
    	
    }
    
    @DELETE
    @Path("/delete/{id}")
    @Produces({ MediaType.APPLICATION_JSON})
    @Consumes("application/json")
    public Response deleteTempSchedule(@PathParam("id") int id){
    	DbManager dbManager = new DbManager();
    	dbManager.deleteTempScheduleById(id);        
        return Response.ok().build();
    }
    
    @PUT
    @Path("/update")
    @Produces({ MediaType.APPLICATION_JSON})
    @Consumes("application/json")
    public Response updateTempSchedule(TempScheduleDto ts){
    	DbManager dbManager = new DbManager();
    	dbManager.updateTempScheduleById(ts);
    	
        return Response.ok().build();
    }    
}