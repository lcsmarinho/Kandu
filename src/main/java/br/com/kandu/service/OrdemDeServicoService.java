// src/main/java/br/com/kandu/service/OrdemDeServicoService.java
package br.com.kandu.service;

import br.com.kandu.dto.OrdemDeServicoCriacaoDTO;
import br.com.kandu.entity.Empresa;
import br.com.kandu.entity.OrdemDeServico;
import br.com.kandu.entity.Usuario;
import br.com.kandu.enums.NivelHierarquia;
import br.com.kandu.enums.StatusOS;
import br.com.kandu.repository.OrdemDeServicoRepository;
import br.com.kandu.repository.UsuarioRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class OrdemDeServicoService {

    private final OrdemDeServicoRepository osRepository;
    private final UsuarioService usuarioService; // Usaremos para obter o utilizador logado

    @Autowired
    public OrdemDeServicoService(OrdemDeServicoRepository osRepository, UsuarioService usuarioService) {
        this.osRepository = osRepository;
        this.usuarioService = usuarioService;
    }

    /**
     * Cria uma nova Ordem de Serviço.
     * Associa automaticamente o criador e a empresa com base no utilizador autenticado.
     * @param dto DTO com os dados da OS.
     * @return A entidade OrdemDeServico criada.
     */
    @Transactional
    public OrdemDeServico criarOS(OrdemDeServicoCriacaoDTO dto) {
        Usuario criador = usuarioService.getUsuarioAutenticado();
        Empresa empresa = criador.getEmpresa();

        // Validação básica
        if (dto.getTitulo() == null || dto.getTitulo().trim().isEmpty()) {
            throw new IllegalArgumentException("O título da OS é obrigatório.");
        }

        OrdemDeServico novaOS = OrdemDeServico.builder()
                .titulo(dto.getTitulo())
                .descricao(dto.getDescricao())
                .local(dto.getLocal())
                .prazo(dto.getPrazo())
                .requisitos(dto.getRequisitos())
                .status(StatusOS.ABERTA) // Status inicial
                .projetoPrivado(dto.isProjetoPrivado())
                .criador(criador)
                .empresa(empresa)
                .build();

        return osRepository.save(novaOS);
    }

    /**
     * Busca uma Ordem de Serviço por ID, garantindo o isolamento da empresa.
     * @param id O ID da OS.
     * @return A entidade OrdemDeServico.
     * @throws SecurityException se o utilizador não tiver permissão para ver a OS.
     */
    @Transactional(readOnly = true)
    public OrdemDeServico buscarOSPorId(Long id) {
        Usuario utilizadorLogado = usuarioService.getUsuarioAutenticado();
        OrdemDeServico os = osRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Ordem de Serviço não encontrada com o ID: " + id));

        // Regra de Isolamento: Só pode ver OS da própria empresa (a menos que seja ADM do sistema)
        if (utilizadorLogado.getNivelHierarquia() != NivelHierarquia.ADM &&
                !os.getEmpresa().getId().equals(utilizadorLogado.getEmpresa().getId())) {
            throw new SecurityException("Acesso negado à Ordem de Serviço de outra empresa.");
        }

        // Adicionar regras de visibilidade para 'projetoPrivado' aqui se necessário no futuro.

        return os;
    }

    /**
     * Lista Ordens de Serviço com base em filtros e nas permissões do utilizador.
     * @return Uma lista de Ordens de Serviço.
     */
    @Transactional(readOnly = true)
    public List<OrdemDeServico> listarOS(/* Adicionar parâmetros de filtro aqui no futuro, ex: StatusOS status */) {
        Usuario utilizadorLogado = usuarioService.getUsuarioAutenticado();
        NivelHierarquia nivel = utilizadorLogado.getNivelHierarquia();

        // ADM pode ver todas as OS (poderia ter um filtro por empresa opcional)
        if (nivel == NivelHierarquia.ADM) {
            return osRepository.findAll(); // Cuidado com performance em produção com muitos dados
        }

        // DIRETOR/GESTOR/SUPERVISOR veem todas as OS da sua empresa
        if (nivel == NivelHierarquia.DIRETOR || nivel == NivelHierarquia.GESTOR || nivel == NivelHierarquia.SUPERVISOR) {
            return osRepository.findByEmpresaId(utilizadorLogado.getEmpresa().getId());
        }

        // COMUM vê apenas as OS que ele criou ou das quais é responsável.
        // Vamos começar com as que ele criou. A responsabilidade virá depois.
        return osRepository.findByEmpresaIdAndCriadorId(utilizadorLogado.getEmpresa().getId(), utilizadorLogado.getId());
    }

    /**
     * Deleta (soft delete) uma Ordem de Serviço, mudando seu status para CANCELADA ou ARQUIVADA.
     * @param id O ID da OS a ser deletada.
     */
    @Transactional
    public void deletarOS(Long id) {
        // A busca já valida se o utilizador tem permissão para ver/interagir com a OS.
        OrdemDeServico osParaDeletar = buscarOSPorId(id);

        // A lógica de quem pode deletar (SUPERVISOR, GESTOR) será aplicada no Controller com @PreAuthorize.
        // Vamos implementar um soft delete.
        if (osParaDeletar.getStatus() == StatusOS.CONCLUIDA) {
            osParaDeletar.setStatus(StatusOS.ARQUIVADA);
        } else {
            osParaDeletar.setStatus(StatusOS.CANCELADA);
        }
        osRepository.save(osParaDeletar);
    }

    // O método de atualização (PUT/PATCH) será mais complexo (atualizar status, responsável, etc.)
    // e pode ser uma issue separada para maior detalhamento.
    // Por enquanto, o CRUD básico está coberto por Criar, Listar, Buscar e Deletar.
}
