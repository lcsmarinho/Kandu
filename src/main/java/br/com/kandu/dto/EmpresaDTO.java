// src/main/java/br/com/kandu/dto/EmpresaDTO.java
package br.com.kandu.dto;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
// Para validações futuras (ex: usando Bean Validation)
// import jakarta.validation.constraints.NotBlank;
// import jakarta.validation.constraints.Size;

@Getter
@Setter
@NoArgsConstructor // Para facilitar a desserialização
public class EmpresaDTO {

    private Long id; // Usado para respostas e, opcionalmente, em atualizações

    // @NotBlank(message = "O nome da empresa é obrigatório.")
    // @Size(min = 2, max = 100, message = "O nome da empresa deve ter entre 2 e 100 caracteres.")
    private String nome;

    // @NotBlank(message = "O código de inscrição é obrigatório.")
    // @Size(min = 3, max = 20, message = "O código de inscrição deve ter entre 3 e 20 caracteres.")
    private String codigoInscricao;

    // Construtor para facilitar a criação do DTO a partir da entidade
    public EmpresaDTO(Long id, String nome, String codigoInscricao) {
        this.id = id;
        this.nome = nome;
        this.codigoInscricao = codigoInscricao;
    }
}
