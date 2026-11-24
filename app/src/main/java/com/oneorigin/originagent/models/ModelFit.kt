package com.oneorigin.originagent.models

/**
 * Pure, device-side fit hints. Pure JVM logic: no Android classes and no I/O, so it can be
 * unit-tested directly. All figures are conservative estimates, never measured RAM usage.
 */

/** Snapshot of device memory in MiB. Values are supplied by the caller; nothing is probed here. */
data class DeviceMemory(val totalMb: Long, val availableMb: Long)

/** Coarse fit hint. Deliberately avoids promising exact memory requirements. */
enum class FitGrade { GREAT, OKAY, TIGHT }

private const val MEGABYTE_BYTES = 1024L * 1024L
/** Runtime, tokenizer and allocator overhead that is not part of the weight file. */
private const val ENGINE_OVERHEAD_MB = 96L
/** KV cache plus scratch buffers per 1024 tokens of configured context. */
private const val CONTEXT_OVERHEAD_MB_PER_1024_TOKENS = 48L
/** Extra working-set padding for transient buffers and fragmentation (percent). */
private const val WORKING_SET_PADDING_PERCENT = 10L
/** Reserved for the OS and foreground services before a model is suggested. */
private const val SAFETY_MARGIN_MB = 64L

private fun ceilDiv(value: Long, divisor: Long): Long = (value + divisor - 1L) / divisor

/**
 * Object facade so callers can use `ModelFit.label(...)` / `ModelFit.recommendedOrder(...)`.
 * The equivalent top-level functions below are pure and are used by JVM unit tests.
 */
object ModelFit {

    /**
     * Conservative *estimate* of the runtime footprint in MiB: rounded-up weight-file size,
     * 10% transient working-set padding, engine overhead, and KV-cache/scratch overhead scaled
     * by the model's context window. This is a hint for ordering and labels, not a guarantee:
     * actual usage depends on the OS, runtime build and conversation length.
     */
    fun estimateRuntimeMb(spec: DownloadableModelSpec): Long {
        val fileMb = ceilDiv(spec.sizeBytes, MEGABYTE_BYTES)
        val workingSetMb = ceilDiv(fileMb * WORKING_SET_PADDING_PERCENT, 100L)
        val contextBlocks = ceilDiv(spec.contextSize.toLong(), 1024L)
        return fileMb + workingSetMb + ENGINE_OVERHEAD_MB +
            contextBlocks * CONTEXT_OVERHEAD_MB_PER_1024_TOKENS
    }

    /**
     * Grades a model against the *available* memory reported by [device]. The available figure
     * is clamped to total memory and reduced by a small safety margin; a further quarter of
     * the remaining memory is kept as headroom for a GREAT grade. Unknown or nonsensical
     * readings degrade to [FitGrade.TIGHT] rather than over-promising.
     */
    fun grade(spec: DownloadableModelSpec, device: DeviceMemory): FitGrade {
        if (device.totalMb <= 0L || device.availableMb <= 0L) return FitGrade.TIGHT
        val usable = (minOf(device.totalMb, device.availableMb) - SAFETY_MARGIN_MB).coerceAtLeast(0L)
        if (usable == 0L) return FitGrade.TIGHT
        val comfortable = usable - usable / 4L
        val estimate = estimateRuntimeMb(spec)
        return when {
            estimate <= comfortable -> FitGrade.GREAT
            estimate <= usable -> FitGrade.OKAY
            else -> FitGrade.TIGHT
        }
    }

    /**
     * Deterministic suggestion order: best fit first, then the everyday recommendation, then
     * the smallest artifact, then model id as a stable tie-breaker. Input order never matters.
     */
    fun recommendedOrder(
        specs: List<DownloadableModelSpec>,
        device: DeviceMemory
    ): List<DownloadableModelSpec> = specs.sortedWith(
        compareBy<DownloadableModelSpec> { grade(it, device).ordinal }
            .thenByDescending { it.recommended }
            .thenBy { it.sizeBytes }
            .thenBy { it.modelId }
    )

    /** Human-facing hint with an approximate figure; it never claims exact RAM usage. */
    fun label(spec: DownloadableModelSpec, device: DeviceMemory): String {
        val headline = when (grade(spec, device)) {
            FitGrade.GREAT -> "Fits comfortably"
            FitGrade.OKAY -> "Workable"
            FitGrade.TIGHT -> "May be tight"
        }
        return "$headline · ~${estimateRuntimeMb(spec)} MB estimated RAM"
    }
}

/** Top-level alias of [ModelFit.estimateRuntimeMb]. */
fun estimateRuntimeMb(spec: DownloadableModelSpec): Long = ModelFit.estimateRuntimeMb(spec)

/** Top-level alias of [ModelFit.grade]. */
fun grade(spec: DownloadableModelSpec, device: DeviceMemory): FitGrade = ModelFit.grade(spec, device)

/** Top-level alias of [ModelFit.recommendedOrder]. */
fun recommendedOrder(
    specs: List<DownloadableModelSpec>,
    device: DeviceMemory
): List<DownloadableModelSpec> = ModelFit.recommendedOrder(specs, device)

/** Top-level alias of [ModelFit.label]. */
fun label(spec: DownloadableModelSpec, device: DeviceMemory): String = ModelFit.label(spec, device)
