package no.nav.foreldrepenger.web.app.tjenester.behandling.klage.aksjonspunkt;


import javax.validation.constraints.NotNull;
import javax.validation.constraints.Pattern;
import javax.validation.constraints.Size;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonTypeName;

import no.nav.foreldrepenger.behandling.aksjonspunkt.BekreftetAksjonspunktDto;
import no.nav.foreldrepenger.behandlingslager.behandling.aksjonspunkt.AksjonspunktKodeDefinisjon;
import no.nav.vedtak.util.InputValideringRegex;

public abstract  class KlageFormkravAksjonspunktDto extends BekreftetAksjonspunktDto {


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

    @JsonProperty("vedtak")
    // TODO (BehandlingIdDto): bør kunne støtte behandlingUuid også?  Hvorfor heter property "vedtak"?
    private Long påKlagdBehandlingId;

    @Size(max = 2000)
    @Pattern(regexp = InputValideringRegex.FRITEKST)
    private String begrunnelse;

    KlageFormkravAksjonspunktDto() { // NOSONAR
        // For Jackson
    }

    public KlageFormkravAksjonspunktDto(boolean erKlagerPart, boolean erFristOverholdt, boolean erKonkret, boolean erSignert, Long påKlagdBehandlingId, String begrunnelse) {
        super(begrunnelse);
        this.erKlagerPart = erKlagerPart;
        this.erFristOverholdt = erFristOverholdt;
        this.erKonkret = erKonkret;
        this.erSignert = erSignert;
        this.påKlagdBehandlingId = påKlagdBehandlingId;
        this.begrunnelse = begrunnelse;
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

    public Long hentpåKlagdBehandlingId() {
        return påKlagdBehandlingId;
    }

    @Override
    public String getBegrunnelse() {
        return begrunnelse;
    }

    @JsonTypeName(AksjonspunktKodeDefinisjon.VURDERING_AV_FORMKRAV_KLAGE_NFP_KODE)
    public static class KlageFormkravNfpAksjonspunktDto extends KlageFormkravAksjonspunktDto {


        KlageFormkravNfpAksjonspunktDto() {
            super();
        }

        public KlageFormkravNfpAksjonspunktDto(boolean erKlagerPart,
                                               boolean erFristOverholdt, boolean erKonkret,
                                               boolean erSignert, Long vedtakId,
                                               String begrunnelse ) {
            super(erKlagerPart, erFristOverholdt, erKonkret, erSignert, vedtakId, begrunnelse);
        }



    }

    @JsonTypeName(AksjonspunktKodeDefinisjon.VURDERING_AV_FORMKRAV_KLAGE_KA_KODE)
    public static class KlageFormkravKaAksjonspunktDto extends KlageFormkravAksjonspunktDto {


        KlageFormkravKaAksjonspunktDto() {
            super();
        }

        public KlageFormkravKaAksjonspunktDto(boolean erKlagerPart,
                                               boolean erFristOverholdt, boolean erKonkret,
                                               boolean erSignert, Long vedtakId,
                                               String begrunnelse ) {
            super(erKlagerPart, erFristOverholdt, erKonkret, erSignert, vedtakId, begrunnelse);
        }



    }
}
