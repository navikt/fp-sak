package no.nav.foreldrepenger.behandling.steg.mottatteopplysninger.es;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import no.nav.foreldrepenger.behandling.steg.mottatteopplysninger.TilknyttFagsakSteg;
import no.nav.foreldrepenger.behandlingskontroll.*;
import no.nav.foreldrepenger.behandlingslager.behandling.BehandlingStegType;
import no.nav.foreldrepenger.behandlingslager.behandling.repository.BehandlingRepository;
import no.nav.foreldrepenger.behandlingslager.fagsak.FagsakYtelseType;
import no.nav.foreldrepenger.produksjonsstyring.behandlingenhet.RegistrerFagsakEgenskaper;

@BehandlingStegRef(BehandlingStegType.INNHENT_SØKNADOPP)
@BehandlingTypeRef
@FagsakYtelseTypeRef(FagsakYtelseType.ENGANGSTØNAD)
@ApplicationScoped
public class TilknyttFagsakStegImpl implements TilknyttFagsakSteg {

    private BehandlingRepository behandlingRepository;
    private RegistrerFagsakEgenskaper registrerFagsakEgenskaper;

    TilknyttFagsakStegImpl() {
        // for CDI proxy
    }


    @Inject
    public TilknyttFagsakStegImpl(BehandlingRepository behandlingRepository, RegistrerFagsakEgenskaper registrerFagsakEgenskaper) {
        this.behandlingRepository  = behandlingRepository;
        this.registrerFagsakEgenskaper = registrerFagsakEgenskaper;
    }

    @Override
    public BehandleStegResultat utførSteg(BehandlingskontrollKontekst kontekst) {
        registrerFagsakEgenskaper.fagsakEgenskaperFraSøknad(behandlingRepository.hentBehandling(kontekst.getBehandlingId()), false);
        return BehandleStegResultat.utførtUtenAksjonspunkter();
    }

}
