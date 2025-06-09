// src/test/java/br/com/kandu/service/OrdemDeServicoServiceTest.java
package br.com.kandu.service;

import br.com.kandu.dto.OrdemDeServicoCriacaoDTO;
import br.com.kandu.entity.Empresa;
import br.com.kandu.entity.OrdemDeServico;
import br.com.kandu.entity.Usuario;
import br.com.kandu.enums.NivelHierarquia;
import br.com.kandu.enums.StatusOS;
import br.com.kandu.repository.OrdemDeServicoRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class OrdemDeServicoServiceTest {

    @Mock
    private OrdemDeServicoRepository osRepository;

    @Mock
    private UsuarioService usuarioService; // Mockamos o serviço de usuário

    @InjectMocks
    private OrdemDeServicoService osService; // Serviço sob teste

    private Usuario usuarioComum;
    private Usuario usuarioSupervisor;
    private Empresa empresa;
    private OrdemDeServicoCriacaoDTO criacaoDTO;

    @BeforeEach
    void setUp() {
        empresa = Empresa.builder().id(1L).nome("Empresa Teste OS").build();

        usuarioComum = Usuario.builder()
                .id(1L)
                .nomeUsuario("comum.user")
                .nivelHierarquia(NivelHierarquia.COMUM)
                .empresa(empresa)
                .build();

        usuarioSupervisor = Usuario.builder()
                .id(2L)
                .nomeUsuario("supervisor.user")
                .nivelHierarquia(NivelHierarquia.SUPERVISOR)
                .empresa(empresa)
                .build();

        criacaoDTO = new OrdemDeServicoCriacaoDTO();
        criacaoDTO.setTitulo("Teste de OS");
        criacaoDTO.setDescricao("Descrição da OS de teste");
    }

    @Test
    @DisplayName("Deve criar OS com sucesso")
    void deveCriarOsComSucesso() {
        // Cenário
        when(usuarioService.getUsuarioAutenticado()).thenReturn(usuarioComum);
        when(osRepository.save(any(OrdemDeServico.class))).thenAnswer(inv -> inv.getArgument(0));

        // Ação
        OrdemDeServico osCriada = osService.criarOS(criacaoDTO);

        // Verificação
        assertThat(osCriada).isNotNull();
        assertThat(osCriada.getTitulo()).isEqualTo(criacaoDTO.getTitulo());
        assertThat(osCriada.getCriador()).isEqualTo(usuarioComum);
        assertThat(osCriada.getEmpresa()).isEqualTo(empresa);
        assertThat(osCriada.getStatus()).isEqualTo(StatusOS.ABERTA);
    }

    @Test
    @DisplayName("[Listar OS] Utilizador COMUM deve ver apenas as suas OS criadas")
    void deveListarApenasOsDoUtilizadorComum() {
        when(usuarioService.getUsuarioAutenticado()).thenReturn(usuarioComum);

        osService.listarOS();

        verify(osRepository).findByEmpresaIdAndCriadorId(empresa.getId(), usuarioComum.getId());
        verify(osRepository, never()).findAll();
        verify(osRepository, never()).findByEmpresaId(anyLong());
    }

    @Test
    @DisplayName("[Listar OS] SUPERVISOR deve ver todas as OS da sua empresa")
    void deveListarTodasOsDaEmpresaParaSupervisor() {
        when(usuarioService.getUsuarioAutenticado()).thenReturn(usuarioSupervisor);

        osService.listarOS();

        verify(osRepository).findByEmpresaId(empresa.getId());
        verify(osRepository, never()).findAll();
        verify(osRepository, never()).findByEmpresaIdAndCriadorId(anyLong(), anyLong());
    }
}
