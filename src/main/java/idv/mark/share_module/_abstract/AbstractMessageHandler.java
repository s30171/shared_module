package idv.mark.share_module._abstract;

public abstract class AbstractMessageHandler {
    private AbstractMessageHandler nextHandler;

    public AbstractMessageHandler next(AbstractMessageHandler nextHandler) {
        this.nextHandler = nextHandler;
        return nextHandler;
    }

    public void handle(String message) {
        if (canHandle(message)) {
            process(message);
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