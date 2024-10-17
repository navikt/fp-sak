package no.nav.foreldrepenger.domene.fpinntektsmelding;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;

public record OpprettForespørselResponse(@NotNull @Valid ForespørselResultat forespørselResultat) {
    protected enum ForespørselResultat {
        FORESPØRSEL_OPPRETTET,
        IKKE_OPPRETTET_FINNES_ALLEREDE_ÅPEN
    }
}
