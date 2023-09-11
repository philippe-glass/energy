package com.sapereapi.api;

import java.util.List;
import java.util.Map;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.sapereapi.log.SapereLogger;
import com.sapereapi.model.LsaForm;
import com.sapereapi.model.Sapere;
import com.sapereapi.model.Service;

@RestController
@RequestMapping("/sapere")
public class SapereController {

	@GetMapping(value = "/startSapere")
	public String startsapere() {
		SapereLogger.getInstance().info("SAPERE starting...");
		return Sapere.getInstance().getInfo();
	}

	@GetMapping(value = "/diffuse")
	public String diffuseLsa(@RequestParam String name, @RequestParam int hops) {
		return Sapere.getInstance().diffuseLsa(name, hops);
	}

	@GetMapping(value = "/info")
	public String getInfo() {
		return Sapere.getInstance().getInfo();
	}

	@GetMapping(value = "/lsasList")
	public List<Service> getLsaList() {
		return Sapere.getInstance().getServices();
	}

	@GetMapping(value = "/node")
	public List<String> getNodes() {
		return Sapere.getInstance().getNodes();
	}

	@GetMapping(value = "/lsas")
	public List<String> getLsas() {
		return Sapere.getInstance().getLsa();
	}

	@GetMapping(value = "/lsasObj")
	public List<LsaForm> getLsasObj() {
		return Sapere.getInstance().getLsasObj();
	}

    @GetMapping(value="/qtable/{name}")
    public Map<String, Double[]> getQtable(@PathVariable String name) {
        return Sapere.getInstance().getQtable(name);
    }

    @GetMapping(value="/lsa/{name}")
    public String getLsa(@PathVariable String name) {
        return Sapere.getInstance().getLsa(name);
    }
}
