package no.nav.foreldrepenger.økonomistøtte.ny;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;

import no.nav.foreldrepenger.behandlingslager.behandling.beregning.AktivitetStatus;
import no.nav.foreldrepenger.behandlingslager.behandling.beregning.BeregningsresultatAndel;
import no.nav.foreldrepenger.behandlingslager.behandling.beregning.BeregningsresultatEntitet;
import no.nav.foreldrepenger.behandlingslager.behandling.beregning.BeregningsresultatFeriepenger;
import no.nav.foreldrepenger.behandlingslager.behandling.beregning.BeregningsresultatFeriepengerPrÅr;
import no.nav.foreldrepenger.behandlingslager.behandling.beregning.BeregningsresultatPeriode;
import no.nav.foreldrepenger.behandlingslager.behandling.beregning.Inntektskategori;
import no.nav.foreldrepenger.behandlingslager.fagsak.FagsakYtelseType;
import no.nav.foreldrepenger.behandlingslager.virksomhet.Arbeidsgiver;
import no.nav.foreldrepenger.behandlingslager.økonomioppdrag.FamilieYtelseType;
import no.nav.foreldrepenger.behandlingslager.økonomioppdrag.Oppdrag110;
import no.nav.foreldrepenger.behandlingslager.økonomioppdrag.Oppdragskontroll;
import no.nav.foreldrepenger.behandlingslager.økonomioppdrag.Oppdragslinje150;
import no.nav.foreldrepenger.behandlingslager.økonomioppdrag.koder.KodeEndring;
import no.nav.foreldrepenger.behandlingslager.økonomioppdrag.koder.KodeFagområde;
import no.nav.foreldrepenger.behandlingslager.økonomioppdrag.koder.KodeKlassifik;
import no.nav.foreldrepenger.domene.typer.AktørId;
import no.nav.foreldrepenger.domene.typer.Saksnummer;
import no.nav.foreldrepenger.økonomistøtte.OppdragMedPositivKvitteringTestUtil;
import no.nav.foreldrepenger.økonomistøtte.ny.domene.Betalingsmottaker;
import no.nav.foreldrepenger.økonomistøtte.ny.domene.KjedeNøkkel;
import no.nav.foreldrepenger.økonomistøtte.ny.domene.Periode;
import no.nav.foreldrepenger.økonomistøtte.ny.domene.Satsen;
import no.nav.foreldrepenger.økonomistøtte.ny.domene.Ytelse;
import no.nav.foreldrepenger.økonomistøtte.ny.domene.YtelsePeriode;
import no.nav.foreldrepenger.økonomistøtte.ny.domene.samlinger.GruppertYtelse;
import no.nav.foreldrepenger.økonomistøtte.ny.domene.samlinger.OverordnetOppdragKjedeOversikt;
import no.nav.foreldrepenger.økonomistøtte.ny.mapper.EksisterendeOppdragMapper;
import no.nav.foreldrepenger.økonomistøtte.ny.mapper.LagOppdragTjeneste;
import no.nav.foreldrepenger.økonomistøtte.ny.mapper.OppdragInput;
import no.nav.foreldrepenger.økonomistøtte.ny.mapper.TilkjentYtelseMapper;
import no.nav.foreldrepenger.økonomistøtte.ny.tjeneste.OppdragskontrollTjenesteImpl;
import no.nav.foreldrepenger.økonomistøtte.ØkonomioppdragRepository;

public abstract class NyOppdragskontrollTjenesteTestBase {

    public static final long PROSESS_TASK_ID = 23L;
    public static final String BRUKER_FNR = "12345678901";
    public static final Saksnummer SAKSNUMMER = Saksnummer.infotrygd("101000");
    public static final long BEHANDLING_ID = 123456L;
    public static final String ANSVARLIG_SAKSBEHANDLER = "Katarzyna";

    public static final LocalDate VEDTAKSDATO = LocalDate.now();

    static final String ARBEIDSFORHOLD_ID = "999999999";
    static final String ARBEIDSFORHOLD_ID_2 = "123456789";
    static final String ARBEIDSFORHOLD_ID_3 = "789123456";
    static final String ARBEIDSFORHOLD_ID_4 = "654321987";
    static final Long AKTØR_ID = 1234567891234L;

    static final LocalDate DAGENS_DATO = LocalDate.now();
    static final int I_ÅR = DAGENS_DATO.getYear();
    static final List<Integer> FERIEPENGEÅR_LISTE = List.of(DAGENS_DATO.plusYears(1).getYear(),
        DAGENS_DATO.plusYears(2).getYear());

    protected ØkonomioppdragRepository økonomioppdragRepository;
    protected OppdragskontrollTjenesteImpl nyOppdragskontrollTjeneste;

    protected String virksomhet = ARBEIDSFORHOLD_ID;
    protected String virksomhet2 = ARBEIDSFORHOLD_ID_2;
    protected String virksomhet3 = ARBEIDSFORHOLD_ID_3;
    protected String virksomhet4 = ARBEIDSFORHOLD_ID_4;

    protected OppdragInput.Builder getInputStandardBuilder(GruppertYtelse gruppertYtelse) {
        return OppdragInput.builder()
            .medTilkjentYtelse(gruppertYtelse)
            .medTidligereOppdrag(OverordnetOppdragKjedeOversikt.TOM)
            .medBrukerFnr(BRUKER_FNR)
            .medBehandlingId(BEHANDLING_ID)
            .medSaksnummer(SAKSNUMMER)
            .medFagsakYtelseType(FagsakYtelseType.FORELDREPENGER)
            .medAnsvarligSaksbehandler(ANSVARLIG_SAKSBEHANDLER)
            .medVedtaksdato(VEDTAKSDATO)
            .medBrukInntrekk(true)
            .medProsessTaskId(PROSESS_TASK_ID);
    }

    protected OverordnetOppdragKjedeOversikt mapTidligereOppdrag(List<Oppdragskontroll> tidligereOppdragskontroll) {
        return new OverordnetOppdragKjedeOversikt(EksisterendeOppdragMapper.tilKjeder(tidligereOppdragskontroll));
    }

    public void setUp() {
        nyOppdragskontrollTjeneste = new OppdragskontrollTjenesteImpl(new LagOppdragTjeneste(), mock(ØkonomioppdragRepository.class));
    }

   protected GruppertYtelse buildTilkjentYtelseFP() {
        return buildTilkjentYtelseFP(false);
    }

    protected GruppertYtelse buildTilkjentYtelseFP(boolean medFeriepenger) {

        var builder = GruppertYtelse.builder()
            .leggTilKjede(
                KjedeNøkkel.lag(KodeKlassifik.FPF_ARBEIDSTAKER, Betalingsmottaker.BRUKER),
                Ytelse.builder()
                    .leggTilPeriode(lagPeriode(VEDTAKSDATO, 0, 7, Satsen.dagsats(1500), 80))
                    .leggTilPeriode(lagPeriode(VEDTAKSDATO, 8, 14, Satsen.dagsats(1600), 80))
                    .leggTilPeriode(lagPeriode(VEDTAKSDATO, 22, 28, Satsen.dagsats(2160), 80))
                    .build())
            .leggTilKjede(
                KjedeNøkkel.lag(KodeKlassifik.FPF_REFUSJON_AG, Betalingsmottaker.forArbeidsgiver(ARBEIDSFORHOLD_ID_2)),
                Ytelse.builder()
                    .leggTilPeriode(lagPeriode(VEDTAKSDATO, 8, 14, Satsen.dagsats(450), 100))
                    .build())
            .leggTilKjede(
                KjedeNøkkel.lag(KodeKlassifik.FPF_REFUSJON_AG, Betalingsmottaker.forArbeidsgiver(ARBEIDSFORHOLD_ID)),
                Ytelse.builder()
                    .leggTilPeriode(lagPeriode(VEDTAKSDATO, 0, 7, Satsen.dagsats(500), 100))
                    .build())
            .leggTilKjede(
                KjedeNøkkel.lag(KodeKlassifik.FPF_REFUSJON_AG, Betalingsmottaker.forArbeidsgiver(ARBEIDSFORHOLD_ID_3)),
                Ytelse.builder()
                    .leggTilPeriode(lagPeriode(VEDTAKSDATO, 15, 21, Satsen.dagsats(2160), 80))
                    .build()
            );

        if (medFeriepenger) {
            builder
                .leggTilKjede(
                    KjedeNøkkel.lag(KodeKlassifik.FERIEPENGER_BRUKER, Betalingsmottaker.BRUKER, VEDTAKSDATO.getYear()),
                    Ytelse.builder()
                        .leggTilPeriode(lagFeriepengerPeriode(VEDTAKSDATO, Satsen.engang(20000)))
                        .build())
                .leggTilKjede(
                    KjedeNøkkel.lag(KodeKlassifik.FPF_FERIEPENGER_AG, Betalingsmottaker.forArbeidsgiver(ARBEIDSFORHOLD_ID), VEDTAKSDATO.getYear()),
                    Ytelse.builder()
                        .leggTilPeriode(lagFeriepengerPeriode(VEDTAKSDATO, Satsen.engang(15000)))
                        .build())
                .leggTilKjede(
                    KjedeNøkkel.lag(KodeKlassifik.FPF_FERIEPENGER_AG, Betalingsmottaker.forArbeidsgiver(ARBEIDSFORHOLD_ID_2), VEDTAKSDATO.getYear()),
                    Ytelse.builder()
                        .leggTilPeriode(lagFeriepengerPeriode(VEDTAKSDATO, Satsen.engang(20000)))
                        .build())
                .leggTilKjede(
                    KjedeNøkkel.lag(KodeKlassifik.FPF_FERIEPENGER_AG, Betalingsmottaker.forArbeidsgiver(ARBEIDSFORHOLD_ID_3), VEDTAKSDATO.getYear()),
                    Ytelse.builder()
                        .leggTilPeriode(lagFeriepengerPeriode(VEDTAKSDATO, Satsen.engang(20000)))
                        .build())
                .leggTilKjede(
                    KjedeNøkkel.lag(KodeKlassifik.FPF_FERIEPENGER_AG, Betalingsmottaker.forArbeidsgiver(ARBEIDSFORHOLD_ID_3), VEDTAKSDATO.plusYears(1).getYear()),
                    Ytelse.builder()
                        .leggTilPeriode(lagFeriepengerPeriode(VEDTAKSDATO.plusYears(1), Satsen.engang(20000)))
                        .build());
        }

        return builder.build();
    }

    protected BeregningsresultatEntitet buildBeregningsresultatFP() {
        return buildBeregningsresultatFP(false);
    }

    protected BeregningsresultatEntitet buildBeregningsresultatFP(boolean medFeriepenger) {
        var beregningsresultat = BeregningsresultatEntitet.builder()
            .medRegelInput("clob1")
            .medRegelSporing("clob2")
            .build();
        var brPeriode1 = buildBeregningsresultatPeriode(beregningsresultat, 1, 7);
        var andelBruker = buildBeregningsresultatAndel(brPeriode1, true, 1500,
            BigDecimal.valueOf(80), virksomhet);
        var andelArbeidsforhold = buildBeregningsresultatAndel(brPeriode1, false, 500,
            BigDecimal.valueOf(100), virksomhet);

        var brPeriode2 = buildBeregningsresultatPeriode(beregningsresultat, 8, 15);
        buildBeregningsresultatAndel(brPeriode2, true, 1600, BigDecimal.valueOf(80), virksomhet2);
        var andelArbeidsforhold2 = buildBeregningsresultatAndel(brPeriode2, false, 450,
            BigDecimal.valueOf(100), virksomhet2);

        var brPeriode3 = buildBeregningsresultatPeriode(beregningsresultat, 16, 22);
        buildBeregningsresultatAndel(brPeriode3, true, 0, BigDecimal.valueOf(80), virksomhet3);
        var andelArbeidsforhold3 = buildBeregningsresultatAndel(brPeriode3, false, 2160,
            BigDecimal.valueOf(80), virksomhet3);

        var brPeriode4 = buildBeregningsresultatPeriode(beregningsresultat, 23, 30);
        buildBeregningsresultatAndel(brPeriode4, true, 2160, BigDecimal.valueOf(80), virksomhet3);
        buildBeregningsresultatAndel(brPeriode4, false, 0, BigDecimal.valueOf(80), virksomhet3);

        if (medFeriepenger) {
            var feriepenger = buildBeregningsresultatFeriepenger(beregningsresultat);
            buildBeregningsresultatFeriepengerPrÅr(feriepenger, andelBruker, 20000L, List.of(DAGENS_DATO));
            buildBeregningsresultatFeriepengerPrÅr(feriepenger, andelArbeidsforhold, 15000L, List.of(DAGENS_DATO));
            buildBeregningsresultatFeriepengerPrÅr(feriepenger, andelArbeidsforhold2, 20000L, List.of(DAGENS_DATO));
            buildBeregningsresultatFeriepengerPrÅr(feriepenger, andelArbeidsforhold3, 20000L,
                List.of(DAGENS_DATO, DAGENS_DATO.plusYears(1)));
        }

        return beregningsresultat;
    }

    protected BeregningsresultatAndel buildBeregningsresultatAndel(BeregningsresultatPeriode beregningsresultatPeriode,
                                                                   Boolean brukerErMottaker,
                                                                   int dagsats,
                                                                   BigDecimal utbetalingsgrad,
                                                                   String virksomhetOrgnr) {
        return buildBeregningsresultatAndel(beregningsresultatPeriode, brukerErMottaker, dagsats, utbetalingsgrad,
            virksomhetOrgnr, AktivitetStatus.ARBEIDSTAKER, Inntektskategori.ARBEIDSTAKER);
    }

    protected BeregningsresultatAndel buildBeregningsresultatAndel(BeregningsresultatPeriode beregningsresultatPeriode,
                                                                   Boolean brukerErMottaker,
                                                                   int dagsats,
                                                                   BigDecimal utbetalingsgrad,
                                                                   String virksomhetOrgnr,
                                                                   AktivitetStatus aktivitetStatus,
                                                                   Inntektskategori inntektskategori) {
        var andelBuilder = BeregningsresultatAndel.builder()
            .medBrukerErMottaker(brukerErMottaker);
        if (!AktivitetStatus.FRILANSER.equals(aktivitetStatus) && virksomhetOrgnr != null) {
            andelBuilder.medArbeidsgiver(Arbeidsgiver.virksomhet(virksomhetOrgnr));
        }
        if (AktivitetStatus.ARBEIDSTAKER.equals(aktivitetStatus) && virksomhetOrgnr == null) {
            andelBuilder.medArbeidsgiver(Arbeidsgiver.person(new AktørId(AKTØR_ID)));
        }
        return andelBuilder.medDagsats(dagsats)
            .medDagsatsFraBg(dagsats)
            .medStillingsprosent(BigDecimal.valueOf(100))
            .medUtbetalingsgrad(utbetalingsgrad)
            .medAktivitetStatus(aktivitetStatus)
            .medInntektskategori(inntektskategori)
            .build(beregningsresultatPeriode);
    }

    protected BeregningsresultatPeriode buildBeregningsresultatPeriode(BeregningsresultatEntitet beregningsresultat,
                                                                       int fom,
                                                                       int tom) {
        return buildBeregningsresultatPeriode(beregningsresultat, DAGENS_DATO.plusDays(fom), DAGENS_DATO.plusDays(tom));
    }

    protected BeregningsresultatPeriode buildBeregningsresultatPeriode(BeregningsresultatEntitet beregningsresultat,
                                                                       LocalDate fom,
                                                                       LocalDate tom) {

        return BeregningsresultatPeriode.builder()
            .medBeregningsresultatPeriodeFomOgTom(fom, tom)
            .build(beregningsresultat);
    }

    protected BeregningsresultatFeriepenger buildBeregningsresultatFeriepenger(BeregningsresultatEntitet beregningsresultat) {
        return BeregningsresultatFeriepenger.builder()
            .medFeriepengerPeriodeFom(DAGENS_DATO.plusDays(1))
            .medFeriepengerPeriodeTom(DAGENS_DATO.plusDays(29))
            .medFeriepengerRegelInput("clob1")
            .medFeriepengerRegelSporing("clob2")
            .build(beregningsresultat);
    }

    protected void buildBeregningsresultatFeriepengerPrÅr(BeregningsresultatFeriepenger beregningsresultatFeriepenger,
                                                          BeregningsresultatAndel andel,
                                                          Long årsBeløp,
                                                          List<LocalDate> opptjeningsårList) {
        for (var opptjeningsår : opptjeningsårList) {
            buildBeregningsresultatFeriepengerPrÅr(beregningsresultatFeriepenger, andel, årsBeløp, opptjeningsår);
        }
    }

    protected void buildBeregningsresultatFeriepengerPrÅr(BeregningsresultatFeriepenger beregningsresultatFeriepenger,
                                                          BeregningsresultatAndel andel,
                                                          Long årsBeløp,
                                                          LocalDate opptjeningsår) {
        BeregningsresultatFeriepengerPrÅr.builder()
            .medOpptjeningsår(opptjeningsår)
            .medÅrsbeløp(årsBeløp)
            .build(beregningsresultatFeriepenger, andel);
    }

   protected GruppertYtelse buildTilkjentYtelseMedFlereInntektskategoriFP(boolean medFeriepenger) {
        var gruppertYtelse = GruppertYtelse.builder()
            .leggTilKjede(
                KjedeNøkkel.lag(KodeKlassifik.FPF_ARBEIDSTAKER, Betalingsmottaker.BRUKER),
                Ytelse.builder()
                    .leggTilPeriode(lagPeriode(VEDTAKSDATO, 0, 7, Satsen.dagsats(1500), 80))
                    .leggTilPeriode(lagPeriode(VEDTAKSDATO, 8, 14, Satsen.dagsats(1600), 80))
                    .build())
            .leggTilKjede(
                KjedeNøkkel.lag(KodeKlassifik.FPF_FRILANSER, Betalingsmottaker.BRUKER),
                Ytelse.builder()
                    .leggTilPeriode(lagPeriode(VEDTAKSDATO, 0, 7, Satsen.dagsats(150), 80))
                    .build())
            .leggTilKjede(
                KjedeNøkkel.lag(KodeKlassifik.FPF_REFUSJON_AG, Betalingsmottaker.forArbeidsgiver(ARBEIDSFORHOLD_ID)),
                Ytelse.builder()
                    .leggTilPeriode(lagPeriode(VEDTAKSDATO, 0, 7, Satsen.dagsats(500), 100))
                    .build())
            .leggTilKjede(
                KjedeNøkkel.lag(KodeKlassifik.FPF_REFUSJON_AG, Betalingsmottaker.forArbeidsgiver(ARBEIDSFORHOLD_ID_2)),
                Ytelse.builder()
                    .leggTilPeriode(lagPeriode(VEDTAKSDATO, 8, 14, Satsen.dagsats(400), 100))
                    .build());

        if (medFeriepenger) {
            gruppertYtelse
                .leggTilKjede(
                    KjedeNøkkel.lag(KodeKlassifik.FERIEPENGER_BRUKER, Betalingsmottaker.BRUKER, VEDTAKSDATO.getYear()),
                    Ytelse.builder()
                        .leggTilPeriode(lagFeriepengerPeriode(VEDTAKSDATO, Satsen.engang(20000)))
                        .build())
                .leggTilKjede(
                    KjedeNøkkel.lag(KodeKlassifik.FPF_FERIEPENGER_AG, Betalingsmottaker.forArbeidsgiver(ARBEIDSFORHOLD_ID), VEDTAKSDATO.getYear()),
                    Ytelse.builder()
                        .leggTilPeriode(lagFeriepengerPeriode(VEDTAKSDATO, Satsen.engang(15000)))
                        .build())
                .leggTilKjede(
                    KjedeNøkkel.lag(KodeKlassifik.FPF_FERIEPENGER_AG, Betalingsmottaker.forArbeidsgiver(ARBEIDSFORHOLD_ID_2), VEDTAKSDATO.getYear()),
                    Ytelse.builder()
                        .leggTilPeriode(lagFeriepengerPeriode(VEDTAKSDATO, Satsen.engang(20000)))
                        .build());
        }
        return gruppertYtelse.build();
    }

    protected YtelsePeriode lagPeriode(LocalDate referanseDato, int plusDagerFom, int plusDagerTom, Satsen sats, int utbetalingsgrad) {
        return new YtelsePeriode(Periode.of(referanseDato.plusDays(plusDagerFom), referanseDato.plusDays(plusDagerTom)), sats, no.nav.foreldrepenger.økonomistøtte.ny.domene.Utbetalingsgrad.prosent(utbetalingsgrad));
    }

    protected YtelsePeriode lagFeriepengerPeriode(LocalDate opptjeningsår,  Satsen sats) {
        return new YtelsePeriode(Periode.of(LocalDate.of(opptjeningsår.plusYears(1).getYear(), 5, 1), LocalDate.of(opptjeningsår.plusYears(1).getYear(), 5, 31)), sats);
    }

    protected BeregningsresultatEntitet buildBeregningsresultatMedFlereInntektskategoriFP(boolean medFeriepenger) {
        var beregningsresultat = BeregningsresultatEntitet.builder()
            .medRegelInput("clob1")
            .medRegelSporing("clob2")
            .build();
        var brPeriode1 = buildBeregningsresultatPeriode(beregningsresultat, 1, 7);

        var andelBruker = buildBeregningsresultatAndel(brPeriode1, true, 1500,
            BigDecimal.valueOf(80), virksomhet);
        buildBeregningsresultatAndel(brPeriode1, true, 1500, BigDecimal.valueOf(80), virksomhet2,
            AktivitetStatus.FRILANSER, Inntektskategori.FRILANSER);
        var andelArbeidsforhold = buildBeregningsresultatAndel(brPeriode1, false, 500,
            BigDecimal.valueOf(100), virksomhet);

        var brPeriode2 = buildBeregningsresultatPeriode(beregningsresultat, 8, 15);
        buildBeregningsresultatAndel(brPeriode2, true, 1600, BigDecimal.valueOf(80), virksomhet2);
        var andelArbeidsforhold2 = buildBeregningsresultatAndel(brPeriode2, false, 400,
            BigDecimal.valueOf(100), virksomhet2);

        if (medFeriepenger) {
            var feriepenger = buildBeregningsresultatFeriepenger(beregningsresultat);
            buildBeregningsresultatFeriepengerPrÅr(feriepenger, andelBruker, 20000L, List.of(DAGENS_DATO));
            buildBeregningsresultatFeriepengerPrÅr(feriepenger, andelArbeidsforhold, 15000L, List.of(DAGENS_DATO));
            buildBeregningsresultatFeriepengerPrÅr(feriepenger, andelArbeidsforhold2, 20000L, List.of(DAGENS_DATO));
        }

        return beregningsresultat;
    }

    protected GruppertYtelse buildTilkjentYtelseMedFlereAndelerSomArbeidsgiver() {
        var gruppertYtelse = GruppertYtelse.builder()
            .leggTilKjede(
                KjedeNøkkel.lag(KodeKlassifik.FPF_ARBEIDSTAKER, Betalingsmottaker.BRUKER),
                Ytelse.builder()
                    .leggTilPeriode(lagPeriode(VEDTAKSDATO, 0, 7, Satsen.dagsats(1500), 80))
                    .leggTilPeriode(lagPeriode(VEDTAKSDATO, 8, 14, Satsen.dagsats(1600), 80))
                    .build())
            .leggTilKjede(
                KjedeNøkkel.lag(KodeKlassifik.FPF_FRILANSER, Betalingsmottaker.BRUKER),
                Ytelse.builder()
                    .leggTilPeriode(lagPeriode(VEDTAKSDATO, 0, 7, Satsen.dagsats(1500), 80))
                    .build())
            .leggTilKjede(
                KjedeNøkkel.lag(KodeKlassifik.FPF_SELVSTENDIG, Betalingsmottaker.BRUKER),
                Ytelse.builder()
                    .leggTilPeriode(lagPeriode(VEDTAKSDATO, 0, 7, Satsen.dagsats(1000), 100))
                    .build())
            .leggTilKjede(
                KjedeNøkkel.lag(KodeKlassifik.FPF_REFUSJON_AG, Betalingsmottaker.forArbeidsgiver(ARBEIDSFORHOLD_ID)),
                Ytelse.builder()
                    .leggTilPeriode(lagPeriode(VEDTAKSDATO, 0, 7, Satsen.dagsats(500), 100))
                    .build())
            .leggTilKjede(
                KjedeNøkkel.lag(KodeKlassifik.FPF_REFUSJON_AG, Betalingsmottaker.forArbeidsgiver(ARBEIDSFORHOLD_ID_2)),
                Ytelse.builder()
                    .leggTilPeriode(lagPeriode(VEDTAKSDATO, 8, 14, Satsen.dagsats(400), 100))
                    .build());
        return gruppertYtelse.build();
    }

    protected void verifiserOppdrag110_ENDR(List<Oppdrag110> nyOpp110Liste,
                                            List<Oppdrag110> originaltOpp110Liste,
                                            boolean medFeriepenger) {
        for (var oppdr110Revurd : nyOpp110Liste) {
            var refusjonsinfo156 = oppdr110Revurd.getOppdragslinje150Liste()
                .stream()
                .map(Oppdragslinje150::getRefusjonsinfo156)
                .filter(Objects::nonNull)
                .filter(r -> r.getRefunderesId()
                    .equals(OppdragskontrollTestVerktøy.endreTilElleveSiffer(ARBEIDSFORHOLD_ID_4)))
                .findFirst();
            if (refusjonsinfo156.isPresent()) {
                var opp110 = refusjonsinfo156.get().getOppdragslinje150().getOppdrag110();
                assertThat(opp110.getKodeEndring()).isEqualTo(KodeEndring.NY);
            } else {
                assertThat(oppdr110Revurd.getKodeEndring()).isEqualTo(KodeFagområde.FORELDREPENGER_BRUKER
                    .equals(
                        oppdr110Revurd.getKodeFagomrade()) ? KodeEndring.ENDRING : KodeEndring.ENDRING);
            }
            assertThat(oppdr110Revurd.getOppdragslinje150Liste()).isNotEmpty();
            var nyMottaker = erMottakerNy(oppdr110Revurd);
            if (!nyMottaker) {
                assertThat(originaltOpp110Liste).anySatisfy(
                    oppdrag110 -> assertThat(oppdrag110.getFagsystemId()).isEqualTo(oppdr110Revurd.getFagsystemId()));
            }
        }
        if (medFeriepenger) {
            var opp150List = nyOpp110Liste.stream()
                .flatMap(oppdrag110 -> oppdrag110.getOppdragslinje150Liste().stream())
                .collect(Collectors.toList());
            assertThat(opp150List).anySatisfy(opp150 -> assertThat(opp150.getKodeKlassifik()).isEqualTo(
                KodeKlassifik.FERIEPENGER_BRUKER));
            assertThat(opp150List).anySatisfy(opp150 -> assertThat(opp150.getKodeKlassifik()).isEqualTo(
                KodeKlassifik.FPF_FERIEPENGER_AG));
        }
    }

    private boolean erMottakerNy(Oppdrag110 oppdr110Revurd) {
        return KodeEndring.NY.equals(oppdr110Revurd.getKodeEndring());
    }

    protected BeregningsresultatEntitet buildBeregningsresultatBrukerFP(LocalDate endringsdato,
                                                                        int dagsatsBruker,
                                                                        int dagsatsArbeidsgiver,
                                                                        LocalDate... perioder) {
        return buildBeregningsresultatBrukerFP(endringsdato, List.of(dagsatsBruker), List.of(dagsatsArbeidsgiver),
            perioder);
    }

    protected BeregningsresultatEntitet buildBeregningsresultatBrukerFP(LocalDate endringsdato,
                                                                        List<Integer> dagsatsBruker,
                                                                        List<Integer> dagsatsArbeidsgiver,
                                                                        LocalDate... perioder) {
        var beregningsresultat = BeregningsresultatEntitet.builder()
            .medRegelInput("clob1")
            .medRegelSporing("clob2")
            .medEndringsdato(endringsdato)
            .build();

        var feriepenger = buildBeregningsresultatFeriepenger(beregningsresultat);
        for (var i = 0; i < dagsatsBruker.size(); i++) {
            var fom = perioder[i * 2];
            var tom = perioder[i * 2 + 1];
            var brPeriode = buildBeregningsresultatPeriode(beregningsresultat, fom, tom);
            var andelBruker = buildBeregningsresultatAndel(brPeriode, true, dagsatsBruker.get(i),
                BigDecimal.valueOf(100), virksomhet);
            if (dagsatsArbeidsgiver.get(i) != 0) {
                var andelArbeidsgiver = buildBeregningsresultatAndel(brPeriode, false,
                    dagsatsArbeidsgiver.get(i), BigDecimal.valueOf(100), virksomhet);
                buildBeregningsresultatFeriepengerPrÅr(feriepenger, andelArbeidsgiver, 5000L, List.of(DAGENS_DATO));
            }
            if (dagsatsBruker.get(i) != 0) {
                buildBeregningsresultatFeriepengerPrÅr(feriepenger, andelBruker, 5000L, List.of(DAGENS_DATO));
            }
        }

        return beregningsresultat;
    }

    protected BeregningsresultatEntitet buildBeregningsresultatMedFlereInntektskategoriFP(boolean sammeKlasseKodeForFlereAndeler,
                                                                                          AktivitetStatus aktivitetStatus,
                                                                                          Inntektskategori inntektskategori) {
        var beregningsresultat = BeregningsresultatEntitet.builder()
            .medRegelInput("clob1")
            .medRegelSporing("clob2")
            .build();
        var brPeriode1 = buildBeregningsresultatPeriode(beregningsresultat, 1, 7);
        var andelBruker = buildBeregningsresultatAndel(brPeriode1, true, 1500,
            BigDecimal.valueOf(80), virksomhet, aktivitetStatus, inntektskategori);
        if (sammeKlasseKodeForFlereAndeler) {
            buildBeregningsresultatAndel(brPeriode1, true, 1500, BigDecimal.valueOf(80), virksomhet2,
                AktivitetStatus.ARBEIDSTAKER, Inntektskategori.ARBEIDSTAKER_UTEN_FERIEPENGER);
        }
        var brPeriode2 = buildBeregningsresultatPeriode(beregningsresultat, 8, 15);
        var andelArbeidsgiver2 = buildBeregningsresultatAndel(brPeriode2, false, 400,
            BigDecimal.valueOf(100), virksomhet2);

        var feriepenger = buildBeregningsresultatFeriepenger(beregningsresultat);
        buildBeregningsresultatFeriepengerPrÅr(feriepenger, andelBruker, 20000L, List.of(DAGENS_DATO));
        buildBeregningsresultatFeriepengerPrÅr(feriepenger, andelArbeidsgiver2, 20000L, List.of(DAGENS_DATO));

        return beregningsresultat;
    }

    protected BeregningsresultatEntitet buildBeregningsresultatEntenForBrukerEllerArbgvr(boolean erBrukerMottaker,
                                                                                         boolean medFeriepenger) {
        var beregningsresultat = BeregningsresultatEntitet.builder()
            .medRegelInput("clob1")
            .medRegelSporing("clob2")
            .build();
        var brPeriode1 = buildBeregningsresultatPeriode(beregningsresultat, 1, 10);
        var andel1 = buildBeregningsresultatAndel(brPeriode1, erBrukerMottaker, 1500,
            BigDecimal.valueOf(100), virksomhet);
        var brPeriode2 = buildBeregningsresultatPeriode(beregningsresultat, 11, 20);
        buildBeregningsresultatAndel(brPeriode2, erBrukerMottaker, 1500, BigDecimal.valueOf(100), virksomhet);
        if (medFeriepenger) {
            var feriepenger = buildBeregningsresultatFeriepenger(beregningsresultat);
            buildBeregningsresultatFeriepengerPrÅr(feriepenger, andel1, 20000L, List.of(DAGENS_DATO));
        }

        return beregningsresultat;
    }

    protected BeregningsresultatEntitet buildBeregningsresultatFPForVerifiseringAvOpp150MedFeriepenger(Optional<LocalDate> endringsdatoOpt,
                                                                                                     boolean erOpptjentOverFlereÅr,
                                                                                                     Long årsbeløp1,
                                                                                                     Long årsbeløp2) {
        var builder = BeregningsresultatEntitet.builder()
            .medRegelInput("clob1")
            .medRegelSporing("clob2");
        endringsdatoOpt.ifPresent(builder::medEndringsdato);
        var beregningsresultat = builder.build();
        var brPeriode1 = buildBeregningsresultatPeriode(beregningsresultat, 1, 10);
        var andel1 = buildBeregningsresultatAndel(brPeriode1, true, 1500, BigDecimal.valueOf(100),
            virksomhet);
        var andel2 = buildBeregningsresultatAndel(brPeriode1, false, 1300, BigDecimal.valueOf(100),
            virksomhet);
        var feriepenger = buildBeregningsresultatFeriepenger(beregningsresultat);
        oppsettFeriepenger(erOpptjentOverFlereÅr, årsbeløp1, årsbeløp2, andel1, feriepenger);
        oppsettFeriepenger(erOpptjentOverFlereÅr, årsbeløp1, årsbeløp2, andel2, feriepenger);

        return beregningsresultat;
    }

    private void oppsettFeriepenger(boolean erOpptjentOverFlereÅr,
                                    Long årsbeløp1,
                                    Long årsbeløp2,
                                    BeregningsresultatAndel andel1,
                                    BeregningsresultatFeriepenger feriepenger) {
        List<LocalDate> opptjeningsårListe;
        if (erOpptjentOverFlereÅr) {
            opptjeningsårListe = List.of(DAGENS_DATO, DAGENS_DATO.plusYears(1));
        } else if (årsbeløp1 > 0 || årsbeløp2 > 0) {
            opptjeningsårListe = årsbeløp2 > 0 ? List.of(DAGENS_DATO.plusYears(1)) : List.of(DAGENS_DATO);
        } else {
            opptjeningsårListe = Collections.emptyList();
        }
        var årsbeløpListe = List.of(årsbeløp1, årsbeløp2);
        var size = opptjeningsårListe.size();
        for (var i = 0; i < årsbeløpListe.size(); i++) {
            var årsbeløp = årsbeløpListe.get(i);
            if (årsbeløp > 0) {
                var opptjeningsår = size == 2 ? opptjeningsårListe.get(i) : opptjeningsårListe.get(0);
                buildBeregningsresultatFeriepengerPrÅr(feriepenger, andel1, årsbeløp, opptjeningsår);
            }
        }
    }

    protected BeregningsresultatEntitet buildBeregningsresultatRevurderingFP(boolean medFeriepenger) {
        var beregningsresultatRevurderingFP = BeregningsresultatEntitet.builder()
            .medRegelInput("clob1")
            .medRegelSporing("clob2")
            .medEndringsdato(LocalDate.now().plusDays(25))
            .build();
        var brPeriode1 = buildBeregningsresultatPeriode(beregningsresultatRevurderingFP, 1, 7);
        buildBeregningsresultatAndel(brPeriode1, true, 1500, BigDecimal.valueOf(80), virksomhet);
        var andelRevurderingArbeidsforhold = buildBeregningsresultatAndel(brPeriode1, false, 500,
            BigDecimal.valueOf(100), virksomhet);

        var brPeriode2 = buildBeregningsresultatPeriode(beregningsresultatRevurderingFP, 8, 15);
        buildBeregningsresultatAndel(brPeriode2, true, 1600, BigDecimal.valueOf(80), virksomhet4);
        var andelRevurderingArbeidsforhold4 = buildBeregningsresultatAndel(brPeriode2, false, 400,
            BigDecimal.valueOf(100), virksomhet4);

        var brPeriode3 = buildBeregningsresultatPeriode(beregningsresultatRevurderingFP, 16, 22);
        buildBeregningsresultatAndel(brPeriode3, true, 0, BigDecimal.valueOf(80), virksomhet3);
        buildBeregningsresultatAndel(brPeriode3, false, 2160, BigDecimal.valueOf(80), virksomhet3);

        var brPeriode4 = buildBeregningsresultatPeriode(beregningsresultatRevurderingFP, 23, 29);
        buildBeregningsresultatAndel(brPeriode4, true, 2160, BigDecimal.valueOf(80), virksomhet3);
        buildBeregningsresultatAndel(brPeriode4, false, 0, BigDecimal.valueOf(80), virksomhet3);

        if (medFeriepenger) {
            var feriepengerRevurdering = buildBeregningsresultatFeriepenger(
                beregningsresultatRevurderingFP);
            buildBeregningsresultatFeriepengerPrÅr(feriepengerRevurdering, andelRevurderingArbeidsforhold, 15000L,
                List.of(DAGENS_DATO));
            buildBeregningsresultatFeriepengerPrÅr(feriepengerRevurdering, andelRevurderingArbeidsforhold4, 15000L,
                List.of(DAGENS_DATO));
        }
        return beregningsresultatRevurderingFP;
    }

    protected BeregningsresultatEntitet buildBeregningsresultatRevurderingFP(AktivitetStatus aktivitetStatus,
                                                                             Inntektskategori inntektskategori,
                                                                             LocalDate endringsdato) {

        return buildBeregningsresultatRevurderingFP(aktivitetStatus, inntektskategori, virksomhet, virksomhet4,
            endringsdato, true);
    }

    /**
     * Lag to perioder, hver med en andel med oppgitt {@link AktivitetStatus} og {@link Inntektskategori}.
     *
     * @param aktivitetStatus
     * @param inntektskategori
     * @param førsteVirksomhetOrgnr
     * @param andreVirksomhetOrgnr
     * @param endringsdato
     * @param medFeriepenger
     * @return
     */
    protected BeregningsresultatEntitet buildBeregningsresultatRevurderingFP(AktivitetStatus aktivitetStatus,
                                                                             Inntektskategori inntektskategori,
                                                                             String førsteVirksomhetOrgnr,
                                                                             String andreVirksomhetOrgnr,
                                                                             LocalDate endringsdato,
                                                                             boolean medFeriepenger) {
        var beregningsresultatRevurderingFP = BeregningsresultatEntitet.builder()
            .medRegelInput("clob1")
            .medRegelSporing("clob2")
            .medEndringsdato(endringsdato)
            .build();
        var brPeriode1 = buildBeregningsresultatPeriode(beregningsresultatRevurderingFP, 1, 7);
        buildBeregningsresultatAndel(brPeriode1, true, 1600, BigDecimal.valueOf(80), førsteVirksomhetOrgnr,
            aktivitetStatus, inntektskategori);
        var andelRevurderingArbeidsforhold = buildBeregningsresultatAndel(brPeriode1, false, 400,
            BigDecimal.valueOf(100), førsteVirksomhetOrgnr, aktivitetStatus, inntektskategori);

        var brPeriode2 = buildBeregningsresultatPeriode(beregningsresultatRevurderingFP, 8, 20);
        buildBeregningsresultatAndel(brPeriode2, true, 1600, BigDecimal.valueOf(80), andreVirksomhetOrgnr,
            aktivitetStatus, inntektskategori);
        var andelRevurderingArbeidsforhold4 = buildBeregningsresultatAndel(brPeriode2, false, 400,
            BigDecimal.valueOf(100), andreVirksomhetOrgnr, aktivitetStatus, inntektskategori);

        if (medFeriepenger) {
            var feriepengerRevurdering = buildBeregningsresultatFeriepenger(
                beregningsresultatRevurderingFP);
            buildBeregningsresultatFeriepengerPrÅr(feriepengerRevurdering, andelRevurderingArbeidsforhold, 15000L,
                List.of(DAGENS_DATO));
            buildBeregningsresultatFeriepengerPrÅr(feriepengerRevurdering, andelRevurderingArbeidsforhold4, 15000L,
                List.of(DAGENS_DATO));
        }

        return beregningsresultatRevurderingFP;
    }

    /**
     * Lag to perioder.
     * Periode 1: Lag andel for {@link AktivitetStatus#ARBEIDSTAKER}.
     * Periode 2: Lag andel for {@link AktivitetStatus#ARBEIDSTAKER} og en annen oppgitt {@link AktivitetStatus} og {@link Inntektskategori}.
     *
     * @param aktivitetStatus  en {@link AktivitetStatus}
     * @param inntektskategori en {@link Inntektskategori}
     * @param endringsdato     en endringsdato
     * @return Beregningsresultat
     */
    protected BeregningsresultatEntitet buildBeregningsresultatRevurderingMedFlereInntektskategoriFP(AktivitetStatus aktivitetStatus,
                                                                                                     Inntektskategori inntektskategori,
                                                                                                     LocalDate endringsdato) {
        var beregningsresultatRevurderingFP = BeregningsresultatEntitet.builder()
            .medRegelInput("clob1")
            .medRegelSporing("clob2")
            .medEndringsdato(endringsdato)
            .build();
        var brPeriode1 = buildBeregningsresultatPeriode(beregningsresultatRevurderingFP, 1, 7);
        buildBeregningsresultatAndel(brPeriode1, true, 1600, BigDecimal.valueOf(80), virksomhet4);

        var andelRevurderingArbeidsiver = buildBeregningsresultatAndel(brPeriode1, false, 400,
            BigDecimal.valueOf(100), virksomhet4, AktivitetStatus.ARBEIDSTAKER, Inntektskategori.ARBEIDSTAKER);

        var brPeriode2 = buildBeregningsresultatPeriode(beregningsresultatRevurderingFP, 8, 15);

        buildBeregningsresultatAndel(brPeriode2, true, 1600, BigDecimal.valueOf(80), virksomhet,
            AktivitetStatus.ARBEIDSTAKER, Inntektskategori.ARBEIDSTAKER);
        buildBeregningsresultatAndel(brPeriode2, true, 1500, BigDecimal.valueOf(80), virksomhet4, aktivitetStatus,
            inntektskategori);

        var feriepengerRevurdering = buildBeregningsresultatFeriepenger(
            beregningsresultatRevurderingFP);
        buildBeregningsresultatFeriepengerPrÅr(feriepengerRevurdering, andelRevurderingArbeidsiver, 16000L,
            List.of(DAGENS_DATO));

        return beregningsresultatRevurderingFP;
    }

    protected BeregningsresultatEntitet buildBeregningsresultatRevurderingEntenForBrukerEllerArbgvr(boolean erBrukerMottaker,
                                                                                                    boolean medFeriepenger,
                                                                                                    LocalDate endringsdato) {
        var beregningsresultatRevurderingFP = BeregningsresultatEntitet.builder()
            .medRegelInput("clob1")
            .medRegelSporing("clob2")
            .medEndringsdato(endringsdato)
            .build();
        var brPeriode1 = buildBeregningsresultatPeriode(beregningsresultatRevurderingFP, 1, 8);
        var andel1 = buildBeregningsresultatAndel(brPeriode1, erBrukerMottaker, 2000,
            BigDecimal.valueOf(100), virksomhet);
        var brPeriode2 = buildBeregningsresultatPeriode(beregningsresultatRevurderingFP, 9, 20);
        buildBeregningsresultatAndel(brPeriode2, erBrukerMottaker, 1500, BigDecimal.valueOf(100), virksomhet);
        if (medFeriepenger) {
            var feriepenger = buildBeregningsresultatFeriepenger(
                beregningsresultatRevurderingFP);
            buildBeregningsresultatFeriepengerPrÅr(feriepenger, andel1, 21000L, List.of(DAGENS_DATO));
        }

        return beregningsresultatRevurderingFP;
    }

    protected BeregningsresultatEntitet buildBeregningsresultatFP(Optional<LocalDate> endringsdato) {
        var builder = BeregningsresultatEntitet.builder()
            .medRegelInput("clob1")
            .medRegelSporing("clob2");
        endringsdato.ifPresent(builder::medEndringsdato);
        return builder.build();
    }

    protected Oppdragskontroll opprettBeregningsresultatOgFørstegangsoppdragForFeriepenger(boolean erOpptjentOverFlereÅr,
                                                                                           Long årsbeløp1,
                                                                                           Long årsbeløp2) {
        return opprettBeregningsresultatOgFørstegangsoppdragForFeriepenger(erOpptjentOverFlereÅr, true, årsbeløp1,
            årsbeløp2);
    }

    protected Oppdragskontroll opprettBeregningsresultatOgFørstegangsoppdragForFeriepenger(boolean erOpptjentOverFlereÅr,
                                                                                           boolean gjelderFødsel,
                                                                                           Long årsbeløp1,
                                                                                           Long årsbeløp2) {
        var beregningsresultat = buildBeregningsresultatFPForVerifiseringAvOpp150MedFeriepenger(
            Optional.empty(), erOpptjentOverFlereÅr, årsbeløp1, årsbeløp2);

        if (gjelderFødsel) {
            var mapper = new TilkjentYtelseMapper(FamilieYtelseType.FØDSEL);
            var gruppertYtelse = mapper.fordelPåNøkler(beregningsresultat);
            var builder = getInputStandardBuilder(gruppertYtelse);

            return OppdragMedPositivKvitteringTestUtil.opprett(nyOppdragskontrollTjeneste, builder.build());
        }

        var mapper = new TilkjentYtelseMapper(FamilieYtelseType.ADOPSJON);
        var gruppertYtelse = mapper.fordelPåNøkler(beregningsresultat);
        var builder = getInputStandardBuilder(gruppertYtelse);

        return OppdragMedPositivKvitteringTestUtil.opprett(nyOppdragskontrollTjeneste, builder.build());
    }

    public static List<Oppdragslinje150> getOppdragslinje150Feriepenger(Oppdragskontroll oppdrag) {
        return oppdrag.getOppdrag110Liste()
            .stream()
            .map(NyOppdragskontrollTjenesteTestBase::getOppdragslinje150Feriepenger)
            .flatMap(List::stream)
            .collect(Collectors.toList());
    }

    public static List<Oppdragslinje150> getOppdragslinje150Feriepenger(Oppdrag110 oppdrag110) {
        return oppdrag110.getOppdragslinje150Liste()
            .stream()
            .filter(opp150 -> opp150.getKodeKlassifik().gjelderFeriepenger())
            .collect(Collectors.toList());
    }

}
