package no.nav.foreldrepenger.web.app.tjenester.dokument.dto;

import java.util.UUID;

import javax.validation.Valid;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Pattern;
import javax.validation.constraints.Size;

import no.nav.foreldrepenger.sikkerhet.abac.AppAbacAttributtType;
import no.nav.vedtak.sikkerhet.abac.AbacDataAttributter;
import no.nav.vedtak.sikkerhet.abac.AbacDto;
import no.nav.vedtak.util.InputValideringRegex;

public class DokumentProdusertDto implements AbacDto {

    @Valid
    @NotNull
    private UUID behandlingUuid;

    @NotNull
    @Size(min = 1, max = 100)
    @Pattern(regexp = InputValideringRegex.KODEVERK)
    private String dokumentMal;

    public DokumentProdusertDto() {
        // trengs for deserialisering av JSON
    }

    public UUID getBehandlingUuid() {
        return behandlingUuid;
    }

    public void setBehandlingUuid(UUID behandlingUuid) {
        this.behandlingUuid = behandlingUuid;
    }

    public String getDokumentMal() {
        return dokumentMal;
    }

    public void setDokumentMal(String dokumentMal) {
        this.dokumentMal = dokumentMal;
    }

    @Override
    public AbacDataAttributter abacAttributter() {
        AbacDataAttributter abac = AbacDataAttributter.opprett();
        return abac.leggTil(AppAbacAttributtType.BEHANDLING_UUID, behandlingUuid);
    }
}
