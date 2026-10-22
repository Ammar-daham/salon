package com.example.salon.security;

import com.example.salon.dao.BusinessDao;
import com.example.salon.model.Role;
import com.example.salon.model.Status;
import com.example.salon.model.User;
import org.springframework.stereotype.Component;

import java.util.Optional;

/**
 * Whether a salon's staff may sign in, given where their salon stands with the platform. An ADMIN or EMPLOYEE
 * of a salon that is PENDING, waiting for a SUPER_ADMIN to review it, or REJECTED can't: the platform hasn't
 * taken the salon on. A SUSPENDED salon's staff still can, to see their own salon, which is hidden from
 * everyone else. A SUPER_ADMIN or a CUSTOMER is never held back here.
 *
 * <p>AuthController.login asks at sign-in, and RefreshingSecurityContextRepository on every request, so a
 * salon that stops being approved signs its staff out.
 */
@Component
public class SalonSignIn
{
	private final BusinessDao businessDao;

	public SalonSignIn(BusinessDao businessDao)
	{
		this.businessDao = businessDao;
	}

	/** Why the user can't sign in, for them to read, or nothing if they can. */
	public Optional<String> refusal(User user)
	{
		boolean staff = user.getRole() == Role.ADMIN || user.getRole() == Role.EMPLOYEE;
		if (!staff || user.getBusinessId() == null)
			return Optional.empty();
		return businessDao.getStatus(user.getBusinessId()).flatMap(SalonSignIn::refusal);
	}

	private static Optional<String> refusal(Status status)
	{
		return switch (status) {
			case PENDING -> Optional.of("Your salon is waiting for approval. You can sign in once it's approved.");
			case REJECTED -> Optional.of("Your salon's registration wasn't approved.");
			case APPROVED, SUSPENDED -> Optional.empty();
		};
	}
}
