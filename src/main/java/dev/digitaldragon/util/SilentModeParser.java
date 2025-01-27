package dev.digitaldragon.util;

import com.beust.jcommander.IStringConverter;
import com.beust.jcommander.ParameterException;
import dev.digitaldragon.jobs.JobMeta;

import java.util.Arrays;
import java.util.Locale;

public class SilentModeParser implements IStringConverter<JobMeta.SilentMode> {
    @Override
    public JobMeta.SilentMode convert(String value) {
        try {
            value = value.toUpperCase(Locale.ENGLISH);
            return JobMeta.SilentMode.valueOf(value);
        } catch (IllegalArgumentException e) {
            throw new ParameterException("Invalid silent mode - it must be one of: " + Arrays.toString(JobMeta.SilentMode.values()));
        }
    }
}
