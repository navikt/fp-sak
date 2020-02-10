package no.nav.foreldrepenger.behandling.revurdering.felles;

import static no.nav.foreldrepenger.behandlingslager.behandling.vilkår.VilkårType.MEDLEMSKAPSVILKÅRET_LØPENDE;

import java.util.List;
import java.util.stream.Collectors;

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

    //TODO(OJR) burde kanskje innfører en egenskap som tilsier at MEDLEMSKAPSVILKÅRET_LØPENDE ikke er et inngangsvilkår?
    public static boolean vurder(Behandling revurdering) {
        List<Vilkår> vilkårene = revurdering.getBehandlingsresultat().getVilkårResultat().getVilkårene()
            .stream().filter(v -> !v.getVilkårType().equals(MEDLEMSKAPSVILKÅRET_LØPENDE)).collect(Collectors.toList());
        return vilkårene.stream().anyMatch(v -> !VilkårUtfallType.OPPFYLT.equals(v.getGjeldendeVilkårUtfall()));
    }

    public static Behandlingsresultat fastsett(Behandling revurdering) {
        boolean skalBeregnesIInfotrygd = harIngenBeregningsreglerILøsningen(revurdering);
        return SettOpphørOgIkkeRett.fastsett(revurdering, skalBeregnesIInfotrygd ? Vedtaksbrev.INGEN : Vedtaksbrev.AUTOMATISK);
    }

    private static boolean harIngenBeregningsreglerILøsningen(Behandling revurdering) {
        List<Vilkår> vilkårene = revurdering.getBehandlingsresultat().getVilkårResultat().getVilkårene();
        return vilkårene.stream()
            .anyMatch(vilkår -> VilkårType.BEREGNINGSGRUNNLAGVILKÅR.equals(vilkår.getVilkårType())
                && Avslagsårsak.INGEN_BEREGNINGSREGLER_TILGJENGELIG_I_LØSNINGEN.equals(vilkår.getAvslagsårsak())
                && VilkårUtfallType.IKKE_OPPFYLT.equals(vilkår.getGjeldendeVilkårUtfall()));
    }
}
