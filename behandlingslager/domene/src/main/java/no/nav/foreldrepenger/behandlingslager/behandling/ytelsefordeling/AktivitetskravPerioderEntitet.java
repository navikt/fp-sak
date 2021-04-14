package no.nav.foreldrepenger.behandlingslager.behandling.ytelsefordeling;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.OneToMany;
import javax.persistence.Table;

import no.nav.foreldrepenger.behandlingslager.BaseEntitet;

@Entity
@Table(name = "YF_AKTIVITETSKRAV_PERIODER")
public class AktivitetskravPerioderEntitet extends BaseEntitet {

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "SEQ_YF_AKTIVITETSKRAV_PERIODER")
    private Long id;

    @OneToMany(mappedBy = "perioder")
    private List<AktivitetskravPeriodeEntitet> perioder = new ArrayList<>();


    public AktivitetskravPerioderEntitet() {
        //for hibernate
    }

    public AktivitetskravPerioderEntitet(AktivitetskravPerioderEntitet perioder) {
        this();
        for (var periode : perioder.getPerioder()) {
            leggTil(periode);
        }
    }

    public List<AktivitetskravPeriodeEntitet> getPerioder() {
        return Collections.unmodifiableList(perioder);
    }

    public AktivitetskravPerioderEntitet leggTil(AktivitetskravPeriodeEntitet periode) {
        validerOverlapp(periode);
        final var entitet = new AktivitetskravPeriodeEntitet(periode);
        entitet.setPerioder(this);
        this.perioder.add(entitet);
        return this;
    }

    private void validerOverlapp(AktivitetskravPeriodeEntitet periode) {
        var perioder = getPerioder();
        for (var p : perioder) {
            if (p.getTidsperiode().overlapper(periode.getTidsperiode())) {
                throw new IllegalStateException("Overlapp i periode " + p.getTidsperiode() + " - " + p.getTidsperiode());
            }
        }
    }

    @Override
    public String toString() {
        return "AktivitetskravPerioderEntitet{" + "perioder=" + perioder + '}';
    }
}
