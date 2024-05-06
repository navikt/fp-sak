package no.nav.foreldrepenger.web.app.tjenester.forvaltning;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;

import jakarta.enterprise.context.Dependent;
import jakarta.inject.Inject;
import jakarta.persistence.EntityManager;

import org.hibernate.jpa.HibernateHints;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import no.nav.foreldrepenger.behandling.FagsakRelasjonTjeneste;
import no.nav.foreldrepenger.behandlingslager.fagsak.FagsakProsesstaskRekkefølge;
import no.nav.foreldrepenger.behandlingslager.fagsak.FagsakRelasjon;
import no.nav.foreldrepenger.domene.ytelsefordeling.YtelseFordelingTjeneste;
import no.nav.vedtak.felles.prosesstask.api.ProsessTask;
import no.nav.vedtak.felles.prosesstask.api.ProsessTaskData;
import no.nav.vedtak.felles.prosesstask.api.ProsessTaskHandler;
import no.nav.vedtak.felles.prosesstask.api.ProsessTaskTjeneste;

@Dependent
@ProsessTask(value = "dekningsgrad.migrering", maxFailedRuns = 1)
@FagsakProsesstaskRekkefølge(gruppeSekvens = false)
class DekningsgradMigreringTask implements ProsessTaskHandler {

    private static final Logger LOG = LoggerFactory.getLogger(DekningsgradMigreringTask.class);
    static final String FOM_DATO_KEY = "fom";
    static final String TOM_DATO_KEY = "tom";
    private static final String FRA_YF_ID = "fraYfId";

    private final EntityManager entityManager;
    private final ProsessTaskTjeneste prosessTaskTjeneste;
    private final FagsakRelasjonTjeneste fagsakRelasjonTjeneste;
    private final YtelseFordelingTjeneste ytelseFordelingTjeneste;

    @Inject
    public DekningsgradMigreringTask(EntityManager entityManager,
                                     ProsessTaskTjeneste prosessTaskTjeneste,
                                     FagsakRelasjonTjeneste fagsakRelasjonTjeneste,
                                     YtelseFordelingTjeneste ytelseFordelingTjeneste) {
        this.entityManager = entityManager;
        this.prosessTaskTjeneste = prosessTaskTjeneste;
        this.fagsakRelasjonTjeneste = fagsakRelasjonTjeneste;
        this.ytelseFordelingTjeneste = ytelseFordelingTjeneste;
    }

    @Override
    public void doTask(ProsessTaskData prosessTaskData) {
        var yfOpprettetTid = LocalDate.parse(prosessTaskData.getPropertyValue(FOM_DATO_KEY), DateTimeFormatter.ISO_LOCAL_DATE);
        var yfIdProperty = prosessTaskData.getPropertyValue(FRA_YF_ID);
        var yfFagsakId = yfIdProperty == null ? null : Long.valueOf(yfIdProperty);

        var yfPåDato = finnYfOpprettetDato(yfOpprettetTid, yfFagsakId);
        if (yfFagsakId == null) {
            LOG.info("Migrerer dekningsgrad for yf opprettet {}", yfOpprettetTid);
        }

        for (var gryfId : yfPåDato) {
            var ytelseFordelingGrunnlagEntitet = ytelseFordelingTjeneste.hentGrunnlagPåId(gryfId.yfId()).orElseThrow();
            var fagsakRelasjon = fagsakRelasjonTjeneste.finnRelasjonForHvisEksisterer(gryfId.fagsakId(), ytelseFordelingGrunnlagEntitet.getOpprettetTidspunkt());
            fagsakRelasjon.ifPresentOrElse(fr -> oppdaterSakskompleksDG(fr, gryfId.yfId()), () -> LOG.info("Migrerer dekningsgrad, finner ikke fagsakrel for {}", gryfId));
        }

        var tomDato = LocalDate.parse(prosessTaskData.getPropertyValue(TOM_DATO_KEY), DateTimeFormatter.ISO_LOCAL_DATE);
        yfPåDato.stream()
            .map(Yf::yfId)
            .max(Long::compareTo)
            .ifPresentOrElse(nesteId -> prosessTaskTjeneste.lagre(opprettTaskForDato(yfOpprettetTid, tomDato, nesteId)), () -> {
                if (yfOpprettetTid.isBefore(tomDato)) {
                    prosessTaskTjeneste.lagre(opprettTaskForDato(yfOpprettetTid.plusDays(1), tomDato, null));
                }
            });

    }

    private void oppdaterSakskompleksDG(FagsakRelasjon fagsakRelasjon, Long gryfId) {
        var sakskompleksDekningsgrad = fagsakRelasjon.getGjeldendeDekningsgrad().getVerdi();
        LOG.info("Migrerer dekningsgrad, setter sakskompleksDG {} for gryfId {}", sakskompleksDekningsgrad, gryfId);
        entityManager.createNativeQuery(
                "UPDATE GR_YTELSES_FORDELING SET SAKSKOMPLEKS_DEKNINGSGRAD = :dekningsgrad WHERE id = :gryfId")
            .setParameter("dekningsgrad", sakskompleksDekningsgrad)
            .setParameter("gryfId", gryfId)
            .executeUpdate();
    }

    private List<Yf> finnYfOpprettetDato(LocalDate opprettetTid, Long fraYfId) {
        var sql = """
            select * from (select gryf.id yfId, f.id fagsakId from fpsak.fagsak f
                                   join fpsak.behandling b on b.fagsak_id = f.id
                                   join fpsak.gr_ytelses_fordeling gryf on b.id = gryf.behandling_id
            where trunc(gryf.OPPRETTET_TID) =:opprettetTid
            and gryf.SAKSKOMPLEKS_DEKNINGSGRAD is null
            and gryf.id >:fraYfId
            and b.BEHANDLING_TYPE in ('BT-002', 'BT-004')
            order by gryf.id)
            where ROWNUM <= 50
            """;

        var query = entityManager.createNativeQuery(sql, Yf.class)
            .setParameter("opprettetTid", opprettetTid)
            .setParameter("fraYfId", fraYfId == null ? 0 : fraYfId)
            .setHint(HibernateHints.HINT_READ_ONLY, "true");
        return query.getResultList();
    }


    public static ProsessTaskData opprettTaskForDato(LocalDate fomDato, LocalDate tomDato, Long fraYfId) {
        var prosessTaskData = ProsessTaskData.forProsessTask(DekningsgradMigreringTask.class);

        prosessTaskData.setProperty(DekningsgradMigreringTask.FOM_DATO_KEY, fomDato.toString());
        prosessTaskData.setProperty(DekningsgradMigreringTask.TOM_DATO_KEY, tomDato.toString());
        prosessTaskData.setProperty(DekningsgradMigreringTask.FRA_YF_ID, fraYfId == null ? null : String.valueOf(fraYfId));
        prosessTaskData.setCallIdFraEksisterende();
        prosessTaskData.setPrioritet(150);
        return prosessTaskData;
    }

    private record Yf(long yfId, long fagsakId) {
    }
}
