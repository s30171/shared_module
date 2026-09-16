package idv.mark.platform.core.process;

/** Percent is nullable: neither a running process nor GPU utilization proves progress. */
public record Progress(String phase, Double percent, Long processedMillis, Long bytes,
                       Double bytesPerSecond, Long etaSeconds, String detail) {}
