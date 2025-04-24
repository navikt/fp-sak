package no.nav.foreldrepenger.domene.prosess;

import java.util.Optional;

import jakarta.enterprise.context.Dependent;
import jakarta.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import no.nav.foreldrepenger.domene.entiteter.BeregningsgrunnlagRepository;
import no.nav.vedtak.felles.prosesstask.api.ProsessTask;
import no.nav.vedtak.felles.prosesstask.api.ProsessTaskData;
import no.nav.vedtak.felles.prosesstask.api.ProsessTaskHandler;
import no.nav.vedtak.felles.prosesstask.api.ProsessTaskTjeneste;

@Dependent
@ProsessTask(value = "beregningsgrunnlag.aap.orkestrerer", prioritet = 4, maxFailedRuns = 1)
public class AAPPraksisendringOrkestrererTask implements ProsessTaskHandler  {
    private static final Logger LOG = LoggerFactory.getLogger(AAPPraksisendringOrkestrererTask.class);
    private static final String DRY_RUN = "dryRun";
    private static final String FRA_OG_MED = "fraOgMed";
    private static final String TIL_OG_MED = "tilOgMed";

    private BeregningsgrunnlagRepository beregningsgrunnlagRepository;
    private ProsessTaskTjeneste prosessTaskTjeneste;

    public AAPPraksisendringOrkestrererTask() {
        // CDI
    }

    @Inject
    public AAPPraksisendringOrkestrererTask(BeregningsgrunnlagRepository beregningsgrunnlagRepository,
                                            ProsessTaskTjeneste prosessTaskTjeneste) {
        this.beregningsgrunnlagRepository = beregningsgrunnlagRepository;
        this.prosessTaskTjeneste = prosessTaskTjeneste;
    }

    @Override
    public void doTask(ProsessTaskData prosessTaskData) {
        LOG.info("Starter task for å hente kandidater påvirket av aap praksisendring.");
        var fraOgMedId = Optional.ofNullable(prosessTaskData.getPropertyValue(FRA_OG_MED)).map(Long::valueOf).orElseThrow();
        var tilOgMedId = Optional.ofNullable(prosessTaskData.getPropertyValue(TIL_OG_MED)).map(Long::valueOf).orElseThrow();
        var erDryRun = Optional.ofNullable(prosessTaskData.getPropertyValue(DRY_RUN)).filter("false"::equalsIgnoreCase).isEmpty();
        var fagsakIder = beregningsgrunnlagRepository.hentFagsakerMedAAPIGrunnlag(fraOgMedId, tilOgMedId);
        LOG.info("Fant {} fagsaker som vil undersøkes for påvirkning av praksisendring", fagsakIder.size());
        if (erDryRun) {
            LOG.info("Kjøring var dryrun, avslutter task.");
        } else {
            fagsakIder.forEach(id -> {
                LOG.info("Oppretter task for fagsak {}", id);
                var nyData = ProsessTaskData.forProsessTask(AAPPraksisendringSakTask.class);
                nyData.setProperty(AAPPraksisendringSakTask.FAGSAK_ID, String.valueOf(id));
                prosessTaskTjeneste.lagre(nyData);
            });
        }
        fagsakIder.stream()
            .max(Long::compareTo)
            .ifPresent(nesteId -> prosessTaskTjeneste.lagre(opprettNesteTask(nesteId+1, erDryRun, tilOgMedId)));
        LOG.info("Avslutter task for å hente kandidater påvirket av aap praksisendring.");
    }

    public static ProsessTaskData opprettNesteTask(Long nyFraOgMed, boolean dryRun, Long tilOgMed) {
        var prosessTaskData = ProsessTaskData.forProsessTask(AAPPraksisendringOrkestrererTask.class);
        prosessTaskData.setProperty(FRA_OG_MED, String.valueOf(nyFraOgMed));
        prosessTaskData.setProperty(TIL_OG_MED, String.valueOf(tilOgMed));
        prosessTaskData.setProperty(DRY_RUN, String.valueOf(dryRun));
        return prosessTaskData;
    }

}
