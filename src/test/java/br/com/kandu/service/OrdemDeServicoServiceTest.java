// src/test/java/br/com/kandu/service/OrdemDeServicoServiceTest.java
package br.com.kandu.service;

import br.com.kandu.dto.OrdemDeServicoCriacaoDTO;
import br.com.kandu.entity.Empresa;
import br.com.kandu.entity.LogHistoricoOS;
import br.com.kandu.entity.OrdemDeServico;
import br.com.kandu.entity.ParticipanteOS;
import br.com.kandu.entity.Usuario;
import br.com.kandu.enums.NivelHierarquia;
import br.com.kandu.enums.StatusOS;
import br.com.kandu.repository.LogHistoricoOSRepository;
import br.com.kandu.repository.OrdemDeServicoRepository;
import br.com.kandu.repository.ParticipanteOSRepository;
import br.com.kandu.repository.UsuarioRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class OrdemDeServicoServiceTest {

    @Mock
    private OrdemDeServicoRepository osRepository;
    @Mock
    private UsuarioService usuarioService;
    @Mock
    private LogHistoricoOSRepository logRepository;
    @Mock
    private ParticipanteOSRepository participanteRepository;
    @Mock
    private UsuarioRepository usuarioRepository;

    @InjectMocks
    private OrdemDeServicoService osService;

    private Usuario usuarioComum;
    private Usuario usuarioSupervisor;
    private Empresa empresa;
    private OrdemDeServicoCriacaoDTO criacaoDTO;

    @BeforeEach
    void setUp() {
        empresa = Empresa.builder().id(1L).nome("Empresa Teste OS").build();
        usuarioComum = Usuario.builder().id(1L).nomeUsuario("comum.user").nivelHierarquia(NivelHierarquia.COMUM).empresa(empresa).build();
        usuarioSupervisor = Usuario.builder().id(2L).nomeUsuario("supervisor.user").nivelHierarquia(NivelHierarquia.SUPERVISOR).empresa(empresa).build();
        criacaoDTO = new OrdemDeServicoCriacaoDTO();
        criacaoDTO.setTitulo("Teste de OS");
        criacaoDTO.setDescricao("Descrição da OS de teste");
    }

    @Test
    @DisplayName("Deve criar OS e registrar um log de criação")
    void deveCriarOsERegistrarLog() {
        when(usuarioService.getUsuarioAutenticado()).thenReturn(usuarioComum);
        when(osRepository.save(any(OrdemDeServico.class))).thenAnswer(inv -> {
            OrdemDeServico os = inv.getArgument(0);
            os.setId(10L);
            return os;
        });
        ArgumentCaptor<LogHistoricoOS> logCaptor = ArgumentCaptor.forClass(LogHistoricoOS.class);
        osService.criarOS(criacaoDTO);
        verify(logRepository).save(logCaptor.capture());
        LogHistoricoOS logSalvo = logCaptor.getValue();
        assertThat(logSalvo).isNotNull();
        assertThat(logSalvo.getDescricaoAcao()).isEqualTo("Ordem de Serviço criada.");
        assertThat(logSalvo.getOrdemDeServico().getId()).isEqualTo(10L);
    }

    @Test
    @DisplayName("Deve registrar log de cancelamento ao deletar uma OS aberta")
    void deveRegistrarLogDeCancelamentoAoDeletarOs() {
        OrdemDeServico osExistente = OrdemDeServico.builder().id(20L).status(StatusOS.ABERTA).empresa(empresa).criador(usuarioComum).build();
        when(usuarioService.getUsuarioAutenticado()).thenReturn(usuarioSupervisor);
        when(osRepository.findById(20L)).thenReturn(Optional.of(osExistente));
        ArgumentCaptor<LogHistoricoOS> logCaptor = ArgumentCaptor.forClass(LogHistoricoOS.class);
        osService.deletarOS(20L);
        verify(osRepository).save(any(OrdemDeServico.class));
        verify(logRepository).save(logCaptor.capture());
        LogHistoricoOS logSalvo = logCaptor.getValue();
        assertThat(logSalvo.getDescricaoAcao()).isEqualTo("OS cancelada.");
        assertThat(logSalvo.getDadosAntigos()).isEqualTo("status: ABERTA");
        assertThat(logSalvo.getDadosNovos()).isEqualTo("status: CANCELADA");
    }

    // --- Testes para Participantes ---

    @Test
    @DisplayName("[Participantes] Supervisor deve adicionar participante com sucesso")
    void supervisorDeveAdicionarParticipanteComSucesso() {
        Usuario participanteCandidato = Usuario.builder().id(3L).nomeCompleto("Candidato a Participante").empresa(empresa).build();
        OrdemDeServico os = OrdemDeServico.builder().id(1L).empresa(empresa).build();

        when(usuarioService.getUsuarioAutenticado()).thenReturn(usuarioSupervisor);
        when(osRepository.findById(1L)).thenReturn(Optional.of(os));
        when(usuarioRepository.findById(3L)).thenReturn(Optional.of(participanteCandidato));
        when(participanteRepository.findByOrdemDeServicoIdAndUsuarioId(1L, 3L)).thenReturn(Optional.empty());

        osService.adicionarParticipante(1L, 3L);

        verify(participanteRepository).save(any(ParticipanteOS.class));
        verify(logRepository).save(argThat(log ->
                log.getDescricaoAcao().contains("Participante 'Candidato a Participante' adicionado")
        ));
    }

    @Test
    @DisplayName("[Participantes] Deve lançar exceção ao tentar adicionar um participante que já existe")
    void deveLancarExcecaoAoAdicionarParticipanteExistente() {
        Usuario participanteExistente = Usuario.builder().id(3L).empresa(empresa).build();
        OrdemDeServico os = OrdemDeServico.builder().id(1L).empresa(empresa).build();

        when(usuarioService.getUsuarioAutenticado()).thenReturn(usuarioSupervisor);
        when(osRepository.findById(1L)).thenReturn(Optional.of(os));
        when(usuarioRepository.findById(3L)).thenReturn(Optional.of(participanteExistente));
        when(participanteRepository.findByOrdemDeServicoIdAndUsuarioId(1L, 3L)).thenReturn(Optional.of(new ParticipanteOS()));

        assertThrows(IllegalStateException.class, () -> osService.adicionarParticipante(1L, 3L));
    }

    @Test
    @DisplayName("[Participantes] Supervisor deve remover participante com sucesso")
    void supervisorDeveRemoverParticipanteComSucesso() {
        Usuario participanteParaRemover = Usuario.builder().id(3L).nomeCompleto("Participante a Sair").empresa(empresa).build();
        OrdemDeServico os = OrdemDeServico.builder().id(1L).empresa(empresa).participantes(new ArrayList<>()).build();
        ParticipanteOS participacao = ParticipanteOS.builder().id(5L).ordemDeServico(os).usuario(participanteParaRemover).build();
        os.getParticipantes().add(participacao);

        when(usuarioService.getUsuarioAutenticado()).thenReturn(usuarioSupervisor);
        when(osRepository.findById(1L)).thenReturn(Optional.of(os));
        when(participanteRepository.findById(5L)).thenReturn(Optional.of(participacao));

        osService.removerParticipante(1L, 5L);

        verify(osRepository).save(os);
        verify(logRepository).save(argThat(log ->
                log.getDescricaoAcao().contains("Participante 'Participante a Sair' removido")
        ));
        assertThat(os.getParticipantes()).isEmpty(); // Verifica se a lista foi esvaziada
    }
}
