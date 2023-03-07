package com.sapereapi.api;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseBody;

import com.sapereapi.model.User;

@Controller
@RequestMapping(path = "/user")
public class UserController {

	@Autowired // This means to get the bean called userRepository
	private UserRepository userRepository;

	@PostMapping(path = "/add") 
	public @ResponseBody String addNewUser(@RequestBody String login, @RequestBody String passwd) {
		User n = new User();
		n.setUsername(login);
		n.setPassword(passwd);
		userRepository.save(n);
		return "Saved";
	}

	@GetMapping(path = "/all")
	public @ResponseBody Iterable<User> getAllUsers() {
		return userRepository.findAll();
	}

	@PostMapping(path = "/checkuser")
	public @ResponseBody Boolean checkUser(@RequestBody User user) {
		Boolean exists = false; 
		for (User u : userRepository.findAll()) {
			if (u.equals(user)) {
				exists = true;
				break;
			}
		}
		return exists;
	}

}
