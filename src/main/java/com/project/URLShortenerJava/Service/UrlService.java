package com.project.URLShortenerJava.Service;


import java.time.LocalDate;

import org.apache.commons.validator.GenericValidator;
import org.apache.commons.validator.routines.UrlValidator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.project.URLShortenerJava.Bean.RequestUrl;
import com.project.URLShortenerJava.Bean.Response;
import com.project.URLShortenerJava.Bean.UrlDto;
import com.project.URLShortenerJava.Bean.UrlReportDto;
import com.project.URLShortenerJava.Bean.UserDto;
import com.project.URLShortenerJava.Exception.DatabaseException;
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
	public Mono<Response> generateShortUrl(RequestUrl request) {
		return Mono.just(request)
					.filter(userData -> (!userData.getUrl().contains(domain) && UrlValidator.getInstance().isValid(userData.getUrl())))
					.switchIfEmpty(Mono.error(new InvalidLongUrlException("Please enter a valid Url")))
					.flatMap(userData -> mapRequestUrlToUserDto(userData))
					.flatMap(userDtoData -> {
						UrlDto urlDto = generateUrl(userDtoData);
						UrlReportDto urlReportDto = generateReport(urlDto);
						Mono<UrlDto> urlDtoResponse = saveToUrlRepository(urlDto);
						Mono<UrlReportDto> urlReportDtoResponse = saveToUrlReportRepository(urlReportDto);
						return Mono.zip(urlDtoResponse, urlReportDtoResponse)
						.map(zippedResponse -> zippedResponse.getT1())
						.map(AppUtil::urlDtoToResponse);
					})
					.onErrorResume(err -> Mono.error(new DatabaseException("Cannot Process Request. Please Try Again Later")));	
		
	}
	
	//Find UserId else Create UserId
	public Mono<UserDto> mapRequestUrlToUserDto(RequestUrl userData) {
		return Mono.just(userData)
				.flatMap(user -> {
					if(user.getUserId().isEmpty()) {
						return Mono.just(generateUserDto(user))
								.flatMap(userDto -> saveToUserRepository(userDto)
										.then(Mono.just(userDto)));
					} else {
						return Mono.just(generateUserDto(user))
								.flatMap(userDto -> checkUserIdAndLongUrl(userDto));
					}
						
				});
	}
	
	
	//Check UserId exists
	//Check LongUrl exists for the UserId
	public Mono<UserDto> checkUserIdAndLongUrl(UserDto userDto) {
		return Mono.just(userDto)
				.flatMap(userDtoData -> userRepository.findByUserId(userDto.getUserId())
						.switchIfEmpty(Mono.error(new InvalidUserIdException("Invalid UserId")))
						.then(Mono.just(userDto)))
				.flatMap(userDtoData -> urlRepository
						.findByUserIdAndLongUrl(userDto.getUserId(), userDto.getUrl())
						.map(AppUtil::urlEntityToDto)
						.flatMap(existingUrl -> Mono.error(new UrlAlreadyExistsException("Short Url Already Exists for this URL",existingUrl.getShortUrl())))
						.then(Mono.just(userDtoData)));
	}
	
	//Map requestUrl to UserDto
	//Generate UserId if it doesn't exist
	public UserDto generateUserDto(RequestUrl userData) {
		UserDto userDto = new UserDto();
		userDto.setUrl(userData.getUrl());
		if(userData.getUserId().isEmpty()) {
			userDto.setUserId(UserIdUtil.gen());
		} else {
			userDto.setUserId(userData.getUserId().get());
		}
		return userDto;
	}
	
	//Save to User Repository
	public Mono<UserDto> saveToUserRepository(UserDto userDto){
		return userRepository.save(AppUtil.userDtoToEntity(userDto))
				.map(AppUtil::userEntityToDto);
	}
	
	//Generate UrlDto
	public UrlDto generateUrl(UserDto userDtoData){
		return new UrlDto(domain + HashIdUtil.encode(),userDtoData.getUrl(),0L,userDtoData.getUserId());
	}
	
	//Delete from UrlRepository
	public Mono<Void> deleteFromUrlRepository(UrlDto urlDto){
		return urlRepository
				.delete(AppUtil.urlDtoToEntity(urlDto));
	}
	
	//Save to UrlRepository
	public Mono<UrlDto> saveToUrlRepository(UrlDto urlDto) {
		return urlRepository.save(AppUtil.urlDtoToEntity(urlDto))
				.map(AppUtil::urlEntityToDto);
	}
	
	//Save To UrlReportRepository
	public Mono<UrlReportDto> saveToUrlReportRepository(UrlReportDto urlReportDto) {
		return urlReportRepository.save(AppUtil.urlReportDtoToEntity(urlReportDto))
				.map(AppUtil::urlReportEntityToDto);
	}
	
	//Generate UrlReportDto
	public UrlReportDto generateReport(UrlDto urlDto) {
		return new UrlReportDto(urlDto.getCreateDate(),0L,urlDto.getShortUrl(),urlDto.getUserId());
	}
	
	//Get LongUrl from Database
	public Mono<String> getLongUrl(String shortUrl) {
		logger.info("getLongUrl Hit with shortUrl " + shortUrl);
		return Mono.just(shortUrl)
				.filter(url -> url.length() == 6)
				.switchIfEmpty(Mono.error(new ShortUrlNotFoundException("Invalid Short Url")))
				.flatMap(url -> urlRepository.findByShortUrl(domain + url)
						.map(AppUtil::urlEntityToDto))
				.switchIfEmpty(Mono.error(new ShortUrlNotFoundException("No Page Found")))
				.flatMap(urlDtoData -> {
					Mono<UrlDto> urlDto = updateUrl(urlDtoData);
					Mono<UrlReportDto> urlReportDto = updateElseCreateUrlReport(urlDtoData);
					return Mono.zip(urlDto,urlReportDto)
							.map(zippedResponse -> zippedResponse.getT1().getLongUrl());
				})
				.onErrorResume(err -> Mono.error(new DatabaseException("Cannot Process Request. Please Try Again Later")));
	}
	
	//Update Clicks in UrlDto for ShortUrl
	public Mono<UrlDto> updateUrl(UrlDto url){
		return Mono.just(url)
				.doOnNext(urlDtoData -> urlDtoData.setClicks(urlDtoData.getClicks() + 1))
				.flatMap(urlDtoData -> {
					if(urlDtoData.getClicks() < threshold) {
						return saveToUrlRepository(urlDtoData);
					} else {
						return deleteFromUrlRepository(urlDtoData).then(Mono.just(urlDtoData));
					}
				});
		 
	}
	
	//Update Clicks in UrlReportDto for ShortUrl for CurrentDate
	public Mono<UrlReportDto> updateElseCreateUrlReport(UrlDto urlData){
		return urlReportRepository
				.findByShortUrlAndFetchDate(urlData.getShortUrl(), LocalDate.now())
				.map(AppUtil::urlReportEntityToDto)
				.defaultIfEmpty(generateReport(urlData))
				.doOnNext(urlReportData -> urlReportData.setClicks(urlReportData.getClicks() + 1))
				.flatMap(urlReportData -> saveToUrlReportRepository(urlReportData));
	}
	
	
	
	//Get Report on Clicks for particular Date
	public Flux<UrlReportDto> getVisitedReportByDate(String date) {
		if(!GenericValidator.isDate(date,"yyyy-MM-dd",true))
			return Flux.error(new InvalidDateException(date + " is not a Valid Date Format"));
		else {
			return urlReportRepository
					.findByFetchDateAndClicksGreaterThan(LocalDate.parse(date),0)
					.map(AppUtil::urlReportEntityToDto)
					.switchIfEmpty(Mono.error(new NoDataExistsException("No data exists for " + date)));
		}
	}
	
	//Get Report on Clicks for All Date
	public Flux<UrlReportDto> getVisitedReportAll() {
		return urlReportRepository
				.findByClicksGreaterThan(0)
				.map(AppUtil::urlReportEntityToDto)
				.switchIfEmpty(Mono.error(new NoDataExistsException("No data exists")));
	}
	
	//Get Report on GeneratedUrls for particular Date
	public Flux<UrlReportDto> getGeneratedReportByDate(String date) {
		if(!GenericValidator.isDate(date,"yyyy-MM-dd",true))
			return Flux.error(new InvalidDateException(date + " is not a Valid Date Format"));
		else {
			return urlReportRepository
					.findByCreateDate(LocalDate.parse(date))
					.map(AppUtil::urlReportEntityToDto)
					.switchIfEmpty(Mono.error(new NoDataExistsException("No data exists for " + date)));
		}
	}
	
	//Get Report on GeneratedUrls for All Date
	public Flux<UrlReportDto> getGeneratedReportAll() {
		return urlReportRepository
				.findAll()
				.map(AppUtil::urlReportEntityToDto)
				.switchIfEmpty(Mono.error(new NoDataExistsException("No data exists")));
	}

}
