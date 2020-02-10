package no.nav.foreldrepenger.domene.iay.modell;

import java.io.Serializable;

import javax.persistence.Embeddable;
import no.nav.foreldrepenger.behandlingslager.diff.IndexKey;
import no.nav.foreldrepenger.behandlingslager.geografisk.Landkoder;

/**
 * Hibernate entitet som modellerer en utenlandsk virksomhet.
 */
@Embeddable
public class OppgittUtenlandskVirksomhet implements IndexKey, Serializable {

    private Landkoder landkode = Landkoder.NOR;

    private String utenlandskVirksomhetNavn;

    public OppgittUtenlandskVirksomhet() {
        //hibernate
    }

    public OppgittUtenlandskVirksomhet(Landkoder landkode, String utenlandskVirksomhetNavn) {
        this.landkode = landkode == null? Landkoder.NOR : landkode;
        this.utenlandskVirksomhetNavn = utenlandskVirksomhetNavn;
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
