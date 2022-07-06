package org.schabi.newpipe.util;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import org.schabi.newpipe.extractor.streamdata.format.AudioMediaFormat;
import org.schabi.newpipe.extractor.streamdata.format.VideoAudioMediaFormat;
import org.schabi.newpipe.extractor.streamdata.format.registry.AudioFormatRegistry;
import org.schabi.newpipe.extractor.streamdata.format.registry.VideoAudioFormatRegistry;
import org.schabi.newpipe.extractor.streamdata.stream.AudioStream;
import org.schabi.newpipe.extractor.streamdata.stream.VideoStream;
import org.schabi.newpipe.util.StreamItemAdapter.StreamSizeWrapper;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Objects;

public class SecondaryStreamHelper<T extends AudioStream> {
    private final int position;
    private final StreamSizeWrapper<T> streams;

    public SecondaryStreamHelper(@NonNull final StreamSizeWrapper<T> streams,
                                 final T selectedStream) {
        this.streams = streams;
        this.position = streams.getStreamsList().indexOf(selectedStream);
        if (this.position < 0) {
            throw new IllegalArgumentException("selected stream not found");
        }
    }

    /**
     * Find the correct audio stream for the desired video stream.
     *
     * @param audioStreams list of audio streams
     * @param videoStream  desired video ONLY stream
     * @return selected audio stream or null if a candidate was not found
     */
    @Nullable
    public static AudioStream getAudioStreamFor(@NonNull final List<AudioStream> audioStreams,
                                                @NonNull final VideoStream videoStream) {

        final Map<VideoAudioMediaFormat, List<AudioMediaFormat>> linkedFormats = Map.ofEntries(
                Map.entry(VideoAudioFormatRegistry.WEBM, Arrays.asList(
                        AudioFormatRegistry.WEBMA,
                        AudioFormatRegistry.WEBMA_OPUS
                )),
                Map.entry(VideoAudioFormatRegistry.MPEG_4, Collections.singletonList(
                        AudioFormatRegistry.M4A
                ))
        );

        // 1. Get the supported audiomedia formats for the videostream
        return linkedFormats.entrySet()
                .stream()
                .filter(e -> Objects.equals(e.getKey().name(), videoStream.mediaFormat().name()))
                .findFirst()
                .map(Map.Entry::getValue)
                .orElse(Collections.emptyList())
                // 2. Find a matching audio stream
                .stream()
                .flatMap(amf ->
                        audioStreams.stream().filter(a -> amf.id() == a.mediaFormat().id()))
                .findFirst()
                .orElse(null);
    }

    public T getStream() {
        return streams.getStreamsList().get(position);
    }

    public long getSizeInBytes() {
        return streams.getSizeInBytes(position);
    }
}
