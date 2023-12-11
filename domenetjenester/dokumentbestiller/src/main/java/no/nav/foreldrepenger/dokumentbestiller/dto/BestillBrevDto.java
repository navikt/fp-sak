package no.nav.foreldrepenger.dokumentbestiller.dto;

import java.util.UUID;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

import no.nav.foreldrepenger.behandlingslager.behandling.RevurderingVarslingÅrsak;
import no.nav.foreldrepenger.dokumentbestiller.DokumentMalType;
import no.nav.foreldrepenger.validering.ValidKodeverk;
import no.nav.vedtak.util.InputValideringRegex;

public class BestillBrevDto {

    @Min(0)
    @Max(Long.MAX_VALUE)
    private Long behandlingId;

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

    public BestillBrevDto(long behandlingId, UUID behandlingUuid, DokumentMalType dokumentMalType, String fritekst, RevurderingVarslingÅrsak arsakskode) {
        this.behandlingId = behandlingId;
        this.behandlingUuid = behandlingUuid;
        this.brevmalkode = dokumentMalType;
        this.fritekst = fritekst;
        this.arsakskode = arsakskode;
    }

    public BestillBrevDto(long behandlingId, UUID behandlingUuid, DokumentMalType dokumentMalType) {
        this(behandlingId, behandlingUuid, dokumentMalType, null, null);
    }

    public BestillBrevDto(long behandlingId, UUID behandlingUuid, DokumentMalType dokumentMalType, String fritekst) {
        this(behandlingId, behandlingUuid, dokumentMalType, fritekst, null);
    }

    public Long getBehandlingId() {
        return behandlingId;
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

    public void setBehandlingId(Long behandlingId) {
        this.behandlingId = behandlingId;
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
