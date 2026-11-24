package com.oneorigin.originagent

import com.oneorigin.originagent.memory.ProvenanceLedger
import com.oneorigin.originagent.util.Hash
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

class ProvenanceLedgerTest {

    @Test
    fun `chain verifies after appends and fails after tampering`() = runBlocking {
        val dir = File(System.getProperty("java.io.tmpdir"), "origin-test-${System.nanoTime()}")
        val ledger = ProvenanceLedger(dir)
        assertTrue(ledger.verify())

        ledger.append("intent.received", "test", "intent_1", "hello")
        ledger.append("plan.created", "test", "plan_1", "calculator")
        assertTrue(ledger.verify())

        val file = File(File(dir, "origin"), "provenance.jsonl")
        val tampered = file.readText().replace("hello", "hacked")
        file.writeText(tampered)
        assertFalse(ledger.verify())
    }

    @Test
    fun `malformed ledger line fails closed`() = runBlocking {
        val dir = File(System.getProperty("java.io.tmpdir"), "origin-test-malformed-${System.nanoTime()}")
        val ledger = ProvenanceLedger(dir)
        ledger.append("intent.received", "test", "intent_1", "hello")
        val file = File(File(dir, "origin"), "provenance.jsonl")
        file.appendText("{not valid json}\n")
        assertFalse(ledger.verify())
    }

    @Test
    fun `hash chain payload verification detects reorder`() {
        val first = Hash.chainPayload("a", "b", "c", "d", 1L, "0000000000000000")
        val second = Hash.chainPayload("a", "b", "c", "d", 2L, Hash.fnv1a64(first))
        val good = listOf(
            Hash.serialize(first, Hash.fnv1a64(first)),
            Hash.serialize(second, Hash.fnv1a64(second))
        )
        assertTrue(Hash.verifySerializedChain(good))
        assertFalse(Hash.verifySerializedChain(good.asReversed()))
    }
}
