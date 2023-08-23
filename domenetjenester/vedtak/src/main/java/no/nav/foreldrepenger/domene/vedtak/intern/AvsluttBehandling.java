package no.nav.foreldrepenger.domene.vedtak.intern;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import no.nav.foreldrepenger.behandlingskontroll.BehandlingskontrollTjeneste;
import no.nav.foreldrepenger.behandlingslager.behandling.BehandlingStegType;
import no.nav.foreldrepenger.behandlingslager.behandling.repository.BehandlingRepository;
import no.nav.foreldrepenger.behandlingslager.behandling.repository.BehandlingRepositoryProvider;
import no.nav.foreldrepenger.behandlingslager.behandling.vedtak.BehandlingVedtakRepository;
import no.nav.foreldrepenger.behandlingslager.behandling.vedtak.IverksettingStatus;
import no.nav.foreldrepenger.behandlingsprosess.prosessering.BehandlingProsesseringTjeneste;
import no.nav.foreldrepenger.domene.vedtak.impl.BehandlingVedtakEventPubliserer;
import no.nav.foreldrepenger.domene.vedtak.impl.VurderBehandlingerUnderIverksettelse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@ApplicationScoped
public class AvsluttBehandling {

    private static final Logger LOG = LoggerFactory.getLogger(AvsluttBehandling.class);

    private BehandlingRepository behandlingRepository;
    private BehandlingskontrollTjeneste behandlingskontrollTjeneste;
    private BehandlingVedtakEventPubliserer behandlingVedtakEventPubliserer;
    private BehandlingVedtakRepository behandlingVedtakRepository;
    private BehandlingProsesseringTjeneste behandlingProsesseringTjeneste;
    private VurderBehandlingerUnderIverksettelse vurderBehandlingerUnderIverksettelse;

    public AvsluttBehandling() {
        // CDI
    }

    @Inject
    public AvsluttBehandling(BehandlingRepositoryProvider repositoryProvider,
                             BehandlingskontrollTjeneste behandlingskontrollTjeneste,
                             BehandlingVedtakEventPubliserer behandlingVedtakEventPubliserer,
                             VurderBehandlingerUnderIverksettelse vurderBehandlingerUnderIverksettelse,
                             BehandlingProsesseringTjeneste behandlingProsesseringTjeneste) {
        this.behandlingRepository = repositoryProvider.getBehandlingRepository();
        this.behandlingskontrollTjeneste = behandlingskontrollTjeneste;
        this.behandlingVedtakRepository = repositoryProvider.getBehandlingVedtakRepository();
        this.behandlingVedtakEventPubliserer = behandlingVedtakEventPubliserer;
        this.vurderBehandlingerUnderIverksettelse = vurderBehandlingerUnderIverksettelse;
        this.behandlingProsesseringTjeneste = behandlingProsesseringTjeneste;
    }

    void avsluttBehandling(Long behandlingId) {
        LOG.info("Avslutter behandling inngang {}", behandlingId);
        var kontekst = behandlingskontrollTjeneste.initBehandlingskontroll(behandlingId);
        var behandling = behandlingRepository.hentBehandling(behandlingId);

        var vedtak = behandlingVedtakRepository.hentForBehandling(behandling.getId());
        vedtak.setIverksettingStatus(IverksettingStatus.IVERKSATT);

        behandlingVedtakRepository.lagre(vedtak, kontekst.getSkriveLås());
        LOG.info("Avslutter behandling iverksatt vedtak: {}", behandlingId);

        behandlingVedtakEventPubliserer.fireEvent(vedtak, behandling);

        LOG.info("Avslutter behandling gjenopptar: {}", behandlingId);

        behandlingskontrollTjeneste.prosesserBehandlingGjenopptaHvisStegVenter(kontekst, BehandlingStegType.IVERKSETT_VEDTAK);

        LOG.info("Avslutter behandling har avsluttet behandling: {}", behandlingId);

        // TODO (Fluoritt): Kunne vi flyttet dette ut i en Event observer (ref BehandlingStatusEvent) Hilsen FC.
        var ventendeBehandlingOpt = vurderBehandlingerUnderIverksettelse.finnBehandlingSomVenterIverksetting(behandling);
        ventendeBehandlingOpt.ifPresent(ventendeBehandling -> {
            LOG.info("Avslutter behandling fortsetter iverksetting av ventende behandling: {}", ventendeBehandling.getId());
            behandlingProsesseringTjeneste.opprettTasksForFortsettBehandling(ventendeBehandling);
        });

        LOG.info("Avslutter behandling utgang: {}", behandlingId);
    }
}
