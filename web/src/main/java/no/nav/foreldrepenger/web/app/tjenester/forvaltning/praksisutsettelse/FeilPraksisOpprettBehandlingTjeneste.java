package no.nav.foreldrepenger.web.app.tjenester.forvaltning.praksisutsettelse;

import java.util.Optional;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import no.nav.foreldrepenger.behandling.revurdering.RevurderingTjeneste;
import no.nav.foreldrepenger.behandlingskontroll.FagsakYtelseTypeRef;
import no.nav.foreldrepenger.behandlingslager.behandling.Behandling;
import no.nav.foreldrepenger.behandlingslager.behandling.BehandlingStatus;
import no.nav.foreldrepenger.behandlingslager.behandling.BehandlingÅrsakType;
import no.nav.foreldrepenger.behandlingslager.behandling.repository.BehandlingRepository;
import no.nav.foreldrepenger.behandlingslager.behandling.repository.BehandlingRepositoryProvider;
import no.nav.foreldrepenger.behandlingslager.fagsak.Fagsak;
import no.nav.foreldrepenger.behandlingslager.fagsak.FagsakLåsRepository;
import no.nav.foreldrepenger.behandlingslager.fagsak.FagsakYtelseType;
import no.nav.foreldrepenger.behandlingsprosess.prosessering.BehandlingProsesseringTjeneste;
import no.nav.foreldrepenger.produksjonsstyring.behandlingenhet.BehandlendeEnhetTjeneste;

/**
 *  Opprette revurderinger der det er oppdaget feil praksis
 */
@ApplicationScoped
public class FeilPraksisOpprettBehandlingTjeneste {
    private static final Logger LOG = LoggerFactory.getLogger(FeilPraksisOpprettBehandlingTjeneste.class);

    private BehandlingRepository behandlingRepository;
    private FagsakLåsRepository fagsakLåsRepository;
    private RevurderingTjeneste revurderingTjeneste;
    private BehandlendeEnhetTjeneste behandlendeEnhetTjeneste;
    private BehandlingProsesseringTjeneste behandlingProsesseringTjeneste;

    @Inject
    public FeilPraksisOpprettBehandlingTjeneste(BehandlingRepositoryProvider behandlingRepositoryProvider,
                                                @FagsakYtelseTypeRef(FagsakYtelseType.FORELDREPENGER) RevurderingTjeneste revurderingTjeneste,
                                                BehandlendeEnhetTjeneste behandlendeEnhetTjeneste,
                                                BehandlingProsesseringTjeneste behandlingProsesseringTjeneste) {
        this.fagsakLåsRepository = behandlingRepositoryProvider.getFagsakLåsRepository();
        this.behandlingRepository = behandlingRepositoryProvider.getBehandlingRepository();
        this.behandlendeEnhetTjeneste = behandlendeEnhetTjeneste;
        this.behandlingProsesseringTjeneste = behandlingProsesseringTjeneste;
        this.revurderingTjeneste = revurderingTjeneste;
    }

    FeilPraksisOpprettBehandlingTjeneste() {
        // CDI
    }

    public void opprettBehandling(Fagsak fagsak) {
        if (finnÅpenYtelsesbehandling(fagsak).isPresent()) {
            LOG.info("FeilPraksisUtsettelse: Har åpen ytelsesbehandling saksnummer {}", fagsak.getSaksnummer());
            return;
        }

        var sisteVedtatte = behandlingRepository.finnSisteAvsluttedeIkkeHenlagteBehandling(fagsak.getId()).orElse(null);
        if (sisteVedtatte == null) {
            LOG.info("FeilPraksisUtsettelse: Fant ingen vedtatte behandlinger saksnummer {}", fagsak.getSaksnummer());
            return;
        }

        var enhet = behandlendeEnhetTjeneste.finnBehandlendeEnhetFra(sisteVedtatte);
        fagsakLåsRepository.taLås(fagsak.getId());
        var revurdering = revurderingTjeneste.opprettAutomatiskRevurdering(fagsak, BehandlingÅrsakType.FEIL_PRAKSIS_UTSETTELSE, enhet);
        behandlingProsesseringTjeneste.opprettTasksForStartBehandling(revurdering);
        LOG.info("FeilPraksisUtsettelse: Opprettet revurdering med behandlingId {} saksnummer {}", revurdering.getId(), fagsak.getSaksnummer());

    }

    private Optional<Behandling> finnÅpenYtelsesbehandling(Fagsak fagsak) {
        return behandlingRepository.hentSisteYtelsesBehandlingForFagsakId(fagsak.getId())
            .filter(b -> !BehandlingStatus.getFerdigbehandletStatuser().contains(b.getStatus()));
    }
}
