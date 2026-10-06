package com.example.salon.datasource;

import com.example.salon.dao.BusinessHoursDao;
import com.example.salon.model.AppointmentStatus;
import com.example.salon.model.OpeningInterval;
import com.example.salon.model.Role;
import com.example.salon.model.User;
import com.example.salon.security.AuthenticatedUser;
import com.example.salon.service.AppointmentService;
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

	@Autowired
	private AppointmentService appointmentService;

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

	@Test
	void concurrentStatusChangesToOneAppointmentRunOneAfterTheOther() throws Exception
	{
		// Without the row lock in AppointmentService, completing still sees BOOKED and overwrites the cancellation.
		long id = jdbcTemplate.queryForObject("""
				INSERT INTO appointments (business_id, customer_id, staff_id, service_id, starts_at, ends_at, price)
				VALUES (?, ?, ?, ?, '2025-03-04 10:00+01', '2025-03-04 10:45+01', 45.00) RETURNING id""",
				Long.class, Fixture.GLOW, Fixture.GLOW_CUSTOMER, Fixture.GLOW_STAFF, Fixture.GLOW_HAIRCUT);
		AuthenticatedUser superAdmin = new AuthenticatedUser(new User(1L, "Sam", "Root", Role.SUPER_ADMIN, null));
		TransactionTemplate tx = new TransactionTemplate(transactionManager);
		CountDownLatch firstHasCancelled = new CountDownLatch(1);

		CompletableFuture<Void> first = CompletableFuture.runAsync(() -> tx.executeWithoutResult(status -> {
			appointmentService.changeStatus(Fixture.GLOW, id, AppointmentStatus.CANCELLED, superAdmin);
			firstHasCancelled.countDown();
			try {
				Thread.sleep(500);
			} catch (InterruptedException ex) {
				throw new IllegalStateException(ex);
			}
		}));
		firstHasCancelled.await();
		CompletableFuture<Void> second = CompletableFuture.runAsync(() ->
				appointmentService.changeStatus(Fixture.GLOW, id, AppointmentStatus.COMPLETED, superAdmin));

		first.get();
		assertThatThrownBy(second::get).hasRootCauseMessage("A CANCELLED appointment can't become COMPLETED.");
		assertThat(jdbcTemplate.queryForObject("SELECT status FROM appointments WHERE id = ?", String.class, id))
				.isEqualTo("CANCELLED");
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
