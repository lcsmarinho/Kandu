// src/test/java/br/com/kandu/controller/OrdemDeServicoControllerIntegrationTest.java
package br.com.kandu.controller;

import br.com.kandu.dto.OrdemDeServicoCriacaoDTO;
import br.com.kandu.entity.Empresa;
import br.com.kandu.entity.OrdemDeServico;
import br.com.kandu.entity.Usuario;
import br.com.kandu.enums.NivelHierarquia;
import br.com.kandu.enums.StatusOS;
import br.com.kandu.repository.EmpresaRepository;
import br.com.kandu.repository.OrdemDeServicoRepository;
import br.com.kandu.repository.UsuarioRepository;
import br.com.kandu.util.TestUtils; // <-- IMPORT QUE ESTAVA FALTANDO
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
public class OrdemDeServicoControllerIntegrationTest {

    @Autowired
    private MockMvc mockMvc;
    @Autowired
    private ObjectMapper objectMapper;
    @Autowired
    private UsuarioRepository usuarioRepository;
    @Autowired
    private EmpresaRepository empresaRepository;
    @Autowired
    private OrdemDeServicoRepository osRepository;
    @Autowired
    private TestUtils testUtils; // Injetando nossa classe de utilitários

    private String comumToken;
    private String supervisorToken;
    private Usuario usuarioComum;
    private Usuario usuarioSupervisor;
    private Empresa empresa;

    @BeforeEach
    @Transactional
    void setUp() throws Exception {
        osRepository.deleteAllInBatch();
        usuarioRepository.deleteAllInBatch();
        empresaRepository.deleteAllInBatch();

        empresa = empresaRepository.saveAndFlush(Empresa.builder().nome("Empresa OS Teste").codigoInscricao("OS-EMP").build());

        usuarioComum = testUtils.criarUsuario("comum_os", "comum.os@kandu.com", "pass", NivelHierarquia.COMUM, empresa);
        comumToken = testUtils.obterToken("comum_os", "pass");

        usuarioSupervisor = testUtils.criarUsuario("sup_os", "sup.os@kandu.com", "pass", NivelHierarquia.SUPERVISOR, empresa);
        supervisorToken = testUtils.obterToken("sup_os", "pass");
    }

    @Test
    @DisplayName("[Criar OS] Utilizador COMUM deve criar OS com sucesso")
    void comumDeveCriarOsComSucesso() throws Exception {
        OrdemDeServicoCriacaoDTO dto = new OrdemDeServicoCriacaoDTO();
        dto.setTitulo("OS criada pelo utilizador comum");
        dto.setDescricao("Descrição teste");

        mockMvc.perform(post("/api/os")
                        .header("Authorization", "Bearer " + comumToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(dto)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.titulo", is("OS criada pelo utilizador comum")))
                .andExpect(jsonPath("$.criadorId", is(usuarioComum.getId().intValue())))
                .andExpect(jsonPath("$.empresaId", is(empresa.getId().intValue())));
    }

    @Test
    @DisplayName("[Listar OS] Deve listar OS corretamente para diferentes perfis")
    void deveListarOsCorretamenteParaPerfis() throws Exception {
        // Criar uma OS pelo comum
        osRepository.save(OrdemDeServico.builder().titulo("OS do Comum").descricao("...").status(StatusOS.ABERTA).empresa(empresa).criador(usuarioComum).build());
        // Criar uma OS por outro utilizador (o supervisor)
        osRepository.save(OrdemDeServico.builder().titulo("OS do Supervisor").descricao("...").status(StatusOS.EM_ANDAMENTO).empresa(empresa).criador(usuarioSupervisor).build());

        // Teste para Utilizador COMUM: deve ver apenas a sua OS
        mockMvc.perform(get("/api/os").header("Authorization", "Bearer " + comumToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].titulo", is("OS do Comum")));

        // Teste para SUPERVISOR: deve ver as duas OS
        mockMvc.perform(get("/api/os").header("Authorization", "Bearer " + supervisorToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(2)));
    }

    @Test
    @DisplayName("[Deletar OS] SUPERVISOR deve deletar (soft delete) uma OS")
    void supervisorDeveDeletarOs() throws Exception {
        OrdemDeServico os = osRepository.save(OrdemDeServico.builder().titulo("OS a ser deletada").descricao("...").status(StatusOS.ABERTA).empresa(empresa).criador(usuarioComum).build());

        mockMvc.perform(delete("/api/os/" + os.getId())
                        .header("Authorization", "Bearer " + supervisorToken))
                .andExpect(status().isNoContent());

        OrdemDeServico osDeletada = osRepository.findById(os.getId()).orElseThrow();
        assertThat(osDeletada.getStatus()).isEqualTo(StatusOS.CANCELADA);
    }

    @Test
    @DisplayName("[Deletar OS] Utilizador COMUM não deve conseguir deletar OS")
    void comumNaoDeveDeletarOs() throws Exception {
        OrdemDeServico os = osRepository.save(OrdemDeServico.builder().titulo("OS intocável").descricao("...").status(StatusOS.ABERTA).empresa(empresa).criador(usuarioComum).build());

        mockMvc.perform(delete("/api/os/" + os.getId())
                        .header("Authorization", "Bearer " + comumToken))
                .andExpect(status().isForbidden());
    }
}
