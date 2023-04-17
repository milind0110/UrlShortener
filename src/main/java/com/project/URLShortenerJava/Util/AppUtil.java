package com.project.URLShortenerJava.Util;

import org.springframework.beans.BeanUtils;

import com.project.URLShortenerJava.Bean.RequestUrl;
import com.project.URLShortenerJava.Bean.UrlEntity;
import com.project.URLShortenerJava.Bean.UrlReportDto;
import com.project.URLShortenerJava.Bean.UrlReportEntity;
import com.project.URLShortenerJava.Bean.UserEntity;
import com.project.URLShortenerJava.Bean.UserResponseDto;
import com.project.URLShortenerJava.Bean.UserResponseEntity;

public class AppUtil {

	public static UrlReportDto urlReportEntityToDto(UrlReportEntity entity) {
		UrlReportDto dto = new UrlReportDto();
		BeanUtils.copyProperties(entity,dto);
		return dto;
	}
	
	public static UrlReportEntity urlReportDtoToEntity(UrlReportDto dto) {
		UrlReportEntity entity = new UrlReportEntity();
		BeanUtils.copyProperties(dto,entity);
		return entity;
	}
	
	public static UserResponseDto userResponseEntityToDto(UserResponseEntity entity) {
		UserResponseDto dto = new UserResponseDto();
		BeanUtils.copyProperties(entity,dto);
		return dto;
	}
	
	public static UserResponseEntity userResponseDtoToEntity(UserResponseDto dto) {
		UserResponseEntity entity = new UserResponseEntity();
		BeanUtils.copyProperties(dto,entity);
		return entity;
	}
	
	public static UserEntity userResponseEntitytoEntity(UserResponseEntity userEntity) {
		UserEntity entity = new UserEntity();
		BeanUtils.copyProperties(userEntity,entity);
		return entity;
	}
	
	public static UserResponseEntity urlEntityToResponse(UrlEntity entity) {
		UserResponseEntity response = new UserResponseEntity();
		response.setUrl(entity.getShortUrl());
		response.setUserId(entity.getUserId());
		return response;
	}
	
	public static UserResponseEntity requestToUserResponseEntity(RequestUrl request) {
		UserResponseEntity response = new UserResponseEntity();
		BeanUtils.copyProperties(request,response);
		return response;
	}
	
	
	
}
