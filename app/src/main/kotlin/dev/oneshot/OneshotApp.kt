package dev.oneshot

import android.app.Application

/**
 * Host for [AppGraph] once M1 wires the camera stack. Intentionally empty now — the
 * capability probe and parameter store live in M1, not here.
 */
class OneshotApp : Application()
