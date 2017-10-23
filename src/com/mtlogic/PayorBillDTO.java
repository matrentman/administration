package com.mtlogic;

public class PayorBillDTO {

	public PayorBillDTO() {

	}

	private String code;
	private Integer numberOfInquiries;
	private Integer billAmount;
	
	public String getCode() {
		return code;
	}
	public void setCode(String code) {
		this.code = code;
	}
	public Integer getNumberOfInquiries() {
		return numberOfInquiries;
	}
	public void setNumberOfInquiries(Integer numberOfInquiries) {
		this.numberOfInquiries = numberOfInquiries;
	}
	public Integer getBillAmount() {
		return billAmount;
	}
	public void setBillAmount(Integer billAmount) {
		this.billAmount = billAmount;
	}
	
}
