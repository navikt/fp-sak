package no.nav.foreldrepenger.behandlingslager.økonomioppdrag;

/**
 * Enum for mottaker status i økonomi oppdrag
 */
public enum OppdragsmottakerStatus {

    /**
     * NY: Mottaker i førstegangsoppdrag eller det finnes ingen oppdrag fra før hvis det gjelder ny førstegangsbehandling eller revurdering
     */
    NY,

    /**
     * UENDR: Mottaker finnes i original tilkjent ytelse og kun før endringsdato i ny tilkjent ytelse
     */
    UENDR,

    /**
     * ENDR: Mottaker finnes i original tilkjent ytelse og i ny tilkjent ytelse både før og etter endringsdato
     */
    ENDR,

    /**
     * OPPH: Mottaker finnes i original tilkjent ytelse men finnes ikke i ny tilkjent ytelse i det hele tatt
     */
    OPPH,

    /**
     * IKKE_MOTTAKER: Ikke mottaker
     */
    IKKE_MOTTAKER
}
