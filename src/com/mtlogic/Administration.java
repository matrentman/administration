package com.mtlogic;

import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import org.apache.http.HttpStatus;
import org.json.JSONException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Path("/api")
public class Administration {
	final Logger logger = LoggerFactory.getLogger(Administration.class);
	
	private Boolean isProduction = Boolean.FALSE;
	
	@Path("/admin/payercode")
	@GET
	@Produces(MediaType.TEXT_PLAIN)
	public Response retrievePayerCodes() throws JSONException 
	{	
		logger.info(">>>ENTERED retrievePayerCodes()");
		
		Response response = null;
		AdministrationService administrationService = null;
		int responseCode = 200;
		String responseMessage = null;
		
		try {	
			administrationService = new AdministrationService(isProduction);
			responseMessage = administrationService.lookupPayerCodes();
		} catch (Exception e) {
			logger.error("Message could not be processed: " + e.getMessage());
			response = Response.status(422).entity("Message could not be processed: " + e.getMessage()).build();
		}
		
		if (response == null) {
			response = Response.status(responseCode).entity(responseMessage).build();
		}
		
		logger.info("<<<EXITED retrievePayerCodes()");
		return response;
	}
	
	@Path("/admin/client")
	@POST
	@Consumes(MediaType.APPLICATION_JSON)
	@Produces(MediaType.TEXT_PLAIN)
	public Response addClient(String inputMessage) throws JSONException 
	{	
		logger.info(">>>ENTERED addCredentials()");
		Response response = null;
		String statusMessage = null;

		int responseCode = HttpStatus.SC_ACCEPTED;
		
		try {
			AdministrationService administrationService = new AdministrationService(isProduction);
			statusMessage = administrationService.addClient(inputMessage);
		} catch (Exception e) {
			logger.error("Message could not be processed: " + e.getMessage());
			e.printStackTrace();
			response = Response.status(HttpStatus.SC_UNPROCESSABLE_ENTITY).entity("Message could not be processed: " + e.getMessage()).build();
		}
		
		if (response == null) {
			response = Response.status(responseCode).entity(statusMessage).build();
		}
		logger.info("<<<EXITED addCredentials()");
		return response;
	}
	
	@Path("/admin/billing/eligibility/{id}/{year}/{month}")
	@GET
	@Produces(MediaType.TEXT_PLAIN)
	public Response retrieveMonthlyEligibilityBillingForClient(@PathParam("id") Integer clientId, @PathParam("year") String year, @PathParam("month") String month) throws JSONException 
	{	
		logger.info(">>>ENTERED retrieveMonthlyEligibilityBillingForClient(" + clientId + ", " + year + ", " + month + ")");
		
		Response response = null;
		AdministrationService administrationService = null;
		int responseCode = 200;
		String jsonString = null;
		
		try {
			administrationService = new AdministrationService(isProduction);
			jsonString = administrationService.retrieveMonthlyEligibilityBillingForClient(clientId, year, month);
		} catch (Exception e) {
			logger.error("Message could not be processed: " + e.getMessage());
			response = Response.status(422).entity("Message could not be processed: " + e.getMessage()).build();
		}
		
		if (response == null) {
			response = Response.status(responseCode).entity(jsonString).build();
		}
		
		logger.info("<<<EXITED retrieveMonthlyEligibilityBillingForClient()");
		return response;
	}
	
	@Path("/admin/billing/eligibility/{year}/{month}")
	@GET
	@Produces(MediaType.TEXT_PLAIN)
	public Response retrieveMonthlyEligibilityBillingForAllClients(@PathParam("year") String year, @PathParam("month") String month) throws JSONException 
	{	
		logger.info(">>>ENTERED retrieveMonthlyEligibilityBillingForAllClients(" + year + ", " + month + ")");
		
		Response response = null;
		AdministrationService administrationService = null;
		int responseCode = 200;
		String jsonString = null;
		
		try {
			administrationService = new AdministrationService(isProduction);
			jsonString = administrationService.retrieveMonthlyEligibilityBillingForAllClients(year, month);
		} catch (Exception e) {
			logger.error("Message could not be processed: " + e.getMessage());
			response = Response.status(422).entity("Message could not be processed: " + e.getMessage()).build();
		}
		
		if (response == null) {
			response = Response.status(responseCode).entity(jsonString).build();
		}
		
		logger.info("<<<EXITED retrieveMonthlyEligibilityBillingForAllClients()");
		return response;
	}
	
	@Path("/admin/billing/eligibility/report/{year}/{month}")
	@GET
	@Produces(MediaType.TEXT_PLAIN)
	public Response createDetailedMonthlyEligibilityBillingReportForAllClients(@PathParam("year") String year, @PathParam("month") String month) throws JSONException 
	{	
		logger.info(">>>ENTERED createDetailedMonthlyEligibilityBillingReportForAllClients(" + year + ", " + month + ")");
		
		Response response = null;
		AdministrationService administrationService = null;
		int responseCode = 200;
		String jsonString = null;
		StringBuilder sb = null;
		
		try {
			administrationService = new AdministrationService(isProduction);
			jsonString = administrationService.retrieveMonthlyEligibilityBillingForAllClients(year, month);
			sb = administrationService.createDetailedMonthlyEligibilityBillingReport(jsonString);
		} catch (Exception e) {
			logger.error("Message could not be processed: " + e.getMessage());
			response = Response.status(422).entity("Message could not be processed: " + e.getMessage()).build();
		}
		
		if (response == null) {
			response = Response.status(responseCode).entity(sb.toString()).build();
		}
		
		logger.info("<<<EXITED createDetailedMonthlyEligibilityBillingReportForAllClients()");
		return response;
	}
	
}