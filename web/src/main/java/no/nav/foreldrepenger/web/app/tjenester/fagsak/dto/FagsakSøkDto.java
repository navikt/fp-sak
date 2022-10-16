package no.nav.foreldrepenger.web.app.tjenester.fagsak.dto;

import java.time.LocalDate;

import no.nav.foreldrepenger.behandlingslager.fagsak.Fagsak;
import no.nav.foreldrepenger.behandlingslager.fagsak.FagsakStatus;
import no.nav.foreldrepenger.behandlingslager.fagsak.FagsakYtelseType;

/**
 * Brukes for oppslag på aktørId eller søk etter saksnummer/fnr. Med fødselsdato for visnings/sorteringsformål
 */
public record FagsakSøkDto(String saksnummer,
                           FagsakYtelseType fagsakYtelseType,
                           FagsakStatus status,
                           @Deprecated(forRemoval = true) String aktoerId,
                           String aktørId,
                           PersonSøkDto person,
                           @Deprecated(forRemoval = true) LocalDate barnFodt,
                           LocalDate barnFødt) {

    public FagsakSøkDto(Fagsak fagsak, PersonSøkDto person, LocalDate barnFødt) {
        this(fagsak.getSaksnummer().getVerdi(), fagsak.getYtelseType(), fagsak.getStatus(),
            fagsak.getAktørId().getId(), fagsak.getAktørId().getId(), person, barnFødt, barnFødt);
    }

    public record PersonSøkDto(String navn, Integer alder, String fødselsnummer, Boolean erKvinne) {

        @Override
        public String toString() {
            return "FagsakMedPersonDto{Person}";
        }
    }


    @Override
    public String toString() {
        return "FagsakSøkDto{" + "saksnummer='" + saksnummer + "', fagsakYtelseType=" + fagsakYtelseType + ", status=" + status
            + ", barnFødt=" + barnFødt + '}';
    }
}
