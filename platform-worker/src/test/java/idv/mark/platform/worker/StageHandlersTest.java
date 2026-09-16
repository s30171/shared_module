package idv.mark.platform.worker;
import com.fasterxml.jackson.databind.ObjectMapper;
import idv.mark.platform.core.model.*;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
class StageHandlersTest {
 @Test void whisperAndInsanelyUseRealTimestamps()throws Exception{
   var mapper=new ObjectMapper();var spec=AsrCatalog.normalize(new TaskSpec.Asr(null,null,null,null,null));
   var a=StageHandlers.parseWhisper(mapper.readTree("{\"segments\":[{\"start\":1.2,\"end\":2.5,\"text\":\"Hello\"}]}"),spec);
   var b=StageHandlers.parseWhisper(mapper.readTree("{\"chunks\":[{\"timestamp\":[1.2,2.5],\"text\":\"Hello\"}]}"),spec);
   assertEquals(a.segments(),b.segments());assertEquals(1200,a.segments().get(0).startMs());
 }
 @Test void truncatedOutputCannotBeMarkedSuccessful()throws Exception{
   var mapper=new ObjectMapper();var spec=AsrCatalog.normalize(new TaskSpec.Asr(null,null,null,null,null));
   assertThrows(IllegalArgumentException.class,()->StageHandlers.parseWhisper(mapper.readTree("{\"chunks\":[{\"timestamp\":[0,null],\"text\":\"unfinished\"}]}"),spec));
 }
}
