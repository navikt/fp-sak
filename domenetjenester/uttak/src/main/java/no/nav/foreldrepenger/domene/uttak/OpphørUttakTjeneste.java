package no.nav.foreldrepenger.domene.uttak;

import java.time.LocalDate;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import no.nav.foreldrepenger.behandling.BehandlingReferanse;
import no.nav.foreldrepenger.behandling.Skjæringstidspunkt;
import no.nav.foreldrepenger.behandlingslager.behandling.Behandlingsresultat;
import no.nav.foreldrepenger.behandlingslager.uttak.fp.FpUttakRepository;
import no.nav.foreldrepenger.behandlingslager.uttak.fp.PeriodeResultatÅrsak;
import no.nav.foreldrepenger.behandlingslager.uttak.fp.UttakResultatEntitet;
import no.nav.foreldrepenger.behandlingslager.uttak.fp.UttakResultatPeriodeEntitet;
import no.nav.foreldrepenger.behandlingslager.uttak.fp.UttakResultatPerioderEntitet;

@ApplicationScoped
public class OpphørUttakTjeneste {

    private FpUttakRepository fpUttakRepository;

    OpphørUttakTjeneste() {
        //CDI
    }

    @Inject
    public OpphørUttakTjeneste(UttakRepositoryProvider repositoryProvider) {
        this.fpUttakRepository = repositoryProvider.getFpUttakRepository();
    }

    public Optional<LocalDate> getOpphørsdato(BehandlingReferanse ref, Skjæringstidspunkt stp, Behandlingsresultat behandlingsresultat) {
        if (!behandlingsresultat.isBehandlingsresultatOpphørt()) {
            return Optional.empty();
        }
        var skjæringstidspunkt = stp.getUtledetSkjæringstidspunkt();
        var opphørsdato = utledOpphørsdatoFraUttak(hentUttakResultatFor(ref.behandlingId()), skjæringstidspunkt);

        return Optional.ofNullable(opphørsdato);
    }

    private UttakResultatEntitet hentUttakResultatFor(Long behandlingId) {
        return fpUttakRepository.hentUttakResultatHvisEksisterer(behandlingId).orElse(null);
    }

    private LocalDate utledOpphørsdatoFraUttak(UttakResultatEntitet uttakResultat, LocalDate skjæringstidspunkt) {
        var opphørsårsaker = PeriodeResultatÅrsak.opphørsAvslagÅrsaker();
        var perioder = getUttaksperioderIOmvendtRekkefølge(uttakResultat);

        // Finn fom-dato i første periode av de siste sammenhengende periodene med opphørårsaker
        LocalDate fom = null;
        for (var periode : perioder) {
            if (opphørsårsaker.contains(periode.getResultatÅrsak())) {
                fom = periode.getFom();
            } else if (fom != null && periode.isInnvilget()) {
                return fom;
            }
        }
        // bruk skjæringstidspunkt hvis fom = null eller tidligste periode i uttaksplan er opphørt eller avslått
        return skjæringstidspunkt;
    }

    private List<UttakResultatPeriodeEntitet> getUttaksperioderIOmvendtRekkefølge(UttakResultatEntitet uttakResultat) {
        return Optional.ofNullable(uttakResultat)
            .map(UttakResultatEntitet::getGjeldendePerioder)
            .map(UttakResultatPerioderEntitet::getPerioder).orElse(Collections.emptyList()).stream()
            .sorted(Comparator.comparing(UttakResultatPeriodeEntitet::getFom).reversed())
            .toList();
    }
}
