package no.nav.foreldrepenger.web.app.tjenester.behandling.klage.aksjonspunkt;


import java.time.LocalDate;
import java.util.UUID;

import jakarta.validation.Valid;
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
public final class KlageFormkravAksjonspunktDto extends BekreftetAksjonspunktDto implements KlageFormKravLagreDto {

    private boolean erKlagerPart;
    private boolean erFristOverholdt;
    private boolean erKonkret;
    private boolean erSignert;

    private boolean erTilbakekreving;

    @Valid
    private KlageTilbakekrevingDto klageTilbakekreving;

    @JsonProperty("vedtakBehandlingUuid")
    @Valid
    private UUID påKlagdBehandlingUuid;

    @Size(max = 100000)
    @Pattern(regexp = InputValideringRegex.FRITEKST)
    private String fritekstTilBrev;

    private LocalDate mottattDato;


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
                                        String fritekstTilBrev,
                                        LocalDate mottattDato) {
        super(begrunnelse);
        this.erKlagerPart = erKlagerPart;
        this.erFristOverholdt = erFristOverholdt;
        this.erKonkret = erKonkret;
        this.erSignert = erSignert;
        this.påKlagdBehandlingUuid = påKlagdBehandlingUuid;
        this.erTilbakekreving = erTilbakekreving;
        this.klageTilbakekreving = klageTilbakekreving;
        this.fritekstTilBrev = fritekstTilBrev;
        this.mottattDato = mottattDato;
    }

    @Override
    public boolean erKlagerPart() {
        return erKlagerPart;
    }
    @Override
    public boolean erFristOverholdt() {
        return erFristOverholdt;
    }
    @Override
    public boolean erKonkret() {
        return erKonkret;
    }
    @Override
    public boolean erSignert() {
        return erSignert;
    }

    public UUID påKlagdBehandlingUuid() {
        return påKlagdBehandlingUuid;
    }

    @Override
    public boolean erTilbakekreving() {
        return erTilbakekreving;
    }

    @Override
    public LocalDate mottattDato() {
        return mottattDato;
    }


    @Override
    @JsonProperty("tilbakekrevingInfo")
    public KlageTilbakekrevingDto klageTilbakekreving() {
        return klageTilbakekreving;
    }

    public String fritekstTilBrev() {
        return fritekstTilBrev;
    }


}
