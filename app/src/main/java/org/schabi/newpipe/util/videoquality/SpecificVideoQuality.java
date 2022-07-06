package org.schabi.newpipe.util.videoquality;

import org.schabi.newpipe.extractor.streamdata.stream.quality.VideoQualityData;

public class SpecificVideoQuality implements WantedVideoQuality {
    public static final int UNKNOWN_FPS = -1;

    private final int height;
    private final int fps;

    public SpecificVideoQuality(final int height) {
        this(height, UNKNOWN_FPS);
    }

    public SpecificVideoQuality(final int height, final int fps) {
        this.height = height;
        this.fps = fps;
    }

    public int height() {
        return height;
    }

    public int fps() {
        return fps;
    }

    public static SpecificVideoQuality from(final String str) {
        if (str == null) {
            return null;
        }
        try {
            if (str.contains("p")) {
                final String[] parts = str.split("p");
                return new SpecificVideoQuality(
                        Integer.parseInt(parts[0]), Integer.parseInt(parts[1]));
            }
            return new SpecificVideoQuality(Integer.parseInt(str));
        } catch (final NumberFormatException nfe) {
            return null;
        }
    }

    public static SpecificVideoQuality from(final VideoQualityData qualityData) {
        if (qualityData == null) {
            return null;
        }
        return new SpecificVideoQuality(qualityData.height(), qualityData.fps());
    }
}
