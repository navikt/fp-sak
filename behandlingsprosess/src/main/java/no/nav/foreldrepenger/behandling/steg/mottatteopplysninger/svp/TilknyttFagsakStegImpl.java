package no.nav.foreldrepenger.behandling.steg.mottatteopplysninger.svp;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import no.nav.foreldrepenger.behandling.FagsakRelasjonTjeneste;
import no.nav.foreldrepenger.behandling.steg.mottatteopplysninger.TilknyttFagsakSteg;
import no.nav.foreldrepenger.behandlingskontroll.BehandleStegResultat;
import no.nav.foreldrepenger.behandlingskontroll.BehandlingStegRef;
import no.nav.foreldrepenger.behandlingskontroll.BehandlingTypeRef;
import no.nav.foreldrepenger.behandlingskontroll.BehandlingskontrollKontekst;
import no.nav.foreldrepenger.behandlingskontroll.FagsakYtelseTypeRef;
import no.nav.foreldrepenger.behandlingslager.behandling.BehandlingStegKoder;
import no.nav.foreldrepenger.behandlingslager.fagsak.Dekningsgrad;
import no.nav.foreldrepenger.behandlingslager.fagsak.FagsakRepository;

@BehandlingStegRef(kode = BehandlingStegKoder.INNHENT_SØKNADOPP_KODE)
@BehandlingTypeRef("BT-002")
@FagsakYtelseTypeRef("SVP")
@ApplicationScoped
public class TilknyttFagsakStegImpl implements TilknyttFagsakSteg {

    private FagsakRepository fagsakRepository;
    private FagsakRelasjonTjeneste fagsakRelasjonTjeneste;

    TilknyttFagsakStegImpl() {
        // for CDI proxy
    }

    @Inject
    public TilknyttFagsakStegImpl(FagsakRepository fagsakRepository, // NOSONAR
            FagsakRelasjonTjeneste fagsakRelasjonTjeneste) {// NOSONAR
        this.fagsakRepository = fagsakRepository;
        this.fagsakRelasjonTjeneste = fagsakRelasjonTjeneste;
    }

    @Override
    public BehandleStegResultat utførSteg(BehandlingskontrollKontekst kontekst) {
        var fagsak = fagsakRepository.finnEksaktFagsak(kontekst.getFagsakId());
        if (!fagsakRelasjonTjeneste.finnRelasjonForHvisEksisterer(fagsak).isPresent()) {
            fagsakRelasjonTjeneste.opprettRelasjon(fagsak, Dekningsgrad._100);
        }
        return BehandleStegResultat.utførtUtenAksjonspunkter();
    }
}
