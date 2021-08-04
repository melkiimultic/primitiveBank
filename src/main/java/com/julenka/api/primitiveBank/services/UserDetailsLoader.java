package com.julenka.api.primitiveBank.services;

import com.julenka.api.primitiveBank.domain.User;
import com.julenka.api.primitiveBank.repositories.UserRepo;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Component;

import java.util.Optional;

@RequiredArgsConstructor
@Component
public class UserDetailsLoader implements UserDetailsService {

    private final UserRepo repo;

    @Override
    public User loadUserByUsername(String username) throws UsernameNotFoundException {
        Optional<User> user = repo.findOneByUsername(username);
        if(!user.isPresent()){
            throw new UsernameNotFoundException("No such user!");
        }
        return user.get();
    }
}
