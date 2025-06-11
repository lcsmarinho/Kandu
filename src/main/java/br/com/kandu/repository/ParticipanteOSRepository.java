// src/main/java/br/com/kandu/repository/ParticipanteOSRepository.java
package br.com.kandu.repository;

import br.com.kandu.entity.ParticipanteOS;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface ParticipanteOSRepository extends JpaRepository<ParticipanteOS, Long> {

    /**
     * Encontra uma entrada de participação específica com base na OS e no Usuário.
     * Útil para verificar se um utilizador já é participante.
     * @param ordemDeServicoId O ID da Ordem de Serviço.
     * @param usuarioId O ID do Usuário.
     * @return Um Optional contendo a entidade ParticipanteOS se a relação existir.
     */
    Optional<ParticipanteOS> findByOrdemDeServicoIdAndUsuarioId(Long ordemDeServicoId, Long usuarioId);
}
