package no.difi.virksert.server.controller;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;

@Controller
@RequestMapping("/")
public class HomeController {

    @RequestMapping
    public String home() {
        return "home";
    }

    @RequestMapping("/feed")
    public String front() {
        return "feed";
    }

}
