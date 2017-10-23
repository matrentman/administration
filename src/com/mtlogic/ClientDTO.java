package com.mtlogic;

public class ClientDTO {

	public ClientDTO() {}
	
	public ClientDTO(Integer id, Integer parentId, String clientNumber, String name, String description) {
		this.id = id;
		this.parentId = parentId;
		this.clientNumber = clientNumber;
		this.name = name;
		this.description = description;
	}
	
	private Integer id;
	private Integer parentId;
	private String clientNumber;
	private String name;
	private String description;
	
	public Integer getId() {
		return id;
	}
	public void setId(Integer id) {
		this.id = id;
	}
	public Integer getParentId() {
		return parentId;
	}
	public void setParentId(Integer parentId) {
		this.parentId = parentId;
	}
	public String getClientNumber() {
		return clientNumber;
	}
	public void setClientNumber(String clientNumber) {
		this.clientNumber = clientNumber;
	}
	public String getName() {
		return name;
	}
	public void setName(String name) {
		this.name = name;
	}
	public String getDescription() {
		return description;
	}
	public void setDescription(String description) {
		this.description = description;
	}

}
