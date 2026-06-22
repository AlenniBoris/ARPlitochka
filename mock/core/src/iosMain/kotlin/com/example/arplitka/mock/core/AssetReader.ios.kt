package com.example.arplitka.mock.core

import kotlinx.cinterop.ExperimentalForeignApi
import platform.Foundation.NSBundle
import platform.Foundation.NSFileManager
import platform.Foundation.NSString
import platform.Foundation.NSUTF8StringEncoding
import platform.Foundation.stringWithContentsOfFile

actual object AssetReader {
    @OptIn(ExperimentalForeignApi::class)
    actual fun readAsset(path: String): String {
        val resolvedPath = resolvePath(path)

        if (resolvedPath == null) {
            println("ASSET ERROR: Asset not found at $path")
            return "{\"items\": []}"
        }

        val content = NSString.stringWithContentsOfFile(resolvedPath, encoding = NSUTF8StringEncoding, error = null)
        return content ?: "{\"items\": []}"
    }

    actual fun resolveAssetPath(path: String): String? {
        val resolved = resolvePath(path) ?: return null
        val url = "file://$resolved"
        println("ASSET DEBUG: Resolved $path to absolute path: $url")
        return url
    }

    private fun resolvePath(path: String): String? {
        val normalizedPath = path.removePrefix("/")
        val fileName = normalizedPath.substringAfterLast("/")
        val directory = normalizedPath.substringBeforeLast("/", missingDelimiterValue = "")
        val name = fileName.substringBeforeLast(".", missingDelimiterValue = fileName)
        val extension = fileName.substringAfterLast(".", missingDelimiterValue = "")

        println("ASSET DEBUG: Searching for $name.$extension in directory=$directory")

        // 1. Try with full directory path (as preserved by folder references)
        var resolvedPath = NSBundle.mainBundle.pathForResource(
            name = name,
            ofType = extension,
            inDirectory = directory.ifEmpty { null }
        )

        // 2. Try in Resources/ directory (common in some KMP setups)
        if (resolvedPath == null) {
            resolvedPath = NSBundle.mainBundle.pathForResource(
                name = name,
                ofType = extension,
                inDirectory = "Resources/$directory".removeSuffix("/")
            )
        }

        // 3. Try in root of bundle (if Xcode flattened the folder)
        if (resolvedPath == null) {
            resolvedPath = NSBundle.mainBundle.pathForResource(name, extension)
        }

        // 4. Recursive search (last resort)
        if (resolvedPath == null) {
            val bundlePath = NSBundle.mainBundle.bundlePath
            println("ASSET DEBUG: Starting recursive search in $bundlePath")
            val enumerator = NSFileManager.defaultManager.enumeratorAtPath(bundlePath)
            var nextFile = enumerator?.nextObject() as? String
            var count = 0
            while (nextFile != null) {
                // Log first 100 files to see structure (only for debugging)
                if (count < 100) {
                    println("ASSET DEBUG: Bundle file: $nextFile")
                }

                // Check for exact match or just filename match if flattened
                if (nextFile.endsWith(normalizedPath) || nextFile == fileName || nextFile.endsWith("/$fileName")) {
                    resolvedPath = "$bundlePath/$nextFile"
                    println("ASSET DEBUG: Found via recursion: $resolvedPath")
                    break
                }
                nextFile = enumerator?.nextObject() as? String
                count++
                if (count > 2000) break // Safety break
            }
        }

        if (resolvedPath == null) {
            println("ASSET DEBUG: Failed to find $path")
        }

        return resolvedPath
    }
}
