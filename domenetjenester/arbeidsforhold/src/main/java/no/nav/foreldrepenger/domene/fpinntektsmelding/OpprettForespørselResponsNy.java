package no.nav.foreldrepenger.domene.fpinntektsmelding;

import java.util.List;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;

public record OpprettForespørselResponsNy(@NotNull @Valid List<OrganisasjonsnummerMedStatus> organisasjonsnumreMedStatus) {
    public record OrganisasjonsnummerMedStatus(@NotNull @Valid OrganisasjonsnummerDto organisasjonsnummerDto, ForespørselResultat status) {}
    protected enum ForespørselResultat {
        FORESPØRSEL_OPPRETTET,
        IKKE_OPPRETTET_FINNES_ALLEREDE,
    }
}
