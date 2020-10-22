package com.github.damianwajser.rest.templates;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.PropertyNamingStrategy;
import com.fasterxml.jackson.datatype.jdk8.Jdk8Module;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.github.damianwajser.rest.configuration.TimeoutConfigurationProperties;
import com.github.damianwajser.rest.interceptors.RestTemplateInterceptor;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClientBuilder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.http.client.ClientHttpRequestFactory;
import org.springframework.http.client.ClientHttpRequestInterceptor;
import org.springframework.http.client.HttpComponentsClientHttpRequestFactory;
import org.springframework.http.converter.HttpMessageConverter;
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter;
import org.springframework.web.client.RestTemplate;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

@Configuration
public class RestTemplateConfiguration {

	@Autowired
	private TimeoutConfigurationProperties timeouts;

	@Bean
	@Primary
	@Qualifier("default_template")
	public RestTemplate restTemplate() {
		return getRestTemplate();
	}

	private RestTemplate getRestTemplate() {
		final RestTemplate restTemplate = new RestTemplate(getClientHttpRequestFactory());
		restTemplate.setInterceptors(getInterceptors());

		return restTemplate;
	}

	private ClientHttpRequestFactory getClientHttpRequestFactory() {
		final RequestConfig config = RequestConfig.custom()
				.setConnectTimeout(timeouts.getConnection())
				.setConnectionRequestTimeout(timeouts.getWrite())
				.setSocketTimeout(timeouts.getRead())
				.build();
		final CloseableHttpClient client = HttpClientBuilder
				.create()
				.setDefaultRequestConfig(config)
				.build();

		return new HttpComponentsClientHttpRequestFactory(client);
	}

	private List<ClientHttpRequestInterceptor> getInterceptors() {
		return Collections.singletonList(new RestTemplateInterceptor());
	}

	@Bean
	@Qualifier("snake_template")
	public RestTemplate restTemplateSnake() {
		RestTemplate restTemplate = getRestTemplate();

		List<HttpMessageConverter<?>> messageConverters = new ArrayList<>();
		MappingJackson2HttpMessageConverter jsonMessageConverter = new MappingJackson2HttpMessageConverter();
		ObjectMapper mapper = new ObjectMapper();
		mapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
		mapper.setPropertyNamingStrategy(PropertyNamingStrategy.SNAKE_CASE);
		mapper.registerModule(new Jdk8Module());
		mapper.registerModule(new JavaTimeModule());
		jsonMessageConverter.setObjectMapper(mapper);
		messageConverters.add(jsonMessageConverter);
		restTemplate.setMessageConverters(messageConverters);
		return restTemplate;
	}
}
