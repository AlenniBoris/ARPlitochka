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

        var resolvedPath = NSBundle.mainBundle.pathForResource(
            name = name,
            ofType = extension,
            inDirectory = "Resources/$directory".removeSuffix("/")
        )

        if (resolvedPath == null) {
            resolvedPath = NSBundle.mainBundle.pathForResource(
                name = name,
                ofType = extension,
                inDirectory = directory.ifEmpty { null }
            )
        }

        if (resolvedPath == null) {
            resolvedPath = NSBundle.mainBundle.pathForResource(name, extension)
        }

        if (resolvedPath == null) {
            val enumerator = NSFileManager.defaultManager.enumeratorAtPath(NSBundle.mainBundle.bundlePath)
            var nextFile = enumerator?.nextObject() as? String
            while (nextFile != null) {
                if (nextFile.endsWith(normalizedPath)) {
                    resolvedPath = NSBundle.mainBundle.bundlePath + "/" + nextFile
                    break
                }
                nextFile = enumerator?.nextObject() as? String
            }
        }
        return resolvedPath
    }
}
