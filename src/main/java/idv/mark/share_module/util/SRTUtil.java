package idv.mark.share_module.util;

import idv.mark.share_module.model.craw.SRTModel;
import org.apache.commons.io.FileUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.util.CollectionUtils;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

public class SRTUtil {

    public static List<SRTModel> srtFileToSRTModel(File file) {
        try {
            String fileText = FileUtils.readFileToString(file, "UTF-8");
            return convertToSRTModel(fileText);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    // 將SRT文字轉換為SRTModel
    public static List<SRTModel> convertToSRTModel(String text) {
        if (StringUtils.isAllBlank(text)) {
            return null;
        }
        List<SRTModel> srtModels = new ArrayList<>();
        String[] split = text.split("\n");

        // 刪除空行
        int startLine = 0;
        for (int i = 0; i < split.length; i++) {
            split[i] = split[i].trim();
            // 判斷第一行在哪
            if (StringUtils.isNotBlank(split[i])) {
                try {
                    Integer.parseInt(split[i]);
                } catch (NumberFormatException e) {
                    continue;
                }
                startLine = i;
                break;
            }
        }
        split = Arrays.copyOfRange(split, startLine, split.length);

        // 將SRT文字分成區塊
        List<String> currentBlock = new ArrayList<>();
        for (int i = 0; i < split.length; i++) {
            String line = split[i];
            if (StringUtils.isNotBlank(line)) { line = line.trim(); }
            // 如果是新的編號行且currentBlock不為空，則處理並清空currentBlock
            if (line.matches("\\d+") && !currentBlock.isEmpty()) {
                try {
                    srtModels.add(convertBlockToSRTModel(currentBlock));
                } catch (Exception e) {
                    System.out.println("line error : {" + line + "}");
                }
                // 在這裡處理完整的SRT區塊
                currentBlock.clear();
            }

            // 將當前行加入currentBlock
            currentBlock.add(line);

            // 處理最後一行結束時的情況
            if (i == split.length - 1) {
                // 處理最後一個SRT區塊
                try {
                    srtModels.add(convertBlockToSRTModel(currentBlock));
                } catch (Exception e) {
                    System.out.println("line error : {" + line + "}");
                }
            }
        }
        List<SRTModel> srtModelList = removeIfEmpty(srtModels);
        resetBlockSequence(srtModelList);
        return srtModelList;
    }

    // 將SRTModel轉換為SRT文字
    public static String srtModelToText(List<SRTModel> srtModels) {
        if (CollectionUtils.isEmpty(srtModels)) {
            return "";
        }
        StringBuilder sb = new StringBuilder();
        for (SRTModel srtModel : srtModels) {
            sb.append(srtModel.getSequence()).append("\n");
            sb.append(srtModel.getTime()).append("\n");
            sb.append(srtModel.getText()).append("\n");
            sb.append("\n");
        }
        return sb.toString();
    }

    // 將SRTModel轉換為SRT文字
    public static String srtModelsExtractText(List<SRTModel> srtModels) {
        return CollectionUtils.isEmpty(srtModels) ? "" : srtModels.stream().map(SRTModel::getText).collect(Collectors.joining("\n"));
    }

    public static void resetBlockSequence(List<SRTModel> srtModels) {
        if (CollectionUtils.isEmpty(srtModels)) {
            return;
        }
        srtModels = srtModels.stream().sorted((Comparator.comparing(SRTModel::getSequence))).collect(Collectors.toList());
        int sequence = srtModels.get(0).getSequence();
        for (SRTModel srtModel : srtModels) {
            srtModel.setSequence(sequence++);
        }
    }

    public static void reSizeTextTwoLine(List<SRTModel> srtModels, int size) {
        if (CollectionUtils.isEmpty(srtModels)) {
            return;
        }
        for (SRTModel srtModel : srtModels) {
            String text = srtModel.getText();
            if (StringUtils.isNotBlank(text) && text.length() > size && !text.contains("\n")) {
                srtModel.setText(splitSentence(text, size));
            }
        }
    }

    /**
     * 若字串長度超過 maxLength，則根據離字串中間最近的標點符號進行切割，
     * 並優先選用英文逗號（,）作為切割點。
     * 若找不到任何標點符號，則直接回傳原字串。
     *
     * @param sentence 要處理的句子
     * @param maxLength 字串長度門檻
     * @return 切割後的字串陣列（若切割成功則陣列長度為2，否則僅包含原句）
     */
    public static String splitSentence(String sentence, int maxLength) {
        String copySentence = sentence;
        if (sentence == null || sentence.length() <= maxLength) {
            return sentence;
        }
        if (sentence.contains("\n")) {
            copySentence = sentence.replace("\n", " ");
        }

        int mid = sentence.length() / 2;
        int bestIndex = -1;
        int bestDistance = sentence.length();

        // 若未找到逗號，再尋找其他常用標點符號
        char[] punctuation = {',', '.', '!', '?', '。', '，', '、', ';', '；'};
        for (int i = 0; i < copySentence.length(); i++) {
            for (char p : punctuation) {
                if (copySentence.charAt(i) == p) {
                    int distance = Math.abs(i - mid);
                    if (distance < bestDistance) {
                        bestDistance = distance;
                        bestIndex = i;
                    }
                }
            }
        }

        // 如果仍然找不到標點符號，則不切割，直接回傳原字串
        if (bestIndex == -1) {
            return sentence;
        }

        // 切割字串，保留標點符號在前一段的結尾
        String firstPart = copySentence.substring(0, bestIndex + 1).trim();
        String secondPart = copySentence.substring(bestIndex + 1).trim();

        return StringUtils.isAllBlank(secondPart) ?  firstPart : firstPart + "\n" + secondPart;
    }

    private static List<SRTModel> removeIfEmpty(List<SRTModel> srtModels) {
        return srtModels.stream().filter(srtModel -> StringUtils.isNotBlank(srtModel.getText())).collect(Collectors.toList());
    }

    private static SRTModel convertBlockToSRTModel(List<String> block) {
        String text = "";
        if (block.size() <= 2) {
            text = "";
            System.out.println("block error : {" + String.join("\n", block) + "}");
        } else {
            text = String.join("\n", block.subList(2, block.size()));
        }
        // 移除最後一個換行符號（如果存在）
        if (text.endsWith("\n")) {
            text = text.substring(0, text.length() - 1);
        }
        SRTModel srtModel = new SRTModel(Integer.parseInt(block.get(0)), block.get(1), text, "\n");
        srtModel.resetIfHallucination();
        srtModel.replaceSpecialCharacter();
        srtModel.swapTimeCheck();
        srtModel.compressString();
        srtModel.compressRepeatedWord();
        srtModel.repeatedSubstringPattern();
        srtModel.compressString();
        return srtModel;
    }
}
