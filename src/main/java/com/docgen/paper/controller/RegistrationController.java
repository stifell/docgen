package com.docgen.paper.controller;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

/**
 * @author Денис on 05.06.2024
 */
@Controller
public class RegistrationController {
    @GetMapping("/registration")
    public String showRegistrationPage() {
        return "registration";
    }

}
