package hello;

import io.micrometer.core.annotation.Timed;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@Timed
public class HelloController {
    
    @RequestMapping("/")
    @Timed
    public String index() {
        return "Greetings from Spring Boot!";
    }
}
