package com.example.movietickets.demo.controller.general;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;

@Controller
@RequestMapping("/popcorn")
public class PopcornController {
    @GetMapping()
    public String Popcorn() {
        return "popcorn/popcorn";
    }
}