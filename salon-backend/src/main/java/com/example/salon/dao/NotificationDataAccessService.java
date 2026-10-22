package com.example.salon.dao;

import com.example.salon.model.Notification;
import com.example.salon.model.NotificationType;
import com.example.salon.model.Role;
import com.example.salon.paging.Page;
import com.example.salon.paging.PageQuery;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.List;
import java.util.Optional;

@Repository
public class NotificationDataAccessService implements NotificationDao
{
    private static final String COLUMNS = "n.id, n.type, n.business_id, b.name AS business_name, n.created_at, n.read_at";
    private static final String FROM = "notifications n LEFT JOIN businesses b ON b.id = n.business_id";

    private final JdbcTemplate jdbcTemplate;

    @Autowired
    public NotificationDataAccessService(JdbcTemplate jdbcTemplate)
    {
        this.jdbcTemplate = jdbcTemplate;
    }

    @Override
    public int notifyRole(Role role, NotificationType type, Long businessId)
    {
        return jdbcTemplate.update("""
                INSERT INTO notifications (user_id, type, business_id)
                SELECT id, ?, ? FROM users WHERE role = ?
                """, type.name(), businessId, role.name());
    }

    @Override
    public Page<Notification> getNotifications(long userId, boolean unreadOnly, PageQuery page)
    {
        ListQuery query = new ListQuery().where("n.user_id = ?", userId);
        if (unreadOnly)
            query.where("n.read_at IS NULL");
        return query.page(jdbcTemplate, COLUMNS, FROM, ListQuery.orderBy(List.of("n.created_at"), true, "n.id"),
                page, (rs, i) -> mapRow(rs));
    }

    @Override
    public long countUnread(long userId)
    {
        return jdbcTemplate.queryForObject(
                "SELECT count(*) FROM notifications WHERE user_id = ? AND read_at IS NULL", Long.class, userId);
    }

    @Override
    public Optional<Notification> markRead(long userId, long id)
    {
        int row = jdbcTemplate.update(
                "UPDATE notifications SET read_at = COALESCE(read_at, now()) WHERE id = ? AND user_id = ?",
                id, userId);
        if (row == 0)
            return Optional.empty();
        return jdbcTemplate.query("SELECT " + COLUMNS + " FROM " + FROM + " WHERE n.id = ?",
                (rs, i) -> mapRow(rs), id).stream().findFirst();
    }

    @Override
    public int markAllRead(long userId)
    {
        return jdbcTemplate.update(
                "UPDATE notifications SET read_at = now() WHERE user_id = ? AND read_at IS NULL", userId);
    }

    private static Notification mapRow(ResultSet rs) throws SQLException
    {
        long businessId = rs.getLong("business_id");
        Notification.BusinessRef business = rs.wasNull() ? null
                : new Notification.BusinessRef(businessId, rs.getString("business_name"));
        Timestamp readAt = rs.getTimestamp("read_at");
        return new Notification(
                rs.getLong("id"),
                NotificationType.valueOf(rs.getString("type")),
                business,
                rs.getTimestamp("created_at").toInstant(),
                readAt != null ? readAt.toInstant() : null
        );
    }
}
