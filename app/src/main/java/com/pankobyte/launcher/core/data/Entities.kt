package com.pankobyte.launcher.core.data

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(tableName = "kid_profile")
data class KidProfile(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val colour: Int,
    /** Minutes, or -1 for no limit. */
    val timeLimitMin: Int,
    val allowedFromMin: Int? = null,
    val allowedToMin: Int? = null,
)

/** One app the child may open. */
@Entity(tableName = "kid_app", primaryKeys = ["profileId", "packageName"])
data class KidApp(
    val profileId: Long,
    val packageName: String,
)

@Entity(tableName = "kid_contact", indices = [Index("profileId")])
data class KidContact(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val profileId: Long,
    val displayName: String,
    val number: String,
    val photoUri: String? = null,
)

@Entity(tableName = "kid_session", indices = [Index("profileId")])
data class KidSession(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val profileId: Long,
    val startedAt: Long,
    val endedAt: Long? = null,
)

@Entity(tableName = "kid_session_app", primaryKeys = ["sessionId", "packageName"])
data class KidSessionApp(
    val sessionId: Long,
    val packageName: String,
    val foregroundMs: Long,
)
