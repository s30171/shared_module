package idv.mark.share_module.util;

import org.apache.commons.lang3.StringUtils;

public class CalculateUtil {

    private static final String CHINESE_REGEX = "[\\u4e00-\\u9fa5]";

    public static double calculateChineseRatio(String text) {
        if (StringUtils.isAllBlank(text)) {
            return 0.0;
        }
        int chineseCount = 0;
        int totalCount = text.length();
        for (char ch : text.toCharArray()) {
            if (String.valueOf(ch).matches(CHINESE_REGEX)) {
                chineseCount++;
            }
        }

        return (double) chineseCount / totalCount;
    }

}
