package no.nav.foreldrepenger.web.app.tjenester.forvaltning.praksisutsettelse;

import static no.nav.foreldrepenger.produksjonsstyring.behandlingenhet.EnhetsTjeneste.MIDLERTIDIG_ENHET;

import java.util.List;
import java.util.Optional;

import jakarta.enterprise.context.Dependent;
import jakarta.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import no.nav.foreldrepenger.behandlingslager.behandling.historikk.HistorikkAktør;
import no.nav.foreldrepenger.behandlingslager.behandling.historikk.Historikkinnslag;
import no.nav.foreldrepenger.behandlingslager.behandling.historikk.HistorikkinnslagRepository;
import no.nav.foreldrepenger.behandlingslager.fagsak.Fagsak;
import no.nav.foreldrepenger.behandlingslager.fagsak.FagsakEgenskapRepository;
import no.nav.foreldrepenger.behandlingslager.fagsak.FagsakProsesstaskRekkefølge;
import no.nav.foreldrepenger.behandlingslager.fagsak.FagsakRepository;
import no.nav.foreldrepenger.behandlingslager.fagsak.egenskaper.FagsakMarkering;
import no.nav.foreldrepenger.produksjonsstyring.oppgavebehandling.OppgaveTjeneste;
import no.nav.vedtak.felles.integrasjon.oppgave.v1.Oppgavetype;
import no.nav.vedtak.felles.prosesstask.api.ProsessTask;
import no.nav.vedtak.felles.prosesstask.api.ProsessTaskData;
import no.nav.vedtak.felles.prosesstask.api.ProsessTaskHandler;

@Dependent
@ProsessTask(value = "behandling.friutsettelse.ikrafttredelse.single", prioritet = 4, maxFailedRuns = 1)
@FagsakProsesstaskRekkefølge(gruppeSekvens = false)
class FeilPraksisUtsettelseIkrafttredelseSingleTask implements ProsessTaskHandler {

    private static final Logger LOG = LoggerFactory.getLogger(FeilPraksisUtsettelseIkrafttredelseSingleTask.class);
    static final String FAGSAK_ID = "fagsakId";
    static final String DRY_RUN = "dryRun";
    private static final List<String> OPPGAVE_TYPER = List.of(Oppgavetype.VURDER_DOKUMENT.getKode(), Oppgavetype.VURDER_KONSEKVENS_YTELSE.getKode(),
        "VURD_HENV", "VUR_SVAR", "KONT_BRUK");

    private final FeilPraksisOpprettBehandlingTjeneste feilPraksisOpprettBehandlingTjeneste;
    private final HistorikkinnslagRepository historikkinnslagRepository;
    private final FagsakRepository fagsakRepository;
    private final FagsakEgenskapRepository fagsakEgenskapRepository;
    private final OppgaveTjeneste oppgaveTjeneste;

    @Inject
    public FeilPraksisUtsettelseIkrafttredelseSingleTask(FeilPraksisOpprettBehandlingTjeneste feilPraksisOpprettBehandlingTjeneste,
                                                         HistorikkinnslagRepository historikkinnslagRepository,
                                                         FagsakRepository fagsakRepository,
                                                         FagsakEgenskapRepository fagsakEgenskapRepository,
                                                         OppgaveTjeneste oppgaveTjeneste) {
        this.feilPraksisOpprettBehandlingTjeneste = feilPraksisOpprettBehandlingTjeneste;
        this.historikkinnslagRepository = historikkinnslagRepository;
        this.fagsakRepository = fagsakRepository;
        this.fagsakEgenskapRepository = fagsakEgenskapRepository;
        this.oppgaveTjeneste = oppgaveTjeneste;
    }

    @Override
    public void doTask(ProsessTaskData prosessTaskData) {
        var dryRun = Optional.ofNullable(prosessTaskData.getPropertyValue(DRY_RUN)).filter("false"::equalsIgnoreCase).isEmpty();
        var fagsak = Optional.ofNullable(prosessTaskData.getPropertyValue(FAGSAK_ID))
            .map(fid -> fagsakRepository.finnEksaktFagsak(Long.parseLong(fid)))
            .orElseThrow();
        if (fagsakEgenskapRepository.finnFagsakMarkeringer(fagsak.getId()).contains(FagsakMarkering.PRAKSIS_UTSETTELSE)) {
            LOG.info("FeilPraksisUtsettelse Fagsak {} har allerede saksmerking {}", fagsak.getSaksnummer(), FagsakMarkering.PRAKSIS_UTSETTELSE);
            return;
        }
        if (harOppgaverIGosysMidlertidigEnhet(fagsak)) {
            LOG.info("FeilPraksisUtsettelse Fagsak {} har oppgaver i gosys - oppretter ikke revurdering med brev", fagsak.getSaksnummer());
            return;
        }

        if (!dryRun) {
            fagsakEgenskapRepository.leggTilFagsakMarkering(fagsak.getId(), FagsakMarkering.PRAKSIS_UTSETTELSE);
            lagHistorikkInnslag(fagsak, FagsakMarkering.PRAKSIS_UTSETTELSE);
        }
        feilPraksisOpprettBehandlingTjeneste.opprettBehandling(fagsak, dryRun);
    }

    private void lagHistorikkInnslag(Fagsak fagsak, FagsakMarkering ny) {
        var historikkinnslag = new Historikkinnslag.Builder()
            .medAktør(HistorikkAktør.VEDTAKSLØSNINGEN)
            .medFagsakId(fagsak.getId())
            .medTittel("Fakta er endret")
            .addLinje(String.format("Saksmerkering %s er lagt til", ny.getNavn()))
            .build();
        historikkinnslagRepository.lagre(historikkinnslag);
    }

    private boolean harOppgaverIGosysMidlertidigEnhet(Fagsak fagsak) {
        return !oppgaveTjeneste.hentOppgaver(OPPGAVE_TYPER, fagsak.getAktørId().getId(), MIDLERTIDIG_ENHET.enhetId(), "50").isEmpty();
    }

}
