package org.schabi.newpipe.database.subscription

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Ignore
import androidx.room.Index
import androidx.room.PrimaryKey
import org.schabi.newpipe.database.subscription.SubscriptionEntity.Companion.SUBSCRIPTION_SERVICE_ID
import org.schabi.newpipe.database.subscription.SubscriptionEntity.Companion.SUBSCRIPTION_TABLE
import org.schabi.newpipe.database.subscription.SubscriptionEntity.Companion.SUBSCRIPTION_URL
import org.schabi.newpipe.extractor.channel.ChannelInfo
import org.schabi.newpipe.extractor.channel.ChannelInfoItem
import java.io.Serializable

@Entity(
    tableName = SUBSCRIPTION_TABLE,
    indices = [
        Index(value = [SUBSCRIPTION_SERVICE_ID, SUBSCRIPTION_URL], unique = true)
    ]
)
data class SubscriptionEntity(
    @PrimaryKey(autoGenerate = true)
    var uid: Long = 0,

    @ColumnInfo(name = SUBSCRIPTION_SERVICE_ID)
    var serviceId: Int,

    @ColumnInfo(name = SUBSCRIPTION_URL)
    var url: String? = null,

    @ColumnInfo(name = SUBSCRIPTION_NAME)
    var name: String? = null,

    @ColumnInfo(name = SUBSCRIPTION_AVATAR_URL)
    var avatarUrl: String? = null,

    @ColumnInfo(name = SUBSCRIPTION_SUBSCRIBER_COUNT)
    var subscriberCount: Long? = null,

    @ColumnInfo(name = SUBSCRIPTION_DESCRIPTION)
    var description: String? = null,

    @get:NotificationMode
    @ColumnInfo(name = SUBSCRIPTION_NOTIFICATION_MODE)
    var notificationMode: Int = 0
) : Serializable {
    @Ignore
    constructor(info: ChannelInfo) : this(
        serviceId = info.serviceId,
        url = info.url,
        name = info.name,
        avatarUrl = info.avatarUrl,
        description = info.description,
        subscriberCount = info.subscriberCount
    )

    @Ignore
    fun setData(n: String?, au: String?, d: String?, sc: Long?) {
        name = n
        avatarUrl = au
        description = d
        subscriberCount = sc
    }

    @Ignore
    fun toChannelInfoItem(): ChannelInfoItem {
        val item = ChannelInfoItem(serviceId, url, name)
        item.thumbnailUrl = avatarUrl
        subscriberCount?.let {
            item.subscriberCount = it
        }
        item.description = description
        return item
    }

    companion object {
        const val SUBSCRIPTION_UID = "uid"
        const val SUBSCRIPTION_TABLE = "subscriptions"
        const val SUBSCRIPTION_SERVICE_ID = "service_id"
        const val SUBSCRIPTION_URL = "url"
        const val SUBSCRIPTION_NAME = "name"
        const val SUBSCRIPTION_AVATAR_URL = "avatar_url"
        const val SUBSCRIPTION_SUBSCRIBER_COUNT = "subscriber_count"
        const val SUBSCRIPTION_DESCRIPTION = "description"
        const val SUBSCRIPTION_NOTIFICATION_MODE = "notification_mode"

        @Ignore
        fun from(info: ChannelInfo): SubscriptionEntity {
            return SubscriptionEntity(
                serviceId = info.serviceId,
                url = info.url,
                name = info.name,
                avatarUrl = info.avatarUrl,
                description = info.description,
                subscriberCount = info.subscriberCount
            )
        }
    }
}
