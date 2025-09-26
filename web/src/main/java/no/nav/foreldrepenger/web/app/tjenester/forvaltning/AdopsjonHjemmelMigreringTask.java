package no.nav.foreldrepenger.web.app.tjenester.forvaltning;

import java.util.Optional;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.stream.Stream;

import jakarta.enterprise.context.Dependent;
import jakarta.inject.Inject;
import jakarta.persistence.EntityManager;

import no.nav.foreldrepenger.behandlingslager.behandling.repository.BehandlingRepository;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import no.nav.foreldrepenger.behandlingslager.behandling.BehandlingsresultatRepository;
import no.nav.foreldrepenger.behandlingslager.behandling.familiehendelse.FamilieHendelseGrunnlagEntitet;
import no.nav.foreldrepenger.behandlingslager.behandling.familiehendelse.VilkårHjemmelMapper;
import no.nav.foreldrepenger.behandlingslager.fagsak.FagsakProsesstaskRekkefølge;
import no.nav.foreldrepenger.familiehendelse.FamilieHendelseTjeneste;
import no.nav.vedtak.felles.prosesstask.api.ProsessTask;
import no.nav.vedtak.felles.prosesstask.api.ProsessTaskData;
import no.nav.vedtak.felles.prosesstask.api.ProsessTaskHandler;
import no.nav.vedtak.felles.prosesstask.api.ProsessTaskTjeneste;

@Dependent
@ProsessTask(value = "adopsjonhjemmel.migrering", prioritet = 4, maxFailedRuns = 1)
@FagsakProsesstaskRekkefølge(gruppeSekvens = false)
class AdopsjonHjemmelMigreringTask implements ProsessTaskHandler {

    private static final Logger LOG = LoggerFactory.getLogger(AdopsjonHjemmelMigreringTask.class);
    private static final String FRA_ID = "fraId";
    private static final String MAX_ID = "maxId";
    private static final String DRY_RUN = "dryRun";


    private final EntityManager entityManager;
    private final ProsessTaskTjeneste prosessTaskTjeneste;
    private final FamilieHendelseTjeneste familieHendelseTjeneste;
    private final BehandlingsresultatRepository behandlingsresultatRepository;
    private final BehandlingRepository behandlingRepository;

    @Inject
    public AdopsjonHjemmelMigreringTask(EntityManager entityManager,
                                        ProsessTaskTjeneste prosessTaskTjeneste,
                                        FamilieHendelseTjeneste familieHendelseTjeneste,
                                        BehandlingsresultatRepository behandlingsresultatRepository,
                                        BehandlingRepository behandlingRepository) {
        this.entityManager = entityManager;
        this.prosessTaskTjeneste = prosessTaskTjeneste;
        this.familieHendelseTjeneste = familieHendelseTjeneste;
        this.behandlingsresultatRepository = behandlingsresultatRepository;
        this.behandlingRepository = behandlingRepository;
    }

    public static ProsessTaskData opprettNesteTask(Long fraVedtakId, boolean dryRun, Long maxId) {
        var prosessTaskData = ProsessTaskData.forProsessTask(AdopsjonHjemmelMigreringTask.class);

        prosessTaskData.setProperty(AdopsjonHjemmelMigreringTask.FRA_ID, fraVedtakId == null ? null : String.valueOf(fraVedtakId));
        prosessTaskData.setProperty(AdopsjonHjemmelMigreringTask.MAX_ID, maxId == null ? null : String.valueOf(maxId));
        prosessTaskData.setProperty(AdopsjonHjemmelMigreringTask.DRY_RUN, String.valueOf(dryRun));
        return prosessTaskData;
    }

    @Override
    public void doTask(ProsessTaskData prosessTaskData) {
        var fraId = Optional.ofNullable(prosessTaskData.getPropertyValue(FRA_ID)).map(Long::valueOf).orElse(null);
        var maxId = Optional.ofNullable(prosessTaskData.getPropertyValue(MAX_ID)).map(Long::valueOf).orElse(null);
        var dryRun = Optional.ofNullable(prosessTaskData.getPropertyValue(DRY_RUN)).filter("false"::equalsIgnoreCase).isEmpty();

        var adopsjoner = finnNesteHundreAdopsjoner(fraId).toList();

        adopsjoner.stream().filter(b -> maxId == null || b.getId() < maxId).forEach(ur -> håndterAdopsjon(ur, dryRun));

        adopsjoner.stream()
            .map(FamilieHendelseGrunnlagEntitet::getId)
            .max(Long::compareTo)
            .filter(v -> maxId == null || v < maxId)
            .ifPresent(nesteId -> prosessTaskTjeneste.lagre(opprettNesteTask(nesteId, dryRun, maxId)));
    }

    private Stream<FamilieHendelseGrunnlagEntitet> finnNesteHundreAdopsjoner(Long fraId) {
        var sql = """
            select * from (
                select * from GR_FAMILIE_HENDELSE where overstyrt_familie_hendelse_id in (
                    select distinct(gr.overstyrt_familie_hendelse_id) from GR_FAMILIE_HENDELSE gr
                    inner join fh_familie_hendelse fh on gr.overstyrt_familie_hendelse_id = fh.id
                    inner join fh_adopsjon adop on fh.id =  adop.familie_hendelse_id
                    where adop.familie_hendelse_id >:fraId)
                order by overstyrt_familie_hendelse_id)
            where ROWNUM <= 100
            """;

        var query = entityManager.createNativeQuery(sql, FamilieHendelseGrunnlagEntitet.class).setParameter(FRA_ID, fraId == null ? 0 : fraId);
        return query.getResultStream();
    }

    /**
     * TODO:
     * En utfordring jeg har støtt på når jeg har laget migreringsjobben er:
     *  - Når man overstyrer opprettes det ett nytt grunnlag i GR_FAMILIE_HENDELSE
     *  - Dette gjøres med å lage ny rad i GR_FAMILIE_HENDELSE hvor kolonnen AKTIV flippes til J/N for ny og gamle rader)
     * Ønsker vi å oppdatere alle entries, både akive og inaktive, for alle familiehendelser som har overstyring? Eller bare opprette nytt grunnlag
     */
    private void håndterAdopsjon(FamilieHendelseGrunnlagEntitet familiehendelse, boolean dryRun) {
        var endret = new AtomicBoolean(false);
        var oppdatertOverstyrtHendelse = familieHendelseTjeneste.opprettBuilderForOverstyring(familiehendelse.getBehandlingId());
        // TODO(siri): er det nødvendig å sjekke om det finnes relevant AP som er knyttet til behandlingen, eller kan vi tolke at hvis det finnes en overstyring så vet vi at det har vært AP
        familiehendelse.getOverstyrtVersjon().ifPresent(overstyring -> {
            overstyring.getAdopsjon().ifPresent(adopsjon -> {
                behandlingsresultatRepository.hentHvisEksisterer(familiehendelse.getBehandlingId())
                    .ifPresent(br -> br.getVilkårResultat()
                        .getVilkårene()
                        .stream()
                        .filter(v -> v.getVilkårType().gjelderOmsorgEllerForeldreansvar())
                        .forEach(v -> {
                            var fagsakYtelseType = behandlingRepository.hentBehandling(familiehendelse.getBehandlingId()).getFagsakYtelseType();
                            oppdatertOverstyrtHendelse.medAdopsjon(oppdatertOverstyrtHendelse.getAdopsjonBuilder()
                                .medVilkårHjemmel(VilkårHjemmelMapper.mapVilkårTypeTilVilkårHjemmel(v.getVilkårType(), fagsakYtelseType)));
                            endret.set(true);
                        }));
            });
        });


        if (endret.get()) {
            if (dryRun) {
                LOG.info("FPSAK KONTO DRYRUN id {} skal oppdatere med {}", familiehendelse.getId(), oppdatertOverstyrtHendelse);
                return;
            }
            familieHendelseTjeneste.lagreOverstyrtHendelse(familiehendelse.getBehandlingId(), oppdatertOverstyrtHendelse);
        }
    }

}
