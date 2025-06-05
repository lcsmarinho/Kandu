// src/main/java/br/com/kandu/dto/UsuarioCadastroDTO.java
package br.com.kandu.dto;

import lombok.Getter;
import lombok.Setter;
// Validações do Jakarta Bean Validation (ex: @NotBlank, @Email, @Size)
// podem ser adicionadas aqui futuramente para validação automática na camada de controller.
// Exemplo:
// import jakarta.validation.constraints.Email;
// import jakarta.validation.constraints.NotBlank;
// import jakarta.validation.constraints.Size;

@Getter
@Setter
public class UsuarioCadastroDTO {

    // @NotBlank(message = "Nome completo é obrigatório")
    // @Size(min = 3, max = 150, message = "Nome completo deve ter entre 3 e 150 caracteres")
    private String nomeCompleto;

    // @NotBlank(message = "Nome de usuário é obrigatório")
    // @Size(min = 3, max = 50, message = "Nome de usuário deve ter entre 3 e 50 caracteres")
    private String nomeUsuario;

    // @NotBlank(message = "E-mail é obrigatório")
    // @Email(message = "Formato de e-mail inválido")
    // @Size(max = 100, message = "E-mail não pode exceder 100 caracteres")
    private String email;

    // @NotBlank(message = "Senha é obrigatória")
    // @Size(min = 6, max = 100, message = "Senha deve ter entre 6 e 100 caracteres") // Ajustar max para acomodar hash se necessário, mas a validação é no plain text
    private String senha;

    // @NotBlank(message = "Código de inscrição da empresa é obrigatório")
    // @Size(max = 20, message = "Código de inscrição não pode exceder 20 caracteres")
    private String codigoInscricao; // Novo campo para identificar a empresa
}
