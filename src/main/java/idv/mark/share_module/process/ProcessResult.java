package idv.mark.share_module.process;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class ProcessResult {
    private Long taskId;
    private int exitCode;
    private String stdout;
    private String stderr;

    public boolean isSuccess() {
        return exitCode == 0;
    }
}
