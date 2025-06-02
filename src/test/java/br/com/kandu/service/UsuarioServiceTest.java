package br.com.kandu.service;

import br.com.kandu.dto.UsuarioCadastroDTO;
import br.com.kandu.entity.Usuario;
import br.com.kandu.enums.NivelHierarquia;
import br.com.kandu.repository.UsuarioRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class) // Habilita o uso de anotações do Mockito
public class UsuarioServiceTest {

    @Mock // Cria um mock para UsuarioRepository
    private UsuarioRepository usuarioRepository;

    @Mock // Cria um mock para PasswordEncoder
    private PasswordEncoder passwordEncoder;

    @InjectMocks // Injeta os mocks criados nas dependências de UsuarioService
    private UsuarioService usuarioService;

    private UsuarioCadastroDTO usuarioCadastroDTO;

    @BeforeEach
    void setUp() {
        // Configura um DTO base para os testes
        usuarioCadastroDTO = new UsuarioCadastroDTO();
        usuarioCadastroDTO.setNomeCompleto("Usuário Teste Completo");
        usuarioCadastroDTO.setNomeUsuario("testuser");
        usuarioCadastroDTO.setEmail("test@example.com");
        usuarioCadastroDTO.setSenha("senhaSegura123");
    }

    @Test
    @DisplayName("Deve cadastrar usuário com sucesso")
    void deveCadastrarUsuarioComSucesso() {
        // Cenário
        // Mock das chamadas ao repositório e encoder
        when(usuarioRepository.existsByNomeUsuario(anyString())).thenReturn(false);
        when(usuarioRepository.existsByEmail(anyString())).thenReturn(false);
        when(passwordEncoder.encode(anyString())).thenReturn("senhaHasheada");

        // Mock da persistência do usuário
        Usuario usuarioSalvoMock = new Usuario();
        usuarioSalvoMock.setId(1L);
        usuarioSalvoMock.setNomeUsuario(usuarioCadastroDTO.getNomeUsuario());
        usuarioSalvoMock.setEmail(usuarioCadastroDTO.getEmail());
        usuarioSalvoMock.setSenha("senhaHasheada");
        usuarioSalvoMock.setNivelHierarquia(NivelHierarquia.COMUM);
        usuarioSalvoMock.setAtivo(true);
        when(usuarioRepository.save(any(Usuario.class))).thenReturn(usuarioSalvoMock);

        // Ação
        Usuario resultado = usuarioService.cadastrarUsuario(usuarioCadastroDTO);

        // Verificação
        assertThat(resultado).isNotNull();
        assertThat(resultado.getNomeUsuario()).isEqualTo(usuarioCadastroDTO.getNomeUsuario());
        assertThat(resultado.getEmail()).isEqualTo(usuarioCadastroDTO.getEmail());
        assertThat(resultado.getSenha()).isEqualTo("senhaHasheada"); // Verifica se a senha foi hasheada
        assertThat(resultado.getNivelHierarquia()).isEqualTo(NivelHierarquia.COMUM);
        assertThat(resultado.isAtivo()).isTrue();

        // Verifica se o passwordEncoder.encode foi chamado com a senha correta
        verify(passwordEncoder).encode(usuarioCadastroDTO.getSenha());
        // Verifica se o repository.save foi chamado
        verify(usuarioRepository).save(any(Usuario.class));

        // Captura o argumento passado para usuarioRepository.save para mais asserções
        ArgumentCaptor<Usuario> usuarioArgumentCaptor = ArgumentCaptor.forClass(Usuario.class);
        verify(usuarioRepository).save(usuarioArgumentCaptor.capture());
        Usuario usuarioCapturado = usuarioArgumentCaptor.getValue();

        assertThat(usuarioCapturado.getNomeCompleto()).isEqualTo(usuarioCadastroDTO.getNomeCompleto());
        assertThat(usuarioCapturado.getNomeUsuario()).isEqualTo(usuarioCadastroDTO.getNomeUsuario());
    }

    @Test
    @DisplayName("Deve lançar exceção ao tentar cadastrar com nome de usuário existente")
    void deveLancarExcecaoComNomeUsuarioExistente() {
        // Cenário
        when(usuarioRepository.existsByNomeUsuario("testuser")).thenReturn(true);

        // Ação & Verificação
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () -> {
            usuarioService.cadastrarUsuario(usuarioCadastroDTO);
        });
        assertThat(exception.getMessage()).contains("Nome de usuário 'testuser' já está em uso.");

        // Garante que o save e o encode não foram chamados
        verify(usuarioRepository, never()).save(any(Usuario.class));
        verify(passwordEncoder, never()).encode(anyString());
    }

    @Test
    @DisplayName("Deve lançar exceção ao tentar cadastrar com email existente")
    void deveLancarExcecaoComEmailExistente() {
        // Cenário
        when(usuarioRepository.existsByNomeUsuario(anyString())).thenReturn(false); // Nome de usuário não existe
        when(usuarioRepository.existsByEmail("test@example.com")).thenReturn(true); // Email existe

        // Ação & Verificação
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () -> {
            usuarioService.cadastrarUsuario(usuarioCadastroDTO);
        });
        assertThat(exception.getMessage()).contains("E-mail 'test@example.com' já está em uso.");

        verify(usuarioRepository, never()).save(any(Usuario.class));
        verify(passwordEncoder, never()).encode(anyString());
    }

    @Test
    @DisplayName("Deve lançar exceção para dados de cadastro inválidos (ex: nomeUsuario nulo)")
    void deveLancarExcecaoParaDadosInvalidos() {
        // Cenário
        usuarioCadastroDTO.setNomeUsuario(null); // Tornando o DTO inválido

        // Ação & Verificação
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () -> {
            usuarioService.cadastrarUsuario(usuarioCadastroDTO);
        });
        assertThat(exception.getMessage()).isEqualTo("Dados de cadastro inválidos. Todos os campos são obrigatórios.");

        // Nenhuma interação com o repositório ou encoder deve ocorrer
        verifyNoInteractions(usuarioRepository);
        verifyNoInteractions(passwordEncoder);
    }
}