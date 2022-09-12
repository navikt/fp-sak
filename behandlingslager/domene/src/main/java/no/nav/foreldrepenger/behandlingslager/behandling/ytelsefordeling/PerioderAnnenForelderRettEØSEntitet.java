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
@DiscriminatorValue("ANNEN_FORELDER_RETT_EOS")
public class PerioderAnnenForelderRettEØSEntitet extends DokumentasjonPerioderEntitet {

    @OneToMany(mappedBy = "perioder")
    @ChangeTracked
    private List<PeriodeAnnenForelderRettEØSEntitet> perioder = new ArrayList<>();

    public PerioderAnnenForelderRettEØSEntitet(boolean annenForelderRettEØS) {
        if (annenForelderRettEØS) {
            // Legger inn en dummy periode for å indikere saksbehandlers valg. Inntil vi faktisk har perioder her
            leggTil(new PeriodeAnnenForelderRettEØSEntitet(LocalDate.now(), LocalDate.now()));
        }
    }

    PerioderAnnenForelderRettEØSEntitet() {
        //Hibernate
    }

    public List<PeriodeAnnenForelderRettEØSEntitet> getPerioder() {
        return Collections.unmodifiableList(perioder);
    }

    private void leggTil(PeriodeAnnenForelderRettEØSEntitet periode) {
        final var entitet = new PeriodeAnnenForelderRettEØSEntitet(periode);
        entitet.setPerioder(this);
        this.perioder.add(entitet);
    }
}
