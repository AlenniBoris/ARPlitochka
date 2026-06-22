package com.example.arplitka.shared.validation

sealed interface Validated<out F, out A> {
    data class Valid<A>(val value: A) : Validated<Nothing, A>
    data class Invalid<F>(val failure: F) : Validated<F, Nothing>

    fun isValid(): Boolean = this is Valid

    fun onValid(action: (A) -> Unit): Validated<F, A> {
        if (this is Valid) action(value)
        return this
    }

    fun onInvalid(action: (F) -> Unit): Validated<F, A> {
        if (this is Invalid) action(failure)
        return this
    }
}

fun <A> valid(value: A): Validated<Nothing, A> = Validated.Valid(value)

fun <F> invalid(failure: F): Validated<F, Nothing> = Validated.Invalid(failure)
