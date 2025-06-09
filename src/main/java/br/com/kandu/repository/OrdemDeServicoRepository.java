// src/main/java/br/com/kandu/repository/OrdemDeServicoRepository.java
package br.com.kandu.repository;

import br.com.kandu.entity.OrdemDeServico;
import br.com.kandu.enums.StatusOS;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor; // Para filtros dinâmicos
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
// JpaSpecificationExecutor permite criar queries dinâmicas e complexas, muito útil para filtros
public interface OrdemDeServicoRepository extends JpaRepository<OrdemDeServico, Long>, JpaSpecificationExecutor<OrdemDeServico> {

    // Exemplos de queries que o Spring Data JPA pode gerar automaticamente
    List<OrdemDeServico> findByEmpresaId(Long empresaId);
    List<OrdemDeServico> findByEmpresaIdAndStatus(Long empresaId, StatusOS status);
    List<OrdemDeServico> findByEmpresaIdAndResponsavelId(Long empresaId, Long responsavelId);
    List<OrdemDeServico> findByEmpresaIdAndCriadorId(Long empresaId, Long criadorId);
}
