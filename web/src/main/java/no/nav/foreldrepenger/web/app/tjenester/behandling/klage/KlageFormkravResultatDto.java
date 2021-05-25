package no.nav.foreldrepenger.web.app.tjenester.behandling.klage;

import java.util.List;
import java.util.UUID;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.JsonProperty;

import no.nav.foreldrepenger.behandlingslager.behandling.BehandlingType;
import no.nav.foreldrepenger.behandlingslager.behandling.klage.KlageAvvistÅrsak;

@JsonAutoDetect(getterVisibility= JsonAutoDetect.Visibility.NONE, setterVisibility= JsonAutoDetect.Visibility.NONE, fieldVisibility= JsonAutoDetect.Visibility.ANY)
public class KlageFormkravResultatDto {

    @JsonProperty("paKlagdBehandlingId")
    private Long paKlagdBehandlingId;
    @JsonProperty("paKlagdBehandlingUuid")
    private UUID paKlagdBehandlingUuid;
    @JsonProperty("paklagdBehandlingType")
    private BehandlingType paklagdBehandlingType;
    @JsonProperty("begrunnelse")
    private String begrunnelse;
    @JsonProperty("erKlagerPart")
    private boolean erKlagerPart;
    @JsonProperty("erKlageKonkret")
    private boolean erKlageKonkret;
    @JsonProperty("erKlagefirstOverholdt")
    private boolean erKlagefirstOverholdt;
    @JsonProperty("erSignert")
    private boolean erSignert;
    @JsonProperty("avvistArsaker")
    private List<KlageAvvistÅrsak> avvistArsaker;


    public KlageFormkravResultatDto() {
    }

    public List<KlageAvvistÅrsak> getAvvistArsaker() {
        return avvistArsaker;
    }

    void setAvvistArsaker(List<KlageAvvistÅrsak> avvistArsaker) {
        this.avvistArsaker = avvistArsaker;
    }

    public Long getPaKlagdBehandlingId() {
        return paKlagdBehandlingId;
    }

    void setPaKlagdBehandlingId(Long paKlagdBehandlingId) {
        this.paKlagdBehandlingId = paKlagdBehandlingId;
    }

    public UUID getPaKlagdBehandlingUuid() {
        return paKlagdBehandlingUuid;
    }

    public void setPaKlagdBehandlingUuid(UUID paKlagdBehandlingUuid) {
        this.paKlagdBehandlingUuid = paKlagdBehandlingUuid;
    }

    public BehandlingType getPaklagdBehandlingType() {
        return paklagdBehandlingType;
    }

    void setPaklagdBehandlingType(BehandlingType paklagdBehandlingType) {
        this.paklagdBehandlingType = paklagdBehandlingType;
    }

    public String getBegrunnelse() {
        return begrunnelse;
    }

    void setBegrunnelse(String begrunnelse) {
        this.begrunnelse = begrunnelse;
    }

    public boolean isErKlagerPart() {
        return erKlagerPart;
    }

    void setErKlagerPart(boolean erKlagerPart) {
        this.erKlagerPart = erKlagerPart;
    }

    public boolean isErKlageKonkret() {
        return erKlageKonkret;
    }

    void setErKlageKonkret(boolean erKlageKonkret) {
        this.erKlageKonkret = erKlageKonkret;
    }

    public boolean isErKlagefirstOverholdt() {
        return erKlagefirstOverholdt;
    }

    void setErKlagefirstOverholdt(boolean erKlagefirstOverholdt) {
        this.erKlagefirstOverholdt = erKlagefirstOverholdt;
    }

    public boolean isErSignert() {
        return erSignert;
    }

    void setErSignert(boolean erSignert) {
        this.erSignert = erSignert;
    }
}
