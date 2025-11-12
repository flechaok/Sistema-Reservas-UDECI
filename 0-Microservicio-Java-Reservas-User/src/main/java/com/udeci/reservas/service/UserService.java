package com.udeci.reservas.service;

import com.udeci.reservas.model.Role;
import com.udeci.reservas.model.User;
import com.udeci.reservas.repository.RoleRepository;
import com.udeci.reservas.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import java.util.Collections;
import java.util.stream.Collectors;

@Service
public class UserService implements UserDetailsService {

    @Autowired
    private UserRepository userRepository;

    // 🔹 Carga usuario por email (no username)
    @Override
    public UserDetails loadUserByUsername(String email) throws UsernameNotFoundException {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new UsernameNotFoundException("Usuario no encontrado con email: " + email));

        return new org.springframework.security.core.userdetails.User(
                user.getEmail(),
                user.getPassword(),
                user.isActive(),
                true, true, true,
                user.getRoles().stream()
                        .map(role -> new SimpleGrantedAuthority("ROLE_" + role.getName()))
                        .collect(Collectors.toList())
        );
    }

    //  Para registro manual
    @Autowired
    private RoleRepository roleRepository;


public void saveUser(User user) {
    // activar usuario
    user.setActive(true);

    // asignar rol por defecto USER
    Role userRole = roleRepository.findByName("USER");
    user.setRoles(Collections.singleton(userRole));

    // guardar en la DB
    userRepository.save(user);
}


    public User findUserByEmailToClient(String email) {
    return userRepository.findByEmail(email).orElse(null);
}

}
