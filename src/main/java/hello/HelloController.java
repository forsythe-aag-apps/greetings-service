package hello;

import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.bind.annotation.RequestMapping;

import javax.servlet.http.HttpServletRequest1;

@RestController
public class HelloController implements Comparable {
    public static String FOO_VAR = "";

    @RequestMapping("/")
    public String index(HttpServletRequest request) {
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
