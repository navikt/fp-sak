package no.nav.foreldrepenger.dokumentbestiller.dto;

import java.util.UUID;

import javax.validation.Valid;
import javax.validation.constraints.Max;
import javax.validation.constraints.Min;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Pattern;
import javax.validation.constraints.Size;

import no.nav.foreldrepenger.dokumentbestiller.DokumentMalType;
import no.nav.vedtak.util.InputValideringRegex;

public class BestillBrevDto {

    @Min(0)
    @Max(Long.MAX_VALUE)
    private Long behandlingId;

    @Valid
    private UUID behandlingUuid;

    @NotNull
    @Pattern(regexp = InputValideringRegex.NAVN)
    @Size(max = 256)
    private String mottaker;

    @NotNull
    @Size(min = 1, max = 100)
    @Pattern(regexp = InputValideringRegex.KODEVERK)
    private String brevmalkode;

    @Size(max = 4000)
    @Pattern(regexp = InputValideringRegex.FRITEKST)
    public String fritekst;

    @Size(min = 1, max = 100)
    @Pattern(regexp = InputValideringRegex.KODEVERK)
    public String arsakskode;

    public BestillBrevDto() { // NOSONAR
    }

    public BestillBrevDto(long behandlingId, UUID behandlingUuid, DokumentMalType dokumentMalType, String fritekst, String arsakskode) { // NOSONAR
        this.behandlingId = behandlingId;
        this.behandlingUuid = behandlingUuid;
        this.brevmalkode = dokumentMalType == null ? null : dokumentMalType.getKode();
        this.fritekst = fritekst;
        this.mottaker = "Søker";
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

    public String getÅrsakskode() {
        return arsakskode;
    }

    public void setÅrsakskode(String årsakskode) {
        this.arsakskode = årsakskode;
    }

    public void setBehandlingId(Long behandlingId) {
        this.behandlingId = behandlingId;
    }

    public String getMottaker() {
        return mottaker;
    }

    public void setMottaker(String mottaker) {
        this.mottaker = mottaker;
    }

    public String getBrevmalkode() {
        return brevmalkode;
    }

    public void setBrevmalkode(String brevmalkode) {
        this.brevmalkode = brevmalkode;
    }

    public String getFritekst() {
        return fritekst;
    }

    public void setFritekst(String fritekst) {
        this.fritekst = fritekst;
    }
}
