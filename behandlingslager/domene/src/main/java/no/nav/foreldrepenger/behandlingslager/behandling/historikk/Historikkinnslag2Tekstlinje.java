package no.nav.foreldrepenger.behandlingslager.behandling.historikk;

import java.util.Objects;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

import no.nav.foreldrepenger.behandlingslager.BaseEntitet;
import no.nav.foreldrepenger.behandlingslager.diff.IndexKey;

@Entity(name = "Historikkinnslag2Tekstlinje")
@Table(name = "HISTORIKKINNSLAG2_TEKSTLINJE")
public class Historikkinnslag2Tekstlinje extends BaseEntitet implements IndexKey {

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "SEQ_HISTORIKKINNSLAG2_TEKSTLINJE")
    private Long id;

    @Column(name="tekst", nullable = false)
    private String tekst;

    @Column(name="rekkefoelge_indeks", nullable = false)
    private String rekkefølgeIndeks;

    @ManyToOne(optional = false)
    @JoinColumn(name = "historikkinnslag_id", nullable = false)
    private Historikkinnslag2 historikkinnslag;

    @Override
    public String getIndexKey() {
        return IndexKey.createKey(rekkefølgeIndeks);
    }

    public Historikkinnslag2Tekstlinje(String tekst, String rekkefølgeIndeks) {
        this.tekst = tekst;
        this.rekkefølgeIndeks = rekkefølgeIndeks;
    }

    protected Historikkinnslag2Tekstlinje() {
    }

    public String getTekst() {
        return tekst;
    }

    public int getRekkefølgeIndeks() {
        return Integer.valueOf(rekkefølgeIndeks); //TODO: Sjekk om vi skal lage number i database istedenfor varchar.
    }

    void setHistorikkinnslag(Historikkinnslag2 historikkinnslag) {
        this.historikkinnslag = historikkinnslag;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o)
            return true;
        if (!(o instanceof Historikkinnslag2Tekstlinje that))
            return false;
        return Objects.equals(tekst, that.tekst) && Objects.equals(rekkefølgeIndeks, that.rekkefølgeIndeks);
    }

    @Override
    public int hashCode() {
        return Objects.hash(tekst, rekkefølgeIndeks);
    }

    @Override
    public String toString() {
        return "Historikkinnslag2Tekstlinje{" + "id=" + id + ", tekst='" + "***" + '\'' + ", rekkefølgeIndeks='" + rekkefølgeIndeks + '\'' + '}';
    }
}
