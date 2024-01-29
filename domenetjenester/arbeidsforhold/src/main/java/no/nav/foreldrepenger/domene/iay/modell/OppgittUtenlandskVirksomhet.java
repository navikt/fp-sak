package no.nav.foreldrepenger.domene.iay.modell;

import java.io.Serializable;

import no.nav.foreldrepenger.behandlingslager.diff.ChangeTracked;
import no.nav.foreldrepenger.behandlingslager.diff.IndexKey;
import no.nav.foreldrepenger.behandlingslager.geografisk.Landkoder;

public class OppgittUtenlandskVirksomhet implements IndexKey, Serializable {

    @ChangeTracked
    private Landkoder landkode = Landkoder.NOR;
    @ChangeTracked
    private String utenlandskVirksomhetNavn;

    public OppgittUtenlandskVirksomhet() {
        // hibernate
    }

    public OppgittUtenlandskVirksomhet(Landkoder landkode, String utenlandskVirksomhetNavn) {
        this.landkode = landkode == null ? Landkoder.NOR : landkode;
        this.utenlandskVirksomhetNavn = utenlandskVirksomhetNavn;
    }

    public OppgittUtenlandskVirksomhet(OppgittUtenlandskVirksomhet utenlandskVirksomhet) {
        this.landkode = utenlandskVirksomhet.landkode;
        this.utenlandskVirksomhetNavn = utenlandskVirksomhet.utenlandskVirksomhetNavn;
    }

    @Override
    public String getIndexKey() {
        return IndexKey.createKey(utenlandskVirksomhetNavn, landkode);
    }

    public Landkoder getLandkode() {
        return landkode;
    }

    public String getNavn() {
        return utenlandskVirksomhetNavn;
    }
}
