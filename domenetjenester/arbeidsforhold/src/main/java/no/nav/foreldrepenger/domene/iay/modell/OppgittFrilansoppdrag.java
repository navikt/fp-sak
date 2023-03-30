package no.nav.foreldrepenger.domene.iay.modell;

import java.util.Objects;

import no.nav.foreldrepenger.behandlingslager.BaseEntitet;
import no.nav.foreldrepenger.behandlingslager.diff.ChangeTracked;
import no.nav.foreldrepenger.behandlingslager.diff.IndexKey;
import no.nav.foreldrepenger.domene.tid.DatoIntervallEntitet;

public class OppgittFrilansoppdrag extends BaseEntitet implements IndexKey {

    private OppgittFrilans frilans;

    @ChangeTracked
    private String oppdragsgiver;

    @ChangeTracked
    private DatoIntervallEntitet periode;

    OppgittFrilansoppdrag() {
    }

    public OppgittFrilansoppdrag(String oppdragsgiver, DatoIntervallEntitet periode) {
        this.oppdragsgiver = oppdragsgiver;
        this.periode = periode;
    }

    @Override
    public String getIndexKey() {
        return IndexKey.createKey(periode, oppdragsgiver);
    }

    void setOppgittOpptjening(OppgittFrilans frilans) {
        this.frilans = frilans;
    }

    void setPeriode(DatoIntervallEntitet periode) {
        this.periode = periode;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if ((o == null) || !(o instanceof OppgittFrilansoppdrag that)) {
            return false;
        }
        return Objects.equals(frilans, that.frilans) &&
                Objects.equals(oppdragsgiver, that.oppdragsgiver) &&
                Objects.equals(periode, that.periode);
    }

    @Override
    public int hashCode() {
        return Objects.hash(frilans, oppdragsgiver, periode);
    }

    @Override
    public String toString() {
        return "FrilansoppdragEntitet{" +
                "frilans=" + frilans +
                ", oppdragsgiver='" + oppdragsgiver + '\'' +
                ", periode=" + periode +
                '}';
    }

    public DatoIntervallEntitet getPeriode() {
        return periode;
    }

    public String getOppdragsgiver() {
        return oppdragsgiver;
    }

    // FIXME (OJR) kan ikke ha mutators
    public void setFrilans(OppgittFrilans frilans) {
        this.frilans = frilans;
    }
}
