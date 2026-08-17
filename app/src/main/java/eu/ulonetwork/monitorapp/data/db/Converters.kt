package eu.ulonetwork.monitorapp.data.db

import androidx.room.TypeConverter

class Converters {
    @TypeConverter
    fun fromMatchMode(mode: MatchMode): String = mode.name

    @TypeConverter
    fun toMatchMode(value: String): MatchMode = MatchMode.valueOf(value)

    @TypeConverter
    fun fromAlertEventType(type: AlertEventType): String = type.name

    @TypeConverter
    fun toAlertEventType(value: String): AlertEventType = AlertEventType.valueOf(value)
}
