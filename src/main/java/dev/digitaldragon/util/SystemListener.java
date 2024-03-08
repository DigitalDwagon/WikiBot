package dev.digitaldragon.util;

import dev.digitaldragon.jobs.events.JobLogEvent;
import net.badbird5907.lightning.annotation.EventHandler;

public class SystemListener {
    @EventHandler
    public void onJobLog(JobLogEvent event) {
        System.out.println("log event: " + event.getMessage());
    }
}
