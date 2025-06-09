// src/main/java/br/com/kandu/enums/StatusOS.java
package br.com.kandu.enums;

/**
 * Define os possíveis status de uma Ordem de Serviço (OS) no sistema.
 * Por que foi implementado: Para padronizar o ciclo de vida de uma OS,
 * garantindo que ela sempre tenha um estado válido e bem definido,
 * o que facilita a lógica de negócio, filtros e relatórios.
 */
public enum StatusOS {
    /**
     * A OS foi criada e aguarda designação ou início dos trabalhos.
     */
    ABERTA,

    /**
     * A OS está sendo executada por um ou mais responsáveis.
     */
    EM_ANDAMENTO,

    /**
     * A OS foi concluída pelo executor e aguarda a aprovação de um superior (ex: Gestor).
     */
    PENDENTE_APROVACAO,

    /**
     * A OS foi finalizada e aprovada.
     */
    CONCLUIDA,

    /**
     * A OS foi concluída (ou cancelada) e movida para o arquivo histórico, não aparecendo mais nas listas ativas.
     */
    ARQUIVADA,

    /**
     * A OS foi cancelada antes da sua conclusão.
     */
    CANCELADA
}
