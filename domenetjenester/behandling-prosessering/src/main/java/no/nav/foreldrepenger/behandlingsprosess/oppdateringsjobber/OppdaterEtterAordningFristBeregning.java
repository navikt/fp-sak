package no.nav.foreldrepenger.behandlingsprosess.oppdateringsjobber;

import static java.time.temporal.TemporalAdjusters.next;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Set;
import java.util.stream.Stream;

import jakarta.enterprise.context.Dependent;
import jakarta.inject.Inject;
import jakarta.persistence.EntityManager;

import no.nav.foreldrepenger.behandlingslager.behandling.Behandling;
import no.nav.foreldrepenger.behandlingslager.behandling.BehandlingStegType;
import no.nav.foreldrepenger.behandlingslager.behandling.aksjonspunkt.AksjonspunktDefinisjon;
import no.nav.foreldrepenger.behandlingslager.behandling.aksjonspunkt.AksjonspunktStatus;
import no.nav.foreldrepenger.behandlingslager.behandling.historikk.HistorikkAktør;
import no.nav.foreldrepenger.behandlingslager.behandling.historikk.Historikkinnslag;
import no.nav.foreldrepenger.behandlingslager.behandling.historikk.HistorikkinnslagRepository;
import no.nav.foreldrepenger.behandlingslager.behandling.repository.BehandlingRepository;
import no.nav.foreldrepenger.behandlingslager.fagsak.FagsakProsesstaskRekkefølge;
import no.nav.foreldrepenger.behandlingslager.fagsak.FagsakYtelseType;
import no.nav.foreldrepenger.behandlingslager.hendelser.StartpunktType;
import no.nav.foreldrepenger.behandlingslager.kodeverk.Kodeverdi;
import no.nav.foreldrepenger.behandlingsprosess.prosessering.BehandlingProsesseringTjeneste;
import no.nav.foreldrepenger.regler.uttak.fastsetteperiode.Virkedager;
import no.nav.vedtak.felles.prosesstask.api.ProsessTask;
import no.nav.vedtak.felles.prosesstask.api.ProsessTaskData;
import no.nav.vedtak.felles.prosesstask.api.ProsessTaskHandler;
import no.nav.vedtak.felles.prosesstask.api.ProsessTaskTjeneste;

@Dependent
@ProsessTask(value = "batch.oppdater.aordningfrist", prioritet = 4, maxFailedRuns = 1)
@FagsakProsesstaskRekkefølge(gruppeSekvens = false)
class OppdaterEtterAordningFristBeregning implements ProsessTaskHandler {

    private static final int A_ORDNING_FRIST_DAG = 5;

    private static final Set<AksjonspunktDefinisjon> REFRESH = Set.of(AksjonspunktDefinisjon.AVKLAR_AKTIVITETER,
        AksjonspunktDefinisjon.VURDER_FAKTA_FOR_ATFL_SN, AksjonspunktDefinisjon.VURDER_PERIODER_MED_OPPTJENING,
        AksjonspunktDefinisjon.FASTSETT_BEREGNINGSGRUNNLAG_ARBEIDSTAKER_FRILANS,
        AksjonspunktDefinisjon.FASTSETT_BEREGNINGSGRUNNLAG_TIDSBEGRENSET_ARBEIDSFORHOLD);

    private final BehandlingProsesseringTjeneste prosesseringTjeneste;
    private final EntityManager entityManager;
    private final BehandlingRepository behandlingRepository;
    private final HistorikkinnslagRepository historikkinnslagRepository;
    private final ProsessTaskTjeneste prosessTaskTjeneste;

    @Inject
    public OppdaterEtterAordningFristBeregning(BehandlingProsesseringTjeneste prosesseringTjeneste,
                                               EntityManager entityManager,
                                               ProsessTaskTjeneste prosessTaskTjeneste) {
        this.prosesseringTjeneste = prosesseringTjeneste;
        this.entityManager = entityManager;
        this.behandlingRepository = new BehandlingRepository(entityManager);
        this.historikkinnslagRepository = new HistorikkinnslagRepository(entityManager);
        this.prosessTaskTjeneste = prosessTaskTjeneste;
    }

    @Override
    public void doTask(ProsessTaskData prosessTaskData) {
        var rapporteringsfrist = finnFristDato(LocalDate.now());
        var nesteVirkedagEtterFrist = Virkedager.plusVirkedager(rapporteringsfrist, 1);

        finnBehandlingerForOppdatering(nesteVirkedagEtterFrist.getDayOfMonth())
            .filter(b -> !b.isBehandlingPåVent())
            .forEach(this::rullTilbakeBehandling);

        // Kjør neste task om en måned
        var nesteMånedFrist = finnFristDato(LocalDate.now().plusMonths(1));
        var nesteKjøring = Virkedager.plusVirkedager(nesteMånedFrist, 2);
        var nesteTask = ProsessTaskData.forProsessTask(OppdaterEtterAordningFristBeregning.class);
        nesteTask.setNesteKjøringEtter(nesteKjøring.atStartOfDay());
        prosessTaskTjeneste.lagre(nesteTask);
    }

    private void rullTilbakeBehandling(Behandling behandling) {
        var lås = behandlingRepository.taSkriveLås(behandling.getId());
        if (behandling.isBehandlingPåVent()) {
            prosesseringTjeneste.taBehandlingAvVent(behandling);
        }
        var tilSteg = finnStegÅHoppeTil(behandling);
        lagHistorikkinnslag(behandling, tilSteg.getNavn());
        prosesseringTjeneste.reposisjonerBehandlingTilbakeTil(behandling, lås, tilSteg);
        prosesseringTjeneste.opprettTasksForGjenopptaOppdaterFortsett(behandling, LocalDateTime.now());
    }

    private BehandlingStegType finnStegÅHoppeTil(Behandling behandling) {
        if (behandling.harÅpentAksjonspunktMedType(AksjonspunktDefinisjon.VURDER_PERIODER_MED_OPPTJENING)) {
            return BehandlingStegType.VURDER_OPPTJENING_FAKTA;
        } else if (FagsakYtelseType.SVANGERSKAPSPENGER.equals(behandling.getFagsakYtelseType())) {
            // SVP har ikke dekningsgradsteg
            return BehandlingStegType.VURDER_SAMLET;
        } else {
            // Ved start i uttak / tilkjent må vi kopiere beregningsgrunnlaget fra originalbehandling i KOFAK steget
            var harStartPunktEtterBeregning =
                StartpunktType.UTTAKSVILKÅR.equals(behandling.getStartpunkt()) || StartpunktType.TILKJENT_YTELSE.equals(behandling.getStartpunkt());
            return harStartPunktEtterBeregning ? BehandlingStegType.KONTROLLER_FAKTA : BehandlingStegType.DEKNINGSGRAD;
        }
    }

    private LocalDate finnFristDato(LocalDate dato) {
        var rapporteringsfrist = dato.withDayOfMonth(A_ORDNING_FRIST_DAG);
        return DayOfWeek.from(rapporteringsfrist).getValue() > DayOfWeek.FRIDAY.getValue()
            ? rapporteringsfrist.with(next(DayOfWeek.MONDAY))
            : rapporteringsfrist;
    }

    private void lagHistorikkinnslag(Behandling behandling, String tilStegNavn) {
        var fraStegNavn = behandling.getAktivtBehandlingSteg() != null ? behandling.getAktivtBehandlingSteg().getNavn() : null;
        var historikkinnslag = new Historikkinnslag.Builder()
            .medAktør(HistorikkAktør.VEDTAKSLØSNINGEN)
            .medFagsakId(behandling.getFagsakId())
            .medBehandlingId(behandling.getId())
            .medTittel("Behandlingen er flyttet")
            .addLinje(String.format("Behandlingen er flyttet fra __%s__ tilbake til __%s__", fraStegNavn, tilStegNavn))
            .build();
        historikkinnslagRepository.lagre(historikkinnslag);
    }

    @SuppressWarnings("unchecked") // getresultList
    private Stream<Behandling> finnBehandlingerForOppdatering(int offset) {
        var sql = """
            select * from (
              select distinct b.*
              from behandling b
              join aksjonspunkt a on a.behandling_id = b.id
              join behandling_resultat br on br.behandling_id = b.id
              join opptjening o on br.inngangsvilkar_resultat_id = o.vilkar_resultat_id
              where aksjonspunkt_def in (:apkode)
                and aksjonspunkt_status = :astatus
                and o.aktiv = 'J'
                and trunc(tom+1, 'mm') + :offset < sysdate
                and sist_oppdatert_tidspunkt < trunc(tom+1, 'mm') + :offset
                and sist_oppdatert_tidspunkt < sysdate
            )
          """;

        var query = entityManager.createNativeQuery(sql, Behandling.class)
            .setParameter("offset", offset)
            .setParameter("apkode", REFRESH.stream().map(Kodeverdi::getKode).toList())
            .setParameter("astatus", AksjonspunktStatus.OPPRETTET.getKode());
        return query.getResultStream();
    }

}
