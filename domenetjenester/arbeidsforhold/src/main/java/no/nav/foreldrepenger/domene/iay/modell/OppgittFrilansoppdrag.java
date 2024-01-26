package no.nav.foreldrepenger.domene.iay.modell;

import java.util.Objects;

import no.nav.foreldrepenger.behandlingslager.diff.ChangeTracked;
import no.nav.foreldrepenger.behandlingslager.diff.IndexKey;
import no.nav.foreldrepenger.domene.tid.DatoIntervallEntitet;

public class OppgittFrilansoppdrag implements IndexKey {

    @ChangeTracked
    private String oppdragsgiver;

    @ChangeTracked
    private DatoIntervallEntitet periode;

    public OppgittFrilansoppdrag(String oppdragsgiver, DatoIntervallEntitet periode) {
        this.oppdragsgiver = oppdragsgiver;
        this.periode = periode;
    }

    public OppgittFrilansoppdrag(OppgittFrilansoppdrag oppgittFrilansoppdrag) {
        this.oppdragsgiver = oppgittFrilansoppdrag.oppdragsgiver;
        this.periode = oppgittFrilansoppdrag.periode;
    }

    @Override
    public String getIndexKey() {
        return IndexKey.createKey(periode, oppdragsgiver);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof OppgittFrilansoppdrag that)) {
            return false;
        }
        return Objects.equals(oppdragsgiver, that.oppdragsgiver) &&
                Objects.equals(periode, that.periode);
    }

    @Override
    public int hashCode() {
        return Objects.hash(oppdragsgiver, periode);
    }

    @Override
    public String toString() {
        return "FrilansoppdragEntitet{" +
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
}
