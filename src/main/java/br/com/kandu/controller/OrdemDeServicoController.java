// src/main/java/br/com/kandu/controller/OrdemDeServicoController.java
package br.com.kandu.controller;

import br.com.kandu.dto.OrdemDeServicoCriacaoDTO;
import br.com.kandu.dto.OrdemDeServicoResponseDTO;
import br.com.kandu.entity.OrdemDeServico;
import br.com.kandu.service.OrdemDeServicoService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/os")
public class OrdemDeServicoController {

    private final OrdemDeServicoService osService;

    @Autowired
    public OrdemDeServicoController(OrdemDeServicoService osService) {
        this.osService = osService;
    }

    @PostMapping
    @PreAuthorize("hasAuthority('COMUM') or hasAuthority('SUPERVISOR') or hasAuthority('GESTOR') or hasAuthority('DIRETOR') or hasAuthority('ADM')")
    public ResponseEntity<?> criarOS(@RequestBody OrdemDeServicoCriacaoDTO dto) {
        try {
            OrdemDeServico novaOS = osService.criarOS(dto);
            return ResponseEntity.status(HttpStatus.CREATED).body(mapToResponseDTO(novaOS));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    @GetMapping
    @PreAuthorize("isAuthenticated()") // Qualquer utilizador autenticado pode listar (a lógica de serviço faz o filtro)
    public ResponseEntity<List<OrdemDeServicoResponseDTO>> listarOS() {
        List<OrdemDeServico> listaOS = osService.listarOS();
        List<OrdemDeServicoResponseDTO> dtos = listaOS.stream()
                .map(this::mapToResponseDTO)
                .collect(Collectors.toList());
        return ResponseEntity.ok(dtos);
    }

    @GetMapping("/{id}")
    @PreAuthorize("isAuthenticated()") // A lógica de visibilidade está no serviço
    public ResponseEntity<?> buscarOSPorId(@PathVariable Long id) {
        try {
            OrdemDeServico os = osService.buscarOSPorId(id);
            return ResponseEntity.ok(mapToResponseDTO(os));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.notFound().build();
        } catch (SecurityException e) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body(e.getMessage());
        }
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAnyAuthority('SUPERVISOR', 'GESTOR', 'DIRETOR', 'ADM')") // Apenas supervisores ou superiores podem deletar/arquivar
    public ResponseEntity<Void> deletarOS(@PathVariable Long id) {
        try {
            osService.deletarOS(id);
            return ResponseEntity.noContent().build();
        } catch (Exception e) {
            // Pode ser not found ou forbidden
            return ResponseEntity.notFound().build();
        }
    }

    // Método helper para mapear Entidade para DTO de resposta
    private OrdemDeServicoResponseDTO mapToResponseDTO(OrdemDeServico os) {
        return OrdemDeServicoResponseDTO.builder()
                .id(os.getId())
                .titulo(os.getTitulo())
                .descricao(os.getDescricao())
                .local(os.getLocal())
                .dataCadastro(os.getDataCadastro())
                .prazo(os.getPrazo())
                .status(os.getStatus())
                .requisitos(os.getRequisitos())
                .projetoPrivado(os.isProjetoPrivado())
                .empresaId(os.getEmpresa().getId())
                .criadorId(os.getCriador().getId())
                .criadorNome(os.getCriador().getNomeCompleto())
                .responsavelId(os.getResponsavel() != null ? os.getResponsavel().getId() : null)
                .responsavelNome(os.getResponsavel() != null ? os.getResponsavel().getNomeCompleto() : null)
                .build();
    }
}
