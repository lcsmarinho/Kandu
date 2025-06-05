// src/test/java/br/com/kandu/controller/UsuarioControllerIntegrationTest.java
package br.com.kandu.controller;

import br.com.kandu.dto.LoginDTO;
import br.com.kandu.dto.TokenDTO;
import br.com.kandu.dto.UsuarioAdminCriacaoDTO;
import br.com.kandu.dto.UsuarioAdminAtualizacaoDTO;
import br.com.kandu.entity.Empresa;
import br.com.kandu.entity.Usuario;
import br.com.kandu.enums.NivelHierarquia;
import br.com.kandu.repository.EmpresaRepository;
import br.com.kandu.repository.UsuarioRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.transaction.annotation.Transactional;


import static org.hamcrest.Matchers.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
public class UsuarioControllerIntegrationTest {

    @Autowired
    private MockMvc mockMvc;
    @Autowired
    private ObjectMapper objectMapper;
    @Autowired
    private UsuarioRepository usuarioRepository;
    @Autowired
    private EmpresaRepository empresaRepository;
    @Autowired
    private PasswordEncoder passwordEncoder;

    private String adminSistemaToken;
    private String diretorEmpresaAToken;
    private String diretorEmpresaBToken;

    private Empresa empresaA;
    private Empresa empresaB;
    private Usuario usuarioComumEmpresaA;
    private Usuario usuarioComumEmpresaB;

    @BeforeEach
    @Transactional
    void setUp() throws Exception {
        usuarioRepository.deleteAllInBatch();
        empresaRepository.deleteAllInBatch();

        empresaA = empresaRepository.saveAndFlush(Empresa.builder().nome("Empresa Kandu A").codigoInscricao("EMPA001").build());
        empresaB = empresaRepository.saveAndFlush(Empresa.builder().nome("Empresa Kandu B").codigoInscricao("EMPB002").build());

        adminSistemaToken = criarUsuarioEObterToken("admsys", "admsys@kandu.com", "pass", NivelHierarquia.ADM, empresaA);
        diretorEmpresaAToken = criarUsuarioEObterToken("diretora", "diretora@empa.com", "pass", NivelHierarquia.DIRETOR, empresaA);
        usuarioComumEmpresaA = usuarioRepository.saveAndFlush(Usuario.builder()
                .nomeCompleto("Comum da Empresa A")
                .nomeUsuario("comum_empa")
                .email("comum@empa.com")
                .senha(passwordEncoder.encode("passcomum"))
                .nivelHierarquia(NivelHierarquia.COMUM)
                .ativo(true)
                .empresa(empresaA)
                .build());

        diretorEmpresaBToken = criarUsuarioEObterToken("diretorb", "diretorb@empb.com", "pass", NivelHierarquia.DIRETOR, empresaB);
        usuarioComumEmpresaB = usuarioRepository.saveAndFlush(Usuario.builder()
                .nomeCompleto("Comum da Empresa B")
                .nomeUsuario("comum_empb")
                .email("comum@empb.com")
                .senha(passwordEncoder.encode("passcomumb"))
                .nivelHierarquia(NivelHierarquia.COMUM)
                .ativo(true)
                .empresa(empresaB)
                .build());
    }

    @AfterEach
    @Transactional
    void tearDown() {
        usuarioRepository.deleteAllInBatch();
        empresaRepository.deleteAllInBatch();
    }

    private String criarUsuarioEObterToken(String nomeUsuario, String email, String senha, NivelHierarquia nivel, Empresa empresa) throws Exception {
        Usuario user = Usuario.builder()
                .nomeCompleto("Usuário " + nomeUsuario)
                .nomeUsuario(nomeUsuario)
                .email(email)
                .senha(passwordEncoder.encode(senha))
                .nivelHierarquia(nivel)
                .ativo(true)
                .empresa(empresa)
                .build();
        usuarioRepository.saveAndFlush(user);
        return obterToken(nomeUsuario, senha);
    }

    private String obterToken(String nomeUsuario, String senha) throws Exception {
        LoginDTO loginDTO = new LoginDTO();
        loginDTO.setNomeUsuario(nomeUsuario);
        loginDTO.setSenha(senha);

        MvcResult loginResult = mockMvc.perform(post("/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(loginDTO)))
                .andExpect(status().isOk())
                .andReturn();
        return objectMapper.readValue(loginResult.getResponse().getContentAsString(), TokenDTO.class).getToken();
    }

    @Test
    @DisplayName("[Isolamento] DIRETOR da Empresa A deve listar apenas usuários da Empresa A")
    void diretorAEveListarApenasUsuariosDaEmpresaA() throws Exception {
        mockMvc.perform(get("/api/usuarios")
                        .header("Authorization", "Bearer " + diretorEmpresaAToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[*].empresaId", everyItem(is(empresaA.getId().intValue()))))
                .andExpect(jsonPath("$[?(@.nomeUsuario == 'comum_empb')]", empty()));
    }

    @Test
    @DisplayName("[Isolamento] DIRETOR da Empresa A NÃO deve conseguir buscar usuário da Empresa B por ID")
    void diretorANaoDeveBuscarUsuarioDaEmpresaB() throws Exception {
        mockMvc.perform(get("/api/usuarios/" + usuarioComumEmpresaB.getId())
                        .header("Authorization", "Bearer " + diretorEmpresaAToken))
                .andExpect(status().isForbidden())
                // Ajuste aqui para verificar o corpo como string
                .andExpect(content().string(containsString("Acesso negado ao usuário de outra empresa.")));
    }

    @Test
    @DisplayName("[Isolamento] ADM do sistema DEVE conseguir buscar usuário da Empresa B por ID")
    void admSistemaDeveBuscarUsuarioDaEmpresaB() throws Exception {
        mockMvc.perform(get("/api/usuarios/" + usuarioComumEmpresaB.getId())
                        .header("Authorization", "Bearer " + adminSistemaToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id", is(usuarioComumEmpresaB.getId().intValue())));
    }

    @Test
    @DisplayName("[Isolamento] DIRETOR da Empresa A NÃO deve conseguir atualizar usuário da Empresa B")
    void diretorANaoDeveAtualizarUsuarioDaEmpresaB() throws Exception {
        UsuarioAdminAtualizacaoDTO atualizacaoDTO = new UsuarioAdminAtualizacaoDTO();
        atualizacaoDTO.setFuncao("Tentativa de Hack");
        atualizacaoDTO.setAtivo(false);
        atualizacaoDTO.setNivelHierarquia(NivelHierarquia.COMUM);

        mockMvc.perform(put("/api/usuarios/" + usuarioComumEmpresaB.getId())
                        .header("Authorization", "Bearer " + diretorEmpresaAToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(atualizacaoDTO)))
                .andExpect(status().isBadRequest()) // Ou Forbidden, dependendo da exceção exata do serviço
                .andExpect(content().string(containsString("Usuário não pertence à sua empresa")));
    }

    @Test
    @DisplayName("[Isolamento] DIRETOR da Empresa A deve criar usuário apenas na Empresa A")
    void diretorADeveCriarUsuarioApenasNaEmpresaA() throws Exception {
        UsuarioAdminCriacaoDTO dto = new UsuarioAdminCriacaoDTO();
        dto.setNomeCompleto("Novo Empregado EmpA");
        dto.setNomeUsuario("novo_empa");
        dto.setEmail("novo@empa.com");
        dto.setSenha("senhaNova");
        dto.setNivelHierarquia(NivelHierarquia.COMUM);
        dto.setFuncao("Assistente");

        mockMvc.perform(post("/api/usuarios")
                        .header("Authorization", "Bearer " + diretorEmpresaAToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(dto)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.nomeUsuario", is("novo_empa")))
                .andExpect(jsonPath("$.empresaId", is(empresaA.getId().intValue())))
                .andExpect(jsonPath("$.empresaNome", is(empresaA.getNome())));
    }

    @Test
    @DisplayName("[Permissão] DIRETOR da Empresa A deve criar SUPERVISOR na Empresa A")
    void diretorADeveCriarSupervisorNaSuaEmpresa() throws Exception {
        UsuarioAdminCriacaoDTO dto = new UsuarioAdminCriacaoDTO();
        dto.setNomeCompleto("Novo Supervisor EmpA");
        dto.setNomeUsuario("novosuper_empa");
        dto.setEmail("novosuper@empa.com");
        dto.setSenha("superpass");
        dto.setNivelHierarquia(NivelHierarquia.SUPERVISOR);
        dto.setFuncao("Supervisor de Equipe EmpA");

        mockMvc.perform(post("/api/usuarios")
                        .header("Authorization", "Bearer " + diretorEmpresaAToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(dto)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.nomeUsuario", is("novosuper_empa")))
                .andExpect(jsonPath("$.nivelHierarquia", is("SUPERVISOR")))
                .andExpect(jsonPath("$.empresaId", is(empresaA.getId().intValue())));
    }
}
