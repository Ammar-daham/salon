package com.example.salon.controller;

import com.example.salon.model.Notification;
import com.example.salon.model.UnreadCount;
import com.example.salon.paging.Page;
import com.example.salon.paging.PageQuery;
import com.example.salon.security.AuthenticatedUser;
import com.example.salon.service.NotificationService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

/**
 * The signed-in user's notifications, under {@code /api/v1/notifications}.
 *
 * <p>Anyone signed in, about themselves only: no one reads or marks another user's. Today a SUPER_ADMIN is
 * told of each salon an owner registers ({@code BUSINESS_REGISTERED}); no other role is told anything yet.
 */
@RestController
@RequestMapping("api/v1/notifications")
public class NotificationController
{
	private final NotificationService notificationService;

	@Autowired
	public NotificationController(NotificationService notificationService)
	{
		this.notificationService = notificationService;
	}

	/**
	 * Lists the caller's notifications, newest first, a page at a time.
	 *
	 * <p>{@code GET /api/v1/notifications}. Anyone signed in. {@code unread=true} leaves out those already
	 * read. Each is a {@link Notification}.
	 *
	 * <p>Errors: 400 if {@code unread}, {@code page} or {@code size} is out of range.
	 *
	 * @param unread {@code true} for only the unread ones
	 * @param page the page, counting from 1
	 * @param size notifications per page, 1 to 100, 20 if left out
	 * @param principal the signed-in caller
	 * @return 200 with one page of notifications
	 */
	@GetMapping
	public Page<Notification> getNotifications(
			@RequestParam(defaultValue = "false") boolean unread,
			@RequestParam(required = false) Integer page,
			@RequestParam(required = false) Integer size,
			@AuthenticationPrincipal AuthenticatedUser principal)
	{
		return notificationService.getNotifications(unread, PageQuery.of(page, size), principal);
	}

	/**
	 * Says how many of the caller's notifications are unread, for a badge.
	 *
	 * <p>{@code GET /api/v1/notifications/unread-count}. Anyone signed in. Answers {@code {"count": 3}}.
	 *
	 * @param principal the signed-in caller
	 * @return 200 with the count
	 */
	@GetMapping("/unread-count")
	public UnreadCount countUnread(@AuthenticationPrincipal AuthenticatedUser principal)
	{
		return notificationService.countUnread(principal);
	}

	/**
	 * Marks one of the caller's notifications read.
	 *
	 * <p>{@code PUT /api/v1/notifications/{id}/read}. Anyone signed in, for their own. Takes no body. One
	 * already read keeps the time it was first read.
	 *
	 * <p>Errors: 404 if the caller has no such notification, whether or not someone else does.
	 *
	 * @param id the notification's id
	 * @param principal the signed-in caller
	 * @return 200 with the notification as stored
	 */
	@PutMapping("/{id}/read")
	public Notification markRead(@PathVariable long id, @AuthenticationPrincipal AuthenticatedUser principal)
	{
		return notificationService.markRead(id, principal);
	}

	/**
	 * Marks all the caller's notifications read.
	 *
	 * <p>{@code PUT /api/v1/notifications/read}. Anyone signed in. Takes no body. Answers 204.
	 *
	 * @param principal the signed-in caller
	 */
	@PutMapping("/read")
	@ResponseStatus(HttpStatus.NO_CONTENT)
	public void markAllRead(@AuthenticationPrincipal AuthenticatedUser principal)
	{
		notificationService.markAllRead(principal);
	}
}
