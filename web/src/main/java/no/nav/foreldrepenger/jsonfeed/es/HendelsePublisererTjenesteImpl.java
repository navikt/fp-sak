package no.nav.foreldrepenger.jsonfeed.es;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import no.nav.foreldrepenger.behandling.FagsakStatusEvent;
import no.nav.foreldrepenger.behandling.revurdering.etterkontroll.EtterkontrollRepository;
import no.nav.foreldrepenger.behandlingskontroll.FagsakYtelseTypeRef;
import no.nav.foreldrepenger.behandlingslager.behandling.Behandling;
import no.nav.foreldrepenger.behandlingslager.behandling.BehandlingsresultatRepository;
import no.nav.foreldrepenger.behandlingslager.behandling.repository.BehandlingRepositoryProvider;
import no.nav.foreldrepenger.behandlingslager.behandling.vedtak.BehandlingVedtak;
import no.nav.foreldrepenger.jsonfeed.AbstractHendelsePublisererTjeneste;

@ApplicationScoped
@FagsakYtelseTypeRef("ES")
public class HendelsePublisererTjenesteImpl extends AbstractHendelsePublisererTjeneste {

    // Det finnes ingen vedtaksfeed for Engangsstønad pr i dag

    public HendelsePublisererTjenesteImpl() {
        //CDI
    }

    @Inject
    public HendelsePublisererTjenesteImpl(BehandlingsresultatRepository behandlingsresultatRepository,
                                          EtterkontrollRepository etterkontrollRepository,
                                          BehandlingRepositoryProvider provider) {
        super(behandlingsresultatRepository, etterkontrollRepository, provider);
    }

    @Override
    protected void doLagreVedtak(BehandlingVedtak vedtak, Behandling behandling) {
    }

    @Override
    protected boolean hendelseEksistererAllerede(BehandlingVedtak vedtak) {
         return false;
    }

    @Override
    public void lagreFagsakAvsluttet(FagsakStatusEvent event) {
    }

    @Override
    protected boolean uttakFomEllerTomErEndret(Long orginalbehId, Long behandlingId) {
        return false;
    }
}
