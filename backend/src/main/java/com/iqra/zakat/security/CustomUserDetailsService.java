package com.iqra.zakat.security;

import com.iqra.zakat.entity.AppUser;
import com.iqra.zakat.repository.AppUserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class CustomUserDetailsService implements UserDetailsService {

    private final AppUserRepository userRepository;

    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        AppUser user = userRepository.findByUsername(username)
                .orElseThrow(() -> new UsernameNotFoundException("User not found: " + username));
        if (!"Y".equalsIgnoreCase(user.getIsActive())) {
            throw new UsernameNotFoundException("User is deactivated: " + username);
        }
        return UserPrincipal.create(user);
    }
}
