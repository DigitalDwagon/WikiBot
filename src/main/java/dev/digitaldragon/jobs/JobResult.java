package dev.digitaldragon.jobs;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@RequiredArgsConstructor
public class JobResult {
    private final boolean success;
    private final int exitCode;
}
