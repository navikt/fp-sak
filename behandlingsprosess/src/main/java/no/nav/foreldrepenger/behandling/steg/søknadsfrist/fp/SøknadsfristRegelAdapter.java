package no.nav.foreldrepenger.behandling.steg.søknadsfrist.fp;

import java.time.LocalDate;
import java.util.Comparator;
import java.util.List;

import no.nav.foreldrepenger.behandlingslager.behandling.søknad.SøknadEntitet;
import no.nav.foreldrepenger.behandlingslager.behandling.ytelsefordeling.periode.OppgittPeriodeEntitet;
import no.nav.foreldrepenger.regler.SøknadsfristUtil;
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

        var søknadMottattDato = søknad.getMottattDato();

        var førsteUttaksdato = oppgittePerioder.stream()
            .filter(p -> !p.isUtsettelse())
            .filter(p -> periodeSkalVurderes(p, søknadMottattDato))
            .min(Comparator.comparing(OppgittPeriodeEntitet::getFom))
            .map(o -> o.getFom());

        return SøknadsfristGrunnlag.builder()
            .søknadMottattDato(søknadMottattDato)
            .førsteUttaksdato(førsteUttaksdato.orElse(null))
            .build();
    }

    private static boolean periodeSkalVurderes(OppgittPeriodeEntitet periode, LocalDate søknadMottattDato) {
        // Mangler tidligst mottatt dato, eller tidligst mottatt er >= søknadMottatt/periodeMottatt
        if (periode.getTidligstMottattDato().isEmpty() || periode.getMottattDato() == null ||
            periode.getTidligstMottattDato().filter(tmd -> tmd.isBefore(søknadMottattDato)).isEmpty() ||
            periode.getTidligstMottattDato().filter(tmd -> tmd.isBefore(periode.getMottattDato())).isEmpty()) {
            return true;
        }
        // Det skal finnes en tidligst mottatt dato og den skal være før mottatt dato. Perioden har vært behandlet i tidligere behandling
        var tidligstedato = periode.getTidligstMottattDato().map(SøknadsfristUtil::finnFørsteLoveligeUttaksdag).orElseThrow();
        // Sjekk perioder med for tidlig fom - de kan ha blitt underkjent i tidligere behandling (frist eller uttaksregler).
        return periode.getFom().isBefore(tidligstedato);
    }
}
