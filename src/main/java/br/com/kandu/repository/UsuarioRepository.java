package br.com.kandu.repository;

import br.com.kandu.entity.Usuario;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository // Opcional se estender JpaRepository, mas boa prática para clareza
public interface UsuarioRepository extends JpaRepository<Usuario, Long> {

    // Spring Data JPA criará a implementação para estes métodos automaticamente
    Optional<Usuario> findByNomeUsuario(String nomeUsuario);
    Optional<Usuario> findByEmail(String email);
    boolean existsByNomeUsuario(String nomeUsuario);
    boolean existsByEmail(String email);
}