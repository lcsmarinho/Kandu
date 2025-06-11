// src/main/java/br/com/kandu/service/OrdemDeServicoService.java
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
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class OrdemDeServicoService {

    private final OrdemDeServicoRepository osRepository;
    private final UsuarioService usuarioService;
    private final LogHistoricoOSRepository logRepository; // Nova dependência

    @Autowired
    public OrdemDeServicoService(OrdemDeServicoRepository osRepository, UsuarioService usuarioService, LogHistoricoOSRepository logRepository) {
        this.osRepository = osRepository;
        this.usuarioService = usuarioService;
        this.logRepository = logRepository;
    }

    /**
     * Cria uma nova Ordem de Serviço e registra o evento de criação no histórico.
     */
    @Transactional
    public OrdemDeServico criarOS(OrdemDeServicoCriacaoDTO dto) {
        Usuario criador = usuarioService.getUsuarioAutenticado();
        Empresa empresa = criador.getEmpresa();

        if (dto.getTitulo() == null || dto.getTitulo().trim().isEmpty()) {
            throw new IllegalArgumentException("O título da OS é obrigatório.");
        }

        OrdemDeServico novaOS = OrdemDeServico.builder()
                .titulo(dto.getTitulo())
                .descricao(dto.getDescricao())
                .local(dto.getLocal())
                .prazo(dto.getPrazo())
                .requisitos(dto.getRequisitos())
                .status(StatusOS.ABERTA)
                .projetoPrivado(dto.isProjetoPrivado())
                .criador(criador)
                .empresa(empresa)
                .build();

        OrdemDeServico osSalva = osRepository.save(novaOS);

        // Registrar o log de criação
        registrarLog(osSalva, criador, "Ordem de Serviço criada.", null, "status: ABERTA");

        return osSalva;
    }

    /**
     * Deleta (soft delete) uma Ordem de Serviço e registra o evento no histórico.
     */
    @Transactional
    public void deletarOS(Long id) {
        Usuario utilizadorLogado = usuarioService.getUsuarioAutenticado();
        OrdemDeServico osParaDeletar = buscarOSPorId(id);

        StatusOS statusAnterior = osParaDeletar.getStatus();
        StatusOS novoStatus;

        if (statusAnterior == StatusOS.CONCLUIDA) {
            novoStatus = StatusOS.ARQUIVADA;
            osParaDeletar.setStatus(novoStatus);
            registrarLog(osParaDeletar, utilizadorLogado, "OS arquivada.", "status: " + statusAnterior, "status: " + novoStatus);
        } else {
            novoStatus = StatusOS.CANCELADA;
            osParaDeletar.setStatus(novoStatus);
            registrarLog(osParaDeletar, utilizadorLogado, "OS cancelada.", "status: " + statusAnterior, "status: " + novoStatus);
        }
        osRepository.save(osParaDeletar);
    }

    // --- Outros Métodos do Serviço (buscarOSPorId, listarOS) permanecem aqui ---
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
        if (nivel == NivelHierarquia.ADM) {
            return osRepository.findAll();
        }
        if (nivel == NivelHierarquia.DIRETOR || nivel == NivelHierarquia.GESTOR || nivel == NivelHierarquia.SUPERVISOR) {
            return osRepository.findByEmpresaId(utilizadorLogado.getEmpresa().getId());
        }
        return osRepository.findByEmpresaIdAndCriadorId(utilizadorLogado.getEmpresa().getId(), utilizadorLogado.getId());
    }

    /**
     * Método helper privado para criar e salvar uma entrada de log.
     * @param os A Ordem de Serviço relacionada.
     * @param utilizador O utilizador que realizou a ação.
     * @param descricao A descrição da ação.
     * @param dadosAntigos String (JSON) representando o estado anterior.
     * @param dadosNovos String (JSON) representando o novo estado.
     */
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
