package com.project.URLShortenerJava.Service;


import java.time.LocalDate;

import org.apache.commons.validator.GenericValidator;
import org.apache.commons.validator.routines.UrlValidator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.project.URLShortenerJava.Bean.RequestUrl;
import com.project.URLShortenerJava.Bean.UrlEntity;
import com.project.URLShortenerJava.Bean.UrlReportEntity;
import com.project.URLShortenerJava.Bean.UserResponseEntity;
import com.project.URLShortenerJava.Exception.InvalidDateException;
import com.project.URLShortenerJava.Exception.InvalidLongUrlException;
import com.project.URLShortenerJava.Exception.InvalidUserIdException;
import com.project.URLShortenerJava.Exception.NoDataExistsException;
import com.project.URLShortenerJava.Exception.ShortUrlNotFoundException;
import com.project.URLShortenerJava.Exception.UrlAlreadyExistsException;
import com.project.URLShortenerJava.Repository.UrlReportRepository;
import com.project.URLShortenerJava.Repository.UrlRepository;
import com.project.URLShortenerJava.Repository.UserRepository;
import com.project.URLShortenerJava.Util.AppUtil;
import com.project.URLShortenerJava.Util.HashIdUtil;
import com.project.URLShortenerJava.Util.UserIdUtil;

import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@Service
public class UrlService {
	
//	@Value("${application.domain}")
	private String domain = "http://localhost:8080/";
//	@Value("${application.threshold}")
	private Long threshold = 3L;
	@Autowired
	private UrlRepository urlRepository;
	@Autowired
	private UrlReportRepository urlReportRepository;
	@Autowired
	private UserRepository userRepository;
	
	Logger logger = LoggerFactory.getLogger(UrlService.class);
	
	
	//Generate ShortUrl for LongUrl
	public Mono<UserResponseEntity> generateShortUrl(RequestUrl request) {
		return Mono.fromSupplier(() -> request)
					.flatMap(userData -> checkValid(userData))
					.flatMap(userData -> mapRequestUrlToUserResponse(userData))
					.flatMap(userEntityData -> {
						UrlEntity urlEntity = generateUrl(userEntityData);
						UrlReportEntity urlReportEntity = generateReport(urlEntity);
						Mono<UrlEntity> urlEntityResponse = saveToUrlRepository(urlEntity);
						Mono<UrlReportEntity> urlReportEntityResponse = saveToUrlReportRepository(urlReportEntity);
						return Mono.zip(urlEntityResponse, urlReportEntityResponse)
						.map(zippedResponse -> zippedResponse.getT1())
						.map(AppUtil::urlEntityToResponse);
					});
		
	}
	
	private Mono<RequestUrl> checkValid(RequestUrl request){
		return Mono.fromSupplier(() -> request)
				.filter(userData -> UrlValidator.getInstance().isValid(userData.getUrl()))
				.filter(userData -> (!userData.getUrl().contains(domain)))
				.switchIfEmpty(Mono.error(new InvalidLongUrlException("Please enter a valid Url")))
				.filter(userData -> userData.getUserId() != null)
				.switchIfEmpty(Mono.error(new InvalidUserIdException("Please enter a valid UserId")));
	}
	
	//Find UserId else Create UserId
	private Mono<UserResponseEntity> mapRequestUrlToUserResponse(RequestUrl userData) { 
		return Mono.fromSupplier(() -> userData)
				.flatMap(user -> {
					if(user.getUserId().isBlank()) {
						return Mono.fromSupplier(() -> userData)
								.map(userResponse -> generateUserResponse(userResponse))
								.doOnNext(response -> userRepository.save(AppUtil.userResponseEntitytoEntity(response)).subscribe());
								
					} else {
						return Mono.fromSupplier(() -> generateUserResponse(user))
								.flatMap(response -> checkUserIdAndLongUrl(response));
					}
							
				});
	}
	
	
	//Check UserId exists
	//Check LongUrl exists for the UserId
	private Mono<UserResponseEntity> checkUserIdAndLongUrl(UserResponseEntity response) {
		return Mono.fromSupplier(() -> response)
				.flatMap(userEntity -> userRepository.findByUserId(userEntity.getUserId())
						.switchIfEmpty(Mono.error(new InvalidUserIdException("Invalid UserId")))
						.then(Mono.fromSupplier(() -> userEntity)))
				.flatMap(userEntity -> urlRepository
						.findByUserIdAndLongUrl(userEntity.getUserId(), userEntity.getUrl())
						.flatMap(existingUrl -> Mono.error(new UrlAlreadyExistsException("Short Url Already Exists for this URL",existingUrl.getShortUrl())))
						.then(Mono.fromSupplier(() -> userEntity)));
	}
	
	//Map requestUrl to UserEntity
	//Generate UserId if it doesn't exist
	private UserResponseEntity generateUserResponse(RequestUrl userData) {
		UserResponseEntity response = new UserResponseEntity();
		response.setUrl(userData.getUrl());
		if(userData.getUserId().isBlank()) {
			response.setUserId(UserIdUtil.gen());
		} else {
			response.setUserId(userData.getUserId());
		}
		return response;
	}
	
	//Generate UrlEntity
	private UrlEntity generateUrl(UserResponseEntity userEntityData){
		return new UrlEntity(domain + HashIdUtil.encode(),userEntityData.getUrl(),0L,LocalDate.now(),userEntityData.getUserId(),true);
	}
	
	//Delete from UrlRepository
	private Mono<UrlEntity> deleteFromUrlRepository(UrlEntity urlEntity){
		urlEntity.setIsActive(false);
		return urlRepository.save(urlEntity);
	}
	
	//Save to UrlRepository
	private Mono<UrlEntity> saveToUrlRepository(UrlEntity urlEntity) {
		return urlRepository.save(urlEntity);
	}
	
	//Save To UrlReportRepository
	private Mono<UrlReportEntity> saveToUrlReportRepository(UrlReportEntity urlReportEntity) {
		return urlReportRepository.save(urlReportEntity);
	}
	
	//Generate UrlReportEntity
	private UrlReportEntity generateReport(UrlEntity urlEntity) {
		return new UrlReportEntity(urlEntity.getCreateDate(),LocalDate.now(),0L,urlEntity.getShortUrl(),urlEntity.getUserId());
	}
	
	//Get LongUrl from Database
	public Mono<String> getLongUrl(String shortUrl) {
		logger.info("getLongUrl Hit with shortUrl " + shortUrl);
		return Mono.fromSupplier(() -> shortUrl)
				.filter(url -> url.length() == 6)
				.switchIfEmpty(Mono.error(new ShortUrlNotFoundException("Invalid Short Url")))
				.flatMap(url -> urlRepository.findByShortUrl(domain + url))
				.switchIfEmpty(Mono.error(new ShortUrlNotFoundException("No Page Found")))
				.flatMap(urlEntityData -> {
					Mono<UrlEntity> urlEntity = updateUrl(urlEntityData);
					Mono<UrlReportEntity> urlReportEntity = updateElseCreateUrlReport(urlEntityData);
					return Mono.zip(urlEntity,urlReportEntity)
							.map(zippedResponse -> zippedResponse.getT1().getLongUrl());
				});
	}
	
	//Update Clicks in UrlEntity for ShortUrl
	private Mono<UrlEntity> updateUrl(UrlEntity url){
		return Mono.fromSupplier(() -> url)
				.doOnNext(urlEntityData -> urlEntityData.setClicks(urlEntityData.getClicks() + 1))
				.flatMap(urlEntityData -> {
					if(urlEntityData.getClicks() < threshold) {
						return saveToUrlRepository(urlEntityData);
					} else {
						return deleteFromUrlRepository(urlEntityData);
					}
				});
		 
	}
	
	//Update Clicks in UrlReportEntity for ShortUrl for CurrentDate
	private Mono<UrlReportEntity> updateElseCreateUrlReport(UrlEntity urlData){
		return urlReportRepository
				.findByShortUrlAndFetchDate(urlData.getShortUrl(), LocalDate.now())
				.defaultIfEmpty(generateReport(urlData))
				.doOnNext(urlReportData -> urlReportData.setClicks(urlReportData.getClicks() + 1))
				.flatMap(urlReportData -> saveToUrlReportRepository(urlReportData));
	}
	
	
	
	//Get Report on Clicks for particular Date
	public Flux<UrlReportEntity> getVisitedReportByDate(String queryDate) {
		return Mono.fromSupplier(() -> queryDate)
				.filter(date -> GenericValidator.isDate(date,"yyyy-MM-dd",true))
				.switchIfEmpty(Mono.error(new InvalidDateException(queryDate + " is not a Valid Date Format")))
				.flux()
				.flatMap(date -> urlReportRepository.findByFetchDateAndClicksGreaterThan(LocalDate.parse(date),0))
				.switchIfEmpty(Flux.error(new NoDataExistsException("No data exists for " + queryDate)));
	}
	
	//Get Report on Clicks for All Date
	public Flux<UrlReportEntity> getVisitedReportAll() {
		return urlReportRepository
				.findByClicksGreaterThan(0)
				.switchIfEmpty(Mono.error(new NoDataExistsException("No data exists")));
	}
	
	//Get Report on GeneratedUrls for particular Date
	public  Flux<UrlReportEntity> getGeneratedReportByDate(String queryDate) {
		return Mono.fromSupplier(() -> queryDate)
				.filter(date -> GenericValidator.isDate(date,"yyyy-MM-dd",true))
				.switchIfEmpty(Mono.error(new InvalidDateException(queryDate + " is not a Valid Date Format")))
				.flux()
				.flatMap(date -> urlReportRepository.findByCreateDate(LocalDate.parse(date)))
				.switchIfEmpty(Flux.error(new NoDataExistsException("No data exists for " + queryDate)));
	}
	
	//Get Report on GeneratedUrls for All Date
	public  Flux<UrlReportEntity> getGeneratedReportAll() {
		return urlReportRepository
				.findAll()
				.switchIfEmpty(Mono.error(new NoDataExistsException("No data exists")));
	}

}
