package org.schabi.newpipe.database.playlist.model

import android.text.TextUtils
import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Ignore
import androidx.room.Index
import androidx.room.PrimaryKey
import org.schabi.newpipe.database.LocalItem.LocalItemType
import org.schabi.newpipe.database.playlist.PlaylistLocalItem
import org.schabi.newpipe.database.playlist.model.PlaylistRemoteEntity.Companion.REMOTE_PLAYLIST_NAME
import org.schabi.newpipe.database.playlist.model.PlaylistRemoteEntity.Companion.REMOTE_PLAYLIST_SERVICE_ID
import org.schabi.newpipe.database.playlist.model.PlaylistRemoteEntity.Companion.REMOTE_PLAYLIST_TABLE
import org.schabi.newpipe.database.playlist.model.PlaylistRemoteEntity.Companion.REMOTE_PLAYLIST_URL
import org.schabi.newpipe.extractor.playlist.PlaylistInfo

@Entity(
    tableName = REMOTE_PLAYLIST_TABLE,
    indices = [
        Index(value = [REMOTE_PLAYLIST_NAME]),
        Index(value = [REMOTE_PLAYLIST_SERVICE_ID, REMOTE_PLAYLIST_URL], unique = true)
    ]
)
data class PlaylistRemoteEntity(
    @PrimaryKey(autoGenerate = true)
    @ColumnInfo(name = REMOTE_PLAYLIST_ID)
    var uid: Long = 0,

    @ColumnInfo(name = REMOTE_PLAYLIST_SERVICE_ID)
    var serviceId: Int,

    @ColumnInfo(name = REMOTE_PLAYLIST_NAME)
    var name: String?,

    @ColumnInfo(name = REMOTE_PLAYLIST_URL)
    var url: String?,

    @ColumnInfo(name = REMOTE_PLAYLIST_THUMBNAIL_URL)
    var thumbnailUrl: String?,

    @ColumnInfo(name = REMOTE_PLAYLIST_UPLOADER_NAME)
    var uploader: String?,

    @ColumnInfo(name = REMOTE_PLAYLIST_STREAM_COUNT)
    var streamCount: Long?
) : PlaylistLocalItem {
    @Ignore
    constructor(info: PlaylistInfo) : this(
        serviceId = info.serviceId,
        name = info.name,
        url = info.url,
        thumbnailUrl = if (info.thumbnailUrl == null) info.uploaderAvatarUrl else info.thumbnailUrl,
        uploader = info.uploaderName,
        streamCount = info.streamCount
    )

    @Ignore
    fun isIdenticalTo(info: PlaylistInfo): Boolean {
        /*
         * Returns boolean comparing the online playlist and the local copy.
         * (False if info changed such as playlist name or track count)
         */
        return (
            serviceId == info.serviceId && streamCount == info.streamCount && TextUtils.equals(name, info.name) &&
                TextUtils.equals(url, info.url) &&
                TextUtils.equals(thumbnailUrl, info.thumbnailUrl) &&
                TextUtils.equals(uploader, info.uploaderName)
            )
    }

    override fun getLocalItemType(): LocalItemType {
        return LocalItemType.PLAYLIST_REMOTE_ITEM
    }

    override fun getOrderingName(): String? {
        return name
    }

    companion object {
        const val REMOTE_PLAYLIST_TABLE = "remote_playlists"
        const val REMOTE_PLAYLIST_ID = "uid"
        const val REMOTE_PLAYLIST_SERVICE_ID = "service_id"
        const val REMOTE_PLAYLIST_NAME = "name"
        const val REMOTE_PLAYLIST_URL = "url"
        const val REMOTE_PLAYLIST_THUMBNAIL_URL = "thumbnail_url"
        const val REMOTE_PLAYLIST_UPLOADER_NAME = "uploader"
        const val REMOTE_PLAYLIST_STREAM_COUNT = "stream_count"
    }
}
