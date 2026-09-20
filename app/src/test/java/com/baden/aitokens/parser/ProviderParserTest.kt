package com.baden.aitokens.parser

import com.baden.aitokens.data.model.Provider
import com.baden.aitokens.data.remote.provider.CopilotParser
import com.baden.aitokens.data.remote.provider.DeepSeekParser
import com.baden.aitokens.data.remote.provider.MiniMaxParser
import com.baden.aitokens.data.remote.provider.ZaiParser
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class ProviderParserTest {

    @Test
    fun deepSeek_parsesToppedUpBalance() {
        val json = """
            {"is_available":true,"balance_infos":[
              {"currency":"CNY","total_balance":"110.00","granted_balance":"10.00","topped_up_balance":"100.00"}
            ]}
        """.trimIndent()

        val usage = DeepSeekParser.parse("acc-1", json)

        assertEquals(Provider.DEEPSEEK, usage.provider)
        assertNull(usage.error)
        assertNotNull(usage.balance)
        val balance = usage.balance!!
        assertEquals("CNY", balance.currency)
        assertEquals(100.0, balance.toppedUp, 0.001)
        assertEquals(10.0, balance.granted, 0.001)
        assertEquals(110.0, balance.total, 0.001)
        assertTrue(balance.available)
    }

    @Test
    fun zai_parsesTokenAndMcpLimits() {
        val json = """
            {"code":200,"msg":"success","success":true,"data":{"limits":[
              {"type":"TOKENS_LIMIT","usage":10000000,"currentValue":500000,"percentage":5,"nextResetTime":1706200000000},
              {"type":"TIME_LIMIT","usage":100,"currentValue":10,"percentage":10}
            ]}}
        """.trimIndent()

        val usage = ZaiParser.parse("acc-2", json)

        assertEquals(Provider.ZAI, usage.provider)
        assertEquals(2, usage.windows.size)
        val tokens = usage.windows[0]
        assertEquals(95.0, tokens.percentRemaining!!, 0.001)
        assertEquals(1706200000000L, tokens.resetAtMillis)
        val mcp = usage.windows[1]
        assertEquals(90.0, mcp.percentRemaining!!, 0.001)
        assertNull(mcp.resetAtMillis)
    }

    @Test
    fun miniMax_parsesIntervalAndWeeklyWindows() {
        val json = """
            {"model_remains":[{"model_name":"MiniMax-M2",
              "current_interval_usage_count":120,
              "current_interval_total_count":1000,
              "current_interval_remaining_percent":88.0,
              "current_weekly_usage_count":300,
              "current_weekly_total_count":5000,
              "current_weekly_remaining_percent":94.0,
              "end_time":1706200000000,
              "weekly_end_time":1706800000000}]}
        """.trimIndent()

        val usage = MiniMaxParser.parse("acc-3", json)

        assertEquals(Provider.MINIMAX, usage.provider)
        assertEquals(2, usage.windows.size)
        assertEquals("5-годинне вікно", usage.windows[0].title)
        assertEquals(88.0, usage.windows[0].percentRemaining!!, 0.001)
        assertEquals(1706200000000L, usage.windows[0].resetAtMillis)
        assertEquals("Тижневе вікно", usage.windows[1].title)
        assertEquals(94.0, usage.windows[1].percentRemaining!!, 0.001)
    }

    @Test
    fun miniMax_convertsSecondsEndTime() {
        val json = """
            {"model_remains":[{"model_name":"MiniMax-M2","end_time":1706200000}]}
        """.trimIndent()

        val usage = MiniMaxParser.parse("acc-4", json)

        assertEquals(1706200000000L, usage.windows[0].resetAtMillis)
    }

    @Test
    fun copilot_aggregatesPremiumRequests() {
        val json = """
            {"user":"octocat","timePeriod":{"year":2026,"month":1},"usageItems":[
              {"product":"copilot","sku":"Copilot Premium Request","model":"gpt-4","unitType":"request","grossQuantity":120,"netQuantity":120,"limit":300},
              {"product":"copilot","sku":"Copilot Premium Request","model":"claude-3.5-sonnet","unitType":"request","grossQuantity":30,"netQuantity":30,"limit":300}
            ]}
        """.trimIndent()

        val usage = CopilotParser.parse("acc-5", json)

        assertEquals(Provider.COPILOT, usage.provider)
        assertEquals(1, usage.windows.size)
        val window = usage.windows[0]
        assertEquals(150.0, window.used!!, 0.001)
        assertEquals(300.0, window.total!!, 0.001)
        assertEquals(50.0, window.percentRemaining!!, 0.001)
    }
}
