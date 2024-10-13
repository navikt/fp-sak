package no.nav.foreldrepenger.behandling.revurdering.felles;

import no.nav.foreldrepenger.behandlingslager.behandling.Behandling;
import no.nav.foreldrepenger.behandlingslager.behandling.Behandlingsresultat;
import no.nav.foreldrepenger.behandlingslager.behandling.vedtak.Vedtaksbrev;

class OppfyllerIkkeInngangsvilkårPåSkjæringstidsspunkt {

    private OppfyllerIkkeInngangsvilkårPåSkjæringstidsspunkt() {
    }

    public static boolean vurder(Behandlingsresultat revurdering) {
        return revurdering.isInngangsVilkårAvslått();
    }

    public static Behandlingsresultat fastsett(Behandling revurdering, Behandlingsresultat behandlingsresultat) {
        return SettOpphørOgIkkeRett.fastsett(revurdering, behandlingsresultat, Vedtaksbrev.AUTOMATISK);
    }
}
