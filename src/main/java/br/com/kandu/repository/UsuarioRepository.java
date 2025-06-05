// src/main/java/br/com/kandu/repository/UsuarioRepository.java
package br.com.kandu.repository;

import br.com.kandu.entity.Usuario;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface UsuarioRepository extends JpaRepository<Usuario, Long> {

    // Métodos para busca global (usados por exemplo no login ou validação de cadastro público)
    Optional<Usuario> findByNomeUsuario(String nomeUsuario);
    Optional<Usuario> findByEmail(String email);

    // Métodos para verificar existência global (usados no cadastro público)
    boolean existsByNomeUsuario(String nomeUsuario); // Método que estava faltando na última sugestão para Issue #9
    boolean existsByEmail(String email);           // Método que estava faltando na última sugestão para Issue #9

    // Métodos para unicidade DENTRO de uma empresa (usados no cadastro por admin)
    boolean existsByNomeUsuarioAndEmpresaId(String nomeUsuario, Long empresaId);
    boolean existsByEmailAndEmpresaId(String email, Long empresaId);

    // Método para listar usuários de uma empresa
    List<Usuario> findByEmpresaId(Long empresaId);

    // Método para verificar se uma empresa possui usuários (usado no EmpresaService ao deletar empresa)
    boolean existsByEmpresaId(Long empresaId);
}
