package com.sapereapi.model;

import java.io.Serializable;
import java.util.Date;

public class Session implements Serializable, Cloneable{
	private static final long serialVersionUID = 1L;
	private Long id;
	private String number;
	private Date creationDate;

	public Session() {
		super();
	}

	public Long getId() {
		return id;
	}

	public void setId(Long id) {
		this.id = id;
	}

	public String getNumber() {
		return number;
	}

	public void setNumber(String number) {
		this.number = number;
	}

	public Date getCreationDate() {
		return creationDate;
	}

	public void setCreationDate(Date creationDate) {
		this.creationDate = creationDate;
	}

	public Session(Long id, String number, Date creationDate) {
		super();
		this.id = id;
		this.number = number;
		this.creationDate = creationDate;
	}

	@Override
	public boolean equals(Object obj) {
		if (obj instanceof Session) {
			Session otherSession = (Session) obj;
			return number.equals(otherSession.getNumber());
		}
		return false;
	}

	@Override
	public String toString() {
		return "Session [" + id + ":" + number + "]";
	}

	public Session clone() {
		return new Session(id, number, creationDate);
	}
}
