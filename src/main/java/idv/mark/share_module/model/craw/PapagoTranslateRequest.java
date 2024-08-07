package idv.mark.share_module.model.craw;

import lombok.Data;

@Data
public class PapagoTranslateRequest {
    private String url;
    private String crawTool;
    private String method;
    private boolean postIdSelector;
    private boolean postCSSSelector;
    private boolean postXpathSelector;
    private boolean postClassSelector;
    private String postBody;
    private String postCrawSelector;
    private boolean returnIdSelector;
    private boolean returnCSSSelector;
    private boolean returnXpathSelector;
    private boolean returnClassSelector;
    private String returnCrawSelector;

    public PapagoTranslateRequest() {
        this.url = "https://papago.naver.com";
        this.crawTool = "PAPAGO";
        this.method = "POST";
        this.postCrawSelector = "txtSource";
        this.returnCrawSelector = "txtTarget";
        this.postIdSelector = true;
        this.postXpathSelector = false;
        this.postClassSelector = false;
        this.postCSSSelector = false;
        this.returnIdSelector = true;
        this.returnXpathSelector = false;
        this.returnClassSelector = false;
        this.returnCSSSelector = false;
    }
}
