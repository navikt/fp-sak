package no.nav.foreldrepenger.behandling.steg.inngangsvilkår.opptjening.fp;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import no.nav.foreldrepenger.behandling.steg.inngangsvilkår.InngangsvilkårFellesTjeneste;
import no.nav.foreldrepenger.behandling.steg.inngangsvilkår.opptjening.RyddOpptjening;
import no.nav.foreldrepenger.behandling.steg.inngangsvilkår.opptjening.felles.FastsettOpptjeningsperiodeStegFelles;
import no.nav.foreldrepenger.behandlingskontroll.BehandlingStegModell;
import no.nav.foreldrepenger.behandlingskontroll.BehandlingStegRef;
import no.nav.foreldrepenger.behandlingskontroll.BehandlingTypeRef;
import no.nav.foreldrepenger.behandlingskontroll.BehandlingskontrollKontekst;
import no.nav.foreldrepenger.behandlingskontroll.FagsakYtelseTypeRef;
import no.nav.foreldrepenger.behandlingslager.behandling.BehandlingStegType;
import no.nav.foreldrepenger.behandlingslager.behandling.BehandlingsresultatRepository;
import no.nav.foreldrepenger.behandlingslager.behandling.opptjening.OpptjeningRepository;
import no.nav.foreldrepenger.behandlingslager.behandling.repository.BehandlingRepository;
import no.nav.foreldrepenger.behandlingslager.behandling.repository.BehandlingRepositoryProvider;


@BehandlingStegRef(kode = "VURDER_OPPTJ_PERIODE")
@BehandlingTypeRef
@FagsakYtelseTypeRef("FP")
@ApplicationScoped
public class FastsettOpptjeningsperiodeSteg extends FastsettOpptjeningsperiodeStegFelles {

    private OpptjeningRepository opptjeningRepository;
    private BehandlingRepository behandlingRepository;

    @Inject
    public FastsettOpptjeningsperiodeSteg(BehandlingRepositoryProvider repositoryProvider, OpptjeningRepository opptjeningRepository, InngangsvilkårFellesTjeneste inngangsvilkårFellesTjeneste) {
        super(repositoryProvider, inngangsvilkårFellesTjeneste, BehandlingStegType.FASTSETT_OPPTJENINGSPERIODE);
        this.opptjeningRepository = opptjeningRepository;
        this.behandlingRepository = repositoryProvider.getBehandlingRepository();
    }

    @Override
    public void vedHoppOverBakover(BehandlingskontrollKontekst kontekst, BehandlingStegModell modell, BehandlingStegType førsteSteg,
                                   BehandlingStegType sisteSteg) {
        if (!erVilkårOverstyrt(kontekst.getBehandlingId())) {
            super.vedHoppOverBakover(kontekst, modell, førsteSteg, sisteSteg);
            new RyddOpptjening(behandlingRepository, opptjeningRepository, kontekst).ryddOpp();
        }
    }

    @Override
    public void vedHoppOverFramover(BehandlingskontrollKontekst kontekst, BehandlingStegModell modell, BehandlingStegType førsteSteg,
                                    BehandlingStegType sisteSteg) {
        if (!behandlingRepository.hentBehandling(kontekst.getBehandlingId()).erRevurdering()) {
            if (!erVilkårOverstyrt(kontekst.getBehandlingId())) {
                super.vedHoppOverFramover(kontekst, modell, førsteSteg, sisteSteg);
                new RyddOpptjening(behandlingRepository, opptjeningRepository, kontekst).ryddOpp();
            }
        }
    }
}
