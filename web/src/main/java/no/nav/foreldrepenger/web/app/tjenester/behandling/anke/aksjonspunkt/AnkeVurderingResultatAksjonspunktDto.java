package no.nav.foreldrepenger.web.app.tjenester.behandling.anke.aksjonspunkt;

import javax.validation.constraints.NotNull;
import javax.validation.constraints.Pattern;
import javax.validation.constraints.Size;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonTypeName;

import no.nav.foreldrepenger.behandling.aksjonspunkt.BekreftetAksjonspunktDto;
import no.nav.foreldrepenger.behandlingslager.behandling.aksjonspunkt.AksjonspunktKodeDefinisjon;
import no.nav.foreldrepenger.behandlingslager.behandling.anke.AnkeOmgjørÅrsak;
import no.nav.foreldrepenger.behandlingslager.behandling.anke.AnkeVurdering;
import no.nav.foreldrepenger.behandlingslager.behandling.anke.AnkeVurderingOmgjør;
import no.nav.foreldrepenger.validering.ValidKodeverk;
import no.nav.vedtak.util.InputValideringRegex;

@JsonTypeName(AksjonspunktKodeDefinisjon.MANUELL_VURDERING_AV_ANKE_KODE)
public class AnkeVurderingResultatAksjonspunktDto extends BekreftetAksjonspunktDto {

    @NotNull
    @ValidKodeverk
    private AnkeVurdering ankeVurdering;

    @NotNull
    @JsonProperty("erSubsidiartRealitetsbehandles")
    private boolean erSubsidiartRealitetsbehandles;

    @Size(max = 2000)
    @Pattern(regexp = InputValideringRegex.FRITEKST)
    private String begrunnelse;

    // Økt størrelsen for å håndtere all fritekst som blir skrevet til ankebrev
    @Size(max = 100000)
    @Pattern(regexp = InputValideringRegex.FRITEKST)
    private String fritekstTilBrev;

    @Size(max = 100000)
    @Pattern(regexp = InputValideringRegex.FRITEKST)
    private String merknaderFraBruker;

    @NotNull
    @JsonProperty("erMerknaderMottatt")
    private boolean erMerknaderMottatt;

    @ValidKodeverk
    private AnkeOmgjørÅrsak ankeOmgjoerArsak;

    @ValidKodeverk
    private AnkeVurderingOmgjør ankeVurderingOmgjoer;

    private boolean erGodkjentAvMedunderskriver;

    @JsonProperty("vedtak")
    // TODO (BehandlingIdDto): bør kunne støtte behandlingUuid også?  Hvorfor heter property "vedtak"?
    private Long påAnketBehandlingId;

    @NotNull
    @JsonProperty("erAnkerIkkePart")
    private boolean erAnkerIkkePart;

    @NotNull
    @JsonProperty("erFristIkkeOverholdt")
    private boolean erFristIkkeOverholdt;

    @NotNull
    @JsonProperty("erIkkeKonkret")
    private boolean erIkkeKonkret;

    @NotNull
    @JsonProperty("erIkkeSignert")
    private boolean erIkkeSignert;

    AnkeVurderingResultatAksjonspunktDto() { // NOSONAR
        // For Jackson
    }

    public AnkeVurderingResultatAksjonspunktDto( // NOSONAR
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
                                                 boolean erIkkeSignert,
                                                 boolean erGodkjentAvMedunderskriver,
                                                 String merknaderFraBruker,
                                                 boolean erMerknaderMottatt) {
        super(begrunnelse);
        this.ankeVurdering = ankeVurdering;
        this.begrunnelse = begrunnelse;
        this.fritekstTilBrev = fritekstTilBrev;
        this.ankeOmgjoerArsak = ankeOmgjoerArsak;
        this.ankeVurderingOmgjoer = ankeVurderingOmgjoer;
        this.erSubsidiartRealitetsbehandles = erSubsidiartRealitetsbehandles;
        this.påAnketBehandlingId = påAnketBehandlingId;
        this.erAnkerIkkePart = erIkkeAnkerPart;
        this.erFristIkkeOverholdt = erFristIkkeOverholdt;
        this.erIkkeKonkret = erIkkeKonkret;
        this.erIkkeSignert = erIkkeSignert;
        this.erGodkjentAvMedunderskriver = erGodkjentAvMedunderskriver;
        this.merknaderFraBruker = merknaderFraBruker;
        this.erMerknaderMottatt = erMerknaderMottatt;
    }

    public AnkeVurdering getAnkeVurdering() {
        return ankeVurdering;
    }

    public boolean erSubsidiartRealitetsbehandles() {
        return erSubsidiartRealitetsbehandles;
    }

    @Override
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

    public boolean erGodkjentAvMedunderskriver() {
        return erGodkjentAvMedunderskriver;
    }

    public Long hentPåAnketBehandlingId() {
        return påAnketBehandlingId;
    }

    public boolean erAnkerIkkePart() {
        return erAnkerIkkePart;
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

    public String getMerknaderFraBruker() {
        return merknaderFraBruker;
    }

    public boolean erMerknaderMottatt() {
        return erMerknaderMottatt;
    }

}
