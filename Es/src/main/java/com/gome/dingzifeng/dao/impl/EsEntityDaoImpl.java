package com.gome.dingzifeng.dao.impl;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.aggregation.Aggregation;
import org.springframework.data.mongodb.core.aggregation.AggregationResults;
import org.springframework.data.mongodb.core.aggregation.GroupOperation;
import org.springframework.data.mongodb.core.aggregation.ProjectionOperation;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.stereotype.Component;

import com.gome.dingzifeng.dao.EsEntityDao;
import com.gome.dingzifeng.entity.EsEntity;
import com.gome.dingzifeng.entity.EsLogInfo;
@Component
public class EsEntityDaoImpl implements EsEntityDao {
	@Autowired
	private MongoTemplate mongoTemplate;
	@Override
	public void saveAll(List<EsEntity> list) {
		mongoTemplate.insertAll(list);
	}
	@Override
	public List<String> selectServiceName() {
		List<String> findDistinct = mongoTemplate.findDistinct("serviceName", EsEntity.class, String.class);
		return findDistinct;
	}
	
	@Override
	public List<EsLogInfo> showEsLogInfo(String serviceName, String apiName, Date beginTime, Date endTime) {
		Query query = new Query();
		if (apiName==null) {
			query.addCriteria(Criteria.where("serviceName").is(serviceName)
					.and("time").gte(beginTime).lte(endTime));
		} else {
			query.addCriteria(Criteria.where("serviceName").is(serviceName)
					.and("apiName").is(apiName)
					.and("time").gte(beginTime).lte(endTime));
		}
		List<EsLogInfo> find = mongoTemplate.find(query, EsLogInfo.class);
		System.out.println(find.size());
		return find;
	}
	@Override
	public List<String> selectapiNameByServiceName(String serviceName) {
		Query query = new Query();
		query.addCriteria(Criteria.where("serviceName").is(serviceName));
		List<String> findDistinct = mongoTemplate.findDistinct(query, "apiName", EsEntity.class, String.class);
		return findDistinct;
	}
	@Override
	public List<EsLogInfo> showEsLogInfoByTimelevel(String serviceName, String apiName, Date beginTime, Date endTime,
			Integer timelevel) {
		List<EsLogInfo> esloglist = new ArrayList<>();
		Criteria criteria = null;
		if (apiName != null || "".equals(apiName)) {
			criteria = Criteria.where("serviceName").is(serviceName).and("apiName").is(apiName).and("time")
					.gte(beginTime).lt(endTime);
		} else {
			criteria = Criteria.where("serviceName").is(serviceName).and("time").gte(beginTime).lt(endTime);
		}
		
		ProjectionOperation project = null;
		GroupOperation group = null;
		if(timelevel == 1 || timelevel == null) {
			project = Aggregation.project("time","avgDuration","count");
			group = Aggregation.group("time").first("time").as("time").sum("count").as("count").avg("avgDuration")
					.as("avgDuration");
		} else if(timelevel ==2) {
			project = Aggregation.project("time","avgDuration","count")
					.andExpression("hour(add(time," +  8 * 60 * 60 * 1000 + "))").as("hour")
					.andExpression("dayOfYear(add(time," +  8 * 60 * 60 * 1000 + "))").as("day")
					.andExpression("year(add(time," +  8 * 60 * 60 * 1000 + "))").as("year");
			group = Aggregation.group("year","day","hour").first("time").as("time").sum("count").as("count").avg("avgDuration")
					.as("avgDuration");
		} else if(timelevel ==  3){
			project = Aggregation.project("time","avgDuration","count")
					.andExpression("dayOfYear(add(time," +  8 * 60 * 60 * 1000 + "))").as("day")
					.andExpression("year(add(time," +  8 * 60 * 60 * 1000 + "))").as("year");
			group = Aggregation.group("year","day").first("time").as("time").sum("count").as("count").avg("avgDuration")
					.as("avgDuration");
		} else if(timelevel == 4){
			project = Aggregation.project("time","avgDuration","count")
					.andExpression("week(add(time," +  8 * 60 * 60 * 1000 + "))").as("week")
					.andExpression("year(add(time," +  8 * 60 * 60 * 1000 + "))").as("year");
			group = Aggregation.group("year","week").first("time").as("time").sum("count").as("count").avg("avgDuration")
					.as("avgDuration");
		} else if(timelevel == 5) {
			project = Aggregation.project("time","avgDuration","count")
					.andExpression("month(add(time," +  8 * 60 * 60 * 1000 + "))").as("month")
					.andExpression("year(add(time," +  8 * 60 * 60 * 1000 + "))").as("year");
			group = Aggregation.group("year","month").first("time").as("time").sum("count").as("count").avg("avgDuration")
					.as("avgDuration");
		} else if(timelevel == 6) {
			project = Aggregation.project("time","avgDuration","count")
					.andExpression("year(add(time," +  8 * 60 * 60 * 1000 + "))").as("year");
			group = Aggregation.group("year").first("time").as("time").sum("count").as("count").avg("avgDuration")
					.as("avgDuration");
		}
		
		Aggregation aggregation = Aggregation.newAggregation(
				Aggregation.match(criteria),
				project,
				group);
		AggregationResults<EsLogInfo> aggregate = mongoTemplate.aggregate(aggregation, "service_access_log",
				EsLogInfo.class);
		esloglist.addAll(aggregate.getMappedResults());
		return esloglist;
	}
	
}
