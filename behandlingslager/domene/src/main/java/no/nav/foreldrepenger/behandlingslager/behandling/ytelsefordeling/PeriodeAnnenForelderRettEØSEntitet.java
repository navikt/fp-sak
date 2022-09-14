package no.nav.foreldrepenger.behandlingslager.behandling.ytelsefordeling;

import java.time.LocalDate;

import javax.persistence.DiscriminatorValue;
import javax.persistence.Entity;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;

@Entity
@DiscriminatorValue("ANNEN_FORELDER_RETT_EOS")
public class PeriodeAnnenForelderRettEØSEntitet extends DokumentasjonPeriodeEntitet<PeriodeAnnenForelderRettEØSEntitet>  {

    @ManyToOne(optional = false)
    @JoinColumn(name = "perioder_id", nullable = false, updatable = false, unique = true)
    private PerioderAnnenForelderRettEØSEntitet perioder;

    public PeriodeAnnenForelderRettEØSEntitet() {
        // For hibernate
    }

    public PeriodeAnnenForelderRettEØSEntitet(LocalDate fom, LocalDate tom) {
        super(fom, tom, UttakDokumentasjonType.ANNEN_FORELDER_RETT_EOS);
    }

    PeriodeAnnenForelderRettEØSEntitet(PeriodeAnnenForelderRettEØSEntitet periode) {
        super(periode);
    }

    void setPerioder(PerioderAnnenForelderRettEØSEntitet perioder) {
        this.perioder = perioder;
    }

}
