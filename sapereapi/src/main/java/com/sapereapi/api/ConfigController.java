package com.sapereapi.api;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.Set;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.sapereapi.log.SapereLogger;
import com.sapereapi.model.Config;
import com.sapereapi.model.Generate;
import com.sapereapi.model.Sapere;
import com.sapereapi.model.Service;
import com.sapereapi.model.Simulation;

import eu.sapere.middleware.node.NodeManager;

@RestController
@RequestMapping("/config")
public class ConfigController {

	@Autowired
	private ConfigRepository repository;

	@GetMapping(path = "/")
	public Iterable<Config> getAllConfig() {
		return repository.findAll();
	}

	@GetMapping(value = "/{name}")
	public Config getById(@PathVariable(value = "name") String name) {
		return repository.findConfigByname(name);
	}

	@GetMapping(path = "/neighbours")
	public Set<String> getNeighbours() {
		List<Config> config = repository.findAll();
		return config.get(0).getNeighbours();
	}

	@PostMapping(path = "/update")
	public String updateConfig(@RequestBody Config config) {
		repository.deleteAll();
		repository.save(config);
		Sapere.getInstance().initNodeManager(repository);
		return config.getName();
	}

	@PostMapping(path = "/addServiceSim")
	public String updateAgents(@RequestBody Simulation simulation) {
		SapereLogger.getInstance().info("Simulation updated");
		int size = Sapere.getInstance().nodeManager.getSpace().getAllLsa().size();
		for (int i = size; i < size + simulation.getNumber(); i++) {
			Service service = new Service("s" + i, simulation.getInput(), simulation.getOutput(), "", "");
			Sapere.getInstance().addServiceGeneric(service);
		}
		return "ok";
	}

	@PostMapping(path = "/setnodename")
	public String nodename(String nodename) {
		NodeManager.setConfiguration(nodename, NodeManager.getLocalIP(), NodeManager.getLocalPort());
		//Sapere.getInstance().nodeManager.nodeName = nodename;
		return "ok";
	}

	@PostMapping(path = "/generateSim")
	public String generateSimulation(@RequestBody Generate generate) {
		int number = generate.getNumber();
		String set = generate.getSet();
		String[] alph = set.split("-");
		if (alph.length == 2) {
			int size = Sapere.getInstance().nodeManager.getSpace().getAllLsa().size();
			List<String> alphabetSet = new ArrayList<String>();
			for (char c = alph[0].charAt(0); c <= alph[1].charAt(0); c++) {
				alphabetSet.add(Character.toString(c));
			}
			Random rand = new Random();	
			for (int j = size; j < size+number; j++) {
				int input = rand.nextInt(alphabetSet.size());
				
				int output = input;
				while(output==input)
					output = rand.nextInt(alphabetSet.size()-1);
				
				Service service = new Service("s" + j, new String[] {alphabetSet.get(input)}, new String[] {alphabetSet.get(output)}, "", "");
				Sapere.getInstance().addServiceGeneric(service);
				Sapere.startService(service.getName());
			}
			return "ok";
		} else
			return "set error";
	}
}
