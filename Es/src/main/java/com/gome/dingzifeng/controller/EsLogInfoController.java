package com.gome.dingzifeng.controller;

import java.util.Date;
import java.util.List;
import java.util.concurrent.ExecutionException;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.gome.dingzifeng.dao.EsEntityDao;
import com.gome.dingzifeng.entity.EsLogInfo;
import com.gome.dingzifeng.service.EsScheduledService;

@RestController
@RequestMapping("es")
public class EsLogInfoController {
	@Autowired
	private EsScheduledService esScheduledService;
	@Autowired
	private EsEntityDao esEntityDao;

	@RequestMapping("esLogWrite")
	public void esLogWrite() {
		esScheduledService.esLogMinuteWrite(new Date(1543104000000L));
	}
	
	@RequestMapping("getserviceNames")
	public List<String> getserviceNames() {
		return esEntityDao.selectServiceName();
	}
	@RequestMapping("getapiName")
	public List<String> getapiName(@RequestParam(name="serviceName") String serviceName) {
		return esEntityDao.selectapiNameByServiceName(serviceName);
	}
	@RequestMapping("show")
	public List<EsLogInfo> show(
			@RequestParam(name="serviceName") String serviceName,
			@RequestParam(name="apiName",required=false) String apiName,
			@RequestParam(name="beginTime") Date beginTime,
			@RequestParam(name="endTime") Date endTime,
			@RequestParam(name="timelevel") Long timelevel){
	   Date date = new Date();
	   List<EsLogInfo> showEsLogInfo = esEntityDao.showEsLogInfo(serviceName, apiName, beginTime, endTime);
	   System.out.println(new Date().getTime()-date.getTime());
	   return showEsLogInfo;
	}
	@RequestMapping("showWithtimelevel")
	public List<EsLogInfo> showWithtimelevel(
			@RequestParam(name="serviceName") String serviceName,
			@RequestParam(name="apiName",required=false) String apiName,
			@RequestParam(name="beginTime") Date beginTime,
			@RequestParam(name="endTime") Date endTime,
			@RequestParam(name="timelevel",required=false) Integer timelevel){
		Date date = new Date();
	   List<EsLogInfo> showEsLogInfo = esEntityDao.showEsLogInfoByTimelevel(serviceName, apiName, beginTime, endTime, timelevel);
	   System.err.println(new Date().getTime()-date.getTime());
	   return showEsLogInfo;
	}
	@RequestMapping("writeRangeLog")
	public void writeRangeLog(
			@RequestParam(name="beginTime") Date beginTime,
			@RequestParam(name="endTime") Date endTime) {
		esScheduledService.esLogRangeWrite(beginTime, endTime);
	}
	@RequestMapping("indexExists")
	public boolean indexExists(@RequestParam(name="index") String index) {
		try {
			return esScheduledService.indexExists(index);
		} catch (InterruptedException | ExecutionException e) {
			e.printStackTrace();
		}
		return false;
	}
}
