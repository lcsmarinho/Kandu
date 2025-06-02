// src/test/java/br/com/kandu/controller/SecurityIntegrationTest.java
package br.com.kandu.controller;

import br.com.kandu.dto.LoginDTO;
import br.com.kandu.dto.TokenDTO;
import br.com.kandu.dto.UsuarioCadastroDTO;
import br.com.kandu.repository.UsuarioRepository; // Importar o repositório
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
import org.springframework.transaction.annotation.Transactional; // Pode ser útil para garantir a limpeza

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;

@SpringBootTest
@AutoConfigureMockMvc
// O @DirtiesContext ainda é bom para garantir que o contexto geral seja resetado,
// mas a limpeza explícita do repositório dá controle mais fino sobre os dados.
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_EACH_TEST_METHOD)
public class SecurityIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired // Injete o UsuarioRepository
    private UsuarioRepository usuarioRepository;

    private String jwtToken;

    @BeforeEach
        // @Transactional // Adicionar @Transactional pode ajudar a garantir que o deleteAll() seja efetivo
        // antes das operações seguintes dentro da mesma transação de teste,
        // especialmente se houver lazy loading ou outras complexidades.
        // Para operações simples de deleteAll + save, pode não ser estritamente necessário
        // com H2 e ddl-auto=create-drop (implícito pelo @SpringBootTest com H2).
    void setUp() throws Exception {
        // Limpar a tabela de usuários ANTES de cada execução do setUp.
        // Isso garante que o cadastro do 'integtestuser' não falhe por duplicidade.
        usuarioRepository.deleteAll();
        // Se você tiver outras entidades relacionadas que precisam ser limpas, faça aqui também.

        // Cadastrar um usuário para os testes
        UsuarioCadastroDTO cadastroDTO = new UsuarioCadastroDTO();
        cadastroDTO.setNomeCompleto("Usuário de Integração");
        cadastroDTO.setNomeUsuario("integtestuser");
        cadastroDTO.setEmail("integ@example.com");
        cadastroDTO.setSenha("senha123");

        mockMvc.perform(post("/auth/cadastrar")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(cadastroDTO)))
                .andDo(print()) // Para debug, pode ser removido depois
                .andExpect(status().isCreated()); // Linha 49

        // Fazer login para obter um token
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
                .andExpect(status().isUnauthorized()); // Espera 401 porque as credenciais são inválidas
    }

    @Test
    @DisplayName("Deve acessar endpoint público de teste /api/test/public sem autenticação")
    void deveAcessarEndpointPublicoDeTeste() throws Exception {
        mockMvc.perform(get("/api/test/public"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").value("Este é um recurso público, qualquer um pode acessar."));
    }
}
