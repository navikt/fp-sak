package no.nav.foreldrepenger.domene.uttak.fastsetteperioder.grunnlagbyggere;

import java.util.Objects;
import java.util.Set;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import no.nav.foreldrepenger.behandling.BehandlingReferanse;
import no.nav.foreldrepenger.behandlingslager.behandling.BehandlingsresultatRepository;
import no.nav.foreldrepenger.behandlingslager.behandling.personopplysning.RelasjonsRolleType;
import no.nav.foreldrepenger.behandlingslager.behandling.vilkår.VilkårResultat;
import no.nav.foreldrepenger.behandlingslager.behandling.vilkår.VilkårType;
import no.nav.foreldrepenger.domene.uttak.UttakRepositoryProvider;
import no.nav.foreldrepenger.domene.uttak.input.UttakInput;
import no.nav.foreldrepenger.regler.uttak.fastsetteperiode.grunnlag.Inngangsvilkår;

@ApplicationScoped
public class InngangsvilkårGrunnlagBygger {

    private BehandlingsresultatRepository behandlingsresultatRepository;

    @Inject
    public InngangsvilkårGrunnlagBygger(UttakRepositoryProvider uttakRepositoryProvider) {
        this.behandlingsresultatRepository = uttakRepositoryProvider.getBehandlingsresultatRepository();
    }

    InngangsvilkårGrunnlagBygger() {
        //CDI
    }

    public Inngangsvilkår.Builder byggGrunnlag(UttakInput uttakInput) {
        var ref = uttakInput.getBehandlingReferanse();
        var vilkårResultat = finnVilkårResultat(ref);

        return new Inngangsvilkår.Builder()
                .opptjeningOppfylt(opptjeningsvilkåretOppfylt(vilkårResultat))
                .medlemskapOppfylt(medlemskapsvilkåretOppfylt(vilkårResultat))
                .foreldreansvarnOppfylt(foreldreansvarsvilkåretOppfylt(vilkårResultat))
                .adopsjonOppfylt(adopsjonsvilkåretOppfylt(vilkårResultat))
                .fødselOppfylt(fødselsvilkårOppfylt(vilkårResultat, ref));
    }

    private VilkårResultat finnVilkårResultat(BehandlingReferanse behandlingReferanse) {
        return behandlingsresultatRepository.hent(behandlingReferanse.behandlingId()).getVilkårResultat();
    }

    private static boolean foreldreansvarsvilkåretOppfylt(VilkårResultat vilkårResultat) {
        //Bare 2. ledd for foreldrepenger
        return vilkårAvTypeErOppfylt(vilkårResultat, VilkårType.FORELDREANSVARSVILKÅRET_2_LEDD)  &&
            vilkårAvTypeErOppfylt(vilkårResultat, VilkårType.OMSORGSOVERTAKELSEVILKÅR);
    }

    private static boolean adopsjonsvilkåretOppfylt(VilkårResultat vilkårResultat) {
        return vilkårAvTypeErOppfylt(vilkårResultat, VilkårType.ADOPSJONSVILKARET_FORELDREPENGER) &&
            vilkårAvTypeErOppfylt(vilkårResultat, VilkårType.OMSORGSOVERTAKELSEVILKÅR);
    }

    private static boolean fødselsvilkårOppfylt(VilkårResultat vilkårResultat, BehandlingReferanse ref) {
        if (søkerErMor(ref)) {
            return vilkårAvTypeErOppfylt(vilkårResultat, VilkårType.FØDSELSVILKÅRET_MOR);
        }
        return vilkårAvTypeErOppfylt(vilkårResultat, VilkårType.FØDSELSVILKÅRET_FAR_MEDMOR);
    }

    private static boolean medlemskapsvilkåretOppfylt(VilkårResultat vilkårResultat) {
        return vilkårAvTypeErOppfylt(vilkårResultat, VilkårType.MEDLEMSKAPSVILKÅRET);
    }

    private static boolean opptjeningsvilkåretOppfylt(VilkårResultat vilkårResultat) {
        return vilkårAvTypeErOppfylt(vilkårResultat, VilkårType.OPPTJENINGSVILKÅRET);
    }

    private static boolean søkerErMor(BehandlingReferanse ref) {
        return ref.relasjonRolle().equals(RelasjonsRolleType.MORA);
    }

    private static boolean vilkårAvTypeErOppfylt(VilkårResultat vilkårResultat, VilkårType type) {
        var vilkår = vilkårResultat.getVilkårene()
            .stream()
            .filter(v -> Objects.equals(v.getVilkårType(), type))
            .findFirst();
        return vilkår.map(v -> !v.erIkkeOppfylt()).orElse(true);
    }
}
