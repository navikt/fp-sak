package no.nav.foreldrepenger.domene.iay.modell;

import com.fasterxml.jackson.annotation.JsonBackReference;
import no.nav.foreldrepenger.behandlingslager.BaseEntitet;
import no.nav.foreldrepenger.behandlingslager.diff.IndexKey;
import no.nav.foreldrepenger.domene.tid.DatoIntervallEntitet;

import java.util.Objects;

public class ArbeidsforholdOverstyrtePerioder extends BaseEntitet implements IndexKey {

    private DatoIntervallEntitet periode;

    @JsonBackReference
    private ArbeidsforholdOverstyring arbeidsforholdOverstyring;

    ArbeidsforholdOverstyrtePerioder() {

    }

    ArbeidsforholdOverstyrtePerioder(ArbeidsforholdOverstyrtePerioder arbeidsforholdOverstyrtePerioder) {
        this.periode = arbeidsforholdOverstyrtePerioder.getOverstyrtePeriode();
    }

    @Override
    public String getIndexKey() {
        return IndexKey.createKey(getOverstyrtePeriode());
    }

    void setPeriode(DatoIntervallEntitet periode) {
        this.periode = periode;
    }

    public ArbeidsforholdOverstyring getArbeidsforholdOverstyring() {
        return arbeidsforholdOverstyring;
    }

    void setArbeidsforholdOverstyring(ArbeidsforholdOverstyring arbeidsforholdOverstyring) {
        this.arbeidsforholdOverstyring = arbeidsforholdOverstyring;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof ArbeidsforholdOverstyrtePerioder that)) {
            return false;
        }
        return Objects.equals(periode, that.periode) && Objects.equals(arbeidsforholdOverstyring, that.arbeidsforholdOverstyring);
    }

    @Override
    public int hashCode() {
        return Objects.hash(periode, arbeidsforholdOverstyring);
    }

    @Override
    public String toString() {
        return "ArbeidsforholdInformasjonEntitet{" +
                "periode=" + periode +
                ", arbeidsforholdOverstyring=" + arbeidsforholdOverstyring +
                '}';
    }

    public DatoIntervallEntitet getOverstyrtePeriode() {
        return periode;
    }
}
