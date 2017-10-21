package com.blog.samples.boot.config;

import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.cloud.aws.cache.config.annotation.CacheClusterConfig;
import org.springframework.cloud.aws.cache.config.annotation.EnableElastiCache;
import org.springframework.cloud.aws.context.config.annotation.EnableContextRegion;
import org.springframework.cloud.aws.jdbc.config.annotation.EnableRdsInstance;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.ImportResource;
import org.springframework.data.redis.cache.RedisCacheManager;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.connection.jedis.JedisConnectionFactory;
import org.springframework.data.redis.core.RedisTemplate;

//import com.amazonaws.services.elasticache.AmazonElastiCache;

@Configuration
@ImportResource("classpath:/aws-config.xml")
/*@EnableRdsInstance(databaseName = "${database-name:}", 
                   dbInstanceIdentifier = "${db-instance-identifier:}", 
				   password = "${rdsPassword:}")*/
@EnableRdsInstance(dbInstanceIdentifier = "mydevgeek", username="system", password = "system18")
@EnableContextRegion(region="us-east-2")
//@EnableElastiCache({@CacheClusterConfig(name = "myclustername",expiration=120)})
public class AwsResourceConfig {
}