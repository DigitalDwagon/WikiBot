package dev.digitaldragon.jobs.events;

import dev.digitaldragon.jobs.Job;
import lombok.Getter;
import net.badbird5907.lightning.event.Event;

@Getter
public class JobAbortEvent implements Event {
    private final Job job;
    public JobAbortEvent(Job job){
        this.job = job;
    }
}
