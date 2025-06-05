// src/main/java/br/com/kandu/service/EmpresaService.java
package br.com.kandu.service;

import br.com.kandu.dto.EmpresaDTO;
import br.com.kandu.entity.Empresa;
import br.com.kandu.repository.EmpresaRepository;
import br.com.kandu.repository.UsuarioRepository; // Importar para verificar usuários vinculados
// Importe suas exceções customizadas se preferir, ex:
// import br.com.kandu.exception.RecursoNaoEncontradoException;
// import br.com.kandu.exception.OperacaoNaoPermitidaException;
// import br.com.kandu.exception.ValidacaoException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
public class EmpresaService {

    private final EmpresaRepository empresaRepository;
    private final UsuarioRepository usuarioRepository; // Para verificar usuários antes de deletar

    @Autowired
    public EmpresaService(EmpresaRepository empresaRepository, UsuarioRepository usuarioRepository) {
        this.empresaRepository = empresaRepository;
        this.usuarioRepository = usuarioRepository;
    }

    /**
     * Cria uma nova empresa.
     * Verifica se já existe uma empresa com o mesmo código de inscrição.
     * @param empresaDTO DTO com os dados da nova empresa.
     * @return A entidade Empresa criada e persistida.
     * @throws IllegalArgumentException se o código de inscrição já estiver em uso ou dados forem inválidos.
     */
    @Transactional
    public Empresa criarEmpresa(EmpresaDTO empresaDTO) {
        if (empresaDTO.getNome() == null || empresaDTO.getNome().trim().isEmpty() ||
                empresaDTO.getCodigoInscricao() == null || empresaDTO.getCodigoInscricao().trim().isEmpty()) {
            throw new IllegalArgumentException("Nome e Código de Inscrição são obrigatórios para criar uma empresa.");
            // throw new ValidacaoException("Nome e Código de Inscrição são obrigatórios...");
        }

        if (empresaRepository.existsByCodigoInscricao(empresaDTO.getCodigoInscricao().trim())) {
            throw new IllegalArgumentException("Código de Inscrição '" + empresaDTO.getCodigoInscricao().trim() + "' já está em uso.");
            // throw new RecursoJaExistenteException("Código de Inscrição já cadastrado.");
        }

        Empresa novaEmpresa = new Empresa();
        novaEmpresa.setNome(empresaDTO.getNome().trim());
        novaEmpresa.setCodigoInscricao(empresaDTO.getCodigoInscricao().trim());

        return empresaRepository.save(novaEmpresa);
    }

    /**
     * Busca uma empresa pelo seu ID.
     * @param id O ID da empresa.
     * @return A entidade Empresa.
     * @throws IllegalArgumentException se a empresa não for encontrada (idealmente RecursoNaoEncontradoException).
     */
    @Transactional(readOnly = true)
    public Empresa buscarEmpresaPorId(Long id) {
        return empresaRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Empresa não encontrada com o ID: " + id));
        // .orElseThrow(() -> new RecursoNaoEncontradoException("Empresa não encontrada com o ID: " + id));
    }

    /**
     * Lista todas as empresas cadastradas.
     * @return Uma lista de entidades Empresa.
     */
    @Transactional(readOnly = true)
    public List<Empresa> listarTodasEmpresas() {
        return empresaRepository.findAll();
    }

    /**
     * Atualiza os dados de uma empresa existente.
     * Não permite alterar o código de inscrição por este método para simplicidade.
     * @param id O ID da empresa a ser atualizada.
     * @param empresaDTO DTO com os novos dados (apenas o nome será atualizado aqui).
     * @return A entidade Empresa atualizada.
     * @throws IllegalArgumentException se a empresa não for encontrada ou dados inválidos.
     */
    @Transactional
    public Empresa atualizarEmpresa(Long id, EmpresaDTO empresaDTO) {
        Empresa empresaExistente = buscarEmpresaPorId(id); // Reutiliza o método que já lança exceção se não encontrar

        if (empresaDTO.getNome() == null || empresaDTO.getNome().trim().isEmpty()) {
            throw new IllegalArgumentException("O nome da empresa não pode ser vazio para atualização.");
            // throw new ValidacaoException("Nome da empresa é obrigatório para atualização.");
        }
        // Se desejar permitir alteração de código de inscrição, adicionar lógica de unicidade aqui:
        // if (empresaDTO.getCodigoInscricao() != null && !empresaDTO.getCodigoInscricao().trim().isEmpty() &&
        //     !empresaExistente.getCodigoInscricao().equalsIgnoreCase(empresaDTO.getCodigoInscricao().trim())) {
        //     if (empresaRepository.existsByCodigoInscricaoAndIdNot(empresaDTO.getCodigoInscricao().trim(), id)) {
        //         throw new IllegalArgumentException("Novo Código de Inscrição '" + empresaDTO.getCodigoInscricao().trim() + "' já está em uso por outra empresa.");
        //     }
        //     empresaExistente.setCodigoInscricao(empresaDTO.getCodigoInscricao().trim());
        // }

        empresaExistente.setNome(empresaDTO.getNome().trim());
        // Por enquanto, não permitimos alterar o código de inscrição aqui para simplificar.

        return empresaRepository.save(empresaExistente);
    }

    /**
     * Deleta uma empresa pelo seu ID.
     * Verifica se existem usuários vinculados a esta empresa antes de deletar.
     * @param id O ID da empresa a ser deletada.
     * @throws IllegalArgumentException se a empresa não for encontrada.
     * @throws IllegalStateException se existirem usuários vinculados (idealmente OperacaoNaoPermitidaException).
     */
    @Transactional
    public void deletarEmpresa(Long id) {
        Empresa empresaParaDeletar = buscarEmpresaPorId(id);

        // Verifica se existem usuários associados a esta empresa
        // Se for usar o campo `usuarios` da entidade Empresa: if (empresaParaDeletar.getUsuarios() != null && !empresaParaDeletar.getUsuarios().isEmpty())
        // Ou, de forma mais eficiente, consultando o UsuarioRepository:
        if (usuarioRepository.existsByEmpresaId(id)) {
            throw new IllegalStateException("Não é possível deletar a empresa ID " + id + " pois existem usuários vinculados a ela.");
            // throw new OperacaoNaoPermitidaException("Empresa possui usuários vinculados e não pode ser deletada.");
        }
        // Adicionar verificações para OS, Projetos Kanban, etc., antes de deletar no futuro.

        empresaRepository.delete(empresaParaDeletar);
    }

    /**
     * Busca uma empresa pelo seu código de inscrição.
     * (Método já existente da Issue #7, mantido e agora parte do CRUD)
     */
    @Transactional(readOnly = true)
    public Empresa findByCodigoInscricao(String codigoInscricao) {
        if (codigoInscricao == null || codigoInscricao.trim().isEmpty()) {
            throw new IllegalArgumentException("Código de inscrição não pode ser nulo ou vazio.");
        }
        return empresaRepository.findByCodigoInscricao(codigoInscricao.trim())
                .orElseThrow(() -> new IllegalArgumentException("Empresa não encontrada com o código de inscrição: " + codigoInscricao));
    }
}
