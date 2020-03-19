package no.nav.foreldrepenger.domene.uttak;

import java.time.LocalDate;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import no.nav.foreldrepenger.behandling.BehandlingReferanse;
import no.nav.foreldrepenger.behandlingslager.behandling.Behandlingsresultat;
import no.nav.foreldrepenger.behandlingslager.uttak.IkkeOppfyltÅrsak;
import no.nav.foreldrepenger.behandlingslager.uttak.PeriodeResultatÅrsak;
import no.nav.foreldrepenger.behandlingslager.uttak.UttakRepository;
import no.nav.foreldrepenger.behandlingslager.uttak.UttakResultatEntitet;
import no.nav.foreldrepenger.behandlingslager.uttak.UttakResultatPeriodeEntitet;
import no.nav.foreldrepenger.behandlingslager.uttak.UttakResultatPerioderEntitet;

@ApplicationScoped
public class OpphørUttakTjeneste {

    private UttakRepository uttakRepository;

    OpphørUttakTjeneste() {
        //CDI
    }

    @Inject
    public OpphørUttakTjeneste(UttakRepositoryProvider repositoryProvider) {
        this.uttakRepository = repositoryProvider.getUttakRepository();
    }

    public Optional<LocalDate> getOpphørsdato(BehandlingReferanse ref, Behandlingsresultat behandlingsresultat) {
        if (!behandlingsresultat.isBehandlingsresultatOpphørt()) {
            return Optional.empty();
        }
        LocalDate skjæringstidspunkt = ref.getUtledetSkjæringstidspunkt();
        LocalDate opphørsdato = utledOpphørsdatoFraUttak(hentUttakResultatFor(ref.getBehandlingId()), skjæringstidspunkt);

        return Optional.ofNullable(opphørsdato);
    }

    private UttakResultatEntitet hentUttakResultatFor(Long behandlingId) {
        return uttakRepository.hentUttakResultatHvisEksisterer(behandlingId).orElse(null);
    }

    private LocalDate utledOpphørsdatoFraUttak(UttakResultatEntitet uttakResultat, LocalDate skjæringstidspunkt) {
        Set<PeriodeResultatÅrsak> opphørsårsaker = IkkeOppfyltÅrsak.opphørsAvslagÅrsaker();
        List<UttakResultatPeriodeEntitet> perioder = getUttaksperioderIOmvendtRekkefølge(uttakResultat);

        // Finn fom-dato i første periode av de siste sammenhengende periodene med opphørårsaker
        LocalDate fom = null;
        for (UttakResultatPeriodeEntitet periode : perioder) {
            if (opphørsårsaker.contains(periode.getPeriodeResultatÅrsak())) {
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
            .collect(Collectors.toList());
    }
}
