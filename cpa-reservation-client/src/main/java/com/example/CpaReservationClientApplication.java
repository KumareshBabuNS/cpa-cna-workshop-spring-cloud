package com.example;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;
import org.springframework.cloud.client.loadbalancer.LoadBalanced;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.hateoas.MediaTypes;
import org.springframework.hateoas.Resources;
import org.springframework.hateoas.hal.Jackson2HalModule;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageConverter;
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.client.RestTemplate;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.netflix.hystrix.contrib.javanica.annotation.HystrixCommand;

@EnableDiscoveryClient
@SpringBootApplication
public class CpaReservationClientApplication {

	public static void main(String[] args) {
		SpringApplication.run(CpaReservationClientApplication.class, args);
	}
}


@RequestMapping("/reservations")
@RestController
@Configuration
class ReservationController{

//	@LoadBalanced @Bean
//	RestTemplate template = new RestTemplate();
	
	@LoadBalanced
    @Bean
    RestTemplate restTemplate() {
        return new RestTemplate();
    }
	
	public Collection<String> getNamesFallback(){
		return Arrays.asList("Service not available");
	}
	

	@HystrixCommand(fallbackMethod="getNamesFallback")
	@RequestMapping("/names")
	public Collection<String> getNames(){
		
		RestTemplate template = restTemplate(); 
		
		template.setMessageConverters(getHttpMessageConverters());

		ParameterizedTypeReference<Resources<Reservation>> ptr = new ParameterizedTypeReference<Resources<Reservation>>(){};
		
		ResponseEntity<Resources<Reservation>> response = 
				template.exchange("http://cpa-reservation-service/reservations", HttpMethod.GET, null, ptr);
				
		return response.getBody().getContent()
				.stream()
				.map(Reservation::getName)
				.collect(Collectors.toList());
	}

	private List<HttpMessageConverter<?>> getHttpMessageConverters() {
		List<HttpMessageConverter<?>> converters = new ArrayList<>();

		ObjectMapper mapper = new ObjectMapper();
		mapper.registerModule(new Jackson2HalModule());
		mapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);

		MappingJackson2HttpMessageConverter converter = new MappingJackson2HttpMessageConverter();

		converter.setObjectMapper(mapper);
		converter.setSupportedMediaTypes(Arrays.asList(MediaTypes.HAL_JSON));
		converters.add(converter);

		return converters;
	}
}

class Reservation{
	private String name;

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}
	
	
	
}