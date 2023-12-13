package com.radynamics.dallipay.cryptoledger.xrpl.api;

import com.google.common.primitives.UnsignedInteger;
import com.radynamics.dallipay.cryptoledger.xrpl.Ledger;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.xrpl.xrpl4j.client.JsonRpcClientErrorException;
import org.xrpl.xrpl4j.client.XrplClient;
import org.xrpl.xrpl4j.model.client.common.LedgerIndex;
import org.xrpl.xrpl4j.model.client.common.LedgerSpecifier;
import org.xrpl.xrpl4j.model.client.ledger.LedgerRequestParams;

import java.time.Duration;
import java.time.ZonedDateTime;
import java.time.temporal.ChronoUnit;
import java.util.Optional;

public class OnchainLookupProvider implements LedgerAtTimeProvider {
    final static Logger log = LogManager.getLogger(OnchainLookupProvider.class);
    private XrplClient xrplClient;
    private final LedgerAtTimeCache cache = new LedgerAtTimeCache();
    private LedgerAtTime latestLedgerFirstCall;

    public OnchainLookupProvider(XrplClient xrplClient) {
        this.xrplClient = xrplClient;
    }

    public Optional<LedgerAtTime> estimatedDaysAgo(long daysAgo) throws LedgerAtTimeException {
        if (this.latestLedgerFirstCall == null) {
            try {
                this.latestLedgerFirstCall = getEstimatedLatestLedger();
            } catch (JsonRpcClientErrorException e) {
                throw new LedgerAtTimeException(e.getMessage(), e);
            }
        }

        // Assuming average ledger close time
        var ago = Duration.ofDays(daysAgo);
        var estimatedPassedLedgers = UnsignedInteger.valueOf(ago.getSeconds() / Ledger.AVG_LEDGER_CLOSE_TIME_SEC);
        var latestLedgerIndex = latestLedgerFirstCall.getLedgerSpecifier().ledgerIndex().orElseThrow();
        if (latestLedgerIndex.unsignedIntegerValue().compareTo(estimatedPassedLedgers) < 0) {
            return Optional.empty();
        }
        return Optional.of(new LedgerAtTime(ZonedDateTime.now().minus(ago), LedgerSpecifier.of(latestLedgerIndex.minus(estimatedPassedLedgers))));
    }

    public Optional<LedgerAtTime> at(ZonedDateTime dt) throws LedgerAtTimeException {
        try {
            var ledger = findLedger(dt);
            return ledger == null ? Optional.empty() : Optional.of(ledger);
        } catch (JsonRpcClientErrorException e) {
            throw new LedgerAtTimeException(e.getMessage(), e);
        }
    }

    private LedgerAtTime findLedger(ZonedDateTime dt) throws JsonRpcClientErrorException {
        log.trace(String.format("Find ledger at %s", dt));
        var latestLedger = cache.find(dt);
        if (latestLedger == null) {
            latestLedger = getEstimatedLatestLedger();
        }
        if (dt.isAfter(latestLedger.getPointInTime())) {
            log.trace(String.format("%s is after last ledger -> take last ledger at %s", dt, latestLedger.getPointInTime()));
            return latestLedger;
        }

        var avgDurationPerLedger = getAverageLedgerDuration(latestLedger);

        int iteration = 0;
        final int maxIterations = 20;
        var bestMatch = latestLedger;
        while (!isLedgerAt(bestMatch, dt) && iteration < maxIterations) {
            var fromDifference = Duration.ofMillis(ChronoUnit.MILLIS.between(dt, bestMatch.getPointInTime()));
            var isTooEarly = fromDifference.isNegative();
            var estimatedOffsetSecods = fromDifference.toMillis() / (double) avgDurationPerLedger.toMillis();
            // ceil: if last found is slightly too early just take the next one.
            var estimatedOffset = UnsignedInteger.valueOf(Math.abs(Math.round(Math.ceil(estimatedOffsetSecods))));
            if (estimatedOffset.equals(UnsignedInteger.ZERO)) {
                break;
            }
            var estimatedFromLedgerIndex = isTooEarly
                    ? bestMatch.getLedgerSpecifier().ledgerIndex().orElseThrow().plus(estimatedOffset)
                    : bestMatch.getLedgerSpecifier().ledgerIndex().orElseThrow().minus(estimatedOffset);

            var tmp = get(estimatedFromLedgerIndex, !isTooEarly);
            if (tmp == null) {
                log.warn(String.format("Failed to get ledger index near %s.", estimatedFromLedgerIndex));
                return null;
            }
            bestMatch = tmp;

            log.trace(String.format("Iteration %s: estOffset %s -> %s", iteration, estimatedOffset, bestMatch.getPointInTime()));
            iteration++;
        }

        return bestMatch;
    }

    private LedgerAtTime getEstimatedLatestLedger() throws JsonRpcClientErrorException {
        var latestLedgerResult = xrplClient.ledger(LedgerRequestParams.builder().ledgerSpecifier(LedgerSpecifier.CLOSED).build());
        return cache.add(latestLedgerResult.ledger().closeTimeHuman().get(), latestLedgerResult.ledgerIndexSafe());
    }

    private LedgerAtTime get(LedgerIndex index, boolean searchEarlier) {
        var indexCandidate = index;

        final int Max = 10;
        for (var i = 0; i < Max; i++) {
            var candidate = get(indexCandidate);
            if (candidate != null) {
                return candidate;
            }

            var offset = UnsignedInteger.valueOf(100);
            var nextCandidate = searchEarlier ? indexCandidate.minus(offset) : indexCandidate.plus(offset);
            log.trace(String.format("Ledger index %s not found, looking for %s instead", indexCandidate, nextCandidate));
            indexCandidate = nextCandidate;
        }

        return null;
    }

    private LedgerAtTime get(LedgerIndex index) {
        var ledger = cache.find(index);
        if (ledger != null) {
            return ledger;
        }

        try {
            var ledgerResult = xrplClient.ledger(LedgerRequestParams.builder().ledgerSpecifier(LedgerSpecifier.of(index)).build());
            return cache.add(ledgerResult.ledger().closeTimeHuman().get(), ledgerResult.ledgerIndexSafe());
        } catch (JsonRpcClientErrorException e) {
            if (!e.getMessage().equalsIgnoreCase("ledgerNotFound")) {
                log.warn(e.getMessage(), e);
            }
            return null;
        }
    }

    private Duration getAverageLedgerDuration(LedgerAtTime ledger) {
        return getAverageLedgerDuration(ledger, UnsignedInteger.valueOf(1000));
    }

    private Duration getAverageLedgerDuration(LedgerAtTime ledger, UnsignedInteger referenceInterval) {
        var referenceLedgerIndex = ledger.getLedgerSpecifier().ledgerIndex().orElseThrow().minus(referenceInterval);
        var referenceEarlier = get(referenceLedgerIndex);
        if (referenceEarlier == null) {
            var changedReferenceInterval = referenceInterval.minus(UnsignedInteger.valueOf(100));
            log.trace(String.format("Ledger index %s not found, changing reference interval from %s to %s.", referenceEarlier, referenceInterval, changedReferenceInterval));
            return getAverageLedgerDuration(ledger, changedReferenceInterval);
        }

        var durationSeconds = ChronoUnit.SECONDS.between(ledger.getPointInTime(), referenceEarlier.getPointInTime());
        var avgDurationPerLedger = durationSeconds / (double) referenceInterval.longValue();
        return Duration.ofMillis(Math.round(avgDurationPerLedger * 1000));
    }

    private boolean isLedgerAt(LedgerAtTime ledgerAtTime, ZonedDateTime dt) {
        var closeTime = ledgerAtTime.getPointInTime();
        var diff = ChronoUnit.SECONDS.between(closeTime, dt);
        // accept ledger within a smaller timeframe.
        return 0 < diff && diff < Duration.ofSeconds(60).getSeconds();
    }
}
