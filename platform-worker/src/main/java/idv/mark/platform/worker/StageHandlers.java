package idv.mark.platform.worker;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import idv.mark.platform.core.model.*;
import idv.mark.platform.asr.CloudAsr;
import idv.mark.platform.translation.TranslationClient;
import idv.mark.platform.publish.YouTubePublisher;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Component;
import java.nio.*;
import java.nio.file.*;
import java.time.Duration;
import java.util.*;

/** One durable attempt per stage; all subprocesses use the same managed execution path. */
@Component
public class StageHandlers {
 private final String role;private final boolean demo;private final Environment env;private final ObjectMapper json;
 public StageHandlers(@Value("${worker.role}")String role,@Value("${worker.demo}")boolean demo,Environment env,ObjectMapper json){this.role=role;this.demo=demo;this.env=env;this.json=json;}
 public boolean demo(){return demo;}
 public List<String> capabilities(){
   if(demo)return List.of("DOWNLOAD","VERIFY_MEDIA","PREPARE_AUDIO","ASR:WHISPER_X","ASR:INSANELY_FAST_WHISPER","ASR:GEMINI_TRANSCRIBE","ASR:MAI_TRANSCRIBE","ASR:ELEVENLABS_SCRIBE","TRANSLATE","UPLOAD_VIDEO","UPLOAD_SUBTITLES");
   return switch(role){case "detector"->List.of();case "backup"->executable(bin("yt-dlp"))&&executable(bin("ffmpeg"))&&executable(bin("ffprobe"))?List.of("DOWNLOAD","VERIFY_MEDIA","PREPARE_AUDIO"):List.of();
     case "asr-gpu"->{var capabilities=new ArrayList<String>();if(executable(bin("whisperx")))capabilities.add("ASR:WHISPER_X");if(executable(bin("insanely-fast-whisper")))capabilities.add("ASR:INSANELY_FAST_WHISPER");yield capabilities;}
     case "asr-cloud"->List.of("GEMINI_TRANSCRIBE","MAI_TRANSCRIBE","ELEVENLABS_SCRIBE").stream().filter(p->configured("ASR_"+p+"_","_KEY")).map(p->"ASR:"+p).toList();
     case "translate"->configured("TRANSLATION_","_URL")?List.of("TRANSLATE"):List.of();case "publish"->configured("YOUTUBE_","_REFRESH_TOKEN")?List.of("UPLOAD_VIDEO","UPLOAD_SUBTITLES"):List.of();default->throw new IllegalArgumentException("Unsupported worker role: "+role);};
 }
 public boolean configured(){return role.equals("detector")?env.getProperty("DETECTOR_WATCH_FILE")!=null:!capabilities().isEmpty();}
 private boolean configured(String prefix,String suffix){return System.getenv().keySet().stream().anyMatch(k->k.startsWith(prefix)&&k.endsWith(suffix)&&!env.getProperty(k,"").isBlank());}
 private static boolean executable(String command){Path path=Path.of(command);if(path.isAbsolute())return Files.isRegularFile(path)&&Files.isExecutable(path);return Arrays.stream(System.getenv().getOrDefault("PATH","").split(java.io.File.pathSeparator)).anyMatch(p->Files.isExecutable(Path.of(p,command))||Files.isExecutable(Path.of(p,command+".exe")));}
 private String bin(String key){return env.getRequiredProperty("worker."+key);}
 public Map<String,Object> execute(ExecutionContext ctx)throws Exception{
   if(demo)return demo(ctx);
   return switch(ctx.lease.type()){
     case "DOWNLOAD"->download(ctx);case "VERIFY_MEDIA"->verify(ctx);case "PREPARE_AUDIO"->audio(ctx);case "ASR"->asr(ctx);
     case "TRANSLATE"->translate(ctx);
     case "UPLOAD_VIDEO","UPLOAD_SUBTITLES"->publish(ctx);
     default->throw new IllegalArgumentException("Worker does not implement "+ctx.lease.type());
   };
 }
 private Map<String,Object> download(ExecutionContext ctx)throws Exception{
   ctx.phase("DOWNLOADING");Path output=ctx.directory.resolve("capture.%(ext)s");
   if(ctx.lease.task().source().kind().equals("LIVE_URL")&&env.getProperty("BACKUP_LIVE_TOOL","yt-dlp").equals("ytarchive")){
     ctx.command(List.of(bin("ytarchive"),"--newline","--wait","--merge","-o",ctx.directory.resolve("capture").toString(),ctx.lease.task().source().url(),"best"),Duration.ZERO);
     try(var paths=Files.list(ctx.directory)){var captured=paths.filter(p->p.getFileName().toString().matches("capture\\.(mp4|mkv|webm)")&&Files.isRegularFile(p)).toList();if(captured.size()!=1||Files.size(captured.get(0))==0)throw new IllegalStateException("ytarchive media output missing or ambiguous");return ctx.artifacts(List.of(ctx.publish(captured.get(0),"VIDEO")));}
   }
   List<String> argv=new ArrayList<>(List.of(bin("yt-dlp"),"--ignore-config","--no-playlist","--newline","--progress","--progress-delta","2","--no-overwrites","--merge-output-format","mkv","-o",output.toString()));
   if(ctx.lease.task().source().kind().equals("LIVE_URL"))argv.add("--live-from-start");argv.add("--");argv.add(ctx.lease.task().source().url());
   ctx.command(argv,ctx.lease.task().source().kind().equals("LIVE_URL")?Duration.ZERO:Duration.ofHours(12));
   List<Path> files;try(var stream=Files.list(ctx.directory)){files=stream.filter(Files::isRegularFile).filter(p->p.getFileName().toString().matches("capture\\.(mkv|mp4|webm|m4a|mp3|ogg|wav|flac|opus|aac|aiff)")).toList();}
   if(files.size()!=1||Files.size(files.get(0))==0)throw new IllegalStateException("Download did not produce exactly one complete media artifact");
   return ctx.artifacts(List.of(ctx.publish(files.get(0),"VIDEO")));
 }
 private Map<String,Object> translate(ExecutionContext ctx)throws Exception{
   String profile=ctx.lease.task().translation().profileId()==null?"default":ctx.lease.task().translation().profileId();if(!profile.matches("[A-Za-z0-9_]{1,60}"))throw new IllegalArgumentException("Invalid translation profile");
   String prefix="TRANSLATION_"+profile.toUpperCase(Locale.ROOT);String endpoint=env.getRequiredProperty(prefix+"_URL"),authorization=env.getProperty(prefix+"_AUTHORIZATION"),sourceLanguage=env.getProperty(prefix+"_SOURCE_LANGUAGE");
   if(ctx.lease.inputs().stream().anyMatch(a->a.get("kind").equals("TRANSCRIPT"))){var transcript=json.readTree(ctx.input("TRANSCRIPT").toFile());if(transcript.hasNonNull("language"))sourceLanguage=transcript.get("language").asText();}
   var source=SubtitleFormats.parse(Files.readString(ctx.input("SRT","VTT","SOURCE")));ctx.phase("TRANSLATING");
   var result=new TranslationClient(json).translate(source,java.net.URI.create(endpoint),authorization,sourceLanguage,ctx.lease.parameters().get("language").toString(),ctx.directory.resolve("translation-receipts"),()->ctx.cancelled.get()||ctx.expired(),percent->ctx.phase("TRANSLATING_"+percent+"_PERCENT"));
   Path output=ctx.directory.resolve("translated.srt");Files.writeString(output,SubtitleFormats.render(result,false));return ctx.artifacts(List.of(ctx.publish(output,"SRT")));
 }
 private Map<String,Object> publish(ExecutionContext ctx)throws Exception{
   String ref=ctx.lease.task().output().accountRef();if(ref==null||!ref.matches("[A-Za-z0-9_]{1,60}"))throw new IllegalArgumentException("Select a configured YouTube account reference");String prefix="YOUTUBE_"+ref.toUpperCase(Locale.ROOT);
   var credentials=new YouTubePublisher.Credentials(env.getProperty(prefix+"_CLIENT_ID"),env.getProperty(prefix+"_CLIENT_SECRET"),env.getProperty(prefix+"_REFRESH_TOKEN"));
   var publisher=new YouTubePublisher(json);YouTubePublisher.Checkpoint checkpoint=remote->{ctx.remote(remote);ctx.report();};
   if(ctx.lease.type().equals("UPLOAD_VIDEO")){ctx.phase("UPLOADING_VIDEO");return publisher.video(ctx.input("VIDEO","SOURCE"),ctx.lease.task().source().name(),credentials,ctx.directory,checkpoint,()->ctx.cancelled.get()||ctx.expired());}
   String videoId=ctx.lease.task().output().videoId();for(Object dependency:ctx.lease.dependencyResults().values())if(dependency instanceof Map<?,?> map&&map.get("videoId") instanceof String id)videoId=id;
   String language=ctx.lease.parameters().get("language").toString();if(language.equals("original")){
     if(ctx.lease.inputs().stream().anyMatch(a->a.get("kind").equals("TRANSCRIPT")))language=json.readTree(ctx.input("TRANSCRIPT").toFile()).path("language").asText(null);
     if(language==null||language.equals("original"))language=env.getProperty(prefix+"_ORIGINAL_LANGUAGE");
   }
   ctx.phase("UPLOADING_SUBTITLES");return publisher.caption(ctx.input("SRT","VTT"),videoId,language,credentials,ctx.directory,checkpoint,()->ctx.cancelled.get()||ctx.expired());
 }
 private JsonNode probe(ExecutionContext ctx,Path file)throws Exception{
   var result=ctx.command(List.of(bin("ffprobe"),"-v","error","-show_entries","format=duration:stream=codec_type","-of","json",file.toString()),Duration.ofMinutes(2));
   JsonNode probe=json.readTree(result.stdoutTail());if(!probe.has("streams")||probe.get("streams").isEmpty())throw new IllegalArgumentException("No media streams found");
   if(probe.path("format").path("duration").asDouble(0)<=0)throw new IllegalArgumentException("Media duration is missing or zero");return probe;
 }
 private Map<String,Object> verify(ExecutionContext ctx)throws Exception{
   ctx.phase("VERIFYING");Path input=ctx.input("VIDEO","SOURCE","AUDIO");var probe=probe(ctx,input);
   return Map.of("artifactIds",ctx.lease.inputs().stream().filter(a->Set.of("VIDEO","SOURCE","AUDIO").contains(a.get("kind"))).map(a->a.get("id")).toList(),"durationSeconds",probe.path("format").path("duration").asDouble());
 }
 private Map<String,Object> audio(ExecutionContext ctx)throws Exception{
   ctx.phase("PREPARING_AUDIO");Path input=ctx.input("VIDEO","SOURCE","AUDIO"),output=ctx.directory.resolve("audio.wav");probe(ctx,input);
   ctx.command(List.of(bin("ffmpeg"),"-nostdin","-hide_banner","-y","-i",input.toString(),"-vn","-ac","1","-ar","16000","-c:a","pcm_s16le",output.toString()),Duration.ofHours(3));
   probe(ctx,output);return ctx.artifacts(List.of(ctx.publish(output,"AUDIO")));
 }
 private Map<String,Object> asr(ExecutionContext ctx)throws Exception{
   TaskSpec.Asr spec=AsrCatalog.normalize(ctx.lease.task().asr());ctx.phase("TRANSCRIBING");Path audio=ctx.input("AUDIO","SOURCE");
   if(AsrCatalog.get(spec.provider()).executionMode().equals("cloud")){
     String ref=spec.credentialRef()==null?"default":spec.credentialRef();if(!ref.matches("[A-Za-z0-9_]{1,60}"))throw new IllegalArgumentException("Invalid credential reference");
     String prefix="ASR_"+spec.provider()+"_"+ref.toUpperCase(Locale.ROOT);String key=env.getProperty(prefix+"_KEY"),endpoint=env.getProperty(prefix+"_ENDPOINT");
     var result=new CloudAsr(json).transcribe(spec,audio,ctx.directory,new CloudAsr.Credentials(key,endpoint),remote->{ctx.remote(remote);ctx.report();},()->ctx.cancelled.get()||ctx.expired());
     return publishTranscript(ctx,result.transcript(),result.raw());
   }
   if(spec.provider().equals("WHISPER_X")){
     Path output=ctx.directory.resolve("transcript");Files.createDirectories(output);
     List<String> argv=new ArrayList<>(List.of(bin("whisperx"),audio.toString(),"--model",spec.model(),"--output_dir",output.toString(),"--output_format","json","--batch_size",spec.providerOptions().get("batchSize").toString(),"--compute_type",spec.providerOptions().get("computeType").toString()));
     if(spec.options().get("languageHints") instanceof List<?> hints&&!hints.isEmpty()){argv.add("--language");argv.add(hints.get(0).toString());}
     if(Boolean.FALSE.equals(spec.providerOptions().get("align")))argv.add("--no_align");
     ctx.command(argv,Duration.ofHours(24));List<Path> files;try(var paths=Files.list(output)){files=paths.filter(p->p.toString().endsWith(".json")).toList();}
     if(files.size()!=1)throw new IllegalStateException("WhisperX transcript output missing or ambiguous");
     return publishTranscript(ctx,parseWhisper(json.readTree(files.get(0).toFile()),spec),files.get(0));
   }
   if(spec.provider().equals("INSANELY_FAST_WHISPER")){
     Path output=ctx.directory.resolve("raw.json");ctx.command(List.of(bin("insanely-fast-whisper"),"--file-name",audio.toString(),"--model-name",spec.model(),"--batch-size",spec.providerOptions().get("batchSize").toString(),"--transcript-path",output.toString()),Duration.ofHours(24));
     return publishTranscript(ctx,parseWhisper(json.readTree(output.toFile()),spec),output);
   }
   throw new IllegalArgumentException("ASR provider unavailable in this worker");
 }
 public static Transcript parseWhisper(JsonNode raw,TaskSpec.Asr spec){
   List<Transcript.Segment> segments=new ArrayList<>();JsonNode source=raw.has("segments")?raw.path("segments"):raw.path("chunks");
   for(JsonNode segment:source){String text=segment.path("text").asText().strip();if(text.isEmpty())continue;
     JsonNode start=segment.has("start")?segment.path("start"):segment.path("timestamp").path(0),end=segment.has("end")?segment.path("end"):segment.path("timestamp").path(1);
     if(!start.isNumber()||!end.isNumber())throw new IllegalArgumentException("Transcript has missing timestamps");
     segments.add(new Transcript.Segment(Math.round(start.asDouble()*1000),Math.round(end.asDouble()*1000),text,segment.has("speaker")?segment.get("speaker").asText():null));
   }
   if(segments.isEmpty())throw new IllegalArgumentException("ASR produced no timestamped speech");
   return new Transcript(spec.provider(),spec.model(),raw.path("language").asText(null),String.join(" ",segments.stream().map(Transcript.Segment::text).toList()),segments,List.of(),Map.of());
 }
 private Map<String,Object> publishTranscript(ExecutionContext ctx,Transcript transcript,Path raw)throws Exception{
   Path normalized=ctx.directory.resolve("transcript.json"),srt=ctx.directory.resolve("transcript.srt");json.writeValue(normalized.toFile(),transcript);
   List<Map<String,Object>> files=new ArrayList<>();files.add(ctx.publish(normalized,"TRANSCRIPT"));files.add(ctx.publish(raw,"RAW_RESPONSE"));
   if(!transcript.segments().isEmpty()){Files.writeString(srt,SubtitleFormats.render(transcript.segments(),false));files.add(ctx.publish(srt,"SRT"));}
   var result=new LinkedHashMap<>(ctx.artifacts(files));result.put("provider",transcript.provider());result.put("model",transcript.model());result.put("segments",transcript.segments().size());return result;
 }
 private Map<String,Object> demo(ExecutionContext ctx)throws Exception{
   ctx.phase("DEMO_"+ctx.lease.type());ctx.command(List.of(Path.of(System.getProperty("java.home"),"bin","java").toString(),"-version"),Duration.ofSeconds(10));
   Thread.sleep(1000);if(ctx.cancelled.get())throw new InterruptedException();Map<String,Object> result;
   switch(ctx.lease.type()){
     case "DOWNLOAD","PREPARE_AUDIO"->{Path wav=ctx.directory.resolve("demo.wav");writeSilentWav(wav);result=ctx.artifacts(List.of(ctx.publish(wav,ctx.lease.type().equals("DOWNLOAD")?"VIDEO":"AUDIO")));}
     case "VERIFY_MEDIA"->result=Map.of("artifactIds",ctx.lease.inputs().stream().map(a->a.get("id")).toList(),"durationSeconds",2);
     case "ASR"->{var transcript=new Transcript(ctx.lease.task().asr().provider(),"DEMO", "en","[DEMO] Offline workflow fixture.",List.of(new Transcript.Segment(0,1800,"[DEMO] Offline workflow fixture.",null)),List.of(),Map.of("demo",true));Path raw=ctx.directory.resolve("demo-raw.json");json.writeValue(raw.toFile(),Map.of("demo",true));result=publishTranscript(ctx,transcript,raw);}
     case "TRANSLATE"->{Path output=ctx.directory.resolve("translated.srt");Files.writeString(output,"1\n00:00:00,000 --> 00:00:01,800\n[DEMO "+ctx.lease.parameters().get("language")+"] Offline translation fixture.\n\n");result=ctx.artifacts(List.of(ctx.publish(output,"SRT")));}
     case "UPLOAD_VIDEO"->result=Map.of("videoId","demo-"+ctx.lease.taskId());
     case "UPLOAD_SUBTITLES"->result=Map.of("language",ctx.lease.parameters().get("language"),"uploaded",false);
     default->throw new IllegalArgumentException("Unsupported demo stage");
   }
   var tagged=new LinkedHashMap<>(result);tagged.put("demo",true);return tagged;
 }
 static void writeSilentWav(Path target)throws Exception{int bytes=64000;ByteBuffer out=ByteBuffer.allocate(44+bytes).order(ByteOrder.LITTLE_ENDIAN);out.put("RIFF".getBytes()).putInt(36+bytes).put("WAVEfmt ".getBytes()).putInt(16).putShort((short)1).putShort((short)1).putInt(16000).putInt(32000).putShort((short)2).putShort((short)16).put("data".getBytes()).putInt(bytes);Files.write(target,out.array());}
}
