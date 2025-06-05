// src/test/java/br/com/kandu/service/UsuarioServiceTest.java
package br.com.kandu.service;

import br.com.kandu.dto.LoginDTO;
import br.com.kandu.dto.UsuarioAdminCriacaoDTO;
import br.com.kandu.dto.UsuarioAdminAtualizacaoDTO;
import br.com.kandu.dto.UsuarioCadastroDTO;
import br.com.kandu.entity.Empresa;
import br.com.kandu.entity.Usuario;
import br.com.kandu.enums.NivelHierarquia;
import br.com.kandu.repository.UsuarioRepository;
import br.com.kandu.security.jwt.JwtTokenProvider;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class UsuarioServiceTest {

    @Mock
    private UsuarioRepository usuarioRepository;
    @Mock
    private PasswordEncoder passwordEncoder;
    @Mock
    private JwtTokenProvider jwtTokenProvider;
    @Mock
    private EmpresaService empresaService;

    @Mock
    private Authentication authentication;
    @Mock
    private SecurityContext securityContext;

    @InjectMocks
    private UsuarioService usuarioService;

    private UsuarioCadastroDTO usuarioCadastroPublicoDTO;
    private UsuarioAdminCriacaoDTO usuarioAdminCriacaoDTO;
    private UsuarioAdminAtualizacaoDTO usuarioAdminAtualizacaoDTO;
    private Empresa empresaMockA;
    private Empresa empresaMockB;
    private Usuario adminSistema;
    private Usuario diretorEmpresaA;
    private Usuario gestorEmpresaA;
    private Usuario comumEmpresaA;
    private LoginDTO loginDTO;
    private Usuario usuarioExistenteAuthTest;

    @BeforeEach
    void setUp() {
        SecurityContextHolder.clearContext();

        empresaMockA = Empresa.builder().id(1L).nome("Empresa A Teste").codigoInscricao("EMPA").build();
        empresaMockB = Empresa.builder().id(2L).nome("Empresa B Teste").codigoInscricao("EMPB").build();

        usuarioCadastroPublicoDTO = new UsuarioCadastroDTO();
        usuarioCadastroPublicoDTO.setNomeCompleto("Novo Usuario Publico");
        usuarioCadastroPublicoDTO.setNomeUsuario("publicuser");
        usuarioCadastroPublicoDTO.setEmail("public@example.com");
        usuarioCadastroPublicoDTO.setSenha("pubsenha123");
        usuarioCadastroPublicoDTO.setCodigoInscricao("EMPA");

        usuarioAdminCriacaoDTO = new UsuarioAdminCriacaoDTO();
        usuarioAdminCriacaoDTO.setNomeCompleto("Criado Por Admin");
        usuarioAdminCriacaoDTO.setNomeUsuario("admincreated");
        usuarioAdminCriacaoDTO.setEmail("admincreated@empa.com");
        usuarioAdminCriacaoDTO.setSenha("adminpass123");
        usuarioAdminCriacaoDTO.setNivelHierarquia(NivelHierarquia.COMUM);
        usuarioAdminCriacaoDTO.setFuncao("Função Criada");

        usuarioAdminAtualizacaoDTO = new UsuarioAdminAtualizacaoDTO();
        usuarioAdminAtualizacaoDTO.setNivelHierarquia(NivelHierarquia.SUPERVISOR);
        usuarioAdminAtualizacaoDTO.setFuncao("Função Atualizada");
        usuarioAdminAtualizacaoDTO.setAtivo(true);

        adminSistema = Usuario.builder().id(100L).nomeUsuario("admsys").email("admsys@kandu.com").nivelHierarquia(NivelHierarquia.ADM).empresa(empresaMockA).ativo(true).senha("hashedPass").build();
        diretorEmpresaA = Usuario.builder().id(101L).nomeUsuario("diretor_empa").email("diretor@empa.com").nivelHierarquia(NivelHierarquia.DIRETOR).empresa(empresaMockA).ativo(true).senha("hashedPass").build();
        gestorEmpresaA = Usuario.builder().id(102L).nomeUsuario("gestor_empa").email("gestor@empa.com").nivelHierarquia(NivelHierarquia.GESTOR).empresa(empresaMockA).ativo(true).senha("hashedPass").build();
        comumEmpresaA = Usuario.builder().id(103L).nomeUsuario("comum_empa").email("comum@empa.com").nivelHierarquia(NivelHierarquia.COMUM).empresa(empresaMockA).ativo(true).senha("hashedPass").funcao("Analista").build();

        loginDTO = new LoginDTO();
        loginDTO.setNomeUsuario("authtestuser");
        loginDTO.setSenha("authsenha123");

        Empresa empresaAuthMock = Empresa.builder().id(3L).codigoInscricao("AUTH_EMP").nome("Auth Empresa Teste").build();
        usuarioExistenteAuthTest = Usuario.builder()
                .id(200L)
                .nomeUsuario("authtestuser")
                .email("auth@example.com")
                .senha("senhaHasheadaPeloBCrypt")
                .nivelHierarquia(NivelHierarquia.COMUM)
                .ativo(true)
                .empresa(empresaAuthMock)
                .build();
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    private void simularUsuarioAutenticado(Usuario usuario) {
        reset(authentication, securityContext);
        when(securityContext.getAuthentication()).thenReturn(authentication);
        when(authentication.getName()).thenReturn(usuario.getNomeUsuario());
        SecurityContextHolder.setContext(securityContext);
        when(usuarioRepository.findByNomeUsuario(usuario.getNomeUsuario())).thenReturn(Optional.of(usuario));
    }

    @Test
    @DisplayName("[Cadastro Público] Deve cadastrar usuário COMUM com sucesso")
    void cadastroPublico_deveCadastrarUsuarioComumComSucesso() {
        when(empresaService.findByCodigoInscricao("EMPA")).thenReturn(empresaMockA); // Necessário para cadastro público
        when(usuarioRepository.existsByNomeUsuario("publicuser")).thenReturn(false);
        when(usuarioRepository.existsByEmail("public@example.com")).thenReturn(false);
        when(passwordEncoder.encode("pubsenha123")).thenReturn("hashedPubSenha");
        when(usuarioRepository.save(any(Usuario.class))).thenAnswer(invocation -> {
            Usuario u = invocation.getArgument(0);
            u.setId(1L);
            return u;
        });

        Usuario resultado = usuarioService.cadastrarUsuario(usuarioCadastroPublicoDTO);

        assertThat(resultado).isNotNull();
        assertThat(resultado.getNomeUsuario()).isEqualTo("publicuser");
        assertThat(resultado.getNivelHierarquia()).isEqualTo(NivelHierarquia.COMUM);
        assertThat(resultado.getEmpresa()).isEqualTo(empresaMockA);
        verify(empresaService).findByCodigoInscricao("EMPA"); // Verifica a chamada
        verify(passwordEncoder).encode("pubsenha123");
    }


    @Test
    @DisplayName("[Listar Usuários] DIRETOR deve listar usuários da sua empresa")
    void listarUsuarios_diretorDeveListarUsuariosSuaEmpresa() {
        simularUsuarioAutenticado(diretorEmpresaA);
        when(usuarioRepository.findByEmpresaId(empresaMockA.getId())).thenReturn(List.of(diretorEmpresaA, gestorEmpresaA, comumEmpresaA));

        List<Usuario> usuarios = usuarioService.listarUsuariosDaEmpresa();

        assertThat(usuarios).hasSize(3).contains(diretorEmpresaA, gestorEmpresaA, comumEmpresaA);
        verify(usuarioRepository).findByEmpresaId(empresaMockA.getId());
    }

    @Test
    @DisplayName("[Admin Criação] DIRETOR deve criar SUPERVISOR na sua empresa com sucesso")
    void criarUsuarioPorAdmin_diretorDeveCriarSupervisorComSucesso() {
        simularUsuarioAutenticado(diretorEmpresaA); // Empresa é obtida do admin logado
        usuarioAdminCriacaoDTO.setNivelHierarquia(NivelHierarquia.SUPERVISOR);

        when(usuarioRepository.existsByNomeUsuarioAndEmpresaId(usuarioAdminCriacaoDTO.getNomeUsuario(), empresaMockA.getId())).thenReturn(false);
        when(usuarioRepository.existsByEmailAndEmpresaId(usuarioAdminCriacaoDTO.getEmail(), empresaMockA.getId())).thenReturn(false);
        when(passwordEncoder.encode(usuarioAdminCriacaoDTO.getSenha())).thenReturn("hashedAdminPass");
        when(usuarioRepository.save(any(Usuario.class))).thenAnswer(inv -> {
            Usuario u = inv.getArgument(0);
            u.setId(10L);
            return u;
        });

        Usuario resultado = usuarioService.criarUsuarioPorAdmin(usuarioAdminCriacaoDTO);

        assertThat(resultado).isNotNull();
        assertThat(resultado.getNivelHierarquia()).isEqualTo(NivelHierarquia.SUPERVISOR);
        assertThat(resultado.getEmpresa()).isEqualTo(empresaMockA);
        // Não precisamos mais verificar empresaService.findByCodigoInscricao aqui
        verify(passwordEncoder).encode(usuarioAdminCriacaoDTO.getSenha());
    }

    @Test
    @DisplayName("[Admin Criação] DIRETOR não deve criar usuário com nível DIRETOR (mesmo nível)")
    void criarUsuarioPorAdmin_diretorNaoDeveCriarDiretor() {
        simularUsuarioAutenticado(diretorEmpresaA);
        usuarioAdminCriacaoDTO.setNivelHierarquia(NivelHierarquia.DIRETOR);
        // Nenhum mock para empresaService.findByCodigoInscricao é necessário aqui

        SecurityException exception = assertThrows(SecurityException.class, () -> {
            usuarioService.criarUsuarioPorAdmin(usuarioAdminCriacaoDTO);
        });
        assertThat(exception.getMessage()).contains("Você não pode atribuir um nível hierárquico igual ou superior ao seu");
        verifyNoMoreInteractions(empresaService); // Garante que não foi chamado
    }

    @Test
    @DisplayName("[Admin Criação] GESTOR não deve criar usuário com nível ADM")
    void criarUsuarioPorAdmin_gestorNaoDeveCriarAdm() {
        simularUsuarioAutenticado(gestorEmpresaA);
        usuarioAdminCriacaoDTO.setNivelHierarquia(NivelHierarquia.ADM);

        SecurityException exception = assertThrows(SecurityException.class, () -> {
            usuarioService.criarUsuarioPorAdmin(usuarioAdminCriacaoDTO);
        });
        assertThat(exception.getMessage()).contains("Apenas administradores do sistema podem definir o nível ADM");
        verifyNoMoreInteractions(empresaService);
    }

    @Test
    @DisplayName("[Admin Criação] Deve falhar se nome de usuário já existe na empresa")
    void criarUsuarioPorAdmin_falhaSeNomeUsuarioJaExisteNaEmpresa() {
        simularUsuarioAutenticado(diretorEmpresaA);
        // Não mockamos empresaService.findByCodigoInscricao
        when(usuarioRepository.existsByNomeUsuarioAndEmpresaId(usuarioAdminCriacaoDTO.getNomeUsuario(), empresaMockA.getId())).thenReturn(true);

        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () -> {
            usuarioService.criarUsuarioPorAdmin(usuarioAdminCriacaoDTO);
        });
        assertThat(exception.getMessage()).contains("Nome de usuário '" + usuarioAdminCriacaoDTO.getNomeUsuario() + "' já existe nesta empresa.");
        verify(usuarioRepository).existsByNomeUsuarioAndEmpresaId(usuarioAdminCriacaoDTO.getNomeUsuario(), empresaMockA.getId());
        verifyNoMoreInteractions(empresaService);
    }

    @Test
    @DisplayName("[Admin Atualização] DIRETOR deve atualizar função e ativar/desativar COMUM da sua empresa")
    void atualizarUsuarioPorAdmin_diretorAtualizaComumComSucesso() {
        simularUsuarioAutenticado(diretorEmpresaA);
        when(usuarioRepository.findById(comumEmpresaA.getId())).thenReturn(Optional.of(comumEmpresaA));
        when(usuarioRepository.save(any(Usuario.class))).thenAnswer(inv -> inv.getArgument(0));

        usuarioAdminAtualizacaoDTO.setFuncao("Analista Senior");
        usuarioAdminAtualizacaoDTO.setAtivo(false);
        usuarioAdminAtualizacaoDTO.setNivelHierarquia(NivelHierarquia.COMUM);

        Usuario resultado = usuarioService.atualizarUsuarioPorAdmin(comumEmpresaA.getId(), usuarioAdminAtualizacaoDTO);

        assertThat(resultado.getFuncao()).isEqualTo("Analista Senior");
        assertThat(resultado.isAtivo()).isFalse();
    }

    @Test
    @DisplayName("[Admin Atualização] DIRETOR não deve atualizar usuário de OUTRA empresa")
    void atualizarUsuarioPorAdmin_diretorNaoAtualizaUsuarioOutraEmpresa() {
        simularUsuarioAutenticado(diretorEmpresaA);
        Usuario comumEmpresaB = Usuario.builder().id(200L).nomeUsuario("comum_empb").empresa(empresaMockB).nivelHierarquia(NivelHierarquia.COMUM).build();
        when(usuarioRepository.findById(200L)).thenReturn(Optional.of(comumEmpresaB));

        SecurityException exception = assertThrows(SecurityException.class, () -> {
            usuarioService.atualizarUsuarioPorAdmin(200L, usuarioAdminAtualizacaoDTO);
        });
        assertThat(exception.getMessage()).contains("Usuário não pertence à sua empresa");
    }

    @Test
    @DisplayName("[Admin Atualização] GESTOR não deve promover COMUM para DIRETOR (nível acima do seu)")
    void atualizarUsuarioPorAdmin_gestorNaoPromoveParaDiretor() {
        simularUsuarioAutenticado(gestorEmpresaA);
        when(usuarioRepository.findById(comumEmpresaA.getId())).thenReturn(Optional.of(comumEmpresaA));

        usuarioAdminAtualizacaoDTO.setNivelHierarquia(NivelHierarquia.DIRETOR);

        SecurityException exception = assertThrows(SecurityException.class, () -> {
            usuarioService.atualizarUsuarioPorAdmin(comumEmpresaA.getId(), usuarioAdminAtualizacaoDTO);
        });
        assertThat(exception.getMessage()).contains("Você não pode atribuir um nível hierárquico igual ou superior ao seu");
    }

    @Test
    @DisplayName("[Admin Atualização] ADM Sistema deve poder atualizar usuário de qualquer empresa")
    void atualizarUsuarioPorAdmin_admSistemaAtualizaQualquerEmpresa() {
        simularUsuarioAutenticado(adminSistema);
        when(usuarioRepository.findById(comumEmpresaA.getId())).thenReturn(Optional.of(comumEmpresaA));
        when(usuarioRepository.save(any(Usuario.class))).thenAnswer(inv -> inv.getArgument(0));

        usuarioAdminAtualizacaoDTO.setFuncao("Promovido pelo ADM");
        usuarioAdminAtualizacaoDTO.setNivelHierarquia(NivelHierarquia.GESTOR);

        Usuario resultado = usuarioService.atualizarUsuarioPorAdmin(comumEmpresaA.getId(), usuarioAdminAtualizacaoDTO);

        assertThat(resultado.getFuncao()).isEqualTo("Promovido pelo ADM");
        assertThat(resultado.getNivelHierarquia()).isEqualTo(NivelHierarquia.GESTOR);
    }

    // --- Testes de Autenticação (Mantidos) ---
    @Test
    @DisplayName("[Autenticação] Deve autenticar usuário com sucesso e retornar token JWT")
    void deveAutenticarUsuarioComSucesso() {
        when(usuarioRepository.findByNomeUsuario(loginDTO.getNomeUsuario())).thenReturn(Optional.of(usuarioExistenteAuthTest));
        when(passwordEncoder.matches(loginDTO.getSenha(), usuarioExistenteAuthTest.getSenha())).thenReturn(true);
        when(jwtTokenProvider.generateToken(usuarioExistenteAuthTest)).thenReturn("token.jwt.valido");

        String token = usuarioService.autenticar(loginDTO);
        assertThat(token).isEqualTo("token.jwt.valido");
    }

    @Test
    @DisplayName("[Autenticação] Deve lançar BadCredentialsException para nome de usuário não encontrado")
    void deveLancarExcecaoParaUsuarioNaoEncontradoNaAutenticacao() {
        loginDTO.setNomeUsuario("usuarioInexistente");
        when(usuarioRepository.findByNomeUsuario(loginDTO.getNomeUsuario())).thenReturn(Optional.empty());

        assertThrows(BadCredentialsException.class, () -> usuarioService.autenticar(loginDTO));
        verifyNoInteractions(passwordEncoder, jwtTokenProvider);
    }

    @Test
    @DisplayName("[Autenticação] Deve lançar BadCredentialsException para senha incorreta")
    void deveLancarExcecaoParaSenhaIncorreta() {
        loginDTO.setSenha("senhaErrada");
        when(usuarioRepository.findByNomeUsuario(loginDTO.getNomeUsuario())).thenReturn(Optional.of(usuarioExistenteAuthTest));
        when(passwordEncoder.matches(loginDTO.getSenha(), usuarioExistenteAuthTest.getSenha())).thenReturn(false);

        assertThrows(BadCredentialsException.class, () -> usuarioService.autenticar(loginDTO));
        verifyNoInteractions(jwtTokenProvider);
    }

    @Test
    @DisplayName("[Autenticação] Deve lançar BadCredentialsException para usuário inativo")
    void deveLancarExcecaoParaUsuarioInativo() {
        usuarioExistenteAuthTest.setAtivo(false);
        when(usuarioRepository.findByNomeUsuario(loginDTO.getNomeUsuario())).thenReturn(Optional.of(usuarioExistenteAuthTest));

        assertThrows(BadCredentialsException.class, () -> usuarioService.autenticar(loginDTO));
        verifyNoInteractions(passwordEncoder, jwtTokenProvider);
    }

    @Test
    @DisplayName("[Autenticação] Deve lançar BadCredentialsException para DTO de login com nome de usuário nulo")
    void deveLancarExcecaoLoginComNomeUsuarioNulo() {
        loginDTO.setNomeUsuario(null);
        assertThrows(BadCredentialsException.class, () -> usuarioService.autenticar(loginDTO));
        verifyNoInteractions(usuarioRepository, passwordEncoder, jwtTokenProvider);
    }
}
