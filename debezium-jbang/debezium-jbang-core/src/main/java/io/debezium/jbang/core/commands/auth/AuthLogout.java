/*
 * Copyright Debezium Authors.
 *
 * Licensed under the Apache Software License version 2.0, available at http://www.apache.org/licenses/LICENSE-2.0
 */
package io.debezium.jbang.core.commands.auth;

import io.debezium.jbang.core.DebeziumJBangMain;
import io.debezium.jbang.core.auth.AuthException;
import io.debezium.jbang.core.auth.CredentialStore;
import io.debezium.jbang.core.auth.StoredCredential;
import io.debezium.jbang.core.auth.TokenEndpoint;
import io.debezium.jbang.core.auth.TokenProvider;
import io.debezium.jbang.core.commands.DebeziumCommand;
import io.debezium.jbang.core.commands.PlatformFactory;

import picocli.CommandLine;

@CommandLine.Command(name = "logout", description = "Revoke the session at the issuer and delete it locally", mixinStandardHelpOptions = true, sortOptions = false)
public class AuthLogout extends DebeziumCommand {

    @CommandLine.Mixin
    PlatformFactory platformFactory;

    @CommandLine.Option(names = {
            "--local" }, description = "Delete the stored session without contacting the issuer (leaves the server-side session alive)")
    boolean localOnly;

    public AuthLogout(DebeziumJBangMain main) {
        super(main);
    }

    @Override
    public Integer doCall() throws Exception {
        String platformAddress = CredentialStore.normalizeKey(platformFactory.resolvedApiUrl(configuration()).toString());
        StoredCredential credential = CredentialStore.find(platformAddress);

        if (credential == null) {
            println("Not logged in to " + platformAddress + ".");
            return 0;
        }

        // Revoke first, delete second. The other order can leave a live offline session at the
        // issuer with no local record of it — a long-lived credential nobody can see or revoke.
        if (!localOnly) {
            try {
                TokenEndpoint.endSession(
                        TokenProvider.endpointsFor(credential.issuer()),
                        credential.clientId(),
                        credential.refreshToken());
                println("Session revoked at " + credential.issuer() + ".");
            }
            catch (AuthException e) {
                println("Warning: could not revoke at the issuer — " + e.getMessage());
                println("The local session will still be removed; the server-side session may remain active.");
            }
        }

        TokenProvider.invalidate(platformAddress);
        CredentialStore.remove(platformAddress);
        println("Logged out of " + platformAddress + ".");
        return 0;
    }
}
