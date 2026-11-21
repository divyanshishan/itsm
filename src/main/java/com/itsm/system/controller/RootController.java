package com.itsm.system.controller;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

/**
 * Root Controller ensuring GET "/" cleanly forwards to "/index.html".
 */
@Controller
public class RootController {

    @GetMapping("/")
    public String root() {
        return "forward:/index.html";
    }
}
