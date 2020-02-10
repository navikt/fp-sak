package no.nav.foreldrepenger.domene.vedtak.intern;

import java.util.Optional;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import no.nav.foreldrepenger.behandlingskontroll.BehandlingskontrollKontekst;
import no.nav.foreldrepenger.behandlingskontroll.BehandlingskontrollTjeneste;
import no.nav.foreldrepenger.behandlingslager.behandling.Behandling;
import no.nav.foreldrepenger.behandlingslager.behandling.BehandlingStegType;
import no.nav.foreldrepenger.behandlingslager.behandling.repository.BehandlingRepository;
import no.nav.foreldrepenger.behandlingslager.behandling.repository.BehandlingRepositoryFeil;
import no.nav.foreldrepenger.behandlingslager.behandling.repository.BehandlingRepositoryProvider;
import no.nav.foreldrepenger.behandlingslager.behandling.vedtak.BehandlingVedtak;
import no.nav.foreldrepenger.behandlingslager.behandling.vedtak.BehandlingVedtakRepository;
import no.nav.foreldrepenger.behandlingslager.behandling.vedtak.IverksettingStatus;
import no.nav.foreldrepenger.behandlingsprosess.prosessering.task.FortsettBehandlingTaskProperties;
import no.nav.foreldrepenger.domene.vedtak.impl.BehandlingVedtakEventPubliserer;
import no.nav.foreldrepenger.domene.vedtak.impl.VurderBehandlingerUnderIverksettelse;
import no.nav.vedtak.felles.prosesstask.api.ProsessTaskData;
import no.nav.vedtak.felles.prosesstask.api.ProsessTaskRepository;

@ApplicationScoped
public class AvsluttBehandling {

    private static final Logger log = LoggerFactory.getLogger(AvsluttBehandling.class);

    private BehandlingRepository behandlingRepository;
    private BehandlingskontrollTjeneste behandlingskontrollTjeneste;
    private BehandlingVedtakEventPubliserer behandlingVedtakEventPubliserer;
    private BehandlingVedtakRepository behandlingVedtakRepository;
    private ProsessTaskRepository prosessTaskRepository;
    private VurderBehandlingerUnderIverksettelse vurderBehandlingerUnderIverksettelse;

    public AvsluttBehandling() {
        // CDI
    }

    @Inject
    public AvsluttBehandling(BehandlingRepositoryProvider repositoryProvider,
                             BehandlingskontrollTjeneste behandlingskontrollTjeneste,
                             BehandlingVedtakEventPubliserer behandlingVedtakEventPubliserer,
                             VurderBehandlingerUnderIverksettelse vurderBehandlingerUnderIverksettelse,
                             ProsessTaskRepository prosessTaskRepository) {
        this.behandlingRepository = repositoryProvider.getBehandlingRepository();
        this.behandlingskontrollTjeneste = behandlingskontrollTjeneste;
        this.behandlingVedtakRepository = repositoryProvider.getBehandlingVedtakRepository();
        this.behandlingVedtakEventPubliserer = behandlingVedtakEventPubliserer;
        this.vurderBehandlingerUnderIverksettelse = vurderBehandlingerUnderIverksettelse;
        this.prosessTaskRepository = prosessTaskRepository;
    }

    void avsluttBehandling(Long behandlingId) {
        log.info("Avslutter behandling: {}", behandlingId); //$NON-NLS-1$
        BehandlingskontrollKontekst kontekst = behandlingskontrollTjeneste.initBehandlingskontroll(behandlingId);
        Behandling behandling = behandlingRepository.hentBehandling(behandlingId);

        BehandlingVedtak vedtak = behandlingVedtakRepository.hentBehandlingvedtakForBehandlingId(behandling.getId())
            .orElseThrow(() -> BehandlingRepositoryFeil.FACTORY.fantIkkeBehandlingVedtak(behandlingId).toException());
        vedtak.setIverksettingStatus(IverksettingStatus.IVERKSATT);

        behandlingVedtakRepository.lagre(vedtak, kontekst.getSkriveLås());
        behandlingVedtakEventPubliserer.fireEvent(vedtak, behandling);

        behandlingskontrollTjeneste.prosesserBehandlingGjenopptaHvisStegVenter(kontekst, BehandlingStegType.IVERKSETT_VEDTAK);

        log.info("Har avsluttet behandling: {}", behandlingId); //$NON-NLS-1$

        // TODO (Fluoritt): Kunne vi flyttet dette ut i en Event observer (ref BehandlingStatusEvent) Hilsen FC.
        Optional<Behandling> ventendeBehandlingOpt = vurderBehandlingerUnderIverksettelse.finnBehandlingSomVenterIverksetting(behandling);
        ventendeBehandlingOpt.ifPresent(ventendeBehandling -> {
            log.info("Fortsetter iverksetting av ventende behandling: {}", ventendeBehandling.getId()); //$NON-NLS-1$
            opprettTaskForProsesserBehandling(ventendeBehandling);
        });
    }

    private void opprettTaskForProsesserBehandling(Behandling behandling) {
        ProsessTaskData prosessTaskData = new ProsessTaskData(FortsettBehandlingTaskProperties.TASKTYPE);
        prosessTaskData.setBehandling(behandling.getFagsakId(), behandling.getId(), behandling.getAktørId().getId());
        prosessTaskData.setCallIdFraEksisterende();
        prosessTaskRepository.lagre(prosessTaskData);
    }
}
