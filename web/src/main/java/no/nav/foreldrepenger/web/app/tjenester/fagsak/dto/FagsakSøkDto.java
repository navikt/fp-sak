package no.nav.foreldrepenger.web.app.tjenester.fagsak.dto;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Optional;

import no.nav.foreldrepenger.behandlingslager.behandling.personopplysning.RelasjonsRolleType;
import no.nav.foreldrepenger.behandlingslager.fagsak.Fagsak;
import no.nav.foreldrepenger.behandlingslager.fagsak.FagsakStatus;
import no.nav.foreldrepenger.behandlingslager.fagsak.FagsakYtelseType;

/**
 * Brukes for oppslag på aktørId eller søk etter saksnummer/fnr. Med fødselsdato for visnings/sorteringsformål
 */
public record FagsakSøkDto(String saksnummer,
                           FagsakYtelseType fagsakYtelseType,
                           FagsakStatus status,
                           RelasjonsRolleType relasjonsRolleType,
                           String aktørId,
                           PersonDto person,
                           LocalDate barnFødt,
                           LocalDate opprettet,
                           LocalDate endret) {

    public FagsakSøkDto(Fagsak fagsak, PersonDto person, LocalDate barnFødt) {
        this(fagsak.getSaksnummer().getVerdi(), fagsak.getYtelseType(), fagsak.getStatus(), fagsak.getRelasjonsRolleType(),
            fagsak.getAktørId().getId(), person, barnFødt,
            Optional.ofNullable(fagsak.getOpprettetTidspunkt()).map(LocalDateTime::toLocalDate).orElse(null),
            Optional.ofNullable(fagsak.getEndretTidspunkt()).map(LocalDateTime::toLocalDate).orElse(null));
    }

    @Override
    public String toString() {
        return "FagsakSøkDto{" + "saksnummer='" + saksnummer + "', fagsakYtelseType=" + fagsakYtelseType + ", status=" + status + '}';
    }
}
