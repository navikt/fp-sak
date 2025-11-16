package no.nav.foreldrepenger.web.app.tjenester.forvaltning.dto;

import java.time.LocalDate;

import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import jakarta.ws.rs.QueryParam;

import io.swagger.v3.oas.annotations.Parameter;
import no.nav.foreldrepenger.web.server.abac.AppAbacAttributtType;
import no.nav.vedtak.sikkerhet.abac.AbacDataAttributter;
import no.nav.vedtak.sikkerhet.abac.AbacDto;

public class MottaPapirsøknadDto implements AbacDto {


    @NotNull
    @Parameter(description = "Saksnummer")
    @QueryParam("saksnummer")
    @Digits(integer = 18, fraction = 0)
    private String saksnummer;

    @NotNull
    @Parameter(description = "JournalpostId")
    @QueryParam("journalpostId")
    @Digits(integer = 18, fraction = 0)
    private String journalpostId;

    @Size(max = 8)
    @Parameter(description = "DokumentTypeId for søknad")
    @QueryParam("dokumentTypeId")
    @Pattern(regexp = "^[a-zA-ZæøåÆØÅ_\\-0-9]*")
    private String dokumentTypeId;

    @NotNull
    @Parameter(description = "Mottatt dato")
    @QueryParam("forsendelseMottatt")
    private LocalDate forsendelseMottatt;

    public MottaPapirsøknadDto(String saksnummer, String journalpostId, String dokumentTypeId, LocalDate forsendelseMottatt) {
        this.saksnummer = saksnummer;
        this.journalpostId = journalpostId;
        this.dokumentTypeId = dokumentTypeId;
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

    public String getDokumentTypeId() {
        return dokumentTypeId;
    }

    public LocalDate getForsendelseMottatt() {
        return forsendelseMottatt;
    }
}
