package no.nav.foreldrepenger.web.app.tjenester.behandling.dto.behandling.v2;

import no.nav.foreldrepenger.behandlingslager.behandling.personopplysning.RelasjonsRolleType;
import no.nav.foreldrepenger.behandlingslager.fagsak.FagsakYtelseType;

public record FagsakV2Dto(String saksnummer, FagsakYtelseType fagsakYtelseType, RelasjonsRolleType relasjonsRolleType, String aktørId,
                          Integer dekningsgrad) {

    @Override
    public String toString() {
        return "FagsakV2Dto{" + "saksnummer=" + saksnummer + ", fagsakYtelseType=" + fagsakYtelseType + ", relasjonsRolleType=" + relasjonsRolleType
            + ", dekningsgrad=" + dekningsgrad + '}';
    }
}
