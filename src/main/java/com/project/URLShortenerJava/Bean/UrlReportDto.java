package com.project.URLShortenerJava.Bean;

import java.time.LocalDate;

public class UrlReportDto {
	private LocalDate fetchDate;
	private LocalDate createDate;
	private Long clicks;
	private String shortUrl;
	private String userId;

	public UrlReportDto() {
		super();
	}

	public UrlReportDto(LocalDate fetchDate, Long clicks, String shortUrl, String userId) {
		super();
		this.fetchDate = fetchDate;
		this.createDate = LocalDate.now();
		this.clicks = clicks;
		this.shortUrl = shortUrl;
		this.userId = userId;
	}

	public String getUserId() {
		return userId;
	}

	public void setUserId(String userId) {
		this.userId = userId;
	}

	public LocalDate getFetchDate() {
		return fetchDate;
	}

	public void setFetchDate(LocalDate fetchDate) {
		this.fetchDate = fetchDate;
	}

	public LocalDate getCreateDate() {
		return createDate;
	}

	public void setCreateDate(LocalDate createDate) {
		this.createDate = createDate;
	}

	public Long getClicks() {
		return clicks;
	}

	public void setClicks(Long clicks) {
		this.clicks = clicks;
	}

	public String getShortUrl() {
		return shortUrl;
	}

	public void setShortUrl(String shortUrl) {
		this.shortUrl = shortUrl;
	}

	@Override
	public String toString() {
		return "UrlReportDto [fetchDate=" + fetchDate + ", createDate=" + createDate + ", clicks=" + clicks
				+ ", shortUrl=" + shortUrl + ", userId=" + userId + "]";
	}
}
