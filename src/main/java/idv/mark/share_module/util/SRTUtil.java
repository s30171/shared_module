package idv.mark.share_module.util;

import idv.mark.share_module.model.craw.SRTModel;
import org.apache.commons.io.FileUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.util.CollectionUtils;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
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
            // 如果是新的編號行且currentBlock不為空，則處理並清空currentBlock
            if (line.matches("\\d+") && !currentBlock.isEmpty()) {
                srtModels.add(convertBlockToSRTModel(currentBlock));
                // 在這裡處理完整的SRT區塊
                currentBlock.clear();
            }

            // 將當前行加入currentBlock
            currentBlock.add(line);

            // 處理最後一行結束時的情況
            if (i == split.length - 1) {
                // 處理最後一個SRT區塊
                srtModels.add(convertBlockToSRTModel(currentBlock));
            }
        }

        return srtModels;
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
            sb.append(srtModel.getLineBreak()).append("\n");
        }
        return sb.toString();
    }

    // 將SRTModel轉換為SRT文字
    public static String srtModelsExtractText(List<SRTModel> srtModels) {
        return CollectionUtils.isEmpty(srtModels) ? "" : srtModels.stream().map(SRTModel::getText).collect(Collectors.joining("\n"));
    }

    private static SRTModel convertBlockToSRTModel(List<String> block) {
        String text = block.size() > 4 ? String.join("\n", block.subList(2, block.size())) : block.get(2);
        // 移除最後一個換行符號（如果存在）
        if (text.endsWith("\n")) {
            text = text.substring(0, text.length() - 1);
        }
        SRTModel srtModel = new SRTModel(Integer.parseInt(block.get(0)), block.get(1), text, "\n");
        srtModel.resetIfHallucination();
        srtModel.replaceSpecialCharacter();
        srtModel.swapTimeCheck();
        return srtModel;
    }
}
