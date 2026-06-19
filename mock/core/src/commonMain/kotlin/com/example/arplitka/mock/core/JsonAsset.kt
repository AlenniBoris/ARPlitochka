package com.example.arplitka.mock.core

data class JsonAsset(val path: String) {
    fun read(): String = AssetReader.readAsset(path)
    
    companion object
}

val JsonAsset.Companion.emptyJson get() = JsonAsset("common/empty.json")
val JsonAsset.Companion.emptyJsonObject get() = JsonAsset("common/empty_object.json")
val JsonAsset.Companion.emptyList get() = JsonAsset("common/empty_list.json")
