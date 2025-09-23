package no.nav.foreldrepenger.web.app.tjenester.tilbake;

import java.time.LocalDate;
import java.util.UUID;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

public record TilbakeFullDto(@NotNull BehandlingDto behandling,
                             @NotNull FagsakDto fagsak,
                             @NotNull FamilieHendelseDto familieHendelse,
                             FeilutbetalingDto feilutbetaling,
                             boolean sendtoppdrag,
                             VergeDto verge) {

    public enum YtelseType { FORELDREPENGER, SVANGERSKAPSPENGER, ENGANGSSTØNAD }

    public enum FamilieHendelseType { FØDSEL, ADOPSJON }

    public enum VergeType { BARN, FORELDRELØS, VOKSEN, ADVOKAT, FULLMEKTIG }

    public enum FeilutbetalingValg { OPPRETT, OPPDATER, IGNORER, INNTREKK }

    public enum Språkkode { NB, NN, EN }

    public record BehandlingDto(@NotNull UUID uuid, @NotNull HenvisningDto henvisning,
                                String behandlendeEnhetId, String behandlendeEnhetNavn,
                                Språkkode språkkode, LocalDate vedtaksdato) {}

    public record FamilieHendelseDto(@NotNull FamilieHendelseType familieHendelseType, @NotNull Integer antallBarn) {}

    public record FagsakDto(@NotNull String aktørId, @NotNull String saksnummer, @NotNull YtelseType fagsakYtelseType) { }

    public record FeilutbetalingDto(@NotNull FeilutbetalingValg feilutbetalingValg, String varseltekst) { }

    public record HenvisningDto(@NotNull @Min(0) @Max(Long.MAX_VALUE) Long henvisning) { }

    public record VergeDto(@NotNull VergeType vergeType, String aktørId, String navn, String organisasjonsnummer,
                           @NotNull LocalDate gyldigFom, @NotNull LocalDate gyldigTom) { }

}
