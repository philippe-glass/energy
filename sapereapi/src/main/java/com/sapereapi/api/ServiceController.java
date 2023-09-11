package com.sapereapi.api;

import java.util.List;

import org.springframework.web.bind.annotation.*;

import com.sapereapi.model.Sapere;
import com.sapereapi.model.Service;

@RestController
@RequestMapping("/service")
public class ServiceController {

	@PostMapping(value = "/addService")
	public List<Service> addService (@RequestBody Service service){
		return Sapere.getInstance().addServiceGeneric(service);
	}
	
}
