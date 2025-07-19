package dev.digitaldragon.jobs.queues;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.jetbrains.annotations.NotNull;

@Getter
@RequiredArgsConstructor
public class Queue {
    @NotNull
    private String name;
    @NotNull
    private Integer concurrency;
    @NotNull
    private Integer priority;

}
