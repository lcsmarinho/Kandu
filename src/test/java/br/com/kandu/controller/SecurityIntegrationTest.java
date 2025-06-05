// src/test/java/br/com/kandu/controller/SecurityIntegrationTest.java
package br.com.kandu.controller;

import br.com.kandu.dto.LoginDTO;
import br.com.kandu.dto.TokenDTO;
import br.com.kandu.dto.UsuarioCadastroDTO;
import br.com.kandu.entity.Empresa; // Novo import
import br.com.kandu.repository.EmpresaRepository; // Novo import
import br.com.kandu.repository.UsuarioRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
// import org.springframework.transaction.annotation.Transactional; // Se precisar

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;

@SpringBootTest
@AutoConfigureMockMvc
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_EACH_TEST_METHOD)
public class SecurityIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private UsuarioRepository usuarioRepository;

    @Autowired // Injete o EmpresaRepository
    private EmpresaRepository empresaRepository;

    private String jwtToken;
    private final String codigoInscricaoTesteGlobal = "INTEG_EMP001"; // Código global para o teste

    @BeforeEach
        // @Transactional // Pode ser útil se houver problemas de transação com deleteAll
    void setUp() throws Exception {
        // Limpar dados de testes anteriores para garantir isolamento
        usuarioRepository.deleteAll();
        empresaRepository.deleteAll();

        // 1. Criar uma empresa para o teste
        Empresa empresaParaTeste = Empresa.builder()
                .nome("Empresa de Teste de Integração Setup")
                .codigoInscricao(codigoInscricaoTesteGlobal)
                .build();
        // Como não temos endpoint de criar empresa ainda (ou não queremos depender dele aqui),
        // podemos salvar diretamente via repositório para o setup do teste.
        // Em um teste de integração mais puro, você chamaria o endpoint de criação de empresa.
        empresaRepository.save(empresaParaTeste);


        // 2. Cadastrar um usuário associado a essa empresa
        UsuarioCadastroDTO cadastroDTO = new UsuarioCadastroDTO();
        cadastroDTO.setNomeCompleto("Usuário de Integração");
        cadastroDTO.setNomeUsuario("integtestuser");
        cadastroDTO.setEmail("integ@example.com");
        cadastroDTO.setSenha("senha123");
        cadastroDTO.setCodigoInscricao(codigoInscricaoTesteGlobal); // Usar o código da empresa criada

        // Realiza a chamada ao endpoint de cadastro
        mockMvc.perform(post("/auth/cadastrar")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(cadastroDTO)))
                .andDo(print()) // Para debug
                .andExpect(status().isCreated());

        // 3. Fazer login para obter um token para os testes protegidos
        LoginDTO loginDTO = new LoginDTO();
        loginDTO.setNomeUsuario("integtestuser");
        loginDTO.setSenha("senha123");

        MvcResult loginResult = mockMvc.perform(post("/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(loginDTO)))
                .andExpect(status().isOk())
                .andReturn();

        String responseString = loginResult.getResponse().getContentAsString();
        TokenDTO tokenDTO = objectMapper.readValue(responseString, TokenDTO.class);
        this.jwtToken = tokenDTO.getToken();
    }

    // ... (seus métodos de teste @Test continuam aqui) ...
    // Exemplo:
    @Test
    @DisplayName("Deve acessar endpoint protegido com token JWT válido")
    void deveAcessarEndpointProtegidoComTokenValido() throws Exception {
        mockMvc.perform(get("/api/test/protected")
                        .header("Authorization", "Bearer " + this.jwtToken))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").value("Olá, integtestuser! Você acessou um recurso protegido. Seu nível é: [COMUM]"));
    }

    @Test
    @DisplayName("Deve retornar 401 (Unauthorized) ao acessar endpoint protegido sem token")
    void deveRetornarUnauthorizedSemToken() throws Exception {
        mockMvc.perform(get("/api/test/protected"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("Deve retornar 401 (Unauthorized) ao acessar endpoint protegido com token inválido/expirado")
    void deveRetornarUnauthorizedComTokenInvalido() throws Exception {
        mockMvc.perform(get("/api/test/protected")
                        .header("Authorization", "Bearer token.invalido.jwt"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("Deve acessar endpoint público /auth/login sem autenticação (mas falhar com credenciais erradas)")
    void deveAcessarLoginEndpointPublico() throws Exception {
        LoginDTO loginDTORuim = new LoginDTO();
        loginDTORuim.setNomeUsuario("usuario_nao_existe_no_setup");
        loginDTORuim.setSenha("senha_errada");

        mockMvc.perform(post("/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(loginDTORuim)))
                .andExpect(status().isUnauthorized());
    }
    @Test
    @DisplayName("Deve acessar endpoint público de teste /api/test/public sem autenticação")
    void deveAcessarEndpointPublicoDeTeste() throws Exception {
        mockMvc.perform(get("/api/test/public"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").value("Este é um recurso público, qualquer um pode acessar."));
    }
}
