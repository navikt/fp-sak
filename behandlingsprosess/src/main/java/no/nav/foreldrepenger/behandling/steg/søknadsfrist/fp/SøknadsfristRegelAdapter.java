package no.nav.foreldrepenger.behandling.steg.søknadsfrist.fp;

import java.util.Comparator;
import java.util.List;

import no.nav.foreldrepenger.behandlingslager.behandling.søknad.SøknadEntitet;
import no.nav.foreldrepenger.behandlingslager.behandling.ytelsefordeling.periode.OppgittPeriodeEntitet;
import no.nav.foreldrepenger.regler.soknadsfrist.SøknadsfristRegelOrkestrering;
import no.nav.foreldrepenger.regler.soknadsfrist.SøknadsfristResultat;
import no.nav.foreldrepenger.regler.soknadsfrist.grunnlag.SøknadsfristGrunnlag;

final class SøknadsfristRegelAdapter {

    static SøknadsfristResultat vurderSøknadsfristFor(SøknadEntitet søknad, List<OppgittPeriodeEntitet> oppgittePerioder) {
        var søknadsfristGrunnlag = tilGrunnlag(søknad, oppgittePerioder);
        var regelOrkestrering = new SøknadsfristRegelOrkestrering();
        return regelOrkestrering.vurderSøknadsfrist(søknadsfristGrunnlag);
    }

    private static SøknadsfristGrunnlag tilGrunnlag(SøknadEntitet søknad, List<OppgittPeriodeEntitet> oppgittePerioder) {
        if (oppgittePerioder.isEmpty()) {
            throw new IllegalArgumentException("Prøver å kjøre søknadsfristregel uten oppgitte perioder");
        }

        var førsteUttaksdato = oppgittePerioder.stream()
            .filter(p -> !p.isUtsettelse())
            .min(Comparator.comparing(OppgittPeriodeEntitet::getFom))
            .map(o -> o.getFom());

        return SøknadsfristGrunnlag.builder()
            .medSøknadMottattDato(søknad.getMottattDato())
            .medFørsteUttaksdato(førsteUttaksdato.orElse(null))
            .build();
    }
}
