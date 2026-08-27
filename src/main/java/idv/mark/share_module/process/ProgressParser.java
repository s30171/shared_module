package idv.mark.share_module.process;

@FunctionalInterface
public interface ProgressParser {
    /**
     * 解析一行輸出，回傳 0~100 進度值，null 表示非進度行
     */
    Integer parse(String line);
}
