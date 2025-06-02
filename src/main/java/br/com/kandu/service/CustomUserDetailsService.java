// src/main/java/br/com/kandu/service/CustomUserDetailsService.java
package br.com.kandu.service;

import br.com.kandu.entity.Usuario;
import br.com.kandu.repository.UsuarioRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.User; // User do Spring Security
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Collection;
import java.util.Collections; // Para criar lista de autoridades facilmente

@Service
public class CustomUserDetailsService implements UserDetailsService {

    private final UsuarioRepository usuarioRepository;

    @Autowired
    public CustomUserDetailsService(UsuarioRepository usuarioRepository) {
        this.usuarioRepository = usuarioRepository;
    }

    @Override
    @Transactional(readOnly = true) // Boa prática para métodos de leitura
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        Usuario usuario = usuarioRepository.findByNomeUsuario(username)
                .orElseThrow(() ->
                        new UsernameNotFoundException("Usuário não encontrado com o nome: " + username)
                );

        // As "authorities" (ou roles) são prefixadas com "ROLE_" por convenção no Spring Security,
        // se você usar checagens baseadas em roles como hasRole('ADM').
        // Para NivelHierarquia, podemos usar diretamente o nome do enum como autoridade.
        // Ou, se quisermos usar hasRole(), seria "ROLE_" + usuario.getNivelHierarquia().name()
        Collection<? extends GrantedAuthority> authorities =
                Collections.singletonList(new SimpleGrantedAuthority(usuario.getNivelHierarquia().name()));
        // Exemplo se fosse usar "ROLE_":
        // Collection<? extends GrantedAuthority> authorities =
        // Collections.singletonList(new SimpleGrantedAuthority("ROLE_" + usuario.getNivelHierarquia().name()));


        // Retorna um objeto User do Spring Security que implementa UserDetails
        return new User(usuario.getNomeUsuario(), usuario.getSenha(), usuario.isAtivo(),
                true, // accountNonExpired
                true, // credentialsNonExpired
                true, // accountNonLocked
                authorities);
    }
}