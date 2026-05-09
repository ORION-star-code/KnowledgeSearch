package com.knowledge.search.config;

import javax.sql.DataSource;
import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.context.annotation.Configuration;

@Configuration
@ConditionalOnBean(DataSource.class)
@MapperScan("com.knowledge.search.mapper")
public class MapperConfig {
}
