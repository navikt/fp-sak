package no.nav.foreldrepenger.web.app.tjenester.forvaltning.dto;

import javax.validation.constraints.Digits;
import javax.validation.constraints.NotNull;
import javax.ws.rs.QueryParam;

import no.nav.foreldrepenger.web.server.abac.AppAbacAttributtType;
import no.nav.vedtak.sikkerhet.abac.AbacDataAttributter;

public class HoppTilbakeTil5080OgSlettInntektsmeldingDto extends ForvaltningBehandlingIdDto {

    @NotNull
    @QueryParam("journalpostid")
    @Digits(integer = 18, fraction = 0)
    private String journalpostId;

    public String getJournalpostId() {
        return journalpostId;
    }

    @Override
    public AbacDataAttributter abacAttributter() {
        return super.abacAttributter().leggTil(AppAbacAttributtType.JOURNALPOST_ID, journalpostId);
    }
}
