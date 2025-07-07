package no.nav.foreldrepenger.økonomistøtte.oppdrag;

import static org.assertj.core.api.Assertions.assertThat;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import no.nav.foreldrepenger.behandlingslager.fagsak.FagsakYtelseType;
import no.nav.foreldrepenger.behandlingslager.økonomioppdrag.FamilieYtelseType;
import no.nav.foreldrepenger.behandlingslager.økonomioppdrag.Oppdrag110;
import no.nav.foreldrepenger.behandlingslager.økonomioppdrag.koder.KodeEndringLinje;
import no.nav.foreldrepenger.behandlingslager.økonomioppdrag.koder.KodeKlassifik;
import no.nav.foreldrepenger.behandlingslager.økonomioppdrag.koder.KodeStatusLinje;
import no.nav.foreldrepenger.økonomistøtte.OppdragMedPositivKvitteringTestUtil;
import no.nav.foreldrepenger.økonomistøtte.oppdrag.mapper.TilkjentYtelseMapper;

class OppdragskontrollTjenesteImplSVPTest extends NyOppdragskontrollTjenesteTestBase {

    @Override
    @BeforeEach
    public void setUp() {
        super.setUp();
    }


    @Test
    void skal_sende_oppdrag_for_svangerskapspenger() {
        //Arrange
        var baseDato = LocalDate.now();
        var beregningsresultat = buildEmptyBeregningsresultatFP();

        var brPeriode_1 = buildBeregningsresultatPeriode(beregningsresultat, baseDato.withDayOfMonth(1), baseDato);
        var andelBruker_1 = buildBeregningsresultatAndel(brPeriode_1, true, 1000, BigDecimal.valueOf(100L), virksomhet);
        var andelArbeidsgiver_1 = buildBeregningsresultatAndel(brPeriode_1, false, 1000, BigDecimal.valueOf(100L), virksomhet);

        var feriepenger = buildBeregningsresultatFeriepenger();
        buildBeregningsresultatFeriepengerPrÅr(feriepenger, andelBruker_1, 100L, baseDato);
        buildBeregningsresultatFeriepengerPrÅr(feriepenger, andelArbeidsgiver_1, 101L, baseDato);

        var mapper = new TilkjentYtelseMapper(FamilieYtelseType.SVANGERSKAPSPENGER);
        var gruppertYtelse = mapper.fordelPåNøkler(beregningsresultat, feriepenger);
        var builder = getInputStandardBuilder(gruppertYtelse).medFagsakYtelseType(FagsakYtelseType.SVANGERSKAPSPENGER);

        //Act
        var oppdragskontroll = nyOppdragskontrollTjeneste.opprettOppdrag(builder.build());

        //Assert
        assertThat(oppdragskontroll).isPresent();
        var oppdrag = oppdragskontroll.get();
        assertThat(oppdrag).isNotNull();
        var oppdrag110List = oppdrag.getOppdrag110Liste();
        assertThat(oppdrag110List).hasSize(2);
        //Oppdrag110 - Bruker
        var oppdrag110_Bruker = oppdrag110List.stream()
            .filter(o110 -> !o110.getKodeFagomrade().gjelderRefusjonTilArbeidsgiver())
            .findFirst();
        assertThat(oppdrag110_Bruker).isPresent();
        //Oppdrag110 - Arbeidsgiver
        var oppdrag110_Arbeidsgiver = oppdrag110List.stream()
            .filter(o110 -> o110.getKodeFagomrade().gjelderRefusjonTilArbeidsgiver())
            .findFirst();
        assertThat(oppdrag110_Arbeidsgiver).isPresent();
        //Oppdragslinje150 - Bruker
        var opp150List_Bruker = oppdrag110_Bruker.get().getOppdragslinje150Liste();
        assertThat(opp150List_Bruker.stream().filter(opp150 -> KodeKlassifik.SVP_ARBEDISTAKER.equals(opp150.getKodeKlassifik()))).hasSize(1);
        assertThat(opp150List_Bruker.stream().filter(opp150 -> KodeKlassifik.SVP_FERIEPENGER_BRUKER.equals(opp150.getKodeKlassifik()))).hasSize(1);
        //Oppdragslinje150 - Arbeidsgiver
        var opp150List_Arbeidsgiver = oppdrag110_Arbeidsgiver.get().getOppdragslinje150Liste();
        assertThat(opp150List_Arbeidsgiver).anySatisfy(opp150 ->
            assertThat(opp150.getKodeKlassifik()).isIn(Arrays.asList(
                KodeKlassifik.SVP_REFUSJON_AG,
                KodeKlassifik.SVP_FERIEPENGER_AG)
            ));
    }

    @Test
    void skal_sende_oppdrag_for_svangerskapspenger_overgang() {
        //Arrange
        var baseDato = LocalDate.of(2022, 12, 31);
        var beregningsresultat = buildEmptyBeregningsresultatFP();

        var brPeriode_1 = buildBeregningsresultatPeriode(beregningsresultat, baseDato.withDayOfMonth(1), baseDato);
        var andelBruker_1 = buildBeregningsresultatAndel(brPeriode_1, true, 1000, BigDecimal.valueOf(100L), virksomhet);
        var andelArbeidsgiver_1 = buildBeregningsresultatAndel(brPeriode_1, false, 1000, BigDecimal.valueOf(100L), virksomhet);

        var brPeriode_2 = buildBeregningsresultatPeriode(beregningsresultat, baseDato.plusDays(1), baseDato.plusDays(31));
        var andelBruker_2 = buildBeregningsresultatAndel(brPeriode_2, true, 1000, BigDecimal.valueOf(100L), virksomhet);
        var andelArbeidsgiver_2 =buildBeregningsresultatAndel(brPeriode_2, false, 1000, BigDecimal.valueOf(100L), virksomhet);

        var feriepenger = buildBeregningsresultatFeriepenger();
        buildBeregningsresultatFeriepengerPrÅr(feriepenger, andelBruker_1, 100L, baseDato);
        buildBeregningsresultatFeriepengerPrÅr(feriepenger, andelArbeidsgiver_1, 101L, baseDato);
        buildBeregningsresultatFeriepengerPrÅr(feriepenger, andelBruker_2, 100L, baseDato.plusYears(1));
        buildBeregningsresultatFeriepengerPrÅr(feriepenger, andelArbeidsgiver_2, 101L, baseDato.plusYears(1));

        var mapper = new TilkjentYtelseMapper(FamilieYtelseType.SVANGERSKAPSPENGER);
        var gruppertYtelse = mapper.fordelPåNøkler(beregningsresultat, feriepenger);
        var builder = getInputStandardBuilder(gruppertYtelse).medFagsakYtelseType(FagsakYtelseType.SVANGERSKAPSPENGER);

        //Act
        var oppdragskontroll = nyOppdragskontrollTjeneste.opprettOppdrag(builder.build());

        //Assert
        assertThat(oppdragskontroll).isPresent();
        var oppdrag = oppdragskontroll.get();
        assertThat(oppdrag).isNotNull();
        var oppdrag110List = oppdrag.getOppdrag110Liste();
        assertThat(oppdrag110List).hasSize(2);
        //Oppdrag110 - Bruker
        var oppdrag110_Bruker = oppdrag110List.stream()
            .filter(o110 -> !o110.getKodeFagomrade().gjelderRefusjonTilArbeidsgiver())
            .findFirst();
        assertThat(oppdrag110_Bruker).isPresent();
        //Oppdrag110 - Arbeidsgiver
        var oppdrag110_Arbeidsgiver = oppdrag110List.stream()
            .filter(o110 -> o110.getKodeFagomrade().gjelderRefusjonTilArbeidsgiver())
            .findFirst();
        assertThat(oppdrag110_Arbeidsgiver).isPresent();
        //Oppdragslinje150 - Bruker
        var opp150List_Bruker = oppdrag110_Bruker.get().getOppdragslinje150Liste();
        assertThat(opp150List_Bruker.stream().filter(opp150 -> KodeKlassifik.SVP_ARBEDISTAKER.equals(opp150.getKodeKlassifik()))).hasSize(1);
        assertThat(opp150List_Bruker.stream().filter(opp150 -> KodeKlassifik.FERIEPENGER_BRUKER.equals(opp150.getKodeKlassifik()))).hasSize(1);
        assertThat(opp150List_Bruker.stream().filter(opp150 -> KodeKlassifik.SVP_FERIEPENGER_BRUKER.equals(opp150.getKodeKlassifik()))).hasSize(1);
        //Oppdragslinje150 - Arbeidsgiver
        var opp150List_Arbeidsgiver = oppdrag110_Arbeidsgiver.get().getOppdragslinje150Liste();
        assertThat(opp150List_Arbeidsgiver).anySatisfy(opp150 ->
            assertThat(opp150.getKodeKlassifik()).isIn(Arrays.asList(
                KodeKlassifik.SVP_REFUSJON_AG,
                KodeKlassifik.SVP_FERIEPENGER_AG)
            ));
    }

    @Test
    void skal_sende_oppdrag_for_svangerskapspenger_migrer() {
        //Arrange
        var baseDato = LocalDate.of(2023, 12, 31);
        var beregningsresultat = buildEmptyBeregningsresultatFP();

        var brPeriode_1 = buildBeregningsresultatPeriode(beregningsresultat, baseDato.withDayOfMonth(1), baseDato);
        var andelBruker_1 = buildBeregningsresultatAndel(brPeriode_1, true, 1000, BigDecimal.valueOf(100L), virksomhet);
        var andelArbeidsgiver_1 = buildBeregningsresultatAndel(brPeriode_1, false, 1000, BigDecimal.valueOf(100L), virksomhet);

        var feriepenger = buildBeregningsresultatFeriepenger();
        buildBeregningsresultatFeriepengerPrÅr(feriepenger, andelBruker_1, 100L, baseDato);
        buildBeregningsresultatFeriepengerPrÅr(feriepenger, andelArbeidsgiver_1, 101L, baseDato);

        var mapper = new TilkjentYtelseMapper(FamilieYtelseType.SVANGERSKAPSPENGER);
        var gruppertYtelse = mapper.fordelPåNøkler(beregningsresultat, feriepenger);
        var builder = getInputStandardBuilder(gruppertYtelse).medFagsakYtelseType(FagsakYtelseType.SVANGERSKAPSPENGER);

        //Act
        var oppdragskontroll = OppdragMedPositivKvitteringTestUtil.opprett(nyOppdragskontrollTjeneste, builder.build());

        // Først sørge for at begge feriepengene er FPATFER - slik som tidligere sendte oppdrag vil være
        var original150L = oppdragskontroll.getOppdrag110Liste().stream()
            .map(Oppdrag110::getOppdragslinje150Liste)
            .flatMap(Collection::stream)
            .filter(o150 -> o150.getKodeKlassifik().gjelderFeriepenger())
            .toList();
        original150L.stream().filter(o150 -> KodeKlassifik.SVP_FERIEPENGER_BRUKER.equals(o150.getKodeKlassifik()))
            .forEach(o150 -> o150.setKodeKlassifik(KodeKlassifik.FERIEPENGER_BRUKER));
        assertThat(original150L.stream().filter(o150 -> KodeKlassifik.FERIEPENGER_BRUKER.equals(o150.getKodeKlassifik()))).hasSize(1);
        assertThat(original150L.stream().filter(o150 -> KodeKlassifik.SVP_FERIEPENGER_BRUKER.equals(o150.getKodeKlassifik()))).isEmpty();

        //Revurdering #1
        var rberegningsresultat = buildEmptyBeregningsresultatFP();

        var rbrPeriode_1 = buildBeregningsresultatPeriode(rberegningsresultat, baseDato.withDayOfMonth(1), baseDato);
        var randelBruker_1 = buildBeregningsresultatAndel(rbrPeriode_1, true, 1000, BigDecimal.valueOf(100L), virksomhet);
        var randelArbeidsgiver_1 = buildBeregningsresultatAndel(rbrPeriode_1, false, 1000, BigDecimal.valueOf(100L), virksomhet);

        var rferiepenger = buildBeregningsresultatFeriepenger();
        buildBeregningsresultatFeriepengerPrÅr(rferiepenger, randelBruker_1, 100L, baseDato);
        buildBeregningsresultatFeriepengerPrÅr(rferiepenger, randelArbeidsgiver_1, 101L, baseDato);

        var gruppertYtelse2 = mapper.fordelPåNøkler(rberegningsresultat, rferiepenger);
        var builder2 = getInputStandardBuilder(gruppertYtelse2).medTidligereOppdrag(mapTidligereOppdrag(List.of(oppdragskontroll)));

        var oppdragRevurdering = nyOppdragskontrollTjeneste.opprettOppdrag(builder2.build());

        var ny150L = oppdragRevurdering.orElseThrow().getOppdrag110Liste().stream()
            .map(Oppdrag110::getOppdragslinje150Liste)
            .flatMap(Collection::stream)
            .filter(o150 -> o150.getKodeKlassifik().gjelderFeriepenger())
            .toList();

        // Validere at eneste endring er opphør av FPATFER og innvilget FPADATFER
        assertThat(ny150L).hasSize(2);
        assertThat(ny150L.stream().filter(o150 -> KodeKlassifik.FERIEPENGER_BRUKER.equals(o150.getKodeKlassifik()) && KodeStatusLinje.OPPH.equals(o150.getKodeStatusLinje()))).hasSize(1);
        assertThat(ny150L.stream().filter(o150 -> KodeKlassifik.SVP_FERIEPENGER_BRUKER.equals(o150.getKodeKlassifik()) && KodeEndringLinje.NY.equals(o150.getKodeEndringLinje()))).hasSize(1);

    }
}
