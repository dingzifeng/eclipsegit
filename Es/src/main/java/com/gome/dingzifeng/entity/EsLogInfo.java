package com.gome.dingzifeng.entity;

import java.util.Date;

import org.springframework.data.mongodb.core.mapping.Document;
@Document(collection="service_access_log")
public class EsLogInfo {
	private Date time;
	private Long count;
	private Integer avgDuration;
	public EsLogInfo() {
		super();
	}
	public EsLogInfo(Date time, Long count, Integer avgDuration) {
		super();
		this.time = time;
		this.count = count;
		this.avgDuration = avgDuration;
	}
	public Date getTime() {
		return time;
	}
	public void setTime(Date time) {
		this.time = time;
	}
	public Long getCount() {
		return count;
	}
	public void setCount(Long count) {
		this.count = count;
	}
	public Integer getAvgDuration() {
		return avgDuration;
	}
	public void setAvgDuration(Integer avgDuration) {
		this.avgDuration = avgDuration;
	}
	@Override
	public String toString() {
		return "EsLogInfo [time=" + time + ", count=" + count + ", avgDuration=" + avgDuration + "]";
	}
	
}
