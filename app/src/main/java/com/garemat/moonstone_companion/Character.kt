package com.garemat.moonstone_companion

import androidx.room.Entity
import androidx.room.PrimaryKey
import kotlinx.serialization.Serializable

@Serializable
enum class Faction {
    COMMONWEALTH, DOMINION, LESHAVULT, SHADES
}

@Serializable
data class ActiveAbility(
    val name: String,
    val cost: Int,
    val range: String = "",
    val description: String = "",
    val oncePerTurn: Boolean = false,
    val oncePerGame: Boolean = false
)

@Serializable
data class ArcaneAbility(
    val name: String,
    val cost: Int,
    val range: String = "",
    val description: String = "",
    val oncePerTurn: Boolean = false,
    val oncePerGame: Boolean = false,
    val reloadable: Boolean = false
)

@Serializable
data class SignatureMove(
    val name: String,
    val upgradeFrom: String,
    val results: List<SignatureResultEntry>,
    val damageType: String? = null, // Slicing, Piercing, Impact, Magical
    val passiveEffect: String? = null,
    val endStepEffect: String? = null
)

@Serializable
data class SignatureResultEntry(
    val opponentPlay: String,
    val deal: String,
    val isFollowUp: Boolean = false
)

@Entity
@Serializable
data class Character(
    @PrimaryKey(autoGenerate = true)
    val id: Int = 0,
    val version: String = "",
    val name: String,
    val factions: List<Faction>,
    val tags: List<String>,
    val melee: Int,
    val meleeRange: Int,
    val arcane: Int,
    val evade: String,
    val health: Int,
    val energyTrack: List<Int>, 
    val passiveAbilities: List<PassiveAbility>,
    val activeAbilities: List<ActiveAbility>,
    val arcaneAbilities: List<ArcaneAbility>,
    val signatureMove: SignatureMove,
    val baseSize: String = "30mm",
    val imageName: String? = null,
    val shareCode: String = "AAA",

    val impactDamageBuff: Int = 0,
    val slicingDamageBuff: Int = 0,
    val piercingDamageBuff: Int = 0,
    val dealsMagicalDamage: Boolean = false,
    val impactDamageMitigation: Int = 0,
    val slicingDamageMitigation: Int = 0,
    val piercingDamageMitigation: Int = 0,
    val allDamageMitigation: Int = 0,
    val magicalDamageMitigation: Int = 0
)

@Serializable
data class PassiveAbility(
    val name: String,
    val description: String,
    val oncePerTurn: Boolean = false,
    val oncePerGame: Boolean = false
)

@Entity
@Serializable
data class Troupe(
    @PrimaryKey(autoGenerate = true)
    val id: Int = 0,
    val troupeName: String,
    val faction: Faction,
    val characterIds: List<Int>,
    val shareCode: String
)
