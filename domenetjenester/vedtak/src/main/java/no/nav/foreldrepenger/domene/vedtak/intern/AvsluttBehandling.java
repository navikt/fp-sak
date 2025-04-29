package no.nav.foreldrepenger.domene.vedtak.intern;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import no.nav.foreldrepenger.behandlingslager.fagsak.FagsakYtelseType;
import no.nav.foreldrepenger.konfig.Environment;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import no.nav.foreldrepenger.behandling.BehandlingEventPubliserer;
import no.nav.foreldrepenger.behandling.BehandlingReferanse;
import no.nav.foreldrepenger.behandlingskontroll.BehandlingskontrollTjeneste;
import no.nav.foreldrepenger.behandlingslager.behandling.BehandlingStegType;
import no.nav.foreldrepenger.behandlingslager.behandling.events.BehandlingVedtakEvent;
import no.nav.foreldrepenger.behandlingslager.behandling.repository.BehandlingRepository;
import no.nav.foreldrepenger.behandlingslager.behandling.repository.BehandlingRepositoryProvider;
import no.nav.foreldrepenger.behandlingslager.behandling.vedtak.BehandlingVedtakRepository;
import no.nav.foreldrepenger.behandlingslager.behandling.vedtak.IverksettingStatus;
import no.nav.foreldrepenger.behandlingsprosess.prosessering.BehandlingProsesseringTjeneste;
import no.nav.foreldrepenger.domene.arbeidsforhold.InntektArbeidYtelseTjeneste;
import no.nav.foreldrepenger.domene.prosess.BeregningTjeneste;
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
    private BeregningTjeneste beregningTjeneste;
    private InntektArbeidYtelseTjeneste iayTjeneste;

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
                             BeregningTjeneste beregningTjeneste,
                             InntektArbeidYtelseTjeneste iayTjeneste) {
        this.behandlingRepository = repositoryProvider.getBehandlingRepository();
        this.behandlingskontrollTjeneste = behandlingskontrollTjeneste;
        this.behandlingVedtakRepository = repositoryProvider.getBehandlingVedtakRepository();
        this.behandlingEventPubliserer = behandlingEventPubliserer;
        this.vurderBehandlingerUnderIverksettelse = vurderBehandlingerUnderIverksettelse;
        this.behandlingProsesseringTjeneste = behandlingProsesseringTjeneste;
        this.oppdatereFagsakRelasjonVedVedtak = oppdatereFagsakRelasjonVedVedtak;
        this.beregningTjeneste = beregningTjeneste;
        this.iayTjeneste = iayTjeneste;
    }

    void avsluttBehandling(Long behandlingId) {
        LOG.info("Avslutter behandling inngang {}", behandlingId);
        var kontekst = behandlingskontrollTjeneste.initBehandlingskontroll(behandlingId);
        var behandling = behandlingRepository.hentBehandling(behandlingId);

        oppdatereFagsakRelasjonVedVedtak.oppdaterRelasjonVedVedtattBehandling(behandling);

        var vedtak = behandlingVedtakRepository.hentForBehandling(behandling.getId());
        vedtak.setIverksettingStatus(IverksettingStatus.IVERKSATT);

        behandlingVedtakRepository.lagre(vedtak, kontekst.getSkriveLås());
        LOG.info("Avslutter behandling iverksatt vedtak: {}", behandlingId);

        behandlingEventPubliserer.publiserBehandlingEvent(new BehandlingVedtakEvent(vedtak, behandling));

        LOG.info("Avslutter behandling gjenopptar: {}", behandlingId);

        behandlingskontrollTjeneste.prosesserBehandlingGjenopptaHvisStegVenter(kontekst, BehandlingStegType.IVERKSETT_VEDTAK);

        LOG.info("Avslutter behandling har avsluttet behandling: {}", behandlingId);

        // TODO (Fluoritt): Kunne vi flyttet dette ut i en Event observer (ref BehandlingStatusEvent) Hilsen FC.
        var ventendeBehandlingOpt = vurderBehandlingerUnderIverksettelse.finnBehandlingSomVenterIverksetting(behandling);
        ventendeBehandlingOpt.ifPresent(ventendeBehandling -> {
            LOG.info("Avslutter behandling fortsetter iverksetting av ventende behandling: {}", ventendeBehandling.getId());
            behandlingProsesseringTjeneste.opprettTasksForFortsettBehandling(ventendeBehandling);
        });

        if (behandling.erYtelseBehandling() && !FagsakYtelseType.ENGANGSTØNAD.equals(behandling.getFagsakYtelseType())) {
            iayTjeneste.avslutt(behandlingId);
            beregningTjeneste.avslutt(BehandlingReferanse.fra(behandling));
        }

        LOG.info("Avslutter behandling utgang: {}", behandlingId);
    }
}
