package no.nav.foreldrepenger.web.app.tjenester.behandling.anke;

import java.util.UUID;

import jakarta.validation.constraints.NotNull;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.JsonProperty;

import no.nav.foreldrepenger.behandlingslager.behandling.anke.AnkeOmgjørÅrsak;
import no.nav.foreldrepenger.behandlingslager.behandling.anke.AnkeVurdering;
import no.nav.foreldrepenger.behandlingslager.behandling.anke.AnkeVurderingOmgjør;

@JsonAutoDetect(getterVisibility = JsonAutoDetect.Visibility.NONE, setterVisibility = JsonAutoDetect.Visibility.NONE, fieldVisibility = JsonAutoDetect.Visibility.ANY)
public class AnkeVurderingResultatDto {

    @JsonProperty("ankeVurdering")
    private AnkeVurdering ankeVurdering;
    @JsonProperty("begrunnelse") @NotNull
    private String begrunnelse;
    @JsonProperty("fritekstTilBrev")
    private String fritekstTilBrev;
    @JsonProperty("ankeOmgjoerArsak")
    private AnkeOmgjørÅrsak ankeOmgjoerArsak;
    @JsonProperty("ankeVurderingOmgjoer")
    private AnkeVurderingOmgjør ankeVurderingOmgjoer;
    @JsonProperty("erAnkerIkkePart") @NotNull
    private boolean erAnkerIkkePart;
    @JsonProperty("erFristIkkeOverholdt") @NotNull
    private boolean erFristIkkeOverholdt;
    @JsonProperty("erIkkeKonkret") @NotNull
    private boolean erIkkeKonkret;
    @JsonProperty("erIkkeSignert") @NotNull
    private boolean erIkkeSignert;
    @JsonProperty("erSubsidiartRealitetsbehandles") @NotNull
    private boolean erSubsidiartRealitetsbehandles;
    @JsonProperty("erMerknaderMottatt")
    private boolean erMerknaderMottatt;
    @JsonProperty("merknadKommentar")
    private String merknadKommentar;
    @JsonProperty("påAnketKlageBehandlingUuid")
    private UUID påAnketKlageBehandlingUuid;
    @JsonProperty("trygderettVurdering")
    private AnkeVurdering trygderettVurdering;
    @JsonProperty("trygderettOmgjoerArsak")
    private AnkeOmgjørÅrsak trygderettOmgjoerArsak;
    @JsonProperty("trygderettVurderingOmgjoer")
    private AnkeVurderingOmgjør trygderettVurderingOmgjoer;

    public AnkeVurdering getAnkeVurdering() {
        return ankeVurdering;
    }

    public String getBegrunnelse() {
        return begrunnelse;
    }

    public String getFritekstTilBrev() {
        return fritekstTilBrev;
    }

    public AnkeOmgjørÅrsak getAnkeOmgjoerArsak() {
        return ankeOmgjoerArsak;
    }

    public AnkeVurderingOmgjør getAnkeVurderingOmgjoer() {
        return ankeVurderingOmgjoer;
    }

    public boolean isErAnkerIkkePart() {
        return erAnkerIkkePart;
    }

    public boolean isErFristIkkeOverholdt() {
        return erFristIkkeOverholdt;
    }

    public boolean isErIkkeKonkret() {
        return erIkkeKonkret;
    }

    public boolean isErIkkeSignert() {
        return erIkkeSignert;
    }

    public boolean isErSubsidiartRealitetsbehandles() {
        return erSubsidiartRealitetsbehandles;
    }

    public UUID getPåAnketKlageBehandlingUuid() {
        return påAnketKlageBehandlingUuid;
    }

    public void setAnkeVurderingOmgjoer(AnkeVurderingOmgjør ankeVurderingOmgjoer) {
        this.ankeVurderingOmgjoer = ankeVurderingOmgjoer;
    }

    void setAnkeVurdering(AnkeVurdering ankeVurdering) {
        this.ankeVurdering = ankeVurdering;
    }

    void setBegrunnelse(String begrunnelse) {
        this.begrunnelse = begrunnelse;
    }

    public void setFritekstTilBrev(String fritekstTilBrev) {
        this.fritekstTilBrev = fritekstTilBrev;
    }

    void setAnkeOmgjoerArsak(AnkeOmgjørÅrsak ankeOmgjoerArsak) {
        this.ankeOmgjoerArsak = ankeOmgjoerArsak;
    }

    public void setErAnkerIkkePart(boolean erAnkerIkkePart) {
        this.erAnkerIkkePart = erAnkerIkkePart;
    }

    public void setErFristIkkeOverholdt(boolean erFristIkkeOverholdt) {
        this.erFristIkkeOverholdt = erFristIkkeOverholdt;
    }

    public void setErIkkeKonkret(boolean erIkkeKonkret) {
        this.erIkkeKonkret = erIkkeKonkret;
    }

    public void setErIkkeSignert(boolean erIkkeSignert) {
        this.erIkkeSignert = erIkkeSignert;
    }

    public void setErSubsidiartRealitetsbehandles(boolean erSubsidiartRealitetsbehandles) {
        this.erSubsidiartRealitetsbehandles = erSubsidiartRealitetsbehandles;
    }

    public boolean isErMerknaderMottatt() {
        return erMerknaderMottatt;
    }

    public void setErMerknaderMottatt(boolean erMerknaderMottatt) {
        this.erMerknaderMottatt = erMerknaderMottatt;
    }

    public String getMerknadKommentar() {
        return merknadKommentar;
    }

    public void setMerknadKommentar(String merknadKommentar) {
        this.merknadKommentar = merknadKommentar;
    }

    public void setPåAnketKlageBehandlingUuid(UUID påAnketKlageBehandlingUuid) {
        this.påAnketKlageBehandlingUuid = påAnketKlageBehandlingUuid;
    }

    public AnkeVurdering getTrygderettVurdering() {
        return trygderettVurdering;
    }

    public void setTrygderettVurdering(AnkeVurdering trygderettVurdering) {
        this.trygderettVurdering = trygderettVurdering;
    }

    public AnkeOmgjørÅrsak getTrygderettOmgjoerArsak() {
        return trygderettOmgjoerArsak;
    }

    public void setTrygderettOmgjoerArsak(AnkeOmgjørÅrsak trygderettOmgjoerArsak) {
        this.trygderettOmgjoerArsak = trygderettOmgjoerArsak;
    }

    public AnkeVurderingOmgjør getTrygderettVurderingOmgjoer() {
        return trygderettVurderingOmgjoer;
    }

    public void setTrygderettVurderingOmgjoer(AnkeVurderingOmgjør trygderettVurderingOmgjoer) {
        this.trygderettVurderingOmgjoer = trygderettVurderingOmgjoer;
    }
}
