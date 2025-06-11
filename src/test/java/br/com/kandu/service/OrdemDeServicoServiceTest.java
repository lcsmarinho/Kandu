// src/test/java/br/com/kandu/service/OrdemDeServicoServiceTest.java
package br.com.kandu.service;

import br.com.kandu.dto.OrdemDeServicoCriacaoDTO;
import br.com.kandu.entity.Empresa;
import br.com.kandu.entity.LogHistoricoOS; // Novo import
import br.com.kandu.entity.OrdemDeServico;
import br.com.kandu.entity.Usuario;
import br.com.kandu.enums.NivelHierarquia;
import br.com.kandu.enums.StatusOS;
import br.com.kandu.repository.LogHistoricoOSRepository; // Novo import
import br.com.kandu.repository.OrdemDeServicoRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor; // Novo import
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class OrdemDeServicoServiceTest {

    @Mock
    private OrdemDeServicoRepository osRepository;
    @Mock
    private UsuarioService usuarioService;
    @Mock
    private LogHistoricoOSRepository logRepository; // Novo mock

    @InjectMocks
    private OrdemDeServicoService osService;

    private Usuario usuarioComum;
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
        criacaoDTO = new OrdemDeServicoCriacaoDTO();
        criacaoDTO.setTitulo("Teste de OS");
        criacaoDTO.setDescricao("Descrição da OS de teste");
    }

    @Test
    @DisplayName("Deve criar OS e registrar um log de criação")
    void deveCriarOsERegistrarLog() {
        // Cenário
        when(usuarioService.getUsuarioAutenticado()).thenReturn(usuarioComum);
        // Simular que o save retorna o objeto com um ID
        when(osRepository.save(any(OrdemDeServico.class))).thenAnswer(invocation -> {
            OrdemDeServico os = invocation.getArgument(0);
            os.setId(10L); // Simula o ID gerado pelo banco
            return os;
        });
        // Capturador para o LogHistoricoOS
        ArgumentCaptor<LogHistoricoOS> logCaptor = ArgumentCaptor.forClass(LogHistoricoOS.class);

        // Ação
        OrdemDeServico osCriada = osService.criarOS(criacaoDTO);

        // Verificação
        assertThat(osCriada).isNotNull();

        // Verificar se o logRepository.save foi chamado uma vez
        verify(logRepository).save(logCaptor.capture());

        // Inspecionar o log capturado
        LogHistoricoOS logSalvo = logCaptor.getValue();
        assertThat(logSalvo).isNotNull();
        assertThat(logSalvo.getDescricaoAcao()).isEqualTo("Ordem de Serviço criada.");
        assertThat(logSalvo.getOrdemDeServico().getId()).isEqualTo(10L);
        assertThat(logSalvo.getUsuarioResponsavelAcao()).isEqualTo(usuarioComum);
        assertThat(logSalvo.getDadosNovos()).isEqualTo("status: ABERTA");
        assertThat(logSalvo.getDadosAntigos()).isNull();
    }

    @Test
    @DisplayName("Deve registrar log de cancelamento ao deletar uma OS aberta")
    void deveRegistrarLogDeCancelamentoAoDeletarOs() {
        // Cenário
        OrdemDeServico osExistente = OrdemDeServico.builder()
                .id(20L)
                .status(StatusOS.ABERTA)
                .empresa(empresa)
                .criador(usuarioComum)
                .build();
        when(usuarioService.getUsuarioAutenticado()).thenReturn(usuarioComum); // Utilizador que está a fazer a ação
        when(osRepository.findById(20L)).thenReturn(Optional.of(osExistente));
        ArgumentCaptor<LogHistoricoOS> logCaptor = ArgumentCaptor.forClass(LogHistoricoOS.class);

        // Ação
        osService.deletarOS(20L);

        // Verificação
        verify(osRepository).save(any(OrdemDeServico.class)); // Verifica se a OS foi salva com o novo status
        verify(logRepository).save(logCaptor.capture()); // Captura o log

        LogHistoricoOS logSalvo = logCaptor.getValue();
        assertThat(logSalvo.getDescricaoAcao()).isEqualTo("OS cancelada.");
        assertThat(logSalvo.getDadosAntigos()).isEqualTo("status: ABERTA");
        assertThat(logSalvo.getDadosNovos()).isEqualTo("status: CANCELADA");
    }
}
