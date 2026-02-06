package com.garemat.moonstone_companion

import android.content.Context
import android.util.Log
import kotlinx.serialization.json.Json

object CharacterData {
    fun getCharactersFromAssets(context: Context): List<Character> {
        val characters = mutableListOf<Character>()
        val json = Json { 
            ignoreUnknownKeys = true 
            coerceInputValues = true // Helps with enum mismatches or nulls
        }
        
        try {
            val files = context.assets.list("characters") ?: return emptyList()
            
            for (fileName in files) {
                if (fileName.endsWith(".json")) {
                    try {
                        val jsonString = context.assets.open("characters/$fileName").bufferedReader().use { it.readText() }
                        val character = json.decodeFromString<Character>(jsonString)
                        characters.add(character)
                    } catch (e: Exception) {
                        // Log the specific file that failed to help the user debug
                        Log.e("CharacterData", "Failed to parse character file: $fileName", e)
                    }
                }
            }
        } catch (e: Exception) {
            Log.e("CharacterData", "Error listing character assets", e)
        }
        
        return characters
    }
}
