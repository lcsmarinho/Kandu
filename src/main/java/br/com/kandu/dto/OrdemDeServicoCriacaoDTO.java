// src/main/java/br/com/kandu/dto/OrdemDeServicoCriacaoDTO.java
package br.com.kandu.dto;

import lombok.Getter;
import lombok.Setter;
import java.time.LocalDate;
// Pode adicionar anotações de validação do Jakarta Bean Validation
// import jakarta.validation.constraints.NotBlank;
// import jakarta.validation.constraints.FutureOrPresent;
// import jakarta.validation.constraints.Size;

@Getter
@Setter
public class OrdemDeServicoCriacaoDTO {

    // @NotBlank(message = "O título é obrigatório.")
    // @Size(max = 200)
    private String titulo;

    // @NotBlank(message = "A descrição é obrigatória.")
    private String descricao;

    // @Size(max = 255)
    private String local;

    // @FutureOrPresent(message = "O prazo não pode ser uma data passada.")
    private LocalDate prazo;

    private String requisitos;

    private boolean projetoPrivado = false;

    // O responsável (responsavelId) será atribuído por um supervisor numa outra operação.
    // O criador e a empresa são obtidos automaticamente do utilizador autenticado.
}
