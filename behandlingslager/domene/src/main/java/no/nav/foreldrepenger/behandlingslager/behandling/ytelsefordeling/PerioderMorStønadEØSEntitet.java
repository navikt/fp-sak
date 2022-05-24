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
@DiscriminatorValue("MOR_STONAD_EOS")
public class PerioderMorStønadEØSEntitet extends DokumentasjonPerioderEntitet {

    @OneToMany(mappedBy = "perioder")
    @ChangeTracked
    private List<PeriodeMorStønadEØSEntitet> perioder = new ArrayList<>();

    public PerioderMorStønadEØSEntitet(boolean morStønadEØS) {
        if (morStønadEØS) {
            // Legger inn en dummy periode for å indikere saksbehandlers valg. Inntil vi faktisk har perioder her
            leggTil(new PeriodeMorStønadEØSEntitet(LocalDate.now(), LocalDate.now()));
        }
    }

    PerioderMorStønadEØSEntitet() {
        //Hibernate
    }

    public List<PeriodeMorStønadEØSEntitet> getPerioder() {
        return Collections.unmodifiableList(perioder);
    }

    private void leggTil(PeriodeMorStønadEØSEntitet periode) {
        final var entitet = new PeriodeMorStønadEØSEntitet(periode);
        entitet.setPerioder(this);
        this.perioder.add(entitet);
    }
}
