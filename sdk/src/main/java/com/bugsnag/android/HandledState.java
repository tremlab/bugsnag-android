package com.bugsnag.android;

import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.annotation.StringDef;
import android.text.TextUtils;

import java.io.IOException;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

final class HandledState implements JsonStream.Streamable {


    @StringDef({REASON_UNHANDLED_EXCEPTION, REASON_STRICT_MODE, REASON_HANDLED_EXCEPTION,
        REASON_USER_SPECIFIED, REASON_CALLBACK_SPECIFIED})
    @Retention(RetentionPolicy.SOURCE)
    @interface SeverityReason {
    }

    static final String REASON_UNHANDLED_EXCEPTION = "unhandledException";
    static final String REASON_STRICT_MODE = "strictMode";
    static final String REASON_HANDLED_EXCEPTION = "handledException";
    static final String REASON_USER_SPECIFIED = "userSpecifiedSeverity";
    static final String REASON_CALLBACK_SPECIFIED = "userCallbackSetSeverity";

    @SeverityReason
    private final String severityReasonType;

    @Nullable
    private final String attributeValue;

    private final Severity defaultSeverity;
    private Severity currentSeverity;
    private final boolean unhandled;

    static HandledState newInstance(@SeverityReason String severityReasonType) {
        return newInstance(severityReasonType, null, null);
    }

    static HandledState newInstance(@SeverityReason String severityReasonType,
                                    @Nullable Severity severity,
                                    @Nullable String attributeValue) {

        if (severityReasonType.equals(REASON_STRICT_MODE) && TextUtils.isEmpty(attributeValue)) {
            throw new IllegalArgumentException("No reason supplied for strictmode");
        }
        if (!severityReasonType.equals(REASON_STRICT_MODE) && !TextUtils.isEmpty(attributeValue)) {
            throw new IllegalArgumentException("attributeValue should not be supplied");
        }

        switch (severityReasonType) {
            case REASON_UNHANDLED_EXCEPTION:
                return new HandledState(severityReasonType, Severity.ERROR, true, null);
            case REASON_STRICT_MODE:
                return new HandledState(severityReasonType, Severity.WARNING, true, attributeValue);
            case REASON_HANDLED_EXCEPTION:
                return new HandledState(severityReasonType, Severity.WARNING, false, null);
            case REASON_USER_SPECIFIED:
                return new HandledState(severityReasonType, severity, false, null);
            default:
                String msg = String.format("Invalid argument '%s' for severityReason",
                    severityReasonType);
                throw new IllegalArgumentException(msg);
        }
    }

    private HandledState(String severityReasonType, Severity currentSeverity, boolean unhandled,
                         @Nullable String attributeValue) {
        this.severityReasonType = severityReasonType;
        this.defaultSeverity = currentSeverity;
        this.unhandled = unhandled;
        this.attributeValue = attributeValue;
        this.currentSeverity = currentSeverity;
    }

    String calculateSeverityReasonType() {
        return defaultSeverity == currentSeverity ? severityReasonType : REASON_CALLBACK_SPECIFIED;
    }

    Severity getCurrentSeverity() {
        return currentSeverity;
    }

    boolean isUnhandled() {
        return unhandled;
    }

    @Nullable
    String getAttributeValue() {
        return attributeValue;
    }

    void setCurrentSeverity(Severity severity) {
        this.currentSeverity = severity;
    }

    @Override
    public void toStream(@NonNull JsonStream writer) throws IOException {
        writer.beginObject().name("type").value(calculateSeverityReasonType());

        if (attributeValue != null) {
            writer.name("attributes").beginObject()
                .name("violationType").value(attributeValue)
                .endObject();
        }
        writer.endObject();
    }

}
