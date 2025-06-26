package no.nav.foreldrepenger.behandlingsprosess.oppdateringsjobber;

import static java.time.temporal.TemporalAdjusters.next;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Stream;

import jakarta.enterprise.context.Dependent;
import jakarta.inject.Inject;
import jakarta.persistence.EntityManager;

import no.nav.foreldrepenger.behandlingslager.behandling.Behandling;
import no.nav.foreldrepenger.behandlingslager.behandling.BehandlingStegStatus;
import no.nav.foreldrepenger.behandlingslager.behandling.BehandlingStegType;
import no.nav.foreldrepenger.behandlingslager.behandling.BehandlingType;
import no.nav.foreldrepenger.behandlingslager.behandling.aksjonspunkt.AksjonspunktDefinisjon;
import no.nav.foreldrepenger.behandlingslager.behandling.aksjonspunkt.AksjonspunktStatus;
import no.nav.foreldrepenger.behandlingslager.behandling.historikk.HistorikkAktør;
import no.nav.foreldrepenger.behandlingslager.behandling.historikk.Historikkinnslag;
import no.nav.foreldrepenger.behandlingslager.behandling.historikk.HistorikkinnslagRepository;
import no.nav.foreldrepenger.behandlingslager.behandling.repository.BehandlingRepository;
import no.nav.foreldrepenger.behandlingslager.fagsak.FagsakProsesstaskRekkefølge;
import no.nav.foreldrepenger.behandlingslager.fagsak.FagsakYtelseType;
import no.nav.foreldrepenger.behandlingslager.hendelser.StartpunktType;
import no.nav.foreldrepenger.behandlingsprosess.prosessering.BehandlingProsesseringTjeneste;
import no.nav.foreldrepenger.regler.uttak.fastsetteperiode.Virkedager;
import no.nav.vedtak.felles.prosesstask.api.ProsessTask;
import no.nav.vedtak.felles.prosesstask.api.ProsessTaskData;
import no.nav.vedtak.felles.prosesstask.api.ProsessTaskHandler;

@Dependent
@ProsessTask(value = "batch.oppdater.aordning.midlertidig", prioritet = 4, maxFailedRuns = 1)
@FagsakProsesstaskRekkefølge(gruppeSekvens = false)
class MidlertidigOppdateringTask implements ProsessTaskHandler {

    private static final int A_ORDNING_FRIST_DAG = 5;

    private final BehandlingProsesseringTjeneste prosesseringTjeneste;
    private final EntityManager entityManager;
    private final BehandlingRepository behandlingRepository;
    private final HistorikkinnslagRepository historikkinnslagRepository;

    @Inject
    public MidlertidigOppdateringTask(BehandlingProsesseringTjeneste prosesseringTjeneste,
                                      EntityManager entityManager) {
        this.prosesseringTjeneste = prosesseringTjeneste;
        this.entityManager = entityManager;
        this.behandlingRepository = new BehandlingRepository(entityManager);
        this.historikkinnslagRepository = new HistorikkinnslagRepository(entityManager);
    }

    @Override
    public void doTask(ProsessTaskData prosessTaskData) {
        var rapporteringsfrist = finnFristDato(LocalDate.now());
        var nesteVirkedagEtterFrist = Virkedager.plusVirkedager(rapporteringsfrist, 1);

        finnBehandlingerForOppdateringIM(nesteVirkedagEtterFrist.getDayOfMonth())
            .filter(b -> !b.isBehandlingPåVent())
            .forEach(this::rullTilbakeBehandling);

        finnBehandlingerForOppdateringPermisjon(nesteVirkedagEtterFrist.getDayOfMonth())
            .filter(b -> !b.isBehandlingPåVent())
            .forEach(this::rullTilbakeBehandling);
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
        if (behandling.harÅpentAksjonspunktMedType(AksjonspunktDefinisjon.VURDER_ARBEIDSFORHOLD_INNTEKTSMELDING)) {
            return BehandlingStegType.KONTROLLER_FAKTA_ARBEIDSFORHOLD_INNTEKTSMELDING;
        } else if (behandling.harÅpentAksjonspunktMedType(AksjonspunktDefinisjon.VURDER_PERMISJON_UTEN_SLUTTDATO)) {
            return BehandlingStegType.VURDER_ARB_FORHOLD_PERMISJON;
        } else if (behandling.harÅpentAksjonspunktMedType(AksjonspunktDefinisjon.VURDER_PERIODER_MED_OPPTJENING)) {
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

    private Stream<Behandling> finnBehandlingerForOppdateringIM(int offset) {
        var sql = """
            select * from (
              select distinct b.*
              from behandling b
              join aksjonspunkt a on a.behandling_id = b.id
              join behandling_steg_tilstand st on st.behandling_id = b.id
              where behandling_steg = :steg and behandling_type = :forste
                and aksjonspunkt_def = :apkode
                and aksjonspunkt_status = :astatus
                and behandling_steg_status in (:ferdig)
                and sist_oppdatert_tidspunkt < st.opprettet_tid + 28
                and sist_oppdatert_tidspunkt < trunc(st.opprettet_tid + 28, 'mm') + :offset
            )
          """;

        var query = entityManager.createNativeQuery(sql, Behandling.class)
            .setParameter("offset", offset)
            .setParameter("apkode", AksjonspunktDefinisjon.VURDER_ARBEIDSFORHOLD_INNTEKTSMELDING.getKode())
            .setParameter("astatus", AksjonspunktStatus.OPPRETTET.getKode())
            .setParameter("forste", BehandlingType.FØRSTEGANGSSØKNAD.getKode())
            .setParameter("steg", BehandlingStegType.VURDER_KOMPLETT_BEH.getKode())
            .setParameter("ferdig", List.of(BehandlingStegStatus.UTFØRT.getKode(), BehandlingStegStatus.AVBRUTT.getKode()));
        return query.getResultStream();
    }

    @SuppressWarnings("unchecked") // getresultList
    private Stream<Behandling> finnBehandlingerForOppdateringPermisjon(int offset) {
        var sql = """
            select * from (
              select distinct b.*
              from behandling b
              join aksjonspunkt a on a.behandling_id = b.id
              where aksjonspunkt_def = :apkode
                and aksjonspunkt_status = :astatus
                and sist_oppdatert_tidspunkt < trunc(sysdate, 'mm') + :offset
            )
          """;

        var query = entityManager.createNativeQuery(sql, Behandling.class)
            .setParameter("offset", offset)
            .setParameter("apkode", AksjonspunktDefinisjon.VURDER_PERMISJON_UTEN_SLUTTDATO.getKode())
            .setParameter("astatus", AksjonspunktStatus.OPPRETTET.getKode());
        return query.getResultStream();
    }

}
