package no.nav.foreldrepenger.web.app.tjenester.behandling.klage.aksjonspunkt;

import java.time.LocalDate;

import javax.validation.Valid;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Pattern;
import javax.validation.constraints.Size;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonTypeName;

import no.nav.foreldrepenger.behandling.aksjonspunkt.BekreftetAksjonspunktDto;
import no.nav.foreldrepenger.behandlingslager.behandling.aksjonspunkt.AksjonspunktKodeDefinisjon;
import no.nav.foreldrepenger.behandlingslager.behandling.klage.KlageAvvistÅrsak;
import no.nav.foreldrepenger.behandlingslager.behandling.klage.KlageMedholdÅrsak;
import no.nav.foreldrepenger.behandlingslager.behandling.klage.KlageVurdering;
import no.nav.foreldrepenger.behandlingslager.behandling.klage.KlageVurderingOmgjør;
import no.nav.foreldrepenger.validering.ValidKodeverk;
import no.nav.vedtak.util.InputValideringRegex;

@JsonAutoDetect(getterVisibility= JsonAutoDetect.Visibility.NONE, setterVisibility= JsonAutoDetect.Visibility.NONE, fieldVisibility= JsonAutoDetect.Visibility.ANY)
public abstract class KlageVurderingResultatAksjonspunktDto extends BekreftetAksjonspunktDto {

    @NotNull
    @ValidKodeverk
    @JsonProperty("klageVurdering")
    private KlageVurdering klageVurdering;

    @Size(max = 2000)
    @Pattern(regexp = InputValideringRegex.FRITEKST)
    @JsonProperty("begrunnelse")
    private String begrunnelse;

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

    @Valid
    @JsonProperty("vedtaksdatoPaklagdBehandling")
    private LocalDate vedtaksdatoPaklagdBehandling;

    @JsonProperty("erGodkjentAvMedunderskriver")
    private boolean erGodkjentAvMedunderskriver;

    KlageVurderingResultatAksjonspunktDto() { // NOSONAR
        // For Jackson
    }

    public KlageVurderingResultatAksjonspunktDto( // NOSONAR
            String begrunnelse,
            KlageVurdering klageVurdering,
            KlageMedholdÅrsak klageMedholdArsak,
            KlageAvvistÅrsak klageAvvistArsak,
            LocalDate vedtaksdatoPaklagdBehandling,
            String fritekstTilBrev, KlageVurderingOmgjør klageVurderingOmgjoer, boolean erGodkjentAvMedunderskriver) {
        super(begrunnelse);
        this.klageVurdering = klageVurdering;
        this.begrunnelse = begrunnelse;
        this.fritekstTilBrev = fritekstTilBrev;
        this.klageAvvistArsak = klageAvvistArsak;
        this.klageMedholdArsak = klageMedholdArsak;
        this.klageVurderingOmgjoer = klageVurderingOmgjoer;
        this.vedtaksdatoPaklagdBehandling = vedtaksdatoPaklagdBehandling;
        this.erGodkjentAvMedunderskriver = erGodkjentAvMedunderskriver;
    }

    public KlageVurdering getKlageVurdering() {
        return klageVurdering;
    }

    @Override
    public String getBegrunnelse() {
        return begrunnelse;
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

    public LocalDate getVedtaksdatoPaklagdBehandling() {
        return vedtaksdatoPaklagdBehandling;
    }

    public boolean isErGodkjentAvMedunderskriver() {
        return erGodkjentAvMedunderskriver;
    }

    @JsonTypeName(AksjonspunktKodeDefinisjon.MANUELL_VURDERING_AV_KLAGE_NFP_KODE)
    public static class KlageVurderingResultatNfpAksjonspunktDto extends KlageVurderingResultatAksjonspunktDto {


        KlageVurderingResultatNfpAksjonspunktDto() {
            super();
        }

        public KlageVurderingResultatNfpAksjonspunktDto(String begrunnelse, KlageVurdering klageVurdering,
                                                        KlageMedholdÅrsak klageMedholdÅrsak, KlageAvvistÅrsak klageAvvistÅrsak,
                                                        LocalDate vedtaksdatoPaklagdBehandling, String fritekstTilBrev, KlageVurderingOmgjør klageVurderingOmgjoer) {
            super(begrunnelse, klageVurdering, klageMedholdÅrsak, klageAvvistÅrsak, vedtaksdatoPaklagdBehandling, fritekstTilBrev, klageVurderingOmgjoer, false);
        }

    }

    @JsonTypeName(AksjonspunktKodeDefinisjon.MANUELL_VURDERING_AV_KLAGE_NK_KODE)
    public static class KlageVurderingResultatNkAksjonspunktDto extends KlageVurderingResultatAksjonspunktDto {


        KlageVurderingResultatNkAksjonspunktDto() {
            super();
        }

        public KlageVurderingResultatNkAksjonspunktDto(String begrunnelse, KlageVurdering klageVurdering,
                                                       KlageMedholdÅrsak klageMedholdÅrsak, KlageAvvistÅrsak klageAvvistÅrsak,
                                                       LocalDate vedtaksdatoPaklagdBehandling, String fritekstTilBrev, KlageVurderingOmgjør klageVurderingOmgjoer,
                                                       boolean erGodkjentAvMedunderskriver) {
            super(begrunnelse, klageVurdering, klageMedholdÅrsak, klageAvvistÅrsak, vedtaksdatoPaklagdBehandling, fritekstTilBrev, klageVurderingOmgjoer, erGodkjentAvMedunderskriver);
        }

    }

}
