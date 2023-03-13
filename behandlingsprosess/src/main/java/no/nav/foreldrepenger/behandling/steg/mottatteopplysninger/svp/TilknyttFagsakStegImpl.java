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
import no.nav.foreldrepenger.behandlingslager.behandling.BehandlingStegType;
import no.nav.foreldrepenger.behandlingslager.behandling.BehandlingType;
import no.nav.foreldrepenger.behandlingslager.fagsak.Dekningsgrad;
import no.nav.foreldrepenger.behandlingslager.fagsak.FagsakRepository;
import no.nav.foreldrepenger.behandlingslager.fagsak.FagsakYtelseType;

@BehandlingStegRef(BehandlingStegType.INNHENT_SØKNADOPP)
@BehandlingTypeRef(BehandlingType.FØRSTEGANGSSØKNAD)
@FagsakYtelseTypeRef(FagsakYtelseType.SVANGERSKAPSPENGER)
@ApplicationScoped
public class TilknyttFagsakStegImpl implements TilknyttFagsakSteg {

    private FagsakRepository fagsakRepository;
    private FagsakRelasjonTjeneste fagsakRelasjonTjeneste;

    TilknyttFagsakStegImpl() {
        // for CDI proxy
    }

    @Inject
    public TilknyttFagsakStegImpl(FagsakRepository fagsakRepository,
            FagsakRelasjonTjeneste fagsakRelasjonTjeneste) {
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
