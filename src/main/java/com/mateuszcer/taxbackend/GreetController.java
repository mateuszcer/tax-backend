package com.mateuszcer.taxbackend;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ResponseBody;

@Controller
public class GreetController {

    @GetMapping("/hello")
    @ResponseBody
    public String sayHello() {
        return "hello";
    }
}
