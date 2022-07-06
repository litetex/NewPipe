package org.schabi.newpipe.util;

import android.content.Context;
import android.util.SparseArray;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.Spinner;
import android.widget.TextView;

import androidx.annotation.NonNull;

import org.schabi.newpipe.DownloaderImpl;
import org.schabi.newpipe.R;
import org.schabi.newpipe.extractor.streamdata.format.registry.AudioFormatRegistry;
import org.schabi.newpipe.extractor.streamdata.stream.AudioStream;
import org.schabi.newpipe.extractor.streamdata.stream.BaseAudioStream;
import org.schabi.newpipe.extractor.streamdata.stream.Stream;
import org.schabi.newpipe.extractor.streamdata.stream.SubtitleStream;
import org.schabi.newpipe.extractor.streamdata.stream.VideoStream;

import java.io.Serializable;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.Callable;

import io.reactivex.rxjava3.android.schedulers.AndroidSchedulers;
import io.reactivex.rxjava3.core.Single;
import io.reactivex.rxjava3.schedulers.Schedulers;
import us.shandian.giga.util.Utility;

/**
 * A list adapter for a list of {@link Stream streams}.
 *
 * @param <T> the primary stream type's class extending {@link Stream}
 * @param <U> the secondary stream type's class extending {@link Stream}
 */
public class StreamItemAdapter<T extends Stream<?>, U extends AudioStream> extends BaseAdapter {
    private final Context context;

    private final StreamSizeWrapper<T> streamsWrapper;
    private final SparseArray<SecondaryStreamHelper<U>> secondaryStreams;

    /**
     * Indicates that at least one of the primary streams is an instance of {@link VideoStream},
     * has no audio and has no secondary stream associated with it.
     */
    private final boolean hasAnyVideoOnlyStreamWithNoSecondaryStream;

    public StreamItemAdapter(final Context context, final StreamSizeWrapper<T> streamsWrapper,
                             final SparseArray<SecondaryStreamHelper<U>> secondaryStreams) {
        this.context = context;
        this.streamsWrapper = streamsWrapper;
        this.secondaryStreams = secondaryStreams;

        this.hasAnyVideoOnlyStreamWithNoSecondaryStream =
                checkHasAnyVideoOnlyStreamWithNoSecondaryStream();
    }

    public StreamItemAdapter(final Context context, final StreamSizeWrapper<T> streamsWrapper) {
        this(context, streamsWrapper, null);
    }

    public List<T> getAll() {
        return streamsWrapper.getStreamsList();
    }

    public SparseArray<SecondaryStreamHelper<U>> getAllSecondary() {
        return secondaryStreams;
    }

    @Override
    public int getCount() {
        return streamsWrapper.getStreamsList().size();
    }

    @Override
    public T getItem(final int position) {
        return streamsWrapper.getStreamsList().get(position);
    }

    @Override
    public long getItemId(final int position) {
        return position;
    }

    @Override
    public View getDropDownView(final int position,
                                final View convertView,
                                final ViewGroup parent) {
        return getCustomView(position, convertView, parent, true);
    }

    @Override
    public View getView(final int position, final View convertView, final ViewGroup parent) {
        return getCustomView(((Spinner) parent).getSelectedItemPosition(),
                convertView, parent, false);
    }

    @NonNull
    private View getCustomView(final int position,
                               final View view,
                               final ViewGroup parent,
                               final boolean isDropdownItem) {
        View convertView = view;
        if (convertView == null) {
            convertView = LayoutInflater.from(context).inflate(
                    R.layout.stream_quality_item, parent, false);
        }

        final ImageView woSoundIconView = convertView.findViewById(R.id.wo_sound_icon);
        final TextView formatNameView = convertView.findViewById(R.id.stream_format_name);
        final TextView qualityView = convertView.findViewById(R.id.stream_quality);
        final TextView sizeView = convertView.findViewById(R.id.stream_size);

        if (streamsWrapper.getSizeInBytes(position) > 0) {
            final SecondaryStreamHelper<U> secondary =
                    secondaryStreams == null
                            ? null
                            : secondaryStreams.get(position);
            if (secondary != null) {
                final long size = secondary.getSizeInBytes()
                        + streamsWrapper.getSizeInBytes(position);
                sizeView.setText(Utility.formatBytes(size));
            } else {
                sizeView.setText(streamsWrapper.getFormattedSize(position));
            }
            sizeView.setVisibility(View.VISIBLE);
        } else {
            sizeView.setVisibility(View.GONE);
        }

        final T stream = getItem(position);

        qualityView.setText(getQualityString(stream));
        woSoundIconView.setVisibility(
                determineWoSoundIconVisibility(stream, position, isDropdownItem));
        formatNameView.setText(determineTextForFormatNameView(stream));

        return convertView;
    }

    private String determineTextForFormatNameView(final Stream<?> stream) {
        if (stream instanceof SubtitleStream) {
            return ((SubtitleStream) stream).languageCode();
        } else if (stream.mediaFormat().name().equals(AudioFormatRegistry.WEBMA_OPUS.name())) {
            return AudioFormatRegistry.OPUS.name();
        }
        return stream.mediaFormat().name();
    }

    private String getQualityString(final Stream<?> stream) {
        if (stream instanceof VideoStream) {
            return VideoQualityStringifier.toString(((VideoStream) stream).qualityData());
        } else if (stream instanceof AudioStream) {
            final AudioStream audioStream = ((AudioStream) stream);
            return audioStream.averageBitrate() != BaseAudioStream.UNKNOWN_AVG_BITRATE
                    ? audioStream.averageBitrate() + "kbps"
                    : audioStream.mediaFormat().name();
        } else if (stream instanceof SubtitleStream) {
            final SubtitleStream subtitleStream = ((SubtitleStream) stream);
            return subtitleStream.getDisplayLanguageName()
                    + (subtitleStream.autoGenerated()
                    ? " (" + context.getString(R.string.caption_auto_generated) + ")"
                    : "");
        }
        return stream.mediaFormat().suffix();
    }

    private int determineWoSoundIconVisibility(
            final Stream<?> stream,
            final int position,
            final boolean isDropdownItem
    ) {
        if (!(stream instanceof VideoStream) || !hasAnyVideoOnlyStreamWithNoSecondaryStream) {
            return View.GONE;
        }

        if (StreamTypeUtil.isVideoOnly(stream)) {
            // It doesn't have a secondary stream, icon is visible no matter what.
            if (!hasSecondaryStream(position)) {
                return View.VISIBLE;
            }
            // It has a secondary stream associated with it, so check if it's a
            // dropdown view so it doesn't look out of place (missing margin)
            // compared to those that don't.
            return isDropdownItem ? View.INVISIBLE : View.GONE;
        }
        return View.INVISIBLE;
    }

    /**
     * @param position which primary stream to check.
     * @return whether the primary stream at position has a secondary stream associated with it.
     */
    private boolean hasSecondaryStream(final int position) {
        return secondaryStreams != null && secondaryStreams.get(position) != null;
    }

    /**
     * @return if there are any video-only streams with no secondary stream associated with them.
     * @see #hasAnyVideoOnlyStreamWithNoSecondaryStream
     */
    private boolean checkHasAnyVideoOnlyStreamWithNoSecondaryStream() {
        for (int i = 0; i < streamsWrapper.getStreamsList().size(); i++) {
            final T stream = streamsWrapper.getStreamsList().get(i);
            // Check if stream has only video data and no audio
            if (stream instanceof VideoStream && !(stream instanceof BaseAudioStream)
                    // And if it has no secondary stream...
                    && !hasSecondaryStream(i)) {
                return true;
            }
        }

        return false;
    }

    /**
     * A wrapper class that includes a way of storing the stream sizes.
     *
     * @param <T> the stream type's class extending {@link Stream}
     */
    public static class StreamSizeWrapper<T extends Stream<?>> implements Serializable {
        private static final StreamSizeWrapper<Stream<?>> EMPTY = new StreamSizeWrapper<>(
                Collections.emptyList(), null);
        private final List<T> streamsList;
        private final long[] streamSizes;
        private final String unknownSize;

        public StreamSizeWrapper(final List<T> sL, final Context context) {
            this.streamsList = sL != null
                    ? sL
                    : Collections.emptyList();
            this.streamSizes = new long[streamsList.size()];
            this.unknownSize = context == null
                    ? "--.-" : context.getString(R.string.unknown_content);

            Arrays.fill(streamSizes, -2);
        }

        /**
         * Helper method to fetch the sizes of all the streams in a wrapper.
         *
         * @param <X>            the stream type's class extending {@link Stream}
         * @param streamsWrapper the wrapper
         * @return a {@link Single} that returns a boolean indicating if any elements were changed
         */
        public static <X extends Stream<?>> Single<Boolean> fetchSizeForWrapper(
                final StreamSizeWrapper<X> streamsWrapper) {
            final Callable<Boolean> fetchAndSet = () -> {
                boolean hasChanged = false;
                for (final X stream : streamsWrapper.getStreamsList()) {
                    if (streamsWrapper.getSizeInBytes(stream) > -2) {
                        continue;
                    }

                    streamsWrapper.setSize(
                            stream,
                            stream.deliveryData()
                                    .getExpectedContentLength(DownloaderImpl.getInstance()));
                    hasChanged = true;
                }
                return hasChanged;
            };

            return Single.fromCallable(fetchAndSet)
                    .subscribeOn(Schedulers.io())
                    .observeOn(AndroidSchedulers.mainThread())
                    .onErrorReturnItem(true);
        }

        public static <X extends Stream<?>> StreamSizeWrapper<X> empty() {
            //noinspection unchecked
            return (StreamSizeWrapper<X>) EMPTY;
        }

        public List<T> getStreamsList() {
            return streamsList;
        }

        public long getSizeInBytes(final int streamIndex) {
            return streamSizes[streamIndex];
        }

        public long getSizeInBytes(final T stream) {
            return streamSizes[streamsList.indexOf(stream)];
        }

        public String getFormattedSize(final int streamIndex) {
            return formatSize(getSizeInBytes(streamIndex));
        }

        public String getFormattedSize(final T stream) {
            return formatSize(getSizeInBytes(stream));
        }

        private String formatSize(final long size) {
            if (size > -1) {
                return Utility.formatBytes(size);
            }
            return unknownSize;
        }

        public void setSize(final int streamIndex, final long sizeInBytes) {
            streamSizes[streamIndex] = sizeInBytes;
        }

        public void setSize(final T stream, final long sizeInBytes) {
            streamSizes[streamsList.indexOf(stream)] = sizeInBytes;
        }
    }
}
