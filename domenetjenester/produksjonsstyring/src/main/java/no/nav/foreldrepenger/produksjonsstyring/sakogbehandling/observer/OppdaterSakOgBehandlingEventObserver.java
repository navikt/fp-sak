package no.nav.foreldrepenger.produksjonsstyring.sakogbehandling.observer;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.event.Observes;
import jakarta.inject.Inject;

import no.nav.foreldrepenger.behandlingskontroll.events.BehandlingStatusEvent;
import no.nav.foreldrepenger.behandlingskontroll.events.BehandlingStatusEvent.BehandlingAvsluttetEvent;
import no.nav.foreldrepenger.behandlingskontroll.events.BehandlingStatusEvent.BehandlingOpprettetEvent;
import no.nav.foreldrepenger.behandlingslager.behandling.Behandling;
import no.nav.foreldrepenger.behandlingslager.behandling.BehandlingStatus;
import no.nav.foreldrepenger.behandlingslager.behandling.BehandlingTema;
import no.nav.foreldrepenger.behandlingslager.behandling.Tema;
import no.nav.foreldrepenger.behandlingslager.behandling.familiehendelse.FamilieHendelseGrunnlagEntitet;
import no.nav.foreldrepenger.behandlingslager.behandling.familiehendelse.FamilieHendelseRepository;
import no.nav.foreldrepenger.behandlingslager.behandling.repository.BehandlingRepository;
import no.nav.foreldrepenger.behandlingslager.behandling.repository.BehandlingRepositoryProvider;
import no.nav.foreldrepenger.produksjonsstyring.sakogbehandling.task.SakOgBehandlingTask;
import no.nav.vedtak.felles.prosesstask.api.ProsessTaskData;
import no.nav.vedtak.felles.prosesstask.api.ProsessTaskTjeneste;

/**
 * Observerer relevante status endringer på behandling og fagsak og varsler til Sakogbehandling (eksternt system).
 *
 */
@ApplicationScoped
public class OppdaterSakOgBehandlingEventObserver {

    private FamilieHendelseRepository familieGrunnlagRepository;
    private BehandlingRepository behandlingRepository;
    private ProsessTaskTjeneste taskTjeneste;

    static final String FORELDREPENGER_SAKSTEMA = Tema.FOR.getOffisiellKode();

    @Inject
    public OppdaterSakOgBehandlingEventObserver(BehandlingRepositoryProvider repositoryProvider,
                                                ProsessTaskTjeneste taskTjeneste) {
        this.behandlingRepository = repositoryProvider.getBehandlingRepository();
        this.taskTjeneste = taskTjeneste;
        this.familieGrunnlagRepository = repositoryProvider.getFamilieHendelseRepository();
    }

    public void observerBehandlingStatus(@Observes BehandlingAvsluttetEvent event) {
        oppdaterSakOgBehandlingVedBehandlingsstatusEndring(event);
    }

    public void observerBehandlingStatus(@Observes BehandlingOpprettetEvent event) {
        oppdaterSakOgBehandlingVedBehandlingsstatusEndring(event);
    }

    /**
     * Sender melding til køen som systemet "Sak og behandling" lytter på
     */
    private void oppdaterSakOgBehandlingVedBehandlingsstatusEndring(BehandlingStatusEvent event) {

        var nyStatus = event.getNyStatus();

        var kontekst = event.getKontekst();
        var behandling = behandlingRepository.hentBehandling(kontekst.getBehandlingId());

        sendMeldingTilSakOgBehandling(behandling, nyStatus);

    }

    private void sendMeldingTilSakOgBehandling(Behandling behandling, BehandlingStatus nyStatus) {
        var behandlingTema = behandlingTemaFraBehandling(behandling);
        if (behandlingTema.equals(BehandlingTema.UDEFINERT)) {
            throw new IllegalStateException("Utviklerfeil: Finner ikke behandlingstema for fagsak");
        }

        var prosessTaskData = ProsessTaskData.forProsessTask(SakOgBehandlingTask.class);
        prosessTaskData.setBehandling(behandling.getFagsakId(), behandling.getId(), behandling.getAktørId().getId());
        prosessTaskData.setCallIdFraEksisterende();
        taskTjeneste.lagre(prosessTaskData);
    }

    private BehandlingTema behandlingTemaFraBehandling(Behandling sisteBehandling) {
        var grunnlag = familieGrunnlagRepository.hentAggregatHvisEksisterer(sisteBehandling.getId());
        return BehandlingTema.fraFagsak(sisteBehandling.getFagsak(), grunnlag.map(FamilieHendelseGrunnlagEntitet::getSøknadVersjon).orElse(null));
    }
}
