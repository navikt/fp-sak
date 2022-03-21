package no.nav.foreldrepenger.web.app.tjenester.behandling.anke.aksjonspunkt;

import java.util.UUID;

import javax.validation.Valid;
import javax.validation.constraints.Pattern;
import javax.validation.constraints.Size;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonTypeName;

import no.nav.foreldrepenger.behandling.aksjonspunkt.BekreftetAksjonspunktDto;
import no.nav.foreldrepenger.behandlingslager.behandling.aksjonspunkt.AksjonspunktKodeDefinisjon;
import no.nav.foreldrepenger.behandlingslager.behandling.anke.AnkeOmgjørÅrsak;
import no.nav.foreldrepenger.behandlingslager.behandling.anke.AnkeVurdering;
import no.nav.foreldrepenger.behandlingslager.behandling.anke.AnkeVurderingOmgjør;
import no.nav.foreldrepenger.behandlingslager.behandling.klage.KlageHjemmel;
import no.nav.foreldrepenger.validering.ValidKodeverk;
import no.nav.vedtak.util.InputValideringRegex;

@JsonTypeName(AksjonspunktKodeDefinisjon.MANUELL_VURDERING_AV_ANKE_KODE)
@JsonAutoDetect(getterVisibility= JsonAutoDetect.Visibility.NONE, setterVisibility= JsonAutoDetect.Visibility.NONE, fieldVisibility= JsonAutoDetect.Visibility.ANY)
public class AnkeVurderingResultatAksjonspunktDto extends BekreftetAksjonspunktDto {

    @ValidKodeverk
    @JsonProperty("ankeVurdering")
    private AnkeVurdering ankeVurdering;

    @JsonProperty("erSubsidiartRealitetsbehandles")
    private boolean erSubsidiartRealitetsbehandles;

    // Økt størrelsen for å håndtere all fritekst som blir skrevet til ankebrev
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

    @JsonProperty("erGodkjentAvMedunderskriver")
    private boolean erGodkjentAvMedunderskriver;

    @JsonProperty("påAnketKlageBehandlingUuid")
    @Valid
    private UUID påAnketKlageBehandlingUuid;

    @JsonProperty("erAnkerIkkePart")
    private boolean erAnkerIkkePart;

    @JsonProperty("erFristIkkeOverholdt")
    private boolean erFristIkkeOverholdt;

    @JsonProperty("erIkkeKonkret")
    private boolean erIkkeKonkret;

    @JsonProperty("erIkkeSignert")
    private boolean erIkkeSignert;

    @JsonProperty("sendTilKabal")
    private Boolean sendTilKabal;

    @ValidKodeverk
    @JsonProperty("klageHjemmel")
    private KlageHjemmel klageHjemmel;

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
                                                 UUID påAnketKlageBehandlingUuid ,
                                                 boolean erIkkeAnkerPart,
                                                 boolean erFristIkkeOverholdt,
                                                 boolean erIkkeKonkret,
                                                 boolean erIkkeSignert,
                                                 boolean erGodkjentAvMedunderskriver,
                                                 KlageHjemmel klageHjemmel,
                                                 Boolean sendTilKabal) {
        super(begrunnelse);
        this.ankeVurdering = ankeVurdering;
        this.fritekstTilBrev = fritekstTilBrev;
        this.ankeOmgjoerArsak = ankeOmgjoerArsak;
        this.ankeVurderingOmgjoer = ankeVurderingOmgjoer;
        this.erSubsidiartRealitetsbehandles = erSubsidiartRealitetsbehandles;
        this.påAnketKlageBehandlingUuid = påAnketKlageBehandlingUuid ;
        this.erAnkerIkkePart = erIkkeAnkerPart;
        this.erFristIkkeOverholdt = erFristIkkeOverholdt;
        this.erIkkeKonkret = erIkkeKonkret;
        this.erIkkeSignert = erIkkeSignert;
        this.erGodkjentAvMedunderskriver = erGodkjentAvMedunderskriver;
        this.sendTilKabal = sendTilKabal;
        this.klageHjemmel = klageHjemmel;
    }

    public AnkeVurdering getAnkeVurdering() {
        return ankeVurdering;
    }

    public boolean erSubsidiartRealitetsbehandles() {
        return erSubsidiartRealitetsbehandles;
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

    public UUID getPåAnketKlageBehandlingUuid() {
        return påAnketKlageBehandlingUuid;
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

    public Boolean getSendTilKabal() {
        return sendTilKabal;
    }

    public KlageHjemmel getKlageHjemmel() {
        return klageHjemmel;
    }
}
