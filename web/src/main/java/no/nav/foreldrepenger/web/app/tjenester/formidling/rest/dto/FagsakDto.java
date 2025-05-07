package no.nav.foreldrepenger.web.app.tjenester.formidling.rest.dto;

import no.nav.foreldrepenger.web.app.tjenester.formidling.rest.kodeverk.FagsakYtelseTypeDto;
import no.nav.foreldrepenger.web.app.tjenester.formidling.rest.kodeverk.RelasjonsRolleTypeDto;

public record FagsakDto(String saksnummer, FagsakYtelseTypeDto fagsakYtelseType, RelasjonsRolleTypeDto relasjonsRolleType, String aktørId,
                        Integer dekningsgrad) {

    @Override
    public String toString() {
        return "FagsakDto{" + "saksnummer=" + saksnummer + ", fagsakYtelseType=" + fagsakYtelseType + ", relasjonsRolleType=" + relasjonsRolleType
            + ", dekningsgrad=" + dekningsgrad + '}';
    }
}
