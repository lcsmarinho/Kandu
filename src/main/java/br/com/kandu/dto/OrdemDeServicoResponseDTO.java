// src/main/java/br/com/kandu/dto/OrdemDeServicoResponseDTO.java
package br.com.kandu.dto;

import br.com.kandu.enums.StatusOS;
import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Getter
@Setter
@Builder // Usar o padrão Builder para facilitar a construção
public class OrdemDeServicoResponseDTO {

    private Long id;
    private String titulo;
    private String descricao;
    private String local;
    private LocalDateTime dataCadastro;
    private LocalDate prazo;
    private StatusOS status;
    private String requisitos;
    private boolean projetoPrivado;
    private Long empresaId;
    private Long criadorId;
    private String criadorNome;
    private Long responsavelId;
    private String responsavelNome;
}
