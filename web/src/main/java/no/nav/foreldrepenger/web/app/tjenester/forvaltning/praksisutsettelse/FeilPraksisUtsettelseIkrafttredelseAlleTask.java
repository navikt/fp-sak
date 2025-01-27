package no.nav.foreldrepenger.web.app.tjenester.forvaltning.praksisutsettelse;

import jakarta.enterprise.context.Dependent;
import jakarta.inject.Inject;
import no.nav.foreldrepenger.behandlingslager.fagsak.FagsakProsesstaskRekkefølge;
import no.nav.foreldrepenger.dokumentbestiller.infobrev.FeilPraksisUtsettelseRepository;
import no.nav.vedtak.felles.prosesstask.api.ProsessTask;
import no.nav.vedtak.felles.prosesstask.api.ProsessTaskData;
import no.nav.vedtak.felles.prosesstask.api.ProsessTaskHandler;
import no.nav.vedtak.felles.prosesstask.api.ProsessTaskTjeneste;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Optional;

import static no.nav.foreldrepenger.web.app.tjenester.forvaltning.praksisutsettelse.FeilPraksisUtsettelseIkrafttredelseSingleTask.DRY_RUN;

@Dependent
@ProsessTask(value = "behandling.friutsettelse.ikrafttredelse.alle", prioritet = 4, maxFailedRuns = 1)
@FagsakProsesstaskRekkefølge(gruppeSekvens = false)
class FeilPraksisUtsettelseIkrafttredelseAlleTask implements ProsessTaskHandler {
    private static final Logger LOG = LoggerFactory.getLogger(FeilPraksisUtsettelseIkrafttredelseAlleTask.class);

    private static final String UTVALG = "utvalg";
    private final FeilPraksisUtsettelseRepository utvalgRepository;
    private final ProsessTaskTjeneste prosessTaskTjeneste;

    public enum Utvalg { MOR, FAR }

    @Inject
    public FeilPraksisUtsettelseIkrafttredelseAlleTask(FeilPraksisUtsettelseRepository utvalgRepository,
                                                       ProsessTaskTjeneste prosessTaskTjeneste) {
        this.utvalgRepository = utvalgRepository;
        this.prosessTaskTjeneste = prosessTaskTjeneste;
    }

    @Override
    public void doTask(ProsessTaskData prosessTaskData) {
        var dryRun = Optional.ofNullable(prosessTaskData.getPropertyValue(DRY_RUN)).filter("false"::equalsIgnoreCase).isEmpty();
        var utvalg = Optional.ofNullable(prosessTaskData.getPropertyValue(UTVALG))
                .map(Utvalg::valueOf)
                .orElseThrow();
        var saker = switch (utvalg) {
            case MOR -> utvalgRepository.alleSakerMorAvslagUtsettelse();
            case FAR -> utvalgRepository.alleSakerFarBeggeEllerAleneUtsettelse();
        };
        LOG.info("Hentet {} saker for utvalg {}", saker.size(), utvalg);
        saker.stream().map((Long fagsakId) -> opprettTaskForEnkeltSak(fagsakId, dryRun)).forEach(prosessTaskTjeneste::lagre);
    }

    private static ProsessTaskData opprettTaskForEnkeltSak(Long fagsakId, boolean dryRun) {
        var prosessTaskData = ProsessTaskData.forProsessTask(FeilPraksisUtsettelseIkrafttredelseSingleTask.class);
        prosessTaskData.setProperty(FeilPraksisUtsettelseIkrafttredelseSingleTask.FAGSAK_ID, String.valueOf(fagsakId));
        prosessTaskData.setProperty(DRY_RUN, String.valueOf(dryRun));
        return prosessTaskData;
    }
}
