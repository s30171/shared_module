package idv.mark.share_module.util;

import com.google.common.collect.Lists;
import com.google.gson.Gson;
import idv.mark.share_module.model.craw.CrawModel;
import idv.mark.share_module.model.craw.PapagoTranslateRequest;
import idv.mark.share_module.model.craw.RetryTimes;
import idv.mark.share_module.model.craw.SRTModel;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.io.FileUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.util.CollectionUtils;
import org.springframework.web.client.RestTemplate;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.time.Instant;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

@Slf4j
public class TranslateUtil {

    // 設定任務池大小 (通常搭配WebDriver的個數)
    private static final int translateTaskSize = 2;
    // 設定一次送去翻譯的筆數
    private static final int partitionSize = 150;

    private static RestTemplate restTemplate = RESTUtil.restTemplate;

    // 翻譯SRT檔案 (取回File)
    public static File translate(File originalLanguageSRTfile, String crawUrl, String doneFileNamePath) throws IOException {
        List<SRTModel> srtModels = translateToSRTModel(originalLanguageSRTfile, crawUrl);
        saveSrtToFile(srtModels, doneFileNamePath);
        // 產出翻譯後的SRT檔案
        return new File(doneFileNamePath);
    }

    // 翻譯SRT檔案 (取回SRTModel)
    public static List<SRTModel> translateToSRTModel(File originalLanguageSRTfile, String crawUrl) throws IOException {
        Instant start = Instant.now();
        String text = FileUtils.readFileToString(originalLanguageSRTfile, "UTF-8");
        List<SRTModel> srtModelList = convertTextToSRTModels(text);
        retrySendAPIRequestWithTask(srtModelList, new RetryTimes(3), crawUrl);
        sortSRTModel(srtModelList);
        Instant end = Instant.now();
        log.info("translate cost time: {}", end.toEpochMilli() - start.toEpochMilli());
        // 產出翻譯後的SRT檔案
        return srtModelList;
    }

    // 將SRTModel存成檔案
    public static void saveSrtToFile(List<SRTModel> srtModelList, String doneFileName) {
        StringBuilder sb = new StringBuilder();
        srtModelList.forEach(srtModel -> {
            sb.append(srtModel.getSequence()).append("\n");
            sb.append(srtModel.getTime()).append("\n");
            sb.append(srtModel.getText()).append("\n");
            sb.append(srtModel.getLineBreak());
        });
        File file = new File(doneFileName);
        try (BufferedWriter writer = new BufferedWriter(new FileWriter(file))) {
            writer.write(sb.toString());
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    // 重試機制
    public static void retrySendAPIRequestWithTask(List<SRTModel> srtModelList, RetryTimes retryTimes, String url) {
        if (retryTimes.getRetryTimes() <= 0) {
            return;
        }
        ExecutorService executorService = Executors.newFixedThreadPool(translateTaskSize);
        // 先拆分150筆一次
        List<List<SRTModel>> partition = Lists.partition(srtModelList, partitionSize);
        List<List<SRTModel>> needTranslatePartition = Lists.newArrayList();
        for (int i = 0; i < partition.size(); i++) {
            List<SRTModel> srtModels = partition.get(i);
            String appendRequestString = srtModels.stream()
                    .collect(StringBuilder::new, (builder, element) -> builder.append(element.getText()).append("\n"), StringBuilder::append)
                    .toString();
            double v = CalculateUtil.calculateChineseRatio(appendRequestString);
            if (v < 0.1) {
                // 如果中文比例小於10%，代表尚未翻譯
                needTranslatePartition.add(srtModels);
            }
        }
        if (needTranslatePartition.isEmpty()) {
            // 全部都不用翻就return
            return;
        }
        List<CompletableFuture<Integer>> allTaskCollector = Lists.newArrayList();
        List<Integer> errorTaskCollector = Lists.newArrayList();
        try {
            for (int i = 0; i < needTranslatePartition.size(); i++) {
                int needTranslateIndex = i;
                CompletableFuture<Integer> completableFuture = CompletableFuture.supplyAsync(() -> {
                            sendPapagoTranslateRequest(srtModelList, needTranslatePartition.get(needTranslateIndex), url);
                            return needTranslateIndex;
                        }, executorService)
                        .whenComplete((integer, throwable) -> {
                            if (throwable == null) {
                                printTranslateProgress(needTranslatePartition.get(needTranslateIndex), srtModelList.size());
                            }
                        })
                        .exceptionally((throwable) -> {
                            log.error("translate error, {}", throwable.getMessage());
                            errorTaskCollector.add(needTranslateIndex);
                            return null;
                        });
                allTaskCollector.add(completableFuture);
            }
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            executorService.shutdown();
        }

        CompletableFuture.allOf(allTaskCollector.toArray(new CompletableFuture[0])).join();

        // 如果有錯, 拋出任務重試
        if (!CollectionUtils.isEmpty(errorTaskCollector)) {
            Integer nowRetryTimes = retryTimes.getRetryTimes();
            retryTimes.setRetryTimes(--nowRetryTimes);
            try {
                Thread.sleep(5000);
            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            }
            retrySendAPIRequestWithTask(srtModelList, retryTimes, url);
        }
    }

    // 送去Papago翻譯 with 遞迴 (因為有字數上限)
    private static void sendPapagoTranslateRequest(List<SRTModel> allSRTModelList, List<SRTModel> partitionStringList, String url) {
        String appendRequestString = partitionStringList.stream()
                .collect(StringBuilder::new, (builder, element) -> builder.append(element.getText()).append("\n"), StringBuilder::append)
                .toString();

        // 如果字數超過2500，就分成兩半送
        if (appendRequestString.length() > 2500) {
            List<List<SRTModel>> partitionTwoList = Lists.partition(partitionStringList, partitionStringList.size() / 2);
            for (List<SRTModel> srtModels : partitionTwoList) {
                sendPapagoTranslateRequest(allSRTModelList, srtModels, url);
            }
            return;
        }

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        PapagoTranslateRequest request = new PapagoTranslateRequest();
        request.setPostBody(appendRequestString);
        String json = new Gson().toJson(request);
        HttpEntity<String> entity = new HttpEntity<>(json, headers);
        ResponseEntity<CrawModel> crawModelResponseEntity = restTemplate.postForEntity(url, entity, CrawModel.class);
        String returnHtml = crawModelResponseEntity.getBody().getReturnHtml();
        String translateSRTString = convertHtmlToLongString(returnHtml);
        reAppendSRTString(allSRTModelList, partitionStringList, translateSRTString);
    }

    // html to srt
    public static String convertHtmlToLongString(String html) {
        if (StringUtils.isAllBlank(html)) {
            return "";
        }
        // Remove all HTML tags, except <br> which will be replaced with a newline
        String srt = html.replaceAll("(?i)<br\\s*/?>", "\n");

        srt = srt.replaceAll("<span></span>", "\n");

        // Replace all HTML tags
        srt = srt.replaceAll("<[^>]*>", "");

        // Replace HTML entity for arrow
        srt = srt.replaceAll("--&gt;", "-->");

        // Trim extra spaces and newlines, then replace multiple newlines with single newlines
        srt = srt.trim().replaceAll("\n\n\n", "\n\n");

        return srt;
    }

    // 印出進度
    private static void printTranslateProgress(List<SRTModel> partitionStringList, int size) {
        try {
            SRTModel srtModel = partitionStringList.get(0);
            Integer sequence = srtModel.getSequence();
            log.info("translate progress: {} / {}, about {}%", sequence, size, (sequence * 100) / size);
        } catch (Exception e) {
            // ignore
        }
    }

    // 排序SRTModel (因為sequence可能亂序)
    private static void sortSRTModel(List<SRTModel> srtModelList) {
        srtModelList.sort(Comparator.comparing(SRTModel::getSequence));
    }

    // 重新append時間軸和Sequence回去
    private static void reAppendSRTString(List<SRTModel> allSRTModelList, List<SRTModel> partitionStringList, String translateSRTString) {
        String[] split = translateSRTString.split("\n");
        for (int i = 0; i < partitionStringList.size(); i++) {
            SRTModel srtModel = partitionStringList.get(i);
            try {
                String s = split[i];
                srtModel.setText(s);
                Integer sequence = srtModel.getSequence();
                allSRTModelList.get(sequence - 1).setText(s);
            } catch (ArrayIndexOutOfBoundsException e) {
                log.error("srtModel[{}] , ArrayIndexOutOfBoundsException translateSRTString error, {}", srtModel, translateSRTString);
            }
        }
    }

    // 將SRT檔案轉換成SRTModel
    private static List<SRTModel> convertTextToSRTModels(String text) {
        List<SRTModel> srtModelList = new ArrayList<>();
        String[] split = text.split("\n");
        Lists.partition(Arrays.asList(split), 4).forEach(partitionStringList -> {
            SRTModel srtModel = new SRTModel();
            srtModel.setSequence(Integer.parseInt(partitionStringList.get(0)));
            srtModel.setTime(partitionStringList.get(1));
            srtModel.setText(partitionStringList.size() > 2 ? partitionStringList.get(2) : "");
            srtModelList.add(srtModel);
        });
        return srtModelList;
    }

}
