package no.nav.foreldrepenger.behandling.revurdering.felles;

import static no.nav.foreldrepenger.behandlingslager.behandling.vilkår.VilkårType.MEDLEMSKAPSVILKÅRET_LØPENDE;

import no.nav.foreldrepenger.behandlingslager.behandling.Behandling;
import no.nav.foreldrepenger.behandlingslager.behandling.Behandlingsresultat;
import no.nav.foreldrepenger.behandlingslager.behandling.vedtak.Vedtaksbrev;
import no.nav.foreldrepenger.behandlingslager.behandling.vilkår.Avslagsårsak;
import no.nav.foreldrepenger.behandlingslager.behandling.vilkår.Vilkår;
import no.nav.foreldrepenger.behandlingslager.behandling.vilkår.VilkårType;
import no.nav.foreldrepenger.behandlingslager.behandling.vilkår.VilkårUtfallType;

class OppfyllerIkkeInngangsvilkårPåSkjæringstidsspunkt {

    private OppfyllerIkkeInngangsvilkårPåSkjæringstidsspunkt() {
    }

    // TODO(OJR) burde kanskje innfører en egenskap som tilsier at
    // MEDLEMSKAPSVILKÅRET_LØPENDE ikke er et inngangsvilkår?
    public static boolean vurder(Behandlingsresultat revurdering) {
        return revurdering.isVilkårAvslått() || revurdering.getVilkårResultat().getVilkårene().stream()
                .filter(v -> !MEDLEMSKAPSVILKÅRET_LØPENDE.equals(v.getVilkårType()))
                .map(Vilkår::getGjeldendeVilkårUtfall)
                .anyMatch(VilkårUtfallType.IKKE_OPPFYLT::equals);
    }

    public static Behandlingsresultat fastsett(Behandling revurdering, Behandlingsresultat behandlingsresultat) {
        var skalBeregnesIInfotrygd = harIngenBeregningsreglerILøsningen(behandlingsresultat);
        return SettOpphørOgIkkeRett.fastsett(revurdering, behandlingsresultat, skalBeregnesIInfotrygd ? Vedtaksbrev.INGEN : Vedtaksbrev.AUTOMATISK);
    }

    private static boolean harIngenBeregningsreglerILøsningen(Behandlingsresultat behandlingsresultat) {
        var vilkårene = behandlingsresultat.getVilkårResultat().getVilkårene();
        return vilkårene.stream()
                .anyMatch(vilkår -> VilkårType.BEREGNINGSGRUNNLAGVILKÅR.equals(vilkår.getVilkårType())
                        && Avslagsårsak.INGEN_BEREGNINGSREGLER_TILGJENGELIG_I_LØSNINGEN.equals(vilkår.getAvslagsårsak())
                        && VilkårUtfallType.IKKE_OPPFYLT.equals(vilkår.getGjeldendeVilkårUtfall()));
    }
}
