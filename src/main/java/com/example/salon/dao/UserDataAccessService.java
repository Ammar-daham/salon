package com.example.salon.dao;

import com.example.salon.model.Role;
import com.example.salon.model.User;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public class UserDataAccessService implements UserDao
{
    private final JdbcTemplate jdbcTemplate;
    private final ContactDao contactDao;
    private final AddressDao addressDao;

    @Autowired
    public UserDataAccessService(JdbcTemplate jdbcTemplate, ContactDao contactDao, AddressDao addressDao) {
        this.jdbcTemplate = jdbcTemplate;
        this.contactDao = contactDao;
        this.addressDao = addressDao;
    }

    @Override
    public Long addUser(User user)
    {
        String sql = """
                INSERT INTO users (first_name, last_name, role)
                VALUES (?, ?, ?)
                RETURNING id
                """;

        Long userId = jdbcTemplate.queryForObject(
                sql,
                Long.class,
                user.getFirstName(),
                user.getLastName(),
                user.getRole().name()
        );
        user.setId(userId);

        // Insert addresses
        if (user.getAddresses() != null)
        {
            user.getAddresses().forEach(address ->
            {
                address.setUserId(userId);
                addressDao.addAddress(address);
            });
        }

        // Insert contacts
        if (user.getContacts() != null)
        {
            user.getContacts().forEach(contact ->
            {
                contact.setUserId(userId);
                contactDao.addContact(contact);
            });
        }
        return userId;
    }

    @Override
    public List<User> getAllUsers()
    {
        String sql = "SELECT * FROM users";
        List<User> users = jdbcTemplate.query(sql, (rs, i) ->
                new User (
                        rs.getLong("id"),
                        rs.getString("first_name"),
                        rs.getString("last_name"),
                        Role.valueOf(rs.getString("role")),
                        rs.getTimestamp("created_at").toInstant()
                )
        );
        for (User user : users)
        {
            user.setAddresses(addressDao.getAddressesForUser(user.getId()));
            user.setContacts(contactDao.getContactsForUser(user.getId()));
        }
        return users;
    }

    @Override
    public User getUserById(int id) {
        return null;
    }

    @Override
    public int updateUserById(User user) {
        return 0;
    }

    @Override
    public int deleteUserById(int id) {
        return 0;
    }
}
