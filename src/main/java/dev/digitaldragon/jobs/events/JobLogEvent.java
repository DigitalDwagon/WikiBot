package dev.digitaldragon.jobs.events;

import dev.digitaldragon.jobs.Job;
import lombok.Getter;
import net.badbird5907.lightning.event.Event;

@Getter
public class JobLogEvent implements Event {
    private final Job job;
    private final String message;
    public JobLogEvent(Job job, String message){
        this.job = job;
        this.message = message;
    }
}
