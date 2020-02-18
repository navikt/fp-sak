package no.nav.foreldrepenger.behandling.revurdering.felles;


import java.util.List;
import java.util.function.Predicate;

import no.nav.foreldrepenger.behandlingslager.behandling.Behandling;
import no.nav.foreldrepenger.behandlingslager.behandling.Behandlingsresultat;
import no.nav.foreldrepenger.behandlingslager.behandling.KonsekvensForYtelsen;


public interface HarEtablertYtelse {


    boolean vurder(Behandling revurdering,
                   boolean finnesInnvilgetIkkeOpphørtVedtak,
                   VurderOpphørDagensDato opphørFørEllerEtterDagensDato,
                   UttakResultatHolder uttakresultatOpt);

    Behandlingsresultat fastsettForIkkeEtablertYtelse(Behandling revurdering, List<KonsekvensForYtelsen> konsekvenserForYtelsen);

    interface VurderOpphørDagensDato extends Predicate<Behandlingsresultat> {
    }


}
