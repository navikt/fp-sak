package no.nav.foreldrepenger.behandling.revurdering.felles;

import java.time.LocalDate;

import no.nav.foreldrepenger.behandlingslager.BaseEntitet;
import no.nav.foreldrepenger.behandlingslager.behandling.vedtak.BehandlingVedtak;
import no.nav.foreldrepenger.behandlingslager.uttak.UttakResultatPerioderEntitet;
public interface UttakResultatHolder {


    BaseEntitet getUttakResultat();

    LocalDate getSisteDagAvSistePeriode();
    LocalDate getFørsteDagAvFørstePeriode();

    BehandlingVedtak getBehandlingVedtak();

    boolean eksistererUttakResultat();

    UttakResultatPerioderEntitet getGjeldendePerioder();

    boolean kontrollerErSisteUttakAvslåttMedÅrsak();

    boolean vurderOmErEndringIUttakFraEndringsdato(LocalDate endringsdato,UttakResultatHolder uttakresultatSammenligneMed);
}
