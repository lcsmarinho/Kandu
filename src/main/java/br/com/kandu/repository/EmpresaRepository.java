package br.com.kandu.repository;

import br.com.kandu.entity.Empresa;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface EmpresaRepository extends JpaRepository<Empresa, Long> {
    Optional<Empresa> findByCodigoInscricao(String codigoInscricao);
    boolean existsByCodigoInscricao(String codigoInscricao);
}
