package idv.mark.share_module.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class SRTModel {
    private Integer sequence;
    private String time;
    private String text;
    private String lineBreak = "\n";
}
