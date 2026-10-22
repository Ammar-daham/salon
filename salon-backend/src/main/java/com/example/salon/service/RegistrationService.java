package com.example.salon.service;

import com.example.salon.model.Business;
import com.example.salon.model.NotificationType;
import com.example.salon.model.Status;
import com.example.salon.model.User;
import com.example.salon.security.RegistrationThrottle;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * A salon owner registering their salon and their own account, with no one signed in. The salon starts
 * PENDING, the owner is its ADMIN, and every SUPER_ADMIN is told, to review it. Until one approves it the
 * owner can't sign in (SalonSignIn). It all happens in one transaction: a salon name or email that's taken
 * leaves nothing behind.
 */
@Service
public class RegistrationService
{
    private static final Logger log = LoggerFactory.getLogger(RegistrationService.class);

    private final BusinessService businessService;
    private final UserService userService;
    private final NotificationService notificationService;
    private final RegistrationThrottle registrationThrottle;

    @Autowired
    public RegistrationService(BusinessService businessService, UserService userService,
            NotificationService notificationService, RegistrationThrottle registrationThrottle)
    {
        this.businessService = businessService;
        this.userService = userService;
        this.notificationService = notificationService;
        this.registrationThrottle = registrationThrottle;
    }

    /** clientAddress is who's registering, for the throttle: every attempt counts, whether or not it goes through. */
    @Transactional
    public void register(Business salon, User owner, String clientAddress)
    {
        registrationThrottle.admit(clientAddress);
        salon.setStatus(Status.PENDING);
        Business created = businessService.addBusiness(salon);
        User account = userService.addOwner(owner, created.getId());
        notificationService.notifySuperAdmins(NotificationType.BUSINESS_REGISTERED, created.getId());
        log.info("Business {} registered by user {}, waiting for review", created.getId(), account.getId());
    }
}
