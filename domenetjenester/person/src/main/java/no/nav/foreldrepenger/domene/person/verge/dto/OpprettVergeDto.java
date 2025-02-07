package no.nav.foreldrepenger.domene.person.verge.dto;

import no.nav.foreldrepenger.behandlingslager.behandling.verge.VergeType;

import java.time.LocalDate;

public record OpprettVergeDto(
        String navn,
        String fnr,
        LocalDate gyldigFom,
        LocalDate gyldigTom,
        VergeType vergeType,
        String organisasjonsnummer,
        String begrunnelse
) {
    public OpprettVergeDto {
        if ((organisasjonsnummer == null && fnr == null) || (organisasjonsnummer != null && fnr != null)) {
            throw new IllegalArgumentException("Verge må ha enten fnr eller organisasjonsnummer oppgitt.");
        }
        if (VergeType.ADVOKAT.equals(vergeType)) {
            if (organisasjonsnummer == null) {
                throw new IllegalArgumentException(String.format("Verge av type %s må ha organisasjonsnummer oppgitt.", vergeType.getKode()));
            }
        } else {
            if (fnr == null) {
                throw new IllegalArgumentException(String.format("Verge av type %s må ha fnr oppgitt.", vergeType.getKode()));
            }
        }
    }
}