package no.nav.foreldrepenger.dokumentbestiller.klient;

import no.nav.foreldrepenger.behandlingslager.behandling.vedtak.Vedtaksbrev;

public class TekstFraSaksbehandler {
    private Vedtaksbrev vedtaksbrev = Vedtaksbrev.UDEFINERT;
    private String overskrift;
    private String avslagarsakFritekst;
    private String fritekstbrev;

    public Vedtaksbrev getVedtaksbrev() {
        return vedtaksbrev;
    }

    public void setVedtaksbrev(Vedtaksbrev vedtaksbrev) {
        this.vedtaksbrev = vedtaksbrev;
    }

    public String getOverskrift() {
        return overskrift;
    }

    public void setOverskrift(String overskrift) {
        this.overskrift = overskrift;
    }

    public String getAvslagarsakFritekst() {
        return avslagarsakFritekst;
    }

    public void setAvslagarsakFritekst(String avslagarsakFritekst) {
        this.avslagarsakFritekst = avslagarsakFritekst;
    }

    public String getFritekstbrev() {
        return fritekstbrev;
    }

    public void setFritekstbrev(String fritekstbrev) {
        this.fritekstbrev = fritekstbrev;
    }
}
