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

import org.junit.Rule;
import org.junit.Test;

import no.nav.foreldrepenger.behandlingslager.behandling.Behandling;
import no.nav.foreldrepenger.behandlingslager.behandling.BehandlingResultatType;
import no.nav.foreldrepenger.behandlingslager.behandling.Behandlingsresultat;
import no.nav.foreldrepenger.behandlingslager.behandling.beregning.BeregningsresultatEntitet;
import no.nav.foreldrepenger.behandlingslager.behandling.beregning.BeregningsresultatPeriode;
import no.nav.foreldrepenger.behandlingslager.behandling.beregning.BeregningsresultatRepository;
import no.nav.foreldrepenger.behandlingslager.behandling.repository.BehandlingRepository;
import no.nav.foreldrepenger.behandlingslager.behandling.repository.BehandlingRepositoryProvider;
import no.nav.foreldrepenger.behandlingslager.behandling.vedtak.BehandlingVedtak;
import no.nav.foreldrepenger.behandlingslager.behandling.vedtak.BehandlingVedtakRepository;
import no.nav.foreldrepenger.behandlingslager.behandling.vedtak.VedtakResultatType;
import no.nav.foreldrepenger.behandlingslager.kodeverk.Fagsystem;
import no.nav.foreldrepenger.behandlingslager.testutilities.behandling.AbstractTestScenario;
import no.nav.foreldrepenger.behandlingslager.testutilities.behandling.ScenarioMorSøkerForeldrepenger;
import no.nav.foreldrepenger.behandlingslager.ytelse.RelatertYtelseType;
import no.nav.foreldrepenger.behandlingslager.ytelse.TemaUnderkategori;
import no.nav.foreldrepenger.dbstoette.UnittestRepositoryRule;
import no.nav.foreldrepenger.domene.abakus.AbakusInMemoryInntektArbeidYtelseTjeneste;
import no.nav.foreldrepenger.domene.arbeidsforhold.InntektArbeidYtelseTjeneste;
import no.nav.foreldrepenger.domene.iay.modell.InntektArbeidYtelseAggregatBuilder;
import no.nav.foreldrepenger.domene.iay.modell.VersjonType;
import no.nav.foreldrepenger.domene.iay.modell.YtelseAnvist;
import no.nav.foreldrepenger.domene.iay.modell.YtelseBuilder;
import no.nav.foreldrepenger.domene.iay.modell.kodeverk.RelatertYtelseTilstand;
import no.nav.foreldrepenger.domene.typer.AktørId;
import no.nav.foreldrepenger.domene.typer.Saksnummer;
import no.nav.foreldrepenger.domene.tid.DatoIntervallEntitet;
import no.nav.vedtak.felles.testutilities.db.Repository;
import no.nav.vedtak.felles.testutilities.db.RepositoryRule;

public class VurderOmArenaYtelseSkalOpphøreTest {

    @Rule
    public final RepositoryRule repoRule = new UnittestRepositoryRule();
    private BehandlingRepositoryProvider repositoryProvider = new BehandlingRepositoryProvider(repoRule.getEntityManager());
    private final BehandlingRepository behandlingRepository = repositoryProvider.getBehandlingRepository();
    private final BehandlingVedtakRepository behandlingVedtakRepository = repositoryProvider.getBehandlingVedtakRepository();
    private final Repository repository = repoRule.getRepository();
    private final BeregningsresultatRepository beregningsresultatRepository = new BeregningsresultatRepository(repoRule.getEntityManager());

    private static final AktørId AKTØR_ID = AktørId.dummy();
    private static final String SAK_ID = "1200095";
    private static final LocalDate SKJÆRINGSTIDSPUNKT = LocalDate.now();
    private static Long MELDEKORTPERIODE = 14L;

    private final ScenarioMorSøkerForeldrepenger scenario = ScenarioMorSøkerForeldrepenger.forFødselMedGittAktørId(AKTØR_ID);
    private InntektArbeidYtelseTjeneste iayTjeneste = new AbakusInMemoryInntektArbeidYtelseTjeneste();
    private final VurderOmArenaYtelseSkalOpphøre vurdereOmArenaYtelseSkalOpphør = new VurderOmArenaYtelseSkalOpphøre(
        beregningsresultatRepository,
        iayTjeneste, behandlingVedtakRepository, null);

    private Behandling behandling;

    private BeregningsresultatEntitet.Builder beregningsresultatFPBuilder;
    private BeregningsresultatPeriode.Builder brPeriodebuilder;

    private Behandling lagre(AbstractTestScenario<?> scenario) {
        return scenario.lagre(repositoryProvider);
    }

    @Test
    public void skal_teste_arena_ytelser_finnes_ikke() {
        // Arrange
        LocalDate vedtaksDato = SKJÆRINGSTIDSPUNKT.minusDays(7);
        LocalDate startDatoFP = SKJÆRINGSTIDSPUNKT;
        byggScenarioUtenYtelseIArena();
        // Act
        boolean resultat = vurdereOmArenaYtelseSkalOpphør.vurderArenaYtelserOpphøres(behandling.getId(), behandling.getAktørId(), startDatoFP, vedtaksDato);
        // Assert
        assertThat(resultat).isFalse();
    }

    // T1: Siste utbetalingsdato for ARENA-ytelse før vedtaksdato for foreldrepenger
    // T2: Første forventede utbetalingsdato for ARENA-ytelse etter vedtaksdato for foreldrepenger

    @Test
    public void skal_teste_startdatoFP_før_T1() {
        // Arrange
        // Startdato før T1 , vedtaksdato etter T1
        LocalDate meldekortT1 = SKJÆRINGSTIDSPUNKT.plusDays(2);
        LocalDate ytelseVedtakFOM = meldekortT1.minusDays(MELDEKORTPERIODE);
        LocalDate ytelseVedtakTOM = SKJÆRINGSTIDSPUNKT.plusDays(MELDEKORTPERIODE * 2);
        LocalDate vedtaksDato = SKJÆRINGSTIDSPUNKT.plusDays(36);
        LocalDate startDatoFP = SKJÆRINGSTIDSPUNKT;
        byggScenario(ytelseVedtakFOM, ytelseVedtakTOM, meldekortT1, vedtaksDato, startDatoFP, Fagsystem.ARENA);
        // Act
        boolean resultat = vurdereOmArenaYtelseSkalOpphør.vurderArenaYtelserOpphøres(behandling.getId(), behandling.getAktørId(), startDatoFP, vedtaksDato);
        // Assert
        assertThat(resultat).isTrue();
    }

    @Test
    public void skal_teste_startdatoFP_før_T1_FPFEIL_3526() {
        // Arrange
        // Startdato før T1 , vedtaksdato etter T1
        LocalDate meldekortT1 = LocalDate.of(2018, 8, 12);
        LocalDate ytelseVedtakFOM = LocalDate.of(2018, 7, 29);
        LocalDate ytelseVedtakTOM = LocalDate.of(2018, 8, 26);
        LocalDate vedtaksDato = LocalDate.of(2018, 11, 14);
        LocalDate startDatoFP = LocalDate.of(2018, 4, 6);
        byggScenario(ytelseVedtakFOM, ytelseVedtakTOM, meldekortT1, vedtaksDato, startDatoFP, Fagsystem.ARENA);
        // Act
        boolean resultat = vurdereOmArenaYtelseSkalOpphør.vurderArenaYtelserOpphøres(behandling.getId(), behandling.getAktørId(), startDatoFP, vedtaksDato);
        // Assert
        assertThat(resultat).isTrue();
    }

    @Test
    public void skal_teste_startdatoFP_etter_T2() {
        // Arrange
        // Startdato før T2, vedtaksdato etter T2
        LocalDate meldekortT1 = SKJÆRINGSTIDSPUNKT.minusDays(64);
        LocalDate ytelseVedtakFOM = meldekortT1.minusDays(MELDEKORTPERIODE);
        LocalDate ytelseVedtakTOM = SKJÆRINGSTIDSPUNKT.plusDays(19);
        LocalDate vedtaksDato = SKJÆRINGSTIDSPUNKT.minusDays(49);
        LocalDate startDatoFP = SKJÆRINGSTIDSPUNKT;
        byggScenario(ytelseVedtakFOM, ytelseVedtakTOM, meldekortT1, vedtaksDato, startDatoFP, Fagsystem.ARENA);
        // Act
        boolean resultat = vurdereOmArenaYtelseSkalOpphør.vurderArenaYtelserOpphøres(behandling.getId(), behandling.getAktørId(), startDatoFP, vedtaksDato);
        // Assert
        assertThat(resultat).isFalse();
    }

    @Test
    public void skal_teste_startdatoFP_mellom_T1_T2_vedtaksdato_mindre_enn_8_dager_etter_T1() {
        // Arrange
        // startdato mellom T1 og T2, vedtaksdato mellom T1 og (T1 + 8 dager)
        LocalDate meldekortT1 = SKJÆRINGSTIDSPUNKT.minusDays(5);
        LocalDate ytelseVedtakFOM = meldekortT1.minusDays(MELDEKORTPERIODE);
        LocalDate ytelseVedtakTOM = SKJÆRINGSTIDSPUNKT.plusDays(MELDEKORTPERIODE * 2);
        LocalDate vedtaksDato = SKJÆRINGSTIDSPUNKT.minusDays(4);
        LocalDate startDatoFP = SKJÆRINGSTIDSPUNKT;
        byggScenario(ytelseVedtakFOM, ytelseVedtakTOM, meldekortT1, vedtaksDato, startDatoFP, Fagsystem.ARENA);
        // Act
        boolean resultat = vurdereOmArenaYtelseSkalOpphør.vurderArenaYtelserOpphøres(behandling.getId(), behandling.getAktørId(), startDatoFP, vedtaksDato);
        // Assert
        assertThat(resultat).isFalse();
    }

    @Test
    public void skal_teste_startdatoFP_mellom_T1_T2_vedtaksdato_mindre_enn_8_dager_før_T2() {
        // Arrange
        // startdato mellom T1 og T2, vedtaksdato mellom (T2 - 8 dager) og T2
        LocalDate meldekortT1 = SKJÆRINGSTIDSPUNKT.minusDays(8);
        LocalDate ytelseVedtakFOM = meldekortT1.minusDays(MELDEKORTPERIODE);
        LocalDate ytelseVedtakTOM = SKJÆRINGSTIDSPUNKT.plusDays(MELDEKORTPERIODE * 2);
        LocalDate vedtaksDato = SKJÆRINGSTIDSPUNKT.plusDays(4);
        LocalDate startDatoFP = SKJÆRINGSTIDSPUNKT;
        byggScenario(ytelseVedtakFOM, ytelseVedtakTOM, meldekortT1, vedtaksDato, startDatoFP, Fagsystem.ARENA);
        // Act
        boolean resultat = vurdereOmArenaYtelseSkalOpphør.vurderArenaYtelserOpphøres(behandling.getId(), behandling.getAktørId(), startDatoFP, vedtaksDato);
        // Assert
        assertThat(resultat).isTrue();
    }

    @Test
    public void skal_teste_Arena_ytelse_interval_før_vedtaksdato_fom_overlapper_FP() {
        // Arrange
        // Arena ytelser etter startdato men før vedtaksdato .
        LocalDate meldekortT1 = SKJÆRINGSTIDSPUNKT.plusDays(MELDEKORTPERIODE + 7);
        LocalDate ytelseVedtakFOM = meldekortT1.minusDays(MELDEKORTPERIODE);
        LocalDate ytelseVedtakTOM = meldekortT1.plusDays(MELDEKORTPERIODE);
        LocalDate vedtaksDato = SKJÆRINGSTIDSPUNKT.plusDays(47);
        LocalDate startDatoFP = SKJÆRINGSTIDSPUNKT;
        byggScenario(ytelseVedtakFOM, ytelseVedtakTOM, meldekortT1, vedtaksDato, startDatoFP, Fagsystem.ARENA);
        // Act
        boolean resultat = vurdereOmArenaYtelseSkalOpphør.vurderArenaYtelserOpphøres(behandling.getId(), behandling.getAktørId(), startDatoFP, vedtaksDato);
        // Assert
        assertThat(resultat).isTrue();
    }

    @Test
    public void skal_teste_startdato_er_like_T1_og_T2_er_null() {
        // Arrange
        // Arena ytelser før vedtaksdato og mellom startdato FP og sluttdato FP.
        LocalDate meldekortT1 = SKJÆRINGSTIDSPUNKT;
        LocalDate ytelseVedtakFOM = SKJÆRINGSTIDSPUNKT.minusDays(MELDEKORTPERIODE * 2);
        LocalDate ytelseVedtakTOM = SKJÆRINGSTIDSPUNKT.minusDays(1);
        LocalDate vedtaksDato = SKJÆRINGSTIDSPUNKT.plusDays(47);
        LocalDate startDatoFP = SKJÆRINGSTIDSPUNKT.minusDays(1);
        byggScenario(ytelseVedtakFOM, ytelseVedtakTOM, meldekortT1, vedtaksDato, startDatoFP, Fagsystem.ARENA);
        // Act
        boolean resultat = vurdereOmArenaYtelseSkalOpphør.vurderArenaYtelserOpphøres(behandling.getId(), behandling.getAktørId(), startDatoFP, vedtaksDato);
        // Assert
        assertThat(resultat).isFalse();
    }

    @Test
    public void skal_teste_vedtaksdato_er_like_T2() {
        // Arrange
        // Arena ytelser før vedtaksdato og mellom startdato FP og sluttdato FP.
        LocalDate meldekortT1 = SKJÆRINGSTIDSPUNKT.minusDays(MELDEKORTPERIODE);
        LocalDate ytelseVedtakFOM = meldekortT1.minusDays(MELDEKORTPERIODE);
        LocalDate ytelseVedtakTOM = SKJÆRINGSTIDSPUNKT.plusDays(MELDEKORTPERIODE * 2);
        LocalDate vedtaksDato = SKJÆRINGSTIDSPUNKT;
        LocalDate startDatoFP = SKJÆRINGSTIDSPUNKT.plusDays(10);
        byggScenario(ytelseVedtakFOM, ytelseVedtakTOM, meldekortT1, vedtaksDato, startDatoFP, Fagsystem.ARENA);
        // Act
        boolean resultat = vurdereOmArenaYtelseSkalOpphør.vurderArenaYtelserOpphøres(behandling.getId(), behandling.getAktørId(), startDatoFP, vedtaksDato);
        // Assert
        assertThat(resultat).isFalse();
    }

    @Test
    public void skal_teste_startdato_før_T1_og_FP_overlapper_ikke_ARENA_ytelse() {
        // Arrange
        // Arena ytelser før vedtaksdato og utenfor startdato FP og sluttdato FP.
        LocalDate meldekortT1 = SKJÆRINGSTIDSPUNKT.plusDays(7);
        LocalDate ytelseVedtakFOM = meldekortT1.minusDays(MELDEKORTPERIODE);
        LocalDate ytelseVedtakTOM = SKJÆRINGSTIDSPUNKT.plusDays(MELDEKORTPERIODE * 2);
        LocalDate vedtaksDato = SKJÆRINGSTIDSPUNKT.plusDays(54);
        LocalDate startDatoFP = SKJÆRINGSTIDSPUNKT;
        byggScenario(ytelseVedtakFOM, ytelseVedtakTOM, meldekortT1, vedtaksDato, startDatoFP, Fagsystem.ARENA);
        // Act
        boolean resultat = vurdereOmArenaYtelseSkalOpphør.vurderArenaYtelserOpphøres(behandling.getId(), behandling.getAktørId(), startDatoFP, vedtaksDato);
        // Assert
        assertThat(resultat).isTrue();
    }

    @Test
    public void skal_teste_startdato_for_5910() {
        // Arrange
        // Arena ytelser før vedtaksdato og utenfor startdato FP og sluttdato FP.
        LocalDate meldekortT1 = SKJÆRINGSTIDSPUNKT.minusDays(5); // 2019-02-03
        LocalDate ytelseVedtakFOM = meldekortT1.minusDays(MELDEKORTPERIODE); // 2019-01-21
        LocalDate ytelseVedtakTOM = SKJÆRINGSTIDSPUNKT.plusMonths(6);// 2019-10-01
        LocalDate vedtaksDato = SKJÆRINGSTIDSPUNKT.plusDays(7); // 2019-02-15
        LocalDate startDatoFP = SKJÆRINGSTIDSPUNKT; // 2019-02-08
        byggScenario(ytelseVedtakFOM, ytelseVedtakTOM, meldekortT1, vedtaksDato, startDatoFP, Fagsystem.ARENA);
        // Act
        boolean resultat = vurdereOmArenaYtelseSkalOpphør.vurderArenaYtelserOpphøres(behandling.getId(), behandling.getAktørId(), startDatoFP, vedtaksDato);
        // Assert
        assertThat(resultat).isTrue();
    }

    @Test
    public void ytelse_avsluttet_før_stp_siste_meldekort_rett_etter() {
        // Arrange
        // Siste meldekort vil som regel komme rett etter perioden og med prosent <200
        LocalDate meldekortT1 = SKJÆRINGSTIDSPUNKT.plusDays(2); // 2019-02-03
        LocalDate ytelseVedtakFOM = SKJÆRINGSTIDSPUNKT.minusDays(MELDEKORTPERIODE * 2 - 1); // 2019-01-21
        LocalDate ytelseVedtakTOM = SKJÆRINGSTIDSPUNKT.minusDays(1);// 2019-10-01
        LocalDate vedtaksDato = SKJÆRINGSTIDSPUNKT.plusDays(7); // 2019-02-15
        LocalDate startDatoFP = SKJÆRINGSTIDSPUNKT; // 2019-02-08
        byggScenario(ytelseVedtakFOM, ytelseVedtakTOM, meldekortT1, vedtaksDato, startDatoFP, Fagsystem.ARENA);
        // Act
        boolean resultat = vurdereOmArenaYtelseSkalOpphør.vurderArenaYtelserOpphøres(behandling.getId(), behandling.getAktørId(), startDatoFP, vedtaksDato);
        // Assert
        assertThat(resultat).isFalse();
    }

    @Test
    public void vanlig_case_vedtak_før_start() {
        // Arrange
        // Gir arena nok tid til å avslutte løpende ytelse
        LocalDate meldekortT1 = SKJÆRINGSTIDSPUNKT.minusDays(16); // 2019-02-03
        LocalDate ytelseVedtakFOM = meldekortT1.minusDays(MELDEKORTPERIODE * 8); // 2019-01-21
        LocalDate ytelseVedtakTOM = meldekortT1.plusDays(MELDEKORTPERIODE * 8);// 2019-10-01
        LocalDate vedtaksDato = SKJÆRINGSTIDSPUNKT.minusDays(14); // 2019-02-15
        LocalDate startDatoFP = SKJÆRINGSTIDSPUNKT; // 2019-02-08
        byggScenario(ytelseVedtakFOM, ytelseVedtakTOM, meldekortT1, vedtaksDato, startDatoFP, Fagsystem.ARENA);
        // Act
        boolean resultat = vurdereOmArenaYtelseSkalOpphør.vurderArenaYtelserOpphøres(behandling.getId(), behandling.getAktørId(), startDatoFP, vedtaksDato);
        // Assert
        assertThat(resultat).isFalse();
    }

    @Test
    public void vanlig_case_vedtak_rett_før_start() {
        // Arrange
        // Potensielt for liten tid til å avslutte løpende ytelse
        LocalDate meldekortT1 = SKJÆRINGSTIDSPUNKT.minusDays(10); // 2019-02-03
        LocalDate ytelseVedtakFOM = meldekortT1.minusDays(MELDEKORTPERIODE * 8); // 2019-01-21
        LocalDate ytelseVedtakTOM = meldekortT1.plusDays(MELDEKORTPERIODE * 8);// 2019-10-01
        LocalDate vedtaksDato = SKJÆRINGSTIDSPUNKT.minusDays(4); // 2019-02-15
        LocalDate startDatoFP = SKJÆRINGSTIDSPUNKT; // 2019-02-08
        byggScenario(ytelseVedtakFOM, ytelseVedtakTOM, meldekortT1, vedtaksDato, startDatoFP, Fagsystem.ARENA);
        // Act
        boolean resultat = vurdereOmArenaYtelseSkalOpphør.vurderArenaYtelserOpphøres(behandling.getId(), behandling.getAktørId(), startDatoFP, vedtaksDato);
        // Assert
        assertThat(resultat).isTrue();
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
        behandling = lagre(scenario);

        // Legg til ytelse
        InntektArbeidYtelseAggregatBuilder aggregatBuilder = InntektArbeidYtelseAggregatBuilder.oppdatere(empty(), VersjonType.REGISTER);
        InntektArbeidYtelseAggregatBuilder.AktørYtelseBuilder aktørYtelseBuilder = aggregatBuilder.getAktørYtelseBuilder(AKTØR_ID);
        aktørYtelseBuilder.leggTilYtelse(byggYtelser(ytelserFom, ytelserTom, t1, fagsystem));
        aggregatBuilder.leggTilAktørYtelse(aktørYtelseBuilder);
        iayTjeneste.lagreIayAggregat(behandling.getId(), aggregatBuilder);

        // Legg til beregningresultat
        beregningsresultatFPBuilder = BeregningsresultatEntitet.builder();
        brPeriodebuilder = BeregningsresultatPeriode.builder();
        BeregningsresultatEntitet beregningsresultat = byggBeregningsresultatFP(fpIntervall.getFomDato(), fpIntervall.getTomDato());
        beregningsresultatRepository.lagre(behandling, beregningsresultat);

        // Legg til behandling resultat
        Behandlingsresultat behandlingsresultat = Behandlingsresultat.builder()
            .medBehandlingResultatType(BehandlingResultatType.INNVILGET)
            .buildFor(behandling);
        repository.lagre(behandlingsresultat);
        repository.lagre(behandling);
        repository.flushAndClear();

        // Legg til vedtak
        final BehandlingVedtak behandlingVedtak = BehandlingVedtak.builder()
            .medBehandlingsresultat(behandlingsresultat)
            .medVedtakResultatType(VedtakResultatType.INNVILGET)
            .medVedtakstidspunkt(vedtakstidspunkt)
            .medAnsvarligSaksbehandler("asdf").build();
        behandlingVedtakRepository.lagre(behandlingVedtak, behandlingRepository.taSkriveLås(behandling));
    }

    private YtelseBuilder byggYtelser(LocalDate ytelserFom, LocalDate ytelserTom, LocalDate t1, Fagsystem fagsystem) {
        YtelseBuilder ytelseBuilder = YtelseBuilder.oppdatere(Optional.empty())
            .medKilde(fagsystem)
            .medSaksnummer(new Saksnummer(SAK_ID))
            .medPeriode(DatoIntervallEntitet.fraOgMedTilOgMed(ytelserFom, ytelserTom))
            .medStatus(RelatertYtelseTilstand.LØPENDE)
            .medYtelseType(RelatertYtelseType.DAGPENGER)
            .medBehandlingsTema(TemaUnderkategori.UDEFINERT);
        byggYtelserAnvist(ytelserFom, ytelserTom, t1, ytelseBuilder).forEach(
            ytelseAnvist -> {
                ytelseBuilder.medYtelseAnvist(ytelseAnvist);
            });
        return ytelseBuilder;
    }

    private List<YtelseAnvist> byggYtelserAnvist(LocalDate yaFom, @SuppressWarnings("unused") LocalDate yaTom, LocalDate t1, YtelseBuilder ytelseBuilder) {
        // Man må sende meldekort hver 2 uker.
        final long ytelseDagerMellomrom = 13;
        List<YtelseAnvist> ytelseAnvistList = new ArrayList<>();
        LocalDate fom = yaFom;
        LocalDate tom = yaFom.plusDays(ytelseDagerMellomrom);
        do {
            YtelseAnvist ya = ytelseBuilder.getAnvistBuilder()
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

    private BeregningsresultatEntitet byggBeregningsresultatFP(LocalDate fom, LocalDate tom) {
        BeregningsresultatEntitet beregningsresultat = beregningsresultatFPBuilder
            .medRegelInput("clob1")
            .medRegelSporing("clob2")
            .build();
        byggBeregningsresultatPeriode(beregningsresultat, fom, tom);
        return beregningsresultat;
    }

    private BeregningsresultatPeriode byggBeregningsresultatPeriode(BeregningsresultatEntitet beregningsresultat,
                                                                    LocalDate fom, LocalDate tom) {
        return brPeriodebuilder
            .medBeregningsresultatPeriodeFomOgTom(fom, tom)
            .build(beregningsresultat);
    }
}
