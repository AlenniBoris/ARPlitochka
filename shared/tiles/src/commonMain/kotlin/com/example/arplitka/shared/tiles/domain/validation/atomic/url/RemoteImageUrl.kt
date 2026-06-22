package com.example.arplitka.shared.tiles.domain.validation.atomic.url

private val remoteImageUrlValidator = hasHttpOrHttpsPrefixValidator()

fun isRemoteImageUrl(url: String): Boolean = remoteImageUrlValidator(url).isValid()
