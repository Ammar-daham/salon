package com.example.salon.datasource;

import com.example.salon.service.BusinessService;
import com.example.salon.support.IntegrationTest;
import org.junit.jupiter.api.Test;
import org.springframework.aop.support.AopUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * The services' multi-row writes rely on @Transactional to be atomic. Guards against a dependency
 * change leaving no transaction manager, or one that isn't bound to the JdbcTemplate's DataSource.
 */
class TransactionTest extends IntegrationTest
{
	@Autowired
	private PlatformTransactionManager transactionManager;

	@Autowired
	private JdbcTemplate jdbcTemplate;

	@Autowired
	private BusinessService businessService;

	@Test
	void aFailedTransactionRollsBackJdbcWrites()
	{
		TransactionTemplate tx = new TransactionTemplate(transactionManager);

		assertThatThrownBy(() -> tx.executeWithoutResult(status -> {
			jdbcTemplate.update("UPDATE businesses SET description = 'rolled back?' WHERE id = ?", Fixture.GLOW);
			throw new IllegalStateException("abort");
		})).isInstanceOf(IllegalStateException.class);

		String description = jdbcTemplate.queryForObject(
				"SELECT description FROM businesses WHERE id = ?", String.class, Fixture.GLOW);
		assertThat(description).isEqualTo("Colour and precision cuts.");
	}

	@Test
	void transactionalServicesAreProxied()
	{
		assertThat(AopUtils.isAopProxy(businessService)).isTrue();
	}
}
