package idv.mark.platform.worker;

import com.fasterxml.jackson.databind.ObjectMapper;
import idv.mark.platform.core.model.WorkLease;
import idv.mark.platform.asr.CloudAsr;
import idv.mark.platform.publish.YouTubePublisher;
import idv.mark.platform.core.process.*;
import jakarta.annotation.PreDestroy;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.slf4j.Logger;import org.slf4j.LoggerFactory;
import java.nio.file.*;
import java.util.*;
import java.util.concurrent.*;

@Component
public class WorkerRuntime {
 private static final Logger log=LoggerFactory.getLogger(WorkerRuntime.class);
 private final ControlClient control;private final ObjectMapper json;private final StageHandlers handlers;private final String id,role;private final int slots;private final Path root;
 private final ExecutorService pool;private final ManagedProcessExecutor processes;private final Map<String,ExecutionContext> active=new ConcurrentHashMap<>();
 private volatile boolean registered,stopping;
 private final LiveDetector detector;
 public WorkerRuntime(ControlClient control,ObjectMapper json,StageHandlers handlers,LiveDetector detector,@Value("${worker.id}")String id,@Value("${worker.role}")String role,@Value("${worker.slots}")int slots,@Value("${worker.root}")String root,@Value("${worker.token}")String token)throws Exception{
   this.control=control;this.json=json;this.handlers=handlers;this.detector=detector;this.role=role;this.slots=slots;this.root=Path.of(root).toAbsolutePath().normalize();
   this.id=id.isBlank()?role+"-"+UUID.randomUUID():id;if(!this.id.matches("[A-Za-z0-9_-]{1,100}")||slots<1||slots>8)throw new IllegalArgumentException("Invalid worker ID or slots");
   Files.createDirectories(this.root);pool=Executors.newFixedThreadPool(slots);processes=new ManagedProcessExecutor(new SecretRedactor(List.of(token)));
 }
 @EventListener(ApplicationReadyEvent.class) public void ready(){tick();}
 @Scheduled(fixedDelay=2000) public void tick(){
   if(stopping)return;
   try{
     if(!registered){control.post("/internal/v1/workers/register",Map.of("workerId",id,"role",role,"version","0.1.0","slots",slots,"capabilities",handlers.capabilities(),"runtime",metadata()),null,Map.class);registered=true;}
     if(active.size()<slots){WorkLease lease=control.post("/internal/v1/workers/"+id+"/claim",Map.of(),null,WorkLease.class);if(lease!=null){var context=new ExecutionContext(lease,root,control,json,processes);active.put(lease.attemptId(),context);pool.submit(()->execute(context));}}
   }catch(Exception ex){log.warn("Worker poll unavailable: {}",ex.getClass().getSimpleName());}
 }
 @Scheduled(fixedDelay=5000) @SuppressWarnings("unchecked") public void heartbeat(){
   if(!registered||stopping)return;
   try{var response=control.post("/internal/v1/workers/"+id+"/heartbeat",Map.of("runtime",metadata()),null,Map.class);var cancelled=(List<String>)response.get("cancelAttemptIds");cancelled.forEach(a->{var ctx=active.get(a);if(ctx!=null)ctx.cancelled.set(true);});}
   catch(ControlClient.Rejected ex){if(ex.status==404)registered=false;}
   catch(Exception ex){log.warn("Worker heartbeat unavailable: {}",ex.getClass().getSimpleName());}
   for(var context:active.values())try{context.report();}catch(Exception ex){if(context.expired()||ex instanceof ControlClient.Rejected)context.cancelled.set(true);}
 }
 private Map<String,Object> metadata(){return Map.of("java",System.getProperty("java.version"),"os",System.getProperty("os.name"),"active",active.size(),"demo",handlers.demo(),"ready",handlers.configured(),"detector",detector.status());}
 private void execute(ExecutionContext context){
   Map<String,Object> result=new LinkedHashMap<>();result.put("outputs",Map.of());result.put("state","FAILED");
   try{context.report();result.put("outputs",handlers.execute(context));result.put("state",context.cancelled.get()?"CANCELLED":"COMPLETED");result.put("exitCode",0);}
   catch(ExecutionContext.CommandFailure ex){result.put("state",ex.outcome.state().name());result.put("exitCode",ex.outcome.exitCode());result.put("error",ex.getMessage());}
   catch(CloudAsr.UnknownResult ex){result.put("state","UNKNOWN");result.put("error",ex.getMessage());}
   catch(YouTubePublisher.UnknownResult ex){result.put("state","UNKNOWN");result.put("error",ex.getMessage());}
   catch(InterruptedException ex){Thread.currentThread().interrupt();result.put("state","CANCELLED");result.put("error","Execution interrupted");}
   catch(Exception ex){if(Files.exists(context.directory.resolve("provider-response.json"))||Files.exists(context.directory.resolve("youtube-session.json"))||Files.exists(context.directory.resolve("youtube-response.json")))result.put("state","UNKNOWN");result.put("error",ex.getClass().getSimpleName()+": "+new SecretRedactor(List.of()).redact(ex.getMessage()));}
   try{
     // Preserve a local completion receipt if control is unavailable; never repeat external work to recover reporting.
     json.writeValue(context.directory.resolve("completion.json").toFile(),result);
     if(!context.expired())try{context.report();}catch(Exception ex){log.warn("Final progress unavailable for attempt {}: {}",context.lease.attemptId(),ex.getClass().getSimpleName());}
     boolean reported=false;
     for(int retry=0;retry<3&&!context.expired();retry++)try{control.post("/internal/v1/attempts/"+context.lease.attemptId()+"/complete",result,context.lease.leaseToken(),Void.class);Files.writeString(context.directory.resolve(".reported"),result.get("state").toString());reported=true;break;}catch(ControlClient.Rejected ex){throw ex;}catch(Exception ex){Thread.sleep(1000);}
     if(!reported)log.warn("Completion could not be reported for attempt {}; receipt retained",context.lease.attemptId());
   }catch(Exception ex){log.warn("Completion needs reconciliation for attempt {}: {}",context.lease.attemptId(),ex.getClass().getSimpleName());}
   finally{active.remove(context.lease.attemptId());}
 }
 @PreDestroy public void close(){stopping=true;active.values().forEach(c->c.cancelled.set(true));processes.close();pool.shutdown();try{if(!pool.awaitTermination(20,TimeUnit.SECONDS))pool.shutdownNow();}catch(InterruptedException ex){pool.shutdownNow();Thread.currentThread().interrupt();}}
}
