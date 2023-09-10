package com.example.metricsotel;

import io.micrometer.core.instrument.MeterRegistry;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@SpringBootApplication
public class MetricsOtelApplication {

	public static void main(String[] args) {
		SpringApplication.run(MetricsOtelApplication.class, args);
	}

}

@RestController
class GreetingController {

	private final MeterRegistry registry;

	GreetingController(MeterRegistry registry) {
		this.registry = registry;
	}

	@GetMapping("/greeting")
	public String greeting(@RequestParam(value = "name", defaultValue = "World") String name) {
		registry.counter("greetings.total", "name", name).increment();
		return "Hello " + name;
	}

}
