package no.nav.foreldrepenger.web.app.tjenester.forvaltning.dto;

import static no.nav.foreldrepenger.web.app.tjenester.forvaltning.dto.InputValideringRegexDato.DATO_PATTERN;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.ws.rs.QueryParam;

import no.nav.foreldrepenger.web.server.abac.AppAbacAttributtType;
import no.nav.vedtak.sikkerhet.abac.AbacDataAttributter;
import no.nav.vedtak.sikkerhet.abac.AbacDto;

public class MottaPapirsøknadDto implements AbacDto {

    public enum SøknadType { FORELDREPENGER_FØDSEL, ENGANGSSTØNAD_FØDSEL, SVANGERSKAPSPENGER,
        ENDRING_FORELDREPENGER, FORELDREPENGER_ADOPSJON, ENGANGSSTØNAD_ADOPSJON }

    @NotNull
    @QueryParam("saksnummer")
    @Digits(integer = 18, fraction = 0)
    private String saksnummer;

    @NotNull
    @QueryParam("journalpostId")
    @Digits(integer = 18, fraction = 0)
    private String journalpostId;

    @NotNull
    @QueryParam("søknadType")
    @Valid
    private SøknadType søknadType;

    @NotNull
    @QueryParam("mottattDato (Y-M-D)")
    @Pattern(regexp = DATO_PATTERN)
    private String mottattDato;

    public MottaPapirsøknadDto(@NotNull String saksnummer, @NotNull String journalpostId, @NotNull SøknadType søknadType, @NotNull String forsendelseMottatt) {
        this.saksnummer = saksnummer;
        this.journalpostId = journalpostId;
        this.søknadType = søknadType;
        this.mottattDato = forsendelseMottatt;
    }

    public MottaPapirsøknadDto() {
    }

    @Override
    public AbacDataAttributter abacAttributter() {
        return AbacDataAttributter.opprett().leggTil(AppAbacAttributtType.SAKSNUMMER, saksnummer);
    }

    public String getSaksnummer() {
        return saksnummer;
    }

    public String getJournalpostId() {
        return journalpostId;
    }

    public SøknadType getSøknadType() {
        return søknadType;
    }

    public LocalDate getMottattDato() {
        return LocalDate.parse(mottattDato, DateTimeFormatter.ISO_LOCAL_DATE);
    }
}
