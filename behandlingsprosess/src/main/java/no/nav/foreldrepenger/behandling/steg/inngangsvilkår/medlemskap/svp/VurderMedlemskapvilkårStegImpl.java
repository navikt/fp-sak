package no.nav.foreldrepenger.behandling.steg.inngangsvilkår.medlemskap.svp;

import static java.util.Collections.singletonList;

import java.util.List;

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
import no.nav.foreldrepenger.behandlingslager.behandling.BehandlingsresultatRepository;
import no.nav.foreldrepenger.behandlingslager.behandling.medlemskap.MedlemskapVilkårPeriodeRepository;
import no.nav.foreldrepenger.behandlingslager.behandling.repository.BehandlingRepositoryProvider;
import no.nav.foreldrepenger.behandlingslager.behandling.vilkår.VilkårType;
import no.nav.foreldrepenger.behandlingslager.behandling.vilkår.VilkårUtfallType;
import no.nav.foreldrepenger.inngangsvilkaar.RegelResultat;
import no.nav.foreldrepenger.skjæringstidspunkt.SkjæringstidspunktTjeneste;

@BehandlingStegRef(kode = "VURDERMV")
@BehandlingTypeRef
@FagsakYtelseTypeRef("SVP")
@ApplicationScoped
public class VurderMedlemskapvilkårStegImpl extends InngangsvilkårStegImpl {

    private static final List<VilkårType> STØTTEDE_VILKÅR = singletonList(VilkårType.MEDLEMSKAPSVILKÅRET);

    private final MedlemskapVilkårPeriodeRepository medlemskapVilkårPeriodeRepository;
    private final BehandlingsresultatRepository behandlingsresultatRepository;

    private final SkjæringstidspunktTjeneste skjæringstidspunktTjeneste;

    @Inject
    public VurderMedlemskapvilkårStegImpl(BehandlingRepositoryProvider repositoryProvider,
                                          InngangsvilkårFellesTjeneste inngangsvilkårFellesTjeneste,
                                          SkjæringstidspunktTjeneste skjæringstidspunktTjeneste) {
        super(repositoryProvider, inngangsvilkårFellesTjeneste, BehandlingStegType.VURDER_MEDLEMSKAPVILKÅR);
        this.medlemskapVilkårPeriodeRepository = repositoryProvider.getMedlemskapVilkårPeriodeRepository();
        this.behandlingsresultatRepository = repositoryProvider.getBehandlingsresultatRepository();
        this.skjæringstidspunktTjeneste = skjæringstidspunktTjeneste;
    }

    @Override
    public List<VilkårType> vilkårHåndtertAvSteg() {
        return STØTTEDE_VILKÅR;
    }

    @Override
    protected void utførtRegler(BehandlingskontrollKontekst kontekst,
                                Behandling behandling,
                                RegelResultat regelResultat) {
        var skjæringstidspunkt = skjæringstidspunktTjeneste.getSkjæringstidspunkter(behandling.getId())
            .getUtledetSkjæringstidspunkt();

        var behandlingsresultat = behandlingsresultatRepository.hent(behandling.getId());
        var medlemskapsvilkåret = behandlingsresultat.getVilkårResultat()
            .getVilkårene()
            .stream()
            .filter(v -> VilkårType.MEDLEMSKAPSVILKÅRET.equals(v.getVilkårType()))
            .findFirst();

        var utfall = medlemskapsvilkåret.orElseThrow(() -> new IllegalStateException("Finner ikke medlemskapsvikåret."))
            .getGjeldendeVilkårUtfall();

        var grBuilder = medlemskapVilkårPeriodeRepository.hentBuilderFor(behandling);
        var builder = grBuilder.getPeriodeBuilder();
        var periode = builder.getBuilderForVurderingsdato(skjæringstidspunkt);
        periode.medVilkårUtfall(utfall);
        if (VilkårUtfallType.IKKE_OPPFYLT.equals(utfall)) {
            periode.medVilkårUtfallMerknad(medlemskapsvilkåret.get().getVilkårUtfallMerknad());
        }
        builder.leggTil(periode);
        grBuilder.medMedlemskapsvilkårPeriode(builder);
        medlemskapVilkårPeriodeRepository.lagreMedlemskapsvilkår(behandling, grBuilder);
    }
}
