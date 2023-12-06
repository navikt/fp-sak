package no.nav.foreldrepenger.økonomistøtte.oppdrag;

import static org.assertj.core.api.Assertions.assertThat;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Arrays;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import no.nav.foreldrepenger.behandlingslager.fagsak.FagsakYtelseType;
import no.nav.foreldrepenger.behandlingslager.økonomioppdrag.FamilieYtelseType;
import no.nav.foreldrepenger.behandlingslager.økonomioppdrag.koder.KodeKlassifik;
import no.nav.foreldrepenger.økonomistøtte.oppdrag.mapper.TilkjentYtelseMapper;

public class OppdragskontrollTjenesteImplSVPTest extends NyOppdragskontrollTjenesteTestBase {

    @Override
    @BeforeEach
    public void setUp() {
        super.setUp();
    }

    @Test
    void skal_sende_oppdrag_for_svangerskapspenger() {
        //Arrange
        var beregningsresultat = buildEmptyBeregningsresultatFP();

        var brPeriode_1 = buildBeregningsresultatPeriode(beregningsresultat, 1, 10);
        var andelBruker_1 = buildBeregningsresultatAndel(brPeriode_1, true, 1000, BigDecimal.valueOf(100L), virksomhet);
        var andelArbeidsgiver_1 = buildBeregningsresultatAndel(brPeriode_1, false, 1000, BigDecimal.valueOf(100L), virksomhet);

        var brPeriode_2 = buildBeregningsresultatPeriode(beregningsresultat, 11, 20);
        buildBeregningsresultatAndel(brPeriode_2, true, 1000, BigDecimal.valueOf(100L), virksomhet);
        buildBeregningsresultatAndel(brPeriode_2, false, 1000, BigDecimal.valueOf(100L), virksomhet);

        var feriepenger = buildBeregningsresultatFeriepenger(beregningsresultat);
        buildBeregningsresultatFeriepengerPrÅr(feriepenger, andelBruker_1, 10000L, LocalDate.of(2018, 12, 31));
        buildBeregningsresultatFeriepengerPrÅr(feriepenger, andelArbeidsgiver_1, 10000L, LocalDate.of(2018, 12, 31));

        var mapper = new TilkjentYtelseMapper(FamilieYtelseType.SVANGERSKAPSPENGER);
        var gruppertYtelse = mapper.fordelPåNøkler(beregningsresultat);

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
        assertThat(opp150List_Bruker).anySatisfy(opp150 ->
            assertThat(opp150.getKodeKlassifik()).isIn(Arrays.asList(
                KodeKlassifik.SVP_ARBEDISTAKER,
                KodeKlassifik.FERIEPENGER_BRUKER.getKode())
            ));
        //Oppdragslinje150 - Arbeidsgiver
        var opp150List_Arbeidsgiver = oppdrag110_Arbeidsgiver.get().getOppdragslinje150Liste();
        assertThat(opp150List_Arbeidsgiver).anySatisfy(opp150 ->
            assertThat(opp150.getKodeKlassifik()).isIn(Arrays.asList(
                KodeKlassifik.SVP_REFUSJON_AG,
                KodeKlassifik.SVP_FERIEPENGER_AG)
            ));
    }
}
