package idv.mark.platform.worker;

import com.fasterxml.jackson.databind.ObjectMapper;
import idv.mark.platform.core.model.WorkLease;
import idv.mark.platform.core.process.*;
import java.nio.file.*;
import java.time.*;
import java.util.*;
import java.util.concurrent.atomic.AtomicBoolean;

public final class ExecutionContext implements ProcessObserver {
 public final WorkLease lease;public final Path directory;public final AtomicBoolean cancelled=new AtomicBoolean();
 private final ControlClient control;private final ObjectMapper json;private final ManagedProcessExecutor processes;
 private final Deque<Map<String,String>> lines=new ArrayDeque<>();
 private Map<String,Object> progress=Map.of("phase","STARTING"),remote=Map.of();private Long pid;private String processStarted;
 private boolean outputObserved,progressObserved;private long sequence,leaseUntil;private int commandNumber;
 public ExecutionContext(WorkLease lease,Path root,ControlClient control,ObjectMapper json,ManagedProcessExecutor processes)throws Exception{
   this.lease=lease;this.control=control;this.json=json;this.processes=processes;directory=root.resolve(lease.attemptId());Files.createDirectories(directory);leaseUntil=lease.leaseUntil();
 }
 @Override public synchronized void started(String id,long pid,Instant started){this.pid=pid;processStarted=started.toString();var update=new LinkedHashMap<>(progress);update.put("processAlive",true);progress=update;}
 @Override public synchronized void finished(ProcessOutcome outcome){var update=new LinkedHashMap<>(progress);update.put("processAlive",false);update.put("processState",outcome.state().name());progress=update;}
 @Override public synchronized void output(String id,String stream,String line,Instant time){
   outputObserved=true;if(lines.size()>=40)lines.removeFirst();lines.addLast(Map.of("stream",stream,"line",line.length()>3900?line.substring(0,3900):line));
   var decoded=ProgressDecoder.decode(lease.task().source().kind().equals("LIVE_URL")?"ytarchive":"ffmpeg",line,null);if(decoded!=null){var update=new LinkedHashMap<String,Object>(json.convertValue(decoded,Map.class));update.put("processAlive",true);progress=update;progressObserved=true;}
 }
 public synchronized void phase(String phase){progress=Map.of("phase",phase);progressObserved=true;}
 public synchronized void remote(Map<String,Object> value){remote=Map.copyOf(value);}
 public synchronized boolean expired(){return System.currentTimeMillis()>leaseUntil-5000;}
 @SuppressWarnings("unchecked") public void report()throws Exception{
   Map<String,Object> body=new LinkedHashMap<>();
   synchronized(this){body.put("sequence",++sequence);body.put("progress",progress);body.put("pid",pid);body.put("processStartedAt",processStarted);body.put("outputObserved",outputObserved);body.put("progressObserved",progressObserved);body.put("remote",remote);body.put("logs",List.copyOf(lines));lines.clear();outputObserved=false;progressObserved=false;}
   Map<String,Object> reply=control.post("/internal/v1/attempts/"+lease.attemptId()+"/progress",body,lease.leaseToken(),Map.class);
   synchronized(this){leaseUntil=Math.max(leaseUntil,((Number)reply.get("leaseUntil")).longValue());}if("CANCEL_REQUESTED".equals(reply.get("state")))cancelled.set(true);
 }
 public ProcessOutcome command(List<String> argv,Duration timeout)throws Exception{
   if(cancelled.get()||expired())throw new InterruptedException("Execution cancelled or lease lost");
   var spec=new ProcessSpec(lease.attemptId()+"-"+(++commandNumber),argv,directory,Map.of("PYTHONUNBUFFERED","1"),timeout,Duration.ofSeconds(5),directory.resolve("logs"),32768,5_000_000,3);
   var outcome=processes.execute(spec,this,()->cancelled.get()||expired());
   if(!outcome.success())throw new CommandFailure(outcome);return outcome;
 }
 public static class CommandFailure extends Exception{public final ProcessOutcome outcome;CommandFailure(ProcessOutcome outcome){super("Command "+outcome.state()+" (exit "+outcome.exitCode()+")");this.outcome=outcome;}}
 public Path input(String... kinds)throws Exception{
   var artifact=Arrays.stream(kinds).flatMap(kind->lease.inputs().stream().filter(a->kind.equals(a.get("kind")))).findFirst().orElseThrow(()->new IllegalArgumentException("Required input artifact missing: "+String.join(",",kinds)));
   String filename=((String)artifact.get("filename")).replaceAll("[^a-zA-Z0-9._-]","_");Path target=directory.resolve(artifact.get("id")+"-"+filename);
   if(!Files.exists(target))control.download((String)artifact.get("id"),target);
   if(Files.size(target)!=((Number)artifact.get("bytes")).longValue())throw new IllegalStateException("Artifact size mismatch");
   var digest=java.security.MessageDigest.getInstance("SHA-256");try(var stream=Files.newInputStream(target)){byte[] buffer=new byte[65536];int count;while((count=stream.read(buffer))!=-1)digest.update(buffer,0,count);}
   if(!HexFormat.of().formatHex(digest.digest()).equals(artifact.get("sha256")))throw new IllegalStateException("Artifact checksum mismatch");
   return target;
 }
 public Map<String,Object> publish(Path file,String kind)throws Exception{return control.upload(file,kind,lease.attemptId(),lease.leaseToken(),Map.of("provider",lease.task().asr().provider()));}
 public Map<String,Object> artifacts(List<Map<String,Object>> files){return Map.of("artifactIds",files.stream().map(a->a.get("id")).toList());}
}
