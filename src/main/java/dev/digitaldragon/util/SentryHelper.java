package dev.digitaldragon.util;

import dev.digitaldragon.WikiBot;
import io.sentry.Sentry;
import io.sentry.protocol.SentryId;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class SentryHelper {
    private static final Logger logger = LoggerFactory.getLogger(SentryHelper.class);

    public static SentryId captureException(Throwable error) {
        SentryId eventId = Sentry.captureException(error);
        if (WikiBot.getConfig().sentryConfig.isEnabled()) {
            logger.error("Reported exception to Sentry (event ID: {}): {} - {}", eventId, error.getClass().getName(), error.getMessage());
            return eventId;
        }
        logger.error("An exception occurred", error);
        return eventId;
    }
}
