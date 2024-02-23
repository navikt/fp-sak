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
import no.nav.foreldrepenger.behandlingslager.behandling.BehandlingType;
import no.nav.foreldrepenger.behandlingslager.behandling.BehandlingÅrsakType;
import no.nav.foreldrepenger.behandlingslager.behandling.repository.BehandlingRepository;
import no.nav.foreldrepenger.behandlingslager.behandling.repository.BehandlingRepositoryProvider;
import no.nav.foreldrepenger.behandlingslager.behandling.vedtak.BehandlingVedtak;
import no.nav.foreldrepenger.behandlingslager.behandling.vedtak.BehandlingVedtakRepository;
import no.nav.foreldrepenger.behandlingslager.behandling.vedtak.VedtakResultatType;
import no.nav.foreldrepenger.behandlingslager.fagsak.Fagsak;
import no.nav.foreldrepenger.behandlingslager.fagsak.FagsakLåsRepository;
import no.nav.foreldrepenger.behandlingslager.fagsak.FagsakYtelseType;
import no.nav.foreldrepenger.behandlingsprosess.prosessering.BehandlingOpprettingTjeneste;
import no.nav.foreldrepenger.produksjonsstyring.behandlingenhet.BehandlendeEnhetTjeneste;

/**
 *  Dersom det er identifisert overlapp av VurderOpphørAvYtelser, vil denne tjenesten opprette en
 *  "vurder konsekvens for ytelse"-oppgave i Gosys, og en revurdering med egen årsak slik at saksbehandler kan vurdere
 *  om opphør skal gjennomføres eller ikke. Saksbehandling må skje manuelt, og fritekstbrev må benyttes for opphør av løpende sak.
 */
@ApplicationScoped
public class FeilPraksisOpprettBehandlingTjeneste {
    private static final Logger LOG = LoggerFactory.getLogger(FeilPraksisOpprettBehandlingTjeneste.class);

    private BehandlingRepository behandlingRepository;
    private FagsakLåsRepository fagsakLåsRepository;
    private BehandlingVedtakRepository behandlingVedtakRepository;
    private BehandlingOpprettingTjeneste behandlingOpprettingTjeneste;
    private RevurderingTjeneste revurderingTjeneste;
    private BehandlendeEnhetTjeneste behandlendeEnhetTjeneste;

    @Inject
    public FeilPraksisOpprettBehandlingTjeneste(BehandlingRepositoryProvider behandlingRepositoryProvider,
                                                BehandlingOpprettingTjeneste behandlingOpprettingTjeneste,
                                                @FagsakYtelseTypeRef(FagsakYtelseType.FORELDREPENGER) RevurderingTjeneste revurderingTjeneste,
                                                BehandlendeEnhetTjeneste behandlendeEnhetTjeneste) {
        this.fagsakLåsRepository = behandlingRepositoryProvider.getFagsakLåsRepository();
        this.behandlingRepository = behandlingRepositoryProvider.getBehandlingRepository();
        this.behandlingVedtakRepository = behandlingRepositoryProvider.getBehandlingVedtakRepository();
        this.behandlingOpprettingTjeneste = behandlingOpprettingTjeneste;
        this.behandlendeEnhetTjeneste = behandlendeEnhetTjeneste;
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
        var sisteVedtak = behandlingVedtakRepository.hentForBehandlingHvisEksisterer(sisteVedtatte.getId())
            .map(BehandlingVedtak::getVedtakResultatType).orElse(VedtakResultatType.UDEFINERT);
        if (VedtakResultatType.UDEFINERT.equals(sisteVedtak)) {
            LOG.info("FeilPraksisUtsettelse: Fant ikke siste vedtak saksnummer {}", fagsak.getSaksnummer());
            return;
        }
        var enhet = behandlendeEnhetTjeneste.finnBehandlendeEnhetFra(sisteVedtatte);
        if (VedtakResultatType.AVSLAG.equals(sisteVedtak)) {
            var behandling = behandlingOpprettingTjeneste.opprettBehandling(fagsak, BehandlingType.FØRSTEGANGSSØKNAD, enhet, BehandlingÅrsakType.FEIL_PRAKSIS_UTSETTELSE);
            behandlingOpprettingTjeneste.asynkStartBehandlingsprosess(behandling);
            LOG.info("FeilPraksisUtsettelse: Opprettet førstegangsbehandling med behandlingId {} saksnummer {}", behandling.getId(), fagsak.getSaksnummer());
        } else {
            fagsakLåsRepository.taLås(fagsak.getId());
            var revurdering = revurderingTjeneste.opprettAutomatiskRevurdering(fagsak, BehandlingÅrsakType.FEIL_PRAKSIS_UTSETTELSE, enhet);
            behandlingOpprettingTjeneste.asynkStartBehandlingsprosess(revurdering);
            LOG.info("FeilPraksisUtsettelse: Opprettet revurdering med behandlingId {} saksnummer {}", revurdering.getId(), fagsak.getSaksnummer());
        }

    }

    private Optional<Behandling> finnÅpenYtelsesbehandling(Fagsak fagsak) {
        return behandlingRepository.hentSisteYtelsesBehandlingForFagsakId(fagsak.getId())
            .filter(b -> !BehandlingStatus.getFerdigbehandletStatuser().contains(b.getStatus()));
    }
}
