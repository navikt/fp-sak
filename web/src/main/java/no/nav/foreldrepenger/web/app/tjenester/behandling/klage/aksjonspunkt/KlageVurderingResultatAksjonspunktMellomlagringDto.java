package no.nav.foreldrepenger.web.app.tjenester.behandling.klage.aksjonspunkt;


import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import no.nav.foreldrepenger.behandlingslager.behandling.klage.KlageHjemmel;
import no.nav.foreldrepenger.behandlingslager.behandling.klage.KlageMedholdÅrsak;
import no.nav.foreldrepenger.behandlingslager.behandling.klage.KlageVurdering;
import no.nav.foreldrepenger.behandlingslager.behandling.klage.KlageVurderingOmgjør;
import no.nav.foreldrepenger.validering.ValidKodeverk;
import no.nav.vedtak.util.InputValideringRegex;

import java.util.UUID;

@JsonAutoDetect(getterVisibility= JsonAutoDetect.Visibility.NONE, setterVisibility= JsonAutoDetect.Visibility.NONE, fieldVisibility= JsonAutoDetect.Visibility.ANY)
public class KlageVurderingResultatAksjonspunktMellomlagringDto {

    @Pattern(regexp = InputValideringRegex.KODEVERK)
    @JsonProperty("kode")
    private String kode;

    @Valid
    @NotNull
    @JsonProperty("behandlingUuid")
    private UUID behandlingUuid;

    @ValidKodeverk
    @JsonProperty("klageVurdering")
    private KlageVurdering klageVurdering;

    @Size(max = 2000)
    @Pattern(regexp = InputValideringRegex.FRITEKST)
    @JsonProperty("begrunnelse")
    private String begrunnelse;

    @Size(max = 100000)
    @Pattern(regexp = InputValideringRegex.FRITEKST)
    @JsonProperty("fritekstTilBrev")
    private String fritekstTilBrev;

    @ValidKodeverk
    @JsonProperty("klageMedholdArsak")
    private KlageMedholdÅrsak klageMedholdArsak;

    @ValidKodeverk
    @JsonProperty("klageVurderingOmgjoer")
    private KlageVurderingOmgjør klageVurderingOmgjoer;

    @ValidKodeverk
    @JsonProperty("klageHjemmel")
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

    public KlageVurdering getKlageVurdering() {
        return klageVurdering;
    }

    public String getBegrunnelse() {
        return begrunnelse;
    }

    public String getFritekstTilBrev() {
        return fritekstTilBrev;
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

    public String getKode() {
        return kode;
    }

    public UUID getBehandlingUuid() {
        return behandlingUuid;
    }
}
