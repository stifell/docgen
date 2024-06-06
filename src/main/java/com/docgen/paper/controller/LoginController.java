//package com.docgen.paper.controller;
//
//import com.docgen.paper.entity.User;
//import com.docgen.paper.repository.UserRepository;
//import jakarta.servlet.http.HttpServletRequest;
//import org.springframework.beans.factory.annotation.Autowired;
//import org.springframework.stereotype.Controller;
//import org.springframework.ui.Model;
//import org.springframework.web.bind.annotation.GetMapping;
//import org.springframework.web.bind.annotation.PostMapping;
//import org.springframework.web.bind.annotation.RequestParam;
//
///**
// * @author Денис on 05.06.2024
// */
//@Controller
//public class LoginController {
//
//    private final UserRepository userRepository;
//
//    @Autowired
//    public LoginController(UserRepository userRepository) {
//        this.userRepository = userRepository;
//    }
//
//    @GetMapping("/login")
//    public String loginPage(@RequestParam(value = "error", required = false) String error, Model model) {
//        if (error != null) {
//            model.addAttribute("error", "Неправильная почта или пароль");
//        }
//        return "login";
//    }
//
//    @PostMapping("/login")
//    public String handleLogin(@RequestParam("email") String email,
//                              @RequestParam("password") String password,
//                              Model model) {
//        User user = userRepository.findByEmail(email);
//        if (user != null && user.getPassword().equals(password)) {
//            // Assuming password is stored in plain text, which is not recommended
//            return "redirect:/history";
//        } else {
//            model.addAttribute("error", "Неправильная почта или пароль");
//            return "login";
//        }
//    }
//
//    @GetMapping("/history")
//    public String historyPage() {
//        return "history";
//    }
//}
