package idv.mark.platform.core.model;

import java.util.List;
import java.util.Map;

public record WorkLease(String taskId, String stageId, String attemptId, String leaseToken,
                        long leaseUntil, String type, TaskSpec task, Map<String, Object> parameters,
                        List<Map<String, Object>> inputs, Map<String, Object> dependencyResults) {}
