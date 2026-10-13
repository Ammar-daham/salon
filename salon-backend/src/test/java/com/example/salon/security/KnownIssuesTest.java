package com.example.salon.security;

import com.example.salon.support.IntegrationTest;

/**
 * Known bugs, written as the behaviour we want.
 * They fail today, so each is disabled until the branch named in its reason lands; that branch
 * removes the @Disabled. Run them anyway with: ./gradlew test -PrunKnownIssues
 *
 * Empty right now: every finding the suite could reproduce has been fixed and its test moved to
 * {@link AuthorizationRulesTest}.
 */
class KnownIssuesTest extends IntegrationTest
{
}
