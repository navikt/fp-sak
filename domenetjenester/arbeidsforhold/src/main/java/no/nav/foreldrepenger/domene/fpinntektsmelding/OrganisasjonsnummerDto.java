package no.nav.foreldrepenger.domene.fpinntektsmelding;

import static no.nav.foreldrepenger.behandlingslager.virksomhet.OrgNummer.tilMaskertNummer;

import jakarta.validation.constraints.NotNull;

import com.fasterxml.jackson.annotation.JsonValue;

public record OrganisasjonsnummerDto(@NotNull @JsonValue String orgnr) {
    @Override
    public String toString() {
        return getClass().getSimpleName() + "<" + tilMaskertNummer(orgnr) + ">";
    }
}
