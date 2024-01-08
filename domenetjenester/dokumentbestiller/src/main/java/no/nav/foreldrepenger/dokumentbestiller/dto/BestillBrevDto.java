package no.nav.foreldrepenger.dokumentbestiller.dto;

import java.util.UUID;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

import no.nav.foreldrepenger.behandlingslager.behandling.RevurderingVarslingÅrsak;
import no.nav.foreldrepenger.dokumentbestiller.DokumentMalType;
import no.nav.foreldrepenger.validering.ValidKodeverk;
import no.nav.vedtak.util.InputValideringRegex;

public class BestillBrevDto {
    @Valid
    private UUID behandlingUuid;

    @ValidKodeverk
    @NotNull
    private DokumentMalType brevmalkode;

    @Size(max = 10000)
    @Pattern(regexp = InputValideringRegex.FRITEKST)
    private String fritekst;

    @ValidKodeverk
    private RevurderingVarslingÅrsak arsakskode;

    public BestillBrevDto() {
    }

    public BestillBrevDto(UUID behandlingUuid, DokumentMalType dokumentMalType, String fritekst, RevurderingVarslingÅrsak arsakskode) {
        this.behandlingUuid = behandlingUuid;
        this.brevmalkode = dokumentMalType;
        this.fritekst = fritekst;
        this.arsakskode = arsakskode;
    }

    public BestillBrevDto(UUID behandlingUuid, DokumentMalType dokumentMalType) {
        this(behandlingUuid, dokumentMalType, null, null);
    }

    public BestillBrevDto(UUID behandlingUuid, DokumentMalType dokumentMalType, String fritekst) {
        this(behandlingUuid, dokumentMalType, fritekst, null);
    }

    public UUID getBehandlingUuid() {
        return behandlingUuid;
    }

    public RevurderingVarslingÅrsak getArsakskode() {
        return arsakskode;
    }

    public void setArsakskode(RevurderingVarslingÅrsak arsakskode) {
        this.arsakskode = arsakskode;
    }

    public DokumentMalType getBrevmalkode() {
        return brevmalkode;
    }

    public void setBrevmalkode(DokumentMalType brevmalkode) {
        this.brevmalkode = brevmalkode;
    }

    public String getFritekst() {
        return fritekst;
    }

    public void setFritekst(String fritekst) {
        this.fritekst = fritekst;
    }
}
