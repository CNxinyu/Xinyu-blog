package com.xinyu.auth.security;

import com.xinyu.user.entity.UserEntity;
import com.xinyu.user.service.UserService;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

@Service
public class DatabaseUserDetailsService implements UserDetailsService {
    private final UserService userService;

    public DatabaseUserDetailsService(UserService userService) {
        this.userService = userService;
    }

    @Override
    public UserDetails loadUserByUsername(String identifier) throws UsernameNotFoundException {
        UserEntity user = userService.findByIdentifier(identifier);
        if (user == null) {
            throw new UsernameNotFoundException("invalid credentials");
        }
        return UserPrincipal.from(user);
    }
}
