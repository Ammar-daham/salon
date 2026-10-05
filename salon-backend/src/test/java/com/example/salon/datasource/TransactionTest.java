package com.example.salon.datasource;

import com.example.salon.dao.BusinessHoursDao;
import com.example.salon.model.OpeningInterval;
import com.example.salon.service.BusinessService;
import com.example.salon.support.IntegrationTest;
import org.junit.jupiter.api.Test;
import org.springframework.aop.support.AopUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

import java.time.DayOfWeek;
import java.time.LocalTime;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CountDownLatch;

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

	@Autowired
	private BusinessHoursDao businessHoursDao;

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
	void concurrentReplacementsOfASalonsHoursRunOneAfterTheOther() throws Exception
	{
		// Without the row lock in replaceHoursForBusiness, the second insert hits business_hours_no_overlap.
		TransactionTemplate tx = new TransactionTemplate(transactionManager);
		CountDownLatch firstHasWritten = new CountDownLatch(1);

		CompletableFuture<Void> first = CompletableFuture.runAsync(() -> tx.executeWithoutResult(status -> {
			businessHoursDao.replaceHoursForBusiness(Fixture.GLOW, List.of(mondayFrom(9)));
			firstHasWritten.countDown();
			try {
				Thread.sleep(500);
			} catch (InterruptedException ex) {
				throw new IllegalStateException(ex);
			}
		}));
		firstHasWritten.await();
		CompletableFuture<Void> second = CompletableFuture.runAsync(() -> tx.executeWithoutResult(status ->
				businessHoursDao.replaceHoursForBusiness(Fixture.GLOW, List.of(mondayFrom(10)))));

		first.get();
		second.get();
		assertThat(businessHoursDao.getHoursForBusiness(Fixture.GLOW)).containsExactly(mondayFrom(10));
	}

	private static OpeningInterval mondayFrom(int hour)
	{
		return new OpeningInterval(DayOfWeek.MONDAY, LocalTime.of(hour, 0), LocalTime.of(hour + 8, 0));
	}

	@Test
	void transactionalServicesAreProxied()
	{
		assertThat(AopUtils.isAopProxy(businessService)).isTrue();
	}
}
