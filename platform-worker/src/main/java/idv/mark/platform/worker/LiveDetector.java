package idv.mark.platform.worker;
import com.fasterxml.jackson.databind.ObjectMapper;
import idv.mark.platform.core.model.TaskSpec;
import idv.mark.platform.core.process.*;
import jakarta.annotation.PreDestroy;
import org.springframework.core.env.Environment;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import java.net.URI;
import java.nio.file.*;
import java.time.*;
import java.util.*;

/** Opt-in detector configuration. All detected events submit the same durable task API. */
@Component
public class LiveDetector {
 public record Watch(String id,String channelUrl,TaskSpec task){}
 private final Environment env;private final ControlClient control;private final ObjectMapper json;private final ManagedProcessExecutor processes=new ManagedProcessExecutor();
 private volatile Map<String,Object> status=Map.of("state","NOT_CONFIGURED");
 public LiveDetector(Environment env,ControlClient control,ObjectMapper json){this.env=env;this.control=control;this.json=json;}
 public Map<String,Object> status(){return status;}
 @Scheduled(fixedDelayString="${DETECTOR_INTERVAL_MS:120000}",initialDelay=5000) public void scan(){
   if(!env.getRequiredProperty("worker.role").equals("detector"))return;
   String config=env.getProperty("DETECTOR_WATCH_FILE");if(config==null||config.isBlank())return;
   try{
     List<Watch> watches=json.readValue(Path.of(config).toFile(),json.getTypeFactory().constructCollectionType(List.class,Watch.class));if(watches.size()>50)throw new IllegalArgumentException("At most 50 channel watches per detector");
     Path root=Path.of(env.getRequiredProperty("worker.root")).toAbsolutePath().resolve("detector");Files.createDirectories(root);
     for(Watch watch:watches){
       if(watch.id()==null||!watch.id().matches("[A-Za-z0-9_-]{1,60}"))throw new IllegalArgumentException("Invalid watch ID");
       URI uri=URI.create(watch.channelUrl());if(!"https".equals(uri.getScheme())||!Set.of("youtube.com","www.youtube.com").contains(uri.getHost())||uri.getUserInfo()!=null)throw new IllegalArgumentException("Detector requires a YouTube channel HTTPS URL");
       status=Map.of("state","CHECKING","watchId",watch.id(),"startedAt",System.currentTimeMillis());
       var spec=new ProcessSpec("detect-"+watch.id(),List.of(env.getRequiredProperty("worker.yt-dlp"),"--ignore-config","--no-playlist","--skip-download","--socket-timeout","15","--print","{\"id\":%(id)j,\"live_status\":%(live_status)j,\"title\":%(title)j}",watch.channelUrl()),root,Map.of(),Duration.ofSeconds(45),Duration.ofSeconds(3),root.resolve("logs"),32768,1_000_000,2);
       var outcome=processes.execute(spec,new ProcessObserver(){public void started(String id,long pid,Instant started){status=Map.of("state","CHECKING","watchId",watch.id(),"pid",pid,"startedAt",started.toEpochMilli());}},()->Thread.currentThread().isInterrupted());
       if(!outcome.success()){status=Map.of("state","NO_LIVE_OR_CHECK_FAILED","watchId",watch.id(),"checkedAt",System.currentTimeMillis(),"exitCode",outcome.exitCode()==null?-1:outcome.exitCode());continue;}
       var event=json.readTree(outcome.stdoutTail().strip());if(!"is_live".equals(event.path("live_status").asText())){status=Map.of("state","IDLE","watchId",watch.id(),"checkedAt",System.currentTimeMillis());continue;}
       String videoId=event.path("id").asText();TaskSpec template=watch.task()==null?new TaskSpec("CAPTURE_ONLY",null,null,null,null):watch.task();
       TaskSpec task=new TaskSpec(template.workflow(),new TaskSpec.Source("LIVE_URL","youtube","https://www.youtube.com/watch?v="+videoId,null,event.path("title").asText()),template.asr(),template.translation(),template.output());
       control.post("/internal/v1/detections",Map.of("detectorId",watch.id(),"videoId",videoId,"task",task),null,Map.class);
       status=Map.of("state","SUBMITTED","watchId",watch.id(),"videoId",videoId,"checkedAt",System.currentTimeMillis());
     }
   }catch(Exception ex){status=Map.of("state","ERROR","errorType",ex.getClass().getSimpleName(),"checkedAt",System.currentTimeMillis());}
 }
 @PreDestroy public void close(){processes.close();}
}
