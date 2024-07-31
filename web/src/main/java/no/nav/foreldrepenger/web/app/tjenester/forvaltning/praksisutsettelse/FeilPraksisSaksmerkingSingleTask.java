package no.nav.foreldrepenger.web.app.tjenester.forvaltning.praksisutsettelse;

import java.util.List;
import java.util.Optional;

import jakarta.enterprise.context.Dependent;
import jakarta.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import no.nav.foreldrepenger.behandlingslager.behandling.historikk.HistorikkAktør;
import no.nav.foreldrepenger.behandlingslager.behandling.historikk.HistorikkEndretFeltType;
import no.nav.foreldrepenger.behandlingslager.behandling.historikk.HistorikkRepository;
import no.nav.foreldrepenger.behandlingslager.behandling.historikk.Historikkinnslag;
import no.nav.foreldrepenger.behandlingslager.behandling.historikk.HistorikkinnslagType;
import no.nav.foreldrepenger.behandlingslager.fagsak.Fagsak;
import no.nav.foreldrepenger.behandlingslager.fagsak.FagsakEgenskapRepository;
import no.nav.foreldrepenger.behandlingslager.fagsak.FagsakProsesstaskRekkefølge;
import no.nav.foreldrepenger.behandlingslager.fagsak.FagsakRepository;
import no.nav.foreldrepenger.behandlingslager.fagsak.egenskaper.FagsakMarkering;
import no.nav.foreldrepenger.historikk.HistorikkInnslagTekstBuilder;
import no.nav.vedtak.felles.prosesstask.api.ProsessTask;
import no.nav.vedtak.felles.prosesstask.api.ProsessTaskData;
import no.nav.vedtak.felles.prosesstask.api.ProsessTaskHandler;

@Dependent
@ProsessTask(value = "behandling.saksmerkepraksisutsettelse.single", prioritet = 4, maxFailedRuns = 1)
@FagsakProsesstaskRekkefølge(gruppeSekvens = false)
class FeilPraksisSaksmerkingSingleTask implements ProsessTaskHandler {

    private static final Logger LOG = LoggerFactory.getLogger(FeilPraksisSaksmerkingSingleTask.class);

    static final String FAGSAK_ID = "fagsakId";
    private final HistorikkRepository historikkRepository;
    private final FagsakRepository fagsakRepository;
    private final FagsakEgenskapRepository fagsakEgenskapRepository;

    @Inject
    public FeilPraksisSaksmerkingSingleTask(HistorikkRepository historikkRepository,
                                            FagsakRepository fagsakRepository,
                                            FagsakEgenskapRepository fagsakEgenskapRepository) {
        this.historikkRepository = historikkRepository;
        this.fagsakRepository = fagsakRepository;
        this.fagsakEgenskapRepository = fagsakEgenskapRepository;
    }

    @Override
    public void doTask(ProsessTaskData prosessTaskData) {
        var fagsak = Optional.ofNullable(prosessTaskData.getPropertyValue(FAGSAK_ID))
            .map(fid -> fagsakRepository.finnEksaktFagsak(Long.parseLong(fid)))
            .orElseThrow();
        var eksisterende = fagsakEgenskapRepository.finnFagsakMarkeringer(fagsak.getId());
        if (eksisterende.contains(FagsakMarkering.PRAKSIS_UTSETTELSE)) {
            return;
        }
        // TODO: Sjekk om aktuelt å flytte utland eller sammensatt kontroll til nasjonal kø og overstyre merking
        if (eksisterende.stream().anyMatch(FagsakMarkering::erPrioritert)) {
            LOG.info("FeilPraksisUtsettelse: Endrer ikke saksmerking for {} har {}", fagsak.getSaksnummer(), eksisterende);
            return;
        }
        fagsakEgenskapRepository.lagreAlleFagsakMarkeringer(fagsak.getId(), List.of(FagsakMarkering.PRAKSIS_UTSETTELSE)); // TODO lagre enkeltmerke
        if (!eksisterende.isEmpty()) {
            lagHistorikkInnslag(fagsak, eksisterende.stream().findFirst().orElseThrow(), FagsakMarkering.PRAKSIS_UTSETTELSE);
        }
    }

    private void lagHistorikkInnslag(Fagsak fagsak, FagsakMarkering eksisterende, FagsakMarkering ny) {
        var historikkinnslag = new Historikkinnslag.Builder()
            .medFagsakId(fagsak.getId())
            .medAktør(HistorikkAktør.SAKSBEHANDLER)
            .medType(HistorikkinnslagType.FAKTA_ENDRET)
            .build();

        var builder = new HistorikkInnslagTekstBuilder()
            .medHendelse(HistorikkinnslagType.FAKTA_ENDRET)
            .medEndretFelt(HistorikkEndretFeltType.SAKSMARKERING, eksisterende.getNavn(), ny.getNavn());

        builder.build(historikkinnslag);
        historikkRepository.lagre(historikkinnslag);
    }

}
