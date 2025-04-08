package no.nav.foreldrepenger.web.app.tjenester.formidling.rest.dto;

import no.nav.foreldrepenger.behandlingslager.behandling.personopplysning.RelasjonsRolleType;
import no.nav.foreldrepenger.behandlingslager.fagsak.FagsakYtelseType;

public record FagsakDto(String saksnummer, FagsakYtelseType fagsakYtelseType, RelasjonsRolleType relasjonsRolleType, String aktørId,
                        Integer dekningsgrad) {

    @Override
    public String toString() {
        return "FagsakDto{" + "saksnummer=" + saksnummer + ", fagsakYtelseType=" + fagsakYtelseType + ", relasjonsRolleType=" + relasjonsRolleType
            + ", dekningsgrad=" + dekningsgrad + '}';
    }
}
