package com.project.URLShortenerJava.Service;

import java.time.LocalDate;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;

import com.project.URLShortenerJava.Bean.UrlReportDto;
import com.project.URLShortenerJava.Bean.UrlReportEntity;
import com.project.URLShortenerJava.Exception.InvalidDateException;
import com.project.URLShortenerJava.Exception.NoDataExistsException;
import com.project.URLShortenerJava.Repository.UrlReportRepository;

import reactor.core.publisher.Flux;
import reactor.test.StepVerifier;

@ExtendWith(MockitoExtension.class)
public class UrlReportGetTest {
	
	@Mock
	private UrlReportRepository urlReportRepository;
	
	@InjectMocks
	private UrlService service;
	
	
	@Test
	public void getVisitedReportByDateTest() {
		
		String date = "2022-03-23";
		String mockUrl1 = "http://localhost:8080/abc";
		String mockUrl2 = "http://localhost:8080/bcd";
		
		Flux<UrlReportEntity> mockFlux = Flux.just(
				new UrlReportEntity("1", LocalDate.parse(date), LocalDate.parse(date), 0L, mockUrl1,"Milind"),
				new UrlReportEntity("2", LocalDate.parse(date), LocalDate.parse(date), 0L, mockUrl2,"Milind"));
		
		Mockito.when(urlReportRepository.findByFetchDateAndClicksGreaterThan(Mockito.any(LocalDate.class),Mockito.anyInt())).thenReturn(mockFlux);
		Flux<UrlReportDto> report = service.getVisitedReportByDate(date);
		
		StepVerifier.create(report)
		.expectNextCount(2)
		.verifyComplete();
	}
	
	@Test
	public void getVisitedReportInvalidDateTest() {
		
		String date = "20-03-23";
		
		Flux<UrlReportDto> report = service.getVisitedReportByDate(date)
				.doOnError(err -> System.out.println(err.getMessage()));
		
		StepVerifier.create(report)
		.verifyError(InvalidDateException.class);
	}
	
	@Test
	public void getVisitedReportWithoutDateTest() {
		
		String date = "2022-03-23";
		String mockUrl1 = "http://localhost:8080/abc";
		String mockUrl2 = "http://localhost:8080/bcd";
		
		Flux<UrlReportEntity> mockFlux = Flux.just(
				new UrlReportEntity("1", LocalDate.parse(date), LocalDate.parse(date), 0L, mockUrl1,"Milind"),
				new UrlReportEntity("2", LocalDate.parse(date), LocalDate.parse(date), 0L, mockUrl2,"Milind"));
		
		Mockito.when(urlReportRepository.findByClicksGreaterThan(Mockito.anyInt())).thenReturn(mockFlux);
		Flux<UrlReportDto> report = service.getVisitedReportAll();
		
		StepVerifier.create(report)
		.expectNextCount(2)
		.verifyComplete();
	}
	
	
	@Test
	public void getVisitedUrlWithDateNoDataExistsTest() {
		
		String date = "2022-03-23";
		
		Mockito.when(urlReportRepository.findByFetchDateAndClicksGreaterThan(Mockito.any(LocalDate.class),Mockito.anyInt())).thenReturn(Flux.empty());
		Flux<UrlReportDto> report = service.getVisitedReportByDate(date);
		
		StepVerifier.create(report)
		.verifyError(NoDataExistsException.class);
	}
	
	@Test
	public void getVisitedUrlWithoutDateNoDataExistsTest() {
		
		Mockito.when(urlReportRepository.findByClicksGreaterThan(Mockito.anyInt())).thenReturn(Flux.empty());
		Flux<UrlReportDto> report = service.getVisitedReportAll();
		
		StepVerifier.create(report)
		.verifyError(NoDataExistsException.class);
	}
	
	@Test
	public void getGeneratedReportByDateTest() {
		
		String date = "2022-03-23";
		String mockUrl1 = "http://localhost:8080/abc";
		String mockUrl2 = "http://localhost:8080/bcd";
		
		Flux<UrlReportEntity> mockFlux = Flux.just(
				new UrlReportEntity("1", LocalDate.parse(date), LocalDate.parse(date), 0L, mockUrl1,"Milind"),
				new UrlReportEntity("2", LocalDate.parse(date), LocalDate.parse(date), 0L, mockUrl2,"Milind"));
		
		Mockito.when(urlReportRepository.findByCreateDate(Mockito.any(LocalDate.class))).thenReturn(mockFlux);
		Flux<UrlReportDto> report = service.getGeneratedReportByDate(date);
		
		StepVerifier.create(report)
		.expectNextCount(2)
		.verifyComplete();
	}
	
	@Test
	public void getGeneratedReportInvalidDateTest() {
		
		String date = "20-03-23";
		
		Flux<UrlReportDto> report = service.getGeneratedReportByDate(date)
				.doOnError(err -> System.out.println(err.getMessage()));
		
		StepVerifier.create(report)
		.verifyError(InvalidDateException.class);
	}
	
	@Test
	public void getGeneratedReportWithoutDateTest() {
		
		String date = "2022-03-23";
		String mockUrl1 = "http://localhost:8080/abc";
		String mockUrl2 = "http://localhost:8080/bcd";
		
		Flux<UrlReportEntity> mockFlux = Flux.just(
				new UrlReportEntity("1", LocalDate.parse(date), LocalDate.parse(date), 0L, mockUrl1,"Milind"),
				new UrlReportEntity("2", LocalDate.parse(date), LocalDate.parse(date), 0L, mockUrl2,"Milind"));
		
		Mockito.when(urlReportRepository.findAll()).thenReturn(mockFlux);
		Flux<UrlReportDto> report = service.getGeneratedReportAll();
		
		StepVerifier.create(report)
		.expectNextCount(2)
		.verifyComplete();
	}
	
	
	@Test
	public void getGeneratedUrlWithDateNoDataExistsTest() {
		
		String date = "2022-03-23";
		
		Mockito.when(urlReportRepository.findByCreateDate(Mockito.any(LocalDate.class))).thenReturn(Flux.empty());
		Flux<UrlReportDto> report = service.getGeneratedReportByDate(date);
		
		StepVerifier.create(report)
		.verifyError(NoDataExistsException.class);
	}
	
	@Test
	public void getGeneratedUrlWithoutDateNoDataExistsTest() {
		
		Mockito.when(urlReportRepository.findAll()).thenReturn(Flux.empty());
		Flux<UrlReportDto> report = service.getGeneratedReportAll();
		
		StepVerifier.create(report)
		.verifyError(NoDataExistsException.class);
	}
	

}
