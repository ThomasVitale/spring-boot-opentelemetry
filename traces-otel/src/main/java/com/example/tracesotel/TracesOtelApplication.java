package com.example.tracesotel;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@SpringBootApplication
public class TracesOtelApplication {

	public static void main(String[] args) {
		SpringApplication.run(TracesOtelApplication.class, args);
	}

}

@RestController
class GreetingController {

	private static final Logger log = LoggerFactory.getLogger(GreetingController.class);

	@GetMapping("/greeting")
	public String greeting(@RequestParam(value = "name", defaultValue = "World") String name) {
		var greeting = "Hello " + name;
		log.info("Greeting: " +  greeting);
		return greeting;
	}

}
