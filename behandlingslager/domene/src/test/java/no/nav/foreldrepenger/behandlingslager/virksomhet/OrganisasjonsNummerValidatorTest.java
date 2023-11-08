package no.nav.foreldrepenger.behandlingslager.virksomhet;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class OrganisasjonsNummerValidatorTest {

    @Test
    void erGyldig() {
        assertThat(OrganisasjonsNummerValidator.erGyldig("889640782")).isTrue(); // NAV
        assertThat(OrganisasjonsNummerValidator.erGyldig("974760673")).isTrue(); // Brreg
        assertThat(OrganisasjonsNummerValidator.erGyldig("123123341")).isFalse();

        // kunstig org for saksbehandlers endringer.
        assertThat(OrganisasjonsNummerValidator.erGyldig(OrgNummer.KUNSTIG_ORG)).isFalse();

        assertThat(OrganisasjonsNummerValidator.erGyldig("1")).isFalse();
    }
}
