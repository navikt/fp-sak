package no.nav.foreldrepenger.web.app.tjenester.behandling.klage.aksjonspunkt;


import java.util.UUID;

import javax.validation.Valid;
import javax.validation.constraints.NotNull;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonTypeName;

import no.nav.foreldrepenger.behandling.aksjonspunkt.BekreftetAksjonspunktDto;
import no.nav.foreldrepenger.behandlingslager.behandling.aksjonspunkt.AksjonspunktKodeDefinisjon;

@JsonAutoDetect(getterVisibility = JsonAutoDetect.Visibility.NONE, setterVisibility = JsonAutoDetect.Visibility.NONE, fieldVisibility = JsonAutoDetect.Visibility.ANY)
public abstract class KlageFormkravAksjonspunktDto extends BekreftetAksjonspunktDto {

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

    @JsonProperty("vedtak")
    private Long påKlagdBehandlingId;

    @JsonProperty("vedtakBehandlingUuid")
    @Valid
    private UUID påKlagdBehandlingUuid;


    KlageFormkravAksjonspunktDto() { // NOSONAR
        // For Jackson
    }

    public KlageFormkravAksjonspunktDto(boolean erKlagerPart,
                                        boolean erFristOverholdt,
                                        boolean erKonkret,
                                        boolean erSignert,
                                        Long påKlagdBehandlingId,
                                        UUID påKlagdBehandlingUuid,
                                        String begrunnelse,
                                        boolean erTilbakekreving,
                                        KlageTilbakekrevingDto klageTilbakekreving) {
        super(begrunnelse);
        this.erKlagerPart = erKlagerPart;
        this.erFristOverholdt = erFristOverholdt;
        this.erKonkret = erKonkret;
        this.erSignert = erSignert;
        this.påKlagdBehandlingId = påKlagdBehandlingId;
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

    public Long hentPåKlagdBehandlingId() {
        return påKlagdBehandlingId;
    }

    public UUID hentPåKlagdBehandlingUuid() {
        return påKlagdBehandlingUuid;
    }

    public UUID hentpåKlagdEksternBehandlingUuId() {
        return erTilbakekreving
            && klageTilbakekreving != null ? klageTilbakekreving.getPåklagdEksternBehandlingUuid() : null;
    }

    public boolean erTilbakekreving() {
        return erTilbakekreving;
    }

    @JsonProperty("tilbakekrevingInfo")
    public KlageTilbakekrevingDto getKlageTilbakekreving() {
        return klageTilbakekreving;
    }

    @JsonTypeName(AksjonspunktKodeDefinisjon.VURDERING_AV_FORMKRAV_KLAGE_NFP_KODE)
    public static class KlageFormkravNfpAksjonspunktDto extends KlageFormkravAksjonspunktDto {


        KlageFormkravNfpAksjonspunktDto() {
            super();
        }

        public KlageFormkravNfpAksjonspunktDto(boolean erKlagerPart,
                                               boolean erFristOverholdt,
                                               boolean erKonkret,
                                               boolean erSignert,
                                               Long vedtakId,
                                               UUID vedtakBehandlingUuid,
                                               String begrunnelse,
                                               boolean erTilbakekreving,
                                               KlageTilbakekrevingDto klageTilbakekreving) {
            super(erKlagerPart, erFristOverholdt, erKonkret, erSignert, vedtakId, vedtakBehandlingUuid, begrunnelse, erTilbakekreving,
                klageTilbakekreving);
        }
    }

    @JsonTypeName(AksjonspunktKodeDefinisjon.VURDERING_AV_FORMKRAV_KLAGE_KA_KODE)
    public static class KlageFormkravKaAksjonspunktDto extends KlageFormkravAksjonspunktDto {


        KlageFormkravKaAksjonspunktDto() {
            super();
        }

        public KlageFormkravKaAksjonspunktDto(boolean erKlagerPart,
                                              boolean erFristOverholdt,
                                              boolean erKonkret,
                                              boolean erSignert,
                                              Long vedtakId,
                                              UUID vedtakBehandlingUuid,
                                              String begrunnelse,
                                              boolean erTilbakekreving,
                                              KlageTilbakekrevingDto klageTilbakekreving) {
            super(erKlagerPart, erFristOverholdt, erKonkret, erSignert, vedtakId, vedtakBehandlingUuid, begrunnelse, erTilbakekreving,
                klageTilbakekreving);
        }
    }

}
