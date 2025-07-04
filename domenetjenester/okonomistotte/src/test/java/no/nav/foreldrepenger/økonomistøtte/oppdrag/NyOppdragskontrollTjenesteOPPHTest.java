package no.nav.foreldrepenger.økonomistøtte.oppdrag;

import static org.assertj.core.api.Assertions.assertThat;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Comparator;
import java.util.List;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import no.nav.foreldrepenger.behandlingslager.behandling.beregning.AktivitetStatus;
import no.nav.foreldrepenger.behandlingslager.behandling.beregning.BeregningsresultatEntitet;
import no.nav.foreldrepenger.behandlingslager.behandling.beregning.BeregningsresultatPeriode;
import no.nav.foreldrepenger.behandlingslager.behandling.beregning.Inntektskategori;
import no.nav.foreldrepenger.behandlingslager.økonomioppdrag.Avstemming;
import no.nav.foreldrepenger.behandlingslager.økonomioppdrag.FamilieYtelseType;
import no.nav.foreldrepenger.behandlingslager.økonomioppdrag.Oppdrag110;
import no.nav.foreldrepenger.behandlingslager.økonomioppdrag.Oppdragskontroll;
import no.nav.foreldrepenger.behandlingslager.økonomioppdrag.Oppdragslinje150;
import no.nav.foreldrepenger.behandlingslager.økonomioppdrag.Refusjonsinfo156;
import no.nav.foreldrepenger.behandlingslager.økonomioppdrag.Sats;
import no.nav.foreldrepenger.behandlingslager.økonomioppdrag.Utbetalingsgrad;
import no.nav.foreldrepenger.behandlingslager.økonomioppdrag.koder.KodeEndring;
import no.nav.foreldrepenger.behandlingslager.økonomioppdrag.koder.KodeEndringLinje;
import no.nav.foreldrepenger.behandlingslager.økonomioppdrag.koder.KodeFagområde;
import no.nav.foreldrepenger.behandlingslager.økonomioppdrag.koder.KodeKlassifik;
import no.nav.foreldrepenger.behandlingslager.økonomioppdrag.koder.KodeStatusLinje;
import no.nav.foreldrepenger.behandlingslager.økonomioppdrag.koder.TypeSats;
import no.nav.foreldrepenger.økonomistøtte.OppdragKvitteringTestUtil;
import no.nav.foreldrepenger.økonomistøtte.OppdragMedPositivKvitteringTestUtil;
import no.nav.foreldrepenger.økonomistøtte.oppdrag.domene.samlinger.GruppertYtelse;
import no.nav.foreldrepenger.økonomistøtte.oppdrag.mapper.TilkjentYtelseMapper;

public class NyOppdragskontrollTjenesteOPPHTest extends NyOppdragskontrollTjenesteTestBase {

    public static final String ANSVARLIG_SAKSBEHANDLER = "Katarzyna";

    @BeforeEach
    public void setUp() {
        super.setUp();
    }

    @Test
    void skalSendeOppdragForOpphør() {
        // Arrange
        var beregningsresultat = buildBeregningsresultatFP();

        var mapper = new TilkjentYtelseMapper(FamilieYtelseType.FØDSEL);
        var gruppertYtelse = mapper.fordelPåNøkler(beregningsresultat);
        var builder = getInputStandardBuilder(gruppertYtelse);

        var originaltOppdrag = OppdragMedPositivKvitteringTestUtil.opprett(nyOppdragskontrollTjeneste, builder.build());

        // Act

        var builder2 = getInputStandardBuilder(GruppertYtelse.TOM).medTidligereOppdrag(mapTidligereOppdrag(List.of(originaltOppdrag)));

        // Act
        var oppdragRevurdering = OppdragMedPositivKvitteringTestUtil.opprett(nyOppdragskontrollTjeneste, builder2.build());

        //Assert
        OppdragskontrollTestVerktøy.verifiserAvstemming(oppdragRevurdering);
        var oppdrag110RevurderingList = verifiserOppdrag110_OPPH(oppdragRevurdering, originaltOppdrag);
        var oppdragslinje150Liste = verifiserOppdragslinje150_OPPH(oppdragRevurdering, originaltOppdrag);
        OppdragskontrollTestVerktøy.verifiserGrad(oppdragslinje150Liste, originaltOppdrag);
        OppdragskontrollTestVerktøy.verifiserRefusjonInfo156(oppdrag110RevurderingList, originaltOppdrag);
    }

    @Test
    void skalSendeOppdragForOpphørNårFørstegangsbehandlingHarFlereInntektskategori() {
        var beregningsresultat = buildBeregningsresultatMedFlereInntektskategoriFP(false);
        var mapper = new TilkjentYtelseMapper(FamilieYtelseType.FØDSEL);
        var gruppertYtelse = mapper.fordelPåNøkler(beregningsresultat);
        var builder = getInputStandardBuilder(gruppertYtelse);

        var originaltOppdrag = OppdragMedPositivKvitteringTestUtil.opprett(nyOppdragskontrollTjeneste, builder.build());

        // Act

        var builder2 = getInputStandardBuilder(GruppertYtelse.TOM).medTidligereOppdrag(mapTidligereOppdrag(List.of(originaltOppdrag)));

        // Act
        var oppdragRevurdering = OppdragMedPositivKvitteringTestUtil.opprett(nyOppdragskontrollTjeneste, builder2.build());

        //Assert
        OppdragskontrollTestVerktøy.verifiserAvstemming(oppdragRevurdering);
        verifiserOppdrag110_OPPH(oppdragRevurdering, originaltOppdrag);
        verifiserOppdragslinje150MedFlereKategorier_OPPH(oppdragRevurdering, originaltOppdrag);
    }

    @Test
    void skalSendeOpphørFørstSomEnDelAvEndringsoppdragForBruker() {
        // Arrange
        var fom = LocalDate.of(I_ÅR, 8, 1);
        var tom = LocalDate.of(I_ÅR, 8, 20);
        var beregningsresultat = buildBeregningsresultatBrukerFP(1500, 500, fom, tom);
        var mapper = new TilkjentYtelseMapper(FamilieYtelseType.FØDSEL);
        var gruppertYtelse = mapper.fordelPåNøkler(beregningsresultat);
        var builder = getInputStandardBuilder(gruppertYtelse);

        var originaltOppdrag = OppdragMedPositivKvitteringTestUtil.opprett(nyOppdragskontrollTjeneste, builder.build());

        var originaltOppdrag110Liste = originaltOppdrag.getOppdrag110Liste();

        var endringsdato = beregningsresultat.getGjeldendePerioder().get(0).getBeregningsresultatPeriodeFom();
        var beregningsresultatRevurderingFP = buildBeregningsresultatRevurderingEntenForBrukerEllerArbgvr(false, true);

        var gruppertYtelse2 = mapper.fordelPåNøkler(beregningsresultatRevurderingFP);
        var builder2 = getInputStandardBuilder(gruppertYtelse2).medTidligereOppdrag(mapTidligereOppdrag(List.of(originaltOppdrag)));

        // Act
        var oppdragRevurdering = OppdragMedPositivKvitteringTestUtil.opprett(nyOppdragskontrollTjeneste, builder2.build());

        //Assert
        OppdragskontrollTestVerktøy.verifiserAvstemming(oppdragRevurdering);
        verifiserOPPHForBrukerIENDR(oppdragRevurdering, originaltOppdrag110Liste, endringsdato);
    }

    @Test
    void opphørSkalIkkeSendesHvisEndringstidspunktErEtterAlleTidligereOppdragForBrukerMedFlereKlassekode() {
        // Arrange
        var beregningsresultat = buildBeregningsresultatMedFlereInntektskategoriFP(true);

        var mapper = new TilkjentYtelseMapper(FamilieYtelseType.FØDSEL);
        var gruppertYtelse = mapper.fordelPåNøkler(beregningsresultat);
        var builder = getInputStandardBuilder(gruppertYtelse);

        var originaltOppdrag = OppdragMedPositivKvitteringTestUtil.opprett(nyOppdragskontrollTjeneste, builder.build());

        var sistePeriodeTom = beregningsresultat.getGjeldendePerioder().stream()
            .map(BeregningsresultatPeriode::getBeregningsresultatPeriodeTom).max(Comparator.comparing(Function.identity())).get();
        var endringsdato = sistePeriodeTom.plusMonths(18);

        var beregningsresultatRevurderingFP = buildEmptyBeregningsresultatFP();
        var brPeriode_1 = buildBeregningsresultatPeriode(beregningsresultatRevurderingFP, endringsdato.plusDays(1), endringsdato.plusDays(10));
        buildBeregningsresultatAndel(brPeriode_1, true, 500, BigDecimal.valueOf(100), virksomhet);

        var gruppertYtelse2 = mapper.fordelPåNøkler(beregningsresultatRevurderingFP);
        var builder2 = getInputStandardBuilder(gruppertYtelse2).medTidligereOppdrag(mapTidligereOppdrag(List.of(originaltOppdrag)));

        // Act
        var oppdragRevurdering = OppdragMedPositivKvitteringTestUtil.opprett(nyOppdragskontrollTjeneste, builder2.build());

        //Assert
        var oppdragslinje150OpphørtListe = oppdragRevurdering.getOppdrag110Liste().stream().flatMap(oppdrag110 -> oppdrag110.getOppdragslinje150Liste()
            .stream()).filter(Oppdragslinje150::gjelderOpphør).toList();
        assertThat(oppdragslinje150OpphørtListe).isNotEmpty();
    }

    @Test
    @DisplayName("Opphør skal ikke sendes hvis endringstidspunkt er etter alle tidligere oppdrag.")
    void opphørSkalIkkeSendesHvisEndringstidspunktErEtterAlleTidligereOppdrag() {
        // Arrange
        var fom = LocalDate.of(I_ÅR, 8, 1);
        var tom = LocalDate.of(I_ÅR, 8, 7);
        var beregningsresultat = buildBeregningsresultatBrukerFP(1500, 500, fom, tom);

        var mapper = new TilkjentYtelseMapper(FamilieYtelseType.FØDSEL);
        var gruppertYtelse = mapper.fordelPåNøkler(beregningsresultat);
        var builder = getInputStandardBuilder(gruppertYtelse);

        var originaltOppdrag = OppdragMedPositivKvitteringTestUtil.opprett(nyOppdragskontrollTjeneste, builder.build());

        var sistePeriodeTom = beregningsresultat.getGjeldendePerioder().stream()
            .map(BeregningsresultatPeriode::getBeregningsresultatPeriodeTom).max(Comparator.comparing(Function.identity())).get();
        var endringsdato = sistePeriodeTom.plusMonths(18);

        var beregningsresultatRevurderingFP = buildEmptyBeregningsresultatFP();
        var brPeriode_1 = buildBeregningsresultatPeriode(beregningsresultatRevurderingFP, endringsdato.plusDays(1), endringsdato.plusDays(10));
        buildBeregningsresultatAndel(brPeriode_1, true, 500, BigDecimal.valueOf(100), virksomhet);

        var gruppertYtelse2 = mapper.fordelPåNøkler(beregningsresultatRevurderingFP);
        var builder2 = getInputStandardBuilder(gruppertYtelse2).medTidligereOppdrag(mapTidligereOppdrag(List.of(originaltOppdrag)));

        // Act
        var oppdragRevurdering = OppdragMedPositivKvitteringTestUtil.opprett(nyOppdragskontrollTjeneste, builder2.build());

        //Assert
        var oppdragslinje150OpphørtListe = oppdragRevurdering.getOppdrag110Liste().stream().flatMap(oppdrag110 -> oppdrag110.getOppdragslinje150Liste()
            .stream()).filter(Oppdragslinje150::gjelderOpphør).toList();
        assertThat(oppdragslinje150OpphørtListe).isNotEmpty();
    }

    @Test
    void opphørSkalIkkeSendesForYtelseHvisEndringsdatoErEtterSisteTomDatoITidligereOppdragForArbeidsgiver() {
        // Arrange
        var beregningsresultat = buildBeregningsresultatEntenForBrukerEllerArbgvr(false, true);

        var mapper = new TilkjentYtelseMapper(FamilieYtelseType.FØDSEL);
        var gruppertYtelse = mapper.fordelPåNøkler(beregningsresultat);
        var builder = getInputStandardBuilder(gruppertYtelse);

        var originaltOppdrag = OppdragMedPositivKvitteringTestUtil.opprett(nyOppdragskontrollTjeneste, builder.build());

        var sistePeriodeTom = beregningsresultat.getGjeldendePerioder().stream()
            .map(BeregningsresultatPeriode::getBeregningsresultatPeriodeTom).max(Comparator.comparing(Function.identity())).get();
        var endringsdato = sistePeriodeTom.plusDays(1);

        var beregningsresultatRevurderingFP = buildEmptyBeregningsresultatFP();
        var brPeriode_1 = buildBeregningsresultatPeriode(beregningsresultatRevurderingFP, endringsdato.plusDays(1), endringsdato.plusDays(10));
        buildBeregningsresultatAndel(brPeriode_1, false, 500, BigDecimal.valueOf(100), virksomhet);

        var gruppertYtelse2 = mapper.fordelPåNøkler(beregningsresultatRevurderingFP);
        var builder2 = getInputStandardBuilder(gruppertYtelse2).medTidligereOppdrag(mapTidligereOppdrag(List.of(originaltOppdrag)));

        // Act
        var oppdragRevurdering = OppdragMedPositivKvitteringTestUtil.opprett(nyOppdragskontrollTjeneste, builder2.build());

        //Assert
        assertThat(oppdragRevurdering).isNotNull();
        OppdragskontrollTestVerktøy.verifiserAvstemming(oppdragRevurdering);
        //verifiserOppdrag110OgOppdragslinje150(oppdragRevurdering, originaltOppdrag110Liste, false); //TODO må redigeres
    }

    @Test
    void opphørSkalIkkeSendesHvisEndringstidspunktErEtterSisteDatoITidligereOppdrForBrukerMedFlereKlassekodeIForrigeBeh() {
        // Arrange
        var beregningsresultat = buildBeregningsresultatMedFlereInntektskategoriFP(false);

        var mapper = new TilkjentYtelseMapper(FamilieYtelseType.FØDSEL);
        var gruppertYtelse = mapper.fordelPåNøkler(beregningsresultat);
        var builder = getInputStandardBuilder(gruppertYtelse);

        var originaltOppdrag = OppdragMedPositivKvitteringTestUtil.opprett(nyOppdragskontrollTjeneste, builder.build());

        var beregningsresultatRevurderingFP = buildBeregningsresultatRevurderingFP(AktivitetStatus.ARBEIDSTAKER, Inntektskategori.ARBEIDSTAKER, virksomhet,
            virksomhet, true);

        var gruppertYtelse2 = mapper.fordelPåNøkler(beregningsresultatRevurderingFP);
        var builder2 = getInputStandardBuilder(gruppertYtelse2).medTidligereOppdrag(mapTidligereOppdrag(List.of(originaltOppdrag)));

        // Act
        var oppdragRevurdering = OppdragMedPositivKvitteringTestUtil.opprett(nyOppdragskontrollTjeneste, builder2.build());

        //Assert
        assertThat(oppdragRevurdering).isNotNull();
        OppdragskontrollTestVerktøy.verifiserAvstemming(oppdragRevurdering);
        // TODO rediger verifiserOppdrag110OgOppdragslinje150(oppdragRevurdering, originaltOppdrag110Liste, true);
    }

    @Test
    void opphørsDatoenMåSettesLikFørsteDatoVedtakFomNårDenneErSenereEnnEndringstdpktBruker() {
        // Arrange
        var beregningsresultat = buildBeregningsresultatEntenForBrukerEllerArbgvr(true, true);
        var mapper = new TilkjentYtelseMapper(FamilieYtelseType.FØDSEL);
        var gruppertYtelse = mapper.fordelPåNøkler(beregningsresultat);
        var builder = getInputStandardBuilder(gruppertYtelse);

        var originaltOppdrag = OppdragMedPositivKvitteringTestUtil.opprett(nyOppdragskontrollTjeneste, builder.build());

        var førsteDatoVedtakFom = beregningsresultat.getGjeldendePerioder().stream().min(Comparator.comparing(BeregningsresultatPeriode::getBeregningsresultatPeriodeFom))
            .map(BeregningsresultatPeriode::getBeregningsresultatPeriodeFom).get();
        var beregningsresultatRevurderingFP = buildBeregningsresultatRevurderingEntenForBrukerEllerArbgvr(true, true);
        var gruppertYtelse2 = mapper.fordelPåNøkler(beregningsresultatRevurderingFP);
        var builder2 = getInputStandardBuilder(gruppertYtelse2).medTidligereOppdrag(mapTidligereOppdrag(List.of(originaltOppdrag)));

        // Act
        var oppdragRevurdering = OppdragMedPositivKvitteringTestUtil.opprett(nyOppdragskontrollTjeneste, builder2.build());

        //Assert
        var oppdragslinje150Opphørt = oppdragRevurdering.getOppdrag110Liste().stream().flatMap(oppdrag110 -> oppdrag110.getOppdragslinje150Liste()
            .stream()).filter(Oppdragslinje150::gjelderOpphør).findFirst().get();
        assertThat(oppdragslinje150Opphørt.getDatoStatusFom()).isEqualTo(førsteDatoVedtakFom);
    }

    @Test
    void opphørsDatoenMåSettesLikFørsteDatoVedtakFomNårDenneErSenereEnnEndringstdpktForBrukerMedFlereKlassekode() {
        // Arrange
        var beregningsresultat = buildBeregningsresultatMedFlereInntektskategoriFP(false);
        var mapper = new TilkjentYtelseMapper(FamilieYtelseType.FØDSEL);
        var gruppertYtelse = mapper.fordelPåNøkler(beregningsresultat);
        var builder = getInputStandardBuilder(gruppertYtelse);

        var originaltOppdrag = OppdragMedPositivKvitteringTestUtil.opprett(nyOppdragskontrollTjeneste, builder.build());

        var beregningsresultatRevurderingFP = buildBeregningsresultatRevurderingFP(AktivitetStatus.ARBEIDSTAKER, Inntektskategori.ARBEIDSTAKER, virksomhet,
            virksomhet2, true);
        var gruppertYtelse2 = mapper.fordelPåNøkler(beregningsresultatRevurderingFP);
        var builder2 = getInputStandardBuilder(gruppertYtelse2).medTidligereOppdrag(mapTidligereOppdrag(List.of(originaltOppdrag)));

        // Act
        var oppdragRevurdering = OppdragMedPositivKvitteringTestUtil.opprett(nyOppdragskontrollTjeneste, builder2.build());

        //Assert
        assertThat(oppdragRevurdering).isNotNull();
        OppdragskontrollTestVerktøy.verifiserOpphørsdatoen(originaltOppdrag, oppdragRevurdering);
    }

    @Test
    void opphørsDatoenMåSettesLikFørsteDatoVedtakFomAvForrigeOppdragNårDenneErSenereEnnEndringstdpktArbgvr() {
        // Arrange
        var beregningsresultat = buildBeregningsresultatEntenForBrukerEllerArbgvr(false, true);
        var mapper = new TilkjentYtelseMapper(FamilieYtelseType.FØDSEL);
        var gruppertYtelse = mapper.fordelPåNøkler(beregningsresultat);
        var builder = getInputStandardBuilder(gruppertYtelse);

        var originaltOppdrag = OppdragMedPositivKvitteringTestUtil.opprett(nyOppdragskontrollTjeneste, builder.build());

        var førsteDatoVedtakFom = beregningsresultat.getGjeldendePerioder().stream().min(Comparator.comparing(BeregningsresultatPeriode::getBeregningsresultatPeriodeFom))
            .map(BeregningsresultatPeriode::getBeregningsresultatPeriodeFom).get();
        var beregningsresultatRevurderingFP = buildBeregningsresultatRevurderingEntenForBrukerEllerArbgvr(false, true);
        var gruppertYtelse2 = mapper.fordelPåNøkler(beregningsresultatRevurderingFP);
        var builder2 = getInputStandardBuilder(gruppertYtelse2).medTidligereOppdrag(mapTidligereOppdrag(List.of(originaltOppdrag)));

        // Act
        var oppdragRevurdering = OppdragMedPositivKvitteringTestUtil.opprett(nyOppdragskontrollTjeneste, builder2.build());

        //Assert
        var oppdragslinje150Opphørt = oppdragRevurdering.getOppdrag110Liste().stream().flatMap(oppdrag110 -> oppdrag110.getOppdragslinje150Liste()
            .stream()).filter(Oppdragslinje150::gjelderOpphør).findFirst().get();
        assertThat(oppdragslinje150Opphørt.getDatoStatusFom()).isEqualTo(førsteDatoVedtakFom);
    }

    @Test
    void retest_av_sak_fagsystem_160364_k27_opplyser_ikke_rett_refusjons_maksdato() {
        // Arrange
        var stønadsdatoFom = LocalDate.now().plusDays(3);
        var stønadsdatoTom = stønadsdatoFom.plusDays(10);

        var originaltOppdrag = Oppdragskontroll.builder().medBehandlingId(BEHANDLING_ID).medSaksnummer(SAKSNUMMER).medProsessTaskId(PROSESS_TASK_ID).medVenterKvittering(Boolean.FALSE).build();
        var oppdragAg = Oppdrag110.builder()
            .medKodeEndring(KodeEndring.NY)
            .medKodeFagomrade(KodeFagområde.FPREF)
            .medFagSystemId(Long.parseLong(SAKSNUMMER.getVerdi() + "100"))
            .medOppdragGjelderId(BRUKER_FNR)
            .medSaksbehId(ANSVARLIG_SAKSBEHANDLER)
            .medAvstemming(Avstemming.ny())
            .medOppdragskontroll(originaltOppdrag)
            .build();
        var oppdragLinjeAg = Oppdragslinje150.builder()
            .medKodeEndringLinje(KodeEndringLinje.NY)
            .medKodeKlassifik(KodeKlassifik.FPF_REFUSJON_AG)
            .medVedtakFomOgTom(stønadsdatoFom, stønadsdatoTom)
            .medSats(Sats.på(1500))
            .medTypeSats(TypeSats.DAG)
            .medDelytelseId(Long.parseLong(SAKSNUMMER.getVerdi() + "100100"))
            .medUtbetalingsgrad(Utbetalingsgrad._100)
            .medOppdrag110(oppdragAg).build();

        Refusjonsinfo156.builder().medMaksDato(stønadsdatoTom).medDatoFom(stønadsdatoFom).medRefunderesId(virksomhet).medOppdragslinje150(oppdragLinjeAg).build();
        OppdragKvitteringTestUtil.lagPositivKvitting(oppdragAg);

        // Tilkyent ytelse i revurdering
        var beregningsresultatRevurderingFP = BeregningsresultatEntitet.builder().medRegelInput("clob1")
            .medRegelSporing("clob2").build();

        var brPeriode1 = buildBeregningsresultatPeriode(beregningsresultatRevurderingFP, 3, 10);
        var andelArb = buildBeregningsresultatAndel(brPeriode1, false, 1500, BigDecimal.valueOf(100), virksomhet);

        var feriepenger = buildBeregningsresultatFeriepenger();
        buildBeregningsresultatFeriepengerPrÅr(feriepenger, andelArb, 20000L, List.of(stønadsdatoFom));

        var mapper = new TilkjentYtelseMapper(FamilieYtelseType.FØDSEL);
        var gruppertYtelse2 = mapper.fordelPåNøkler(beregningsresultatRevurderingFP, feriepenger);
        var builder2 = getInputStandardBuilder(gruppertYtelse2).medTidligereOppdrag(mapTidligereOppdrag(List.of(originaltOppdrag)));

        // Act
        var oppdragRevurdering = OppdragMedPositivKvitteringTestUtil.opprett(nyOppdragskontrollTjeneste, builder2.build());

        //Assert
        var oppdragslinje150Opphørt = oppdragRevurdering.getOppdrag110Liste().stream().flatMap(oppdrag110 -> oppdrag110.getOppdragslinje150Liste()
            .stream()).filter(Oppdragslinje150::gjelderOpphør).findFirst();
        assertThat(oppdragslinje150Opphørt).isPresent();

        var oppdragslinje150Feriepenger = oppdragRevurdering.getOppdrag110Liste().stream().flatMap(oppdrag110 -> oppdrag110.getOppdragslinje150Liste()
            .stream()).filter(o150 -> !o150.gjelderOpphør()).findFirst();
        assertThat(oppdragslinje150Feriepenger).isPresent();

        var sisteOppdragsDatoTom = beregningsresultatRevurderingFP.getBeregningsresultatPerioder().stream().max(Comparator.comparing(BeregningsresultatPeriode::getBeregningsresultatPeriodeTom))
            .map(BeregningsresultatPeriode::getBeregningsresultatPeriodeTom);
        assertThat(sisteOppdragsDatoTom).isPresent();

        var førsteOppdragsDatoFom = beregningsresultatRevurderingFP.getBeregningsresultatPerioder().stream().min(Comparator.comparing(BeregningsresultatPeriode::getBeregningsresultatPeriodeFom))
            .map(BeregningsresultatPeriode::getBeregningsresultatPeriodeFom);
        assertThat(førsteOppdragsDatoFom).isPresent();

        var oppdragslinje150 = oppdragslinje150Opphørt.get();
        var refusjonsinfo156 = oppdragslinje150.getRefusjonsinfo156();
        assertThat(oppdragslinje150.getVedtakId()).isEqualTo(VEDTAKSDATO.toString());
        assertThat(refusjonsinfo156.getDatoFom()).isEqualTo(VEDTAKSDATO);
        assertThat(refusjonsinfo156.getMaksDato()).isEqualTo(sisteOppdragsDatoTom.get());

        var feriepengerFom = LocalDate.of(stønadsdatoFom.plusYears(1).getYear(), 5, 1);
        var feriepengerTom = LocalDate.of(stønadsdatoFom.plusYears(1).getYear(), 5, 31);
        var oppdragslinje150Ferie = oppdragslinje150Feriepenger.get();
        assertThat(oppdragslinje150Ferie.getRefusjonsinfo156().getDatoFom()).isEqualTo(VEDTAKSDATO);
        assertThat(oppdragslinje150Ferie.getRefusjonsinfo156().getMaksDato()).isEqualTo(feriepengerTom);
        assertThat(oppdragslinje150Ferie.getDatoVedtakFom()).isEqualTo(feriepengerFom);
        assertThat(oppdragslinje150Ferie.getDatoVedtakTom()).isEqualTo(feriepengerTom);
    }

    @Test
    void opphørSkalIkkeSendesHvisEndringstidspunktErEtterSisteDatoITidligereOppdragForBruker() {
        // Arrange
        var beregningsresultat = buildBeregningsresultatEntenForBrukerEllerArbgvr(true, false);
        var mapper = new TilkjentYtelseMapper(FamilieYtelseType.FØDSEL);
        var gruppertYtelse = mapper.fordelPåNøkler(beregningsresultat);
        var builder = getInputStandardBuilder(gruppertYtelse);

        var originaltOppdrag = OppdragMedPositivKvitteringTestUtil.opprett(nyOppdragskontrollTjeneste, builder.build());

        var sistePeriodeTom = beregningsresultat.getGjeldendePerioder().stream()
            .map(BeregningsresultatPeriode::getBeregningsresultatPeriodeTom).max(Comparator.comparing(Function.identity())).get();
        var endringsdato = sistePeriodeTom.plusDays(1);
        var beregningsresultatRevurderingFP = buildEmptyBeregningsresultatFP();
        var brPeriode_1 = buildBeregningsresultatPeriode(beregningsresultatRevurderingFP, endringsdato.plusDays(1), endringsdato.plusDays(10));
        buildBeregningsresultatAndel(brPeriode_1, true, 500, BigDecimal.valueOf(100), virksomhet);
        var gruppertYtelse2 = mapper.fordelPåNøkler(beregningsresultatRevurderingFP);
        var builder2 = getInputStandardBuilder(gruppertYtelse2).medTidligereOppdrag(mapTidligereOppdrag(List.of(originaltOppdrag)));

        // Act
        var oppdragRevurdering = OppdragMedPositivKvitteringTestUtil.opprett(nyOppdragskontrollTjeneste, builder2.build());

        //Assert
        assertThat(oppdragRevurdering).isNotNull();
        OppdragskontrollTestVerktøy.verifiserAvstemming(oppdragRevurdering);
        // TODO: sjekk denne metoden og test verifiserOppdrag110OgOppdragslinje150(oppdragRevurdering, originaltOppdrag110Liste, false);
    }

    // Førstegangsbehandling: VedtakResultat=Innvilget, Behandlingsresultat=Innvilget, FinnnesTilkjentYtelse=Ja
    // Revurdering: VedtakResultat=Innvilget, Behandlingsresultat=Foreldrepenger endret, FinnesTilkjentYtelse=Nei(Utbetalingsgrad=0)
    @Test
    void skalSendeOpphørNårForrigeBehandlingHarTilkjentYtelseOgRevurderingHarIngenTilkjentYtelsePgaNullUtbetalingsgradSomBlirSattIUttak() {
        // Arrange
        var fom = LocalDate.of(I_ÅR, 8, 1);
        var tom = LocalDate.of(I_ÅR, 8, 15);
        var beregningsresultat = buildEmptyBeregningsresultatFP();
        var brPeriode = buildBeregningsresultatPeriode(beregningsresultat, fom, tom);
        buildBeregningsresultatAndel(brPeriode, true, 2000, BigDecimal.valueOf(100), virksomhet);
        var mapper = new TilkjentYtelseMapper(FamilieYtelseType.FØDSEL);
        var gruppertYtelse = mapper.fordelPåNøkler(beregningsresultat);
        var builder = getInputStandardBuilder(gruppertYtelse);

        var originaltOppdrag = OppdragMedPositivKvitteringTestUtil.opprett(nyOppdragskontrollTjeneste, builder.build());

        var beregningsresultatRevurderingFP = buildEmptyBeregningsresultatFP();
        var brPeriodeRevurdering = buildBeregningsresultatPeriode(beregningsresultatRevurderingFP, fom, tom);
        buildBeregningsresultatAndel(brPeriodeRevurdering, true, 0, BigDecimal.valueOf(0), virksomhet);
        var gruppertYtelse2 = mapper.fordelPåNøkler(beregningsresultatRevurderingFP);
        var builder2 = getInputStandardBuilder(gruppertYtelse2).medTidligereOppdrag(mapTidligereOppdrag(List.of(originaltOppdrag)));

        // Act
        var oppdragRevurdering = OppdragMedPositivKvitteringTestUtil.opprett(nyOppdragskontrollTjeneste, builder2.build());

        //Assert
        var forrigeYtelseStartDato = brPeriode.getBeregningsresultatPeriodeFom();
        var opp150RevurderingListe = oppdragRevurdering.getOppdrag110Liste().stream()
            .flatMap(oppdrag110 -> oppdrag110.getOppdragslinje150Liste().stream())
            .toList();
        assertThat(opp150RevurderingListe)
            .hasSize(1)
            .allSatisfy(oppdragslinje150 -> {
                assertThat(oppdragslinje150.gjelderOpphør()).isTrue();
                assertThat(oppdragslinje150.getDatoStatusFom()).isEqualTo(forrigeYtelseStartDato);
            });
    }

    @Test
    void skalSendeOppdragUtenOmposteringHvisFullstendigOpphørPåBruker() {
        // Arrange
        var b1fom = LocalDate.of(I_ÅR, 1, 1);
        var b1tom = LocalDate.of(I_ÅR, 8, 20);
        var beregningsresultat = buildBeregningsresultatBrukerFP(400, 400, b1fom, b1tom);
        var mapper = new TilkjentYtelseMapper(FamilieYtelseType.FØDSEL);
        var gruppertYtelse = mapper.fordelPåNøkler(beregningsresultat);
        var builder = getInputStandardBuilder(gruppertYtelse);

        var originaltOppdrag = OppdragMedPositivKvitteringTestUtil.opprett(nyOppdragskontrollTjeneste, builder.build());

        var beregningsresultatRevurderingFP = buildBeregningsresultatBrukerFP(0, 300, b1fom, b1tom);
        var gruppertYtelse2 = mapper.fordelPåNøkler(beregningsresultatRevurderingFP);
        var builder2 = getInputStandardBuilder(gruppertYtelse2).medTidligereOppdrag(mapTidligereOppdrag(List.of(originaltOppdrag)));

        // Act
        var oppdragRevurdering = OppdragMedPositivKvitteringTestUtil.opprett(nyOppdragskontrollTjeneste, builder2.build());

        // Assert
        var oppdrag110Bruker = OppdragskontrollTestVerktøy.getOppdrag110ForBruker(oppdragRevurdering.getOppdrag110Liste());
        assertThat(oppdrag110Bruker.getKodeEndring()).isEqualTo(KodeEndring.ENDR);
        assertThat(oppdrag110Bruker.getOmpostering116()).isNotPresent();
    }

    @SuppressWarnings("unused")
    private void verifiserOppdrag110OgOppdragslinje150(Oppdragskontroll oppdragskontroll, List<Oppdrag110> originaltOpp110Liste, boolean medFlereKlassekode) {
        var nyOppdr110Liste = oppdragskontroll.getOppdrag110Liste();
        for (var oppdr110Revurd : nyOppdr110Liste) {
            assertThat(oppdr110Revurd.getKodeEndring()).isEqualTo(KodeEndring.ENDR);
            assertThat(oppdr110Revurd.getOppdragslinje150Liste()).isNotEmpty();
            assertThat(originaltOpp110Liste).anySatisfy(oppdrag110 ->
                assertThat(oppdrag110.getFagsystemId()).isEqualTo(oppdr110Revurd.getFagsystemId()));
        }
        var opp150RevurderingListe = nyOppdr110Liste.stream().flatMap(oppdrag110 -> oppdrag110.getOppdragslinje150Liste().stream()).toList();
        var opp150OriginalListe = originaltOpp110Liste.stream().flatMap(oppdrag110 -> oppdrag110.getOppdragslinje150Liste().stream()).toList();
        assertThat(opp150RevurderingListe).anySatisfy(opp150 -> assertThat(opp150.getKodeStatusLinje()).isNull());
        if (medFlereKlassekode) {
            OppdragskontrollTestVerktøy.verifiserDelYtelseOgFagsystemIdForEnKlassekode(opp150RevurderingListe, opp150OriginalListe);
        } else {
            OppdragskontrollTestVerktøy.verifiserDelYtelseOgFagsystemIdForFlereKlassekode(opp150RevurderingListe, opp150OriginalListe);
        }
        for (var opp150Revurdering : opp150RevurderingListe) {
            assertThat(opp150OriginalListe).allSatisfy(opp150 ->
                assertThat(opp150.getDelytelseId()).isNotEqualTo(opp150Revurdering.getDelytelseId()));
            if (!gjelderFagområdeBruker(opp150Revurdering.getOppdrag110())) {
                assertThat(opp150Revurdering.getRefusjonsinfo156()).isNotNull();
            }
            if (!OppdragskontrollTestVerktøy.erOpp150ForFeriepenger(opp150Revurdering)) {
                assertThat(opp150Revurdering.getUtbetalingsgrad()).isNotNull();
            } else {
                assertThat(opp150Revurdering.getUtbetalingsgrad()).isNull();
            }
        }
    }

    private void verifiserOPPHForBrukerIENDR(Oppdragskontroll oppdragRevurdering, List<Oppdrag110> originaltOpp110Liste, LocalDate endringsdato) {
        var nyOppdr110Bruker = oppdragRevurdering.getOppdrag110Liste()
            .stream()
            .filter(this::gjelderFagområdeBruker)
            .findFirst();
        assertThat(nyOppdr110Bruker)
            .isPresent()
            .hasValueSatisfying(opp110 -> {
                assertThat(opp110.getKodeEndring()).isEqualTo(KodeEndring.ENDR);
                assertThat(opp110.getOppdragslinje150Liste()).isNotEmpty();
                assertThat(originaltOpp110Liste).anySatisfy(oppdrag110 ->
                    assertThat(oppdrag110.getFagsystemId()).isEqualTo(nyOppdr110Bruker.get().getFagsystemId()));
            });
        verifiserOppdragslinje150_OPPH_Bruker_I_ENDR(originaltOpp110Liste, nyOppdr110Bruker.get(), endringsdato);
    }

    private void verifiserOppdragslinje150_OPPH_Bruker_I_ENDR(List<Oppdrag110> opp110OriginalListe, Oppdrag110 nyOpp110Bruker, LocalDate endringsdato) {
        var originaltOpp150BrukerListe = opp110OriginalListe.stream()
            .filter(this::gjelderFagområdeBruker)
            .flatMap(oppdrag110 -> oppdrag110.getOppdragslinje150Liste().stream())
            .toList();
        var revurderingOpp150BrukerListe = nyOpp110Bruker.getOppdragslinje150Liste();

        assertThat(revurderingOpp150BrukerListe).anySatisfy(opp150 ->
            assertThat(opp150.getKodeKlassifik()).isEqualTo(KodeKlassifik.FERIEPENGER_BRUKER));

        for (var ix = 0; ix < revurderingOpp150BrukerListe.size(); ix++) {
            var revurderingOpp150Bruker = revurderingOpp150BrukerListe.get(ix);
            var originaltOpp150Bruker = originaltOpp150BrukerListe.get(ix);
            assertThat(revurderingOpp150Bruker.getDelytelseId()).isEqualTo(originaltOpp150Bruker.getDelytelseId());
            assertThat(revurderingOpp150Bruker.getRefDelytelseId()).isNull();
            assertThat(revurderingOpp150Bruker.getRefFagsystemId()).isNull();
            assertThat(revurderingOpp150Bruker.getKodeEndringLinje()).isEqualTo(KodeEndringLinje.ENDR);
            assertThat(revurderingOpp150Bruker.getKodeStatusLinje()).isEqualTo(KodeStatusLinje.OPPH);
            var førsteDatoVedtakFom = OppdragskontrollTestVerktøy.finnFørsteDatoVedtakFom(originaltOpp150BrukerListe, originaltOpp150Bruker);
            var datoStatusFom = førsteDatoVedtakFom.isAfter(endringsdato) ? førsteDatoVedtakFom : endringsdato;
            assertThat(revurderingOpp150Bruker.getDatoStatusFom()).isEqualTo(revurderingOpp150Bruker.getKodeKlassifik().equals(KodeKlassifik.FERIEPENGER_BRUKER)
                ? LocalDate.of(I_ÅR + 1, 5, 1) : datoStatusFom);
            assertThat(revurderingOpp150Bruker.getSats()).isEqualTo(originaltOpp150Bruker.getSats());
        }
    }

    private List<Oppdrag110> verifiserOppdrag110_OPPH(Oppdragskontroll oppdragRevurdering, Oppdragskontroll originaltOppdrag) {
        var originaltOppdrag110Liste = originaltOppdrag.getOppdrag110Liste();
        var nyOppdr110Liste = oppdragRevurdering.getOppdrag110Liste();
        verifiserAlleOppdragOpphørt(originaltOppdrag110Liste, nyOppdr110Liste);

        assertThat(oppdragRevurdering.getOppdrag110Liste()).hasSameSizeAs(originaltOppdrag110Liste);
        for (var ix110 = 0; ix110 < nyOppdr110Liste.size(); ix110++) {
            assertThat(nyOppdr110Liste.get(ix110).getKodeEndring()).isEqualTo(KodeEndring.ENDR);
            assertThat(nyOppdr110Liste.get(ix110).getFagsystemId()).isEqualTo(originaltOppdrag110Liste.get(ix110).getFagsystemId());
            assertThat(nyOppdr110Liste.get(ix110).getOppdragslinje150Liste()).isNotEmpty();
        }
        return nyOppdr110Liste;
    }

    private void verifiserAlleOppdragOpphørt(List<Oppdrag110> originaltOpp110Liste, List<Oppdrag110> nyOppdr110Liste) {
        for (var originalt : originaltOpp110Liste) {
            var nyttOppdrag = nyOppdr110Liste.stream()
                .filter(nytt -> originalt.getFagsystemId() == nytt.getFagsystemId())
                .findFirst().orElse(null);
            assertThat(nyttOppdrag).isNotNull();
            var klassifikasjoner = originalt.getOppdragslinje150Liste().stream()
                .map(Oppdragslinje150::getKodeKlassifik)
                .distinct()
                .toList();
            for (var klassifikasjon : klassifikasjoner) {
                var opphørslinje = nyttOppdrag.getOppdragslinje150Liste().stream()
                    .filter(opp150 -> klassifikasjon.equals(opp150.getKodeKlassifik()))
                    .filter(opp150 -> KodeEndringLinje.ENDR.equals(opp150.getKodeEndringLinje()))
                    .filter(opp150 -> KodeStatusLinje.OPPH.equals(opp150.getKodeStatusLinje()))
                    .findFirst();
                assertThat(opphørslinje)
                    .as("Mangler oppdragslinje med opphør for klassifikasjon %s i oppdrag %s", klassifikasjon,
                        nyttOppdrag.getFagsystemId())
                    .isNotEmpty();
            }
        }
    }

    private void verifiserOppdragslinje150MedFlereKategorier_OPPH(Oppdragskontroll oppdragRevurdering, Oppdragskontroll originaltOppdrag) {
        var originaltOppLinjePerKodeKl = originaltOppdrag.getOppdrag110Liste().stream()
            .filter(this::gjelderFagområdeBruker)
            .flatMap(oppdrag110 -> oppdrag110.getOppdragslinje150Liste().stream())
            .collect(Collectors.groupingBy(Oppdragslinje150::getKodeKlassifik));
        var nyOpp150LinjePerKodeKl = oppdragRevurdering.getOppdrag110Liste().stream()
            .filter(this::gjelderFagområdeBruker)
            .flatMap(oppdrag110 -> oppdrag110.getOppdragslinje150Liste().stream())
            .collect(Collectors.groupingBy(Oppdragslinje150::getKodeKlassifik));

        for (var kodeKlassifik : originaltOppLinjePerKodeKl.keySet()) {
            var sisteOriginaltOppdragsLinje = originaltOppLinjePerKodeKl.get(kodeKlassifik).stream()
                .max(Comparator.comparing(Oppdragslinje150::getDelytelseId)).get();
            var sisteNyOppdragsLinje = nyOpp150LinjePerKodeKl.get(kodeKlassifik).get(0);
            assertThat(sisteOriginaltOppdragsLinje.getDelytelseId()).isEqualTo(sisteNyOppdragsLinje.getDelytelseId());
            assertThat(sisteNyOppdragsLinje.getRefDelytelseId()).isNull();
            assertThat(sisteNyOppdragsLinje.getRefFagsystemId()).isNull();
            assertThat(sisteNyOppdragsLinje.getKodeEndringLinje()).isEqualTo(KodeEndringLinje.ENDR);
            assertThat(sisteNyOppdragsLinje.getKodeStatusLinje()).isEqualTo(KodeStatusLinje.OPPH);
        }
    }

    private List<Oppdragslinje150> verifiserOppdragslinje150_OPPH(Oppdragskontroll oppdragRevurdering, Oppdragskontroll originaltOppdrag) {
        var originaltOpp150Liste = OppdragskontrollTestVerktøy.getOppdragslinje150Liste(originaltOppdrag);
        var nyOpp150Liste = OppdragskontrollTestVerktøy.getOppdragslinje150Liste(oppdragRevurdering);

        for (var nyOpp150 : nyOpp150Liste) {
            var originaltOpp150 = originaltOpp150Liste.stream()
                .filter(oppdragslinje150 -> oppdragslinje150.getDatoVedtakFom().equals(nyOpp150.getDatoVedtakFom())
                    && oppdragslinje150.getDatoVedtakTom().equals(nyOpp150.getDatoVedtakTom())
                    && oppdragslinje150.getOppdrag110().getKodeFagomrade().equals(nyOpp150.getOppdrag110().getKodeFagomrade()))
                .findFirst().get();
            assertThat(nyOpp150.getDelytelseId()).isEqualTo(originaltOpp150.getDelytelseId());
            assertThat(nyOpp150.getRefDelytelseId()).isNull();
            assertThat(nyOpp150.getRefFagsystemId()).isNull();
            assertThat(nyOpp150.getKodeEndringLinje()).isEqualTo(KodeEndringLinje.ENDR);
            assertThat(nyOpp150.getKodeStatusLinje()).isEqualTo(KodeStatusLinje.OPPH);
            assertThat(nyOpp150.getSats()).isEqualTo(originaltOpp150.getSats());
        }
        return nyOpp150Liste;
    }

    private boolean gjelderFagområdeBruker(Oppdrag110 oppdrag110) {
        return KodeFagområde.FP.equals(oppdrag110.getKodeFagomrade());
    }
}
