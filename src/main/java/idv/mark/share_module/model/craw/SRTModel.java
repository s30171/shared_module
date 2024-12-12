package idv.mark.share_module.model.craw;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.apache.commons.lang3.StringUtils;

import java.util.ArrayList;
import java.util.List;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class SRTModel {
    private static final int COMPRESS_STRING_LENGTH_LIMIT = 40;

    private Integer sequence;
    private String time;
    private String text;
    private String lineBreak = "\n";

    // 判斷幻覺輸出
    public void resetIfHallucination() {
        if (StringUtils.isAllBlank(this.text)) {
            return;
        }
        this.text = filterRepeatedPhrases(this.text);
        this.text = filterLongTimeStampHallucination(this.time, this.text);
    }

    // 移除特殊字元
    public void replaceSpecialCharacter() {
        if (StringUtils.isAllBlank(this.text)) {
            return;
        }
        this.text = this.text.replace("�", "");
        this.text = this.text.trim();
    }

    // 時間軸錯亂對調
    public void swapTimeCheck() {
        if (StringUtils.isAllBlank(this.time)) {
            return;
        }
        String[] timeArray = this.time.split(" --> ");
        if (timeArray.length != 2) {
            return;
        }
        long startTimeSec = convertToMillis(timeArray[0]);
        long endTimeSec = convertToMillis(timeArray[1]);
        if (startTimeSec > endTimeSec) {
            System.out.println("時間軸錯亂對調 swapTime: " + this.time);
            this.time = timeArray[1] + " --> " + timeArray[0];
        }
    }

    // 移除重複的字元
    private static String filterRepeatedChars(String text) {
        StringBuilder result = new StringBuilder();
        char[] chars = text.toCharArray();
        int count = 1;

        result.append(chars[0]);
        for (int i = 1; i < chars.length; i++) {
            if (chars[i] == chars[i - 1]) {
                count++;
                if (count <= 4) {
                    result.append(chars[i]);
                }
            } else {
                count = 1;
                result.append(chars[i]);
            }
        }

        return result.toString();
    }

    // 移除重複的片語
    private static String filterRepeatedPhrases(String text) {
        StringBuilder result = new StringBuilder();
        String[] words = text.split(" ");
        int count = 1;

        result.append(words[0]);
        for (int i = 1; i < words.length; i++) {
            if (words[i].equals(words[i - 1])) {
                count++;
                if (count <= 4) {
                    result.append(" ").append(words[i]);
                }
            } else {
                count = 1;
                result.append(" ").append(words[i]);
            }
        }

        return filterRepeatedChars(result.toString());
    }

    private String filterLongTimeStampHallucination(String timestampText, String text) {
        String result = text;
        String[] timestampTextArray = timestampText.split(" --> ");
        long startTimeStampSec = convertToSeconds(timestampTextArray[0]);
        long endTimeStampSec = convertToSeconds(timestampTextArray[1]);
        try {
            if (timestampTextArray.length != 2) {
                return result;
            }
            if ((endTimeStampSec - startTimeStampSec) > 120) {
                System.out.println("120秒幻覺輸出過濾 filterLongTimeStampHallucination: " + text);
                result = "_";
            }
        } catch (Exception e) {
            System.out.println("filterLongTimeStampHallucination error: " + e);
        }

        return result;
    }

    // 將SRT時間格式轉換為秒數（忽略毫秒）
    public long convertToSeconds(String srtTime) {
        String[] timeParts = srtTime.split("[:,]");

        int hours = Integer.parseInt(timeParts[0]);
        int minutes = Integer.parseInt(timeParts[1]);
        int seconds = Integer.parseInt(timeParts[2]);

        // 計算總秒數
        return hours * 3600L + minutes * 60L + seconds;
    }

    // 將SRT時間格式轉換為毫秒
    public static long convertToMillis(String srtTime) {
        String[] timeParts = srtTime.split("[:,]");

        int hours = Integer.parseInt(timeParts[0]);
        int minutes = Integer.parseInt(timeParts[1]);
        int seconds = Integer.parseInt(timeParts[2]);
        int milliseconds = Integer.parseInt(timeParts[3]);

        // 計算毫秒數
        return (long) hours * 3600 * 1000 + (long) minutes * 60 * 1000 + seconds * 1000L + milliseconds;
    }

    // 壓縮整個字串
    public boolean checkRepeat() {
        if (StringUtils.isAllBlank(this.text)) {
            return false;
        }
        // 將字串翻倍並移除頭尾
        String doubledString = this.text + this.text;
        String trimmedString = doubledString.substring(1, doubledString.length() - 1);

        // 如果原始字串出現在 trimmedString 中，則可以由子字串重複組成
        return trimmedString.contains(this.text);
    }

    public void repeatedSubstringPattern() {
        if (StringUtils.isAllBlank(this.text) || this.text.length() < COMPRESS_STRING_LENGTH_LIMIT) {
            return;
        }
        String s = this.text;
        int n = s.length();

        // 使用滑動視窗檢查所有可能的子字串長度
        for (int len = 1; len <= n / 2; len++) {
            // 取出子字串並進行匹配檢查
            String substring = s.substring(0, len);
            boolean match = true;

            // 檢查所有長度為 len 的子字串是否一致
            for (int i = 0; i + len <= n; i += len) {
                if (!s.substring(i, i + len).equals(substring)) {
                    match = false;
                    break;
                }
            }

            // 檢查剩餘部分是否與子字串的開頭一致
            if (match && n % len != 0) {
                String remainingPart = s.substring(n - n % len);
                if (!substring.startsWith(remainingPart)) {
                    match = false;
                }
            }

            if (match) {
                // 若匹配，返回去除重複的子字串
                this.text = substring;
                System.out.printf("repeatedSubstringPattern[%s], text:[%s] -> substring:[%s]\n", sequence, this.text, substring);
            }
        }
    }

    // 壓縮整個字串
    public void compressString() {
        if (StringUtils.isAllBlank(this.text) || this.text.length() < COMPRESS_STRING_LENGTH_LIMIT) {
            return;
        }
        if (!checkRepeat()) {
            return;
        }

        StringBuilder result = new StringBuilder();
        String remainingInput = this.text;
        if (StringUtils.isAllBlank(remainingInput)) {
            return;
        }

        while (!remainingInput.isEmpty()) {
            List<String> patterns = detectRepeatedPatterns(remainingInput);

            // 壓縮每個模式
            for (String pattern : patterns) {
                int count = 0;
                while (remainingInput.startsWith(pattern)) {
                    count++;
                    remainingInput = remainingInput.substring(pattern.length());
                }
                result.append(pattern);
                System.out.printf("compressString[%s], text:[%s] -> substring:[%s]\n", this.sequence, this.text, pattern);
            }

            // 處理無法再壓縮的部分
            if (!remainingInput.isEmpty() && detectRepeatedPatterns(remainingInput).isEmpty()) {
                result.append(remainingInput.charAt(0));
                remainingInput = remainingInput.substring(1);
            }
        }
        this.text = result.toString();
    }

    // 找出字串中的重複模式
    private static List<String> detectRepeatedPatterns(String input) {
        int n = input.length();
        List<String> repeatedPatterns = new ArrayList<>();

        for (int len = 1; len <= n / 2; len++) {
            if (input.isEmpty()) break; // 檢查是否為空字串
            if (len > input.length()) break; // 防止越界

            String pattern = input.substring(0, len); // 當前字串模式
            int count = 0;

            for (int i = 0; i <= n - len; i += len) {
                if (input.startsWith(pattern, i)) {
                    count++;
                } else {
                    break;
                }
            }

            if (count > 1 && !repeatedPatterns.contains(pattern)) {
                repeatedPatterns.add(pattern);
                input = input.replace(pattern.repeat(count), ""); // 移除已匹配的部分
            }
        }

        return repeatedPatterns;
    }
}
