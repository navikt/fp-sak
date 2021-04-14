package no.nav.foreldrepenger.behandlingslager.behandling.ytelsefordeling;

import java.time.LocalDate;
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

    public PerioderAnnenforelderHarRettEntitet(boolean harRett) {
        if (harRett) {
            // Legger inn en dummy periode for å indikere saksbehandlers valg. Inntil vi faktisk har perioder her
            leggTil(new PeriodeAnnenforelderHarRettEntitet(LocalDate.now(), LocalDate.now()));
        }
    }

    PerioderAnnenforelderHarRettEntitet() {
        //Hibernate
    }

    public List<PeriodeAnnenforelderHarRettEntitet> getPerioder() {
        return Collections.unmodifiableList(perioder);
    }

    private void leggTil(PeriodeAnnenforelderHarRettEntitet periode) {
        final var entitet = new PeriodeAnnenforelderHarRettEntitet(periode);
        entitet.setPerioder(this);
        this.perioder.add(entitet);
    }
}
