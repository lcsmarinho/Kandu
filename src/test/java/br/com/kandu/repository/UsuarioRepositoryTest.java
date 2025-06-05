package br.com.kandu.repository;

import br.com.kandu.entity.Empresa; // Novo import
import br.com.kandu.entity.Usuario;
import br.com.kandu.enums.NivelHierarquia;
import org.junit.jupiter.api.BeforeEach; // Novo import
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;
import org.springframework.dao.DataIntegrityViolationException;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;

@DataJpaTest
public class UsuarioRepositoryTest {

    @Autowired
    private TestEntityManager entityManager;

    @Autowired
    private UsuarioRepository usuarioRepository;

    private Empresa empresaTeste; // Empresa para associar aos usuários

    @BeforeEach // Executa antes de cada método de teste
    void setUp() {
        // Cria uma empresa de teste para ser usada nos testes de usuário
        // Isso garante que cada teste tenha uma empresa limpa, se necessário,
        // ou que possamos referenciar uma empresa persistida.
        empresaTeste = Empresa.builder()
                .nome("Empresa de Teste Padrão")
                .codigoInscricao("ETP001")
                .build();
        entityManager.persistAndFlush(empresaTeste); // Persiste a empresa para que os usuários possam referenciá-la
    }

    @Test
    @DisplayName("Deve persistir um usuário com sucesso associado a uma empresa")
    void devePersistirUsuarioComSucesso() {
        Usuario novoUsuario = Usuario.builder()
                .nomeCompleto("Usuário de Teste")
                .nomeUsuario("testuser")
                .email("test@example.com")
                .senha("senha123")
                .nivelHierarquia(NivelHierarquia.COMUM)
                .ativo(true)
                .empresa(empresaTeste) // Associa à empresa criada no setUp
                .build();

        Usuario usuarioSalvo = usuarioRepository.save(novoUsuario);

        assertThat(usuarioSalvo).isNotNull();
        assertThat(usuarioSalvo.getId()).isNotNull();
        assertThat(usuarioSalvo.getNomeUsuario()).isEqualTo("testuser");
        assertThat(usuarioSalvo.getEmpresa()).isNotNull();
        assertThat(usuarioSalvo.getEmpresa().getId()).isEqualTo(empresaTeste.getId());

        Usuario usuarioEncontradoNoBd = entityManager.find(Usuario.class, usuarioSalvo.getId());
        assertThat(usuarioEncontradoNoBd).isNotNull();
        assertThat(usuarioEncontradoNoBd.getEmail()).isEqualTo("test@example.com");
        assertThat(usuarioEncontradoNoBd.getEmpresa()).isNotNull();
        assertThat(usuarioEncontradoNoBd.getEmpresa().getCodigoInscricao()).isEqualTo("ETP001");
    }

    @Test
    @DisplayName("Deve encontrar um usuário pelo nomeUsuario")
    void deveEncontrarUsuarioPeloNomeUsuario() {
        Usuario usuarioReferencia = Usuario.builder()
                .nomeCompleto("Usuário Buscável")
                .nomeUsuario("buscavelcome")
                .email("busca@example.com")
                .senha("senha123")
                .nivelHierarquia(NivelHierarquia.COMUM)
                .ativo(true)
                .empresa(empresaTeste)
                .build();
        entityManager.persistAndFlush(usuarioReferencia);

        Optional<Usuario> usuarioEncontradoOpt = usuarioRepository.findByNomeUsuario("buscavelcome");

        assertThat(usuarioEncontradoOpt).isPresent();
        assertThat(usuarioEncontradoOpt.get().getEmail()).isEqualTo("busca@example.com");
    }

    // ... (os testes de 'naoDeveEncontrarUsuarioComNomeUsuarioInexistente',
    // 'deveFalharAoPersistirNomeUsuarioDuplicado', e 'deveFalharAoPersistirEmailDuplicado'
    // podem continuar como estão, mas agora os usuários criados neles também devem ter a empresaTeste associada
    // para consistência, embora a constraint de unicidade de nomeUsuario e email ainda seja global
    // conforme definido na entidade Usuario. Se a constraint fosse (nomeUsuario, empresaId), os testes
    // precisariam ser mais elaborados para testar duplicidade dentro da mesma empresa vs. empresas diferentes)

    @Test
    @DisplayName("Não deve encontrar um usuário com nomeUsuario inexistente")
    void naoDeveEncontrarUsuarioComNomeUsuarioInexistente() {
        Optional<Usuario> usuarioEncontradoOpt = usuarioRepository.findByNomeUsuario("naoexiste");
        assertThat(usuarioEncontradoOpt).isNotPresent();
    }

    @Test
    @DisplayName("Deve falhar ao tentar persistir usuário com nomeUsuario duplicado")
    void deveFalharAoPersistirNomeUsuarioDuplicado() {
        Usuario usuario1 = Usuario.builder()
                .nomeCompleto("Usuário Um")
                .nomeUsuario("duplicado")
                .email("um@example.com")
                .senha("senha1")
                .nivelHierarquia(NivelHierarquia.COMUM)
                .empresa(empresaTeste)
                .build();
        entityManager.persistAndFlush(usuario1);

        Usuario usuario2 = Usuario.builder()
                .nomeCompleto("Usuário Dois")
                .nomeUsuario("duplicado")
                .email("dois@example.com")
                .senha("senha2")
                .nivelHierarquia(NivelHierarquia.ADM)
                .empresa(empresaTeste) // Pode ser a mesma ou outra empresa, a constraint é global por enquanto
                .build();

        assertThrows(DataIntegrityViolationException.class, () -> {
            usuarioRepository.saveAndFlush(usuario2);
        });
    }

    @Test
    @DisplayName("Deve falhar ao tentar persistir usuário com email duplicado")
    void deveFalharAoPersistirEmailDuplicado() {
        Usuario usuario1 = Usuario.builder()
                .nomeCompleto("Usuário Email Um")
                .nomeUsuario("emailuser1")
                .email("duplicado@example.com")
                .senha("senha1")
                .nivelHierarquia(NivelHierarquia.COMUM)
                .empresa(empresaTeste)
                .build();
        entityManager.persistAndFlush(usuario1);

        Usuario usuario2 = Usuario.builder()
                .nomeCompleto("Usuário Email Dois")
                .nomeUsuario("emailuser2")
                .email("duplicado@example.com")
                .senha("senha2")
                .nivelHierarquia(NivelHierarquia.ADM)
                .empresa(empresaTeste)
                .build();

        assertThrows(DataIntegrityViolationException.class, () -> {
            usuarioRepository.saveAndFlush(usuario2);
        });
    }
}
