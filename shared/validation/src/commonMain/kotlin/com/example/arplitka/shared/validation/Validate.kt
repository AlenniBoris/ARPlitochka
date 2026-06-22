package com.example.arplitka.shared.validation

fun interface Validate<F, A> : (A) -> Validated<F, A> {
    override fun invoke(value: A): Validated<F, A>
}

fun <F, A> validate(failure: F, predicate: (A) -> Boolean): Validate<F, A> =
    Validate { value ->
        if (predicate(value)) {
            valid(value)
        } else {
            invalid(failure)
        }
    }
