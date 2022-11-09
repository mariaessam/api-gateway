package com.cit.vericash.api.gateway;

import com.cit.vericash.api.gateway.filter.PostFilter;
import com.cit.vericash.api.gateway.filter.PreFilter;
import com.netflix.config.ConfigurationManager;
import io.undertow.util.CopyOnWriteMap;
import okhttp3.OkHttpClient;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.boot.autoconfigure.security.SecurityAutoConfiguration;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.cloud.client.loadbalancer.LoadBalanced;
import org.springframework.cloud.netflix.feign.EnableFeignClients;
import org.springframework.cloud.netflix.zuul.EnableZuulProxy;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.PropertySource;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;
import org.springframework.web.filter.CorsFilter;

import javax.annotation.PostConstruct;
import java.util.LinkedList;
import java.util.concurrent.TimeUnit;

@SpringBootApplication
@EnableZuulProxy
@EnableFeignClients
@PropertySource({"file:${PORTAL_CONFIG_HOME}/${PROJECT}-config/services/shared-config/entity-models.properties"})
@EntityScan(basePackages = {"${vericash.entity.scan.packages}"})
@ComponentScan(basePackages = {"com.cit"})
@EnableAutoConfiguration(exclude = { SecurityAutoConfiguration.class })
public class APIGatewayApplication {

	public static void main(String[] args) {

		SpringApplication.run(APIGatewayApplication.class, args);
	}

	@Bean
	public PostFilter postFilter() {
		return new PostFilter();
	}

	@Bean
	public PreFilter preFilter() {
		return new PreFilter();
	}


	@Bean
	public CorsFilter corsFilter() {
		final UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
		final CorsConfiguration config = new CorsConfiguration();
		config.setAllowCredentials(true);
		config.addAllowedOrigin("*");
		config.addAllowedHeader("*");
		config.addAllowedMethod("OPTIONS");
		config.addAllowedMethod("HEAD");
		config.addAllowedMethod("GET");
		config.addAllowedMethod("PUT");
		config.addAllowedMethod("POST");
		config.addAllowedMethod("DELETE");
		config.addAllowedMethod("PATCH");
		source.registerCorsConfiguration("/**", config);
		return new CorsFilter(source);
	}
	@PostConstruct
	void disableHystrix() {
		ConfigurationManager.getConfigInstance().setProperty("hystrix.command.default.circuitBreaker.enabled", false);
		ConfigurationManager.getConfigInstance().setProperty("hystrix.command.default.execution.timeout.enabled", false);
		ConfigurationManager.getConfigInstance().setProperty("hystrix.command.default.execution.isolation.thread.interruptOnTimeout", false);
		ConfigurationManager.getConfigInstance().setProperty("hystrix.command.default.execution.isolation.thread.timeoutInMilliseconds", "9999999999999999");
		ConfigurationManager.getConfigInstance().setProperty("ribbon.ReadTimeout", "500000");
	}

	@Bean
	public OkHttpClient okHttpClient() {
		return  new OkHttpClient.Builder()
				.connectTimeout(5, TimeUnit.MINUTES)
				.writeTimeout(5, TimeUnit.MINUTES)
				.readTimeout(5, TimeUnit.MINUTES)
				.build();
	}
	@Bean
	@LoadBalanced
	public RestTemplate restTemplate(RestTemplateBuilder builder) {
		return builder.build();
	}

}
