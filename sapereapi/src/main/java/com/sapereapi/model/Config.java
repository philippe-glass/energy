package com.sapereapi.model;

import java.util.Set;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

@Document
public class Config {

	 	@Id
	    private String id;
	    private String name;
	    private String localip;
	    private String localport;
		private Set<String> neighbours;

		public Config() {}
		
		public Config(String id, String name, String localip, String localPort, Set<String> neighbours) {
			super();
			this.id = id;
			this.name = name;
			this.localip = localip;
			this.localport = localPort;
			this.neighbours = neighbours;
		}

		public String getLocalip() {
			return localip;
		}

		public void setLocalip(String localip) {
			this.localip = localip;
		}

		public String getLocalport() {
			return localport;
		}

		public void setLocalport(String localport) {
			this.localport = localport;
		}

		public Set<String> getNeighbours() {
			return neighbours;
		}

		public void addNeighbour(String ip) {
			 neighbours.add(ip);
		}

		public void removeNeighbour(String ip) {
			 neighbours.remove(ip);
		}
		
		public void setNeighbours(Set<String> neighbours) {
			this.neighbours = neighbours;
		}

	    public Config(String name) {
	        this.name = name;
	    }

	    public String getId() {
	        return id;
	    }

	    public void setId(String id) {
	        this.id = id;
	    }

	    public String getName() {
	        return name;
	    }

	    public void setName(String name) {
	        this.name = name;
	    }
}
