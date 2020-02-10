package no.nav.foreldrepenger.behandling.steg.inngangsvilkår.opptjening.felles;

import static java.util.Collections.singletonList;

import java.util.Collections;
import java.util.List;
import java.util.Optional;

import no.nav.foreldrepenger.behandling.steg.inngangsvilkår.InngangsvilkårFellesTjeneste;
import no.nav.foreldrepenger.behandling.steg.inngangsvilkår.InngangsvilkårStegImpl;
import no.nav.foreldrepenger.behandling.steg.inngangsvilkår.opptjening.MapTilOpptjeningAktiviteter;
import no.nav.foreldrepenger.behandling.steg.inngangsvilkår.opptjening.RyddOpptjening;
import no.nav.foreldrepenger.behandlingskontroll.BehandlingStegModell;
import no.nav.foreldrepenger.behandlingskontroll.BehandlingskontrollKontekst;
import no.nav.foreldrepenger.behandlingslager.behandling.Behandling;
import no.nav.foreldrepenger.behandlingslager.behandling.BehandlingStegType;
import no.nav.foreldrepenger.behandlingslager.behandling.opptjening.OpptjeningAktivitet;
import no.nav.foreldrepenger.behandlingslager.behandling.opptjening.OpptjeningRepository;
import no.nav.foreldrepenger.behandlingslager.behandling.repository.BehandlingRepository;
import no.nav.foreldrepenger.behandlingslager.behandling.repository.BehandlingRepositoryProvider;
import no.nav.foreldrepenger.behandlingslager.behandling.vilkår.Vilkår;
import no.nav.foreldrepenger.behandlingslager.behandling.vilkår.VilkårType;
import no.nav.foreldrepenger.behandlingslager.behandling.vilkår.VilkårUtfallType;
import no.nav.foreldrepenger.inngangsvilkaar.RegelResultat;
import no.nav.foreldrepenger.inngangsvilkaar.regelmodell.opptjening.OpptjeningsvilkårResultat;

public abstract class VurderOpptjeningsvilkårStegFelles extends InngangsvilkårStegImpl {

    protected static final VilkårType OPPTJENINGSVILKÅRET = VilkårType.OPPTJENINGSVILKÅRET;
    private static List<VilkårType> STØTTEDE_VILKÅR = singletonList(OPPTJENINGSVILKÅRET);

    private OpptjeningRepository opptjeningRepository;
    private BehandlingRepository behandlingRepository;
    private BehandlingRepositoryProvider repositoryProvider;

    protected VurderOpptjeningsvilkårStegFelles() {
        // CDI proxy
    }

    public VurderOpptjeningsvilkårStegFelles(BehandlingRepositoryProvider repositoryProvider, 
                                             OpptjeningRepository opptjeningRepository, 
                                             InngangsvilkårFellesTjeneste inngangsvilkårFellesTjeneste, 
                                             BehandlingStegType behandlingStegType) {
        super(repositoryProvider, inngangsvilkårFellesTjeneste, behandlingStegType);
        this.opptjeningRepository = opptjeningRepository;
        this.behandlingRepository = repositoryProvider.getBehandlingRepository();
        this.repositoryProvider = repositoryProvider;
    }

    @Override
    protected void utførtRegler(BehandlingskontrollKontekst kontekst, Behandling behandling, RegelResultat regelResultat) {
        if (vilkårErVurdert(regelResultat)) {
            OpptjeningsvilkårResultat opres = getVilkårresultat(behandling, regelResultat);
            MapTilOpptjeningAktiviteter mapper = new MapTilOpptjeningAktiviteter();
            List<OpptjeningAktivitet> aktiviteter = mapTilOpptjeningsaktiviteter(mapper, opres);
            opptjeningRepository.lagreOpptjeningResultat(behandling, opres.getResultatOpptjent(), aktiviteter);
        } else {
            // rydd bort tidligere aktiviteter
            opptjeningRepository.lagreOpptjeningResultat(behandling, null, Collections.emptyList());
        }
    }

    protected abstract List<OpptjeningAktivitet> mapTilOpptjeningsaktiviteter(MapTilOpptjeningAktiviteter mapper, OpptjeningsvilkårResultat oppResultat);

    private OpptjeningsvilkårResultat getVilkårresultat(Behandling behandling, RegelResultat regelResultat) {
        OpptjeningsvilkårResultat op = (OpptjeningsvilkårResultat) regelResultat.getEkstraResultater()
            .get(OPPTJENINGSVILKÅRET);
        if (op == null) {
            throw new IllegalArgumentException(
                "Utvikler-feil: finner ikke resultat fra evaluering av Inngangsvilkår/Opptjeningsvilkåret:" + behandling.getId());
        }
        return op;
    }

    private boolean vilkårErVurdert(RegelResultat regelResultat) {
        Optional<Vilkår> opptjeningsvilkår = regelResultat.getVilkårResultat().getVilkårene().stream()
            .filter(v -> v.getVilkårType().equals(OPPTJENINGSVILKÅRET))
            .findFirst();
        return opptjeningsvilkår.map(v -> !v.getGjeldendeVilkårUtfall().equals(VilkårUtfallType.IKKE_VURDERT))
            .orElse(Boolean.FALSE);
    }

    @Override
    public void vedHoppOverBakover(BehandlingskontrollKontekst kontekst, BehandlingStegModell modell, BehandlingStegType hoppesTilSteg, BehandlingStegType hoppesFraSteg) {
        super.vedHoppOverBakover(kontekst, modell, hoppesTilSteg, hoppesFraSteg);
        if (!erVilkårOverstyrt(kontekst.getBehandlingId())) {
            new RyddOpptjening(behandlingRepository, opptjeningRepository, kontekst).ryddOppAktiviteter();
        }
    }

    @Override
    public void vedHoppOverFramover(BehandlingskontrollKontekst kontekst, BehandlingStegModell modell, BehandlingStegType hoppesFraSteg,
                                    BehandlingStegType hoppesTilSteg) {
        super.vedHoppOverFramover(kontekst, modell, hoppesFraSteg, hoppesTilSteg);
        if (!repositoryProvider.getBehandlingRepository().hentBehandling(kontekst.getBehandlingId()).erRevurdering()) {
            if (!erVilkårOverstyrt(kontekst.getBehandlingId())) {
                new RyddOpptjening(behandlingRepository, opptjeningRepository, kontekst).ryddOppAktiviteter();
            }
        }
    }

    @Override
    public List<VilkårType> vilkårHåndtertAvSteg() {
        return STØTTEDE_VILKÅR;
    }
}
