// src/test/java/br/com/kandu/util/TestUtils.java
package br.com.kandu.util;

import br.com.kandu.dto.LoginDTO;
import br.com.kandu.dto.TokenDTO;
import br.com.kandu.entity.Empresa;
import br.com.kandu.entity.Usuario;
import br.com.kandu.enums.NivelHierarquia;
import br.com.kandu.repository.UsuarioRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Classe de utilitários para ajudar na criação de dados e obtenção de tokens
 * em testes de integração.
 * Por que foi implementada: Para centralizar e reutilizar a lógica de setup comum
 * a várias classes de teste, como a criação de usuários e a simulação de login
 * para obter tokens JWT, tornando os testes mais limpos e fáceis de manter.
 */
@Component
public class TestUtils {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private UsuarioRepository usuarioRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    /**
     * Cria e persiste um usuário no banco de dados.
     * @return A entidade Usuario criada.
     */
    public Usuario criarUsuario(String nomeUsuario, String email, String senha, NivelHierarquia nivel, Empresa empresa) {
        Usuario user = Usuario.builder()
                .nomeCompleto("Usuário " + nomeUsuario)
                .nomeUsuario(nomeUsuario)
                .email(email)
                .senha(passwordEncoder.encode(senha))
                .nivelHierarquia(nivel)
                .ativo(true)
                .empresa(empresa)
                .build();
        return usuarioRepository.saveAndFlush(user);
    }

    /**
     * Simula um login via endpoint /auth/login e retorna o token JWT.
     * @return O token JWT como String.
     */
    public String obterToken(String nomeUsuario, String senha) throws Exception {
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
}
