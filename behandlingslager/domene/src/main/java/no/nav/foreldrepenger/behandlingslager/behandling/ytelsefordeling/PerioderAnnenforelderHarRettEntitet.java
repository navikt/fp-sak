package no.nav.foreldrepenger.behandlingslager.behandling.ytelsefordeling;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import javax.persistence.DiscriminatorValue;
import javax.persistence.Entity;
import javax.persistence.OneToMany;

import no.nav.foreldrepenger.behandlingslager.diff.ChangeTracked;

@Entity
@DiscriminatorValue("ANNEN_FORELDER_HAR_RETT")
public class PerioderAnnenforelderHarRettEntitet extends DokumentasjonPerioderEntitet {

    @OneToMany(mappedBy = "perioder")
    @ChangeTracked
    private List<PeriodeAnnenforelderHarRettEntitet> perioder = new ArrayList<>();

    public PerioderAnnenforelderHarRettEntitet() {
    }

    public PerioderAnnenforelderHarRettEntitet(PerioderAnnenforelderHarRettEntitet perioder) {
        this();
        for (PeriodeAnnenforelderHarRettEntitet periode : perioder.getPerioder()) {
            leggTil(periode);
        }
    }

    public List<PeriodeAnnenforelderHarRettEntitet> getPerioder() {
        return Collections.unmodifiableList(perioder);
    }

    public void leggTil(PeriodeAnnenforelderHarRettEntitet periode) {
        final PeriodeAnnenforelderHarRettEntitet entitet = new PeriodeAnnenforelderHarRettEntitet(periode);
        entitet.setPerioder(this);
        this.perioder.add(entitet);
    }
}
