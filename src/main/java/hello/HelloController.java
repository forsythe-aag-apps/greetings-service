package hello;

import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.bind.annotation.RequestMapping;

@RestController
public class HelloController implements Comparable {
    public static String foo = "";
    
    @RequestMapping("/")
    public String index() {
        return "Greetings from Spring Boot!";
    }
    
    public Object clone() {
       return null;
    }
    
    public int compareTo(Object value) {
        if (value == null) {
            return Integer.MIN_VALUE;
        }
        
        return 0;
    }
}
