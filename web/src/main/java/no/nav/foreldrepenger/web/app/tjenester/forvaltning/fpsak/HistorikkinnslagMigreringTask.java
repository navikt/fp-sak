package no.nav.foreldrepenger.web.app.tjenester.forvaltning.fpsak;

import java.util.Collections;
import java.util.List;
import java.util.Optional;

import jakarta.enterprise.context.Dependent;
import jakarta.inject.Inject;
import jakarta.persistence.EntityManager;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import no.nav.foreldrepenger.behandlingslager.behandling.historikk.HistorikkinnslagOld;
import no.nav.foreldrepenger.behandlingslager.behandling.historikk.Historikkinnslag;
import no.nav.foreldrepenger.behandlingslager.behandling.historikk.HistorikkinnslagDokumentLink;
import no.nav.foreldrepenger.behandlingslager.behandling.historikk.HistorikkinnslagRepository;
import no.nav.foreldrepenger.behandlingslager.behandling.historikk.HistorikkinnslagOldDokumentLink;
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
    private static final String FOM_ID = "fomId";
    private static final String TOM_ID = "tomId";

    private final EntityManager entityManager;
    private final HistorikkinnslagRepository historikkinnslagRepository;
    private final ProsessTaskTjeneste prosessTaskTjeneste;

    @Inject
    public HistorikkinnslagMigreringTask(EntityManager entityManager,
                                         HistorikkinnslagRepository historikkinnslagRepository,
                                         ProsessTaskTjeneste prosessTaskTjeneste) {
        this.entityManager = entityManager;
        this.historikkinnslagRepository = historikkinnslagRepository;
        this.prosessTaskTjeneste = prosessTaskTjeneste;
    }

    @Override
    public void doTask(ProsessTaskData prosessTaskData) {
        Long fomId = Long.valueOf(prosessTaskData.getPropertyValue(FOM_ID));
        Long tomId = Optional.ofNullable(prosessTaskData.getPropertyValue(TOM_ID)).map(Long::parseLong).orElse(Long.MAX_VALUE);
        LOG.info("Starter migrering med fomId={} og tomId={}", fomId, tomId);
        var startTid = System.currentTimeMillis();
        var historikkinnslagListe = finnNesteHundreHistorikkinnslag(fomId, tomId);

        historikkinnslagListe.forEach(h -> {
            try {
                migrer(h);
            } catch (Exception e) {
                LOG.info("Historikkinnslag som feilet er {}", h.getId());
                throw e;
            }
        });

        historikkinnslagListe.stream()
            .map(HistorikkinnslagOld::getId)
            .max(Long::compareTo)
            .ifPresent(nesteId -> prosessTaskTjeneste.lagre(opprettNesteTask(nesteId + 1, tomId)));

        var sluttTid = System.currentTimeMillis();
        LOG.info("Migrering for fomId={} og tomId={} tar totalt={} ms.", fomId, tomId, sluttTid - startTid);
    }

    private void migrer(HistorikkinnslagOld h) {
        if (!erMigrert(h.getId())) {
            LOG.debug("Start migrering for historikkinnslag med id={}", h.getId());
            var historikkinnslagDtoV2 = tilHistorikkinnslag2(h);
            var dokumentlinker2 = mapTilDokumentLinker2(h.getDokumentLinker());
            lagreHistorikkinnslag2(h, historikkinnslagDtoV2, dokumentlinker2);
        } else {
            LOG.debug("Historikkinnslag med id={} er allerede migrert", h.getId());
        }
    }

    private boolean erMigrert(Long id) {
        var rader = entityManager.createQuery("select count(1) from Historikkinnslag where migrertFraId=:id", Long.class)
            .setParameter("id", id)
            .getSingleResult();
        return rader == 1;
    }

    private ProsessTaskData opprettNesteTask(Long nesteId, Long tomId) {
        var prosessTaskData = ProsessTaskData.forProsessTask(HistorikkinnslagMigreringTask.class);
        prosessTaskData.setProperty(HistorikkinnslagMigreringTask.FOM_ID, String.valueOf(nesteId));
        prosessTaskData.setProperty(HistorikkinnslagMigreringTask.TOM_ID, String.valueOf(tomId));
        return prosessTaskData;
    }

    private List<HistorikkinnslagDokumentLink> mapTilDokumentLinker2(List<HistorikkinnslagOldDokumentLink> dokumentLinker) {
        return dokumentLinker.stream()
            .map(d -> new HistorikkinnslagDokumentLink.Builder().medDokumentId(d.getDokumentId())
                .medJournalpostId(d.getJournalpostId())
                .medLinkTekst(d.getLinkTekst())
                .build())
            .toList();
    }

    private void lagreHistorikkinnslag2(HistorikkinnslagOld h, HistorikkinnslagDtoV2 hDtoV2, List<HistorikkinnslagDokumentLink> dokumentLinker) {
        var linjer = hDtoV2.linjer().stream().map(linje -> {
            if (linje.type() == HistorikkinnslagDtoV2.Linje.Type.LINJESKIFT) {
                return HistorikkinnslagLinjeBuilder.LINJESKIFT;
            } else {
                return new HistorikkinnslagLinjeBuilder().tekst(linje.tekst().replaceAll("_{3,}", "---"));
            }
        }).toList();

        var historikkinnslag2 = new Historikkinnslag.Builder().medAktør(hDtoV2.aktør().type())
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
        historikkinnslagRepository.lagre(historikkinnslag2);

        LOG.debug("Nytt historikkinnslag er migrert med migrertFraId={}", historikkinnslag2.getMigrertFraId());
    }

    private HistorikkinnslagDtoV2 tilHistorikkinnslag2(HistorikkinnslagOld h) {
        return HistorikkV2Adapter.map(h, null, Collections.emptyList(), null);
    }

    private List<HistorikkinnslagOld> finnNesteHundreHistorikkinnslag(Long fraId, Long medId) {
        var sql = """
            select * from (
            select h.* from HISTORIKKINNSLAG h
            where h.ID >=:fraId and h.ID <=:medId
            order by h.id)
            where ROWNUM <= 100
            """;

        var query = entityManager.createNativeQuery(sql, HistorikkinnslagOld.class).setParameter("fraId", fraId).setParameter("medId", medId);
        return query.getResultList();
    }
}
