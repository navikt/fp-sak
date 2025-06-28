package no.nav.foreldrepenger.domene.vedtak.intern;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import no.nav.foreldrepenger.behandling.BehandlingEventPubliserer;
import no.nav.foreldrepenger.behandlingskontroll.BehandlingskontrollTjeneste;
import no.nav.foreldrepenger.behandlingslager.behandling.BehandlingStegType;
import no.nav.foreldrepenger.behandlingslager.behandling.events.BehandlingVedtakEvent;
import no.nav.foreldrepenger.behandlingslager.behandling.repository.BehandlingRepository;
import no.nav.foreldrepenger.behandlingslager.behandling.repository.BehandlingRepositoryProvider;
import no.nav.foreldrepenger.behandlingslager.behandling.vedtak.BehandlingVedtakRepository;
import no.nav.foreldrepenger.behandlingslager.behandling.vedtak.IverksettingStatus;
import no.nav.foreldrepenger.behandlingslager.fagsak.FagsakEgenskapRepository;
import no.nav.foreldrepenger.behandlingslager.fagsak.egenskaper.FagsakMarkering;
import no.nav.foreldrepenger.behandlingsprosess.prosessering.BehandlingProsesseringTjeneste;
import no.nav.foreldrepenger.domene.vedtak.impl.VurderBehandlingerUnderIverksettelse;

@ApplicationScoped
public class AvsluttBehandling {

    private static final Logger LOG = LoggerFactory.getLogger(AvsluttBehandling.class);

    private BehandlingRepository behandlingRepository;
    private BehandlingskontrollTjeneste behandlingskontrollTjeneste;
    private BehandlingEventPubliserer behandlingEventPubliserer;
    private BehandlingVedtakRepository behandlingVedtakRepository;
    private BehandlingProsesseringTjeneste behandlingProsesseringTjeneste;
    private VurderBehandlingerUnderIverksettelse vurderBehandlingerUnderIverksettelse;
    private OppdatereFagsakRelasjonVedVedtak oppdatereFagsakRelasjonVedVedtak;
    private FagsakEgenskapRepository fagsakEgenskapRepository;

    public AvsluttBehandling() {
        // CDI
    }

    @Inject
    public AvsluttBehandling(BehandlingRepositoryProvider repositoryProvider,
                             BehandlingskontrollTjeneste behandlingskontrollTjeneste,
                             BehandlingEventPubliserer behandlingEventPubliserer,
                             VurderBehandlingerUnderIverksettelse vurderBehandlingerUnderIverksettelse,
                             BehandlingProsesseringTjeneste behandlingProsesseringTjeneste,
                             OppdatereFagsakRelasjonVedVedtak oppdatereFagsakRelasjonVedVedtak,
                             FagsakEgenskapRepository fagsakEgenskapRepository) {
        this.behandlingRepository = repositoryProvider.getBehandlingRepository();
        this.behandlingskontrollTjeneste = behandlingskontrollTjeneste;
        this.behandlingVedtakRepository = repositoryProvider.getBehandlingVedtakRepository();
        this.behandlingEventPubliserer = behandlingEventPubliserer;
        this.vurderBehandlingerUnderIverksettelse = vurderBehandlingerUnderIverksettelse;
        this.behandlingProsesseringTjeneste = behandlingProsesseringTjeneste;
        this.oppdatereFagsakRelasjonVedVedtak = oppdatereFagsakRelasjonVedVedtak;
        this.fagsakEgenskapRepository = fagsakEgenskapRepository;
    }

    void avsluttBehandling(Long behandlingId) {
        LOG.info("Avslutter behandling inngang {}", behandlingId);
        var lås = behandlingRepository.taSkriveLås(behandlingId);
        var behandling = behandlingRepository.hentBehandling(behandlingId);
        var kontekst = behandlingskontrollTjeneste.initBehandlingskontroll(behandling, lås);

        oppdatereFagsakRelasjonVedVedtak.oppdaterRelasjonVedVedtattBehandling(behandling);

        var vedtak = behandlingVedtakRepository.hentForBehandling(behandling.getId());
        vedtak.setIverksettingStatus(IverksettingStatus.IVERKSATT);

        behandlingVedtakRepository.lagre(vedtak, kontekst.getSkriveLås());
        LOG.info("Avslutter behandling iverksatt vedtak: {}", behandlingId);

        behandlingEventPubliserer.publiserBehandlingEvent(new BehandlingVedtakEvent(vedtak, behandling));

        LOG.info("Avslutter behandling gjenopptar: {}", behandlingId);

        behandlingskontrollTjeneste.prosesserBehandlingGjenopptaHvisStegVenter(kontekst, BehandlingStegType.IVERKSETT_VEDTAK);

        LOG.info("Avslutter behandling har avsluttet behandling: {}", behandlingId);

        if (fagsakEgenskapRepository.harFagsakMarkering(behandling.getFagsakId(), FagsakMarkering.HASTER) && behandling.erYtelseBehandling()) {
            var andreÅpneYtelsesbehandlinger = behandlingRepository.hentÅpneYtelseBehandlingerForFagsakId(behandling.getFagsakId()).stream()
                .anyMatch(b -> !b.getId().equals(behandlingId));
            if (!andreÅpneYtelsesbehandlinger) {
                fagsakEgenskapRepository.fjernFagsakMarkering(behandling.getFagsakId(), FagsakMarkering.HASTER);
            }
        }

        // TODO (Fluoritt): Kunne vi flyttet dette ut i en Event observer (ref BehandlingStatusEvent) Hilsen FC.
        var ventendeBehandlingOpt = vurderBehandlingerUnderIverksettelse.finnBehandlingSomVenterIverksetting(behandling);
        ventendeBehandlingOpt.ifPresent(ventendeBehandling -> {
            LOG.info("Avslutter behandling fortsetter iverksetting av ventende behandling: {}", ventendeBehandling.getId());
            behandlingProsesseringTjeneste.opprettTasksForFortsettBehandling(ventendeBehandling);
        });
        LOG.info("Avslutter behandling utgang: {}", behandlingId);
    }
}
