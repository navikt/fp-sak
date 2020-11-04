package no.nav.foreldrepenger.økonomi.økonomistøtte.dagytelse.svp;

import static org.assertj.core.api.Assertions.assertThat;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import no.nav.foreldrepenger.behandlingslager.behandling.Behandling;
import no.nav.foreldrepenger.behandlingslager.behandling.beregning.BeregningsresultatAndel;
import no.nav.foreldrepenger.behandlingslager.behandling.beregning.BeregningsresultatEntitet;
import no.nav.foreldrepenger.behandlingslager.behandling.beregning.BeregningsresultatFeriepenger;
import no.nav.foreldrepenger.behandlingslager.behandling.beregning.BeregningsresultatPeriode;
import no.nav.foreldrepenger.behandlingslager.økonomioppdrag.FamilieYtelseType;
import no.nav.foreldrepenger.behandlingslager.økonomioppdrag.Oppdrag110;
import no.nav.foreldrepenger.behandlingslager.økonomioppdrag.Oppdragskontroll;
import no.nav.foreldrepenger.behandlingslager.økonomioppdrag.Oppdragslinje150;
import no.nav.foreldrepenger.behandlingslager.økonomioppdrag.ØkonomiKodeKlassifik;
import no.nav.foreldrepenger.økonomi.økonomistøtte.dagytelse.fp.OppdragskontrollTjenesteTestBase;
import no.nav.foreldrepenger.økonomi.økonomistøtte.dagytelse.oppdrag110.KodeFagområdeTjeneste;

public class OppdragskontrollTjenesteImplTest extends OppdragskontrollTjenesteTestBase {

    @Override
    @BeforeEach
    public void setUp() {
        super.setUp();
    }

    @Test
    //TODO (KY): Tilkjent ytelse er ikke implementert for SVP ennå. Derfor tar i bruk testen TY for FP men det skal endres etterpå.
    public void skal_sende_oppdrag_for_svangerskapspenger() {
        //Arrange
        Behandling behandlingSVP = opprettOgLagreBehandling(FamilieYtelseType.SVANGERSKAPSPENGER);
        BeregningsresultatEntitet beregningsresultat = buildBeregningsresultatFP(Optional.empty());
        BeregningsresultatPeriode brPeriode_1 = buildBeregningsresultatPeriode(beregningsresultat, 1, 10);
        BeregningsresultatAndel andelBruker_1 = buildBeregningsresultatAndel(brPeriode_1, true, 1000, BigDecimal.valueOf(100L), virksomhet);
        BeregningsresultatAndel andelArbeidsgiver_1 = buildBeregningsresultatAndel(brPeriode_1, false, 1000, BigDecimal.valueOf(100L), virksomhet);
        BeregningsresultatPeriode brPeriode_2 = buildBeregningsresultatPeriode(beregningsresultat, 11, 20);
        buildBeregningsresultatAndel(brPeriode_2, true, 1000, BigDecimal.valueOf(100L), virksomhet);
        buildBeregningsresultatAndel(brPeriode_2, false, 1000, BigDecimal.valueOf(100L), virksomhet);
        BeregningsresultatFeriepenger feriepenger = buildBeregningsresultatFeriepenger(beregningsresultat);
        buildBeregningsresultatFeriepengerPrÅr(feriepenger, andelBruker_1, 10000L, LocalDate.of(2018, 12, 31));
        buildBeregningsresultatFeriepengerPrÅr(feriepenger, andelArbeidsgiver_1, 10000L, LocalDate.of(2018, 12, 31));
        beregningsresultatRepository.lagre(behandlingSVP, beregningsresultat);

        //Act
        Oppdragskontroll oppdrag = oppdragskontrollTjeneste.opprettOppdrag(behandlingSVP.getId(), 123L).get();

        //Assert
        assertThat(oppdrag).isNotNull();
        List<Oppdrag110> oppdrag110List = oppdrag.getOppdrag110Liste();
        assertThat(oppdrag110List).hasSize(2);
        //Oppdrag110 - Bruker
        Optional<Oppdrag110> oppdrag110_Bruker = oppdrag110List.stream()
            .filter(o110 -> KodeFagområdeTjeneste.forSvangerskapspenger().gjelderBruker(o110))
            .findFirst();
        assertThat(oppdrag110_Bruker).isPresent();
        //Oppdrag110 - Arbeidsgiver
        Optional<Oppdrag110> oppdrag110_Arbeidsgiver = oppdrag110List.stream()
            .filter(o110 -> !KodeFagområdeTjeneste.forSvangerskapspenger().gjelderBruker(o110))
            .findFirst();
        assertThat(oppdrag110_Arbeidsgiver).isPresent();
        //Oppdragslinje150 - Bruker
        List<Oppdragslinje150> opp150List_Bruker = oppdrag110_Bruker.get().getOppdragslinje150Liste();
        assertThat(opp150List_Bruker).anySatisfy(opp150 ->
            assertThat(opp150.getKodeKlassifik()).isIn(Arrays.asList(
                ØkonomiKodeKlassifik.FPSVATORD.getKodeKlassifik(),
                ØkonomiKodeKlassifik.FPATFER.getKodeKlassifik())
            ));
        //Oppdragslinje150 - Arbeidsgiver
        List<Oppdragslinje150> opp150List_Arbeidsgiver = oppdrag110_Arbeidsgiver.get().getOppdragslinje150Liste();
        assertThat(opp150List_Arbeidsgiver).anySatisfy(opp150 ->
            assertThat(opp150.getKodeKlassifik()).isIn(Arrays.asList(
                ØkonomiKodeKlassifik.FPSVREFAG_IOP.getKodeKlassifik(),
                ØkonomiKodeKlassifik.FPSVREFAGFER_IOP.getKodeKlassifik())
            ));
    }
}
