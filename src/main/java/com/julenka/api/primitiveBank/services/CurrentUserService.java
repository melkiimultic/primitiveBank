package com.julenka.api.primitiveBank.services;

import com.julenka.api.primitiveBank.domain.User;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

@RequiredArgsConstructor
@Service
public class CurrentUserService {

    private final UserDetailsLoader loader;

    private String  getCurrentUsername(){
       return SecurityContextHolder.getContext().getAuthentication().getName();
    }

    public User getCurrentUser(){
        String name = getCurrentUsername();
        if(name==null){
            throw new BadCredentialsException("This user has authentication problems");
        }
        return loader.loadUserByUsername(name);
    }

}
