package no.nav.foreldrepenger.web.app.tjenester.behandling.klage.aksjonspunkt;

import java.time.LocalDate;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonTypeName;

import no.nav.foreldrepenger.behandling.aksjonspunkt.BekreftetAksjonspunktDto;
import no.nav.foreldrepenger.behandlingslager.behandling.aksjonspunkt.AksjonspunktKodeDefinisjon;
import no.nav.foreldrepenger.behandlingslager.behandling.klage.KlageAvvistÅrsak;
import no.nav.foreldrepenger.behandlingslager.behandling.klage.KlageHjemmel;
import no.nav.foreldrepenger.behandlingslager.behandling.klage.KlageMedholdÅrsak;
import no.nav.foreldrepenger.behandlingslager.behandling.klage.KlageVurdering;
import no.nav.foreldrepenger.behandlingslager.behandling.klage.KlageVurderingOmgjør;
import no.nav.foreldrepenger.validering.ValidKodeverk;
import no.nav.vedtak.util.InputValideringRegex;

@JsonAutoDetect(getterVisibility = JsonAutoDetect.Visibility.NONE, setterVisibility = JsonAutoDetect.Visibility.NONE, fieldVisibility = JsonAutoDetect.Visibility.ANY)
@JsonTypeName(AksjonspunktKodeDefinisjon.MANUELL_VURDERING_AV_KLAGE_NFP_KODE)
public final class KlageVurderingResultatAksjonspunktDto extends BekreftetAksjonspunktDto {

    @NotNull
    @ValidKodeverk
    @JsonProperty("klageVurdering")
    private KlageVurdering klageVurdering;

    // Økt størrelsen for å håndtere all fritekst som blir skrevet til klagebrev
    @Size(max = 100000)
    @Pattern(regexp = InputValideringRegex.FRITEKST)
    @JsonProperty("fritekstTilBrev")
    private String fritekstTilBrev;

    @ValidKodeverk
    @JsonProperty("klageAvvistArsak")
    private KlageAvvistÅrsak klageAvvistArsak;

    @ValidKodeverk
    @JsonProperty("klageMedholdArsak")
    private KlageMedholdÅrsak klageMedholdArsak;

    @ValidKodeverk
    @JsonProperty("klageVurderingOmgjoer")
    private KlageVurderingOmgjør klageVurderingOmgjoer;

    @ValidKodeverk
    @JsonProperty("klageHjemmel")
    private KlageHjemmel klageHjemmel;

    @Valid
    @JsonProperty("vedtaksdatoPaklagdBehandling")
    private LocalDate vedtaksdatoPaklagdBehandling;

    @JsonProperty("erGodkjentAvMedunderskriver")
    private boolean erGodkjentAvMedunderskriver;

    KlageVurderingResultatAksjonspunktDto() {
        // For Jackson
    }

    public KlageVurderingResultatAksjonspunktDto(String begrunnelse,
                                                 KlageVurdering klageVurdering,
                                                 KlageMedholdÅrsak klageMedholdArsak,
                                                 KlageAvvistÅrsak klageAvvistArsak,
                                                 LocalDate vedtaksdatoPaklagdBehandling,
                                                 String fritekstTilBrev,
                                                 KlageVurderingOmgjør klageVurderingOmgjoer,
                                                 KlageHjemmel klageHjemmel,
                                                 boolean erGodkjentAvMedunderskriver) {
        super(begrunnelse);
        this.klageVurdering = klageVurdering;
        this.fritekstTilBrev = fritekstTilBrev;
        this.klageAvvistArsak = klageAvvistArsak;
        this.klageMedholdArsak = klageMedholdArsak;
        this.klageVurderingOmgjoer = klageVurderingOmgjoer;
        this.klageHjemmel = klageHjemmel;
        this.vedtaksdatoPaklagdBehandling = vedtaksdatoPaklagdBehandling;
        this.erGodkjentAvMedunderskriver = erGodkjentAvMedunderskriver;
    }

    public KlageVurdering getKlageVurdering() {
        return klageVurdering;
    }

    public String getFritekstTilBrev() {
        return fritekstTilBrev;
    }

    public KlageAvvistÅrsak getKlageAvvistArsak() {
        return klageAvvistArsak;
    }

    public KlageMedholdÅrsak getKlageMedholdArsak() {
        return klageMedholdArsak;
    }

    public KlageVurderingOmgjør getKlageVurderingOmgjoer() {
        return klageVurderingOmgjoer;
    }

    public KlageHjemmel getKlageHjemmel() {
        return klageHjemmel;
    }

    public LocalDate getVedtaksdatoPaklagdBehandling() {
        return vedtaksdatoPaklagdBehandling;
    }

    public boolean isErGodkjentAvMedunderskriver() {
        return erGodkjentAvMedunderskriver;
    }


}
