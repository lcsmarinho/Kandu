package br.com.kandu.dto;

import lombok.Getter;
import lombok.Setter;
// Adicionaremos validações do Jakarta Bean Validation (ex: @NotBlank, @Email) aqui posteriormente,
// se adicionarmos a dependência 'spring-boot-starter-validation'.
// Por enquanto, as validações serão feitas no serviço.

@Getter
@Setter
public class UsuarioCadastroDTO {
    private String nomeCompleto;
    private String nomeUsuario;
    private String email;
    private String senha;
    // NivelHierarquia e ativo serão definidos no backend por padrão no cadastro inicial.
    // O código de inscrição da empresa será tratado na Issue #7.
}