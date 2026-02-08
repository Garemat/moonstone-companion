package com.garemat.moonstone_companion

import androidx.room.TypeConverter
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

class Converters {
    @TypeConverter
    fun fromFactionList(value: List<Faction>) = Json.encodeToString(value)

    @TypeConverter
    fun toFactionList(value: String) = Json.decodeFromString<List<Faction>>(value)

    @TypeConverter
    fun fromStringList(value: List<String>) = Json.encodeToString(value)

    @TypeConverter
    fun toStringList(value: String) = Json.decodeFromString<List<String>>(value)

    @TypeConverter
    fun fromActiveAbilityList(value: List<ActiveAbility>) = Json.encodeToString(value)

    @TypeConverter
    fun toActiveAbilityList(value: String) = Json.decodeFromString<List<ActiveAbility>>(value)

    @TypeConverter
    fun fromArcaneAbilityList(value: List<ArcaneAbility>) = Json.encodeToString(value)

    @TypeConverter
    fun toArcaneAbilityList(value: String) = Json.decodeFromString<List<ArcaneAbility>>(value)

    @TypeConverter
    fun fromSignatureMove(value: SignatureMove) = Json.encodeToString(value)

    @TypeConverter
    fun toSignatureMove(value: String) = Json.decodeFromString<SignatureMove>(value)

    @TypeConverter
    fun fromIntList(value: List<Int>) = Json.encodeToString(value)

    @TypeConverter
    fun toIntList(value: String) = Json.decodeFromString<List<Int>>(value)
    
    @TypeConverter
    fun fromFaction(value: Faction) = value.name
    
    @TypeConverter
    fun toFaction(value: String) = Faction.valueOf(value)

    @TypeConverter
    fun fromPassiveAbilityList(value: List<PassiveAbility>) = Json.encodeToString(value)

    @TypeConverter
    fun toPassiveAbilityList(value: String) = Json.decodeFromString<List<PassiveAbility>>(value)

    @TypeConverter
    fun fromPlayerStatList(value: List<PlayerStat>) = Json.encodeToString(value)

    @TypeConverter
    fun toPlayerStatList(value: String) = Json.decodeFromString<List<PlayerStat>>(value)
}
