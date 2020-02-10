package no.nav.foreldrepenger.behandlingslager.behandling.ytelsefordeling;

import java.time.LocalDate;

import javax.persistence.DiscriminatorValue;
import javax.persistence.Entity;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;

@Entity
@DiscriminatorValue("ANNEN_FORELDER_HAR_RETT")
public class PeriodeAnnenforelderHarRettEntitet extends DokumentasjonPeriodeEntitet<PeriodeAnnenforelderHarRettEntitet>  {

    @ManyToOne(optional = false)
    @JoinColumn(name = "perioder_id", nullable = false, updatable = false, unique = true)
    private PerioderAnnenforelderHarRettEntitet perioder;

    public PeriodeAnnenforelderHarRettEntitet() {
        // For hibernate
    }

    public PeriodeAnnenforelderHarRettEntitet(LocalDate fom, LocalDate tom) {
        super(fom, tom, UttakDokumentasjonType.ANNEN_FORELDER_HAR_RETT);
    }

    PeriodeAnnenforelderHarRettEntitet(PeriodeAnnenforelderHarRettEntitet periode) {
        super(periode);
    }

    void setPerioder(PerioderAnnenforelderHarRettEntitet perioder) {
        this.perioder = perioder;
    }

}
