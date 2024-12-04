package no.nav.foreldrepenger.behandlingslager.behandling.historikk;

import java.util.Objects;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

import no.nav.foreldrepenger.behandlingslager.BaseEntitet;
import no.nav.foreldrepenger.behandlingslager.diff.IndexKey;

@Entity(name = "Historikkinnslag2Linje")
@Table(name = "HISTORIKKINNSLAG2_LINJE")
public class Historikkinnslag2Linje extends BaseEntitet implements IndexKey {

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "SEQ_HISTORIKKINNSLAG2_LINJE")
    private Long id;

    @Column(name = "tekst", nullable = false)
    private String tekst;

    @Column(name = "SEKVENS_NR", nullable = false)
    private int sekvensNr;

    @Column(name = "type", nullable = false)
    @Enumerated(EnumType.STRING)
    private HistorikkinnslagLinjeType type;

    @ManyToOne(optional = false)
    @JoinColumn(name = "historikkinnslag_id", nullable = false)
    private Historikkinnslag2 historikkinnslag;

    @Override
    public String getIndexKey() {
        return IndexKey.createKey(sekvensNr);
    }

    public Historikkinnslag2Linje(String tekst, int sekvensNr, HistorikkinnslagLinjeType type) {
        this.tekst = tekst;
        this.sekvensNr = sekvensNr;
        this.type = type;
    }

    protected Historikkinnslag2Linje() {
    }

    public String getTekst() {
        return tekst;
    }

    public HistorikkinnslagLinjeType getType() {
        return type;
    }

    public int getSekvensNr() {
        return sekvensNr;
    }

    void setHistorikkinnslag(Historikkinnslag2 historikkinnslag) {
        this.historikkinnslag = historikkinnslag;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof Historikkinnslag2Linje that)) {
            return false;
        }
        return sekvensNr == that.sekvensNr && Objects.equals(tekst, that.tekst) && type == that.type;
    }

    @Override
    public int hashCode() {
        return Objects.hash(tekst, sekvensNr, type);
    }

    @Override
    public String toString() {
        //tekst kan være fritekst fra saksbehandler
        return "Historikkinnslag2Linje{" + "tekst='***" + '\'' + ", sekvensNr=" + sekvensNr + ", type=" + type + '}';
    }
}
