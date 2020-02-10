package no.nav.foreldrepenger.behandling.steg.inngangsvilkår.medlemskap.fp;

import static java.util.Collections.singletonList;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import no.nav.foreldrepenger.behandling.steg.inngangsvilkår.InngangsvilkårFellesTjeneste;
import no.nav.foreldrepenger.behandling.steg.inngangsvilkår.InngangsvilkårStegImpl;
import no.nav.foreldrepenger.behandlingskontroll.BehandlingStegRef;
import no.nav.foreldrepenger.behandlingskontroll.BehandlingTypeRef;
import no.nav.foreldrepenger.behandlingskontroll.BehandlingskontrollKontekst;
import no.nav.foreldrepenger.behandlingskontroll.FagsakYtelseTypeRef;
import no.nav.foreldrepenger.behandlingslager.behandling.Behandling;
import no.nav.foreldrepenger.behandlingslager.behandling.BehandlingStegType;
import no.nav.foreldrepenger.behandlingslager.behandling.medlemskap.MedlemskapVilkårPeriodeGrunnlagEntitet;
import no.nav.foreldrepenger.behandlingslager.behandling.medlemskap.MedlemskapVilkårPeriodeRepository;
import no.nav.foreldrepenger.behandlingslager.behandling.medlemskap.MedlemskapsvilkårPeriodeEntitet;
import no.nav.foreldrepenger.behandlingslager.behandling.medlemskap.MedlemskapsvilkårPerioderEntitet;
import no.nav.foreldrepenger.behandlingslager.behandling.repository.BehandlingRepositoryProvider;
import no.nav.foreldrepenger.behandlingslager.behandling.vilkår.Vilkår;
import no.nav.foreldrepenger.behandlingslager.behandling.vilkår.VilkårType;
import no.nav.foreldrepenger.behandlingslager.behandling.vilkår.VilkårUtfallType;
import no.nav.foreldrepenger.inngangsvilkaar.RegelResultat;
import no.nav.foreldrepenger.skjæringstidspunkt.SkjæringstidspunktTjeneste;

@BehandlingStegRef(kode = "VURDERMV")
@BehandlingTypeRef
@FagsakYtelseTypeRef("FP")
@ApplicationScoped
public class VurderMedlemskapvilkårStegImpl extends InngangsvilkårStegImpl {

    private static List<VilkårType> STØTTEDE_VILKÅR = singletonList(
        VilkårType.MEDLEMSKAPSVILKÅRET
    );

    private MedlemskapVilkårPeriodeRepository medlemskapVilkårPeriodeRepository;

    private SkjæringstidspunktTjeneste skjæringstidspunktTjeneste;

    @Inject
    public VurderMedlemskapvilkårStegImpl(BehandlingRepositoryProvider repositoryProvider, InngangsvilkårFellesTjeneste inngangsvilkårFellesTjeneste, SkjæringstidspunktTjeneste skjæringstidspunktTjeneste) {
        super(repositoryProvider, inngangsvilkårFellesTjeneste, BehandlingStegType.VURDER_MEDLEMSKAPVILKÅR);
        this.medlemskapVilkårPeriodeRepository = repositoryProvider.getMedlemskapVilkårPeriodeRepository();
        this.skjæringstidspunktTjeneste = skjæringstidspunktTjeneste;
    }

    @Override
    public List<VilkårType> vilkårHåndtertAvSteg() {
        return STØTTEDE_VILKÅR;
    }

    @Override
    protected void utførtRegler(BehandlingskontrollKontekst kontekst, Behandling behandling, RegelResultat regelResultat) {
        LocalDate skjæringstidspunkt = skjæringstidspunktTjeneste.getSkjæringstidspunkter(behandling.getId()).getUtledetSkjæringstidspunkt();

        Optional<Vilkår> medlemskapsvilkåret = behandling.getBehandlingsresultat().getVilkårResultat().getVilkårene()
            .stream()
            .filter(v -> VilkårType.MEDLEMSKAPSVILKÅRET.equals(v.getVilkårType()))
            .findFirst();

        VilkårUtfallType utfall = medlemskapsvilkåret.orElseThrow(() -> new IllegalStateException("Finner ikke medlemskapsvikåret.")).getGjeldendeVilkårUtfall();

        MedlemskapVilkårPeriodeGrunnlagEntitet.Builder grBuilder = medlemskapVilkårPeriodeRepository.hentBuilderFor(behandling);
        MedlemskapsvilkårPeriodeEntitet.Builder builder = grBuilder.getPeriodeBuilder();
        MedlemskapsvilkårPerioderEntitet.Builder periode = builder.getBuilderForVurderingsdato(skjæringstidspunkt);
        periode.medVilkårUtfall(utfall);
        if (VilkårUtfallType.IKKE_OPPFYLT.equals(utfall)) {
            periode.medVilkårUtfallMerknad(medlemskapsvilkåret.get().getVilkårUtfallMerknad());
        }
        builder.leggTil(periode);
        grBuilder.medMedlemskapsvilkårPeriode(builder);
        medlemskapVilkårPeriodeRepository.lagreMedlemskapsvilkår(behandling, grBuilder);
    }
}
