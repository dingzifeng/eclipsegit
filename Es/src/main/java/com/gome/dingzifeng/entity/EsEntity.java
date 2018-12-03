package com.gome.dingzifeng.entity;

import java.io.Serializable;
import java.util.Date;

import org.springframework.data.mongodb.core.mapping.Document;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Document(collection="service_access_log")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class EsEntity implements Serializable {
    /**  */
	private static final long serialVersionUID = 2796121813249250258L;
	
	private String _id;
	
    private String serviceName;

    private String apiName;

    private Long avgDuration;

    private Date time;

    private Long count;
}