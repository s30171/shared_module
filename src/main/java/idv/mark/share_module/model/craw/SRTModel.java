package idv.mark.share_module.model.craw;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.apache.commons.lang3.StringUtils;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class SRTModel {
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
}
