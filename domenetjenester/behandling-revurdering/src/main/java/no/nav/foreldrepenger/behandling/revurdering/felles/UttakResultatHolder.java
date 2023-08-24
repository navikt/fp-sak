package no.nav.foreldrepenger.behandling.revurdering.felles;

import no.nav.foreldrepenger.behandlingslager.behandling.Behandlingsresultat;
import no.nav.foreldrepenger.behandlingslager.behandling.vedtak.BehandlingVedtak;

import java.time.LocalDate;
import java.util.Optional;
import java.util.function.Predicate;

public interface UttakResultatHolder {

    LocalDate getSisteDagAvSistePeriode();

    LocalDate getFørsteDagAvFørstePeriode();

    boolean eksistererUttakResultat();

    interface VurderOpphørFørDagensDato extends Predicate<Behandlingsresultat> {
    }

    /**
     * Bare FP
     */
    boolean kontrollerErSisteUttakAvslåttMedÅrsak();

    default boolean harOpphørsUttakNyeInnvilgetePerioder(UttakResultatHolder other) {
        return false;
    }

    boolean harUlikUttaksplan(UttakResultatHolder other);

    Optional<BehandlingVedtak> getBehandlingVedtak();
}
