package no.nav.foreldrepenger.web.app.tjenester.forvaltning.dto;

import javax.validation.constraints.Digits;
import javax.validation.constraints.Max;
import javax.validation.constraints.Min;
import javax.validation.constraints.NotNull;
import javax.ws.rs.DefaultValue;
import javax.ws.rs.QueryParam;

import no.nav.foreldrepenger.sikkerhet.abac.AppAbacAttributtType;
import no.nav.vedtak.sikkerhet.abac.AbacDataAttributter;
import no.nav.vedtak.sikkerhet.abac.AbacDto;

public class HoppTilbakeTil5080OgSlettInntektsmeldingDto implements AbacDto {

    @NotNull
    @QueryParam("behandlingId")
    @DefaultValue("0")
    @Min(0)
    @Max(Long.MAX_VALUE)
    private Long behandlingId;

    @NotNull
    @QueryParam("journalpostid")
    @Digits(integer = 18, fraction = 0)
    private String journalpostId;

    public HoppTilbakeTil5080OgSlettInntektsmeldingDto(@NotNull Long behandlingId, @NotNull String journalpostId) {
        this.behandlingId = behandlingId;
        this.journalpostId = journalpostId;
    }

    public HoppTilbakeTil5080OgSlettInntektsmeldingDto() {
    }

    @Override
    public AbacDataAttributter abacAttributter() {
        var abac = AbacDataAttributter.opprett();
        if (behandlingId != null) {
            abac.leggTil(AppAbacAttributtType.BEHANDLING_ID, behandlingId);
        }
        if (journalpostId != null) {
            abac.leggTil(AppAbacAttributtType.JOURNALPOST_ID, journalpostId);
        }
        return abac;
    }

    public Long getBehandlingId() {
        return behandlingId;
    }

    public String getJournalpostId() {
        return journalpostId;
    }
}
