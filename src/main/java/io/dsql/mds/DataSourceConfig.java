package io.dsql.mds;

import javax.sql.DataSource;

import io.dsql.mds.DataSourceInterceptor.DataSourceContextHolder;
import jakarta.persistence.EntityManagerFactory;
import java.util.HashMap;
import java.util.Map;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.jdbc.DataSourceBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.jdbc.datasource.lookup.AbstractRoutingDataSource;
import org.springframework.orm.jpa.JpaTransactionManager;
import org.springframework.orm.jpa.LocalContainerEntityManagerFactoryBean;
import org.springframework.orm.jpa.vendor.HibernateJpaVendorAdapter;

import static io.dsql.mds.DataSourceInterceptor.DataSourceContextHolder.READ_ONLY;
import static io.dsql.mds.DataSourceInterceptor.DataSourceContextHolder.READ_WRITE;

@Configuration
public class DataSourceConfig {

	private DataSource dataSource() {
		return DataSourceBuilder.create().build();
	}

	@Bean
	@ConfigurationProperties(prefix = "spring.datasource.write")
	public DataSource writeDataSource() {
		return dataSource();
	}

	@Bean
	@ConfigurationProperties(prefix = "spring.datasource.read")
	public DataSource readDataSource() {
		return dataSource();
	}

	@Bean
	public DataSource routingDataSource() {
		RoutingDataSource routingDataSource = new RoutingDataSource();
		Map<Object, Object> targetDataSources = new HashMap<>();
		targetDataSources.put(READ_ONLY, readDataSource());
		targetDataSources.put(READ_WRITE, writeDataSource());

		routingDataSource.setTargetDataSources(targetDataSources);
		routingDataSource.setDefaultTargetDataSource(readDataSource());
		return routingDataSource;
	}

	@Primary
	@Bean
	public LocalContainerEntityManagerFactoryBean entityManagerFactory(
			@Qualifier("routingDataSource") DataSource dataSource) {
		LocalContainerEntityManagerFactoryBean entityManagerFactoryBean = new LocalContainerEntityManagerFactoryBean();
		entityManagerFactoryBean.setDataSource(dataSource);
		entityManagerFactoryBean.setPackagesToScan("io.dsql.mds");
		entityManagerFactoryBean.setJpaVendorAdapter(new HibernateJpaVendorAdapter());
		return entityManagerFactoryBean;
	}

	@Primary
	@Bean
	public JpaTransactionManager transactionManager(EntityManagerFactory entityManagerFactory) {
		return new JpaTransactionManager(entityManagerFactory);
	}

	static class RoutingDataSource extends AbstractRoutingDataSource {
		@Override
		protected Object determineCurrentLookupKey() {
			return DataSourceContextHolder.getDataSourceType();
		}
	}
}

