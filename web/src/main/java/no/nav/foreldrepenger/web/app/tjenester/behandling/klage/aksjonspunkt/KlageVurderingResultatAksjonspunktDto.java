package no.nav.foreldrepenger.web.app.tjenester.behandling.klage.aksjonspunkt;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.JsonTypeName;

import no.nav.foreldrepenger.behandling.aksjonspunkt.BekreftetAksjonspunktDto;
import no.nav.foreldrepenger.behandlingslager.behandling.aksjonspunkt.AksjonspunktKodeDefinisjon;
import no.nav.foreldrepenger.behandlingslager.behandling.klage.KlageHjemmel;
import no.nav.foreldrepenger.behandlingslager.behandling.klage.KlageMedholdÅrsak;
import no.nav.foreldrepenger.behandlingslager.behandling.klage.KlageVurdering;
import no.nav.foreldrepenger.behandlingslager.behandling.klage.KlageVurderingOmgjør;
import no.nav.foreldrepenger.validering.ValidKodeverk;
import no.nav.vedtak.util.InputValideringRegex;

@JsonAutoDetect(getterVisibility = JsonAutoDetect.Visibility.NONE, setterVisibility = JsonAutoDetect.Visibility.NONE, fieldVisibility = JsonAutoDetect.Visibility.ANY)
@JsonTypeName(AksjonspunktKodeDefinisjon.MANUELL_VURDERING_AV_KLAGE_NFP_KODE)
public final class KlageVurderingResultatAksjonspunktDto extends BekreftetAksjonspunktDto implements KlageVurderingLagreDto {

    @NotNull
    @ValidKodeverk
    private KlageVurdering klageVurdering;

    // Økt størrelsen for å håndtere all fritekst som blir skrevet til klagebrev
    @Size(max = 100000)
    @Pattern(regexp = InputValideringRegex.FRITEKST)
    private String fritekstTilBrev;

    @ValidKodeverk
    private KlageMedholdÅrsak klageMedholdArsak;

    @ValidKodeverk
    private KlageMedholdÅrsak klageMedholdÅrsak;

    @ValidKodeverk
    private KlageVurderingOmgjør klageVurderingOmgjoer;

    @ValidKodeverk
    private KlageVurderingOmgjør klageVurderingOmgjør;

    @ValidKodeverk
    private KlageHjemmel klageHjemmel;

    KlageVurderingResultatAksjonspunktDto() {
        // For Jackson
    }

    public KlageVurderingResultatAksjonspunktDto(String begrunnelse,
                                                 KlageVurdering klageVurdering,
                                                 KlageMedholdÅrsak klageMedholdÅrsak,
                                                 String fritekstTilBrev,
                                                 KlageVurderingOmgjør klageVurderingOmgjør,
                                                 KlageHjemmel klageHjemmel) {
        super(begrunnelse);
        this.klageVurdering = klageVurdering;
        this.fritekstTilBrev = fritekstTilBrev;
        this.klageMedholdArsak = klageMedholdÅrsak;
        this.klageMedholdÅrsak = klageMedholdÅrsak;
        this.klageVurderingOmgjoer = klageVurderingOmgjør;
        this.klageVurderingOmgjør = klageVurderingOmgjør;
        this.klageHjemmel = klageHjemmel;
    }

    @Override
    public KlageVurdering getKlageVurdering() {
        return klageVurdering;
    }

    public String getFritekstTilBrev() {
        return fritekstTilBrev;
    }

    @Override
    public KlageMedholdÅrsak getKlageMedholdÅrsak() {
        return klageMedholdÅrsak == null ? klageMedholdArsak : klageMedholdÅrsak;
    }

    @Override
    public KlageVurderingOmgjør getKlageVurderingOmgjør() {
        return klageVurderingOmgjør == null ? klageVurderingOmgjoer : klageVurderingOmgjør;
    }

    @Override
    public KlageHjemmel getKlageHjemmel() {
        return klageHjemmel;
    }


}
