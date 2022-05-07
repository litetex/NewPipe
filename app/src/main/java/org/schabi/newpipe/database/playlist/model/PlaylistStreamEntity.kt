package org.schabi.newpipe.database.playlist.model

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import org.schabi.newpipe.database.LocalItem
import org.schabi.newpipe.database.playlist.model.PlaylistStreamEntity.Companion.JOIN_INDEX
import org.schabi.newpipe.database.playlist.model.PlaylistStreamEntity.Companion.JOIN_PLAYLIST_ID
import org.schabi.newpipe.database.playlist.model.PlaylistStreamEntity.Companion.JOIN_STREAM_ID
import org.schabi.newpipe.database.playlist.model.PlaylistStreamEntity.Companion.PLAYLIST_STREAM_JOIN_TABLE
import org.schabi.newpipe.database.stream.model.StreamEntity

@Entity(
    tableName = PLAYLIST_STREAM_JOIN_TABLE,
    primaryKeys = [
        JOIN_PLAYLIST_ID,
        JOIN_INDEX
    ],
    indices = [
        Index(value = [JOIN_PLAYLIST_ID, JOIN_INDEX], unique = true),
        Index(value = [JOIN_STREAM_ID])
    ],
    foreignKeys = [
        ForeignKey(
            entity = PlaylistEntity::class,
            parentColumns = [PlaylistEntity.PLAYLIST_ID],
            childColumns = [JOIN_PLAYLIST_ID],
            onDelete = ForeignKey.CASCADE,
            onUpdate = ForeignKey.CASCADE,
            deferred = true
        ),
        ForeignKey(
            entity = StreamEntity::class,
            parentColumns = [StreamEntity.STREAM_ID],
            childColumns = [JOIN_STREAM_ID],
            onDelete = ForeignKey.CASCADE,
            onUpdate = ForeignKey.CASCADE,
            deferred = true
        )
    ]
)
data class PlaylistStreamEntity(
    @ColumnInfo(name = JOIN_PLAYLIST_ID)
    var playlistUid: Long,

    @ColumnInfo(name = JOIN_STREAM_ID)
    var streamUid: Long,

    @ColumnInfo(name = JOIN_INDEX)
    var index: Int
) : LocalItem {

    override fun getLocalItemType(): LocalItem.LocalItemType {
        return LocalItem.LocalItemType.PLAYLIST_STREAM_ITEM
    }

    companion object {
        const val PLAYLIST_STREAM_JOIN_TABLE = "playlist_stream_join"
        const val JOIN_PLAYLIST_ID = "playlist_id"
        const val JOIN_STREAM_ID = "stream_id"
        const val JOIN_INDEX = "join_index"
    }
}
