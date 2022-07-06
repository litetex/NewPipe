package org.schabi.newpipe.util;

import android.content.Context;
import android.content.SharedPreferences;
import android.net.ConnectivityManager;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.StringRes;
import androidx.core.content.ContextCompat;
import androidx.preference.PreferenceManager;

import org.schabi.newpipe.R;
import org.schabi.newpipe.extractor.streamdata.format.AudioMediaFormat;
import org.schabi.newpipe.extractor.streamdata.format.MediaFormat;
import org.schabi.newpipe.extractor.streamdata.format.VideoAudioMediaFormat;
import org.schabi.newpipe.extractor.streamdata.format.registry.AudioFormatRegistry;
import org.schabi.newpipe.extractor.streamdata.format.registry.VideoAudioFormatRegistry;
import org.schabi.newpipe.extractor.streamdata.stream.AudioStream;
import org.schabi.newpipe.extractor.streamdata.stream.VideoAudioStream;
import org.schabi.newpipe.extractor.streamdata.stream.VideoStream;
import org.schabi.newpipe.extractor.streamdata.stream.quality.VideoQualityData;
import org.schabi.newpipe.util.videoquality.BestVideoQuality;
import org.schabi.newpipe.util.videoquality.SpecificVideoQuality;
import org.schabi.newpipe.util.videoquality.WantedVideoQuality;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.function.BiFunction;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public final class ListHelper {
    // Video format in order of quality. 0=lowest quality, n=highest quality
    private static final List<VideoAudioMediaFormat> VIDEO_FORMAT_QUALITY_RANKING = Arrays.asList(
            VideoAudioFormatRegistry.V3GPP,
            VideoAudioFormatRegistry.WEBM,
            VideoAudioFormatRegistry.MPEG_4);

    // Audio format in order of quality. 0=lowest quality, n=highest quality
    private static final List<AudioMediaFormat> AUDIO_FORMAT_QUALITY_RANKING = Arrays.asList(
            AudioFormatRegistry.MP3,
            AudioFormatRegistry.WEBMA,
            AudioFormatRegistry.M4A);
    // Audio format in order of efficiency. 0=most efficient, n=least efficient
    private static final List<AudioMediaFormat> AUDIO_FORMAT_EFFICIENCY_RANKING = Arrays.asList(
            AudioFormatRegistry.WEBMA,
            AudioFormatRegistry.M4A,
            AudioFormatRegistry.MP3);

    private static final Set<String> HIGH_RESOLUTION_LIST
            // Uses a HashSet for better performance
            = new HashSet<>(Arrays.asList("1440p", "2160p", "1440p60", "2160p60"));

    private ListHelper() {
    }

    /**
     * @param context      Android app context
     * @param videoStreams list of the video streams to check
     * @return index of the video stream with the default index
     * @see #getDefaultResolutionIndex(WantedVideoQuality, String, VideoAudioMediaFormat, List)
     */
    public static int getDefaultResolutionIndex(final Context context,
                                                final List<VideoStream> videoStreams) {
        final WantedVideoQuality defaultResolution = computeDefaultResolution(context,
                R.string.default_resolution_key, R.string.default_resolution_value);
        return getDefaultResolutionWithDefaultFormat(context, defaultResolution, videoStreams);
    }

    /**
     * @param context      Android app context
     * @param videoStreams list of the video streams to check
     * @param videoQuality the default resolution to look for
     * @return index of the video stream with the default index
     * @see #getDefaultResolutionIndex(WantedVideoQuality, String, VideoAudioMediaFormat, List)
     */
    public static int getResolutionIndex(final Context context,
                                         final List<VideoStream> videoStreams,
                                         final WantedVideoQuality videoQuality) {
        return getDefaultResolutionWithDefaultFormat(context, videoQuality, videoStreams);
    }

    /**
     * @param context      Android app context
     * @param videoStreams list of the video streams to check
     * @return index of the video stream with the default index
     * @see #getDefaultResolutionIndex(WantedVideoQuality, String, VideoAudioMediaFormat, List)
     */
    public static int getPopupDefaultResolutionIndex(final Context context,
                                                     final List<VideoStream> videoStreams) {
        final WantedVideoQuality wantedVideoQuality = computeDefaultResolution(
                context,
                R.string.default_popup_resolution_key,
                R.string.default_popup_resolution_value);
        return getDefaultResolutionWithDefaultFormat(context, wantedVideoQuality, videoStreams);
    }

    /**
     * @param context      Android app context
     * @param videoStreams list of the video streams to check
     * @param videoQuality the default resolution to look for
     * @return index of the video stream with the default index
     * @see #getDefaultResolutionIndex(WantedVideoQuality, String, VideoAudioMediaFormat, List)
     */
    public static int getPopupResolutionIndex(final Context context,
                                              final List<VideoStream> videoStreams,
                                              final WantedVideoQuality videoQuality) {
        return getDefaultResolutionWithDefaultFormat(context, videoQuality, videoStreams);
    }

    public static int getDefaultAudioFormat(final Context context,
                                            final List<AudioStream> audioStreams) {
        final AudioMediaFormat defaultFormat = getDefaultFormat(
                context,
                R.string.default_audio_format_key,
                R.string.default_audio_format_value,
                ListHelper::getAudioMediaFormatFromKey);

        // If the user has chosen to limit resolution to conserve mobile data
        // usage then we should also limit our audio usage.
        if (isLimitingDataUsage(context)) {
            return getMostCompactAudioIndex(defaultFormat, audioStreams);
        } else {
            return getHighestQualityAudioIndex(defaultFormat, audioStreams);
        }
    }

    /**
     * Join the two lists of video streams (video_only and normal videos),
     * and sort them according with default format chosen by the user.
     *
     * @param context                the context to search for the format to give preference
     * @param videoStreams           the normal videos list
     * @param videoOnlyStreams       the video-only stream list
     * @param ascendingOrder         true -> smallest to greatest | false -> greatest to smallest
     * @param preferVideoOnlyStreams if video-only streams should preferred when both video-only
     *                               streams and normal video streams are available
     * @return the sorted list
     */
    @NonNull
    public static List<VideoStream> getSortedStreamVideosList(
            @NonNull final Context context,
            @Nullable final List<VideoAudioStream> videoStreams,
            @Nullable final List<VideoStream> videoOnlyStreams,
            final boolean ascendingOrder,
            final boolean preferVideoOnlyStreams) {
        final SharedPreferences preferences
                = PreferenceManager.getDefaultSharedPreferences(context);

        final boolean showHigherResolutions = preferences.getBoolean(
                context.getString(R.string.show_higher_resolutions_key), false);
        final VideoAudioMediaFormat defaultFormat = getDefaultFormat(context,
                R.string.default_video_format_key,
                R.string.default_video_format_value,
                ListHelper::getVideoMediaFormatFromKey);

        return getSortedStreamVideosList(defaultFormat, showHigherResolutions, videoStreams,
                videoOnlyStreams, ascendingOrder, preferVideoOnlyStreams);
    }

    /*//////////////////////////////////////////////////////////////////////////
    // Utils
    //////////////////////////////////////////////////////////////////////////*/
    private static WantedVideoQuality computeDefaultResolution(
            final Context context,
            final int key,
            final int value
    ) {
        final SharedPreferences pref = PreferenceManager.getDefaultSharedPreferences(context);

        final String bestResolutionKey = context.getString(R.string.best_resolution_key);

        final String resolution = pref != null
                ? pref.getString(context.getString(key), context.getString(value))
                : bestResolutionKey;

        // Load the preferred resolution otherwise the best available
        WantedVideoQuality preferredQuality = bestResolutionKey.equals(resolution)
                ? new BestVideoQuality()
                : SpecificVideoQuality.from(resolution);

        final WantedVideoQuality maxResolution =
                SpecificVideoQuality.from(getResolutionLimit(context));
        if (maxResolution != null
                && (preferredQuality instanceof BestVideoQuality
                || compareVideoStreamResolution(maxResolution, preferredQuality) < 1)) {
            preferredQuality = maxResolution;
        }
        return preferredQuality;
    }

    /**
     * Return the index of the default stream in the list, based on the parameters
     * defaultResolution and defaultFormat.
     *
     * @param defaultResolution the default resolution to look for
     * @param bestResolutionKey key of the best resolution
     * @param defaultFormat     the default format to look for
     * @param videoStreams      list of the video streams to check
     * @return index of the default resolution&format
     */
    static int getDefaultResolutionIndex(final WantedVideoQuality defaultResolution,
                                         final String bestResolutionKey,
                                         final VideoAudioMediaFormat defaultFormat,
                                         final List<VideoStream> videoStreams) {
        if (videoStreams == null || videoStreams.isEmpty()) {
            return -1;
        }

        sortStreamList(videoStreams, false);
        if (defaultResolution.equals(bestResolutionKey)) {
            return 0;
        }

        final int defaultStreamIndex
                = getVideoStreamIndex(defaultResolution, defaultFormat, videoStreams);

        // this is actually an error,
        // but maybe there is really no stream fitting to the default value.
        if (defaultStreamIndex == -1) {
            return 0;
        }
        return defaultStreamIndex;
    }

    /**
     * Join the two lists of video streams (video_only and normal videos),
     * and sort them according with default format chosen by the user.
     *
     * @param defaultFormat          format to give preference
     * @param showHigherResolutions  show >1080p resolutions
     * @param videoStreams           normal videos list
     * @param videoOnlyStreams       video only stream list
     * @param ascendingOrder         true -> smallest to greatest | false -> greatest to smallest
     * @param preferVideoOnlyStreams if video-only streams should preferred when both video-only
     *                               streams and normal video streams are available
     * @return the sorted list
     */
    @NonNull
    static List<VideoStream> getSortedStreamVideosList(
            @Nullable final VideoAudioMediaFormat defaultFormat,
            final boolean showHigherResolutions,
            @Nullable final List<VideoAudioStream> videoStreams,
            @Nullable final List<VideoStream> videoOnlyStreams,
            final boolean ascendingOrder,
            final boolean preferVideoOnlyStreams
    ) {
        // Determine order of streams
        // The last added list is preferred
        final List<List<? extends VideoStream>> videoStreamsOrdered =
                preferVideoOnlyStreams
                        ? Arrays.asList(videoStreams, videoOnlyStreams)
                        : Arrays.asList(videoOnlyStreams, videoStreams);

        final List<VideoStream> allInitialStreams = videoStreamsOrdered.stream()
                // Ignore lists that are null
                .filter(Objects::nonNull)
                .flatMap(List::stream)
                // Filter out higher resolutions (or not if high resolutions should always be shown)
                .filter(stream -> showHigherResolutions
                        || !HIGH_RESOLUTION_LIST.contains(
                        VideoQualityStringifier.toString(stream.qualityData())))
                .collect(Collectors.toList());

        final HashMap<String, VideoStream> hashMap = new HashMap<>();
        // Add all to the hashmap
        for (final VideoStream videoStream : allInitialStreams) {
            hashMap.put(
                    VideoQualityStringifier.toString(videoStream.qualityData()),
                    videoStream);
        }

        // Override the values when the key == resolution, with the defaultFormat
        for (final VideoStream videoStream : allInitialStreams) {
            if (videoStream.mediaFormat().equals(defaultFormat)) {
                hashMap.put(
                        VideoQualityStringifier.toString(videoStream.qualityData()),
                        videoStream);
            }
        }

        // Return the sorted list
        return sortStreamList(new ArrayList<>(hashMap.values()), ascendingOrder);
    }

    /**
     * Sort the streams list depending on the parameter ascendingOrder;
     * <p>
     * It works like that:<br>
     * - Take a string resolution, remove the letters, replace "0p60" (for 60fps videos) with "1"
     * and sort by the greatest:<br>
     * <blockquote><pre>
     *      720p     ->  720
     *      720p60   ->  721
     *      360p     ->  360
     *      1080p    ->  1080
     *      1080p60  ->  1081
     * <br>
     * ascendingOrder  ? 360 < 720 < 721 < 1080 < 1081
     * !ascendingOrder ? 1081 < 1080 < 721 < 720 < 360</pre></blockquote>
     *
     * @param videoStreams   list that the sorting will be applied
     * @param ascendingOrder true -> smallest to greatest | false -> greatest to smallest
     * @return The sorted list (same reference as parameter videoStreams)
     */
    private static List<VideoStream> sortStreamList(final List<VideoStream> videoStreams,
                                                    final boolean ascendingOrder) {
        final Comparator<VideoStream> comparator = ListHelper::compareVideoStreamResolution;
        Collections.sort(videoStreams, ascendingOrder ? comparator : comparator.reversed());
        return videoStreams;
    }

    /**
     * Get the audio from the list with the highest quality.
     * Format will be ignored if it yields no results.
     *
     * @param format       The target format type or null if it doesn't matter
     * @param audioStreams List of audio streams
     * @return Index of audio stream that produces the most compact results or -1 if not found
     */
    static int getHighestQualityAudioIndex(@Nullable final AudioMediaFormat format,
                                           @Nullable final List<AudioStream> audioStreams) {
        return getAudioIndexByHighestRank(format, audioStreams,
                // Compares descending (last = highest rank)
                (s1, s2) -> compareAudioStreamBitrate(s1, s2, AUDIO_FORMAT_QUALITY_RANKING)
        );
    }

    /**
     * Get the audio from the list with the lowest bitrate and most efficient format.
     * Format will be ignored if it yields no results.
     *
     * @param format       The target format type or null if it doesn't matter
     * @param audioStreams List of audio streams
     * @return Index of audio stream that produces the most compact results or -1 if not found
     */
    static int getMostCompactAudioIndex(@Nullable final AudioMediaFormat format,
                                        @Nullable final List<AudioStream> audioStreams) {

        return getAudioIndexByHighestRank(format, audioStreams,
                // The "-" is important -> Compares ascending (first = highest rank)
                (s1, s2) -> -compareAudioStreamBitrate(s1, s2, AUDIO_FORMAT_EFFICIENCY_RANKING)
        );
    }

    /**
     * Get the audio-stream from the list with the highest rank, depending on the comparator.
     * Format will be ignored if it yields no results.
     *
     * @param targetedFormat The target format type or null if it doesn't matter
     * @param audioStreams   List of audio streams
     * @param comparator     The comparator used for determining the max/best/highest ranked value
     * @return Index of audio stream that produces the highest ranked result or -1 if not found
     */
    private static int getAudioIndexByHighestRank(@Nullable final AudioMediaFormat targetedFormat,
                                                  @Nullable final List<AudioStream> audioStreams,
                                                  final Comparator<AudioStream> comparator) {
        if (audioStreams == null || audioStreams.isEmpty()) {
            return -1;
        }

        final AudioStream highestRankedAudioStream = audioStreams.stream()
                .filter(audioStream -> targetedFormat == null
                        || audioStream.mediaFormat().equals(targetedFormat))
                .max(comparator)
                .orElse(null);

        if (highestRankedAudioStream == null) {
            // Fallback: Ignore targetedFormat if not null
            if (targetedFormat != null) {
                return getAudioIndexByHighestRank(null, audioStreams, comparator);
            }
            // targetedFormat is already null -> return -1
            return -1;
        }

        return audioStreams.indexOf(highestRankedAudioStream);
    }

    /**
     * Locates a possible match for the given resolution and format in the provided list.
     *
     * <p>In this order:</p>
     *
     * <ol>
     * <li>height + fps + format</li>
     * <li>height + format</li>
     * <li>height + fps</li>
     * <li>height</li>
     * <li>height < requested height</li>
     * <li>Give up</li>
     * </ol>
     *
     * @param targetQuality the quality to look for
     * @param targetFormat  the format to look for
     * @param videoStreams  the available video streams
     * @return the index of the preferred video stream
     */
    static int getVideoStreamIndex(@NonNull final WantedVideoQuality targetQuality,
                                   final VideoAudioMediaFormat targetFormat,
                                   final List<VideoStream> videoStreams) {
        final Predicate<VideoStream> predMediaFormat =
                targetFormat == null
                        ? null
                        : v -> v.mediaFormat().equals(targetFormat);

        final List<List<Predicate<VideoStream>>> predicateGroupsToCheck;

        if (targetQuality instanceof BestVideoQuality) {
            predicateGroupsToCheck = buildAndRemoveNull(
                    buildAndRemoveNullOrReturnNull(predMediaFormat),
                    Collections.singletonList(v -> true) // Fallback
            );
        } else if (targetQuality instanceof SpecificVideoQuality) {
            final SpecificVideoQuality specificVideoQuality = (SpecificVideoQuality) targetQuality;

            final Predicate<VideoStream> predHeight =
                    v -> v.qualityData().height() == specificVideoQuality.height();

            final Predicate<VideoStream> predFPS =
                    specificVideoQuality.fps() == SpecificVideoQuality.UNKNOWN_FPS
                            ? null
                            : v -> v.qualityData().fps() == specificVideoQuality.fps();
            final Predicate<VideoStream> predBelowHeight =
                    v -> v.qualityData().height() < specificVideoQuality.height();

            predicateGroupsToCheck = buildAndRemoveNull(
                    buildAndRemoveNullOrReturnNull(predHeight, predFPS, predMediaFormat),
                    buildAndRemoveNullOrReturnNull(predHeight, predMediaFormat),
                    buildAndRemoveNullOrReturnNull(predHeight, predFPS),
                    buildAndRemoveNullOrReturnNull(predHeight),
                    buildAndRemoveNullOrReturnNull(predBelowHeight)
            );
        } else {
            return -1;
        }

        final List<VideoStream> orderedVideoStreams = videoStreams.stream()
                .sorted(Comparator
                        .<VideoStream, Integer>comparing(
                                v -> v.qualityData().height(),
                                Comparator.reverseOrder())
                        .thenComparing(
                                v -> v.qualityData().fps(),
                                Comparator.reverseOrder()))
                .collect(Collectors.toList());

        for (final List<Predicate<VideoStream>> predicatesToCheck : predicateGroupsToCheck) {
            for (final VideoStream videoStream : orderedVideoStreams) {
                if (predicatesToCheck.stream().allMatch(x -> x.test(videoStream))) {
                    return videoStreams.indexOf(videoStream);
                }
            }
        }
        return -1;
    }

    @NonNull
    @SafeVarargs // https://stackoverflow.com/a/21150650
    private static <X> List<X> buildAndRemoveNull(final X... elements) {
        return Stream.of(elements)
                .filter(Objects::nonNull)
                .collect(Collectors.toList());
    }

    @Nullable
    @SafeVarargs // https://stackoverflow.com/a/21150650
    private static <X> List<X> buildAndRemoveNullOrReturnNull(
            final X... elements) {
        final List<X> list = buildAndRemoveNull(elements);

        if (list.isEmpty()) {
            return null;
        }
        return list;
    }

    /**
     * Fetches the desired resolution or returns the default if it is not found.
     * The resolution will be reduced if video chocking is active.
     *
     * @param context         Android app context
     * @param targetedQuality the default resolution
     * @param videoStreams    the list of video streams to check
     * @return the index of the preferred video stream
     */
    private static int getDefaultResolutionWithDefaultFormat(
            final Context context,
            final WantedVideoQuality targetedQuality,
            final List<VideoStream> videoStreams
    ) {
        final VideoAudioMediaFormat defaultFormat = getDefaultFormat(
                context,
                R.string.default_video_format_key,
                R.string.default_video_format_value,
                ListHelper::getVideoMediaFormatFromKey);
        return getDefaultResolutionIndex(
                targetedQuality,
                context.getString(R.string.best_resolution_key),
                defaultFormat,
                videoStreams);
    }

    private static <M extends MediaFormat> M getDefaultFormat(
            final Context context,
            @StringRes final int defaultFormatKey,
            @StringRes final int defaultFormatValueKey,
            final BiFunction<Context, String, M> mediaDataFromKeyFunc
    ) {
        final SharedPreferences preferences
                = PreferenceManager.getDefaultSharedPreferences(context);

        final String defaultFormat = context.getString(defaultFormatValueKey);
        final String defaultFormatString = preferences.getString(
                context.getString(defaultFormatKey), defaultFormat);

        final M defaultMediaFormat = mediaDataFromKeyFunc.apply(context, defaultFormatString);
        if (defaultMediaFormat != null) {
            return defaultMediaFormat;
        }

        preferences.edit().putString(context.getString(defaultFormatKey), defaultFormat).apply();
        return mediaDataFromKeyFunc.apply(context, defaultFormat);
    }

    private static VideoAudioMediaFormat getVideoMediaFormatFromKey(final Context context,
                                                                    final String formatKey) {
        if (formatKey.equals(context.getString(R.string.video_webm_key))) {
            return VideoAudioFormatRegistry.WEBM;
        } else if (formatKey.equals(context.getString(R.string.video_mp4_key))) {
            return VideoAudioFormatRegistry.MPEG_4;
        } else if (formatKey.equals(context.getString(R.string.video_3gp_key))) {
            return VideoAudioFormatRegistry.V3GPP;
        }
        return null;
    }

    private static AudioMediaFormat getAudioMediaFormatFromKey(final Context context,
                                                               final String formatKey) {
        if (formatKey.equals(context.getString(R.string.audio_webm_key))) {
            return AudioFormatRegistry.WEBMA;
        } else if (formatKey.equals(context.getString(R.string.audio_m4a_key))) {
            return AudioFormatRegistry.M4A;
        }
        return null;
    }

    // Compares the quality of two audio streams
    private static int compareAudioStreamBitrate(final AudioStream streamA,
                                                 final AudioStream streamB,
                                                 final List<AudioMediaFormat> formatRanking) {
        if (streamA == null) {
            return -1;
        }
        if (streamB == null) {
            return 1;
        }
        if (streamA.averageBitrate() < streamB.averageBitrate()) {
            return -1;
        }
        if (streamA.averageBitrate() > streamB.averageBitrate()) {
            return 1;
        }

        // Same bitrate and format
        return formatRanking.indexOf(streamA.mediaFormat())
                - formatRanking.indexOf(streamB.mediaFormat());
    }

    private static int compareVideoStreamResolution(final VideoQualityData q1,
                                                    final VideoQualityData q2) {
        final int res1 = q1.height() + (q1.fps() > 30 ? 1 : 0);
        final int res2 = q2.height() + (q2.fps() > 30 ? 1 : 0);
        return res1 - res2;
    }

    // Compares the quality of two video streams.
    private static int compareVideoStreamResolution(final VideoStream streamA,
                                                    final VideoStream streamB) {
        if (streamA == null) {
            return -1;
        }
        if (streamB == null) {
            return 1;
        }

        final int resComp = compareVideoStreamResolution(
                streamA.qualityData(),
                streamB.qualityData());
        if (resComp != 0) {
            return resComp;
        }

        // Same bitrate and format
        return ListHelper.VIDEO_FORMAT_QUALITY_RANKING.indexOf(streamA.mediaFormat())
                - ListHelper.VIDEO_FORMAT_QUALITY_RANKING.indexOf(streamB.mediaFormat());
    }


    private static boolean isLimitingDataUsage(final Context context) {
        return getResolutionLimit(context) != null;
    }

    /**
     * The maximum resolution allowed.
     *
     * @param context App context
     * @return maximum resolution allowed or null if there is no maximum
     */
    private static String getResolutionLimit(final Context context) {
        if (!isMeteredNetwork(context)) {
            return null;
        }

        final SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
        final String defValue = context.getString(R.string.limit_data_usage_none_key);
        final String value = prefs.getString(
                context.getString(R.string.limit_mobile_data_usage_key), defValue);
        return defValue.equals(value) ? null : value;
    }

    /**
     * The current network is metered (like mobile data)?
     *
     * @param context App context
     * @return {@code true} if connected to a metered network
     */
    public static boolean isMeteredNetwork(@NonNull final Context context) {
        final ConnectivityManager manager
                = ContextCompat.getSystemService(context, ConnectivityManager.class);
        if (manager == null || manager.getActiveNetworkInfo() == null) {
            return false;
        }

        return manager.isActiveNetworkMetered();
    }
}
