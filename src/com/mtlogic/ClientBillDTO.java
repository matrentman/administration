package com.mtlogic;

import java.util.List;

public class ClientBillDTO {

	public ClientBillDTO() {

	}

	private String number;
	private String name;
	private Integer charge;
	private Integer inquiries;
	private String billPeriodStart;
	private String billPeriodEnd;
	private List<PayorBillDTO> payorLineItemList;
	
	public String getNumber() {
		return number;
	}
	public void setNumber(String number) {
		this.number = number;
	}
	public String getName() {
		return name;
	}
	public void setName(String name) {
		this.name = name;
	}
	public Integer getCharge() {
		return charge;
	}
	public void setCharge(Integer charge) {
		this.charge = charge;
	}
	public String getBillPeriodStart() {
		return billPeriodStart;
	}
	public void setBillPeriodStart(String billPeriodStart) {
		this.billPeriodStart = billPeriodStart;
	}
	public String getBillPeriodEnd() {
		return billPeriodEnd;
	}
	public void setBillPeriodEnd(String billPeriodEnd) {
		this.billPeriodEnd = billPeriodEnd;
	}
	public List<PayorBillDTO> getPayorLineItemList() {
		return payorLineItemList;
	}
	public void setPayorLineItemList(List<PayorBillDTO> payorLineItemList) {
		this.payorLineItemList = payorLineItemList;
	}
	public Integer getInquiries() {
		return inquiries;
	}
	public void setInquiries(Integer inquiries) {
		this.inquiries = inquiries;
	}
	
}
