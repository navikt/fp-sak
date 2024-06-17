package no.nav.foreldrepenger.behandling.revurdering.felles;

import static no.nav.foreldrepenger.behandlingslager.behandling.vilkår.VilkårType.MEDLEMSKAPSVILKÅRET_LØPENDE;

import no.nav.foreldrepenger.behandlingslager.behandling.Behandling;
import no.nav.foreldrepenger.behandlingslager.behandling.Behandlingsresultat;
import no.nav.foreldrepenger.behandlingslager.behandling.vedtak.Vedtaksbrev;
import no.nav.foreldrepenger.behandlingslager.behandling.vilkår.Vilkår;
import no.nav.foreldrepenger.behandlingslager.behandling.vilkår.VilkårUtfallType;

class OppfyllerIkkeInngangsvilkårPåSkjæringstidsspunkt {

    private OppfyllerIkkeInngangsvilkårPåSkjæringstidsspunkt() {
    }

    public static boolean vurder(Behandlingsresultat revurdering) {
        return revurdering.isVilkårAvslått() || revurdering.getVilkårResultat()
            .getVilkårene()
            .stream()
            .filter(v -> !MEDLEMSKAPSVILKÅRET_LØPENDE.equals(v.getVilkårType())) // Medlemskapvilkår vurderes senere separat.
            .map(Vilkår::getGjeldendeVilkårUtfall)
            .anyMatch(VilkårUtfallType.IKKE_OPPFYLT::equals);
    }

    public static Behandlingsresultat fastsett(Behandling revurdering, Behandlingsresultat behandlingsresultat) {
        return SettOpphørOgIkkeRett.fastsett(revurdering, behandlingsresultat, Vedtaksbrev.AUTOMATISK);
    }
}
