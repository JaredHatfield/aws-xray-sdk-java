package com.amazonaws.xray.strategy;

import java.util.Optional;

import javax.servlet.http.HttpServletRequest;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.amazonaws.xray.entities.SearchPattern;

public class DynamicSegmentNamingStrategy implements SegmentNamingStrategy {
    private static final Log logger =
        LogFactory.getLog(DynamicSegmentNamingStrategy.class);

    private String recognizedHosts;
    private String fallbackName;

    /**
     * Creates an instance of {@code DynamicSegmentNamingStrategy} with the provided {@code fallbackName} and a {@code recognizedHosts} value of "*".
     *
     * @param fallbackName
     *  the fallback segment name used when no host header is included in the incoming request. This will be overriden by the value of the {@code AWS_XRAY_TRACING_NAME} environment variable or {@code com.amazonaws.xray.strategy.tracingName} system property, if either are set to a non-empty value.
     */
    public DynamicSegmentNamingStrategy(String fallbackName) {
        this(fallbackName, "*");
    }

    /**
     * Creates an instance of {@code DynamicSegmentNamingStrategy} with the provided {@code fallbackName} and {@code recognizedHosts} values.
     *
     * @param fallbackName
     *  the fallback segment name used when no host header is included in the incoming request or the incoming host header value does not match the provided pattern. This will be overriden by the value of the {@code AWS_XRAY_TRACING_NAME} environment variable or {@code com.amazonaws.xray.strategy.tracingName} system property, if either are set to a non-empty value.
     * @param recognizedHosts
     *  the pattern to match the incoming host header value against. This pattern is compared against the incoming host header using the {@link com.amazonaws.xray.entities.SearchPattern#wildcardMatch(String, String)} method.
     */
    public DynamicSegmentNamingStrategy(String fallbackName, String recognizedHosts) {
        this.fallbackName = fallbackName;
        String overrideName = getOverrideName();
        if (null != overrideName) {
            this.fallbackName = getOverrideName();
            if (logger.isInfoEnabled()) {
                logger.info("Environment variable " + NAME_OVERRIDE_ENVIRONMENT_VARIABLE_KEY + " or system property " + NAME_OVERRIDE_SYSTEM_PROPERTY_KEY + " set. Overriding DynamicSegmentNamingStrategy constructor argument. Segments generated with this strategy will be named: " + this.fallbackName + " when the host header is unavilable or does not match the provided recognizedHosts pattern.");
            }
        }

        this.recognizedHosts = recognizedHosts;
    }

    /**
     *
     * Returns the derived segment name for an incoming request. Attempts to get the {@code Host} header from the {@code HttpServletRequest}. If the {@code Host} header has a value and if the value matches the optionally provided {@code recognizedHosts} pattern, then this value is returned as the segment name. Otherwise, {@code fallbackName} is returned.
     *
     *
     * @param request
     *  the incoming request
     * @return
     *  the segment name for the incoming request.
     */
    @Override
    public String nameForRequest(HttpServletRequest request) {
        Optional<String> hostHeaderValue = Optional.ofNullable(request.getHeader("Host"));
        if (hostHeaderValue.isPresent() && (null == recognizedHosts || SearchPattern.wildcardMatch(recognizedHosts, hostHeaderValue.get()))) {
            return hostHeaderValue.get();
        }
        return fallbackName;
    }
}
