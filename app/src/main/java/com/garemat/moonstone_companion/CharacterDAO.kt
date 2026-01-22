package com.garemat.moonstone_companion

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Query
import androidx.room.Upsert
import kotlinx.coroutines.flow.Flow

@Dao
interface CharacterDAO {

    // Character Operations
    @Upsert
    suspend fun upsertCharacter(character: Character)

    @Delete
    suspend fun deleteCharacter(character: Character)

    @Query("SELECT * FROM character ORDER BY name ASC")
    fun getCharactersOrderedByName(): Flow<List<Character>>

    @Query("SELECT * FROM character WHERE id IN (:ids)")
    fun getCharactersByIds(ids: List<Int>): Flow<List<Character>>

    // Troupe Operations
    @Upsert
    suspend fun upsertTroupe(troupe: Troupe)

    @Delete
    suspend fun deleteTroupe(troupe: Troupe)

    @Query("SELECT * FROM troupe ORDER BY troupeName ASC")
    fun getTroupes(): Flow<List<Troupe>>

    @Query("SELECT * FROM troupe WHERE shareCode = :code")
    suspend fun getTroupeByShareCode(code: String): Troupe?
}
