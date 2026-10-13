package com.example.salon.security;

import com.example.salon.support.IntegrationTest;

/**
 * Known bugs, written as the behaviour we want. Each fails today, so it's @Disabled with the bug
 * in its reason, and the fix removes the @Disabled. Run them anyway with:
 * ./gradlew test -PrunKnownIssues
 *
 * Empty right now: every bug the suite could reproduce has been fixed.
 */
class KnownIssuesTest extends IntegrationTest
{
}
