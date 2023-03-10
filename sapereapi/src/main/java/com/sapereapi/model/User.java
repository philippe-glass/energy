package com.sapereapi.model;


import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

//@Entity // This tells Hibernate to make a table out of this class
//@Table(name = "user")
@Document
public class User {

	@Id
	//@GeneratedValue(strategy = GenerationType.AUTO)
	private String id;

	private String password;

	private String username;

	public String getUsername() {
		return username;
	}

	public void setUsername(String username) {
		this.username = username;
	}

	public String getId() {
		return id;
	}

	public void setId(String id) {
		this.id = id;
	}

	public String getPassword() {
		return password;
	}

	public void setPassword(String password) {
		this.password = password;
	}

	public Boolean equals(User user) {
		return user.getUsername().equals(this.username) && user.getPassword().equals(this.password);
	}

}
