package no.nav.foreldrepenger.web.app.tjenester.forvaltning;

import java.util.List;
import java.util.Optional;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import javax.validation.Valid;
import javax.validation.constraints.NotNull;
import javax.ws.rs.core.Response;

import io.swagger.v3.oas.annotations.Parameter;
import no.nav.foreldrepenger.behandling.FagsakRelasjonTjeneste;
import no.nav.foreldrepenger.behandling.revurdering.RevurderingTjeneste;
import no.nav.foreldrepenger.behandling.revurdering.ytelse.UttakInputTjeneste;
import no.nav.foreldrepenger.behandlingskontroll.FagsakYtelseTypeRef;
import no.nav.foreldrepenger.behandlingslager.behandling.Behandling;
import no.nav.foreldrepenger.behandlingslager.behandling.BehandlingÅrsakType;
import no.nav.foreldrepenger.behandlingslager.behandling.historikk.HistorikkAktør;
import no.nav.foreldrepenger.behandlingslager.behandling.historikk.HistorikkEndretFeltType;
import no.nav.foreldrepenger.behandlingslager.behandling.historikk.HistorikkRepository;
import no.nav.foreldrepenger.behandlingslager.behandling.historikk.Historikkinnslag;
import no.nav.foreldrepenger.behandlingslager.behandling.historikk.HistorikkinnslagType;
import no.nav.foreldrepenger.behandlingslager.behandling.repository.BehandlingRepository;
import no.nav.foreldrepenger.behandlingslager.behandling.repository.BehandlingRepositoryProvider;
import no.nav.foreldrepenger.behandlingslager.fagsak.Dekningsgrad;
import no.nav.foreldrepenger.behandlingslager.fagsak.Fagsak;
import no.nav.foreldrepenger.behandlingslager.fagsak.FagsakRelasjon;
import no.nav.foreldrepenger.behandlingslager.fagsak.FagsakRepository;
import no.nav.foreldrepenger.behandlingsprosess.prosessering.task.StartBehandlingTask;
import no.nav.foreldrepenger.domene.typer.Saksnummer;
import no.nav.foreldrepenger.domene.uttak.beregnkontoer.BeregnStønadskontoerTjeneste;
import no.nav.foreldrepenger.historikk.HistorikkInnslagTekstBuilder;
import no.nav.foreldrepenger.produksjonsstyring.behandlingenhet.BehandlendeEnhetTjeneste;
import no.nav.vedtak.felles.prosesstask.api.ProsessTaskData;
import no.nav.vedtak.felles.prosesstask.api.ProsessTaskRepository;

@ApplicationScoped
public class OverstyrDekningsgradTjeneste {
    private BehandlingRepository behandlingRepository;
    private FagsakRepository fagsakRepository;
    private FagsakRelasjonTjeneste fagsakRelasjonTjeneste;
    private HistorikkRepository historikkRepository;
    private ProsessTaskRepository prosessTaskRepository;
    private BehandlendeEnhetTjeneste behandlendeEnhetTjeneste;
    private RevurderingTjeneste revurderingTjeneste;
    private BeregnStønadskontoerTjeneste beregnStønadskontoerTjeneste;
    private UttakInputTjeneste uttakInputTjeneste;

    OverstyrDekningsgradTjeneste() {
        // for CDI proxy
    }

    @Inject
    public OverstyrDekningsgradTjeneste(BehandlingRepositoryProvider repositoryProvider,
                                        ProsessTaskRepository prosessTaskRepository,
                                        BehandlendeEnhetTjeneste behandlendeEnhetTjeneste,
                                        @FagsakYtelseTypeRef("FP") RevurderingTjeneste revurderingTjeneste,
                                        BeregnStønadskontoerTjeneste beregnStønadskontoerTjeneste,
                                        UttakInputTjeneste uttakInputTjeneste,
                                        FagsakRelasjonTjeneste fagsakRelasjonTjeneste) {
        this.behandlingRepository = repositoryProvider.getBehandlingRepository();
        this.fagsakRepository = repositoryProvider.getFagsakRepository();
        this.fagsakRelasjonTjeneste = fagsakRelasjonTjeneste;
        this.historikkRepository = repositoryProvider.getHistorikkRepository();
        this.prosessTaskRepository = prosessTaskRepository;
        this.behandlendeEnhetTjeneste = behandlendeEnhetTjeneste;
        this.revurderingTjeneste = revurderingTjeneste;
        this.beregnStønadskontoerTjeneste = beregnStønadskontoerTjeneste;
        this.uttakInputTjeneste = uttakInputTjeneste;
    }

    Response overstyr(@Parameter(description = "Saksnummer") @NotNull @Valid String saksnummer,
                      @Parameter(description = "Dekningsgrad") @NotNull int dekningsgrad) {
        Optional<Fagsak> fagsakOpt = fagsakRepository.hentSakGittSaksnummer(new Saksnummer(saksnummer));
        if (fagsakOpt.isEmpty()) {
            return Response.status(Response.Status.NOT_FOUND).build();
        }
        Fagsak fagsak = fagsakOpt.get();
        Optional<Dekningsgrad> overstyrtVerdi = utledDekningsgrad(dekningsgrad);
        if (overstyrtVerdi.isEmpty()) {
            return Response.status(Response.Status.BAD_REQUEST).build();
        }

        Optional<FagsakRelasjon> fagsakRelasjon = fagsakRelasjonTjeneste.finnRelasjonForHvisEksisterer(fagsak);
        if (fagsakRelasjon.flatMap(FagsakRelasjon::getFagsakNrTo).isPresent()) {
            throw new IllegalStateException("Ikke støttet: Berørt sak");
        }
        Dekningsgrad fraVerdi = fagsakRelasjon.orElseThrow().getDekningsgrad();
        Dekningsgrad tilVerdi = overstyrtVerdi.get();
        if (fraVerdi.equals(tilVerdi)) {
            return Response.noContent().build();
        }

        lagHistorikkinnslagOverstyrtDekningsgrad(fagsak.getId(), fraVerdi, tilVerdi);
        fagsakRelasjonTjeneste.opprettEllerOppdaterRelasjon(fagsak, fagsakRelasjon, tilVerdi);
        Behandling behandling = hentÅpenBehandlingEllerOpprettRevurdering(fagsak);

        var uttakInput = uttakInputTjeneste.lagInput(behandling);
        beregnStønadskontoerTjeneste.beregnStønadskontoer(uttakInput);
        if (behandling.getAktivtBehandlingSteg() == null) {
            opprettTaskForÅStarteBehandling(behandling);
        }
        return Response.ok().build();
    }

    private Behandling hentÅpenBehandlingEllerOpprettRevurdering(Fagsak fagsak) {
        List<Behandling> åpneBehandlinger = behandlingRepository.hentÅpneBehandlingerForFagsakId(fagsak.getId());
        Optional<Behandling> ytelseBehandlingOpt = åpneBehandlinger.stream()
            .filter(Behandling::erYtelseBehandling)
            .findFirst();
        if (ytelseBehandlingOpt.isPresent()) {
            return ytelseBehandlingOpt.get();
        }
        return revurderingTjeneste.opprettManuellRevurdering(fagsak, BehandlingÅrsakType.RE_ENDRING_FRA_BRUKER,
            behandlendeEnhetTjeneste.finnBehandlendeEnhetFor(fagsak)
        );
    }

    private void lagHistorikkinnslagOverstyrtDekningsgrad(Long fagsakId, Dekningsgrad fraVerdi, Dekningsgrad tilVerdi) {
        Historikkinnslag endretDekningsgrad = new Historikkinnslag();
        endretDekningsgrad.setAktør(HistorikkAktør.VEDTAKSLØSNINGEN);
        endretDekningsgrad.setType(HistorikkinnslagType.FAKTA_ENDRET);
        endretDekningsgrad.setFagsakId(fagsakId);

        HistorikkInnslagTekstBuilder historieBuilder = new HistorikkInnslagTekstBuilder()
            .medHendelse(HistorikkinnslagType.FAKTA_ENDRET)
            .medEndretFelt(HistorikkEndretFeltType.DEKNINGSGRAD, fraVerdi.getVerdi() + "%", tilVerdi.getVerdi() + "%");
        historieBuilder.build(endretDekningsgrad);
        historikkRepository.lagre(endretDekningsgrad);
    }

    private Optional<Dekningsgrad> utledDekningsgrad(int dekningsgrad) {
        if (dekningsgrad == 100) {
            return Optional.of(Dekningsgrad._100);
        }
        if (dekningsgrad == 80) {
            return Optional.of(Dekningsgrad._80);
        }
        return Optional.empty();
    }

    private void opprettTaskForÅStarteBehandling(Behandling behandling) {
        ProsessTaskData prosessTaskData = new ProsessTaskData(StartBehandlingTask.TASKTYPE);
        prosessTaskData.setBehandling(behandling.getFagsakId(), behandling.getId(), behandling.getAktørId().getId());
        prosessTaskData.setCallIdFraEksisterende();
        prosessTaskRepository.lagre(prosessTaskData);
    }
}
