package no.nav.foreldrepenger.web.app.tjenester.behandling.anke;

import java.util.UUID;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.JsonProperty;

import no.nav.foreldrepenger.behandlingslager.behandling.anke.AnkeOmgjørÅrsak;
import no.nav.foreldrepenger.behandlingslager.behandling.anke.AnkeVurdering;
import no.nav.foreldrepenger.behandlingslager.behandling.anke.AnkeVurderingOmgjør;

@JsonAutoDetect(getterVisibility= JsonAutoDetect.Visibility.NONE, setterVisibility= JsonAutoDetect.Visibility.NONE, fieldVisibility= JsonAutoDetect.Visibility.ANY)
public class AnkeVurderingResultatDto {

    @JsonProperty("ankeVurdering")
    private AnkeVurdering ankeVurdering;
    @JsonProperty("begrunnelse")
    private String begrunnelse;
    @JsonProperty("fritekstTilBrev")
    private String fritekstTilBrev;
    @JsonProperty("ankeOmgjoerArsak")
    private AnkeOmgjørÅrsak ankeOmgjoerArsak;
    @JsonProperty("ankeVurderingOmgjoer")
    private AnkeVurderingOmgjør ankeVurderingOmgjoer;
    @JsonProperty("godkjentAvMedunderskriver")
    private boolean godkjentAvMedunderskriver;
    @JsonProperty("erAnkerIkkePart")
    private boolean erAnkerIkkePart;
    @JsonProperty("erFristIkkeOverholdt")
    private boolean erFristIkkeOverholdt;
    @JsonProperty("erIkkeKonkret")
    private boolean erIkkeKonkret;
    @JsonProperty("erIkkeSignert")
    private boolean erIkkeSignert;
    @JsonProperty("erSubsidiartRealitetsbehandles")
    private boolean erSubsidiartRealitetsbehandles;
    @JsonProperty("erMerknaderMottatt")
    private boolean erMerknaderMottatt;
    @JsonProperty("merknadKommentar")
    private String merknadKommentar;
    @JsonProperty("paAnketBehandlingId")
    private Long paAnketBehandlingId;
    @JsonProperty("paAnketBehandlingUuid")
    private UUID paAnketBehandlingUuid;

    public AnkeVurderingResultatDto() {
    }

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

    public boolean isGodkjentAvMedunderskriver() {
        return godkjentAvMedunderskriver;
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

    public Long getPaAnketBehandlingId() {
        return paAnketBehandlingId;
    }

    public UUID getPaAnketBehandlingUuid() {
        return paAnketBehandlingUuid;
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

    public void setGodkjentAvMedunderskriver(boolean godkjentAvMedunderskriver) {
        this.godkjentAvMedunderskriver = godkjentAvMedunderskriver;
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

    public void setPaAnketBehandlingId(Long paAnketBehandlingId) {
        this.paAnketBehandlingId = paAnketBehandlingId;
    }

    public void setPaAnketBehandlingUuid(UUID paAnketBehandlingUuid) {
        this.paAnketBehandlingUuid = paAnketBehandlingUuid;
    }
}
