package no.nav.foreldrepenger.web.app.tjenester.behandling.klage.aksjonspunkt;


import java.util.UUID;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

import com.fasterxml.jackson.annotation.JsonAutoDetect;

import no.nav.foreldrepenger.behandlingslager.behandling.klage.KlageHjemmel;
import no.nav.foreldrepenger.behandlingslager.behandling.klage.KlageMedholdÅrsak;
import no.nav.foreldrepenger.behandlingslager.behandling.klage.KlageVurdering;
import no.nav.foreldrepenger.behandlingslager.behandling.klage.KlageVurderingOmgjør;
import no.nav.foreldrepenger.validering.ValidKodeverk;
import no.nav.vedtak.util.InputValideringRegex;

@JsonAutoDetect(getterVisibility= JsonAutoDetect.Visibility.NONE, setterVisibility= JsonAutoDetect.Visibility.NONE, fieldVisibility= JsonAutoDetect.Visibility.ANY)
public class KlageVurderingResultatAksjonspunktMellomlagringDto implements KlageVurderingLagreDto{

    @Pattern(regexp = InputValideringRegex.KODEVERK)
    private String kode;

    @Valid
    @NotNull
    private UUID behandlingUuid;

    @ValidKodeverk
    private KlageVurdering klageVurdering;

    @Size(max = 2000)
    @Pattern(regexp = InputValideringRegex.FRITEKST)
    private String begrunnelse;

    @Size(max = 100000)
    @Pattern(regexp = InputValideringRegex.FRITEKST)
    private String fritekstTilBrev;

    @ValidKodeverk
    private KlageMedholdÅrsak klageMedholdArsak;

    @ValidKodeverk
    private KlageVurderingOmgjør klageVurderingOmgjoer;

    @ValidKodeverk
    private KlageHjemmel klageHjemmel;

    public KlageVurderingResultatAksjonspunktMellomlagringDto() {
        // For Jackson
    }

    public KlageVurderingResultatAksjonspunktMellomlagringDto(String kode,
                                                              UUID behandlingUuid,
                                                              String begrunnelse,
                                                              KlageVurdering klageVurdering,
                                                              KlageMedholdÅrsak klageMedholdArsak,
                                                              String fritekstTilBrev,
                                                              KlageVurderingOmgjør klageVurderingOmgjoer,
                                                              KlageHjemmel klageHjemmel) {
        this.kode = kode;
        this.behandlingUuid = behandlingUuid;
        this.begrunnelse = begrunnelse;
        this.klageVurdering = klageVurdering;
        this.fritekstTilBrev = fritekstTilBrev;
        this.klageMedholdArsak = klageMedholdArsak;
        this.klageVurderingOmgjoer = klageVurderingOmgjoer;
        this.klageHjemmel = klageHjemmel;
    }

    @Override
    public KlageVurdering getKlageVurdering() {
        return klageVurdering;
    }

    public String getBegrunnelse() {
        return begrunnelse;
    }

    public String getFritekstTilBrev() {
        return fritekstTilBrev;
    }

    @Override
    public KlageMedholdÅrsak getKlageMedholdArsak() {
        return klageMedholdArsak;
    }

    @Override
    public KlageVurderingOmgjør getKlageVurderingOmgjoer() {
        return klageVurderingOmgjoer;
    }

    @Override
    public KlageHjemmel getKlageHjemmel() {
        return klageHjemmel;
    }

    public String getKode() {
        return kode;
    }

    public UUID getBehandlingUuid() {
        return behandlingUuid;
    }
}
