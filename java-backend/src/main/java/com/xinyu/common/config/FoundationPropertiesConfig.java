package com.xinyu.common.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.PropertySource;

@Configuration(proxyBeanMethods = false)
@PropertySource("classpath:application-foundation.properties")
public class FoundationPropertiesConfig {
}
