package com.oneorigin.originagent.tools

import com.oneorigin.originagent.domain.RiskLevel
import com.oneorigin.originagent.domain.ToolResult

interface Tool {
    val id: String
    val displayName: String
    val description: String
    val risk: RiskLevel
    val permission: String
    suspend fun execute(input: Map<String, String>): ToolResult
}

/** Only registered tools can execute. Unknown tool IDs are rejected by the harness. */
class ToolRegistry(tools: List<Tool>) {
    private val toolsById = tools.associateBy { it.id }

    fun get(id: String): Tool? = toolsById[id]
    fun all(): List<Tool> = toolsById.values.toList()
}
