import type { BusinessStatus } from "./businesses.types";

/** A super admin's move from one status to another, with the words to confirm and report it. */
export interface StatusAction {
	to: BusinessStatus;
	/** The button, e.g. "Approve". */
	label: string;
	/** Asked before the move: what it does to the salon and its staff. */
	consequence: string;
	/** The toast once it's done, e.g. "Salon approved". */
	done: string;
	/** Shown in the warning tone, for moves that shut the salon's staff out or hide it. */
	destructive: boolean;
}

const APPROVE: StatusAction = {
	to: "APPROVED",
	label: "Approve",
	consequence: "Everyone can see the salon, and its staff can sign in.",
	done: "Salon approved",
	destructive: false,
};

/**
 * What a super admin may do with a salon in each status. The API lets any status follow any other;
 * these are the moves the review flow needs: a registration is approved or rejected, an approved
 * salon suspended, and a suspended or rejected one brought back.
 */
export function statusActions(status: BusinessStatus): StatusAction[] {
	switch (status) {
		case "PENDING":
			return [
				APPROVE,
				{
					to: "REJECTED",
					label: "Reject",
					consequence:
						"The salon stays hidden, and its staff can't sign in. You can approve it later.",
					done: "Salon rejected",
					destructive: true,
				},
			];
		case "APPROVED":
			return [
				{
					to: "SUSPENDED",
					label: "Suspend",
					consequence:
						"The salon is hidden from everyone but its own staff, who can still sign in.",
					done: "Salon suspended",
					destructive: true,
				},
			];
		case "SUSPENDED":
			return [{ ...APPROVE, label: "Reinstate", done: "Salon reinstated" }];
		case "REJECTED":
			return [APPROVE];
	}
}
