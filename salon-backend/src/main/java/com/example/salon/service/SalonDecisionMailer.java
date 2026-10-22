package com.example.salon.service;

import com.example.salon.dao.UserDao;
import com.example.salon.mail.Mailer;
import com.example.salon.model.Role;
import com.example.salon.model.Status;
import com.example.salon.model.User;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.task.TaskExecutor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

/**
 * Emails a salon's ADMINs the outcome of its review, since they can't sign in to see it until it's
 * approved: an approval with a link to sign in ({@code app.sign-in.link}), or a rejection. Suspending or
 * reinstating a salon sends nothing. It sends only once the change is stored, and in the background, as a
 * password reset link is sent, so a slow mail server doesn't hold up the answer.
 */
@Component
public class SalonDecisionMailer
{
    private static final Logger log = LoggerFactory.getLogger(SalonDecisionMailer.class);

    private final UserDao userDao;
    private final Mailer mailer;
    private final TaskExecutor executor;
    private final String signInLink;

    @Autowired
    public SalonDecisionMailer(UserDao userDao, Mailer mailer, TaskExecutor executor,
            @Value("${app.sign-in.link:http://localhost:3000/signin}") String signInLink)
    {
        this.userDao = userDao;
        this.mailer = mailer;
        this.executor = executor;
        this.signInLink = signInLink;
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onStatusChanged(BusinessStatusChanged change)
    {
        if (!change.isDecision())
            return;
        executor.execute(() -> {
            try {
                send(change);
            } catch (RuntimeException ex) {
                log.error("Couldn't email the admins of business {} that it's {}", change.businessId(), change.to(), ex);
            }
        });
    }

    private void send(BusinessStatusChanged change)
    {
        boolean approved = change.to() == Status.APPROVED;
        String subject = approved ? change.name() + " is approved on Salonat Pro"
                : change.name() + " wasn't approved on Salonat Pro";
        for (User admin : userDao.getUsersWithEmail(change.businessId(), Role.ADMIN)) {
            mailer.send(admin.getEmail(), subject, approved ? approvedText(admin, change.name())
                    : rejectedText(admin, change.name()));
        }
        log.info("Told the admins of business {} that it's {}", change.businessId(), change.to());
    }

    private String approvedText(User admin, String salon)
    {
        return """
                Hi %s,

                %s is approved: customers can now find it on Salonat Pro, and you can sign in to set up its \
                services, team and opening hours:

                %s
                """.formatted(admin.getFirstName(), salon, signInLink);
    }

    private static String rejectedText(User admin, String salon)
    {
        return """
                Hi %s,

                We've reviewed %s's registration on Salonat Pro, and it wasn't approved, so the salon isn't \
                listed and its account can't sign in.

                If you think that's a mistake, get in touch with the Salonat Pro team.
                """.formatted(admin.getFirstName(), salon);
    }
}
