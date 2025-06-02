// src/test/java/br/com/kandu/repository/UsuarioRepositoryTest.java
package br.com.kandu.repository;

import br.com.kandu.entity.Usuario;
import br.com.kandu.enums.NivelHierarquia;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager; // Para persistir e limpar dados no teste
import org.springframework.dao.DataIntegrityViolationException; // Para testar constraints

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;

@DataJpaTest // Configura um ambiente de teste focado na camada JPA (usa H2 por padrão se configurado)
public class UsuarioRepositoryTest {

    @Autowired
    private TestEntityManager entityManager; // Ajuda a manipular entidades no contexto de persistência do teste

    @Autowired
    private UsuarioRepository usuarioRepository;

    @Test
    @DisplayName("Deve persistir um usuário com sucesso")
    void devePersistirUsuarioComSucesso() {
        // Cenário
        Usuario novoUsuario = Usuario.builder()
                .nomeCompleto("Usuário de Teste")
                .nomeUsuario("testuser")
                .email("test@example.com")
                .senha("senha123") // Em um cenário real, a senha seria hasheada antes de salvar
                .nivelHierarquia(NivelHierarquia.COMUM)
                .ativo(true)
                .build();

        // Ação: Persistir usando o repository
        Usuario usuarioSalvo = usuarioRepository.save(novoUsuario);

        // Verificação
        assertThat(usuarioSalvo).isNotNull();
        assertThat(usuarioSalvo.getId()).isNotNull();
        assertThat(usuarioSalvo.getNomeUsuario()).isEqualTo("testuser");

        // Opcional: verificar diretamente no entityManager para garantir que foi para o BD
        Usuario usuarioEncontradoNoBd = entityManager.find(Usuario.class, usuarioSalvo.getId());
        assertThat(usuarioEncontradoNoBd).isNotNull();
        assertThat(usuarioEncontradoNoBd.getEmail()).isEqualTo("test@example.com");
    }

    @Test
    @DisplayName("Deve encontrar um usuário pelo nomeUsuario")
    void deveEncontrarUsuarioPeloNomeUsuario() {
        // Cenário: Persistir um usuário de referência
        Usuario usuarioReferencia = Usuario.builder()
                .nomeCompleto("Usuário Buscável")
                .nomeUsuario("buscavelcome")
                .email("busca@example.com")
                .senha("senha123")
                .nivelHierarquia(NivelHierarquia.COMUM)
                .ativo(true)
                .build();
        entityManager.persistAndFlush(usuarioReferencia); // Garante que está no BD

        // Ação
        Optional<Usuario> usuarioEncontradoOpt = usuarioRepository.findByNomeUsuario("buscavelcome");

        // Verificação
        assertThat(usuarioEncontradoOpt).isPresent();
        assertThat(usuarioEncontradoOpt.get().getEmail()).isEqualTo("busca@example.com");
    }

    @Test
    @DisplayName("Não deve encontrar um usuário com nomeUsuario inexistente")
    void naoDeveEncontrarUsuarioComNomeUsuarioInexistente() {
        // Ação
        Optional<Usuario> usuarioEncontradoOpt = usuarioRepository.findByNomeUsuario("naoexiste");

        // Verificação
        assertThat(usuarioEncontradoOpt).isNotPresent();
    }

    @Test
    @DisplayName("Deve falhar ao tentar persistir usuário com nomeUsuario duplicado")
    void deveFalharAoPersistirNomeUsuarioDuplicado() {
        // Cenário
        Usuario usuario1 = Usuario.builder()
                .nomeCompleto("Usuário Um")
                .nomeUsuario("duplicado")
                .email("um@example.com")
                .senha("senha1")
                .nivelHierarquia(NivelHierarquia.COMUM)
                .build();
        entityManager.persistAndFlush(usuario1);

        Usuario usuario2 = Usuario.builder()
                .nomeCompleto("Usuário Dois")
                .nomeUsuario("duplicado") // Mesmo nomeUsuario
                .email("dois@example.com")
                .senha("senha2")
                .nivelHierarquia(NivelHierarquia.ADM)
                .build();

        // Ação & Verificação
        // Espera-se uma DataIntegrityViolationException devido à constraint unique
        assertThrows(DataIntegrityViolationException.class, () -> {
            usuarioRepository.saveAndFlush(usuario2); // saveAndFlush força a escrita no BD e a verificação da constraint
        });
    }

    @Test
    @DisplayName("Deve falhar ao tentar persistir usuário com email duplicado")
    void deveFalharAoPersistirEmailDuplicado() {
        // Cenário
        Usuario usuario1 = Usuario.builder()
                .nomeCompleto("Usuário Email Um")
                .nomeUsuario("emailuser1")
                .email("duplicado@example.com")
                .senha("senha1")
                .nivelHierarquia(NivelHierarquia.COMUM)
                .build();
        entityManager.persistAndFlush(usuario1);

        Usuario usuario2 = Usuario.builder()
                .nomeCompleto("Usuário Email Dois")
                .nomeUsuario("emailuser2")
                .email("duplicado@example.com") // Mesmo email
                .senha("senha2")
                .nivelHierarquia(NivelHierarquia.ADM)
                .build();

        // Ação & Verificação
        assertThrows(DataIntegrityViolationException.class, () -> {
            usuarioRepository.saveAndFlush(usuario2);
        });
    }
}