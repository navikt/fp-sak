package no.nav.foreldrepenger.domene.vedtak.intern;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import no.nav.foreldrepenger.behandlingskontroll.BehandlingskontrollTjeneste;
import no.nav.foreldrepenger.behandlingslager.behandling.BehandlingStegType;
import no.nav.foreldrepenger.behandlingslager.behandling.repository.BehandlingRepository;
import no.nav.foreldrepenger.behandlingslager.behandling.repository.BehandlingRepositoryProvider;
import no.nav.foreldrepenger.behandlingslager.behandling.vedtak.BehandlingVedtakRepository;
import no.nav.foreldrepenger.behandlingslager.behandling.vedtak.IverksettingStatus;
import no.nav.foreldrepenger.behandlingsprosess.prosessering.BehandlingProsesseringTjeneste;
import no.nav.foreldrepenger.domene.vedtak.impl.BehandlingVedtakEventPubliserer;
import no.nav.foreldrepenger.domene.vedtak.impl.VurderBehandlingerUnderIverksettelse;

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
        LOG.info("Avslutter behandling inngang {}", behandlingId); //$NON-NLS-1$
        var kontekst = behandlingskontrollTjeneste.initBehandlingskontroll(behandlingId);
        var behandling = behandlingRepository.hentBehandling(behandlingId);

        var vedtak = behandlingVedtakRepository.hentForBehandling(behandling.getId());
        vedtak.setIverksettingStatus(IverksettingStatus.IVERKSATT);

        behandlingVedtakRepository.lagre(vedtak, kontekst.getSkriveLås());
        LOG.info("Avslutter behandling iverksatt vedtak: {}", behandlingId); //$NON-NLS-1$

        behandlingVedtakEventPubliserer.fireEvent(vedtak, behandling);

        LOG.info("Avslutter behandling gjenopptar: {}", behandlingId); //$NON-NLS-1$

        behandlingskontrollTjeneste.prosesserBehandlingGjenopptaHvisStegVenter(kontekst, BehandlingStegType.IVERKSETT_VEDTAK);

        LOG.info("Avslutter behandling har avsluttet behandling: {}", behandlingId); //$NON-NLS-1$

        // TODO (Fluoritt): Kunne vi flyttet dette ut i en Event observer (ref BehandlingStatusEvent) Hilsen FC.
        var ventendeBehandlingOpt = vurderBehandlingerUnderIverksettelse.finnBehandlingSomVenterIverksetting(behandling);
        ventendeBehandlingOpt.ifPresent(ventendeBehandling -> {
            LOG.info("Avslutter behandling fortsetter iverksetting av ventende behandling: {}", ventendeBehandling.getId()); //$NON-NLS-1$
            behandlingProsesseringTjeneste.opprettTasksForFortsettBehandling(ventendeBehandling);
        });

        LOG.info("Avslutter behandling utgang: {}", behandlingId); //$NON-NLS-1$
    }
}
