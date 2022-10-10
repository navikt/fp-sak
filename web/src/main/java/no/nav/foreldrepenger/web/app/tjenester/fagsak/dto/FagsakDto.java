package no.nav.foreldrepenger.web.app.tjenester.fagsak.dto;

import java.time.LocalDate;

import no.nav.foreldrepenger.behandlingslager.behandling.personopplysning.RelasjonsRolleType;
import no.nav.foreldrepenger.behandlingslager.fagsak.Fagsak;
import no.nav.foreldrepenger.behandlingslager.fagsak.FagsakStatus;
import no.nav.foreldrepenger.behandlingslager.fagsak.FagsakYtelseType;

/**
 * Brukes for backend oppslag av fagsak. Fødselsdato saneres herfra
 */
public record FagsakDto(String saksnummer, @Deprecated(forRemoval = true) String saksnummerString,
                        FagsakYtelseType fagsakYtelseType, @Deprecated(forRemoval = true) FagsakYtelseType sakstype,
                        FagsakStatus status, RelasjonsRolleType relasjonsRolleType, Integer dekningsgrad,
                        String aktørId, @Deprecated(forRemoval = true) String aktoerId,
                        @Deprecated(forRemoval = true) LocalDate barnFødt, @Deprecated(forRemoval = true) LocalDate barnFodt) {


        public FagsakDto(Fagsak fagsak, LocalDate barnFødt, Integer dekningsgrad) {
            this(fagsak.getSaksnummer().getVerdi(), fagsak.getSaksnummer().getVerdi(), fagsak.getYtelseType(), fagsak.getYtelseType(),
                fagsak.getStatus(), fagsak.getRelasjonsRolleType(), dekningsgrad, fagsak.getAktørId().getId(), fagsak.getAktørId().getId(), barnFødt, barnFødt);
    }

    @Override
    public String toString() {
        return "FagsakDto{" +
            "saksnummer=" + saksnummer +
            ", fagsakYtelseType=" + fagsakYtelseType +
            ", relasjonsRolleType=" + relasjonsRolleType +
            ", status=" + status +
            ", barnFødt=" + barnFødt +
            ", dekningsgrad=" + dekningsgrad +
            '}';
    }
}
