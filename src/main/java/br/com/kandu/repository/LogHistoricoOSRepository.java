// src/main/java/br/com/kandu/repository/LogHistoricoOSRepository.java
package br.com.kandu.repository;

import br.com.kandu.entity.LogHistoricoOS;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface LogHistoricoOSRepository extends JpaRepository<LogHistoricoOS, Long> {

    /**
     * Encontra todos os logs de histórico para uma Ordem de Serviço específica,
     * ordenados pelo timestamp mais recente primeiro.
     * @param ordemDeServicoId O ID da Ordem de Serviço.
     * @return Uma lista de logs de histórico.
     */
    List<LogHistoricoOS> findByOrdemDeServicoIdOrderByTimestampDesc(Long ordemDeServicoId);
}
