/*
 * Copyright Debezium Authors.
 *
 * Licensed under the Apache Software License version 2.0, available at http://www.apache.org/licenses/LICENSE-2.0
 */
package io.debezium.jbang.core.commands.source;

import io.debezium.jbang.core.DebeziumJBangMain;
import io.debezium.jbang.core.commands.DebeziumCommand;
import io.debezium.jbang.core.commands.PlatformFactory;
import io.debezium.jbang.core.platform.source.dto.SignalCollectionVerifyRequest;
import io.debezium.jbang.core.platform.source.dto.SignalVerificationResult;

import picocli.CommandLine;

@CommandLine.Command(name = "verify-signals", description = "Verify that signal data collection is configured for a connection", sortOptions = false, mixinStandardHelpOptions = true)
public class SourceVerifySignals extends DebeziumCommand {

    @CommandLine.Option(names = { "--connection" }, required = true, description = "Connection id to verify signal collection for")
    Long connectionId;

    @CommandLine.Option(names = { "--table" }, required = true, description = "Fully qualified signal table name (e.g. public.debezium_signal)")
    String table;

    @CommandLine.Mixin
    PlatformFactory platformFactory;

    public SourceVerifySignals(DebeziumJBangMain main) {
        super(main);
    }

    @Override
    public Integer doCall() throws Exception {
        SignalVerificationResult result = platformFactory.source().verifySignals(new SignalCollectionVerifyRequest(connectionId, table));
        if (result.exists()) {
            String msg = "Signal collection table is available" + (table != null ? " (" + table + ")" : "") + ".";
            if (result.message() != null) {
                msg += " — " + result.message();
            }
            println(msg);
            return 0;
        }
        println("Signal collection table is not available" + (result.message() != null ? " — " + result.message() : "."));
        return 1;
    }
}
