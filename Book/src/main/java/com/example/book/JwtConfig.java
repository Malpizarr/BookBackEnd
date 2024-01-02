package com.example.book;

import com.example.book.Util.JwtTokenUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

@Configuration
public class JwtConfig {

    @Autowired
    private JwtUserDetailsService userDetailsService;

    @Autowired
    private JwtTokenUtil jwtTokenUtil;

    @Bean
    public JwtAuthenticationFilter jwtAuthenticationFilter(JwtUserDetailsService userDetailsService,
                                                           JwtTokenUtil jwtTokenUtil) {

        return new JwtAuthenticationFilter(userDetailsService, jwtTokenUtil);
    }

}
