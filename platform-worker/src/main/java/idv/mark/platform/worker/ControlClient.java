package idv.mark.platform.worker;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import java.net.URI;
import java.net.http.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.time.Duration;
import java.util.*;

/** Streaming artifact transport. Control URL and credentials are deployment settings, never task input. */
@Component
public class ControlClient {
 private final URI base;private final String token;private final ObjectMapper json;
 private final HttpClient http=HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(10)).followRedirects(HttpClient.Redirect.NEVER).build();
 public ControlClient(ObjectMapper json,@Value("${worker.control-url}") String base,@Value("${worker.token}") String token){
   this.json=json;this.base=URI.create(base);this.token=token;
   if(!Set.of("http","https").contains(this.base.getScheme())||this.base.getHost()==null||this.base.getUserInfo()!=null||token.length()<24)throw new IllegalArgumentException("Configure control URL and worker token");
 }
 public static final class Rejected extends RuntimeException {public final int status;Rejected(int status){super("Control request rejected: HTTP "+status);this.status=status;}}
 private HttpRequest.Builder request(String path,String lease){var builder=HttpRequest.newBuilder(base.resolve(path)).timeout(Duration.ofSeconds(25)).header("Authorization","Bearer "+token);if(lease!=null)builder.header("X-Lease-Token",lease);return builder;}
 public <T>T post(String path,Object body,String lease,Class<T> type)throws Exception{
   var response=http.send(request(path,lease).header("Content-Type","application/json").POST(HttpRequest.BodyPublishers.ofString(json.writeValueAsString(body))).build(),HttpResponse.BodyHandlers.ofString());
   check(response.statusCode());if(response.statusCode()==204||type==Void.class)return null;return json.readValue(response.body(),type);
 }
 public void download(String id,Path target)throws Exception{
   if(!id.matches("[a-zA-Z0-9-]{1,100}"))throw new IllegalArgumentException("Invalid artifact ID");
   var response=http.send(request("/internal/v1/artifacts/"+id+"/content",null).timeout(Duration.ofHours(2)).GET().build(),HttpResponse.BodyHandlers.ofInputStream());
   try(var stream=response.body()){check(response.statusCode());Files.copy(stream,target);}
 }
 @SuppressWarnings("unchecked") public Map<String,Object> upload(Path file,String kind,String attempt,String lease,Map<String,String> metadata)throws Exception{
   String boundary="platform-"+UUID.randomUUID();List<HttpRequest.BodyPublisher> parts=new ArrayList<>();
   Map<String,String> fields=new LinkedHashMap<>(metadata);fields.put("kind",kind);fields.put("attemptId",attempt);
   for(var entry:fields.entrySet())parts.add(HttpRequest.BodyPublishers.ofString("--"+boundary+"\r\nContent-Disposition: form-data; name=\""+entry.getKey()+"\"\r\n\r\n"+entry.getValue()+"\r\n"));
   String filename=file.getFileName().toString().replaceAll("[^a-zA-Z0-9._-]","_");
   parts.add(HttpRequest.BodyPublishers.ofString("--"+boundary+"\r\nContent-Disposition: form-data; name=\"file\"; filename=\""+filename+"\"\r\nContent-Type: application/octet-stream\r\n\r\n"));
   parts.add(HttpRequest.BodyPublishers.ofFile(file));parts.add(HttpRequest.BodyPublishers.ofString("\r\n--"+boundary+"--\r\n"));
   var response=http.send(request("/internal/v1/artifacts",lease).timeout(Duration.ofHours(2)).header("Content-Type","multipart/form-data; boundary="+boundary).POST(HttpRequest.BodyPublishers.concat(parts.toArray(HttpRequest.BodyPublisher[]::new))).build(),HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
   check(response.statusCode());return json.readValue(response.body(),Map.class);
 }
 private static void check(int status){if(status<200||status>=300)throw new Rejected(status);}
}
