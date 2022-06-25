package org.schabi.newpipe.database.stream.model

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Ignore
import androidx.room.Index
import androidx.room.PrimaryKey
import org.schabi.newpipe.database.stream.model.StreamEntity.Companion.STREAM_SERVICE_ID
import org.schabi.newpipe.database.stream.model.StreamEntity.Companion.STREAM_TABLE
import org.schabi.newpipe.database.stream.model.StreamEntity.Companion.STREAM_URL
import org.schabi.newpipe.extractor.localization.DateWrapper
import org.schabi.newpipe.extractor.stream.StreamInfo
import org.schabi.newpipe.extractor.stream.StreamInfoItem
import org.schabi.newpipe.player.playqueue.PlayQueueItem
import java.io.Serializable
import java.time.OffsetDateTime

@Entity(
    tableName = STREAM_TABLE,
    indices = [
        Index(value = [STREAM_SERVICE_ID, STREAM_URL], unique = true)
    ]
)
data class StreamEntity(
    @PrimaryKey(autoGenerate = true)
    @ColumnInfo(name = STREAM_ID)
    var uid: Long = 0,

    @ColumnInfo(name = STREAM_SERVICE_ID)
    var serviceId: Int,

    @ColumnInfo(name = STREAM_URL)
    var url: String,

    @ColumnInfo(name = STREAM_TITLE)
    var title: String,

    @ColumnInfo(name = STREAM_LIVE)
    var live: Boolean,

    @ColumnInfo(name = STREAM_AUDIO_ONLY)
    var audioOnly: Boolean,

    @ColumnInfo(name = STREAM_DURATION)
    var duration: Long,

    @ColumnInfo(name = STREAM_UPLOADER)
    var uploader: String,

    @ColumnInfo(name = STREAM_UPLOADER_URL)
    var uploaderUrl: String? = null,

    @ColumnInfo(name = STREAM_THUMBNAIL_URL)
    var thumbnailUrl: String? = null,

    @ColumnInfo(name = STREAM_VIEWS)
    var viewCount: Long? = null,

    @ColumnInfo(name = STREAM_TEXTUAL_UPLOAD_DATE)
    var textualUploadDate: String? = null,

    @ColumnInfo(name = STREAM_UPLOAD_DATE)
    var uploadDate: OffsetDateTime? = null,

    @ColumnInfo(name = STREAM_IS_UPLOAD_DATE_APPROXIMATION)
    var isUploadDateApproximation: Boolean? = null
) : Serializable {
    @Ignore
    constructor(item: StreamInfoItem) : this(
        serviceId = item.serviceId,
        url = item.url,
        title = item.name,
        live = item.isLive,
        audioOnly = item.isAudioOnly,
        duration = item.duration,
        uploader = item.uploaderName,
        uploaderUrl = item.uploaderUrl,
        thumbnailUrl = item.thumbnailUrl,
        viewCount = item.viewCount,
        textualUploadDate = item.textualUploadDate,
        uploadDate = item.uploadDate?.offsetDateTime(),
        isUploadDateApproximation = item.uploadDate?.isApproximation
    )

    @Ignore
    constructor(info: StreamInfo) : this(
        serviceId = info.serviceId,
        url = info.url,
        title = info.name,
        live = info.isLive,
        audioOnly = info.isAudioOnly,
        duration = info.duration,
        uploader = info.uploaderName,
        uploaderUrl = info.uploaderUrl,
        thumbnailUrl = info.thumbnailUrl,
        viewCount = info.viewCount,
        textualUploadDate = info.textualUploadDate,
        uploadDate = info.uploadDate?.offsetDateTime(),
        isUploadDateApproximation = info.uploadDate?.isApproximation
    )

    @Ignore
    constructor(item: PlayQueueItem) : this(
        serviceId = item.serviceId,
        url = item.url,
        title = item.title,
        live = item.isLive,
        audioOnly = item.isAudioOnly,
        duration = item.duration,
        uploader = item.uploader,
        uploaderUrl = item.uploaderUrl,
        thumbnailUrl = item.thumbnailUrl
    )

    fun toStreamInfoItem(): StreamInfoItem {
        val item = StreamInfoItem(serviceId, url, title, live, audioOnly)
        item.duration = duration
        item.uploaderName = uploader
        item.uploaderUrl = uploaderUrl
        item.thumbnailUrl = thumbnailUrl

        if (viewCount != null) item.viewCount = viewCount as Long
        item.textualUploadDate = textualUploadDate
        item.uploadDate = uploadDate?.let {
            DateWrapper(it, isUploadDateApproximation ?: false)
        }

        return item
    }

    companion object {
        const val STREAM_TABLE = "streams"
        const val STREAM_ID = "uid"
        const val STREAM_SERVICE_ID = "service_id"
        const val STREAM_URL = "url"
        const val STREAM_TITLE = "title"
        const val STREAM_LIVE = "live"
        const val STREAM_AUDIO_ONLY = "audio_only"
        const val STREAM_DURATION = "duration"
        const val STREAM_UPLOADER = "uploader"
        const val STREAM_UPLOADER_URL = "uploader_url"
        const val STREAM_THUMBNAIL_URL = "thumbnail_url"

        const val STREAM_VIEWS = "view_count"
        const val STREAM_TEXTUAL_UPLOAD_DATE = "textual_upload_date"
        const val STREAM_UPLOAD_DATE = "upload_date"
        const val STREAM_IS_UPLOAD_DATE_APPROXIMATION = "is_upload_date_approximation"
    }
}
