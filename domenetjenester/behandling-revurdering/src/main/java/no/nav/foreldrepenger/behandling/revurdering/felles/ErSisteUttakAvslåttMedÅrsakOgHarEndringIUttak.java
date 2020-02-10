package no.nav.foreldrepenger.behandling.revurdering.felles;

import no.nav.foreldrepenger.behandlingslager.behandling.Behandling;
import no.nav.foreldrepenger.behandlingslager.behandling.Behandlingsresultat;

public interface ErSisteUttakAvslåttMedÅrsakOgHarEndringIUttak {

    boolean vurder(UttakResultatHolder uttakresultatOpt, boolean erEndringIUttakFraEndringstidspunkt);

    Behandlingsresultat fastsett(Behandling revurdering);
}
