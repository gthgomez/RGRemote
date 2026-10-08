package com.rgremote.app.ui.controller

import java.util.concurrent.atomic.AtomicLong

/**
 * Generation gate for hold-repeat D-pad commands. Each press sequence takes the
 * current generation token; ending or cancelling the sequence bumps the
 * generation so commands the repeat loop already queued behind the serialized
 * command queue become stale and are dropped on dequeue instead of navigating
 * after the finger lifted.
 */
class DpadSequenceGate {
    private val generation = AtomicLong(0L)

    /** Token for a new press sequence; also invalidates any earlier sequence. */
    fun beginSequence(): Long = generation.incrementAndGet()

    /** Drop every queued command from earlier sequences; returns the new generation. */
    fun cancelActiveSequence(): Long = generation.incrementAndGet()

    fun isValid(token: Long): Boolean = generation.get() == token
}
