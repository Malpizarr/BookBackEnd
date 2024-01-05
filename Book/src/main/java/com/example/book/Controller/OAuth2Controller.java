package com.example.book.Controller;

import com.example.book.Model.User;
import com.example.book.Service.CustomOAuth2UserService;
import com.example.book.Service.UserService;
import com.example.book.Util.JwtTokenUtil;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.security.oauth2.client.authentication.OAuth2AuthenticationToken;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.http.ResponseEntity;

import java.io.IOException;

@RestController
@RequestMapping("/OAUTH")
public class OAuth2Controller {

    @Autowired
    private CustomOAuth2UserService customOAuth2UserService;

    @Autowired
    private JwtTokenUtil jwtTokenUtil;

    @Autowired
    private UserService userService;


    @GetMapping("/obtener-jwt")
    public ResponseEntity<String> obtenerJwt(HttpServletRequest request) {
        HttpSession session = request.getSession(true);
        if (session != null) {
            String jwt = (String) session.getAttribute("JWT");
            if (jwt != null) {
                return ResponseEntity.ok(jwt);
            }
        }
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
    }




}
