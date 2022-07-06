package org.schabi.newpipe.util;

import org.schabi.newpipe.extractor.streamdata.stream.quality.VideoQualityData;

/**
 * Converts {@link VideoQualityData} into a (human-readable) String.
 */
public final class VideoQualityStringifier {
    private VideoQualityStringifier() {
        // No impl
    }

    public static String toString(final VideoQualityData data) {
        return possibleUnknownValueToString(data.height())
                + (data.fps() > 30 ? ("p" + data.fps()) : "");
    }

    public static String toFullString(final VideoQualityData data) {
        return possibleUnknownValueToString(data.height())
                + "x"
                + possibleUnknownValueToString(data.width())
                + " "
                + possibleUnknownValueToString(data.fps());
    }

    private static String possibleUnknownValueToString(final int value) {
        return value != VideoQualityData.UNKNOWN ? String.valueOf(value) : "?";
    }
}
