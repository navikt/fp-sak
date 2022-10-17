package no.nav.foreldrepenger.web.app.tjenester.fagsak.dto;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Optional;

import no.nav.foreldrepenger.behandlingslager.aktør.NavBrukerKjønn;
import no.nav.foreldrepenger.behandlingslager.fagsak.Fagsak;
import no.nav.foreldrepenger.behandlingslager.fagsak.FagsakStatus;
import no.nav.foreldrepenger.behandlingslager.fagsak.FagsakYtelseType;
import no.nav.foreldrepenger.behandlingslager.geografisk.Språkkode;

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
                           LocalDate barnFødt,
                           LocalDate opprettet,
                           LocalDate endret) {

    public FagsakSøkDto(Fagsak fagsak, PersonSøkDto person, LocalDate barnFødt) {
        this(fagsak.getSaksnummer().getVerdi(), fagsak.getYtelseType(), fagsak.getStatus(),
            fagsak.getAktørId().getId(), fagsak.getAktørId().getId(), person, barnFødt, barnFødt,
            Optional.ofNullable(fagsak.getOpprettetTidspunkt()).map(LocalDateTime::toLocalDate).orElse(null),
            Optional.ofNullable(fagsak.getEndretTidspunkt()).map(LocalDateTime::toLocalDate).orElse(null));
    }

    // Gjør om til vanlig PersonDto når frontend/los har fjernet bruk av alder og erKvinne - evt legg til disse i PersonDto
    public record PersonSøkDto(String aktørId, String navn, LocalDate fødselsdato, @Deprecated(forRemoval = true) Integer alder,
                               LocalDate dødsdato, String fødselsnummer, String diskresjonskode, Språkkode språkkode,
                               NavBrukerKjønn kjønn, @Deprecated(forRemoval = true) Boolean erKvinne) {
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
