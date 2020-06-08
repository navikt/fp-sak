package no.nav.foreldrepenger.behandling.revurdering.ytelse.fp;

import java.time.LocalDate;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;

import org.jboss.weld.exceptions.UnsupportedOperationException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import no.nav.foreldrepenger.behandling.revurdering.felles.UttakResultatHolder;
import no.nav.foreldrepenger.behandlingslager.behandling.vedtak.BehandlingVedtak;
import no.nav.foreldrepenger.behandlingslager.uttak.fp.IkkeOppfyltÅrsak;
import no.nav.foreldrepenger.behandlingslager.uttak.fp.PeriodeResultatÅrsak;
import no.nav.foreldrepenger.domene.uttak.ForeldrepengerUttak;
import no.nav.foreldrepenger.domene.uttak.ForeldrepengerUttakPeriode;
import no.nav.foreldrepenger.domene.uttak.ForeldrepengerUttakPeriodeAktivitet;


public class UttakResultatHolderImpl implements UttakResultatHolder {

    private static final Logger LOG = LoggerFactory.getLogger(UttakResultatHolder.class);

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
        return uttakresultat.map(ForeldrepengerUttak::getGjeldendePerioder).orElse(Collections.emptyList()).stream()
            .filter(ForeldrepengerUttakPeriode::isInnvilget)
            .map(ForeldrepengerUttakPeriode::getTom)
            .max(Comparator.naturalOrder()).orElse(LocalDate.MIN);
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
        if (uttakresultat.isEmpty()) {
            return List.of();
        }
        return uttakresultat.get().getGjeldendePerioder();
    }

    private Optional<ForeldrepengerUttakPeriode> finnSisteUttaksperiode() {
        return uttakresultat.map(ForeldrepengerUttak::getGjeldendePerioder).orElse(Collections.emptyList()).stream()
            .max(Comparator.comparing(ForeldrepengerUttakPeriode::getFom));
    }

    @Override
    public boolean kontrollerErSisteUttakAvslåttMedÅrsak() {
        Set<PeriodeResultatÅrsak> opphørsAvslagÅrsaker = IkkeOppfyltÅrsak.opphørsAvslagÅrsaker();
        return finnSisteUttaksperiode().map(ForeldrepengerUttakPeriode::getResultatÅrsak).map(opphørsAvslagÅrsaker::contains).orElse(false);
    }

    @Override
    public boolean vurderOmErEndringIUttak(UttakResultatHolder uttakresultatSammenligneMed){
        List<ForeldrepengerUttakPeriode> uttaksPerioderTP = uttakresultatSammenligneMed.getGjeldendePerioder();
        List<ForeldrepengerUttakPeriode> originaleUttaksPerioderTP = getGjeldendePerioder();
        return !erUttakresultatperiodeneLike(uttaksPerioderTP, originaleUttaksPerioderTP);
    }

    @Override
    public Optional<BehandlingVedtak> getBehandlingVedtak() {
        return Optional.ofNullable(vedtak);
    }

    private boolean erUttakresultatperiodeneLike(List<ForeldrepengerUttakPeriode> listeMedPerioder1, List<ForeldrepengerUttakPeriode> listeMedPerioder2) {
        // Sjekk på Ny/fjernet
        if (listeMedPerioder1.size() != listeMedPerioder2.size()) {
            LOG.info("BEHRES avvik antall perioder");
            return false;
        }
        int antallPerioder = listeMedPerioder1.size();
        for (int i = 0; i < antallPerioder; i++) {
            if (!erLikPeriode(listeMedPerioder1.get(i), listeMedPerioder2.get(i))) {
                return false;
            }
        }
        return true;
    }

    private boolean erLikPeriode(ForeldrepengerUttakPeriode p1, ForeldrepengerUttakPeriode p2) {
        if (p1.getAktiviteter().size() != p2.getAktiviteter().size()) {
            LOG.info("BEHRES avvik antall aktiviteter");
            return false;
        }
        var likeAktivitieter = p1.getAktiviteter().stream().allMatch(a1 -> p2.getAktiviteter().stream().anyMatch(a2 -> erLikAktivitet(a1, a2)));
        var sammenlign = p1.getTidsperiode().equals(p2.getTidsperiode()) &&
            Objects.equals(p1.isFlerbarnsdager(), p2.isFlerbarnsdager()) &&
            Objects.equals(p1.getResultatType(), p2.getResultatType()) &&
            Objects.equals(p1.getResultatÅrsak(), p2.getResultatÅrsak()) &&
            Objects.equals(p1.isGraderingInnvilget(), p2.isGraderingInnvilget()) &&
            Objects.equals(p1.getUtsettelseType(), p2.getUtsettelseType()) &&
            Objects.equals(p1.getGraderingAvslagÅrsak(), p2.getGraderingAvslagÅrsak()) &&
            likeAktivitieter;
        if (!sammenlign)
            LOG.info("BEHRES avvik i periodedata, like aktiviteter {}", likeAktivitieter);
        return sammenlign;
    }

    private boolean erLikAktivitet(ForeldrepengerUttakPeriodeAktivitet a1, ForeldrepengerUttakPeriodeAktivitet a2) {
        return Objects.equals(a1.getUttakAktivitet(), a2.getUttakAktivitet()) &&
            Objects.equals(a1.getTrekkonto(), a2.getTrekkonto()) &&
            Objects.equals(a1.getTrekkdager(), a2.getTrekkdager()) &&
            (Objects.equals(a1.getArbeidsprosent(), a2.getArbeidsprosent()) || a1.getArbeidsprosent().compareTo(a2.getArbeidsprosent()) == 0) &&
            (Objects.equals(a1.getUtbetalingsgrad(), a2.getUtbetalingsgrad()) || a1.getUtbetalingsgrad().compareTo(a2.getUtbetalingsgrad()) == 0);
    }

}
