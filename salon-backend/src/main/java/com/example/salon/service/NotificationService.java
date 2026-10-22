package com.example.salon.service;

import com.example.salon.dao.NotificationDao;
import com.example.salon.exception.BaseException;
import com.example.salon.exception.ErrorCode;
import com.example.salon.model.Notification;
import com.example.salon.model.NotificationType;
import com.example.salon.model.Role;
import com.example.salon.model.UnreadCount;
import com.example.salon.paging.Page;
import com.example.salon.paging.PageQuery;
import com.example.salon.security.AuthenticatedUser;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

/**
 * In-app notifications. Each user reads and clears only their own, so another user's notification is a 404,
 * like one that doesn't exist. Other services tell people things through {@link #notifySuperAdmins}.
 */
@Service
public class NotificationService
{
    private static final Logger log = LoggerFactory.getLogger(NotificationService.class);

    private final NotificationDao notificationDao;

    @Autowired
    public NotificationService(NotificationDao notificationDao)
    {
        this.notificationDao = notificationDao;
    }

    /** Tells every SUPER_ADMIN about the salon. Call it inside the transaction that did what they're told of. */
    public void notifySuperAdmins(NotificationType type, long businessId)
    {
        int told = notificationDao.notifyRole(Role.SUPER_ADMIN, type, businessId);
        log.info("Told {} super admins {} for business {}", told, type, businessId);
    }

    public Page<Notification> getNotifications(boolean unreadOnly, PageQuery page, AuthenticatedUser caller)
    {
        return notificationDao.getNotifications(caller.getUser().getId(), unreadOnly, page);
    }

    public UnreadCount countUnread(AuthenticatedUser caller)
    {
        return new UnreadCount(notificationDao.countUnread(caller.getUser().getId()));
    }

    public Notification markRead(long id, AuthenticatedUser caller)
    {
        return notificationDao.markRead(caller.getUser().getId(), id)
                .orElseThrow(() -> new BaseException("Notification with id " + id + " not found.",
                        ErrorCode.NOT_FOUND));
    }

    public void markAllRead(AuthenticatedUser caller)
    {
        notificationDao.markAllRead(caller.getUser().getId());
    }
}
