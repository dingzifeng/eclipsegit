package com.gome.dingzifeng.dao;

import java.util.Date;
import java.util.List;

import com.gome.dingzifeng.entity.EsEntity;
import com.gome.dingzifeng.entity.EsLogInfo;

public interface EsEntityDao {
	public void saveAll(List<EsEntity> list);
	
	public List<String> selectServiceName();
	
	public List<String> selectapiNameByServiceName(String serviceName);
	
	public List<EsLogInfo> showEsLogInfo(String serviceName,String apiName,Date beginTime,Date endTime);
	
	public List<EsLogInfo> showEsLogInfoByTimelevel(String serviceName,String apiName,Date beginTime,Date endTime,Integer timelevel);
}
