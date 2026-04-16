package idv.mark.share_module.util;

import idv.mark.share_module.model.craw.SRTModel;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.io.File;
import java.net.URL;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class SRTUtilTest {

    @Nested
    @DisplayName("filterConsecutiveCharHallucination 單元測試")
    class FilterConsecutiveCharHallucinationTest {

        @Test
        @DisplayName("前綴正常 + 大量重複字元應被壓縮：超級廢爺快快快...快")
        void shouldCompressMixedPrefixWithRepeatedChars() {
            // 模擬原始幻覺：超級廢爺 + 大量「快」
            String repeated = "快".repeat(100);
            SRTModel model = new SRTModel(486, "00:40:05,636 --> 00:40:11,527", "超級廢爺" + repeated, "\n");
            model.filterConsecutiveCharHallucination();

            System.out.println("Result: " + model.getText());
            // 應保留前綴 + 最多 3 個「快」
            assertEquals("超級廢爺快快快", model.getText());
        }

        @Test
        @DisplayName("純重複字元應被壓縮：呵呵呵...呵")
        void shouldCompressPureRepeatedSingleChar() {
            String repeated = "呵".repeat(200);
            SRTModel model = new SRTModel(112, "00:08:57,010 --> 00:09:02,377", repeated, "\n");
            model.filterConsecutiveCharHallucination();

            System.out.println("Result: " + model.getText());
            assertEquals("呵呵呵", model.getText());
        }

        @Test
        @DisplayName("兩種重複字元混合：呵呵呵...哈哈哈...")
        void shouldCompressTwoDominantChars() {
            String text = "呵".repeat(188) + "哈".repeat(72);
            SRTModel model = new SRTModel(112, "00:08:57,010 --> 00:09:02,377", text, "\n");
            model.filterConsecutiveCharHallucination();

            System.out.println("Result: " + model.getText());
            String result = model.getText();
            assertTrue(result.length() < text.length(), "應該有被壓縮");
            assertEquals("呵呵呵哈哈哈", result, "兩種字元都應被壓到3個");
        }

        @Test
        @DisplayName("多種重複字元混合：呵呵呵...哈哈哈...啦啦啦...")
        void shouldCompressThreeDominantChars() {
            String text = "呵".repeat(30) + "哈".repeat(30) + "啦".repeat(30);
            SRTModel model = new SRTModel(112, "00:08:57,010 --> 00:09:02,377", text, "\n");
            model.filterConsecutiveCharHallucination();

            System.out.println("Result: " + model.getText());
            String result = model.getText();
            assertTrue(result.length() < text.length(), "應該有被壓縮");
            assertEquals("呵呵呵哈哈哈啦啦啦", result, "三種字元都應被壓到3個");
        }

        @Test
        @DisplayName("正常文字不應被影響")
        void shouldNotAffectNormalText() {
            String normalText = "這是一段正常的字幕內容，沒有重複的問題";
            SRTModel model = new SRTModel(1, "00:00:01,000 --> 00:00:05,000", normalText, "\n");
            model.filterConsecutiveCharHallucination();

            assertEquals(normalText, model.getText());
        }

        @Test
        @DisplayName("未達連續重複門檻不應被處理")
        void shouldNotProcessShortText() {
            // 連續低於 8 字元不應該被當作幻覺處理
            String shortText = "快快快快快快快";  // 7 chars
            SRTModel model = new SRTModel(1, "00:00:01,000 --> 00:00:05,000", shortText, "\n");
            model.filterConsecutiveCharHallucination();

            assertEquals(shortText, model.getText());
        }

        @Test
        @DisplayName("多段重複間夾雜正常文字")
        void shouldHandleInterleavedRepeats() {
            // 超級廢爺快快快...快然後再接正常文字
            String text = "開頭文字" + "啊".repeat(50) + "中間" + "啊".repeat(10);
            SRTModel model = new SRTModel(1, "00:00:01,000 --> 00:00:05,000", text, "\n");
            model.filterConsecutiveCharHallucination();

            System.out.println("Result: " + model.getText());
            String result = model.getText();
            assertTrue(result.length() < text.length(), "應被壓縮");
            assertFalse(result.contains("啊啊啊啊"), "不應有超過3個連續啊");
            assertTrue(result.contains("開頭文字"), "應保留正常前綴");
            assertTrue(result.contains("中間"), "應保留中間正常文字");
        }
    }

    @Nested
    @DisplayName("SRT 檔案整合測試")
    class SRTFileIntegrationTest {

        @Test
        @DisplayName("讀取實際 SRT 檔，驗證幻覺字幕被正確處理")
        void shouldFilterHallucinationsFromRealSRTFile() {
            URL resource = getClass().getClassLoader().getResource("test_hallucination.srt");
            assertNotNull(resource, "test_hallucination.srt 應存在於 test resources");

            File srtFile = new File(resource.getFile());
            List<SRTModel> srtModels = SRTUtil.srtFileToSRTModel(srtFile);

            assertNotNull(srtModels);
            assertFalse(srtModels.isEmpty());

            System.out.println("Total subtitles: " + srtModels.size());

            // 逐一檢查每個字幕是否有殘留的幻覺
            for (SRTModel model : srtModels) {
                String text = model.getText();
                if (text == null || text.isBlank()) continue;

                // 檢查：任何字元連續出現超過 10 次就是問題
                assertFalse(
                        hasExcessiveRepetition(text, 10),
                        String.format("字幕 #%d 仍有過度重複: [%s]",
                                model.getSequence(),
                                text.length() > 80 ? text.substring(0, 80) + "..." : text)
                );

                // 檢查：總長度超過 200 字元也很可疑
                assertTrue(
                        text.length() <= 200,
                        String.format("字幕 #%d 長度異常 (%d chars): [%s]",
                                model.getSequence(),
                                text.length(),
                                text.length() > 80 ? text.substring(0, 80) + "..." : text)
                );
            }

            // 印出可疑字幕供人工確認
            System.out.println("\n=== 長度 > 30 的字幕 (供人工確認) ===");
            srtModels.stream()
                    .filter(m -> m.getText() != null && m.getText().length() > 30)
                    .forEach(m -> System.out.printf("#%d [%s]: %s%n",
                            m.getSequence(), m.getTime(),
                            m.getText().length() > 100 ? m.getText().substring(0, 100) + "..." : m.getText()));
        }
    }

    /**
     * 檢查字串中是否有任何字元連續出現超過 threshold 次
     */
    private static boolean hasExcessiveRepetition(String text, int threshold) {
        if (text == null || text.length() < threshold) return false;
        int count = 1;
        for (int i = 1; i < text.length(); i++) {
            if (text.charAt(i) == text.charAt(i - 1)) {
                count++;
                if (count > threshold) return true;
            } else {
                count = 1;
            }
        }
        return false;
    }
}
