package com.example.arplitka.mock.core

import platform.Foundation.*

actual object AssetReader {
    actual fun readAsset(path: String): String {
        val normalizedPath = path.removePrefix("/")
        val fileName = normalizedPath.substringAfterLast("/")
        val directory = normalizedPath.substringBeforeLast("/", missingDelimiterValue = "")
        val name = fileName.substringBeforeLast(".", missingDelimiterValue = fileName)
        val extension = fileName.substringAfterLast(".", missingDelimiterValue = "")
        
        val resolvedPath = NSBundle.mainBundle.pathForResource(
            name = name,
            ofType = extension,
            inDirectory = directory.ifEmpty { null }
        ) ?: throw IllegalStateException("Asset not found: $path")
        
        return NSString.stringWithContentsOfFile(resolvedPath, encoding = NSUTF8StringEncoding, error = null) ?: ""
    }
}
