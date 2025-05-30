package idv.mark.share_module.model.craw;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.apache.commons.lang3.StringUtils;

import java.util.*;

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
        try {
            if (StringUtils.isAllBlank(this.text)) {
                return;
            }
            this.text = filterLongTimeStampHallucination(this.time, this.text);
        } catch (Exception e) {
            System.out.println("resetIfHallucination error: " + e + ", text: " + this.text);
        }
    }

    // 移除特殊字元
    public void replaceSpecialCharacter() {
        try {
            if (StringUtils.isAllBlank(this.text)) {
                return;
            }
            this.text = this.text.replace("�", "");
            this.text = this.text.trim();
        } catch (Exception e) {
            System.out.println("replaceSpecialCharacter error: " + e + ", text: " + this.text);
            throw e;
        }
    }

    // 時間軸錯亂對調
    public void swapTimeCheck() {
        try {
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
        } catch (Exception e) {
            System.out.println("swapTimeCheck error: " + e + ", time: " + this.time);
            throw e;
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
        try {
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
        } catch (Exception e) {
            System.out.println("repeatedSubstringPattern error: " + e + ", text: " + this.text);
            throw e;
        }
    }

    // 壓縮整個字串
    public void compressString() {
        try {
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
        } catch (Exception e) {
            System.out.println("compressString error: " + e + ", text: " + this.text);
            throw e;
        }
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

    // 壓縮每個單字裡面重複的部分（依空格切割）
    public void compressRepeatedWord() {
        String originalText = this.text;
        try {
            if (StringUtils.isAllBlank(this.text)) {
                return;
            }
            String[] words = this.text.split(" ");
            for (int i = 0; i < words.length; i++) {
                String originalWord = words[i];
                words[i] = compressRepeatedToken(words[i]);
                if (!originalWord.equals(words[i])) {
                    System.out.printf("compressRepeatedWord: [%s] -> [%s]\n", originalWord, words[i]);
                }
            }
            this.text = String.join(" ", words);

            // 第二層判斷, 判斷單字是否高度重複
            if (this.text.length() > COMPRESS_STRING_LENGTH_LIMIT) {
                int totalTokens = words.length;
                Map<String, Integer> countMap = new HashMap<>();
                for (String token : words) {
                    countMap.put(token, countMap.getOrDefault(token, 0) + 1);
                }

                // 2. 計算出現率並存入 Map<String, Double>
                Map<String, Double> frequencyMap = new HashMap<>();
                for (Map.Entry<String, Integer> entry : countMap.entrySet()) {
                    double frequency = (double) entry.getValue() / totalTokens;
                    frequencyMap.put(entry.getKey(), frequency);
                }

                // 設定門檻值（例如：90%）
                double threshold = 0.9;
                // 找出出現率大於或等於門檻的單字
                Set<String> highFrequencyWords = new HashSet<>();
                for (Map.Entry<String, Double> entry : frequencyMap.entrySet()) {
                    if (entry.getValue() >= threshold) {
                        highFrequencyWords.add(entry.getKey());
                    }
                }

                // 濾除這些高度重複的單字
                // 依照要求：第一次出現保留，後續出現則濾除
                Set<String> addedHighFrequency = new HashSet<>();
                StringBuilder filteredText = new StringBuilder();
                for (String token : words) {
                    if (highFrequencyWords.contains(token)) {
                        if (!addedHighFrequency.contains(token)) {
                            filteredText.append(token).append(" ");
                            addedHighFrequency.add(token);
                        }
                        // 已經出現過的高頻單字就跳過不加入
                    } else {
                        filteredText.append(token).append(" ");
                    }
                }
                this.text = filteredText.toString().trim();
                if (!StringUtils.equals(originalText, this.text)) {
                    System.out.printf("compressRepeatedWord highFrequency: [%s] -> [%s]\n", originalText, this.text);
                }
            }
        } catch (Exception e) {
            System.out.println("compressRepeatedWord error: " + e + ", text: " + this.text);
            throw e;
        }
    }

    // 檢查單一字串是否由重複子字串組成，若重複次數 >= 4 則回傳最小子字串，否則回傳原字串
    private static String compressRepeatedToken(String token) {
        int n = token.length();
        if (n <= 1) {
            return token;
        }
        // 嘗試所有可能的子字串長度
        for (int len = 1; len <= n / 2; len++) {
            // 只有當整個 token 長度能整除子字串長度時才檢查
            if (n % len == 0) {
                int count = n / len;
                // 只有重複次數達 4 次以上才進行壓縮
                if (count < 4) {
                    continue;
                }
                String pattern = token.substring(0, len);
                boolean match = true;
                for (int i = 0; i < n; i += len) {
                    if (!token.substring(i, i + len).equals(pattern)) {
                        match = false;
                        break;
                    }
                }
                if (match) {
                    return pattern;
                }
            }
        }
        return token;
    }
}
