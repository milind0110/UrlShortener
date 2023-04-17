package com.project.URLShortenerJava.Controller;

import java.net.URI;
import java.util.Optional;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.server.reactive.ServerHttpResponse;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.project.URLShortenerJava.Bean.RequestUrl;
import com.project.URLShortenerJava.Bean.UrlReportDto;
import com.project.URLShortenerJava.Bean.UserResponseDto;
import com.project.URLShortenerJava.Service.UrlService;
import com.project.URLShortenerJava.Util.AppUtil;

import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@RestController
public class UrlController {
	
	@Autowired
	private UrlService service;
	
	@PostMapping("/create")
	public Mono<ResponseEntity<UserResponseDto>> createUrl(@RequestBody RequestUrl request) {
		return service.generateShortUrl(request)
				.map(AppUtil::userResponseEntityToDto)
				.map(response -> ResponseEntity.status(HttpStatus.OK)
						.body(response));
	}
	
	@GetMapping("/{shortUrl}")
	public Mono<Void> getUrl(@PathVariable String shortUrl,ServerHttpResponse response) {
		return service.getLongUrl(shortUrl)
				.flatMap(longUrl -> {
					response.setStatusCode(HttpStatus.PERMANENT_REDIRECT);
			        response.getHeaders().setLocation(URI.create(longUrl));
			        return response.setComplete();
				});
	}
	
	@GetMapping("/report/visited")
	public Flux<UrlReportDto> geUrlReport(@RequestParam(required = false) Optional<String> date, @RequestParam(required = false) Optional<String> type) {
		return (date.isPresent() ? service.getVisitedReportByDate(date.get()) : service.getVisitedReportAll())
				.map(AppUtil::urlReportEntityToDto);
	}
	
	@GetMapping("/report/generated")
	public Flux<UrlReportDto> getGeneratedUrlReport(@RequestParam(required = false) Optional<String> date) {
		return (date.isPresent() ? service.getGeneratedReportByDate(date.get()) : service.getGeneratedReportAll())
				.map(AppUtil::urlReportEntityToDto);
	}

}
