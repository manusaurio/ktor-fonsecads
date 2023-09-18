package ar.pelotude.ktorfds.msgs

/**
 * Abstraction for a message with an underlying [value].
 *
 * A message is composed by two templates, two fillers and a conjunction.
 * How these parts are handled is up to the implementation.
 */
interface Message<T, out U> {
    val value: T

    val template1: Template<U>
    val filler1: Filler<U>
    val conjunction: Conjunction<U>
    val template2: Template<U>
    val filler2: Filler<U>
}

interface MessagePart<out T> {
    val value: T
    val text: String
}

interface Category<out T> {
    val id: T
    val name: String
}

interface Template<out T>: MessagePart<T>

interface Filler<out T>: MessagePart<T> {
    val category: Category<T>
}

interface Conjunction<out T>: MessagePart<T>

/**
 * A provider of [Message]s. This abstraction is meant to be implemented
 * by classes that define the validation of [Message]s.
 */
interface Messenger<T, out U> {
    fun create(value: T): Message<T, U>

    fun quickCheck(value: T): Boolean
}