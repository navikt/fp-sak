package no.nav.foreldrepenger.domene.iay.modell;

import java.util.Objects;

import javax.persistence.Convert;

import no.nav.foreldrepenger.behandlingslager.BaseEntitet;
import no.nav.foreldrepenger.behandlingslager.diff.ChangeTracked;
import no.nav.foreldrepenger.behandlingslager.diff.IndexKey;
import no.nav.foreldrepenger.behandlingslager.virksomhet.ArbeidType;
import no.nav.foreldrepenger.domene.tid.DatoIntervallEntitet;


public class OppgittAnnenAktivitet extends BaseEntitet implements IndexKey {

    @ChangeTracked
    DatoIntervallEntitet periode;

    @Convert(converter = ArbeidType.KodeverdiConverter.class)
    @ChangeTracked
    private ArbeidType arbeidType;

    public OppgittAnnenAktivitet(DatoIntervallEntitet periode, ArbeidType arbeidType) {
        this.periode = periode;
        this.arbeidType = arbeidType;
    }

    public OppgittAnnenAktivitet() {
        // hibernate
    }

    @Override
    public String getIndexKey() {
        return IndexKey.createKey(periode, arbeidType);
    }

    public ArbeidType getArbeidType() {
        return arbeidType;
    }

    public DatoIntervallEntitet getPeriode() {
        return periode;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || !(o instanceof OppgittAnnenAktivitet)) return false;
        OppgittAnnenAktivitet that = (OppgittAnnenAktivitet) o;
        return Objects.equals(periode, that.periode) &&
                Objects.equals(arbeidType, that.arbeidType);
    }

    @Override
    public int hashCode() {
        return Objects.hash(periode, arbeidType);
    }

    @Override
    public String toString() {
        return "AnnenAktivitetEntitet{" +
                "periode=" + periode +
                ", arbeidType=" + arbeidType +
                '}';
    }
}
