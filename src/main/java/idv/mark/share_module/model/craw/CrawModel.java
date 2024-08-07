package idv.mark.share_module.model.craw;

import lombok.Data;

@Data
public class CrawModel {
    private String crawToolEnum;
    private String url;
    private String crawSelector;
    private String returnHtml;
}
