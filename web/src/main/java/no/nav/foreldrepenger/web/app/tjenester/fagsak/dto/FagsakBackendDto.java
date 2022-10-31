package no.nav.foreldrepenger.web.app.tjenester.fagsak.dto;

import no.nav.foreldrepenger.behandlingslager.behandling.personopplysning.RelasjonsRolleType;
import no.nav.foreldrepenger.behandlingslager.fagsak.Fagsak;
import no.nav.foreldrepenger.behandlingslager.fagsak.FagsakStatus;
import no.nav.foreldrepenger.behandlingslager.fagsak.FagsakYtelseType;

/**
 * Brukes for backend oppslag av fagsak. Fødselsdato saneres herfra
 */
public record FagsakBackendDto(String saksnummer, FagsakYtelseType fagsakYtelseType, FagsakStatus status, RelasjonsRolleType relasjonsRolleType,
                               String aktørId, Integer dekningsgrad) {


    public FagsakBackendDto(Fagsak fagsak, Integer dekningsgrad) {
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
