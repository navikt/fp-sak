package no.nav.foreldrepenger.domene.uttak.uttaksgrunnlag.fp;

import java.time.LocalDate;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import no.nav.foreldrepenger.behandlingslager.behandling.ytelsefordeling.periode.OppgittPeriodeEntitet;
import no.nav.foreldrepenger.behandlingslager.behandling.ytelsefordeling.årsak.Årsak;

class OppgittPeriodeUtil {

    private OppgittPeriodeUtil() {
        //Forhindrer instanser
    }

    static List<OppgittPeriodeEntitet> sorterEtterFom(List<OppgittPeriodeEntitet> oppgittePerioder) {
        return oppgittePerioder.stream().sorted(Comparator.comparing(OppgittPeriodeEntitet::getFom)).collect(Collectors.toList());
    }

    /**
     * Finn første dato fra søknad som ikke er en utsettelse.
     *
     * @param oppgittePerioder
     * @return første dato fra søknad som ikke er en utsettelse.
     */
    static Optional<LocalDate> finnFørsteSøkteUttaksdato(List<OppgittPeriodeEntitet> oppgittePerioder) {
        List<OppgittPeriodeEntitet> sortertePerioder = sorterEtterFom(oppgittePerioder);
        List<OppgittPeriodeEntitet> perioderMedUttak = sortertePerioder
            .stream()
            .filter(p -> Årsak.UKJENT.equals(p.getÅrsak()) || !p.isOpphold())
            .collect(Collectors.toList());

        if(perioderMedUttak.size() > 0) {
            return Optional.of(perioderMedUttak.get(0).getFom());
        }

        return Optional.empty();
    }

    static Optional<LocalDate> finnFørsteSøknadsdato(List<OppgittPeriodeEntitet> perioder) {
        List<OppgittPeriodeEntitet> sortertePerioder = sorterEtterFom(perioder);

        if(sortertePerioder.size() > 0) {
            return Optional.of(sortertePerioder.get(0).getFom());
        }

        return Optional.empty();
    }

}
