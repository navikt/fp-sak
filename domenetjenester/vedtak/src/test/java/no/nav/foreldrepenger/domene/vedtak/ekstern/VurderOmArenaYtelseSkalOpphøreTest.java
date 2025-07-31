package no.nav.foreldrepenger.domene.vedtak.ekstern;

import static java.time.LocalDate.now;
import static java.util.Optional.empty;
import static org.assertj.core.api.Assertions.assertThat;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import jakarta.persistence.EntityManager;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import no.nav.foreldrepenger.behandlingslager.behandling.Behandling;
import no.nav.foreldrepenger.behandlingslager.behandling.BehandlingResultatType;
import no.nav.foreldrepenger.behandlingslager.behandling.Behandlingsresultat;
import no.nav.foreldrepenger.behandlingslager.behandling.beregning.AktivitetStatus;
import no.nav.foreldrepenger.behandlingslager.behandling.beregning.BeregningsresultatAndel;
import no.nav.foreldrepenger.behandlingslager.behandling.beregning.BeregningsresultatEntitet;
import no.nav.foreldrepenger.behandlingslager.behandling.beregning.BeregningsresultatPeriode;
import no.nav.foreldrepenger.behandlingslager.behandling.beregning.BeregningsresultatRepository;
import no.nav.foreldrepenger.behandlingslager.behandling.beregning.Inntektskategori;
import no.nav.foreldrepenger.behandlingslager.behandling.repository.BehandlingRepository;
import no.nav.foreldrepenger.behandlingslager.behandling.repository.BehandlingRepositoryProvider;
import no.nav.foreldrepenger.behandlingslager.behandling.vedtak.BehandlingVedtak;
import no.nav.foreldrepenger.behandlingslager.behandling.vedtak.BehandlingVedtakRepository;
import no.nav.foreldrepenger.behandlingslager.behandling.vedtak.VedtakResultatType;
import no.nav.foreldrepenger.behandlingslager.kodeverk.Fagsystem;
import no.nav.foreldrepenger.behandlingslager.testutilities.behandling.AbstractTestScenario;
import no.nav.foreldrepenger.behandlingslager.testutilities.behandling.ScenarioMorSøkerForeldrepenger;
import no.nav.foreldrepenger.behandlingslager.ytelse.RelatertYtelseType;
import no.nav.foreldrepenger.dbstoette.EntityManagerAwareTest;
import no.nav.foreldrepenger.domene.abakus.AbakusInMemoryInntektArbeidYtelseTjeneste;
import no.nav.foreldrepenger.domene.arbeidsforhold.InntektArbeidYtelseTjeneste;
import no.nav.foreldrepenger.domene.iay.modell.InntektArbeidYtelseAggregatBuilder;
import no.nav.foreldrepenger.domene.iay.modell.VersjonType;
import no.nav.foreldrepenger.domene.iay.modell.YtelseAnvist;
import no.nav.foreldrepenger.domene.iay.modell.YtelseBuilder;
import no.nav.foreldrepenger.domene.iay.modell.kodeverk.RelatertYtelseTilstand;
import no.nav.foreldrepenger.domene.tid.DatoIntervallEntitet;
import no.nav.foreldrepenger.domene.typer.AktørId;
import no.nav.foreldrepenger.domene.typer.Saksnummer;

class VurderOmArenaYtelseSkalOpphøreTest extends EntityManagerAwareTest {

    private BehandlingRepositoryProvider repositoryProvider;
    private BehandlingRepository behandlingRepository;
    private BehandlingVedtakRepository behandlingVedtakRepository;
    private EntityManager entityManager;
    private BeregningsresultatRepository beregningsresultatRepository;

    private static final AktørId AKTØR_ID = AktørId.dummy();
    private static final String SAK_ID = "1200095";
    private static final LocalDate SKJÆRINGSTIDSPUNKT = LocalDate.now();
    private static final Long MELDEKORTPERIODE = 14L;

    private final ScenarioMorSøkerForeldrepenger scenario = ScenarioMorSøkerForeldrepenger.forFødselMedGittAktørId(AKTØR_ID);
    private InntektArbeidYtelseTjeneste iayTjeneste;
    private VurderOmArenaYtelseSkalOpphøre vurdereOmArenaYtelseSkalOpphør;

    private Behandling behandling;

    private BeregningsresultatEntitet.Builder beregningsresultatFPBuilder;

    private Behandling lagre(AbstractTestScenario<?> scenario) {
        return scenario.lagre(repositoryProvider);
    }

    @BeforeEach
    void setUp() {
        var entityManager = getEntityManager();
        repositoryProvider = new BehandlingRepositoryProvider(entityManager);
        behandlingRepository = repositoryProvider.getBehandlingRepository();
        behandlingVedtakRepository = repositoryProvider.getBehandlingVedtakRepository();
        this.entityManager = entityManager;
        beregningsresultatRepository = new BeregningsresultatRepository(entityManager);
        iayTjeneste = new AbakusInMemoryInntektArbeidYtelseTjeneste();
        vurdereOmArenaYtelseSkalOpphør = new VurderOmArenaYtelseSkalOpphøre(
            beregningsresultatRepository,
            iayTjeneste, behandlingVedtakRepository, null);
    }

    @Test
    void skal_teste_arena_ytelser_finnes_ikke() {
        // Arrange
        var vedtaksDato = SKJÆRINGSTIDSPUNKT.minusDays(7);
        byggScenarioUtenYtelseIArena();
        // Act
        var resultat = vurdereOmArenaYtelseSkalOpphør.vurderArenaYtelserOpphøres(behandling.getId(), behandling.getAktørId(), SKJÆRINGSTIDSPUNKT, vedtaksDato);
        // Assert
        assertThat(resultat).isFalse();
    }

    // T1: Siste utbetalingsdato for ARENA-ytelse før vedtaksdato for foreldrepenger
    // T2: Første forventede utbetalingsdato for ARENA-ytelse etter vedtaksdato for foreldrepenger

    @Test
    void skal_teste_startdatoFP_før_T1() {
        // Arrange
        // Startdato før T1 , vedtaksdato etter T1
        var meldekortT1 = SKJÆRINGSTIDSPUNKT.plusDays(2);
        var ytelseVedtakFOM = meldekortT1.minusDays(MELDEKORTPERIODE);
        var ytelseVedtakTOM = SKJÆRINGSTIDSPUNKT.plusDays(MELDEKORTPERIODE * 2);
        var vedtaksDato = SKJÆRINGSTIDSPUNKT.plusDays(36);
        var startDatoFP = SKJÆRINGSTIDSPUNKT;
        byggScenario(ytelseVedtakFOM, ytelseVedtakTOM, meldekortT1, vedtaksDato, startDatoFP, Fagsystem.ARENA);
        // Act
        var resultat = vurdereOmArenaYtelseSkalOpphør.vurderArenaYtelserOpphøres(behandling.getId(), behandling.getAktørId(), startDatoFP, vedtaksDato);
        // Assert
        assertThat(resultat).isTrue();
    }

    @Test
    void skal_teste_startdatoFP_før_T1_FPFEIL_3526() {
        // Arrange
        // Startdato før T1 , vedtaksdato etter T1
        var meldekortT1 = LocalDate.of(2018, 8, 12);
        var ytelseVedtakFOM = LocalDate.of(2018, 7, 29);
        var ytelseVedtakTOM = LocalDate.of(2018, 8, 26);
        var vedtaksDato = LocalDate.of(2018, 11, 14);
        var startDatoFP = LocalDate.of(2018, 4, 6);
        byggScenario(ytelseVedtakFOM, ytelseVedtakTOM, meldekortT1, vedtaksDato.atStartOfDay(), DatoIntervallEntitet.fraOgMedTilOgMed(startDatoFP, startDatoFP.plusWeeks(34)), Fagsystem.ARENA);
        // Act
        var resultat = vurdereOmArenaYtelseSkalOpphør.vurderArenaYtelserOpphøres(behandling.getId(), behandling.getAktørId(), startDatoFP, vedtaksDato);
        // Assert
        assertThat(resultat).isTrue();
    }

    @Test
    void skal_teste_startdatoFP_etter_T2() {
        // Arrange
        // Startdato før T2, vedtaksdato etter T2
        var meldekortT1 = SKJÆRINGSTIDSPUNKT.minusDays(64);
        var ytelseVedtakFOM = meldekortT1.minusDays(MELDEKORTPERIODE);
        var ytelseVedtakTOM = SKJÆRINGSTIDSPUNKT.plusDays(19);
        var vedtaksDato = SKJÆRINGSTIDSPUNKT.minusDays(49);
        var startDatoFP = SKJÆRINGSTIDSPUNKT;
        byggScenario(ytelseVedtakFOM, ytelseVedtakTOM, meldekortT1, vedtaksDato, startDatoFP, Fagsystem.ARENA);
        // Act
        var resultat = vurdereOmArenaYtelseSkalOpphør.vurderArenaYtelserOpphøres(behandling.getId(), behandling.getAktørId(), startDatoFP, vedtaksDato);
        // Assert
        assertThat(resultat).isFalse();
    }

    @Test
    void skal_teste_startdatoFP_mellom_T1_T2_vedtaksdato_mindre_enn_8_dager_etter_T1() {
        // Arrange
        // startdato mellom T1 og T2, vedtaksdato mellom T1 og (T1 + 8 dager)
        var meldekortT1 = SKJÆRINGSTIDSPUNKT.minusDays(5);
        var ytelseVedtakFOM = meldekortT1.minusDays(MELDEKORTPERIODE);
        var ytelseVedtakTOM = SKJÆRINGSTIDSPUNKT.plusDays(MELDEKORTPERIODE * 2);
        var vedtaksDato = SKJÆRINGSTIDSPUNKT.minusDays(4);
        var startDatoFP = SKJÆRINGSTIDSPUNKT;
        byggScenario(ytelseVedtakFOM, ytelseVedtakTOM, meldekortT1, vedtaksDato, startDatoFP, Fagsystem.ARENA);
        // Act
        var resultat = vurdereOmArenaYtelseSkalOpphør.vurderArenaYtelserOpphøres(behandling.getId(), behandling.getAktørId(), startDatoFP, vedtaksDato);
        // Assert
        assertThat(resultat).isFalse();
    }

    @Test
    void skal_teste_startdatoFP_mellom_T1_T2_vedtaksdato_mindre_enn_8_dager_før_T2() {
        // Arrange
        // startdato mellom T1 og T2, vedtaksdato mellom (T2 - 8 dager) og T2
        var meldekortT1 = SKJÆRINGSTIDSPUNKT.minusDays(8);
        var ytelseVedtakFOM = meldekortT1.minusDays(MELDEKORTPERIODE);
        var ytelseVedtakTOM = SKJÆRINGSTIDSPUNKT.plusDays(MELDEKORTPERIODE * 2);
        var vedtaksDato = SKJÆRINGSTIDSPUNKT.plusDays(4);
        var startDatoFP = SKJÆRINGSTIDSPUNKT;
        byggScenario(ytelseVedtakFOM, ytelseVedtakTOM, meldekortT1, vedtaksDato, startDatoFP, Fagsystem.ARENA);
        // Act
        var resultat = vurdereOmArenaYtelseSkalOpphør.vurderArenaYtelserOpphøres(behandling.getId(), behandling.getAktørId(), startDatoFP, vedtaksDato);
        // Assert
        assertThat(resultat).isTrue();
    }

    @Test
    void skal_teste_Arena_ytelse_interval_før_vedtaksdato_fom_overlapper_FP() {
        // Arrange
        // Arena ytelser etter startdato men før vedtaksdato .
        var meldekortT1 = SKJÆRINGSTIDSPUNKT.plusDays(MELDEKORTPERIODE + 7);
        var ytelseVedtakFOM = meldekortT1.minusDays(MELDEKORTPERIODE);
        var ytelseVedtakTOM = meldekortT1.plusDays(MELDEKORTPERIODE);
        var vedtaksDato = SKJÆRINGSTIDSPUNKT.plusDays(47);
        var startDatoFP = SKJÆRINGSTIDSPUNKT;
        byggScenario(ytelseVedtakFOM, ytelseVedtakTOM, meldekortT1, vedtaksDato, startDatoFP, Fagsystem.ARENA);
        // Act
        var resultat = vurdereOmArenaYtelseSkalOpphør.vurderArenaYtelserOpphøres(behandling.getId(), behandling.getAktørId(), startDatoFP, vedtaksDato);
        // Assert
        assertThat(resultat).isTrue();
    }

    @Test
    void skal_teste_startdato_er_like_T1_og_T2_er_null() {
        // Arrange
        // Arena ytelser før vedtaksdato og mellom startdato FP og sluttdato FP.
        var ytelseVedtakFOM = SKJÆRINGSTIDSPUNKT.minusDays(MELDEKORTPERIODE * 2);
        var ytelseVedtakTOM = SKJÆRINGSTIDSPUNKT.minusDays(1);
        var vedtaksDato = SKJÆRINGSTIDSPUNKT.plusDays(47);
        var startDatoFP = SKJÆRINGSTIDSPUNKT.minusDays(1);
        byggScenario(ytelseVedtakFOM, ytelseVedtakTOM, SKJÆRINGSTIDSPUNKT, vedtaksDato, startDatoFP, Fagsystem.ARENA);
        // Act
        var resultat = vurdereOmArenaYtelseSkalOpphør.vurderArenaYtelserOpphøres(behandling.getId(), behandling.getAktørId(), startDatoFP, vedtaksDato);
        // Assert
        assertThat(resultat).isFalse();
    }

    @Test
    void skal_teste_vedtaksdato_er_like_T2() {
        // Arrange
        // Arena ytelser før vedtaksdato og mellom startdato FP og sluttdato FP.
        var meldekortT1 = SKJÆRINGSTIDSPUNKT.minusDays(MELDEKORTPERIODE);
        var ytelseVedtakFOM = meldekortT1.minusDays(MELDEKORTPERIODE);
        var ytelseVedtakTOM = SKJÆRINGSTIDSPUNKT.plusDays(MELDEKORTPERIODE * 2);
        var vedtaksDato = SKJÆRINGSTIDSPUNKT;
        var startDatoFP = SKJÆRINGSTIDSPUNKT.plusDays(10);
        byggScenario(ytelseVedtakFOM, ytelseVedtakTOM, meldekortT1, vedtaksDato, startDatoFP, Fagsystem.ARENA);
        // Act
        var resultat = vurdereOmArenaYtelseSkalOpphør.vurderArenaYtelserOpphøres(behandling.getId(), behandling.getAktørId(), startDatoFP, vedtaksDato);
        // Assert
        assertThat(resultat).isFalse();
    }

    @Test
    void skal_teste_startdato_før_T1_og_FP_overlapper_ikke_ARENA_ytelse() {
        // Arrange
        // Arena ytelser før vedtaksdato og utenfor startdato FP og sluttdato FP.
        var meldekortT1 = SKJÆRINGSTIDSPUNKT.plusDays(7);
        var ytelseVedtakFOM = meldekortT1.minusDays(MELDEKORTPERIODE);
        var ytelseVedtakTOM = SKJÆRINGSTIDSPUNKT.plusDays(MELDEKORTPERIODE * 2);
        var vedtaksDato = SKJÆRINGSTIDSPUNKT.plusDays(54);
        var startDatoFP = SKJÆRINGSTIDSPUNKT;
        byggScenario(ytelseVedtakFOM, ytelseVedtakTOM, meldekortT1, vedtaksDato, startDatoFP, Fagsystem.ARENA);
        // Act
        var resultat = vurdereOmArenaYtelseSkalOpphør.vurderArenaYtelserOpphøres(behandling.getId(), behandling.getAktørId(), startDatoFP, vedtaksDato);
        // Assert
        assertThat(resultat).isTrue();
    }

    @Test
    void skal_teste_startdato_for_5910() {
        // Arrange
        // Arena ytelser før vedtaksdato og utenfor startdato FP og sluttdato FP.
        var meldekortT1 = SKJÆRINGSTIDSPUNKT.minusDays(5); // 2019-02-03
        var ytelseVedtakFOM = meldekortT1.minusDays(MELDEKORTPERIODE); // 2019-01-21
        var ytelseVedtakTOM = SKJÆRINGSTIDSPUNKT.plusMonths(6);// 2019-10-01
        var vedtaksDato = SKJÆRINGSTIDSPUNKT.plusDays(7); // 2019-02-15
        var startDatoFP = SKJÆRINGSTIDSPUNKT; // 2019-02-08
        byggScenario(ytelseVedtakFOM, ytelseVedtakTOM, meldekortT1, vedtaksDato, startDatoFP, Fagsystem.ARENA);
        // Act
        var resultat = vurdereOmArenaYtelseSkalOpphør.vurderArenaYtelserOpphøres(behandling.getId(), behandling.getAktørId(), startDatoFP, vedtaksDato);
        // Assert
        assertThat(resultat).isTrue();
    }

    @Test
    void ytelse_avsluttet_før_stp_siste_meldekort_rett_etter() {
        // Arrange
        // Siste meldekort vil som regel komme rett etter perioden og med prosent <200
        var meldekortT1 = SKJÆRINGSTIDSPUNKT.plusDays(2); // 2019-02-03
        var ytelseVedtakFOM = SKJÆRINGSTIDSPUNKT.minusDays(MELDEKORTPERIODE * 2 - 1); // 2019-01-21
        var ytelseVedtakTOM = SKJÆRINGSTIDSPUNKT.minusDays(1);// 2019-10-01
        var vedtaksDato = SKJÆRINGSTIDSPUNKT.plusDays(7); // 2019-02-15
        var startDatoFP = SKJÆRINGSTIDSPUNKT; // 2019-02-08
        byggScenario(ytelseVedtakFOM, ytelseVedtakTOM, meldekortT1, vedtaksDato, startDatoFP, Fagsystem.ARENA);
        // Act
        var resultat = vurdereOmArenaYtelseSkalOpphør.vurderArenaYtelserOpphøres(behandling.getId(), behandling.getAktørId(), startDatoFP, vedtaksDato);
        // Assert
        assertThat(resultat).isFalse();
    }

    @Test
    void vanlig_case_vedtak_før_start() {
        // Arrange
        // Gir arena nok tid til å avslutte løpende ytelse
        var meldekortT1 = SKJÆRINGSTIDSPUNKT.minusDays(16); // 2019-02-03
        var ytelseVedtakFOM = meldekortT1.minusDays(MELDEKORTPERIODE * 8); // 2019-01-21
        var ytelseVedtakTOM = meldekortT1.plusDays(MELDEKORTPERIODE * 8);// 2019-10-01
        var vedtaksDato = SKJÆRINGSTIDSPUNKT.minusDays(14); // 2019-02-15
        var startDatoFP = SKJÆRINGSTIDSPUNKT; // 2019-02-08
        byggScenario(ytelseVedtakFOM, ytelseVedtakTOM, meldekortT1, vedtaksDato, startDatoFP, Fagsystem.ARENA);
        // Act
        var resultat = vurdereOmArenaYtelseSkalOpphør.vurderArenaYtelserOpphøres(behandling.getId(), behandling.getAktørId(), startDatoFP, vedtaksDato);
        // Assert
        assertThat(resultat).isFalse();
    }

    @Test
    void vanlig_case_vedtak_rett_før_start() {
        // Arrange
        // Potensielt for liten tid til å avslutte løpende ytelse
        var meldekortT1 = SKJÆRINGSTIDSPUNKT.minusDays(10); // 2019-02-03
        var ytelseVedtakFOM = meldekortT1.minusDays(MELDEKORTPERIODE * 8); // 2019-01-21
        var ytelseVedtakTOM = meldekortT1.plusDays(MELDEKORTPERIODE * 8);// 2019-10-01
        var vedtaksDato = SKJÆRINGSTIDSPUNKT.minusDays(4); // 2019-02-15
        var startDatoFP = SKJÆRINGSTIDSPUNKT; // 2019-02-08
        byggScenario(ytelseVedtakFOM, ytelseVedtakTOM, meldekortT1, vedtaksDato, startDatoFP, Fagsystem.ARENA);
        // Act
        var resultat = vurdereOmArenaYtelseSkalOpphør.vurderArenaYtelserOpphøres(behandling.getId(), behandling.getAktørId(), startDatoFP, vedtaksDato);
        // Assert
        assertThat(resultat).isTrue();
    }

    @Test
    void revurdering_case_midt_under_ytelse() {
        // Arrange
        // Potensielt for liten tid til å avslutte løpende ytelse
        var ytelseVedtakFOM = SKJÆRINGSTIDSPUNKT.plusMonths(2); // 2019-01-21
        var ytelseVedtakTOM = SKJÆRINGSTIDSPUNKT.plusMonths(3);// 2019-10-01
        var meldekortT1 = ytelseVedtakTOM.plusDays(2); // 2019-02-03
        var vedtaksDato = SKJÆRINGSTIDSPUNKT.plusMonths(4); // 2019-02-15
        var startDatoFP = SKJÆRINGSTIDSPUNKT; // 2019-02-08
        byggScenario(ytelseVedtakFOM, ytelseVedtakTOM, meldekortT1, vedtaksDato.atStartOfDay(),
            DatoIntervallEntitet.fraOgMedTilOgMed(startDatoFP, startDatoFP.plusMonths(1)),
            DatoIntervallEntitet.fraOgMedTilOgMed(startDatoFP.plusMonths(4), startDatoFP.plusMonths(6)), Fagsystem.ARENA);
        // Act
        var resultat = vurdereOmArenaYtelseSkalOpphør.vurderArenaYtelserOpphøres(behandling.getId(), behandling.getAktørId(), startDatoFP, vedtaksDato);
        // Assert
        assertThat(resultat).isFalse();
    }

    private void byggScenarioUtenYtelseIArena() {
        byggScenario(now(), now().plusDays(15), now(), now(), now(), Fagsystem.INFOTRYGD);
    }

    private void byggScenario(LocalDate ytelserFom, LocalDate ytelserTom, LocalDate t1, LocalDate vedtaksdato,
                              LocalDate startdatoFP, Fagsystem fagsystem) {
        byggScenario(ytelserFom, ytelserTom, t1, vedtaksdato.atStartOfDay(),
            DatoIntervallEntitet.fraOgMedTilOgMed(startdatoFP, startdatoFP.plusDays(90)), fagsystem);
    }

    private void byggScenario(LocalDate ytelserFom, LocalDate ytelserTom, LocalDate t1, LocalDateTime vedtakstidspunkt,
                              DatoIntervallEntitet fpIntervall, Fagsystem fagsystem) {
        byggScenario(ytelserFom, ytelserTom, t1, vedtakstidspunkt, fpIntervall, null, fagsystem);
    }

    private void byggScenario(LocalDate ytelserFom, LocalDate ytelserTom, LocalDate t1, LocalDateTime vedtakstidspunkt,
                              DatoIntervallEntitet fpIntervall, DatoIntervallEntitet fpIntervall2, Fagsystem fagsystem) {
        behandling = lagre(scenario);

        // Legg til ytelse
        var aggregatBuilder = InntektArbeidYtelseAggregatBuilder.oppdatere(empty(), VersjonType.REGISTER);
        var aktørYtelseBuilder = aggregatBuilder.getAktørYtelseBuilder(AKTØR_ID);
        aktørYtelseBuilder.leggTilYtelse(byggYtelser(ytelserFom, ytelserTom, t1, fagsystem));
        aggregatBuilder.leggTilAktørYtelse(aktørYtelseBuilder);
        iayTjeneste.lagreIayAggregat(behandling.getId(), aggregatBuilder);

        // Legg til beregningresultat
        beregningsresultatFPBuilder = BeregningsresultatEntitet.builder();
        var beregningsresultat = beregningsresultatFPBuilder.medRegelInput("clob1").medRegelSporing("clob2").build();
        var brp = byggBeregningsresultatPeriode(beregningsresultat, fpIntervall.getFomDato(), fpIntervall.getTomDato());
        byggAndel(brp);
        if (fpIntervall2 != null) {
            var brp2 = byggBeregningsresultatPeriode(beregningsresultat, fpIntervall2.getFomDato(), fpIntervall2.getTomDato());
            byggAndel(brp2);
        }
        beregningsresultatRepository.lagre(behandling, beregningsresultat);

        // Legg til behandling resultat
        var behandlingsresultat = Behandlingsresultat.builder()
            .medBehandlingResultatType(BehandlingResultatType.INNVILGET)
            .buildFor(behandling);
        entityManager.persist(behandlingsresultat);
        entityManager.persist(behandling);
        entityManager.flush();
        entityManager.clear();

        // Legg til vedtak
        var behandlingVedtak = BehandlingVedtak.builder()
            .medBehandlingsresultat(behandlingsresultat)
            .medVedtakResultatType(VedtakResultatType.INNVILGET)
            .medVedtakstidspunkt(vedtakstidspunkt)
            .medAnsvarligSaksbehandler("asdf")
            .build();
        behandlingVedtakRepository.lagre(behandlingVedtak, behandlingRepository.taSkriveLås(behandling));
    }

    private YtelseBuilder byggYtelser(LocalDate ytelserFom, LocalDate ytelserTom, LocalDate t1, Fagsystem fagsystem) {
        var ytelseBuilder = YtelseBuilder.oppdatere(Optional.empty())
            .medKilde(fagsystem)
            .medSaksnummer(new Saksnummer(SAK_ID))
            .medPeriode(DatoIntervallEntitet.fraOgMedTilOgMed(ytelserFom, ytelserTom))
            .medStatus(RelatertYtelseTilstand.LØPENDE)
            .medYtelseType(RelatertYtelseType.DAGPENGER);
        byggYtelserAnvist(ytelserFom, t1, ytelseBuilder).forEach(
            ytelseBuilder::medYtelseAnvist);
        return ytelseBuilder;
    }

    private List<YtelseAnvist> byggYtelserAnvist(LocalDate yaFom, LocalDate t1, YtelseBuilder ytelseBuilder) {
        // Man må sende meldekort hver 2 uker.
        final long ytelseDagerMellomrom = 13;
        List<YtelseAnvist> ytelseAnvistList = new ArrayList<>();
        var fom = yaFom;
        var tom = yaFom.plusDays(ytelseDagerMellomrom);
        do {
            var ya = ytelseBuilder.getAnvistBuilder()
                .medAnvistPeriode(DatoIntervallEntitet.fraOgMedTilOgMed(fom, tom))
                .medUtbetalingsgradProsent(BigDecimal.valueOf(100L))
                .medBeløp(BigDecimal.valueOf(30000L))
                .medDagsats(BigDecimal.valueOf(1000L))
                .build();
            ytelseAnvistList.add(ya);
            fom = tom.plusDays(1);
            tom = fom.plusDays(ytelseDagerMellomrom);
        } while (tom.isBefore(t1));

        return ytelseAnvistList;
    }

    private BeregningsresultatPeriode byggBeregningsresultatPeriode(BeregningsresultatEntitet beregningsresultat,
                                                                    LocalDate fom, LocalDate tom) {
        return BeregningsresultatPeriode.builder()
            .medBeregningsresultatPeriodeFomOgTom(fom, tom)
            .build(beregningsresultat);
    }

    private BeregningsresultatAndel byggAndel(BeregningsresultatPeriode bp) {
        return BeregningsresultatAndel.builder().medDagsats(1000)
            .medDagsatsFraBg(1000)
            .medStillingsprosent(BigDecimal.valueOf(100))
            .medUtbetalingsgrad(BigDecimal.valueOf(100))
            .medAktivitetStatus(AktivitetStatus.ARBEIDSAVKLARINGSPENGER)
            .medInntektskategori(Inntektskategori.ARBEIDSAVKLARINGSPENGER)
            .medBrukerErMottaker(true)
            .build(bp);
    }
}
