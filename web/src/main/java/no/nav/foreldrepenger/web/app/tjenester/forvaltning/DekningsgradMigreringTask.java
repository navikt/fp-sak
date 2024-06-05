package no.nav.foreldrepenger.web.app.tjenester.forvaltning;

import static no.nav.foreldrepenger.behandlingslager.behandling.BehandlingStegType.VURDER_SAMLET;

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
import no.nav.foreldrepenger.behandlingslager.behandling.Behandling;
import no.nav.foreldrepenger.behandlingslager.behandling.ytelsefordeling.YtelsesFordelingRepository;
import no.nav.foreldrepenger.behandlingslager.fagsak.FagsakProsesstaskRekkefølge;
import no.nav.foreldrepenger.behandlingslager.fagsak.FagsakRelasjon;
import no.nav.vedtak.felles.prosesstask.api.ProsessTask;
import no.nav.vedtak.felles.prosesstask.api.ProsessTaskData;
import no.nav.vedtak.felles.prosesstask.api.ProsessTaskHandler;
import no.nav.vedtak.felles.prosesstask.api.ProsessTaskTjeneste;

@Dependent
@ProsessTask(value = "dekningsgrad.migrering", prioritet = 4, maxFailedRuns = 1)
@FagsakProsesstaskRekkefølge(gruppeSekvens = false)
class DekningsgradMigreringTask implements ProsessTaskHandler {

    private static final Logger LOG = LoggerFactory.getLogger(DekningsgradMigreringTask.class);
    static final String FOM_DATO_KEY = "fom";
    static final String TOM_DATO_KEY = "tom";
    private static final String FRA_BEHANDLING_ID = "fraBehandlingId";

    private final EntityManager entityManager;
    private final ProsessTaskTjeneste prosessTaskTjeneste;
    private final FagsakRelasjonTjeneste fagsakRelasjonTjeneste;
    private final YtelsesFordelingRepository ytelsesFordelingRepository;

    @Inject
    public DekningsgradMigreringTask(EntityManager entityManager,
                                     ProsessTaskTjeneste prosessTaskTjeneste,
                                     FagsakRelasjonTjeneste fagsakRelasjonTjeneste,
                                     YtelsesFordelingRepository ytelsesFordelingRepository) {
        this.entityManager = entityManager;
        this.prosessTaskTjeneste = prosessTaskTjeneste;
        this.fagsakRelasjonTjeneste = fagsakRelasjonTjeneste;
        this.ytelsesFordelingRepository = ytelsesFordelingRepository;
    }

    @Override
    public void doTask(ProsessTaskData prosessTaskData) {
        var datoOpprettet = LocalDate.parse(prosessTaskData.getPropertyValue(FOM_DATO_KEY), DateTimeFormatter.ISO_LOCAL_DATE);
        var behandlingIdProperty = prosessTaskData.getPropertyValue(FRA_BEHANDLING_ID);
        var fraBehandlingId = behandlingIdProperty == null ? null : Long.valueOf(behandlingIdProperty);

        var behandlingerOpprettetDato = finnBehandlingerOpprettetDato(datoOpprettet, fraBehandlingId);
        if (fraBehandlingId == null) {
            LOG.info("Migrerer dekningsgrad for behandlinger opprettet {}", datoOpprettet);
        }

        for (var behandling : behandlingerOpprettetDato) {
            oppdaterSakskompleksDG(behandling);
        }

        var tomDato = LocalDate.parse(prosessTaskData.getPropertyValue(TOM_DATO_KEY), DateTimeFormatter.ISO_LOCAL_DATE);
        behandlingerOpprettetDato.stream()
            .map(Behandling::getId)
            .max(Long::compareTo)
            .ifPresentOrElse(nesteId -> prosessTaskTjeneste.lagre(opprettTaskForDato(datoOpprettet, tomDato, nesteId)), () -> {
                if (datoOpprettet.isBefore(tomDato)) {
                    prosessTaskTjeneste.lagre(opprettTaskForDato(datoOpprettet.plusDays(1), tomDato, null));
                }
            });
    }

    private void oppdaterSakskompleksDG(Behandling behandling) {
        var alleSteg = behandling.getBehandlingStegTilstandHistorikk().toList();
        if (!behandling.erRevurdering() && alleSteg.stream().noneMatch(steg -> steg.getBehandlingSteg() == VURDER_SAMLET)) {
            LOG.info("Migrerer dekningsgrad, behandling {} ikke kommet langt nok for å oppdatere sakskompleksDG ", behandling.getId());
            return;
        }
        var aktivPåTidspunkt = alleSteg.isEmpty() ? behandling.getOpprettetTidspunkt() : behandling.getSisteBehandlingStegTilstand().orElseThrow().getOpprettetTidspunkt();
        LOG.info("Migrerer dekningsgrad, fant aktivtidspunkt {} for behanding {}", aktivPåTidspunkt, behandling.getId());
        var fagsakRelasjon = fagsakRelasjonTjeneste.finnRelasjonForHvisEksisterer(behandling.getFagsakId(), aktivPåTidspunkt);
        fagsakRelasjon.ifPresentOrElse(fr -> oppdaterSakskompleksDG(fr, behandling.getId()), () -> LOG.info("Migrerer dekningsgrad, finner ikke fagsakrel for {}",
            behandling.getId()));
    }

    private void oppdaterSakskompleksDG(FagsakRelasjon fagsakRelasjon, Long behandlingId) {
        var grunnlagId = ytelsesFordelingRepository.hentIdPåAktivYtelsesFordeling(behandlingId).orElseThrow();

        var sakskompleksDekningsgrad = fagsakRelasjon.getGjeldendeDekningsgrad().getVerdi();
        LOG.info("Migrerer dekningsgrad, setter sakskompleksDG {} på gryf {} for behandling {}", sakskompleksDekningsgrad, grunnlagId, behandlingId);
        entityManager.createNativeQuery(
                "UPDATE GR_YTELSES_FORDELING SET SAKSKOMPLEKS_DEKNINGSGRAD = :dekningsgrad WHERE id = :gryfId")
            .setParameter("dekningsgrad", sakskompleksDekningsgrad)
            .setParameter("gryfId", grunnlagId)
            .executeUpdate();
    }

    private List<Behandling> finnBehandlingerOpprettetDato(LocalDate opprettetDato, Long fraBId) {
        var sql = """
            select * from (select b.* from fpsak.behandling b join fpsak.gr_ytelses_fordeling gryf on b.id = gryf.behandling_id
            where trunc(b.OPPRETTET_TID) =:opprettetTid
            and gryf.SAKSKOMPLEKS_DEKNINGSGRAD is null
            and gryf.AKTIV = 'J'
            and b.id >:fraBId
            and b.BEHANDLING_TYPE in ('BT-002', 'BT-004')
            order by b.id)
            where ROWNUM <= 50
            """;

        var query = entityManager.createNativeQuery(sql, Behandling.class)
            .setParameter("opprettetTid", opprettetDato)
            .setParameter("fraBId", fraBId == null ? 0 : fraBId)
            .setHint(HibernateHints.HINT_READ_ONLY, "true");
        return query.getResultList();
    }


    public static ProsessTaskData opprettTaskForDato(LocalDate fomDato, LocalDate tomDato, Long fraBehandlingId) {
        var prosessTaskData = ProsessTaskData.forProsessTask(DekningsgradMigreringTask.class);

        prosessTaskData.setProperty(DekningsgradMigreringTask.FOM_DATO_KEY, fomDato.toString());
        prosessTaskData.setProperty(DekningsgradMigreringTask.TOM_DATO_KEY, tomDato.toString());
        prosessTaskData.setProperty(DekningsgradMigreringTask.FRA_BEHANDLING_ID, fraBehandlingId == null ? null : String.valueOf(fraBehandlingId));
        prosessTaskData.setCallIdFraEksisterende();
        return prosessTaskData;
    }
}
