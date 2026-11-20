package com.oneorigin.originagent.util

import java.util.UUID

object Ids {
    fun next(prefix: String = "o"): String = "${prefix}_${System.currentTimeMillis().toString(36)}_${UUID.randomUUID().toString().take(8)}"
}
