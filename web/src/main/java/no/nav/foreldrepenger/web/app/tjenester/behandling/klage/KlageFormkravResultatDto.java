package no.nav.foreldrepenger.web.app.tjenester.behandling.klage;

import java.util.List;

import no.nav.foreldrepenger.behandlingslager.behandling.BehandlingType;
import no.nav.foreldrepenger.behandlingslager.behandling.klage.KlageAvvistÅrsak;

public class KlageFormkravResultatDto {
    private Long paKlagdBehandlingId;
    private BehandlingType paklagdBehandlingType;
    private String begrunnelse;
    private boolean erKlagerPart;
    private boolean erKlageKonkret;
    private boolean erKlagefirstOverholdt;
    private boolean erSignert;
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
