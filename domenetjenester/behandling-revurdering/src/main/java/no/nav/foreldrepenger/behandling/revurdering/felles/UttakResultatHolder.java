package no.nav.foreldrepenger.behandling.revurdering.felles;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import no.nav.foreldrepenger.behandlingslager.behandling.vedtak.BehandlingVedtak;
import no.nav.foreldrepenger.domene.uttak.ForeldrepengerUttakPeriode;

public interface UttakResultatHolder {

    Object getUttakResultat();

    LocalDate getSisteDagAvSistePeriode();
    LocalDate getFørsteDagAvFørstePeriode();

    boolean eksistererUttakResultat();

    /**
     * Bare FP
     */
    List<ForeldrepengerUttakPeriode> getGjeldendePerioder();

    boolean kontrollerErSisteUttakAvslåttMedÅrsak();

    boolean vurderOmErEndringIUttakFraEndringsdato(LocalDate endringsdato, UttakResultatHolder uttakresultatSammenligneMed);

    Optional<BehandlingVedtak> getBehandlingVedtak();
}
