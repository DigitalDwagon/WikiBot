package dev.digitaldragon.util;

import com.beust.jcommander.IParameterValidator;
import com.beust.jcommander.ParameterException;

import java.net.MalformedURLException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;

public class URLValidator implements IParameterValidator {

    @Override
    public void validate(String parameter, String value) throws ParameterException {
        try {
            URL url = new URI(value).toURL();
            if (!url.getProtocol().equals("http") && !url.getProtocol().equals("https")) {
                throw new ParameterException("Invalid protocol: " + url.getProtocol());
            }
        } catch (MalformedURLException | URISyntaxException | IllegalArgumentException e) {
            throw new ParameterException("Invalid URL: " + value);
        }
    }
}
