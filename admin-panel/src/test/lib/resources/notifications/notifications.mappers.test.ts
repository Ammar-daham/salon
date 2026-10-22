import { describe, expect, it } from "vitest";
import {
	notificationText,
	toNotification,
	type NotificationDto,
} from "@/lib/resources/notifications/notifications.mappers";

const registered: NotificationDto = {
	id: 7,
	type: "BUSINESS_REGISTERED",
	business: { id: 3, name: "Serenity Day Spa" },
	created_at: "2026-10-21T09:00:00Z",
	read_at: null,
};

describe("toNotification", () => {
	it("maps the wire shape", () => {
		expect(toNotification(registered)).toEqual({
			id: 7,
			type: "BUSINESS_REGISTERED",
			business: { id: 3, name: "Serenity Day Spa" },
			createdAt: "2026-10-21T09:00:00Z",
			readAt: null,
		});
		expect(toNotification({ ...registered, read_at: "2026-10-21T10:00:00Z" }).readAt).toBe(
			"2026-10-21T10:00:00Z",
		);
	});
});

describe("notificationText", () => {
	it("leads a registration to the salon, for its review", () => {
		expect(notificationText(toNotification(registered))).toEqual({
			title: "New salon registration",
			body: "Serenity Day Spa is waiting for your review.",
			href: "/businesses/3",
		});
	});

	it("leads to the pending salons when it names none", () => {
		const text = notificationText(toNotification({ ...registered, business: null }));
		expect(text.body).toBe("A salon is waiting for your review.");
		expect(text.href).toBe("/businesses?status=PENDING");
	});

	it("still reads as something for a type it doesn't know", () => {
		const unknown = toNotification({ ...registered, type: "SOMETHING_NEW" as NotificationDto["type"] });
		expect(notificationText(unknown)).toEqual({ title: "Notification", body: "", href: "/notifications" });
	});
});
