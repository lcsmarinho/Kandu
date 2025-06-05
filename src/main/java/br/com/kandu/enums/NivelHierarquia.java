// src/main/java/br/com/kandu/enums/NivelHierarquia.java
package br.com.kandu.enums;

/**
 * Define os níveis hierárquicos dos usuários no sistema.
 * A ordem aqui pode ser importante para comparações de hierarquia (ex: ordinal()).
 */
public enum NivelHierarquia {
    COMUM,      // Nível mais básico
    SUPERVISOR, // Supervisiona usuários COMUM
    GESTOR,     // Gerencia SUPERVISORES e COMUM
    DIRETOR,    // Gerencia GESTORES, SUPERVISORES e COMUM dentro da sua empresa
    ADM         // Administrador geral do sistema, com acesso total
}
