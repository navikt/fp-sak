package no.nav.foreldrepenger.behandling.steg.søknadsfrist.fp;

import java.time.Period;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

import no.nav.foreldrepenger.behandlingslager.behandling.søknad.SøknadEntitet;
import no.nav.foreldrepenger.behandlingslager.behandling.ytelsefordeling.periode.OppgittPeriodeEntitet;
import no.nav.foreldrepenger.regler.soknadsfrist.grunnlag.SøknadsfristGrunnlag;

class SøknadsfristRegelOversetter {

    private SøknadsfristRegelOversetter() {
        // For å unngå instanser
    }

    static SøknadsfristGrunnlag tilGrunnlag(SøknadEntitet søknad, List<OppgittPeriodeEntitet> oppgittePerioder, Period søknadsfristLengde) {
        List<OppgittPeriodeEntitet> uttaksperioder = oppgittePerioder.stream()
            .sorted(Comparator.comparing(OppgittPeriodeEntitet::getFom))
            .collect(Collectors.toList());

        SøknadsfristGrunnlag grunnlag = SøknadsfristGrunnlag.builder()
            .medSøknadMottattDato(søknad.getMottattDato())
            .medErSøknadOmUttak(!uttaksperioder.isEmpty())
            .medFørsteUttaksdato(uttaksperioder.isEmpty() ? null : uttaksperioder.get(0).getFom())
            .medAntallMånederSøknadsfrist(søknadsfristLengde.getMonths()) // TODO: broken når ikke heltall måneder, må rette opp regler den dagen det ikke matcher heltall måneder
            .build();

        return grunnlag;
    }
}
