package no.nav.foreldrepenger.web.app.tjenester.forvaltning.praksisutsettelse;

import static no.nav.foreldrepenger.behandlingslager.behandling.historikk.HistorikkinnslagTekstlinjeBuilder.fraTilEquals;

import java.util.Optional;

import jakarta.enterprise.context.Dependent;
import jakarta.inject.Inject;

import no.nav.foreldrepenger.behandlingslager.behandling.historikk.HistorikkAktør;
import no.nav.foreldrepenger.behandlingslager.behandling.historikk.Historikkinnslag2;
import no.nav.foreldrepenger.behandlingslager.behandling.historikk.Historikkinnslag2Repository;
import no.nav.foreldrepenger.behandlingslager.fagsak.Fagsak;
import no.nav.foreldrepenger.behandlingslager.fagsak.FagsakEgenskapRepository;
import no.nav.foreldrepenger.behandlingslager.fagsak.FagsakProsesstaskRekkefølge;
import no.nav.foreldrepenger.behandlingslager.fagsak.FagsakRepository;
import no.nav.foreldrepenger.behandlingslager.fagsak.egenskaper.FagsakMarkering;
import no.nav.vedtak.felles.prosesstask.api.ProsessTask;
import no.nav.vedtak.felles.prosesstask.api.ProsessTaskData;
import no.nav.vedtak.felles.prosesstask.api.ProsessTaskHandler;

@Dependent
@ProsessTask(value = "behandling.saksmerkepraksisutsettelse.single", prioritet = 4, maxFailedRuns = 1)
@FagsakProsesstaskRekkefølge(gruppeSekvens = false)
class FeilPraksisSaksmerkingSingleTask implements ProsessTaskHandler {
    static final String FAGSAK_ID = "fagsakId";
    private final Historikkinnslag2Repository historikkinnslagRepository;
    private final FagsakRepository fagsakRepository;
    private final FagsakEgenskapRepository fagsakEgenskapRepository;

    @Inject
    public FeilPraksisSaksmerkingSingleTask(Historikkinnslag2Repository historikkinnslagRepository,
                                            FagsakRepository fagsakRepository,
                                            FagsakEgenskapRepository fagsakEgenskapRepository) {
        this.historikkinnslagRepository = historikkinnslagRepository;
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
        fagsakEgenskapRepository.leggTilFagsakMarkering(fagsak.getId(), FagsakMarkering.PRAKSIS_UTSETTELSE);
        if (!eksisterende.isEmpty()) {
            lagHistorikkInnslag(fagsak, eksisterende.stream().findFirst().orElseThrow(), FagsakMarkering.PRAKSIS_UTSETTELSE);
        }
    }

    private void lagHistorikkInnslag(Fagsak fagsak, FagsakMarkering eksisterende, FagsakMarkering ny) {
        var historikkinnslag = new Historikkinnslag2.Builder()
            .medAktør(HistorikkAktør.SAKSBEHANDLER)
            .medFagsakId(fagsak.getId())
            .medTittel("Fakta endret")
            .addTekstlinje(fraTilEquals("Saksmarkering", eksisterende.getNavn(), ny.getNavn()))
            .build();
        historikkinnslagRepository.lagre(historikkinnslag);
    }

}
