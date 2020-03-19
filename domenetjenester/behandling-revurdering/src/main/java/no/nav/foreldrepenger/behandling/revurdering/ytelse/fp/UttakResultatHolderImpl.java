package no.nav.foreldrepenger.behandling.revurdering.ytelse.fp;

import java.time.LocalDate;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import org.jboss.weld.exceptions.UnsupportedOperationException;

import no.nav.foreldrepenger.behandling.revurdering.felles.UttakResultatHolder;
import no.nav.foreldrepenger.behandlingslager.BaseEntitet;
import no.nav.foreldrepenger.behandlingslager.behandling.vedtak.BehandlingVedtak;
import no.nav.foreldrepenger.behandlingslager.uttak.IkkeOppfyltÅrsak;
import no.nav.foreldrepenger.behandlingslager.uttak.PeriodeResultatÅrsak;
import no.nav.foreldrepenger.behandlingslager.uttak.UttakResultatEntitet;
import no.nav.foreldrepenger.behandlingslager.uttak.UttakResultatPeriodeAktivitetEntitet;
import no.nav.foreldrepenger.behandlingslager.uttak.UttakResultatPeriodeEntitet;
import no.nav.foreldrepenger.behandlingslager.uttak.UttakResultatPerioderEntitet;


public class UttakResultatHolderImpl implements UttakResultatHolder {

    private Optional<UttakResultatEntitet> uttakresultat;


    public UttakResultatHolderImpl(Optional<UttakResultatEntitet> uttakresultat) {
        this.uttakresultat = uttakresultat;
    }

    @Override
    public BaseEntitet getUttakResultat(){
        return  uttakresultat.orElse(null);
    }

    @Override
    public LocalDate getSisteDagAvSistePeriode() {
        if (!uttakresultat.isPresent()) {
            return LocalDate.MIN;
        }
        return uttakresultat.get().getGjeldendePerioder().getPerioder().stream()
            .sorted(inReverseOrder())
            .filter(UttakResultatPeriodeEntitet::isInnvilget)
            .map(UttakResultatPeriodeEntitet::getTom)
            .findFirst().orElse(LocalDate.MIN);
    }


    @Override
    public LocalDate getFørsteDagAvFørstePeriode() {
        throw new UnsupportedOperationException("Not implemented");  // dummy
    }

    @Override
    public BehandlingVedtak getBehandlingVedtak() {
        if (uttakresultat.isPresent())
            return uttakresultat.get().getBehandlingsresultat().getBehandlingVedtak();
        else
            return null;
    }

    @Override
    public boolean eksistererUttakResultat() {
        return uttakresultat.isPresent();
    }

    @Override
    public UttakResultatPerioderEntitet getGjeldendePerioder() {
        return uttakresultat.get().getGjeldendePerioder();
    }

    private UttakResultatPeriodeEntitet finnSisteUttaksperiode() {
        if (uttakresultat.isPresent()) {
            List<UttakResultatPeriodeEntitet> perioder = uttakresultat.get().getGjeldendePerioder().getPerioder();
            perioder.sort(Comparator.comparing(UttakResultatPeriodeEntitet::getFom).reversed());
            return perioder.get(0);
        } else
            return null;

    }

    @Override
    public boolean kontrollerErSisteUttakAvslåttMedÅrsak() {
        if (!uttakresultat.isPresent()) {
            return false;
        }
        Set<PeriodeResultatÅrsak> opphørsAvslagÅrsaker = IkkeOppfyltÅrsak.opphørsAvslagÅrsaker();
        UttakResultatPeriodeEntitet sisteUttaksperiode  = finnSisteUttaksperiode();
        if(sisteUttaksperiode == null){
            return false;
        }
        return opphørsAvslagÅrsaker.contains(finnSisteUttaksperiode().getPeriodeResultatÅrsak());
    }

    @Override
    public boolean vurderOmErEndringIUttakFraEndringsdato(LocalDate endringsdato,UttakResultatHolder uttakresultatSammenligneMed){
        List<UttakResultatPeriodeEntitet> uttaksPerioderEtterEndringTP = finnUttaksperioderEtterEndringsdato(endringsdato, uttakresultatSammenligneMed);
        List<UttakResultatPeriodeEntitet> originaleUttaksPerioderEtterEndringTP = finnUttaksperioderEtterEndringsdato(endringsdato, this);
        return !erUttakresultatperiodeneLike(uttaksPerioderEtterEndringTP, originaleUttaksPerioderEtterEndringTP);
    }

    private Comparator<UttakResultatPeriodeEntitet> inReverseOrder() {
        return Collections.reverseOrder(Comparator.comparing(UttakResultatPeriodeEntitet::getFom));
    }

    public static List<UttakResultatPeriodeEntitet> finnUttaksperioderEtterEndringsdato(LocalDate endringsdato, UttakResultatHolder uttakResultatEntitet) {
        if (!uttakResultatEntitet.eksistererUttakResultat()) {
            return Collections.emptyList();
        }

        return uttakResultatEntitet.getGjeldendePerioder().getPerioder()
            .stream().filter(periode -> !periode.getFom().isBefore(endringsdato)).collect(Collectors.toList());
    }

    private boolean erAktiviteteneIPeriodeneLike(UttakResultatPeriodeEntitet periode1, UttakResultatPeriodeEntitet periode2) {
        List<UttakResultatPeriodeAktivitetEntitet> aktiviteter1 = periode1.getAktiviteter();
        List<UttakResultatPeriodeAktivitetEntitet> aktiviteter2 = periode2.getAktiviteter();
        if (aktiviteter1.size() != aktiviteter2.size()) {
            return false;
        }
        int antallAktiviteter = aktiviteter1.size();
        for (int i = 0; i < antallAktiviteter; i++) {
            // Sjekk på Trekk i antall uker/dager
            UttakResultatPeriodeAktivitetEntitet aktivitet1 = aktiviteter1.get(i);
            UttakResultatPeriodeAktivitetEntitet aktivitet2 = aktiviteter2.get(i);
            if (aktivitet1.getTrekkdager().compareTo(aktivitet2.getTrekkdager()) != 0) {
                return false;
            }
            // Sjekk på Stønadskonto
            if (!aktivitet1.getTrekkonto().equals(aktivitet2.getTrekkonto())) {
                return false;
            }
            // Sjekk på Andel i arbeid
            if ((aktivitet1.getArbeidsprosent()
                .compareTo(aktivitet2.getArbeidsprosent())) != 0) {
                return false;
            }
            // Sjekk på Utbetalingsgrad
            if ((aktivitet1.getUtbetalingsprosent()
                .compareTo(aktivitet2.getUtbetalingsprosent())) != 0) {
                return false;
            }
        }
        return true;
    }

    private boolean erUttakresultatperiodeneLike(List<UttakResultatPeriodeEntitet> listeMedPerioder1, List<UttakResultatPeriodeEntitet> listeMedPerioder2) {
        // Sjekk på Ny/fjernet
        if (listeMedPerioder1.size() != listeMedPerioder2.size()) {
            return false;
        }
        int antallPerioder = listeMedPerioder1.size();
        for (int i = 0; i < antallPerioder; i++) {
            UttakResultatPeriodeEntitet periode1 = listeMedPerioder1.get(i);
            UttakResultatPeriodeEntitet periode2 = listeMedPerioder2.get(i);
            if (!periode1.getFom().isEqual(periode2.getFom()) || !periode1.getTom().isEqual(periode2.getTom())) {
                return false;
            }
            if (!erAktiviteteneIPeriodeneLike(periode1, periode2)) {
                return false;
            }
            // Sjekk på Samtidig uttak
            if (periode1.isSamtidigUttak() != periode2.isSamtidigUttak()) {
                return false;
            }
            if (periode1.isFlerbarnsdager() != periode2.isFlerbarnsdager()) {
                return false;
            }
            // Sjekk på Utfall
            if (!periode1.getPeriodeResultatType().equals(periode2.getPeriodeResultatType())) {
                return false;
            }
            // Sjekk på Gradering utfall
            if (periode1.isGraderingInnvilget() != periode2.isGraderingInnvilget()) {
                return false;
            }



        }
        return true;
    }
}
