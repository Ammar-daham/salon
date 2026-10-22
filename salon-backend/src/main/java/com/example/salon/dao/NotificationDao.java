package com.example.salon.dao;

import com.example.salon.model.Notification;
import com.example.salon.model.NotificationType;
import com.example.salon.model.Role;
import com.example.salon.paging.Page;
import com.example.salon.paging.PageQuery;

import java.util.Optional;

/** Each user's notifications: every method works on the one user's rows only. */
public interface NotificationDao
{
    /** Tells every user who has the role, a row each, and returns how many were told. */
    int notifyRole(Role role, NotificationType type, Long businessId);

    /** Newest first; unreadOnly leaves out the ones already read. */
    Page<Notification> getNotifications(long userId, boolean unreadOnly, PageQuery page);

    long countUnread(long userId);

    /** Marks it read, keeping when it first was; nothing if the user has no such notification. */
    Optional<Notification> markRead(long userId, long id);

    int markAllRead(long userId);
}
