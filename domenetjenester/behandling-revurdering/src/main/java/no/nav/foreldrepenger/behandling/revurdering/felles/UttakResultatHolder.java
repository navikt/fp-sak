package no.nav.foreldrepenger.behandling.revurdering.felles;

import java.time.LocalDate;
import java.util.Optional;
import java.util.function.Predicate;

import no.nav.foreldrepenger.behandlingslager.behandling.Behandlingsresultat;
import no.nav.foreldrepenger.behandlingslager.behandling.vedtak.BehandlingVedtak;

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

    boolean harUlikUttaksplan(UttakResultatHolder other);

    Optional<BehandlingVedtak> getBehandlingVedtak();
}
