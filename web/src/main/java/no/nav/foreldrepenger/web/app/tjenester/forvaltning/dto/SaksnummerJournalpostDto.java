package no.nav.foreldrepenger.web.app.tjenester.forvaltning.dto;

import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotNull;
import jakarta.ws.rs.QueryParam;

import no.nav.foreldrepenger.web.app.tjenester.tilbake.TilbakeRestTjeneste;
import no.nav.foreldrepenger.web.server.abac.AppAbacAttributtType;
import no.nav.vedtak.sikkerhet.abac.AbacDataAttributter;
import no.nav.vedtak.sikkerhet.abac.AbacDto;

public class SaksnummerJournalpostDto implements AbacDto {

    @NotNull
    @QueryParam("saksnummer")
    @Digits(integer = 18, fraction = 0)
    private String saksnummer;

    @NotNull
    @QueryParam("journalpostid")
    @Digits(integer = 18, fraction = 0)
    private String journalpostId;

    public SaksnummerJournalpostDto(@NotNull String saksnummer, @NotNull String journalpostId) {
        this.saksnummer = saksnummer;
        this.journalpostId = journalpostId;
    }

    public SaksnummerJournalpostDto() {
    }

    @Override
    public AbacDataAttributter abacAttributter() {
        return TilbakeRestTjeneste.opprett()
            .leggTil(AppAbacAttributtType.SAKSNUMMER, saksnummer)
            .leggTil(AppAbacAttributtType.JOURNALPOST_ID, journalpostId);
    }

    public String getSaksnummer() {
        return saksnummer;
    }

    public String getJournalpostId() {
        return journalpostId;
    }
}
