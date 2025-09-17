package no.nav.foreldrepenger.web.app.tjenester.behandling.klage.aksjonspunkt;

import java.time.LocalDate;
import java.util.UUID;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.JsonProperty;

import no.nav.vedtak.util.InputValideringRegex;
@JsonAutoDetect(getterVisibility= JsonAutoDetect.Visibility.NONE, setterVisibility= JsonAutoDetect.Visibility.NONE, fieldVisibility= JsonAutoDetect.Visibility.ANY)
public class KlageFormKravAksjonspunktMellomlagringDto implements KlageFormKravLagreDto {
    @Pattern(regexp = InputValideringRegex.KODEVERK)
    private String kode;
    @Valid
    @NotNull
    private UUID behandlingUuid;
    private boolean erKlagerPart;
    private boolean erFristOverholdt;
    private boolean erKonkret;
    private boolean erSignert;
    private boolean erTilbakekreving;
    @Valid
    private KlageTilbakekrevingDto klageTilbakekreving;
    private LocalDate mottattDato;
    @JsonProperty("paKlagdBehandlingUuid")
    private UUID paKlagdBehandlingUuid;
    @Size(max = 2000)
    @Pattern(regexp = InputValideringRegex.FRITEKST)
    private String begrunnelse;
    @Size(max = 100000)
    @Pattern(regexp = InputValideringRegex.FRITEKST)
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
                                                     String fritekstTilBrev,
                                                     LocalDate mottattDato) {
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
        this.mottattDato = mottattDato;
    }

    public String getKode() {
        return kode;
    }

    public UUID behandlingUuid() {
        return behandlingUuid;
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

    public UUID paKlagdBehandlingUuid() {
        return paKlagdBehandlingUuid;
    }

    @Override
    public UUID påKlagdBehandlingUuid() {
        return paKlagdBehandlingUuid;
    }
    @Override
    public boolean erTilbakekreving() {
        return erTilbakekreving;
    }
    @Override
    public KlageTilbakekrevingDto klageTilbakekreving() {
        return klageTilbakekreving;
    }
    @Override
    public LocalDate mottattDato() {
        return mottattDato;
    }

    public String fritekstTilBrev() {
        return fritekstTilBrev;
    }

    public String begrunnelse() {
        return begrunnelse;
    }
}
