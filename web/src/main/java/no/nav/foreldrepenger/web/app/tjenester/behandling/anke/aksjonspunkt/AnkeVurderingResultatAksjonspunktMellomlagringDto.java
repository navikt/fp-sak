package no.nav.foreldrepenger.web.app.tjenester.behandling.anke.aksjonspunkt;

import javax.validation.constraints.Max;
import javax.validation.constraints.Min;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Pattern;
import javax.validation.constraints.Size;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.JsonProperty;

import no.nav.foreldrepenger.behandlingslager.behandling.anke.AnkeOmgjørÅrsak;
import no.nav.foreldrepenger.behandlingslager.behandling.anke.AnkeVurdering;
import no.nav.foreldrepenger.behandlingslager.behandling.anke.AnkeVurderingOmgjør;
import no.nav.foreldrepenger.sikkerhet.abac.AppAbacAttributtType;
import no.nav.foreldrepenger.validering.ValidKodeverk;
import no.nav.vedtak.sikkerhet.abac.AbacDataAttributter;
import no.nav.vedtak.sikkerhet.abac.AbacDto;
import no.nav.vedtak.util.InputValideringRegex;

@JsonAutoDetect(getterVisibility= JsonAutoDetect.Visibility.NONE, setterVisibility= JsonAutoDetect.Visibility.NONE, fieldVisibility= JsonAutoDetect.Visibility.ANY)
public class AnkeVurderingResultatAksjonspunktMellomlagringDto implements AbacDto {

    @Pattern(regexp = InputValideringRegex.KODEVERK)
    private String kode;

    @Min(0)
    @Max(Long.MAX_VALUE)
    // TODO (BehandlingIdDto): bør kunne støtte behandlingUuid også?
    private Long behandlingId;

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
    // TODO (BehandlingIdDto): bør kunne støtte behandlingUuid også?  Hvorfor heter property "vedtak"?
    private Long påAnketBehandlingId;

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
                                                              String begrunnelse,
                                                              AnkeVurdering ankeVurdering,
                                                              AnkeOmgjørÅrsak ankeOmgjoerArsak,
                                                              String fritekstTilBrev,
                                                              AnkeVurderingOmgjør ankeVurderingOmgjoer,
                                                              boolean erSubsidiartRealitetsbehandles,
                                                              Long påAnketBehandlingId,
                                                              boolean erIkkeAnkerPart,
                                                              boolean erFristIkkeOverholdt,
                                                              boolean erIkkeKonkret,
                                                              boolean erIkkeSignert) {
        this.kode = kode;
        this.behandlingId = behandlingId;
        this.begrunnelse = begrunnelse;
        this.ankeVurdering = ankeVurdering;
        this.fritekstTilBrev = fritekstTilBrev;
        this.ankeOmgjoerArsak = ankeOmgjoerArsak;
        this.ankeVurderingOmgjoer = ankeVurderingOmgjoer;
        this.erSubsidiartRealitetsbehandles = erSubsidiartRealitetsbehandles;
        this.påAnketBehandlingId = påAnketBehandlingId;
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

    public Long hentPåAnketBehandlingId() {
        return påAnketBehandlingId;
    }


    @Override
    public AbacDataAttributter abacAttributter() {
        return AbacDataAttributter.opprett()
            .leggTil(AppAbacAttributtType.BEHANDLING_ID, behandlingId);
    }
}
