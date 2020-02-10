package no.nav.foreldrepenger.behandling.klage;

import no.nav.foreldrepenger.behandlingslager.behandling.klage.KlageFormkravEntitet;
import no.nav.foreldrepenger.behandlingslager.behandling.klage.KlageVurdertAv;

public class KlageFormkravAdapter {

    private boolean erKlagerPart;
    private boolean erFristOverholdt;
    private boolean erKonkret;
    private boolean erSignert;
    private boolean gjelderVedtak;
    private String begrunnelse;
    private Long klageBehandlingId;
    private KlageVurdertAv klageVurdertAv;


    public KlageFormkravAdapter() {}

    public KlageFormkravAdapter(KlageFormkravEntitet klageFormkrav) {
        this.erKlagerPart = klageFormkrav.erKlagerPart();
        this.erFristOverholdt = klageFormkrav.erFristOverholdt();
        this.erKonkret = klageFormkrav.erKonkret();
        this.erSignert = klageFormkrav.erSignert();
        this.gjelderVedtak = klageFormkrav.hentGjelderVedtak();
        this.begrunnelse = klageFormkrav.hentBegrunnelse();
        this.klageVurdertAv = klageFormkrav.getKlageVurdertAv();
        this.klageBehandlingId = klageFormkrav.hentKlageResultat().getKlageBehandling().getId();
    }

    public void setErKlagerPart(boolean erKlagerPart) {
        this.erKlagerPart = erKlagerPart;
    }

    public void setErFristOverholdt(boolean erFristOverholdt) {
        this.erFristOverholdt = erFristOverholdt;
    }

    public void setErKonkret(boolean erKonkret) {
        this.erKonkret = erKonkret;
    }

    public void setErSignert(boolean erSignert) {
        this.erSignert = erSignert;
    }

    public void setGjelderVedtak(boolean gjelderVedtak) {
        this.gjelderVedtak = gjelderVedtak;
    }

    public void setBegrunnelse(String begrunnelse) {
        this.begrunnelse = begrunnelse;
    }

    public void setKlageBehandlingId(Long klageBehandlingId) {
        this.klageBehandlingId = klageBehandlingId;
    }

    public void setKlageVurdertAv(KlageVurdertAv klageVurdertAv) {
        this.klageVurdertAv = klageVurdertAv;
    }

    public boolean isErKlagerPart() {
        return erKlagerPart;
    }

    public boolean isErFristOverholdt() {
        return erFristOverholdt;
    }

    public boolean isErKonkret() {
        return erKonkret;
    }

    public boolean isErSignert() {
        return erSignert;
    }

    public boolean gjelderVedtak() {
        return gjelderVedtak;
    }

    public String getBegrunnelse() {
        return begrunnelse;
    }

    public Long getKlageBehandlingId() {
        return klageBehandlingId;
    }

    public KlageVurdertAv getKlageVurdertAvKode() {
        return klageVurdertAv;
    }
}
