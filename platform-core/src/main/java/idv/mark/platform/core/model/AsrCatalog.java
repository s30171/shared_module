package idv.mark.platform.core.model;

import java.util.*;

/** Versioned, executable provider descriptions shared by validation and the UI. */
public final class AsrCatalog {
    private AsrCatalog() {}
    public record Field(String key,String label,String type,Object defaultValue,List<String> choices,Double min,Double max) {}
    public record Provider(String id,String name,String executionMode,List<String> models,List<Field> options,List<Field> providerOptions,String documentation) {}
    private static Field field(String key,String label,String type,Object fallback,String... choices) { return new Field(key,label,type,fallback,List.of(choices),null,null); }
    private static Field number(String key,String label,int fallback,int min,int max) { return new Field(key,label,"number",fallback,List.of(),(double)min,(double)max); }
    private static final List<Field> CLOUD = List.of(field("languageHints","語言提示（空白為自動偵測）","list",List.of()),field("timestamps","時間戳","select","word","word","segment","none"),field("diarization","區分說話者","boolean",false),field("vocabulary","專有詞彙","list",List.of()));
    public static List<Provider> providers() {
        return List.of(
            new Provider("GEMINI_TRANSCRIBE","Gemini 3.5 Transcribe","cloud",List.of("gemini-3.5-transcribe"),CLOUD,List.of(field("mode","轉錄模式","select","verbatim","verbatim","smart")),"https://ai.google.dev/gemini-api/docs/transcribe"),
            new Provider("MAI_TRANSCRIBE","MAI-Transcribe-2","cloud",List.of("MAI-Transcribe-2"),CLOUD,List.of(field("style","轉錄風格","select","verbatim","verbatim","clean")),"https://learn.microsoft.com/en-us/azure/ai-services/speech-service/mai-transcribe"),
            new Provider("ELEVENLABS_SCRIBE","ElevenLabs Scribe v2","cloud",List.of("scribe_v2"),CLOUD,List.of(field("tagAudioEvents","標記聲音事件","boolean",false),field("noVerbatim","去除填充語","boolean",false)),"https://elevenlabs.io/docs/api-reference/speech-to-text/convert"),
            new Provider("WHISPER_X","WhisperX","gpu",List.of("large-v3","large-v3-turbo","medium","small","base","tiny"),List.of(field("languageHints","語言提示（最多一種）","list",List.of())),List.of(number("batchSize","Batch size",4,1,32),field("computeType","計算精度","select","float16","float16","float32","int8"),field("align","時間對齊","boolean",true)),"https://github.com/m-bain/whisperX"),
            new Provider("INSANELY_FAST_WHISPER","Insanely Fast Whisper","gpu",List.of("openai/whisper-large-v3","openai/whisper-large-v3-turbo","openai/whisper-medium"),List.of(),List.of(number("batchSize","Batch size",4,1,32)),"https://github.com/Vaibhavs10/insanely-fast-whisper")
        );
    }
    public static Provider get(String id) { return providers().stream().filter(p -> p.id().equals(id)).findFirst().orElseThrow(() -> new IllegalArgumentException("Unsupported ASR provider")); }
    public static TaskSpec.Asr normalize(TaskSpec.Asr input) {
        Provider provider=get(input.provider());
        String model=input.model()==null||input.model().isBlank()?provider.models().get(0):input.model();
        if(!provider.models().contains(model)) throw new IllegalArgumentException("Unsupported model for " + provider.name());
        Map<String,Object> options=validateFields(input.options(),provider.options());
        Map<String,Object> advanced=validateFields(input.providerOptions(),provider.providerOptions());
        if(provider.id().equals("GEMINI_TRANSCRIBE") && advanced.get("mode").equals("smart") && (!options.get("timestamps").equals("none") || Boolean.TRUE.equals(options.get("diarization"))))
            throw new IllegalArgumentException("Gemini smart mode cannot use timestamps or speaker diarization");
        if(provider.id().equals("WHISPER_X") && ((List<?>)options.get("languageHints")).size()>1) throw new IllegalArgumentException("WhisperX accepts one language hint");
        if(Set.of("MAI_TRANSCRIBE","ELEVENLABS_SCRIBE").contains(provider.id()) && ((List<?>)options.get("languageHints")).size()>1) throw new IllegalArgumentException("This provider accepts one language hint");
        if(provider.id().equals("GEMINI_TRANSCRIBE")&&!((List<?>)options.get("vocabulary")).isEmpty()&&(!options.get("timestamps").equals("none")||Boolean.TRUE.equals(options.get("diarization")))) throw new IllegalArgumentException("Gemini custom vocabulary cannot combine with timestamps or diarization");
        if(provider.id().equals("ELEVENLABS_SCRIBE")) for(Object term:(List<?>)options.get("vocabulary")) {
            String word=term.toString(); if(word.length()>=50||word.strip().split("\\s+").length>5||word.matches(".*[<>{}\\[\\]\\\\].*")) throw new IllegalArgumentException("Invalid Scribe vocabulary term");
        }
        return new TaskSpec.Asr(input.provider(),model,options,advanced,input.credentialRef());
    }
    private static Map<String,Object> validateFields(Map<String,Object> input,List<Field> fields) {
        Map<String,Object> result=new LinkedHashMap<>();
        for(String key:input.keySet()) if(fields.stream().noneMatch(f -> f.key().equals(key))) throw new IllegalArgumentException("Unsupported ASR option: " + key);
        for(Field f:fields) {
            Object value=input.getOrDefault(f.key(),f.defaultValue());
            switch(f.type()) {
                case "boolean" -> { if(!(value instanceof Boolean)) throw new IllegalArgumentException(f.label()+" must be boolean"); }
                case "select" -> { if(!f.choices().contains(value)) throw new IllegalArgumentException("Invalid option: "+f.key()); }
                case "number" -> { if(!(value instanceof Number n) || !Double.isFinite(n.doubleValue()) || n.doubleValue()<f.min() || n.doubleValue()>f.max() || n.doubleValue()!=n.intValue()) throw new IllegalArgumentException("Invalid numeric option: "+f.key()); }
                case "list" -> { if(!(value instanceof List<?> list) || list.size()>1000 || list.stream().anyMatch(v -> !(v instanceof String s)||s.length()>200||s.isBlank())) throw new IllegalArgumentException("Invalid list: "+f.key()); }
                default -> throw new IllegalStateException("Unknown field type");
            }
            result.put(f.key(),value);
        }
        return result;
    }
}
