package dev.digitaldragon.util;

import com.beust.jcommander.IParameterValidator;
import com.beust.jcommander.ParameterException;
import dev.digitaldragon.jobs.JobMeta;

import java.util.Arrays;
import java.util.Locale;

public class SilentModeValidator implements IParameterValidator {

    @Override
    public void validate(String parameter, String value) throws ParameterException {
        try {
            value = value.toUpperCase(Locale.ENGLISH);
            JobMeta.SilentMode.valueOf(value);
        } catch (IllegalArgumentException e) {
            throw new ParameterException("Invalid silent mode - it must be one of: " + Arrays.toString(JobMeta.SilentMode.values()));
        }
    }
}
