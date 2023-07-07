package no.nav.foreldrepenger.web.app.tjenester.behandling.klage.aksjonspunkt;


import java.util.UUID;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonTypeName;

import no.nav.foreldrepenger.behandling.aksjonspunkt.BekreftetAksjonspunktDto;
import no.nav.foreldrepenger.behandlingslager.behandling.aksjonspunkt.AksjonspunktKodeDefinisjon;

@JsonAutoDetect(getterVisibility = JsonAutoDetect.Visibility.NONE, setterVisibility = JsonAutoDetect.Visibility.NONE, fieldVisibility = JsonAutoDetect.Visibility.ANY)
@JsonTypeName(AksjonspunktKodeDefinisjon.VURDERING_AV_FORMKRAV_KLAGE_NFP_KODE)
public final class KlageFormkravAksjonspunktDto extends BekreftetAksjonspunktDto {

    @NotNull
    @JsonProperty("erKlagerPart")
    private boolean erKlagerPart;
    @NotNull
    @JsonProperty("erFristOverholdt")
    private boolean erFristOverholdt;
    @NotNull
    @JsonProperty("erKonkret")
    private boolean erKonkret;
    @NotNull
    @JsonProperty("erSignert")
    private boolean erSignert;

    @JsonProperty("erTilbakekreving")
    private boolean erTilbakekreving;

    @Valid
    private KlageTilbakekrevingDto klageTilbakekreving;

    @JsonProperty("vedtakBehandlingUuid")
    @Valid
    private UUID påKlagdBehandlingUuid;


    KlageFormkravAksjonspunktDto() {
        // For Jackson
    }

    public KlageFormkravAksjonspunktDto(boolean erKlagerPart,
                                        boolean erFristOverholdt,
                                        boolean erKonkret,
                                        boolean erSignert,
                                        UUID påKlagdBehandlingUuid,
                                        String begrunnelse,
                                        boolean erTilbakekreving,
                                        KlageTilbakekrevingDto klageTilbakekreving) {
        super(begrunnelse);
        this.erKlagerPart = erKlagerPart;
        this.erFristOverholdt = erFristOverholdt;
        this.erKonkret = erKonkret;
        this.erSignert = erSignert;
        this.påKlagdBehandlingUuid = påKlagdBehandlingUuid;
        this.erTilbakekreving = erTilbakekreving;
        this.klageTilbakekreving = klageTilbakekreving;
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

    public UUID hentPåKlagdBehandlingUuid() {
        return påKlagdBehandlingUuid;
    }

    public UUID hentpåKlagdEksternBehandlingUuId() {
        return erTilbakekreving
            && klageTilbakekreving != null ? klageTilbakekreving.tilbakekrevingUuid() : null;
    }

    public boolean erTilbakekreving() {
        return erTilbakekreving;
    }

    @JsonProperty("tilbakekrevingInfo")
    public KlageTilbakekrevingDto getKlageTilbakekreving() {
        return klageTilbakekreving;
    }


}
