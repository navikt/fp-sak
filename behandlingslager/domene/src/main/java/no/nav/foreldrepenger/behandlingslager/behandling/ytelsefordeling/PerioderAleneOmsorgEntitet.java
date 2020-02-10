package no.nav.foreldrepenger.behandlingslager.behandling.ytelsefordeling;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import javax.persistence.DiscriminatorValue;
import javax.persistence.Entity;
import javax.persistence.OneToMany;

import no.nav.foreldrepenger.behandlingslager.diff.ChangeTracked;

@Entity
@DiscriminatorValue("ALENEOMSORG")
public class PerioderAleneOmsorgEntitet extends DokumentasjonPerioderEntitet {

    @OneToMany(mappedBy = "perioder")
    @ChangeTracked
    private List<PeriodeAleneOmsorgEntitet> perioder = new ArrayList<>();


    public PerioderAleneOmsorgEntitet() {
        //for
    }

    public PerioderAleneOmsorgEntitet(PerioderAleneOmsorgEntitet perioder) {
        this();
        for (PeriodeAleneOmsorgEntitet periode : perioder.getPerioder()) {
            leggTil(periode);
        }
    }

    public List<PeriodeAleneOmsorgEntitet> getPerioder() {
        return Collections.unmodifiableList(perioder);
    }

    public void leggTil(PeriodeAleneOmsorgEntitet periode) {
        final PeriodeAleneOmsorgEntitet entitet = new PeriodeAleneOmsorgEntitet(periode);
        entitet.setPerioder(this);
        this.perioder.add(entitet);
    }

}
