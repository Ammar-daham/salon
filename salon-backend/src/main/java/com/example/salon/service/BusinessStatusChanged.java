package com.example.salon.service;

import com.example.salon.model.Status;

/** A SUPER_ADMIN moved a salon from one status to another. Published by BusinessService.changeStatus. */
public record BusinessStatusChanged(long businessId, String name, Status from, Status to)
{
	/** A review's outcome, which the salon's admins hear about: approved, or rejected while pending. */
	public boolean isDecision()
	{
		return (to == Status.APPROVED && (from == Status.PENDING || from == Status.REJECTED))
				|| (to == Status.REJECTED && from == Status.PENDING);
	}
}
