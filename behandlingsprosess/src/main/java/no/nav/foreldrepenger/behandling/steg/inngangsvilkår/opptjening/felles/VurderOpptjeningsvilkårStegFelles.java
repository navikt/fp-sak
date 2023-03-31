package no.nav.foreldrepenger.behandling.steg.inngangsvilkår.opptjening.felles;

import static java.util.Collections.singletonList;

import java.util.Collections;
import java.util.List;

import no.nav.foreldrepenger.behandling.steg.inngangsvilkår.InngangsvilkårFellesTjeneste;
import no.nav.foreldrepenger.behandling.steg.inngangsvilkår.InngangsvilkårStegImpl;
import no.nav.foreldrepenger.behandling.steg.inngangsvilkår.opptjening.RyddOpptjening;
import no.nav.foreldrepenger.behandlingskontroll.BehandlingStegModell;
import no.nav.foreldrepenger.behandlingskontroll.BehandlingskontrollKontekst;
import no.nav.foreldrepenger.behandlingslager.behandling.Behandling;
import no.nav.foreldrepenger.behandlingslager.behandling.BehandlingStegType;
import no.nav.foreldrepenger.behandlingslager.behandling.opptjening.OpptjeningAktivitet;
import no.nav.foreldrepenger.behandlingslager.behandling.repository.BehandlingRepositoryProvider;
import no.nav.foreldrepenger.behandlingslager.behandling.vilkår.VilkårType;
import no.nav.foreldrepenger.behandlingslager.behandling.vilkår.VilkårUtfallType;
import no.nav.foreldrepenger.inngangsvilkaar.RegelResultat;
import no.nav.foreldrepenger.inngangsvilkaar.regelmodell.opptjening.OpptjeningsvilkårResultat;

public abstract class VurderOpptjeningsvilkårStegFelles extends InngangsvilkårStegImpl {

    protected static final VilkårType OPPTJENINGSVILKÅRET = VilkårType.OPPTJENINGSVILKÅRET;
    private static final List<VilkårType> STØTTEDE_VILKÅR = singletonList(OPPTJENINGSVILKÅRET);

    private BehandlingRepositoryProvider repositoryProvider;

    public VurderOpptjeningsvilkårStegFelles(BehandlingRepositoryProvider repositoryProvider,
                                             InngangsvilkårFellesTjeneste inngangsvilkårFellesTjeneste,
                                             BehandlingStegType behandlingStegType) {
        super(repositoryProvider, inngangsvilkårFellesTjeneste, behandlingStegType);
        this.repositoryProvider = repositoryProvider;
    }

    protected VurderOpptjeningsvilkårStegFelles() {
        // CDI proxy
    }

    @Override
    protected void utførtRegler(BehandlingskontrollKontekst kontekst, Behandling behandling, RegelResultat regelResultat) {
        var opptjeningRepository = repositoryProvider.getOpptjeningRepository();
        if (vilkårErVurdert(regelResultat)) {
            var opres = getVilkårresultat(behandling, regelResultat);
            var aktiviteter = mapTilOpptjeningsaktiviteter(opres);
            opptjeningRepository.lagreOpptjeningResultat(behandling, opres.getResultatOpptjent(), aktiviteter);
        } else {
            // rydd bort tidligere aktiviteter
            opptjeningRepository.lagreOpptjeningResultat(behandling, null, Collections.emptyList());
        }
    }

    protected abstract List<OpptjeningAktivitet> mapTilOpptjeningsaktiviteter(OpptjeningsvilkårResultat oppResultat);

    private OpptjeningsvilkårResultat getVilkårresultat(Behandling behandling, RegelResultat regelResultat) {
        var op = (OpptjeningsvilkårResultat) regelResultat.ekstraResultater()
                .get(OPPTJENINGSVILKÅRET);
        if (op == null) {
            throw new IllegalArgumentException(
                    "Utvikler-feil: finner ikke resultat fra evaluering av Inngangsvilkår/Opptjeningsvilkåret:" + behandling.getId());
        }
        return op;
    }

    private boolean vilkårErVurdert(RegelResultat regelResultat) {
        var opptjeningsvilkår = regelResultat.vilkårResultat().getVilkårene().stream()
                .filter(v -> v.getVilkårType().equals(OPPTJENINGSVILKÅRET))
                .findFirst();
        return opptjeningsvilkår.filter(v -> !VilkårUtfallType.IKKE_VURDERT.equals(v.getGjeldendeVilkårUtfall())).isPresent();
    }

    @Override
    public void vedHoppOverBakover(BehandlingskontrollKontekst kontekst, BehandlingStegModell modell, BehandlingStegType hoppesTilSteg,
            BehandlingStegType hoppesFraSteg) {
        super.vedHoppOverBakover(kontekst, modell, hoppesTilSteg, hoppesFraSteg);
        if (!erVilkårOverstyrt(kontekst.getBehandlingId())) {
            new RyddOpptjening(repositoryProvider, kontekst).ryddOppAktiviteter();
        }
    }

    @Override
    public void vedHoppOverFramover(BehandlingskontrollKontekst kontekst, BehandlingStegModell modell, BehandlingStegType hoppesFraSteg,
            BehandlingStegType hoppesTilSteg) {
        super.vedHoppOverFramover(kontekst, modell, hoppesFraSteg, hoppesTilSteg);
        if (!repositoryProvider.getBehandlingRepository().hentBehandling(kontekst.getBehandlingId()).erRevurdering()) {
            if (!erVilkårOverstyrt(kontekst.getBehandlingId())) {
                new RyddOpptjening(repositoryProvider, kontekst).ryddOppAktiviteter();
            }
        }
    }

    @Override
    public List<VilkårType> vilkårHåndtertAvSteg() {
        return STØTTEDE_VILKÅR;
    }
}
