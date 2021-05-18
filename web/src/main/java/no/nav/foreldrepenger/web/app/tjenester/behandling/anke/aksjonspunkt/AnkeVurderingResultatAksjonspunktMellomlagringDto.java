package no.nav.foreldrepenger.web.app.tjenester.behandling.anke.aksjonspunkt;

import java.util.UUID;

import javax.validation.Valid;
import javax.validation.constraints.Max;
import javax.validation.constraints.Min;
import javax.validation.constraints.Pattern;
import javax.validation.constraints.Size;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.JsonProperty;

import no.nav.foreldrepenger.behandlingslager.behandling.anke.AnkeOmgjørÅrsak;
import no.nav.foreldrepenger.behandlingslager.behandling.anke.AnkeVurdering;
import no.nav.foreldrepenger.behandlingslager.behandling.anke.AnkeVurderingOmgjør;
import no.nav.foreldrepenger.validering.ValidKodeverk;
import no.nav.vedtak.util.InputValideringRegex;

@JsonAutoDetect(getterVisibility= JsonAutoDetect.Visibility.NONE, setterVisibility= JsonAutoDetect.Visibility.NONE, fieldVisibility= JsonAutoDetect.Visibility.ANY)
public class AnkeVurderingResultatAksjonspunktMellomlagringDto  {

    @Pattern(regexp = InputValideringRegex.KODEVERK)
    @JsonProperty("kode")
    private String kode;

    @Min(0)
    @Max(Long.MAX_VALUE)
    // TODO (BehandlingIdDto): bør kunne støtte behandlingUuid også?
    @JsonProperty("behandlingId")
    private Long behandlingId;

    @Valid
    @JsonProperty("behandlingUuid")
    private UUID behandlingUuid;

    @ValidKodeverk
    @JsonProperty("ankeVurdering")
    private AnkeVurdering ankeVurdering;

    @Size(max = 2000)
    @Pattern(regexp = InputValideringRegex.FRITEKST)
    @JsonProperty("begrunnelse")
    private String begrunnelse;

    @Size(max = 100000)
    @Pattern(regexp = InputValideringRegex.FRITEKST)
    @JsonProperty("fritekstTilBrev")
    private String fritekstTilBrev;

    @ValidKodeverk
    @JsonProperty("ankeOmgjoerArsak")
    private AnkeOmgjørÅrsak ankeOmgjoerArsak;

    @ValidKodeverk
    @JsonProperty("ankeVurderingOmgjoer")
    private AnkeVurderingOmgjør ankeVurderingOmgjoer;

    @Min(0)
    @Max(Long.MAX_VALUE)
    @JsonProperty("vedtak")
    private Long påAnketBehandlingId;

    @JsonProperty("vedtakBehandlingUuid")
    @Valid
    private UUID påAnketBehandlingUuid;

    @JsonProperty("erIkkeAnkerPart")
    private boolean erIkkeAnkerPart;

    @JsonProperty("erFristIkkeOverholdt")
    private boolean erFristIkkeOverholdt;

    @JsonProperty("erIkkeKonkret")
    private boolean erIkkeKonkret;

    @JsonProperty("erIkkeSignert")
    private boolean erIkkeSignert;

    @JsonProperty("erSubsidiartRealitetsbehandles")
    private boolean erSubsidiartRealitetsbehandles;

    public AnkeVurderingResultatAksjonspunktMellomlagringDto() { // NOSONAR
        // For Jackson
    }

    public AnkeVurderingResultatAksjonspunktMellomlagringDto( // NOSONAR
                                                              String kode,
                                                              Long behandlingId,
                                                              UUID behandlingUuid,
                                                              String begrunnelse,
                                                              AnkeVurdering ankeVurdering,
                                                              AnkeOmgjørÅrsak ankeOmgjoerArsak,
                                                              String fritekstTilBrev,
                                                              AnkeVurderingOmgjør ankeVurderingOmgjoer,
                                                              boolean erSubsidiartRealitetsbehandles,
                                                              Long påAnketBehandlingId,
                                                              UUID påAnketBehandlingUuid,
                                                              boolean erIkkeAnkerPart,
                                                              boolean erFristIkkeOverholdt,
                                                              boolean erIkkeKonkret,
                                                              boolean erIkkeSignert) {
        this.kode = kode;
        this.behandlingId = behandlingId;
        this.behandlingUuid = behandlingUuid;
        this.begrunnelse = begrunnelse;
        this.ankeVurdering = ankeVurdering;
        this.fritekstTilBrev = fritekstTilBrev;
        this.ankeOmgjoerArsak = ankeOmgjoerArsak;
        this.ankeVurderingOmgjoer = ankeVurderingOmgjoer;
        this.erSubsidiartRealitetsbehandles = erSubsidiartRealitetsbehandles;
        this.påAnketBehandlingId = påAnketBehandlingId;
        this.påAnketBehandlingUuid = påAnketBehandlingUuid;
        this.erIkkeAnkerPart = erIkkeAnkerPart;
        this.erFristIkkeOverholdt = erFristIkkeOverholdt;
        this.erIkkeKonkret = erIkkeKonkret;
        this.erIkkeSignert = erIkkeSignert;
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

    public boolean erIkkeAnkerPart() {
        return erIkkeAnkerPart;
    }

    public boolean erFristIkkeOverholdt() {
        return erFristIkkeOverholdt;
    }

    public boolean erIkkeKonkret() {
        return erIkkeKonkret;
    }

    public boolean erIkkeSignert() {
        return erIkkeSignert;
    }

    public boolean erSubsidiartRealitetsbehandles() {
        return erSubsidiartRealitetsbehandles;
    }

    public String getKode() {
        return kode;
    }

    public Long getBehandlingId() {
        return behandlingId;
    }

    public UUID getBehandlingUuid() {
        return behandlingUuid;
    }

    public Long hentPåAnketBehandlingId() {
        return påAnketBehandlingId;
    }

    public UUID hentPåAnketBehandlingUuid() {
        return påAnketBehandlingUuid;
    }
}
