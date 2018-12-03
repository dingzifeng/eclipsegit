package com.gome.dingzifeng;

import java.net.InetAddress;
import java.net.UnknownHostException;


import org.elasticsearch.client.transport.TransportClient;
import org.elasticsearch.common.settings.Settings;
import org.elasticsearch.common.transport.TransportAddress;
import org.elasticsearch.transport.client.PreBuiltTransportClient;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;




@Configuration
public class EsclientConfig {
	@Value("${selasticsearch.cluster-name}")
	private String clusterName;
	@Value("${elasticsearch.host}")
	private String host;
	@Value("${elasticsearch.host1}")
	private String host1;
	
	@Bean
	public TransportClient client() throws UnknownHostException {
		TransportAddress node = new TransportAddress(InetAddress.getByName(host),9300);
		//TransportAddress node1 = new TransportAddress(InetAddress.getByName(host1),9300);
		Settings settings = Settings.builder().put("cluster.name",clusterName).put("client.transport.sniff", true).build();
		TransportClient client = new PreBuiltTransportClient(settings);
		//client.addTransportAddress(node).addTransportAddress(node1);
		client.addTransportAddress(node);
		return client;
	}
}
