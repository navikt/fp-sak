package no.nav.foreldrepenger.jsonfeed.observer;

import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.event.Observes;
import javax.enterprise.inject.Any;
import javax.enterprise.inject.Instance;
import javax.inject.Inject;

import no.nav.foreldrepenger.behandling.FagsakStatusEvent;
import no.nav.foreldrepenger.behandlingskontroll.FagsakYtelseTypeRef;
import no.nav.foreldrepenger.behandlingslager.behandling.vedtak.BehandlingVedtakEvent;
import no.nav.foreldrepenger.behandlingslager.behandling.vedtak.IverksettingStatus;
import no.nav.foreldrepenger.behandlingslager.fagsak.Fagsak;
import no.nav.foreldrepenger.behandlingslager.fagsak.FagsakRepository;
import no.nav.foreldrepenger.behandlingslager.fagsak.FagsakStatus;
import no.nav.foreldrepenger.jsonfeed.HendelsePublisererTjeneste;

@ApplicationScoped
public class HendelsePublisererEventObserver {

    private Instance<HendelsePublisererTjeneste> hendelsePubliserere;
    private FagsakRepository fagsakRepository;

    public HendelsePublisererEventObserver() {
        //Classic Design Institute
    }

    @Inject
    public HendelsePublisererEventObserver(@Any Instance<HendelsePublisererTjeneste> hendelsePubliserere,
                                           FagsakRepository fagsakRepository) {
        this.hendelsePubliserere = hendelsePubliserere;
        this.fagsakRepository = fagsakRepository;
    }

    public void observerBehandlingVedtak(@Observes BehandlingVedtakEvent event) {
        if (IverksettingStatus.IVERKSATT.equals(event.getVedtak().getIverksettingStatus())) {
            HendelsePublisererTjeneste hendelsePubliserer = finnFagsak(event.getFagsakId());
            hendelsePubliserer.lagreVedtak(event.getVedtak());
        }
    }

    public void observerFagsakAvsluttetEvent(@Observes FagsakStatusEvent event) {
        HendelsePublisererTjeneste hendelsePubliserer = finnFagsak(event.getFagsakId());
        if (FagsakStatus.LØPENDE.equals(event.getForrigeStatus()) && FagsakStatus.AVSLUTTET.equals(event.getNyStatus())) {
            hendelsePubliserer.lagreFagsakAvsluttet(event);
        }
    }

    private HendelsePublisererTjeneste finnFagsak(Long fagsakId) {
        Fagsak fagsak = fagsakRepository.finnEksaktFagsak(fagsakId);
        return FagsakYtelseTypeRef.Lookup.find(hendelsePubliserere, fagsak.getYtelseType())
            .orElseThrow(() -> new IllegalStateException("Ingen implementasjoner funnet for ytelse: " + fagsak.getYtelseType().getKode()));
    }
}
