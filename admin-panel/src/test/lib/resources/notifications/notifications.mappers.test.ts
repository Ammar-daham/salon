import { describe, expect, it } from "vitest";
import {
	notificationText,
	toNotification,
	unreadBadge,
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

describe("unreadBadge", () => {
	it("shows nothing for none, the count up to 9, then 9+", () => {
		expect(unreadBadge(0)).toBeNull();
		expect(unreadBadge(1)).toBe("1");
		expect(unreadBadge(9)).toBe("9");
		expect(unreadBadge(10)).toBe("9+");
		expect(unreadBadge(250)).toBe("9+");
	});
});
