package com.sapereapi.model.spatial_services;


public class Blood {

	String Position;
	String BloodBags;
	
	public Blood(String Position, String bloodBags) {
		super();
		this.Position = Position;
		BloodBags = bloodBags;
	}
	public String getPosition() {
		return Position;
	}
	public void setPosition(String position) {
		this.Position = position;
	}
	public String getBloodBags() {
		return BloodBags;
	}
	public void setBloodBags(String bloodBags) {
		BloodBags = bloodBags;
	}
	
	
}
