package no.nav.foreldrepenger.web.app.tjenester.behandling.klage.aksjonspunkt;

import java.util.UUID;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.JsonProperty;

import no.nav.vedtak.util.InputValideringRegex;
@JsonAutoDetect(getterVisibility= JsonAutoDetect.Visibility.NONE, setterVisibility= JsonAutoDetect.Visibility.NONE, fieldVisibility= JsonAutoDetect.Visibility.ANY)
public class KlageFormKravAksjonspunktMellomlagringDto {
    @Pattern(regexp = InputValideringRegex.KODEVERK)
    @JsonProperty("kode")
    private String kode;
    @Valid
    @NotNull
    @JsonProperty("behandlingUuid")
    private UUID behandlingUuid;
    @JsonProperty("erKlagerPart")
    private boolean erKlagerPart;
    @JsonProperty("erFristOverholdt")
    private boolean erFristOverholdt;
    @JsonProperty("erKonkret")
    private boolean erKonkret;
    @JsonProperty("erSignert")
    private boolean erSignert;
    @JsonProperty("erTilbakekreving")
    private boolean erTilbakekreving;
    @Valid
    private KlageTilbakekrevingDto klageTilbakekreving;
    @JsonProperty("paklagdBehandlingUuid")
    private UUID paKlagdBehandlingUuid;
    @Size(max = 2000)
    @Pattern(regexp = InputValideringRegex.FRITEKST)
    @JsonProperty("begrunnelse")
    private String begrunnelse;
    @Size(max = 100000)
    @Pattern(regexp = InputValideringRegex.FRITEKST)
    @JsonProperty("fritekstTilBrev")
    private String fritekstTilBrev;


    public KlageFormKravAksjonspunktMellomlagringDto() {
        //cdi
    }

    public KlageFormKravAksjonspunktMellomlagringDto(String kode,
                                                     UUID behandlingUuid,
                                                     boolean erKlagerPart,
                                                     boolean erFristOverholdt,
                                                     boolean erKonkret,
                                                     boolean erSignert,
                                                     UUID paKlagdBehandlingUuid,
                                                     boolean erTilbakekreving,
                                                     KlageTilbakekrevingDto klageTilbakekreving,
                                                     String begrunnelse,
                                                     String fritekstTilBrev) {
        this.kode = kode;
        this.behandlingUuid = behandlingUuid;
        this.erKlagerPart = erKlagerPart;
        this.erFristOverholdt = erFristOverholdt;
        this.erKonkret = erKonkret;
        this.erSignert = erSignert;
        this.paKlagdBehandlingUuid = paKlagdBehandlingUuid;
        this.erTilbakekreving = erTilbakekreving;
        this.klageTilbakekreving = klageTilbakekreving;
        this.begrunnelse = begrunnelse;
        this.fritekstTilBrev = fritekstTilBrev;

    }

    public String getKode() {
        return kode;
    }

    public UUID behandlingUuid() {
        return behandlingUuid;
    }

    public boolean erKlagerPart() {
        return erKlagerPart;
    }

    public boolean erFristOverholdt() {
        return erFristOverholdt;
    }

    public boolean erKonkret() {
        return erKonkret;
    }

    public boolean erSignert() {
        return erSignert;
    }

    public UUID paKlagdBehandlingUuid() {
        return paKlagdBehandlingUuid;
    }

    public boolean erTilbakekreving() {
        return erTilbakekreving;
    }

    public KlageTilbakekrevingDto klageTilbakekreving() {
        return klageTilbakekreving;
    }

    public String fritekstTilBrev() {
        return fritekstTilBrev;
    }

    public String begrunnelse() {
        return begrunnelse;
    }
    public UUID hentpåKlagdEksternBehandlingUuId() {
        return erTilbakekreving
            && klageTilbakekreving != null ? klageTilbakekreving.tilbakekrevingUuid() : null;
    }
}
