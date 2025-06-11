// src/main/java/br/com/kandu/service/OrdemDeServicoService.java
package br.com.kandu.service;

import br.com.kandu.dto.OrdemDeServicoCriacaoDTO;
import br.com.kandu.entity.*;
import br.com.kandu.enums.NivelHierarquia;
import br.com.kandu.enums.StatusOS;
import br.com.kandu.repository.LogHistoricoOSRepository;
import br.com.kandu.repository.OrdemDeServicoRepository;
import br.com.kandu.repository.ParticipanteOSRepository;
import br.com.kandu.repository.UsuarioRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
public class OrdemDeServicoService {

    private final OrdemDeServicoRepository osRepository;
    private final UsuarioService usuarioService;
    private final LogHistoricoOSRepository logRepository;
    private final ParticipanteOSRepository participanteRepository;
    private final UsuarioRepository usuarioRepository;

    @Autowired
    public OrdemDeServicoService(OrdemDeServicoRepository osRepository, UsuarioService usuarioService,
                                 LogHistoricoOSRepository logRepository, ParticipanteOSRepository participanteRepository,
                                 UsuarioRepository usuarioRepository) {
        this.osRepository = osRepository;
        this.usuarioService = usuarioService;
        this.logRepository = logRepository;
        this.participanteRepository = participanteRepository;
        this.usuarioRepository = usuarioRepository;
    }

    @Transactional
    public OrdemDeServico criarOS(OrdemDeServicoCriacaoDTO dto) {
        Usuario criador = usuarioService.getUsuarioAutenticado();
        if (dto.getTitulo() == null || dto.getTitulo().trim().isEmpty()) {
            throw new IllegalArgumentException("O título da OS é obrigatório.");
        }
        OrdemDeServico novaOS = OrdemDeServico.builder()
                .titulo(dto.getTitulo()).descricao(dto.getDescricao()).local(dto.getLocal())
                .prazo(dto.getPrazo()).requisitos(dto.getRequisitos()).status(StatusOS.ABERTA)
                .projetoPrivado(dto.isProjetoPrivado()).criador(criador).empresa(criador.getEmpresa())
                .build();
        OrdemDeServico osSalva = osRepository.save(novaOS);
        registrarLog(osSalva, criador, "Ordem de Serviço criada.", null, "status: ABERTA");
        return osSalva;
    }

    @Transactional(readOnly = true)
    public OrdemDeServico buscarOSPorId(Long id) {
        Usuario utilizadorLogado = usuarioService.getUsuarioAutenticado();
        OrdemDeServico os = osRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Ordem de Serviço não encontrada com o ID: " + id));
        if (utilizadorLogado.getNivelHierarquia() != NivelHierarquia.ADM &&
                !os.getEmpresa().getId().equals(utilizadorLogado.getEmpresa().getId())) {
            throw new SecurityException("Acesso negado à Ordem de Serviço de outra empresa.");
        }
        return os;
    }

    @Transactional(readOnly = true)
    public List<OrdemDeServico> listarOS() {
        Usuario utilizadorLogado = usuarioService.getUsuarioAutenticado();
        NivelHierarquia nivel = utilizadorLogado.getNivelHierarquia();
        if (nivel == NivelHierarquia.ADM) return osRepository.findAll();
        if (nivel == NivelHierarquia.DIRETOR || nivel == NivelHierarquia.GESTOR || nivel == NivelHierarquia.SUPERVISOR) {
            return osRepository.findByEmpresaId(utilizadorLogado.getEmpresa().getId());
        }
        return osRepository.findByEmpresaIdAndCriadorId(utilizadorLogado.getEmpresa().getId(), utilizadorLogado.getId());
    }

    @Transactional
    public void deletarOS(Long id) {
        Usuario utilizadorLogado = usuarioService.getUsuarioAutenticado();
        OrdemDeServico osParaDeletar = buscarOSPorId(id);
        StatusOS statusAnterior = osParaDeletar.getStatus();
        StatusOS novoStatus = (statusAnterior == StatusOS.CONCLUIDA) ? StatusOS.ARQUIVADA : StatusOS.CANCELADA;
        String acaoLog = (novoStatus == StatusOS.ARQUIVADA) ? "OS arquivada." : "OS cancelada.";
        osParaDeletar.setStatus(novoStatus);
        osRepository.save(osParaDeletar);
        registrarLog(osParaDeletar, utilizadorLogado, acaoLog, "status: " + statusAnterior, "status: " + novoStatus);
    }

    @Transactional
    public ParticipanteOS adicionarParticipante(Long osId, Long usuarioId) {
        Usuario adminOuSupervisor = usuarioService.getUsuarioAutenticado();
        OrdemDeServico os = this.buscarOSPorId(osId);
        Usuario participanteParaAdicionar = usuarioRepository.findById(usuarioId)
                .orElseThrow(() -> new IllegalArgumentException("Utilizador a ser adicionado não encontrado com ID: " + usuarioId));

        if (!participanteParaAdicionar.getEmpresa().getId().equals(os.getEmpresa().getId())) {
            throw new IllegalArgumentException("Não é possível adicionar um participante de outra empresa.");
        }
        if (participanteRepository.findByOrdemDeServicoIdAndUsuarioId(osId, usuarioId).isPresent()) {
            throw new IllegalStateException("Este utilizador já é um participante nesta Ordem de Serviço.");
        }
        ParticipanteOS novaParticipacao = ParticipanteOS.builder().ordemDeServico(os).usuario(participanteParaAdicionar).build();
        ParticipanteOS participacaoSalva = participanteRepository.save(novaParticipacao);
        String descricaoLog = String.format("Participante '%s' adicionado à OS.", participanteParaAdicionar.getNomeCompleto());
        registrarLog(os, adminOuSupervisor, descricaoLog, null, "usuarioId: " + participanteParaAdicionar.getId());
        return participacaoSalva;
    }

    @Transactional
    public void removerParticipante(Long osId, Long participanteId) {
        Usuario adminOuSupervisor = usuarioService.getUsuarioAutenticado();
        OrdemDeServico os = this.buscarOSPorId(osId);
        ParticipanteOS participacaoParaRemover = participanteRepository.findById(participanteId)
                .orElseThrow(() -> new IllegalArgumentException("Participação não encontrada com ID: " + participanteId));

        if (!participacaoParaRemover.getOrdemDeServico().getId().equals(os.getId())) {
            throw new IllegalArgumentException("Esta participação não pertence à Ordem de Serviço especificada.");
        }

        os.getParticipantes().remove(participacaoParaRemover);
        osRepository.save(os);

        String descricaoLog = String.format("Participante '%s' removido da OS.", participacaoParaRemover.getUsuario().getNomeCompleto());
        registrarLog(os, adminOuSupervisor, descricaoLog, "usuarioId: " + participacaoParaRemover.getUsuario().getId(), null);
    }

    private void registrarLog(OrdemDeServico os, Usuario utilizador, String descricao, String dadosAntigos, String dadosNovos) {
        LogHistoricoOS log = LogHistoricoOS.builder()
                .ordemDeServico(os)
                .usuarioResponsavelAcao(utilizador)
                .descricaoAcao(descricao)
                .dadosAntigos(dadosAntigos)
                .dadosNovos(dadosNovos)
                .build();
        logRepository.save(log);
    }
}
