package no.nav.foreldrepenger.web.app.tjenester.behandling.klage.aksjonspunkt;


import javax.validation.constraints.Max;
import javax.validation.constraints.Min;
import javax.validation.constraints.Pattern;
import javax.validation.constraints.Size;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.JsonProperty;

import no.nav.foreldrepenger.behandlingslager.behandling.klage.KlageMedholdÅrsak;
import no.nav.foreldrepenger.behandlingslager.behandling.klage.KlageVurdering;
import no.nav.foreldrepenger.behandlingslager.behandling.klage.KlageVurderingOmgjør;
import no.nav.foreldrepenger.sikkerhet.abac.AppAbacAttributtType;
import no.nav.foreldrepenger.validering.ValidKodeverk;
import no.nav.vedtak.sikkerhet.abac.AbacDataAttributter;
import no.nav.vedtak.sikkerhet.abac.AbacDto;
import no.nav.vedtak.util.InputValideringRegex;

@JsonAutoDetect(getterVisibility= JsonAutoDetect.Visibility.NONE, setterVisibility= JsonAutoDetect.Visibility.NONE, fieldVisibility= JsonAutoDetect.Visibility.ANY)
public class KlageVurderingResultatAksjonspunktMellomlagringDto implements AbacDto {

    @Pattern(regexp = InputValideringRegex.KODEVERK)
    @JsonProperty("kode")
    private String kode;

    @Min(0)
    @Max(Long.MAX_VALUE)
    @JsonProperty("behandlingId")
    // TODO (BehandlingIdDto): bør kunne støtte behandlingUuid også?
    private Long behandlingId;

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

    public KlageVurderingResultatAksjonspunktMellomlagringDto() { // NOSONAR
        // For Jackson
    }

    public KlageVurderingResultatAksjonspunktMellomlagringDto( // NOSONAR
                                                               String kode,
                                                               Long behandlingId,
                                                               String begrunnelse,
                                                               KlageVurdering klageVurdering,
                                                               KlageMedholdÅrsak klageMedholdArsak,
                                                               String fritekstTilBrev,
                                                               KlageVurderingOmgjør klageVurderingOmgjoer) {
        this.kode = kode;
        this.behandlingId = behandlingId;
        this.begrunnelse = begrunnelse;
        this.klageVurdering = klageVurdering;
        this.begrunnelse = begrunnelse;
        this.fritekstTilBrev = fritekstTilBrev;
        this.klageMedholdArsak = klageMedholdArsak;
        this.klageVurderingOmgjoer = klageVurderingOmgjoer;
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

    public String getKode() {
        return kode;
    }

    public Long getBehandlingId() {
        return behandlingId;
    }

    @Override
    public AbacDataAttributter abacAttributter() {
        return AbacDataAttributter.opprett()
            .leggTil(AppAbacAttributtType.BEHANDLING_ID, behandlingId);
    }
}
