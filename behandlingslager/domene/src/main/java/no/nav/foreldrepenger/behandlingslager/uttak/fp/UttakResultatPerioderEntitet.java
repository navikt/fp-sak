package no.nav.foreldrepenger.behandlingslager.uttak.fp;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.OneToMany;
import javax.persistence.Table;

import org.hibernate.annotations.BatchSize;

import no.nav.foreldrepenger.behandlingslager.BaseEntitet;

@Entity
@Table(name = "UTTAK_RESULTAT_PERIODER")
public class UttakResultatPerioderEntitet extends BaseEntitet {
    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "SEQ_UTTAK_RESULTAT_PERIODER")
    private Long id;

    @OneToMany(mappedBy = "perioder")
    @BatchSize(size = 25)
    private List<UttakResultatPeriodeEntitet> perioder = new ArrayList<>();

    public UttakResultatPerioderEntitet leggTilPeriode(UttakResultatPeriodeEntitet periode) {
        validerIkkeOverlapp(periode);
        perioder.add(periode);
        periode.setPerioder(this);
        return this;
    }

    private void validerIkkeOverlapp(UttakResultatPeriodeEntitet p2) {
        for (var p1 : perioder) {
            if (p1.getTidsperiode().overlapper(p2.getTidsperiode())) {
                throw new IllegalArgumentException("UttakResultatPerioder kan ikke overlappe " + p2 + p1);
            }
        }
    }

    public List<UttakResultatPeriodeEntitet> getPerioder() {
        return perioder.stream().sorted(Comparator.comparing(UttakResultatPeriodeEntitet::getFom)).toList();
    }
}
