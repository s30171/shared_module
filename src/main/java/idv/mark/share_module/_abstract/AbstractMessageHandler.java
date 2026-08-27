package idv.mark.share_module._abstract;

public abstract class AbstractMessageHandler {
    protected AbstractMessageHandler nextHandler;

    public AbstractMessageHandler() {
    }

    public AbstractMessageHandler(AbstractMessageHandler nextHandler) {
        this.nextHandler = nextHandler;
    }

    public void setNext(AbstractMessageHandler nextHandler) {
        this.nextHandler = nextHandler;
    }

    public void handle(String message) {
        if (canHandle(message)) {
            process(message);
            return; // ★ Fix: 匹配成功後中斷鏈，不再往下傳遞
        }
        if (nextHandler != null) {
            nextHandler.handle(message);
        }
    }

    // 判斷是否能處理此消息
    protected abstract boolean canHandle(String message);

    // 實際處理邏輯
    protected abstract void process(String message);
}
