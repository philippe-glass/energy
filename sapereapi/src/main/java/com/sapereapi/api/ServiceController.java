package com.sapereapi.api;

import java.util.List;

import org.springframework.web.bind.annotation.*;

import com.sapereapi.model.Sapere;
import com.sapereapi.model.Service;

@RestController
public class ServiceController {

	@PostMapping(value = "/addService")
	public List<Service> addService (@RequestBody Service service){
		Sapere.getInstance().addServiceGeneric(service);
		return Sapere.getInstance().getLsas();
	}
	
}
