package com.example.arplitka.shared.tiles.domain.validation.atomic.url

import com.example.arplitka.shared.tiles.domain.entity.error.ImageUrlValidationError
import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class HasHttpOrHttpsPrefixValidatorTest {

    private val validator = hasHttpOrHttpsPrefixValidator()

    @Test
    fun `returns valid for http url`() {
        assertTrue(validator("http://example.com/image.png").isValid())
    }

    @Test
    fun `returns valid for https url`() {
        assertTrue(validator("https://example.com/image.png").isValid())
    }

    @Test
    fun `returns invalid for file url`() {
        assertFalse(validator("file:///path/to/image.png").isValid())
    }

    @Test
    fun `returns invalid for relative path`() {
        assertFalse(validator("mock/tiles/previews/image.png").isValid())
    }

    @Test
    fun `returns invalid failure type`() {
        var failure: ImageUrlValidationError? = null
        validator("file://image.png").onInvalid { failure = it }
        assertTrue(failure == ImageUrlValidationError.NOT_HTTP_OR_HTTPS)
    }
}
