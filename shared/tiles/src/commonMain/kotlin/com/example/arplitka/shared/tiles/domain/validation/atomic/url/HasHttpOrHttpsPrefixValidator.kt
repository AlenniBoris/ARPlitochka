package com.example.arplitka.shared.tiles.domain.validation.atomic.url

import com.example.arplitka.shared.tiles.domain.entity.error.ImageUrlValidationError
import com.example.arplitka.shared.validation.Validate
import com.example.arplitka.shared.validation.validate

private const val HTTP_PREFIX = "http://"
private const val HTTPS_PREFIX = "https://"

fun hasHttpOrHttpsPrefixValidator(
    failure: ImageUrlValidationError = ImageUrlValidationError.NOT_HTTP_OR_HTTPS,
): Validate<ImageUrlValidationError, String> =
    validate(failure) { url ->
        url.startsWith(HTTP_PREFIX) || url.startsWith(HTTPS_PREFIX)
    }
