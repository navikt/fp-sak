package no.nav.foreldrepenger.behandlingslager.behandling.ytelsefordeling;

import java.time.LocalDate;

import javax.persistence.DiscriminatorValue;
import javax.persistence.Entity;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;

@Entity
@DiscriminatorValue("MOR_STONAD_EOS")
public class PeriodeMorStønadEØSEntitet extends DokumentasjonPeriodeEntitet<PeriodeMorStønadEØSEntitet>  {

    @ManyToOne(optional = false)
    @JoinColumn(name = "perioder_id", nullable = false, updatable = false, unique = true)
    private PerioderMorStønadEØSEntitet perioder;

    public PeriodeMorStønadEØSEntitet() {
        // For hibernate
    }

    public PeriodeMorStønadEØSEntitet(LocalDate fom, LocalDate tom) {
        super(fom, tom, UttakDokumentasjonType.MOR_STØNAD_EØS);
    }

    PeriodeMorStønadEØSEntitet(PeriodeMorStønadEØSEntitet periode) {
        super(periode);
    }

    void setPerioder(PerioderMorStønadEØSEntitet perioder) {
        this.perioder = perioder;
    }

}
