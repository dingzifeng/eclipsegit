package com.gome.dingzifeng.service;


import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import org.elasticsearch.action.admin.indices.exists.indices.IndicesExistsRequest;
import org.elasticsearch.action.admin.indices.exists.indices.IndicesExistsResponse;
import org.elasticsearch.action.search.SearchRequestBuilder;
import org.elasticsearch.client.transport.TransportClient;
import org.elasticsearch.index.query.QueryBuilders;
import org.elasticsearch.search.SearchHit;
import org.elasticsearch.search.SearchHits;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import com.gome.dingzifeng.dao.EsEntityDao;
import com.gome.dingzifeng.entity.EsEntity;

import lombok.extern.slf4j.Slf4j;

@Service
@Slf4j
public class EsScheduledService implements ApplicationRunner {
	
	private final int EsEntity_List_Size = 200;
	
	//定时器记录的基准时间
	private Date beginTime = new Date(new Date().getTime()/(60*1000)*60*1000);
	
	//该服务启动的时间
	private Date serviceBeginTime = new Date(new Date().getTime()/(60*1000)*60*1000);
	
	//上次记录的时间点 这里设置为 2018-11-25 08:00:00
	private Date oldDate = new Date(1543104000000L);
	
	//线程池创建
	private ExecutorService executor = Executors.newFixedThreadPool(Runtime.getRuntime().availableProcessors());
	
	// 与elasticsearch节点通信的client
	@Autowired
	private TransportClient client;
	
	//操作mongodb的接口
	@Autowired
	private EsEntityDao esEntityDao;
	
	/**
	 * <p>
	 * 	用于统计分析es中的接口访问日志信息，每分钟30秒执行，记录30秒之前的es数据（考虑到延时，30s后才读取）并将数据写入mongodb中
	 * </p>
	 * @author dingzifeng
	 * @Date 2018年11月22日
	 */
	@Scheduled(cron="30 * * * * *")
	public void esLoginfoStatistics() {
		Date date1 = new Date();
		//去除时间的毫秒部分
		Date date2 = new Date(((date1.getTime()-30*1000)/10000)*10000);
		Date endDate = new Date(beginTime.getTime()+60*1000);
		String index = "zipkin:span-".concat(DateFormat.getDateInstance().format(beginTime.getTime()-8*60*60*1000));
		//比当前时间小就执行操作
		while (endDate.compareTo(date2)<=0) {
			//判断索引存在吗
			try {
				if(!indexExists(index)) {
					beginTime = toNextDay(beginTime);
					return;
				}
			} catch (InterruptedException | ExecutionException e) {
				log.info("indexExists(index)函数抛出异常"+e);
				e.printStackTrace();
			}
			esLogMinuteWrite(beginTime);
			beginTime = endDate;
			endDate = new Date(beginTime.getTime()+60*1000);
		}
	}
	
	

	private Date toNextDay(Date time) {
		SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd");
		String s = sdf.format(time);
		try {
			Date date =  new Date(sdf.parse(s).getTime()+32*60*60*1000);
			return date;
		} catch (ParseException e) {
			log.error(e.getMessage(), e);
		}
		return time;
	}



	/**
	 * <p>
	 * 	用于判断es中是否存在索引 index
	 * </p>
	 * @author dingzifeng
	 * @Date 2018年11月22日
	 * @param index 索引名字
	 * @return 是否存在该索引
	 * @throws InterruptedException
	 * @throws ExecutionException
	 */
	public boolean indexExists(String index) throws InterruptedException, ExecutionException {
		IndicesExistsRequest indicesExistsRequest = new IndicesExistsRequest(index);
		IndicesExistsResponse indicesExistsResponse = client.admin().indices().exists(indicesExistsRequest).get();
		if(indicesExistsResponse.isExists()) {
			return true;
		}
		log.info("索引 ：{} 不存在", index);
		return false;
	}
	
	/**
	 * <p>
	 * 	读取es中时间点（beginTime）到参数date间的时间，该方法将es数据按时间按[beginTime,data)范围过滤，
	 * 时间段按分钟分组、嵌套serviceName（微服务名）分组、嵌套apiName（接口名分组）、聚合duration平均值
	 * </p>
	 * @author dingzifeng
	 * @Date 2018年11月23日
	 * @param date 限制时间
	 */
	public void esLogMinuteWrite(Date beginTime){
		String index = "zipkin:span-".concat(DateFormat.getDateInstance().format(beginTime.getTime()-8*60*60*1000));
		long t=System.currentTimeMillis();
		Date endTime = new Date(beginTime.getTime()+60*1000);
		//向es查询
		//BoolQueryBuilder boolQuery = QueryBuilders.boolQuery();
		SearchRequestBuilder searchResponse = this.client.prepareSearch(index)
				.setTypes("span")
				.setFetchSource(new String[] {"name","duration", "localEndpoint.serviceName", "tags.http.controller_path","timestamp_millis" }, null)
				.setPostFilter(QueryBuilders.rangeQuery("timestamp_millis").gte(beginTime.getTime()).lt(endTime.getTime()));
		if(searchResponse.get().getHits().getTotalHits()==0) {
			return;
		}
		long sums = searchResponse.get().getHits().totalHits;
		//多个分页的总结果集
		List<SearchHit> totalHits = new ArrayList<>();
		//合并各个分页结果集
		int sum = 0;
		while(sum<sums) {
			SearchHits hits = searchResponse.setFrom(sum).setSize(10000).get().getHits();
			log.info("数据返回用时"+(System.currentTimeMillis()-t));
			sum +=hits.getHits().length;
//			totalHits.addAll(Arrays.asList(hits.getHits()));
		}
		log.info("拿到数据返回用时"+(System.currentTimeMillis()-t));
		HashMap<String, HashMap<String, EsEntity>> serviceNameAndApiNameHashMap = new HashMap<>();
		int eslogcount = 0;
		int mongodbcount = 0;
		for (SearchHit searchHit : totalHits) {
			Map<String, Object> sourceAsMap = searchHit.getSourceAsMap();
			String apiName = (String) sourceAsMap.get("name");
			//判断apiName是否存在
			if(apiName != null) {
				Map<String, String> serviceNameMap = (Map<String, String>) sourceAsMap.get("localEndpoint");
				String serviceName = serviceNameMap.get("serviceName");
				HashMap<String, EsEntity> apiNameHashMap = serviceNameAndApiNameHashMap.get(serviceName);
				if(apiNameHashMap == null) {
					apiNameHashMap = new HashMap<>();
					serviceNameAndApiNameHashMap.put(serviceName, apiNameHashMap);
				}
				EsEntity esEntity = apiNameHashMap.get(apiName);
				if(esEntity == null) {
					esEntity = new EsEntity();
					esEntity.setServiceName(serviceName);
					esEntity.setApiName(apiName);
					esEntity.setTime(beginTime);
					esEntity.setCount(0L);
					esEntity.setAvgDuration(0L);
					apiNameHashMap.put(apiName, esEntity);
				}
				Integer duration =(Integer) sourceAsMap.get("duration");
				if (duration != null) {
					Long avgDuration = esEntity.getAvgDuration();
					avgDuration += duration.longValue()/1000;
					esEntity.setAvgDuration(avgDuration);
				}
				esEntity.setCount(esEntity.getCount()+1);
				eslogcount++;
			}
		}
		//将统计容器里的实体类取出存入esEntityList中写入mongodb
		List<EsEntity> esEntityList = new ArrayList<>(EsEntity_List_Size);
		Iterator<Entry<String, HashMap<String, EsEntity>>> iterator = serviceNameAndApiNameHashMap.entrySet().iterator();
		while(iterator.hasNext()) {
			Entry<String, HashMap<String, EsEntity>> next = iterator.next();
			Iterator<Entry<String, EsEntity>> iterator2 = next.getValue().entrySet().iterator();
			while(iterator2.hasNext()) {
				Entry<String, EsEntity> next2 = iterator2.next();
				EsEntity value = next2.getValue();
				value.setAvgDuration(value.getAvgDuration() / value.getCount().intValue());
				esEntityList.add(value);
				if (esEntityList.size() == EsEntity_List_Size) {
					esEntityDao.saveAll(esEntityList);
					esEntityList.clear();
					mongodbcount += EsEntity_List_Size;
				}

			}
		}
		if(!esEntityList.isEmpty()) {
			esEntityDao.saveAll(esEntityList);
			mongodbcount +=esEntityList.size();
		}
//		log.info("本次写入信息是从 "+beginTime+" 到 "+endTime+"之间 ，Es统计了 "+eslogcount+"条信息，数据库写入了 "+mongodbcount+"条记录,耗时 "+(new Date().getTime()-date.getTime())+"ms");
	}
	
	
	
	
	
	/**
	 * <p>
	 * 开始记录[beginTime,enndTime)的记录
	 * </p>
	 * @author dingzifeng
	 * @Date 2018年11月29日
	 * @param beginTime 记录数据的开始时间
	 * @param endTime 记录数据的结束时间
	 */
	public void esLogRangeWrite(Date beginTime,Date endTime) {
		int minute = 60*1000;
		for(long i=beginTime.getTime();i<endTime.getTime();i+=minute) {
			String index = "zipkin:span-".concat(DateFormat.getDateInstance().format(i-8*60*60*1000));
			try {
				if(!indexExists(index)) {
					i = toNextDay(new Date(i)).getTime();
					continue;
				}
			} catch (Exception e) {
				log.error(e.getMessage(), e);
			}
			esLogMinuteWrite(new Date(i));
		}
	}
	
	/**
	 * <p>
	 * 向线程池添加任务，统计上次统计时间点(oldate)到本次现在微服务启动时间(serviceBeginTime)间服务不在线的时间的es数据
	 * </p>
	 * @author dingzifeng
	 * @Date 2018年11月29日
	 * @param beginTime
	 * @param endTime
	 */
	public void writeOldLog(Date beginTime,Date endTime) {
		Date date = new Date();
		int hour = 60*60*1000;
		for(long i = beginTime.getTime();i<=endTime.getTime();i+=hour) {
			final long begin = i;
			long end = i+hour;
			if(end>endTime.getTime()) {
				end = endTime.getTime();
			}
			final long newend = end;
			executor.submit(new Runnable() {
				@Override
				public void run() {
					esLogRangeWrite(new Date(begin),new Date(newend));
				}
			});
		}
		executor.shutdown();  
        while (true) {  
            if (executor.isTerminated()) {  
                log.info("线程池里的所有任务都结束了,统计了上传记录点"+oldDate+"到本次服务启动时间"+serviceBeginTime+"的数据"
                		+"耗时"+(new Date().getTime()-date.getTime()));
                break;  
            }  
            try {
            	//减少while(true)执行次数
				Thread.sleep(5000);
			} catch (InterruptedException e) {
				log.error(e.getMessage(), e);
			}  
        }  
	}

	@Override
	public void run(ApplicationArguments args) throws Exception {
//		if(oldDate.compareTo(serviceBeginTime)<0) {
//			log.info("启动线程池, ");
//			writeOldLog(oldDate, new Date(1543190400000L));
//		}
	}
}
