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
import no.nav.foreldrepenger.behandlingslager.behandling.vedtak.BehandlingVedtak;
import no.nav.foreldrepenger.behandlingslager.uttak.fp.IkkeOppfyltÅrsak;
import no.nav.foreldrepenger.behandlingslager.uttak.fp.PeriodeResultatÅrsak;
import no.nav.foreldrepenger.domene.uttak.ForeldrepengerUttak;
import no.nav.foreldrepenger.domene.uttak.ForeldrepengerUttakPeriode;


public class UttakResultatHolderImpl implements UttakResultatHolder {

    private Optional<ForeldrepengerUttak> uttakresultat;
    private BehandlingVedtak vedtak;


    public UttakResultatHolderImpl(Optional<ForeldrepengerUttak> uttakresultat, BehandlingVedtak vedtak) {
        this.uttakresultat = uttakresultat;
        this.vedtak = vedtak;
    }

    @Override
    public Object getUttakResultat() {
        return uttakresultat.orElse(null);
    }

    @Override
    public LocalDate getSisteDagAvSistePeriode() {
        if (uttakresultat.isEmpty()) {
            return LocalDate.MIN;
        }
        return uttakresultat.get().getGjeldendePerioder().stream()
            .sorted(inReverseOrder())
            .filter(ForeldrepengerUttakPeriode::isInnvilget)
            .map(ForeldrepengerUttakPeriode::getTom)
            .findFirst().orElse(LocalDate.MIN);
    }


    @Override
    public LocalDate getFørsteDagAvFørstePeriode() {
        throw new UnsupportedOperationException("Not implemented");  // dummy
    }

    @Override
    public boolean eksistererUttakResultat() {
        return uttakresultat.isPresent();
    }

    @Override
    public List<ForeldrepengerUttakPeriode> getGjeldendePerioder() {
        return uttakresultat.orElseThrow().getGjeldendePerioder();
    }

    private ForeldrepengerUttakPeriode finnSisteUttaksperiode() {
        if (uttakresultat.isPresent()) {
            List<ForeldrepengerUttakPeriode> perioder = uttakresultat.get().getGjeldendePerioder();
            perioder.sort(Comparator.comparing(ForeldrepengerUttakPeriode::getFom).reversed());
            return perioder.get(0);
        }
        return null;

    }

    @Override
    public boolean kontrollerErSisteUttakAvslåttMedÅrsak() {
        if (uttakresultat.isEmpty()) {
            return false;
        }
        Set<PeriodeResultatÅrsak> opphørsAvslagÅrsaker = IkkeOppfyltÅrsak.opphørsAvslagÅrsaker();
        ForeldrepengerUttakPeriode sisteUttaksperiode  = finnSisteUttaksperiode();
        if(sisteUttaksperiode == null){
            return false;
        }
        return opphørsAvslagÅrsaker.contains(finnSisteUttaksperiode().getResultatÅrsak());
    }

    @Override
    public boolean vurderOmErEndringIUttakFraEndringsdato(LocalDate endringsdato,UttakResultatHolder uttakresultatSammenligneMed){
        List<ForeldrepengerUttakPeriode> uttaksPerioderEtterEndringTP = finnUttaksperioderEtterEndringsdato(endringsdato, uttakresultatSammenligneMed);
        List<ForeldrepengerUttakPeriode> originaleUttaksPerioderEtterEndringTP = finnUttaksperioderEtterEndringsdato(endringsdato, this);
        return !erUttakresultatperiodeneLike(uttaksPerioderEtterEndringTP, originaleUttaksPerioderEtterEndringTP);
    }

    @Override
    public Optional<BehandlingVedtak> getBehandlingVedtak() {
        return Optional.ofNullable(vedtak);
    }

    private Comparator<ForeldrepengerUttakPeriode> inReverseOrder() {
        return Collections.reverseOrder(Comparator.comparing(ForeldrepengerUttakPeriode::getFom));
    }

    public static List<ForeldrepengerUttakPeriode> finnUttaksperioderEtterEndringsdato(LocalDate endringsdato, UttakResultatHolder uttak) {
        if (!uttak.eksistererUttakResultat()) {
            return Collections.emptyList();
        }

        return uttak.getGjeldendePerioder()
            .stream().filter(periode -> !periode.getFom().isBefore(endringsdato)).collect(Collectors.toList());
    }

    private boolean erAktiviteteneIPeriodeneLike(ForeldrepengerUttakPeriode periode1, ForeldrepengerUttakPeriode periode2) {
        var aktiviteter1 = periode1.getAktiviteter();
        var aktiviteter2 = periode2.getAktiviteter();
        if (aktiviteter1.size() != aktiviteter2.size()) {
            return false;
        }
        int antallAktiviteter = aktiviteter1.size();
        for (int i = 0; i < antallAktiviteter; i++) {
            // Sjekk på Trekk i antall uker/dager
            var aktivitet1 = aktiviteter1.get(i);
            var aktivitet2 = aktiviteter2.get(i);
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
            if ((aktivitet1.getUtbetalingsgrad()
                .compareTo(aktivitet2.getUtbetalingsgrad())) != 0) {
                return false;
            }
        }
        return true;
    }

    private boolean erUttakresultatperiodeneLike(List<ForeldrepengerUttakPeriode> listeMedPerioder1, List<ForeldrepengerUttakPeriode> listeMedPerioder2) {
        // Sjekk på Ny/fjernet
        if (listeMedPerioder1.size() != listeMedPerioder2.size()) {
            return false;
        }
        int antallPerioder = listeMedPerioder1.size();
        for (int i = 0; i < antallPerioder; i++) {
            ForeldrepengerUttakPeriode periode1 = listeMedPerioder1.get(i);
            ForeldrepengerUttakPeriode periode2 = listeMedPerioder2.get(i);
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
            if (!periode1.getResultatType().equals(periode2.getResultatType())) {
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
