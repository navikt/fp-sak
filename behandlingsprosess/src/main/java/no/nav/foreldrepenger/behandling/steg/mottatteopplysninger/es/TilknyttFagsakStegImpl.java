package no.nav.foreldrepenger.behandling.steg.mottatteopplysninger.es;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import no.nav.foreldrepenger.behandling.steg.mottatteopplysninger.RegistrerFagsakEgenskaper;
import no.nav.foreldrepenger.behandling.steg.mottatteopplysninger.TilknyttFagsakSteg;
import no.nav.foreldrepenger.behandlingskontroll.BehandleStegResultat;
import no.nav.foreldrepenger.behandlingskontroll.BehandlingStegRef;
import no.nav.foreldrepenger.behandlingskontroll.BehandlingTypeRef;
import no.nav.foreldrepenger.behandlingskontroll.BehandlingskontrollKontekst;
import no.nav.foreldrepenger.behandlingskontroll.FagsakYtelseTypeRef;
import no.nav.foreldrepenger.behandlingslager.behandling.BehandlingStegType;
import no.nav.foreldrepenger.behandlingslager.behandling.repository.BehandlingRepository;
import no.nav.foreldrepenger.behandlingslager.fagsak.FagsakYtelseType;

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
        registrerFagsakEgenskaper.registrerFagsakEgenskaper(behandlingRepository.hentBehandling(kontekst.getBehandlingId()), false);
        return BehandleStegResultat.utførtUtenAksjonspunkter();
    }

}
