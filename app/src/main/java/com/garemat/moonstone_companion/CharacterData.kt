package com.garemat.moonstone_companion

import android.content.Context
import kotlinx.serialization.json.Json

object CharacterData {
    fun getCharactersFromAssets(context: Context): List<Character> {
        val characters = mutableListOf<Character>()
        val json = Json { 
            ignoreUnknownKeys = true 
        }
        
        try {
            val files = context.assets.list("characters") ?: return emptyList()
            
            for (fileName in files) {
                if (fileName.endsWith(".json")) {
                    val jsonString = context.assets.open("characters/$fileName").bufferedReader().use { it.readText() }
                    val character = json.decodeFromString<Character>(jsonString)
                    characters.add(character)
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
        
        return characters
    }
}
