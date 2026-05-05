package dev.digitaldragon.interfaces.api.messages;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@RequiredArgsConstructor
@AllArgsConstructor
public class SystemMessage {
    private DiskSpaceInfo diskSpaceInfo;


    @Getter
    @Setter
    @AllArgsConstructor
    public static class DiskSpaceInfo {
        private long freeBytes;
        private long totalBytes;
    }
}
