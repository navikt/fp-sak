package no.nav.foreldrepenger.web.app.tjenester.fagsak.dto;

import no.nav.foreldrepenger.behandlingslager.behandling.personopplysning.RelasjonsRolleType;
import no.nav.foreldrepenger.behandlingslager.fagsak.Fagsak;
import no.nav.foreldrepenger.behandlingslager.fagsak.FagsakStatus;
import no.nav.foreldrepenger.behandlingslager.fagsak.FagsakYtelseType;
import no.nav.foreldrepenger.web.app.tjenester.behandling.dto.Redirect;

/**
 * Brukes av frontend til polling på status. Se {@link Redirect}
 */
public record FagsakDto(String saksnummer, FagsakYtelseType fagsakYtelseType, FagsakStatus status, RelasjonsRolleType relasjonsRolleType,
                        String aktørId, Integer dekningsgrad) {


    public FagsakDto(Fagsak fagsak, Integer dekningsgrad) {
        this(fagsak.getSaksnummer().getVerdi(), fagsak.getYtelseType(), fagsak.getStatus(), fagsak.getRelasjonsRolleType(),
            fagsak.getAktørId().getId(), dekningsgrad);
    }

    @Override
    public String toString() {
        return "FagsakDto{" +
            "saksnummer=" + saksnummer +
            ", fagsakYtelseType=" + fagsakYtelseType +
            ", relasjonsRolleType=" + relasjonsRolleType +
            ", status=" + status +
            ", dekningsgrad=" + dekningsgrad +
            '}';
    }
}
