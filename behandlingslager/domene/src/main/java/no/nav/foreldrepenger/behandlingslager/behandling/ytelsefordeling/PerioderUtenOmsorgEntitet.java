package no.nav.foreldrepenger.behandlingslager.behandling.ytelsefordeling;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import javax.persistence.DiscriminatorValue;
import javax.persistence.Entity;
import javax.persistence.OneToMany;

import no.nav.foreldrepenger.behandlingslager.diff.ChangeTracked;

@Entity
@DiscriminatorValue("UTEN_OMSORG")
public class PerioderUtenOmsorgEntitet extends DokumentasjonPerioderEntitet {

    @OneToMany(mappedBy = "perioder")
    @ChangeTracked
    private List<PeriodeUtenOmsorgEntitet> perioder = new ArrayList<>();


    public PerioderUtenOmsorgEntitet() {
        //for
    }

    public PerioderUtenOmsorgEntitet(PerioderUtenOmsorgEntitet perioder) {
        this();
        for (PeriodeUtenOmsorgEntitet periode : perioder.getPerioder()) {
            leggTil(periode);
        }
    }

    public List<PeriodeUtenOmsorgEntitet> getPerioder() {
        return Collections.unmodifiableList(perioder);
    }

    public void leggTil(PeriodeUtenOmsorgEntitet periode) {
        final PeriodeUtenOmsorgEntitet entitet = new PeriodeUtenOmsorgEntitet(periode);
        entitet.setPerioder(this);
        this.perioder.add(entitet);
    }

}
