package com.sapereapi.api;

import java.util.List;
import org.springframework.web.bind.annotation.*;

import com.sapereapi.agent.QueryAgent;
import com.sapereapi.model.Sapere;
import com.sapereapi.model.Service;
import com.sapereapi.model.input.Query;

@RestController
@RequestMapping("/query")
public class QueryController {

	@PostMapping(value = "/addQuery")
	public List<Service> addQuery(@RequestBody Query request) {
		return Sapere.getInstance().addQuery(request);
	}

	@GetMapping(value = "/getResult")
	public String getQueryResultByName(@RequestParam String query) {
		QueryAgent answer = Sapere.getInstance().getQueryByName(query);
		return answer != null ? answer.getSelectedResult() : "No result";
	}

	@GetMapping(value = "/reward")
	public String rewardQueryByName(@RequestParam String name, @RequestParam int reward) {
		QueryAgent answer = Sapere.getInstance().getQueryByName(name);
		return answer.rewardLsaFromApi(reward) ? "Rewarded " : "Not rewarded";
	}

}
