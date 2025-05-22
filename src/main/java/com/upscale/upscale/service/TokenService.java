package com.upscale.upscale.service;

import com.upscale.upscale.security.JwtTokenUtil;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class TokenService {

    @Autowired
    private JwtTokenUtil jwtTokenUtil;

    public String getEmailFromToken(HttpServletRequest request){

        String authHeader = request.getHeader("Authorization");
        String token = "";
        if (authHeader != null && authHeader.startsWith("Bearer ")) {
            token = authHeader.substring(7);
        }
        //set all data;
        return jwtTokenUtil.getEmailFromToken(token);
    }

    public String generateToken(String emailId){
        return jwtTokenUtil.generateToken(emailId);
    }
}
