package com.mtlogic;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.text.NumberFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Vector;

import javax.naming.Context;
import javax.naming.InitialContext;
import javax.naming.NamingException;
import javax.sql.DataSource;

import org.json.JSONArray;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class AdministrationService {
	final Logger logger = LoggerFactory.getLogger(AdministrationService.class);
	
	public static final String ADDED_CLIENT = "Successfully added new client!";
	public static final String CLIENT_NAME_REQUIRED = "--CLIENT NAME IS REQUIRED!--";
	public static final String CLIENT_NUMBER_REQUIRED = "--CLIENT NUMBER IS REQUIRED!--";
	public static final String DUPLICATE_CLIENT = "--DUPLICATE CLIENT NUMBER!--";
	public static final String SERVER_ERROR = "--SERVER ERROR!--";
	public static final String BILL_INVOICE = "BILLING INVOICE FOR ELIGIBILITY ";
	public static final String BILL_INVOICE_HEADER = "Client Name                                               Payer       Inquiries     Charge\n";
	public static final String BILL_INVOICE_SEPARATOR = "------ -------------------------------------------------- ---------- ---------- ----------\n";
	public static final String BILL_INVOICE_TOTAL_SEPARATOR = "                                                                     ---------- ----------\n";
	public static final String BILL_INVOICE_TOTAL = "                                                          Total      ";
	public static final String GRAND_TOTAL = "                                                          Grand Tot  ";
	public static final String lINE_BREAK = "\n------------------------------------------------------------------------------------------\n";
	
	private String dataSourceName;
	private DataSource dataSource;

	public AdministrationService(Boolean isProduction) {
		super();
		if (isProduction) {
			dataSourceName = "eligibility-prod";
		} else {
			dataSourceName = "eligibility-qa";
		}
		dataSource = determineDataSource(isProduction);
	}

	public String lookupPayerCodes() {
		logger.info(">>>ENTERED lookupPayerCodes()");
		Context envContext = null;
		Connection con = null;
		PreparedStatement preparedStatement = null;
		
		String selectMessageSQL = "SELECT name, code FROM public.change_payer_code ORDER BY name";
		StringBuilder jsonStringBuilder = new StringBuilder();
		String payerName = "";
		String payerCode = "";
		
		try {
			envContext = new InitialContext();
			Context initContext  = (Context)envContext.lookup("java:/comp/env");
			DataSource ds = (DataSource)initContext.lookup("jdbc/admin");
			con = ds.getConnection();
						
			preparedStatement = con.prepareStatement(selectMessageSQL);
			
			ResultSet rs = preparedStatement.executeQuery();
			jsonStringBuilder.append("{\"payers\":[");
			while (rs.next()) {
				payerName = rs.getString("name");
				payerCode = rs.getString("code");
				jsonStringBuilder.append("{\"name\":\"");
				jsonStringBuilder.append(payerName);
				jsonStringBuilder.append("\",\"code\":\"");
				jsonStringBuilder.append(payerCode);
				if (rs.isLast()) {
					jsonStringBuilder.append("\"}");
				} else {
					jsonStringBuilder.append("\"},");
				}
			}
			jsonStringBuilder.append("]}");
		} catch (SQLException e) {
			e.printStackTrace();
			logger.error("A SQLException occurred!!! : " + e.getMessage());
		} catch (NamingException e) {
			e.printStackTrace();
			logger.error("A NamingException occurred!!! : " + e.getMessage());
		} catch (Exception e) {
			e.printStackTrace();
			logger.error("ERROR!!! : " + e.getMessage());
		} finally {
		    try{preparedStatement.close();}catch(Exception e){};
		    try{con.close();}catch(Exception e){};
		}
		logger.info("<<<EXITED lookupPayerCodes()");
		return jsonStringBuilder.toString();
	}
	
	public String addClient(String jsonMessage) {
		logger.info(">>>ENTERED addClient(");
		StringBuilder sb = new StringBuilder();
		Vector<String> errorList = new Vector<String>();
		final JSONObject obj = new JSONObject(jsonMessage);
		String clientNumber = null;
		try {
			clientNumber = obj.getString("number");
		} catch (Exception e) {
			errorList.add(CLIENT_NUMBER_REQUIRED);
		}
		String clientName = null;
		try {
			clientName = obj.getString("name");
		} catch (Exception e) {
			errorList.add(CLIENT_NAME_REQUIRED);
		}
		String clientDescription = obj.optString("description");
		String parentNumber = obj.optString("parent");
		
		if (errorList.isEmpty()) {
			Integer parentId = lookupClientId(parentNumber); 
			Context envContext = null;
			Connection con = null;
			PreparedStatement preparedStatement = null;
			String insertUserSQL = "insert into public.client (client_number, parent_id, name, description) values(?, ?, ?, ?)";
			
			try {
				envContext = new InitialContext();
				Context initContext  = (Context)envContext.lookup("java:/comp/env");
				DataSource ds = null;
				ds = (DataSource)initContext.lookup("jdbc/admin");
				
				con = ds.getConnection();					
				preparedStatement = con.prepareStatement(insertUserSQL);
				preparedStatement.setString(1, clientNumber);
				if (parentId != null) {
					preparedStatement.setInt(2, parentId);
				} else {
					preparedStatement.setNull(2, java.sql.Types.INTEGER);
				}
				preparedStatement.setString(3, clientName);
				preparedStatement.setString(4, clientDescription);
				
				preparedStatement.executeUpdate();		
			} catch (SQLException e) {
				e.printStackTrace();
				if (e.getMessage().contains("client_client_number_key")) {
					errorList.addElement(DUPLICATE_CLIENT);
				}
			} catch (NamingException e) {
				e.printStackTrace();
				logger.error("ERROR!!! : " + e.getMessage());
				errorList.addElement(SERVER_ERROR);
			} catch (Exception e) {
				e.printStackTrace();
				logger.error("ERROR!!! : " + e.getMessage());
				errorList.addElement(SERVER_ERROR);
			} finally {
			    try{preparedStatement.close();}catch(Exception e){};
			    try{con.close();}catch(Exception e){};
			}
		}
		
		sb.append("{ \"status\":\"");
		if (errorList.isEmpty()) {
			sb.append(ADDED_CLIENT);
		} else { 
			sb.append(errorList.toString());
		}
		sb.append("\" }");
		
		logger.info("<<<EXITED addClient(" + sb.toString() + ")");
		return sb.toString();
	}
	
	public String retrieveMonthlyEligibilityBillingForClient(Integer clientId, String year, String month) {
		logger.info(">>>ENTERED retrieveMonthlyEligibilityBillingForClient()");
		Integer totalBillableMessages = 0;
		JSONObject json = new JSONObject();
		String endMonth = null;
		int tmpMonth = Integer.parseInt(month);
		if (tmpMonth < 8) {
			endMonth = "0" + (++tmpMonth);
		} else if (tmpMonth >= 8 && tmpMonth <= 11) {
			endMonth = String.valueOf(++tmpMonth);
		} else if (tmpMonth == 12) {
			endMonth = "01";
		} else {
			// bad input data
		}
		
		Map<String, Integer> billingMap = createBillingMapForClient(clientId, year + "-" + month + "-01", year + "-" + endMonth + "-01");
		if (billingMap != null) {
			json.append("payorList", billingMap);
		
			Iterator<?> iter = billingMap.entrySet().iterator();
			while (iter.hasNext()) {
		        Map.Entry pair = (Map.Entry)iter.next();
		        totalBillableMessages += (Integer)pair.getValue();
		    }
			
			Integer transactionFee = lookupTransactionFee(totalBillableMessages);
			json.put("transactionFee", transactionFee);
		}
		
		logger.info("<<<EXITED retrieveMonthlyEligibilityBillingForClient()");
		return json.toString();
	}
	
	public String retrieveMonthlyEligibilityBillingForAllClients(String year, String month) {
		logger.info(">>>ENTERED retrieveMonthlyEligibilityBillingForAllClients()");
		Integer totalBillableMessages = 0;
		JSONObject jsonObject = new JSONObject();
		
		String billPeriodStart = year + "-" + month + "-01";
		String billPeriodEnd = calculateBillPeriodEnd(year, month);
		
		List<Integer> clientList = getClientsForMonthlyBilling(billPeriodStart, billPeriodEnd);
		ClientBillDTO clientBill = null;
		PayorBillDTO payorBill = null;
		List<ClientBillDTO> clientBillList = new ArrayList<ClientBillDTO>();
		List<PayorBillDTO> payorBillList = null;
		Integer transactionFee = null;
		for (Integer clientId : clientList) {
			if (clientId == 1 || clientId == 1258) continue;
			totalBillableMessages = 0;
			ClientDTO client = lookupClientInfo(clientId);
			clientBill = new ClientBillDTO();
			clientBill.setName(client.getName());
	        clientBill.setNumber(client.getClientNumber());
	        payorBillList = new ArrayList<PayorBillDTO>();
			Map<String, Integer> billingMap = createBillingMapForClient(clientId, billPeriodStart, billPeriodEnd);
			if (billingMap != null) {			
				Iterator<?> iter = billingMap.entrySet().iterator();
				while (iter.hasNext()) {
			        Map.Entry pair = (Map.Entry)iter.next();
			        totalBillableMessages += (Integer)pair.getValue();
			        
			    }
		
				transactionFee = lookupTransactionFee(totalBillableMessages);
				
				iter = billingMap.entrySet().iterator();
				
				while (iter.hasNext()) {
					payorBill = new PayorBillDTO();
					
			        Map.Entry pair = (Map.Entry)iter.next();
			        
			        payorBill.setCode((String)pair.getKey());
			        payorBill.setNumberOfInquiries((Integer)pair.getValue());
			        payorBill.setBillAmount(transactionFee*(Integer)pair.getValue());
			        payorBillList.add(payorBill);
			    }
				
			}
			clientBill.setCharge(transactionFee*totalBillableMessages);
			clientBill.setInquiries(totalBillableMessages);
			clientBill.setBillPeriodStart(billPeriodStart);
			SimpleDateFormat df = new SimpleDateFormat("yyyy-MM-dd");
			Date endDate = new Date();
			Calendar cal = Calendar.getInstance();
			
			try {
				endDate = df.parse(billPeriodEnd);
				cal.setTime(df.parse(billPeriodEnd));
			} catch (Exception e) {
				logger.info("Could not parse bill period end date");
			}
			cal.add(Calendar.DAY_OF_MONTH, -1);
			clientBill.setBillPeriodEnd(df.format(cal.getTime()));
			clientBill.setPayorLineItemList(payorBillList);
			clientBillList.add(clientBill);
		}
		
		jsonObject.put("client", clientBillList);
		
		logger.info("<<<EXITED retrieveMonthlyEligibilityBillingForAllClients()");
		return jsonObject.toString();
	}

	public StringBuilder createDetailedMonthlyEligibilityBillingReport(String jsonData) {
		logger.info(">>>ENTERED createDetailedMonthlyEligibilityBillingReportForAllClients()");
		
		NumberFormat nf = NumberFormat.getCurrencyInstance(Locale.US);
		Integer grandTotalCharges = new Integer(0);
		Integer grandTotalInquiries = new Integer(0);
		StringBuilder sb = new StringBuilder(BILL_INVOICE);
		sb.append(new SimpleDateFormat("MM/dd/yyyy").format(new Date()));
		sb.append("\n");
		
		JSONObject jsonObject = new JSONObject(jsonData);
		JSONArray clientArray = jsonObject.getJSONArray("client");
		for (int i=0; i< clientArray.length(); i++) {
			JSONObject object = clientArray.getJSONObject(i);
			String clientName = object.getString("name");
			if (clientName.length()>50) {
				clientName = clientName.substring(0, 50);
			}
			String clientNumber = object.getString("number");
			Integer totalCharge = object.getInt("charge");
			grandTotalCharges += totalCharge;
			Integer totalInquiries = object.getInt("inquiries");
			grandTotalInquiries += totalInquiries;
			sb.append("\n");
			sb.append(BILL_INVOICE_HEADER);
			sb.append(BILL_INVOICE_SEPARATOR);
			
			JSONArray payorArray = object.getJSONArray("payorLineItemList");
			for (int j=0; j< payorArray.length(); j++) {
				sb.append(String.format("%-7s", clientNumber));
				sb.append(String.format("%-51s", clientName));
				sb.append(String.format("%-10s", payorArray.getJSONObject(j).getString("code")));
				sb.append(" ");
				sb.append(String.format("%10s", String.valueOf(payorArray.getJSONObject(j).getInt("numberOfInquiries"))));
				sb.append(" ");
				sb.append(String.format("%10s", String.valueOf(payorArray.getJSONObject(j).getInt("billAmount"))));
				sb.append("\n");
			}
			sb.append(BILL_INVOICE_TOTAL_SEPARATOR);
			sb.append(BILL_INVOICE_TOTAL);
			sb.append(String.format("%10s", totalInquiries));
			sb.append(" "); 
			String formattedTotalCharge = nf.format(totalCharge / 100.0);
			sb.append(String.format("%10s", formattedTotalCharge));
			sb.append("\n");
			sb.append(lINE_BREAK);
		}
		sb.append("\n");
		sb.append(GRAND_TOTAL);
		sb.append(String.format("%10s", String.valueOf(grandTotalInquiries)));
		sb.append(" "); 
		String formattedGrandTotalCharge = nf.format(grandTotalCharges / 100.0);
		sb.append(String.format("%10s", formattedGrandTotalCharge));
		
		logger.info("<<<EXITED createDetailedMonthlyEligibilityBillingReportForAllClients()");
		return sb;
	}
	
	public Integer lookupTransactionFee(Integer numberOfMessages) {
		logger.info(">>>ENTERED lookupTransactionFee()");
		Context envContext = null;
		Connection con = null;
		PreparedStatement preparedStatement = null;
		String selectMessageSQL = "select transaction_fee from public.fee_schedule where min <= ? and max >= ?";
		Integer transactionFee = 0;
		try {
			envContext = new InitialContext();
			Context initContext  = (Context)envContext.lookup("java:/comp/env");
			DataSource ds = (DataSource)initContext.lookup("jdbc/admin");
			con = ds.getConnection();
						
			preparedStatement = con.prepareStatement(selectMessageSQL);
			preparedStatement.setInt(1, numberOfMessages);
			preparedStatement.setInt(2, numberOfMessages);
			
			ResultSet rs = preparedStatement.executeQuery();

			if (rs.next()) {
				transactionFee = rs.getInt(1);
			} 
		} catch (SQLException e) {
			e.printStackTrace();
			logger.error("A SQLException occurred!!! : " + e.getMessage());
		} catch (NamingException e) {
			e.printStackTrace();
			logger.error("A NamingException occurred!!! : " + e.getMessage());
		} catch (Exception e) {
			e.printStackTrace();
			logger.error("ERROR!!! : " + e.getMessage());
		} finally {
		    try{preparedStatement.close();}catch(Exception e){};
		    try{con.close();}catch(Exception e){};
		}
		logger.info("<<<EXITED lookupTransactionFee()");
		return transactionFee;
	}

	public ClientDTO lookupClientInfo(Integer clientId) {
		logger.info(">>>ENTERED lookupClientInfo()");
		Context envContext = null;
		Connection con = null;
		PreparedStatement preparedStatement = null;
		String selectClientSQL = "select client_number, parent_id, name, description from public.client where client_id = ?";
		ClientDTO client = null;
		try {
			envContext = new InitialContext();
			Context initContext  = (Context)envContext.lookup("java:/comp/env");
			DataSource ds = (DataSource)initContext.lookup("jdbc/admin");
			con = ds.getConnection();
						
			preparedStatement = con.prepareStatement(selectClientSQL);
			preparedStatement.setInt(1, clientId);
			
			ResultSet rs = preparedStatement.executeQuery();

			if (rs.next()) {
				client = new ClientDTO(clientId, rs.getInt("parent_id"), rs.getString("client_number"), rs.getString("name"), rs.getString("description"));
			} 
		} catch (SQLException e) {
			e.printStackTrace();
			logger.error("A SQLException occurred!!! : " + e.getMessage());
		} catch (NamingException e) {
			e.printStackTrace();
			logger.error("A NamingException occurred!!! : " + e.getMessage());
		} catch (Exception e) {
			e.printStackTrace();
			logger.error("ERROR!!! : " + e.getMessage());
		} finally {
		    try{preparedStatement.close();}catch(Exception e){};
		    try{con.close();}catch(Exception e){};
		}
		logger.info("<<<EXITED lookupClientInfo()");
		return client;
	}
	
	public Map<String, Integer> createBillingMapForClient(Integer clientId, String beginDate, String endDate) {
		logger.info(">>>ENTERED determineNumberOfBillableMessagesForClient()");
		Context envContext = null;
		Connection con = null;
		PreparedStatement preparedStatement = null;
		Map<String, Integer> billingMap = new HashMap<String, Integer>(); 
		//String selectMessageSQL = "select count(*) from message where client_id = ? and type_id = 4 and billable = true and time::date >= '2017-06-01'::date and time::date <= '2017-06-30'::date";
		String selectMessageSQL = "select payor_code from message where client_id = ? and type_id = 4 and billable = true and time::date >= ?::date and time::date < ?::date";
		
		try {
			envContext = new InitialContext();
			Context initContext  = (Context)envContext.lookup("java:/comp/env");
			//DataSource ds = (DataSource)initContext.lookup("jdbc/eligibility");
			con = dataSource.getConnection();
						
			preparedStatement = con.prepareStatement(selectMessageSQL);
			preparedStatement.setInt(1, clientId);
			preparedStatement.setString(2, beginDate);
			preparedStatement.setString(3, endDate);
			
			ResultSet rs = preparedStatement.executeQuery();

			while (rs.next()) {
				String payorCode = rs.getString(1);
				if (billingMap.containsKey(payorCode)) {
					billingMap.put(payorCode, billingMap.get(payorCode) + 1);
		        } else {
		        	billingMap.put(payorCode, 1);
		        }
			}
		} catch (SQLException e) {
			e.printStackTrace();
			logger.error("A SQLException occurred!!! : " + e.getMessage());
		} catch (NamingException e) {
			e.printStackTrace();
			logger.error("A NamingException occurred!!! : " + e.getMessage());
		} catch (Exception e) {
			e.printStackTrace();
			logger.error("ERROR!!! : " + e.getMessage());
		} finally {
		    try{preparedStatement.close();}catch(Exception e){};
		    try{con.close();}catch(Exception e){};
		}
		logger.info("<<<EXITED determineNumberOfBillableMessagesForClient()");
		return billingMap;
	}
	
	public List<Integer> getClientsForMonthlyBilling(String beginDate, String endDate) {
		logger.info(">>>ENTERED determineNumberOfBillableMessagesForClient()");
		Context envContext = null;
		Connection con = null;
		PreparedStatement preparedStatement = null;
		List<Integer> clientList = new ArrayList<Integer>(); 
		//String selectMessageSQL = "select count(*) from message where client_id = ? and type_id = 4 and billable = true and time::date >= '2017-06-01'::date and time::date <= '2017-06-30'::date";
		String selectMessageSQL = "select distinct client_id from message where type_id = 4 and billable = true and time::date >= ?::date and time::date < ?::date";
		
		try {
			envContext = new InitialContext();
			Context initContext  = (Context)envContext.lookup("java:/comp/env");
			//DataSource ds = (DataSource)initContext.lookup("jdbc/eligibility");
			con = dataSource.getConnection();
						
			preparedStatement = con.prepareStatement(selectMessageSQL);
			preparedStatement.setString(1, beginDate);
			preparedStatement.setString(2, endDate);
			
			ResultSet rs = preparedStatement.executeQuery();

			while (rs.next()) {
				Integer clientId = rs.getInt(1);
				if (clientId != 0) {
					clientList.add(clientId);
		        }
			}
		} catch (SQLException e) {
			e.printStackTrace();
			logger.error("A SQLException occurred!!! : " + e.getMessage());
		} catch (NamingException e) {
			e.printStackTrace();
			logger.error("A NamingException occurred!!! : " + e.getMessage());
		} catch (Exception e) {
			e.printStackTrace();
			logger.error("ERROR!!! : " + e.getMessage());
		} finally {
		    try{preparedStatement.close();}catch(Exception e){};
		    try{con.close();}catch(Exception e){};
		}
		logger.info("<<<EXITED determineNumberOfBillableMessagesForClient()");
		return clientList;
	}
	
	private Integer lookupClientId(String clientNumber) {
		logger.info(">>>ENTERED lookupClientId(" + clientNumber + ")");
		Context envContext = null;
		Connection con = null;
		PreparedStatement preparedStatement = null;
		Integer clientId = null;
		 
		String selectSQL = "select client_id from public.client where client_number = ?";
		
		try {
			envContext = new InitialContext();
			Context initContext  = (Context)envContext.lookup("java:/comp/env");
			DataSource ds = null;
		    ds = (DataSource)initContext.lookup("jdbc/admin");
			con = ds.getConnection();
			
			preparedStatement = con.prepareStatement(selectSQL);
			preparedStatement.setString(1, clientNumber);
			
			ResultSet rs = preparedStatement.executeQuery();
			
			if (rs.next()) {
				clientId = rs.getInt("client_id");
			}
		} catch (SQLException e) {
			e.printStackTrace();
			logger.error("ERROR!!! : " + e.getMessage());
		} catch (NamingException e) {
			e.printStackTrace();
			logger.error("ERROR!!! : " + e.getMessage());
		} catch (Exception e) {
			e.printStackTrace();
			logger.error("ERROR!!! : " + e.getMessage());
		} finally {
		    try{preparedStatement.close();}catch(Exception e){};
		    try{con.close();}catch(Exception e){};
		}

		logger.info("<<<EXITED lookupClientId(" + clientId + ")");
        return (clientId);
    }
	
	private String calculateBillPeriodEnd(String year, String month) {
		Integer monthIndex = Integer.parseInt(month);
		if (monthIndex < 12) {
			monthIndex++;
		}
		StringBuilder sb= new StringBuilder();
		sb.append(monthIndex.toString());
		if (sb.length() < 2) {
			sb.insert(0, "0");
		}
		return year + "-" + sb.toString() + "-01";
	}
	
	private DataSource determineDataSource(Boolean isProduction) {
		DataSource ds = null;
		try {
			Context envContext = new InitialContext();
			Context initContext  = (Context)envContext.lookup("java:/comp/env");
			if (isProduction) {
			    ds = (DataSource)initContext.lookup("jdbc/eligibility-prod");
			} else {
				ds = (DataSource)initContext.lookup("jdbc/eligibility-qa");
			}
		} catch (NamingException e) {
			e.printStackTrace();
			logger.error("ERROR!!! Could not locate datasource!: " + e.getMessage());
		}
		return ds;
	}
}
