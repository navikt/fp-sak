package no.nav.foreldrepenger.domene.vedtak.migrering;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.persistence.EntityManager;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import no.nav.foreldrepenger.behandlingslager.fagsak.Fagsak;
import no.nav.foreldrepenger.behandlingslager.fagsak.FagsakProsesstaskRekkefølge;
import no.nav.foreldrepenger.behandlingslager.fagsak.FagsakStatus;
import no.nav.foreldrepenger.behandlingslager.fagsak.FagsakYtelseType;
import no.nav.vedtak.felles.prosesstask.api.ProsessTask;
import no.nav.vedtak.felles.prosesstask.api.ProsessTaskData;
import no.nav.vedtak.felles.prosesstask.api.ProsessTaskHandler;
import no.nav.vedtak.felles.prosesstask.api.ProsessTaskTjeneste;

@ApplicationScoped
@ProsessTask(value = "fpinntektsmelding.opprettManglendeforespørsler", prioritet = 3, maxFailedRuns = 1)
@FagsakProsesstaskRekkefølge(gruppeSekvens = false)
public class OpprettManglendeForespørslerTask implements ProsessTaskHandler {
    private static final Logger LOG = LoggerFactory.getLogger(OpprettManglendeForespørslerTask.class);
    private static final String FRA_OG_MED = "fraOgMed";
    private static final String TIL_OG_MED = "tilOgMed";
    private static final String DRY_RUN = "dryRun";

    private EntityManager entityManager;
    private ProsessTaskTjeneste prosessTaskTjeneste;
    private MigrerManglendeForespørslerTjeneste migrerManglendeForespørslerTjeneste;

    OpprettManglendeForespørslerTask() {
        // for CDI proxy
    }

    @Inject
    public OpprettManglendeForespørslerTask(EntityManager entityManager,
                                            ProsessTaskTjeneste prosessTaskTjeneste,
                                            MigrerManglendeForespørslerTjeneste migrerManglendeForespørslerTjeneste) {
        this.entityManager = entityManager;
        this.prosessTaskTjeneste = prosessTaskTjeneste;
        this.migrerManglendeForespørslerTjeneste = migrerManglendeForespørslerTjeneste;
    }

    @Override
    public void doTask(ProsessTaskData prosessTaskData) {
        var fraOgMedDato = LocalDate.parse(Optional.ofNullable(prosessTaskData.getPropertyValue(FRA_OG_MED)).orElseThrow());
        var tilOgMedDato = LocalDate.parse(Optional.ofNullable(prosessTaskData.getPropertyValue(TIL_OG_MED)).orElseThrow());
        var dryRun = Boolean.parseBoolean(Optional.ofNullable(prosessTaskData.getPropertyValue(DRY_RUN)).orElse("true"));

        var tilDatoIntervall = fraOgMedDato.plusDays(1);
        LOG.info("Henter saker for fradato: {} og tilDato: {} ", fraOgMedDato, tilDatoIntervall);
        var saker = finnNesteIntervallAvSAker(fraOgMedDato, tilDatoIntervall);

        LOG.info("Fant {} saker som skal vurderes for å opprette forespørsler", saker.size());

        saker.forEach(sak -> {
            //Vurder om det skal opprettes forespørsel for behandling
            migrerManglendeForespørslerTjeneste.vurderOmForespørselSkalOpprettes(sak, dryRun);
        });

        var nyFraOgMed = fraOgMedDato.plusDays(1);
        if (nyFraOgMed.isAfter(tilOgMedDato) || nyFraOgMed.isEqual(tilOgMedDato)) {
            LOG.info("Ingen flere saker å hente, fradato: {} er etter eller lik tildato: {}", nyFraOgMed, tilOgMedDato);
        } else {
            prosessTaskTjeneste.lagre(opprettManglendeForespørselTaskForNesteDato(nyFraOgMed, dryRun, tilOgMedDato));
        }
    }

    private List<Fagsak> finnNesteIntervallAvSAker(LocalDate fraOgMed, LocalDate tilOgMed) {
        var sql = """
            select f.*
            from fagsak f
            where f.opprettet_tid < :nyImPortalLansertDato
            and f.opprettet_tid >= to_date(:fraOgMed, 'DD-MM-YYYY')  and f.opprettet_tid <= to_date(:tilOgMed, 'DD-MM-YYYY')
            and f.fagsak_status = :lopendeStatus
            and f.ytelse_type in (:ytelseTyper)
            """;


        var query = entityManager.createNativeQuery(sql, Fagsak.class)
            .setParameter("nyImPortalLansertDato", LocalDate.of(2024, 11, 14))
            .setParameter("fraOgMed", fraOgMed)
            .setParameter("tilOgMed", tilOgMed)
            .setParameter("lopendeStatus", FagsakStatus.LØPENDE.getKode())
            .setParameter("ytelseTyper", List.of(FagsakYtelseType.SVANGERSKAPSPENGER.getKode(), FagsakYtelseType.FORELDREPENGER.getKode()));

        return query.getResultList();
    }

    public static ProsessTaskData opprettManglendeForespørselTaskForNesteDato(LocalDate nyFraOgMed, boolean dryRun, LocalDate tilOgMed) {
        LOG.info("Oppretter OpprettManglendeForespørslerTask for dato {}", nyFraOgMed);
        var prosessTaskData = ProsessTaskData.forProsessTask(OpprettManglendeForespørslerTask.class);
        prosessTaskData.setProperty(FRA_OG_MED, String.valueOf(nyFraOgMed));
        prosessTaskData.setProperty(TIL_OG_MED, String.valueOf(tilOgMed));
        prosessTaskData.setProperty(DRY_RUN, String.valueOf(dryRun));
        return prosessTaskData;
    }
}
