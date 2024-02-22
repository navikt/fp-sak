package no.nav.foreldrepenger.web.app.tjenester.forvaltning;

import jakarta.enterprise.context.Dependent;
import jakarta.inject.Inject;

import no.nav.foreldrepenger.behandlingslager.behandling.BehandlingÅrsakType;
import no.nav.foreldrepenger.behandlingslager.fagsak.FagsakProsesstaskRekkefølge;
import no.nav.foreldrepenger.behandlingslager.fagsak.FagsakRepository;
import no.nav.foreldrepenger.dokumentbestiller.infobrev.FeilPraksisUtsettelseRepository;
import no.nav.foreldrepenger.mottak.vedtak.overlapp.HåndterOpphørAvYtelser;
import no.nav.vedtak.felles.prosesstask.api.ProsessTask;
import no.nav.vedtak.felles.prosesstask.api.ProsessTaskData;
import no.nav.vedtak.felles.prosesstask.api.ProsessTaskHandler;
import no.nav.vedtak.felles.prosesstask.api.ProsessTaskTjeneste;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Comparator;
import java.util.Optional;

@Dependent
@ProsessTask(value = "behandling.feilpraksisutsettelse", maxFailedRuns = 1)
@FagsakProsesstaskRekkefølge(gruppeSekvens = false)
class FeilPraksisUtsettelseTestTask implements ProsessTaskHandler {

    private static final Logger LOG = LoggerFactory.getLogger(FeilPraksisUtsettelseTestTask.class);
    private static final String UTVALG = "utvalg";
    private static final String FRA_FAGSAK_ID = "fraFagsakId";
    private final FeilPraksisUtsettelseRepository utvalgRepository;
    private final HåndterOpphørAvYtelser håndterOpphørAvYtelser;
    private final FagsakRepository fagsakRepository;
    private final ProsessTaskTjeneste prosessTaskTjeneste;

    public enum Utvalg { MOR, FAR_BEGGE_RETT, BARE_FAR_RETT }

    @Inject
    public FeilPraksisUtsettelseTestTask(FeilPraksisUtsettelseRepository utvalgRepository,
                                         HåndterOpphørAvYtelser håndterOpphørAvYtelser,
                                         FagsakRepository fagsakRepository,
                                         ProsessTaskTjeneste prosessTaskTjeneste) {
        this.utvalgRepository = utvalgRepository;
        this.håndterOpphørAvYtelser = håndterOpphørAvYtelser;
        this.fagsakRepository = fagsakRepository;
        this.prosessTaskTjeneste = prosessTaskTjeneste;
    }

    @Override
    public void doTask(ProsessTaskData prosessTaskData) {
        var fagsakIdProperty = prosessTaskData.getPropertyValue(FRA_FAGSAK_ID);
        var fraFagsakId = fagsakIdProperty == null ? null : Long.valueOf(fagsakIdProperty);
        var utvalg = Optional.ofNullable(prosessTaskData.getPropertyValue(UTVALG))
            .map(Utvalg::valueOf).orElseThrow();

        var saker = switch (utvalg) {
            case MOR -> utvalgRepository.finnNeste200AktuelleSakerMor(fraFagsakId);
            case FAR_BEGGE_RETT -> utvalgRepository.finnNeste200AktuelleSakerFarBeggeEllerAlene(fraFagsakId);
            case BARE_FAR_RETT -> utvalgRepository.finnNeste200AktuelleSakerBareFarHarRett(fraFagsakId);
        };
        saker.stream().map(fagsakRepository::finnEksaktFagsak)
            .forEach(f -> håndterOpphørAvYtelser.oppdaterEllerOpprettRevurdering(f, null, BehandlingÅrsakType.FEIL_PRAKSIS_UTSETTELSE, false));

        saker.stream().max(Comparator.naturalOrder())
            .ifPresent(maxsak -> prosessTaskTjeneste.lagre(opprettTaskForNesteUtvalg(maxsak, utvalg)));

    }


    public static ProsessTaskData opprettTaskForNesteUtvalg(Long fraVedtakId, Utvalg utvalg) {
        var prosessTaskData = ProsessTaskData.forProsessTask(FeilPraksisUtsettelseTestTask.class);
        prosessTaskData.setProperty(FeilPraksisUtsettelseTestTask.FRA_FAGSAK_ID, fraVedtakId == null ? null : String.valueOf(fraVedtakId));
        prosessTaskData.setProperty(FeilPraksisUtsettelseTestTask.UTVALG, utvalg.name());
        prosessTaskData.setCallIdFraEksisterende();
        prosessTaskData.setPrioritet(150);
        return prosessTaskData;
    }
}
