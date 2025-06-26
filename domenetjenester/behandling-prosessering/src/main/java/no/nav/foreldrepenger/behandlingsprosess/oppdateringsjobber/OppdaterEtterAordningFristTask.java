package no.nav.foreldrepenger.behandlingsprosess.oppdateringsjobber;

import static java.time.temporal.TemporalAdjusters.next;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Set;
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
class OppdaterEtterAordningFristTask implements ProsessTaskHandler {

    private static final int A_ORDNING_FRIST_DAG = 5;
    private static final String OFFSET = "offset";
    private static final String AKSJONSPUNKT_KODE = "apkode";
    private static final String AKSJONSPUNKT_OPPRETTET = "astatus";

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
    public OppdaterEtterAordningFristTask(BehandlingProsesseringTjeneste prosesseringTjeneste,
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

        // vil gjøre tilbakehopp for revurderinger, mens oppdateringsmekanikken for førstegang vil håndtere inntektsendringer
        finnBehandlingerForOppdateringOpptjeningBeregning(nesteVirkedagEtterFrist.getDayOfMonth())
            .filter(b -> !b.isBehandlingPåVent())
            .forEach(this::rullTilbakeBehandling);

        // Denne kan ha så liten effekt vs volum at den muligens kuttes ut.
        // StartpunktutlederIAY håndterer aksjonspunkter som ikke lenger trengs - del av registeroppdatering
        finnBehandlingerForOppdateringIM(nesteVirkedagEtterFrist.getDayOfMonth())
            .filter(b -> !b.isBehandlingPåVent())
            .forEach(b -> prosesseringTjeneste.opprettTasksForGjenopptaOppdaterFortsett(b, LocalDateTime.now()));

        // StartpunktutlederIAY håndterer aksjonspunkter som ikke lenger trengs - del av registeroppdatering
        finnBehandlingerForOppdateringPermisjon(nesteVirkedagEtterFrist.getDayOfMonth())
            .filter(b -> !b.isBehandlingPåVent())
            .forEach(b -> prosesseringTjeneste.opprettTasksForGjenopptaOppdaterFortsett(b, LocalDateTime.now()));

        // Kjør neste task om en måned
        var nesteMånedFrist = finnFristDato(LocalDate.now().plusMonths(1));
        var nesteKjøring = Virkedager.plusVirkedager(nesteMånedFrist, 2);
        var nesteTask = ProsessTaskData.forProsessTask(OppdaterEtterAordningFristTask.class);
        nesteTask.setNesteKjøringEtter(nesteKjøring.atStartOfDay().plusHours(6).plusMinutes(25));
        prosessTaskTjeneste.lagre(nesteTask);
    }

    private void rullTilbakeBehandling(Behandling behandling) {
        var lås = behandlingRepository.taSkriveLås(behandling.getId());
        // Startpunkt for endring av inntekt før STP er opptjening i førstegang (vil gjøre tilbakehopp) og udefinert for revurderinger
        if (behandling.erRevurdering()) {
            var tilSteg = finnStegÅHoppeTil(behandling);
            lagHistorikkinnslag(behandling, tilSteg.getNavn());
            prosesseringTjeneste.reposisjonerBehandlingTilbakeTil(behandling, lås, tilSteg);
        }
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
    private Stream<Behandling> finnBehandlingerForOppdateringOpptjeningBeregning(int offset) {
        // Her skal vi vurdere om behandling er oppdatert etter rapporteringsfrist for siste hele måned før STP
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
            .setParameter(OFFSET, offset)
            .setParameter(AKSJONSPUNKT_KODE, REFRESH.stream().map(Kodeverdi::getKode).toList())
            .setParameter(AKSJONSPUNKT_OPPRETTET, AksjonspunktStatus.OPPRETTET.getKode());
        return query.getResultStream();
    }

    @SuppressWarnings("unchecked") // getresultList
    private Stream<Behandling> finnBehandlingerForOppdateringIM(int offset) {
        // Ser på inngang til steg VURDER_KOMPLETT_BEH - da skal vi ære 4 uker før skjæringstidspunktet (STP)
        // Aksjonspunktet avhenger av status for arbeidsforhold på STP, så tar med fram til rapporteringsfrist for STP-måneden
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
                and sist_oppdatert_tidspunkt < trunc(add_months(st.opprettet_tid + 28, 1), 'mm') + :offset
            )
          """;

        var query = entityManager.createNativeQuery(sql, Behandling.class)
            .setParameter(OFFSET, offset)
            .setParameter(AKSJONSPUNKT_KODE, AksjonspunktDefinisjon.VURDER_ARBEIDSFORHOLD_INNTEKTSMELDING.getKode())
            .setParameter(AKSJONSPUNKT_OPPRETTET, AksjonspunktStatus.OPPRETTET.getKode())
            .setParameter("forste", BehandlingType.FØRSTEGANGSSØKNAD.getKode())
            .setParameter("steg", BehandlingStegType.VURDER_KOMPLETT_BEH.getKode())
            .setParameter("ferdig", List.of(BehandlingStegStatus.UTFØRT.getKode(), BehandlingStegStatus.AVBRUTT.getKode()));
        return query.getResultStream();
    }

    @SuppressWarnings("unchecked") // getresultList
    private Stream<Behandling> finnBehandlingerForOppdateringPermisjon(int offset) {
        // Gjelder arbeidsforhold med permisjon på STP uten sluttdato
        // Disse kan rapporteres når som helst så sjekker om sist oppdatert før rapporteringsfrist for forrige måned er passert
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
            .setParameter(OFFSET, offset)
            .setParameter(AKSJONSPUNKT_KODE, AksjonspunktDefinisjon.VURDER_PERMISJON_UTEN_SLUTTDATO.getKode())
            .setParameter(AKSJONSPUNKT_OPPRETTET, AksjonspunktStatus.OPPRETTET.getKode());
        return query.getResultStream();
    }

}
