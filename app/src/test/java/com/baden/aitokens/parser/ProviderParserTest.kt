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
    fun copilot_parsesAiCreditUsage() {
        val json = """
            {"timePeriod":{"year":2026,"month":9},"user":"baden","usageItems":[
              {"product":"Copilot AI Credits","sku":"AI Credit","model":"GPT-5","unitType":"ai-credits","pricePerUnit":0.01,"grossQuantity":100,"grossAmount":1.0,"netQuantity":100,"netAmount":1.0},
              {"product":"Copilot AI Credits","sku":"AI Credit","model":"Claude Opus 5","unitType":"ai-credits","pricePerUnit":0.01,"grossQuantity":50,"grossAmount":0.5,"netQuantity":50,"netAmount":0.5}
            ]}
        """.trimIndent()

        val usage = CopilotParser.parse("acc-5", json, planLimit = 7_000.0)

        assertEquals(Provider.COPILOT, usage.provider)
        assertEquals(1, usage.windows.size)
        val window = usage.windows[0]
        assertEquals("AI credits (місяць)", window.title)
        assertEquals(150.0, window.used!!, 0.001)
        assertEquals(7_000.0, window.total!!, 0.001)
        assertEquals(97.857, window.percentRemaining!!, 0.01)
        assertTrue(window.note!!.contains("GPT-5"))
        assertTrue(window.note!!.contains("$1.50"))
    }

    @Test
    fun copilot_emptyUsageUsesPlanLimit() {
        val json = """
            {"timePeriod":{"year":2026,"month":9},"user":"baden","usageItems":[]}
        """.trimIndent()

        val usage = CopilotParser.parse("acc-6", json, planLimit = 7_000.0)

        val window = usage.windows[0]
        assertEquals(0.0, window.used!!, 0.001)
        assertEquals(7_000.0, window.total!!, 0.001)
        assertEquals(100.0, window.percentRemaining!!, 0.001)
        assertTrue(window.note!!.contains("не витрачено"))
    }

    @Test
    fun copilot_usesPlanLimitWhenApiOmitsLimit() {
        val json = """
            {"timePeriod":{"year":2026,"month":9},"user":"baden","usageItems":[
              {"model":"GPT-5","netQuantity":700,"grossQuantity":700}
            ]}
        """.trimIndent()

        val usage = CopilotParser.parse("acc-7", json, planLimit = 7_000.0)

        val window = usage.windows[0]
        assertEquals(700.0, window.used!!, 0.001)
        assertEquals(7_000.0, window.total!!, 0.001)
        assertEquals(90.0, window.percentRemaining!!, 0.001)
    }
}
