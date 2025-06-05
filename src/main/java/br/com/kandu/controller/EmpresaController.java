// src/main/java/br/com/kandu/controller/EmpresaController.java
package br.com.kandu.controller;

import br.com.kandu.dto.EmpresaDTO;
import br.com.kandu.entity.Empresa;
import br.com.kandu.service.EmpresaService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize; // Para segurança baseada em anotações
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/empresas")
// @PreAuthorize("hasAuthority('ADM')") // Pode ser aplicado a nível de classe se todos os métodos são ADM only
public class EmpresaController {

    private final EmpresaService empresaService;

    @Autowired
    public EmpresaController(EmpresaService empresaService) {
        this.empresaService = empresaService;
    }

    /**
     * Endpoint para criar uma nova empresa.
     * Apenas usuários com NivelHierarquia ADM podem acessar.
     * @param empresaDTO DTO com os dados da empresa.
     * @return ResponseEntity com a EmpresaDTO criada e status CREATED, ou erro.
     */
    @PostMapping
    @PreAuthorize("hasAuthority('ADM')") // Protege este endpoint específico
    public ResponseEntity<?> criarEmpresa(@RequestBody EmpresaDTO empresaDTO) {
        try {
            Empresa novaEmpresa = empresaService.criarEmpresa(empresaDTO);
            EmpresaDTO responseDTO = new EmpresaDTO(novaEmpresa.getId(), novaEmpresa.getNome(), novaEmpresa.getCodigoInscricao());
            return ResponseEntity.status(HttpStatus.CREATED).body(responseDTO);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        } catch (Exception e) {
            // Logar e.printStackTrace() ou usar logger
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Erro ao criar empresa: " + e.getMessage());
        }
    }

    /**
     * Endpoint para buscar uma empresa pelo ID.
     * Apenas usuários com NivelHierarquia ADM podem acessar.
     * @param id O ID da empresa.
     * @return ResponseEntity com a EmpresaDTO ou NOT_FOUND.
     */
    @GetMapping("/{id}")
    @PreAuthorize("hasAuthority('ADM')")
    public ResponseEntity<?> buscarEmpresaPorId(@PathVariable Long id) {
        try {
            Empresa empresa = empresaService.buscarEmpresaPorId(id);
            EmpresaDTO responseDTO = new EmpresaDTO(empresa.getId(), empresa.getNome(), empresa.getCodigoInscricao());
            return ResponseEntity.ok(responseDTO);
        } catch (IllegalArgumentException e) { // Ou RecursoNaoEncontradoException
            return ResponseEntity.notFound().build();
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Erro ao buscar empresa: " + e.getMessage());
        }
    }

    /**
     * Endpoint para listar todas as empresas.
     * Apenas usuários com NivelHierarquia ADM podem acessar.
     * @return Lista de EmpresaDTO.
     */
    @GetMapping
    @PreAuthorize("hasAuthority('ADM')")
    public ResponseEntity<List<EmpresaDTO>> listarTodasEmpresas() {
        List<Empresa> empresas = empresaService.listarTodasEmpresas();
        List<EmpresaDTO> dtos = empresas.stream()
                .map(empresa -> new EmpresaDTO(empresa.getId(), empresa.getNome(), empresa.getCodigoInscricao()))
                .collect(Collectors.toList());
        return ResponseEntity.ok(dtos);
    }

    /**
     * Endpoint para atualizar uma empresa existente.
     * Apenas usuários com NivelHierarquia ADM podem acessar.
     * @param id O ID da empresa a ser atualizada.
     * @param empresaDTO DTO com os novos dados.
     * @return ResponseEntity com a EmpresaDTO atualizada ou erro.
     */
    @PutMapping("/{id}")
    @PreAuthorize("hasAuthority('ADM')")
    public ResponseEntity<?> atualizarEmpresa(@PathVariable Long id, @RequestBody EmpresaDTO empresaDTO) {
        try {
            Empresa empresaAtualizada = empresaService.atualizarEmpresa(id, empresaDTO);
            EmpresaDTO responseDTO = new EmpresaDTO(empresaAtualizada.getId(), empresaAtualizada.getNome(), empresaAtualizada.getCodigoInscricao());
            return ResponseEntity.ok(responseDTO);
        } catch (IllegalArgumentException e) { // Ou RecursoNaoEncontradoException/ValidacaoException
            return ResponseEntity.badRequest().body(e.getMessage());
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Erro ao atualizar empresa: " + e.getMessage());
        }
    }

    /**
     * Endpoint para deletar uma empresa.
     * Apenas usuários com NivelHierarquia ADM podem acessar.
     * @param id O ID da empresa a ser deletada.
     * @return ResponseEntity com status NO_CONTENT ou erro.
     */
    @DeleteMapping("/{id}")
    @PreAuthorize("hasAuthority('ADM')")
    public ResponseEntity<?> deletarEmpresa(@PathVariable Long id) {
        try {
            empresaService.deletarEmpresa(id);
            return ResponseEntity.noContent().build();
        } catch (IllegalArgumentException e) { // Ou RecursoNaoEncontradoException
            return ResponseEntity.notFound().build();
        } catch (IllegalStateException e) { // Para o caso de usuários vinculados
            return ResponseEntity.status(HttpStatus.CONFLICT).body(e.getMessage());
        }
        catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Erro ao deletar empresa: " + e.getMessage());
        }
    }
}
