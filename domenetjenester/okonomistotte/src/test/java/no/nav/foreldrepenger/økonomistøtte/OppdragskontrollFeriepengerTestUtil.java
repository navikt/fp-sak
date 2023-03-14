package no.nav.foreldrepenger.økonomistøtte;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.LocalDate;
import java.util.List;
import java.util.stream.Collectors;

import no.nav.foreldrepenger.behandlingslager.økonomioppdrag.Oppdrag110;
import no.nav.foreldrepenger.behandlingslager.økonomioppdrag.Oppdragskontroll;
import no.nav.foreldrepenger.behandlingslager.økonomioppdrag.Oppdragslinje150;
import no.nav.foreldrepenger.behandlingslager.økonomioppdrag.koder.KodeKlassifik;
import no.nav.foreldrepenger.økonomistøtte.oppdrag.NyOppdragskontrollTjenesteTestBase;

public class OppdragskontrollFeriepengerTestUtil {

    static final LocalDate DAGENS_DATO = LocalDate.now();
    static final List<Integer> FERIEPENGEÅR_LISTE = List.of(DAGENS_DATO.plusYears(1).getYear(), DAGENS_DATO.plusYears(2).getYear());

    public static void verifiserOpp150NårEndringGjelderEttFeriepengeår(List<Oppdragslinje150> opp150ForrigeFeriepengerListe,
                                                                       List<Oppdragslinje150> opp150RevurderingFeriepengerListe,
                                                                       boolean erEndringForFørsteÅr) {
        int endretFeriepengeår = erEndringForFørsteÅr ? FERIEPENGEÅR_LISTE.get(0) : FERIEPENGEÅR_LISTE.get(1);
        for (var forrigeOpp150 : opp150ForrigeFeriepengerListe) {
            if (forrigeOpp150.getDatoVedtakFom().getYear() == endretFeriepengeår) {
                assertThat(opp150RevurderingFeriepengerListe).anySatisfy(
                    oppdragslinje150 -> assertThat(oppdragslinje150.getRefDelytelseId()).isEqualTo(forrigeOpp150.getDelytelseId()));
            }
        }
        assertThat(opp150RevurderingFeriepengerListe).allSatisfy(opp150 -> assertThat(opp150.gjelderOpphør()).isFalse());
        var ix = erEndringForFørsteÅr ? 1 : 0;
        int ikkeEndretFeriepengeår = FERIEPENGEÅR_LISTE.get(ix);
        var opp150RevurdFørsteFeriepengeår = opp150RevurderingFeriepengerListe.stream()
            .filter(oppdragslinje150 -> oppdragslinje150.getDatoVedtakFom().getYear() == ikkeEndretFeriepengeår)
            .findFirst();
        assertThat(opp150RevurdFørsteFeriepengeår).isNotPresent();
    }

    public static void verifiserOppdr150MedEttFeriepengeårKunIRevurdering(List<Oppdragslinje150> opp150RevurderingFeriepengerListe,
                                                                          boolean gjelderFørsteÅr) {
        int åretSomSkalHaFeriepenger = gjelderFørsteÅr ? FERIEPENGEÅR_LISTE.get(0) : FERIEPENGEÅR_LISTE.get(1);
        var ix = gjelderFørsteÅr ? 1 : 0;
        int åretSomIkkeSkalHaFeriepenger = FERIEPENGEÅR_LISTE.get(ix);
        assertThat(opp150RevurderingFeriepengerListe).anySatisfy(
            oppdragslinje150 -> assertThat(oppdragslinje150.getDatoVedtakFom().getYear()).isEqualTo(åretSomSkalHaFeriepenger));
        assertThat(opp150RevurderingFeriepengerListe).allSatisfy(
            oppdragslinje150 -> assertThat(oppdragslinje150.getDatoVedtakFom().getYear()).isNotEqualTo(åretSomIkkeSkalHaFeriepenger));
    }

    public static void verifiserOppdr150NårDetIkkeErFeriepengerIRevurdering(Oppdragskontroll forrigeOppdrag,
                                                                            Oppdragskontroll oppdragRevurdering,
                                                                            boolean erOpphørPåFørsteÅr) {
        var opp150FeriepengerListe = getOppdragslinje150Feriepenger(forrigeOppdrag);
        assertThat(opp150FeriepengerListe).isNotEmpty();
        assertThat(opp150FeriepengerListe).allSatisfy(oppdragslinje150 -> {
            assertThat(oppdragslinje150.getRefDelytelseId()).isNull();
            assertThat(oppdragslinje150.getRefFagsystemId()).isNull();
        });
        var opp150RevurdFeriepengerListe = getOppdragslinje150Feriepenger(oppdragRevurdering);
        var ix = erOpphørPåFørsteÅr ? 0 : 1;
        int feriepengeår = FERIEPENGEÅR_LISTE.get(ix);
        assertThat(opp150RevurdFeriepengerListe).isNotEmpty();
        assertThat(opp150RevurdFeriepengerListe).allSatisfy(oppdragslinje150 -> {
            assertThat(oppdragslinje150.gjelderOpphør()).isTrue();
            assertThat(oppdragslinje150.getDatoVedtakFom().getYear()).isEqualTo(feriepengeår);
        });
    }

    public static void verifiserOppdr150NårEttFeriepengeårSkalOpphøre(List<Oppdragslinje150> opp150FeriepengerListe,
                                                                      List<Oppdragslinje150> opp150RevurderingFeriepengerListe,
                                                                      boolean skalFørsteFPÅrOpphøre) {
        int opphørtFeriepengeår = skalFørsteFPÅrOpphøre ? FERIEPENGEÅR_LISTE.get(0) : FERIEPENGEÅR_LISTE.get(1);
        for (var forrigeOpp150 : opp150FeriepengerListe) {
            if (forrigeOpp150.getDatoVedtakFom().getYear() == opphørtFeriepengeår) {
                assertThat(opp150RevurderingFeriepengerListe).anySatisfy(
                    oppdragslinje150 -> assertThat(oppdragslinje150.getDelytelseId()).isEqualTo(forrigeOpp150.getDelytelseId()));
            } else {
                assertThat(opp150RevurderingFeriepengerListe).anySatisfy(
                    oppdragslinje150 -> assertThat(oppdragslinje150.getRefDelytelseId()).isEqualTo(forrigeOpp150.getDelytelseId()));
            }
        }
    }

    public static void verifiserOpp150NårEttFPÅretOpphørerOgAndreIkkeEndrerSeg(List<Oppdragslinje150> opp150FeriepengerListe,
                                                                               List<Oppdragslinje150> opp150RevurderingFeriepengerListe,
                                                                               boolean skalFørsteOpphøre) {
        int oppphørtFeriepengeår = skalFørsteOpphøre ? FERIEPENGEÅR_LISTE.get(0) : FERIEPENGEÅR_LISTE.get(1);
        var ix = skalFørsteOpphøre ? 1 : 0;
        int feriepengeåretUtenEndring = FERIEPENGEÅR_LISTE.get(ix);
        assertThat(opp150RevurderingFeriepengerListe).anySatisfy(opp150 -> {
            assertThat(opp150.gjelderOpphør()).isTrue();
            assertThat(opp150.getDatoVedtakFom().getYear()).isEqualTo(oppphørtFeriepengeår);
        });
        assertThat(opp150RevurderingFeriepengerListe).allSatisfy(
            opp150 -> assertThat(opp150.getDatoVedtakFom().getYear()).isNotEqualTo(feriepengeåretUtenEndring));
        for (var forrigeOpp150 : opp150FeriepengerListe) {
            if (forrigeOpp150.getDatoVedtakFom().getYear() == oppphørtFeriepengeår) {
                assertThat(opp150RevurderingFeriepengerListe).anySatisfy(
                    oppdragslinje150 -> assertThat(oppdragslinje150.getDelytelseId()).isEqualTo(forrigeOpp150.getDelytelseId()));
            }
        }
    }

    public static void verifiserOppdr150NårDetErEndringForToFeriepengeår(List<Oppdragslinje150> forrigeOpp150FeriepengerListe,
                                                                         List<Oppdragslinje150> opp150RevurderingFeriepengerListe) {
        assertThat(opp150RevurderingFeriepengerListe).isNotEmpty().allSatisfy(opp150 -> {
            if (forrigeOpp150FeriepengerListe.isEmpty()) {
                assertThat(opp150.getRefDelytelseId()).isNull();
                assertThat(opp150.getRefFagsystemId()).isNull();
            } else {
                assertThat(opp150.getRefDelytelseId()).isNotNull();
                assertThat(opp150.getRefFagsystemId()).isNotNull();
            }
            assertThat(opp150.gjelderOpphør()).isFalse();
        });
    }

    public static void verifiserRefDelytelseId(List<Oppdragslinje150> opp150FeriepengerListe,
                                               List<Oppdragslinje150> opp150RevurderingFeriepengerListe,
                                               boolean skalFørsteÅrKjedes) {
        int kjedetFeriepengeår = skalFørsteÅrKjedes ? FERIEPENGEÅR_LISTE.get(0) : FERIEPENGEÅR_LISTE.get(1);
        var ix = skalFørsteÅrKjedes ? 1 : 0;
        int ikkeKjedetFeriepengeår = FERIEPENGEÅR_LISTE.get(ix);

        for (var forrigeOpp150 : opp150FeriepengerListe) {
            assertThat(forrigeOpp150.getDatoVedtakFom().getYear()).isNotEqualTo(ikkeKjedetFeriepengeår);
            if (forrigeOpp150.getDatoVedtakFom().getYear() == kjedetFeriepengeår) {
                assertThat(opp150RevurderingFeriepengerListe).anySatisfy(
                    oppdragslinje150 -> assertThat(oppdragslinje150.getRefDelytelseId()).isEqualTo(forrigeOpp150.getDelytelseId()));
            }
        }
    }

    public static void verifiserFeriepengeår(List<Oppdragslinje150> opp150FeriepengerListe) {
        assertThat(opp150FeriepengerListe).isNotEmpty();
        FERIEPENGEÅR_LISTE.forEach(feriepengeår -> assertThat(opp150FeriepengerListe).anySatisfy(
            oppdragslinje150 -> assertThat(oppdragslinje150.getDatoVedtakFom().getYear()).isEqualTo(feriepengeår)));
    }

    public static List<Oppdragslinje150> getOppdr150ForFeriepengerForEnMottaker(List<Oppdragslinje150> forrigeOpp150FeriepengerListe,
                                                                                boolean erBrukerMottaker) {
        var kodeKlassifik = erBrukerMottaker ? KodeKlassifik.FERIEPENGER_BRUKER : KodeKlassifik.FPF_FERIEPENGER_AG;
        return forrigeOpp150FeriepengerListe.stream().filter(opp150 -> kodeKlassifik.equals(opp150.getKodeKlassifik())).collect(Collectors.toList());
    }

    static List<Oppdragslinje150> getOppdragslinje150Feriepenger(Oppdragskontroll oppdrag) {
        return oppdrag.getOppdrag110Liste()
            .stream()
            .map(NyOppdragskontrollTjenesteTestBase::getOppdragslinje150Feriepenger)
            .flatMap(List::stream)
            .collect(Collectors.toList());
    }

    static List<Oppdragslinje150> getOppdragslinje150Feriepenger(Oppdrag110 oppdrag110) {
        return oppdrag110.getOppdragslinje150Liste()
            .stream()
            .filter(opp150 -> opp150.getKodeKlassifik().gjelderFeriepenger())
            .collect(Collectors.toList());
    }
}
