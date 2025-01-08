package no.nav.foreldrepenger.web.app.tjenester.forvaltning.fpsak;

import java.util.Collections;
import java.util.List;
import java.util.Optional;

import jakarta.enterprise.context.Dependent;
import jakarta.inject.Inject;
import jakarta.persistence.EntityManager;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import no.nav.foreldrepenger.behandlingslager.behandling.historikk.Historikkinnslag;
import no.nav.foreldrepenger.behandlingslager.behandling.historikk.Historikkinnslag2;
import no.nav.foreldrepenger.behandlingslager.behandling.historikk.Historikkinnslag2DokumentLink;
import no.nav.foreldrepenger.behandlingslager.behandling.historikk.Historikkinnslag2Repository;
import no.nav.foreldrepenger.behandlingslager.behandling.historikk.HistorikkinnslagDokumentLink;
import no.nav.foreldrepenger.behandlingslager.behandling.historikk.HistorikkinnslagLinjeBuilder;
import no.nav.foreldrepenger.behandlingslager.fagsak.FagsakProsesstaskRekkefølge;
import no.nav.foreldrepenger.web.app.tjenester.behandling.historikk.HistorikkV2Adapter;
import no.nav.foreldrepenger.web.app.tjenester.behandling.historikk.HistorikkinnslagDtoV2;
import no.nav.vedtak.felles.prosesstask.api.ProsessTask;
import no.nav.vedtak.felles.prosesstask.api.ProsessTaskData;
import no.nav.vedtak.felles.prosesstask.api.ProsessTaskHandler;
import no.nav.vedtak.felles.prosesstask.api.ProsessTaskTjeneste;

@Dependent
@ProsessTask(value = "historikkinnslag.migrering", prioritet = 4, maxFailedRuns = 1)
@FagsakProsesstaskRekkefølge(gruppeSekvens = false)
public class HistorikkinnslagMigreringTask implements ProsessTaskHandler {
    private static final Logger LOG = LoggerFactory.getLogger(HistorikkinnslagMigreringTask.class);
    static final String FOM_ID = "fomId";
    static final String TOM_ID = "tomId";

    private final EntityManager entityManager;
    private final Historikkinnslag2Repository historikkinnslag2Repository;
    private final ProsessTaskTjeneste prosessTaskTjeneste;

    @Inject
    public HistorikkinnslagMigreringTask(EntityManager entityManager,
                                         Historikkinnslag2Repository historikkinnslag2Repository,
                                         ProsessTaskTjeneste prosessTaskTjeneste) {
        this.entityManager = entityManager;
        this.historikkinnslag2Repository = historikkinnslag2Repository;
        this.prosessTaskTjeneste = prosessTaskTjeneste;
    }

    @Override
    public void doTask(ProsessTaskData prosessTaskData) {
        Long fraId = Long.valueOf(prosessTaskData.getPropertyValue(FOM_ID));
        var medIdString = prosessTaskData.getPropertyValue(TOM_ID);
        Long medId = Optional.ofNullable(medIdString).map(Long::parseLong).orElse(99999999999L);
        LOG.info("Starter migrering med fomId={} og tomId={}", fraId, medId);
        var startTid = System.currentTimeMillis();
        var historikkinnslagListe = finnNesteHundreHistorikkinnslag(fraId, medId);

        historikkinnslagListe.forEach(h -> {
            if (!erMigrert(h.getId())) {
                LOG.info("Start migrering for historikkinnslag med id={}", h.getId());
                var historikkinnslagDtoV2 = tilHistorikkinnslag2(h);
                var dokumentlinker2 = mapTilDokumentLinker2(h.getDokumentLinker());
                lagreHistorikkinnslag2(h, historikkinnslagDtoV2, dokumentlinker2);
            } else {
                LOG.info("Historikkinnslag med id={} er allerede migrert", h.getId());
            }
        });

        historikkinnslagListe.stream()
            .map(Historikkinnslag::getId)
            .max(Long::compareTo)
            .ifPresent(nesteId -> prosessTaskTjeneste.lagre(opprettNesteTask(nesteId + 1)));

        var sluttTid = System.currentTimeMillis();
        LOG.info("Migrering for fomId={} og tomId={} tar totalt={} ms.", fraId, medId, sluttTid - startTid);
    }

    private boolean erMigrert(Long id) {
        var rader = entityManager.createQuery("select count(1) from Historikkinnslag2 where migrertFraId=:id", Long.class)
            .setParameter("id", id)
            .getSingleResult();
        return rader == 1;
    }

    private ProsessTaskData opprettNesteTask(Long nesteId) {
        var prosessTaskData = ProsessTaskData.forProsessTask(HistorikkinnslagMigreringTask.class);
        prosessTaskData.setProperty(HistorikkinnslagMigreringTask.FOM_ID, nesteId.toString());
        return prosessTaskData;
    }

    private List<Historikkinnslag2DokumentLink> mapTilDokumentLinker2(List<HistorikkinnslagDokumentLink> dokumentLinker) {
        return dokumentLinker.stream()
            .map(d -> new Historikkinnslag2DokumentLink.Builder().medDokumentId(d.getDokumentId())
                .medJournalpostId(d.getJournalpostId())
                .medLinkTekst(d.getLinkTekst())
                .build())
            .toList();
    }

    private void lagreHistorikkinnslag2(Historikkinnslag h, HistorikkinnslagDtoV2 hDtoV2, List<Historikkinnslag2DokumentLink> dokumentLinker) {
        var linjer = hDtoV2.linjer().stream().map(linje -> {
            if (linje.type() == HistorikkinnslagDtoV2.Linje.Type.LINJESKIFT) {
                return HistorikkinnslagLinjeBuilder.LINJESKIFT;
            } else {
                return new HistorikkinnslagLinjeBuilder().tekst(linje.tekst());
            }
        }).toList();

        var historikkinnslag2 = new Historikkinnslag2.Builder().medAktør(hDtoV2.aktør().type())
            .medFagsakId(h.getFagsakId())
            .medBehandlingId(h.getBehandlingId())
            .medTittel(hDtoV2.tittel())
            .medTittel(hDtoV2.skjermlenke())
            .medLinjer(linjer)
            .medMigrertFraId(h.getId())
            .medDokumenter(dokumentLinker)
            .build();
        historikkinnslag2.setOpprettetTidspunkt(h.getOpprettetTidspunkt());
        historikkinnslag2.setOpprettetAv(h.getOpprettetAv());
        historikkinnslag2Repository.lagre(historikkinnslag2);

        LOG.info("Nytt historikkinnslag er migrert med migrertFraId={}", historikkinnslag2.getMigrertFraId());
    }

    private HistorikkinnslagDtoV2 tilHistorikkinnslag2(Historikkinnslag h) {
        return HistorikkV2Adapter.map(h, null, Collections.emptyList(), null);
    }

    private List<Historikkinnslag> finnNesteHundreHistorikkinnslag(Long fraId, Long medId) {
        var sql = """
            select * from (
            select h.* from HISTORIKKINNSLAG h
            where h.ID >=:fraId and h.ID <=:medId
            order by h.id)
            where ROWNUM <= 100
            """;

        var query = entityManager.createNativeQuery(sql, Historikkinnslag.class).setParameter("fraId", fraId).setParameter("medId", medId);
        return query.getResultList();
    }
}
