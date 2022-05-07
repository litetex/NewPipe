package org.schabi.newpipe.database.playlist.model

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Ignore
import androidx.room.Index
import androidx.room.PrimaryKey
import org.schabi.newpipe.database.playlist.model.PlaylistEntity.Companion.PLAYLIST_NAME
import org.schabi.newpipe.database.playlist.model.PlaylistEntity.Companion.PLAYLIST_TABLE
import org.schabi.newpipe.database.stream.model.StreamEntity
import java.io.Serializable

@Entity(
    tableName = PLAYLIST_TABLE,
    indices = [Index(value = [PLAYLIST_NAME])]
)
data class PlaylistEntity(
    @PrimaryKey(autoGenerate = true)
    @ColumnInfo(name = PLAYLIST_ID)
    var uid: Long = 0,

    @ColumnInfo(name = PLAYLIST_NAME)
    var name: String?,

    @ColumnInfo(name = PLAYLIST_THUMBNAIL_URL)
    var thumbnailUrl: String?
) : Serializable {
    @Ignore
    constructor(name: String, streamEntity: StreamEntity) : this(
        name = name,
        thumbnailUrl = streamEntity.thumbnailUrl
    )

    companion object {
        const val PLAYLIST_TABLE = "playlists"
        const val PLAYLIST_ID = "uid"
        const val PLAYLIST_NAME = "name"
        const val PLAYLIST_THUMBNAIL_URL = "thumbnail_url"
    }
}
