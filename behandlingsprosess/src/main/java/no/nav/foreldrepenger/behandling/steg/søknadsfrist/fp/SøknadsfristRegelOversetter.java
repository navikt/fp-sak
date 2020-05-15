package no.nav.foreldrepenger.behandling.steg.søknadsfrist.fp;

import java.time.Period;
import java.util.Comparator;
import java.util.List;

import no.nav.foreldrepenger.behandlingslager.behandling.søknad.SøknadEntitet;
import no.nav.foreldrepenger.behandlingslager.behandling.ytelsefordeling.periode.OppgittPeriodeEntitet;
import no.nav.foreldrepenger.regler.soknadsfrist.grunnlag.SøknadsfristGrunnlag;

class SøknadsfristRegelOversetter {

    private SøknadsfristRegelOversetter() {
        // For å unngå instanser
    }

    static SøknadsfristGrunnlag tilGrunnlag(SøknadEntitet søknad, List<OppgittPeriodeEntitet> oppgittePerioder, Period søknadsfristLengde) {
        var førsteUttaksdato = oppgittePerioder.stream()
            .sorted(Comparator.comparing(OppgittPeriodeEntitet::getFom))
            .map(o -> o.getFom())
            .findFirst().orElseThrow(() -> new IllegalStateException("Prøver å kjøre søknadsfristregel uten oppgitte perioder"));

        return SøknadsfristGrunnlag.builder()
            .medSøknadMottattDato(søknad.getMottattDato())
            .medFørsteUttaksdato(førsteUttaksdato)
            .medAntallMånederSøknadsfrist(søknadsfristLengde.getMonths())
            .build();
    }
}
