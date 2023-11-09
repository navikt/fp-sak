package no.nav.foreldrepenger.web.app.tjenester.behandling.klage.aksjonspunkt;


import java.util.UUID;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonTypeName;

import no.nav.foreldrepenger.behandling.aksjonspunkt.BekreftetAksjonspunktDto;
import no.nav.foreldrepenger.behandlingslager.behandling.aksjonspunkt.AksjonspunktKodeDefinisjon;
import no.nav.vedtak.util.InputValideringRegex;

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

    @Size(max = 100000)
    @Pattern(regexp = InputValideringRegex.FRITEKST)
    @JsonProperty("fritekstTilBrev")
    private String fritekstTilBrev;


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
                                        KlageTilbakekrevingDto klageTilbakekreving,
                                        String fritekstTilBrev) {
        super(begrunnelse);
        this.erKlagerPart = erKlagerPart;
        this.erFristOverholdt = erFristOverholdt;
        this.erKonkret = erKonkret;
        this.erSignert = erSignert;
        this.påKlagdBehandlingUuid = påKlagdBehandlingUuid;
        this.erTilbakekreving = erTilbakekreving;
        this.klageTilbakekreving = klageTilbakekreving;
        this.fritekstTilBrev = fritekstTilBrev;
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

    public String fritekstTilBrev() {
        return fritekstTilBrev;
    }


}
