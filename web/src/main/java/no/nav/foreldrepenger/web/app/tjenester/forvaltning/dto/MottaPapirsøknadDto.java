package no.nav.foreldrepenger.web.app.tjenester.forvaltning.dto;

import java.time.LocalDate;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotNull;
import jakarta.ws.rs.QueryParam;

import io.swagger.v3.oas.annotations.Parameter;
import no.nav.foreldrepenger.web.server.abac.AppAbacAttributtType;
import no.nav.vedtak.sikkerhet.abac.AbacDataAttributter;
import no.nav.vedtak.sikkerhet.abac.AbacDto;

public class MottaPapirsøknadDto implements AbacDto {

    public enum SøknadType { ENGANGSSTØNAD_ADOPSJON, ENGANGSSTØNAD_FØDSEL,
        FORELDREPENGER_ADOPSJON, FORELDREPENGER_FØDSEL, ENDRING_FORELDREPENGER, SVANGERSKAPSPENGER }

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
    @QueryParam("forsendelseMottatt")
    private LocalDate forsendelseMottatt;

    public MottaPapirsøknadDto(String saksnummer, String journalpostId, SøknadType søknadType, LocalDate forsendelseMottatt) {
        this.saksnummer = saksnummer;
        this.journalpostId = journalpostId;
        this.søknadType = søknadType;
        this.forsendelseMottatt = forsendelseMottatt;
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

    public LocalDate getForsendelseMottatt() {
        return forsendelseMottatt;
    }
}
