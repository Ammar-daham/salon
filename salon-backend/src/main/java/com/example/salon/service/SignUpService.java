package com.example.salon.service;

import com.example.salon.model.User;
import com.example.salon.security.SignUpThrottle;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

/**
 * A client creating their own account, with no one signed in, to book in the customer app. The account is a
 * CUSTOMER's with no salon: it meets a salon's client record when the client first books there.
 */
@Service
public class SignUpService
{
    private static final Logger log = LoggerFactory.getLogger(SignUpService.class);

    private final UserService userService;
    private final SignUpThrottle signUpThrottle;

    @Autowired
    public SignUpService(UserService userService, SignUpThrottle signUpThrottle)
    {
        this.userService = userService;
        this.signUpThrottle = signUpThrottle;
    }

    /**
     * Creates the account and returns it as stored. clientAddress is who's signing up, for the throttle:
     * every attempt counts, whether or not it goes through.
     */
    public User signUp(User client, String clientAddress)
    {
        signUpThrottle.admit(clientAddress);
        User account = userService.addCustomer(client);
        log.info("User {} signed up", account.getId());
        return account;
    }
}
