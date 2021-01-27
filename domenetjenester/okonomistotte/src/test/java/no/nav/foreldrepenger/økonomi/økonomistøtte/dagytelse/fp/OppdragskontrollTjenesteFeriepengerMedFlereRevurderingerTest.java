package no.nav.foreldrepenger.økonomi.økonomistøtte.dagytelse.fp;

import static no.nav.foreldrepenger.økonomi.økonomistøtte.dagytelse.fp.OppdragskontrollFeriepengerTestUtil.verifiserFeriepengeår;
import static no.nav.foreldrepenger.økonomi.økonomistøtte.dagytelse.fp.OppdragskontrollFeriepengerTestUtil.verifiserOpp150NårEndringGjelderEttFeriepengeår;
import static no.nav.foreldrepenger.økonomi.økonomistøtte.dagytelse.fp.OppdragskontrollFeriepengerTestUtil.verifiserOppdr150NårDetErEndringForToFeriepengeår;
import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import java.util.stream.Collectors;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import no.nav.foreldrepenger.behandlingslager.behandling.Behandling;
import no.nav.foreldrepenger.behandlingslager.økonomioppdrag.Oppdragskontroll;
import no.nav.foreldrepenger.behandlingslager.økonomioppdrag.Oppdragslinje150;
import no.nav.foreldrepenger.økonomi.økonomistøtte.OppdragMedPositivKvitteringTestUtil;

public class OppdragskontrollTjenesteFeriepengerMedFlereRevurderingerTest extends OppdragskontrollTjenesteTestBase {

    @Override
    @BeforeEach
    public void setUp() {
        setBrukNyOppdragTjeneste(false);
        super.setUp();
    }

    @Test
    public void skalIkkeLagesOppdragForFeriepengerPåAndreRevurderingNårDetBlirIngenEndringIÅrsbeløpForBeggeToFeriepengeår() {
        //Arrange
        //Førstegangsbehandling
        opprettBeregningsresultatOgFørstegangsoppdragForFeriepenger(true, 6000L, 7000L);
        //Revurdering #1
        Behandling revurdering_1 = oppsettBeregningsresultatFPRevurderingForFeriepenger(true, 6000L, 7000L);
        OppdragMedPositivKvitteringTestUtil.opprett(getOppdragTjeneste(), revurdering_1);
        //Revurdering #2
        Behandling revurdering_2 = oppsettBeregningsresultatFPRevurderingForFeriepenger(true, 6000L, 7000L, revurdering_1);

        //Act
        Oppdragskontroll oppdragRevurdering_2 = OppdragMedPositivKvitteringTestUtil.opprett(getOppdragTjeneste(), revurdering_2);

        //Assert
        assertThat(oppdragRevurdering_2.getOppdrag110Liste()).hasSize(2);
        List<Oppdragslinje150> opp150AndreRevurderingFeriepengerListe = getOppdragslinje150Feriepenger(oppdragRevurdering_2);
        assertThat(opp150AndreRevurderingFeriepengerListe).isEmpty();
    }

    @Test
    public void skalLagesOppdragForFeriepengerPåAndreRevurderingNårDetBlirEndringForBeggeToFeriepengeår() {
        //Arrange
        //Førstegangsbehandling
        Oppdragskontroll førstegangsoppdrag = opprettBeregningsresultatOgFørstegangsoppdragForFeriepenger(true, 6000L, 7000L);
        //Revurdering #1
        Behandling revurdering_1 = oppsettBeregningsresultatFPRevurderingForFeriepenger(true, 6000L, 7000L);
        OppdragMedPositivKvitteringTestUtil.opprett(getOppdragTjeneste(), revurdering_1);
        //Revurdering #2
        Behandling revurdering_2 = oppsettBeregningsresultatFPRevurderingForFeriepenger(true, 5999L, 7001L, revurdering_1);

        //Act
        Oppdragskontroll oppdragRevurdering_2 = OppdragMedPositivKvitteringTestUtil.opprett(getOppdragTjeneste(), revurdering_2);

        //Assert
        assertThat(oppdragRevurdering_2.getOppdrag110Liste()).hasSize(2);
        List<Oppdragslinje150> førstegangsopp150FeriepengerListe = getOppdragslinje150Feriepenger(førstegangsoppdrag);
        List<Oppdragslinje150> opp150AndreRevurderingFeriepengerListe = getOppdragslinje150Feriepenger(oppdragRevurdering_2);
        assertThat(opp150AndreRevurderingFeriepengerListe).hasSize(4);

        for (Oppdragslinje150 førstegangsopp150 : førstegangsopp150FeriepengerListe) {
            assertThat(opp150AndreRevurderingFeriepengerListe).anySatisfy(oppdragslinje150 ->
                assertThat(oppdragslinje150.getRefDelytelseId()).isEqualTo(førstegangsopp150.getDelytelseId()));
        }
        verifiserOppdr150NårDetErEndringForToFeriepengeår(førstegangsopp150FeriepengerListe, opp150AndreRevurderingFeriepengerListe);
        verifiserFeriepengeår(opp150AndreRevurderingFeriepengerListe);
    }

    @Test
    public void skalLagesOppdragForFeriepengerPåAndreRevurderingNårDetBlirEndringKunIBeregningForAndreFeriepengeåretOgFørsteRevurderingHarBeregningForBeggeToÅr() {
        //Arrange
        //Førstegangsbehandling
        Oppdragskontroll førstegangsoppdrag = opprettBeregningsresultatOgFørstegangsoppdragForFeriepenger(true, 6000L, 7000L);
        //Revurdering #1
        Behandling revurdering_1 = oppsettBeregningsresultatFPRevurderingForFeriepenger(true, 6000L, 7000L);
        OppdragMedPositivKvitteringTestUtil.opprett(getOppdragTjeneste(), revurdering_1);
        //Revurdering #2
        Behandling revurdering_2 = oppsettBeregningsresultatFPRevurderingForFeriepenger(true, 6000L, 7001L, revurdering_1);

        //Act
        Oppdragskontroll oppdragRevurdering_2 = OppdragMedPositivKvitteringTestUtil.opprett(getOppdragTjeneste(), revurdering_2);

        //Assert
        assertThat(oppdragRevurdering_2.getOppdrag110Liste()).hasSize(2);
        List<Oppdragslinje150> førstegangsopp150FeriepengerListe = getOppdragslinje150Feriepenger(førstegangsoppdrag);
        List<Oppdragslinje150> opp150AndreRevurderingFeriepengerListe = getOppdragslinje150Feriepenger(oppdragRevurdering_2);
        assertThat(opp150AndreRevurderingFeriepengerListe).hasSize(2);
        verifiserOpp150NårEndringGjelderEttFeriepengeår(førstegangsopp150FeriepengerListe, opp150AndreRevurderingFeriepengerListe, false);
    }

    @Test
    public void skalLagesOppdragForFeriepengerPåAndreRevurderingNårDetBlirEndringKunIBeregningForFørsteFeriepengeåretOgFørsteRevurderingHarBeregningForBeggeToÅr() {
        //Arrange
        //Førstegangsbehandling
        Oppdragskontroll førstegangsoppdrag = opprettBeregningsresultatOgFørstegangsoppdragForFeriepenger(true, 6000L, 7000L);
        //Revurdering #1
        Behandling revurdering_1 = oppsettBeregningsresultatFPRevurderingForFeriepenger(true, 6000L, 7000L);
        OppdragMedPositivKvitteringTestUtil.opprett(getOppdragTjeneste(), revurdering_1);
        //Revurdering #2
        Behandling revurdering_2 = oppsettBeregningsresultatFPRevurderingForFeriepenger(true, 5999L, 7000L, revurdering_1);

        //Act
        Oppdragskontroll oppdragRevurdering_2 = OppdragMedPositivKvitteringTestUtil.opprett(getOppdragTjeneste(), revurdering_2);

        //Assert
        assertThat(oppdragRevurdering_2.getOppdrag110Liste()).hasSize(2);
        List<Oppdragslinje150> førstegangsopp150FeriepengerListe = getOppdragslinje150Feriepenger(førstegangsoppdrag);
        List<Oppdragslinje150> opp150AndreRevurderingFeriepengerListe = getOppdragslinje150Feriepenger(oppdragRevurdering_2);
        assertThat(opp150AndreRevurderingFeriepengerListe).hasSize(2);
        verifiserOpp150NårEndringGjelderEttFeriepengeår(førstegangsopp150FeriepengerListe, opp150AndreRevurderingFeriepengerListe, true);
    }

    @Test
    public void skalLagesOppdragForFeriepengerPåAndreRevurderingNårBeregningEndrerSegForFørsteFeriepengeårOgFørsteRevurderingHarOpphørPåAndreÅr() {
        //Arrange
        //Førstegangsbehandling
        opprettBeregningsresultatOgFørstegangsoppdragForFeriepenger(true, 6000L, 7000L);
        //Revurdering #1
        Behandling revurdering_1 = oppsettBeregningsresultatFPRevurderingForFeriepenger(false, 7000L, 0L);
        Oppdragskontroll oppdragRevurdering_1 = OppdragMedPositivKvitteringTestUtil.opprett(getOppdragTjeneste(), revurdering_1);
        //Revurdering #2
        Behandling revurdering_2 = oppsettBeregningsresultatFPRevurderingForFeriepenger(true, 5999L, 7001L, revurdering_1);

        //Act
        Oppdragskontroll oppdragRevurdering_2 = OppdragMedPositivKvitteringTestUtil.opprett(getOppdragTjeneste(), revurdering_2);

        //Assert
        assertThat(oppdragRevurdering_2.getOppdrag110Liste()).hasSize(2);
        List<Oppdragslinje150> opp150FørsteRevurderingFeriepengerListe = getOppdragslinje150Feriepenger(oppdragRevurdering_1);
        List<Oppdragslinje150> opp150AndreRevurderingFeriepengerListe = getOppdragslinje150Feriepenger(oppdragRevurdering_2);
        assertThat(opp150AndreRevurderingFeriepengerListe).hasSize(4);
        for (Oppdragslinje150 førsterevurderingopp150 : opp150FørsteRevurderingFeriepengerListe) {
            if (!førsterevurderingopp150.gjelderOpphør()) {
                assertThat(opp150AndreRevurderingFeriepengerListe).anySatisfy(oppdragslinje150 ->
                    assertThat(oppdragslinje150.getRefDelytelseId()).isEqualTo(førsterevurderingopp150.getDelytelseId()));
            }
        }
        verifiserFeriepengeår(opp150AndreRevurderingFeriepengerListe);
    }

    @Test
    public void skalLagesOppdragForFeriepengerPåAndreRevurderingNårBeregningEndrerSegForFørsteFeriepengeårOgFørsteRevurderingHarOpphørPåFørsteÅr() {
        //Arrange
        //Førstegangsbehandling
        opprettBeregningsresultatOgFørstegangsoppdragForFeriepenger(true, 6000L, 7000L);
        //Revurdering #1
        Behandling revurdering_1 = oppsettBeregningsresultatFPRevurderingForFeriepenger(false, 0L, 7000L);
        Oppdragskontroll oppdragRevurdering_1 = OppdragMedPositivKvitteringTestUtil.opprett(getOppdragTjeneste(), revurdering_1);
        //Revurdering #2
        Behandling revurdering_2 = oppsettBeregningsresultatFPRevurderingForFeriepenger(true, 5999L, 6999L, revurdering_1);

        //Act
        Oppdragskontroll oppdragRevurdering_2 = OppdragMedPositivKvitteringTestUtil.opprett(getOppdragTjeneste(), revurdering_2);

        //Assert
        assertThat(oppdragRevurdering_2.getOppdrag110Liste()).hasSize(2);
        List<Oppdragslinje150> opp150FørsteRevurderingFeriepengerListe = getOppdragslinje150Feriepenger(oppdragRevurdering_1);
        List<Oppdragslinje150> opp150AndreRevurderingFeriepengerListe = getOppdragslinje150Feriepenger(oppdragRevurdering_2);
        assertThat(opp150AndreRevurderingFeriepengerListe).hasSize(4);
        for (Oppdragslinje150 opp150ForFørsteRevurdering : opp150FørsteRevurderingFeriepengerListe) {
            if (!opp150ForFørsteRevurdering.gjelderOpphør())
                assertThat(opp150AndreRevurderingFeriepengerListe).anySatisfy(oppdragslinje150 ->
                    assertThat(oppdragslinje150.getRefDelytelseId()).isEqualTo(opp150ForFørsteRevurdering.getDelytelseId()));
        }
        verifiserFeriepengeår(opp150AndreRevurderingFeriepengerListe);
    }

    @Test
    public void skalLagesOppdragForFeriepengerPåAndreRevurderingNårDetBlirNyFeriepengerBeregningForFørsteÅrOgIngenEndringForAndreÅr() {
        //Arrange
        //Førstegangsbehandling
        opprettBeregningsresultatOgFørstegangsoppdragForFeriepenger(true, 6000L, 7000L);
        //Revurdering #1
        Behandling revurdering_1 = oppsettBeregningsresultatFPRevurderingForFeriepenger(false, 0L, 7000L);
        OppdragMedPositivKvitteringTestUtil.opprett(getOppdragTjeneste(), revurdering_1);
        //Revurdering #2
        Behandling revurdering_2 = oppsettBeregningsresultatFPRevurderingForFeriepenger(true, 5999L, 7000L, revurdering_1);

        //Act
        Oppdragskontroll oppdragRevurdering_2 = OppdragMedPositivKvitteringTestUtil.opprett(getOppdragTjeneste(), revurdering_2);

        //Assert
        assertThat(oppdragRevurdering_2.getOppdrag110Liste()).hasSize(2);
        List<Oppdragslinje150> opp150AndreRevurderingFeriepengerListe = getOppdragslinje150Feriepenger(oppdragRevurdering_2);
        assertThat(opp150AndreRevurderingFeriepengerListe).hasSize(2);
        assertThat(opp150AndreRevurderingFeriepengerListe).allSatisfy(opp150 -> {
            assertThat(opp150.gjelderOpphør()).isFalse();
            assertThat(opp150.getDatoVedtakFom().getYear()).isEqualTo(FERIEPENGEÅR_LISTE.get(0));
            assertThat(opp150.getSats()).isEqualTo(5999L);
            assertThat(opp150.getRefDelytelseId()).isNull();
            assertThat(opp150.getRefFagsystemId()).isNull();
        });
    }

    @Test
    public void skalLagesOppdragForFeriepengerPåAndreRevurderingNårDetBlirNyFeriepengerBeregningForAndreÅrOgIngenEndringForFørsteÅr() {
        //Arrange
        //Førstegangsbehandling
        opprettBeregningsresultatOgFørstegangsoppdragForFeriepenger(true, 6000L, 7000L);
        //Revurdering #1
        Behandling revurdering_1 = oppsettBeregningsresultatFPRevurderingForFeriepenger(false, 7000L, 0L);
        OppdragMedPositivKvitteringTestUtil.opprett(getOppdragTjeneste(), revurdering_1);
        //Revurdering #2
        Behandling revurdering_2 = oppsettBeregningsresultatFPRevurderingForFeriepenger(true, 7000L, 5999L, revurdering_1);

        //Act
        Oppdragskontroll oppdragRevurdering_2 = OppdragMedPositivKvitteringTestUtil.opprett(getOppdragTjeneste(), revurdering_2);

        //Assert
        assertThat(oppdragRevurdering_2.getOppdrag110Liste()).hasSize(2);
        List<Oppdragslinje150> opp150AndreRevurderingFeriepengerListe = getOppdragslinje150Feriepenger(oppdragRevurdering_2);
        assertThat(opp150AndreRevurderingFeriepengerListe).hasSize(2);
        assertThat(opp150AndreRevurderingFeriepengerListe).allSatisfy(opp150 -> {
            assertThat(opp150.gjelderOpphør()).isFalse();
            assertThat(opp150.getDatoVedtakFom().getYear()).isEqualTo(FERIEPENGEÅR_LISTE.get(1));
            assertThat(opp150.getSats()).isEqualTo(5999L);
            assertThat(opp150.getRefDelytelseId()).isNull();
            assertThat(opp150.getRefFagsystemId()).isNull();
        });
    }

    @Test
    public void skalLagesOppdragForFeriepengerPåAndreRevurderingEtterFullstendingOpphørPåBeggeFeriepengeårPåFørsteRevurdering() {
        //Arrange
        //Førstegangsbehandling
        opprettBeregningsresultatOgFørstegangsoppdragForFeriepenger(true, 6000L, 7000L);
        //Revurdering #1
        Behandling revurdering_1 = oppsettBeregningsresultatFPRevurderingForFeriepenger(false, 0L, 0L);
        OppdragMedPositivKvitteringTestUtil.opprett(getOppdragTjeneste(), revurdering_1);
        //Revurdering #2
        Behandling revurdering_2 = oppsettBeregningsresultatFPRevurderingForFeriepenger(true, 6000L, 7000L, revurdering_1);

        //Act
        Oppdragskontroll oppdragRevurdering_2 = OppdragMedPositivKvitteringTestUtil.opprett(getOppdragTjeneste(), revurdering_2);

        //Assert
        assertThat(oppdragRevurdering_2.getOppdrag110Liste()).hasSize(2);
        List<Oppdragslinje150> opp150AndreRevurderingFeriepengerListe = getOppdragslinje150Feriepenger(oppdragRevurdering_2);
        assertThat(opp150AndreRevurderingFeriepengerListe).hasSize(4);
        assertThat(opp150AndreRevurderingFeriepengerListe).allSatisfy(opp150 -> {
            assertThat(opp150.gjelderOpphør()).isFalse();
            assertThat(opp150.getRefDelytelseId()).isNull();
            assertThat(opp150.getRefFagsystemId()).isNull();
        });
        verifiserFeriepengeår(opp150AndreRevurderingFeriepengerListe);
    }

    @Test
    public void skalLagesOppdragForFeriepengerPåAndreRevurderingSomSenderOpphørPåBeggeFeriepengeår() {
        //Arrange
        //Førstegangsbehandling
        Oppdragskontroll førstegangsoppdrag = opprettBeregningsresultatOgFørstegangsoppdragForFeriepenger(true, 6000L, 7000L);
        //Revurdering #1
        Behandling revurdering_1 = oppsettBeregningsresultatFPRevurderingForFeriepenger(true, 6000L, 7000L);
        OppdragMedPositivKvitteringTestUtil.opprett(getOppdragTjeneste(), revurdering_1);
        //Revurdering #2
        Behandling revurdering_2 = oppsettBeregningsresultatFPRevurderingForFeriepenger(false, 0L, 0L, revurdering_1);

        //Act
        Oppdragskontroll oppdragRevurdering_2 = OppdragMedPositivKvitteringTestUtil.opprett(getOppdragTjeneste(), revurdering_2);

        //Assert
        assertThat(oppdragRevurdering_2.getOppdrag110Liste()).hasSize(2);
        List<Oppdragslinje150> opp150ForFørsteOppdragFeriepengerListe = getOppdragslinje150Feriepenger(førstegangsoppdrag);
        List<Oppdragslinje150> opp150AndreRevurderingFeriepengerListe = getOppdragslinje150Feriepenger(oppdragRevurdering_2);
        assertThat(opp150AndreRevurderingFeriepengerListe).hasSize(4);
        assertThat(opp150AndreRevurderingFeriepengerListe).allSatisfy(opp150 ->
            assertThat(opp150.gjelderOpphør()).isTrue());
        for (Oppdragslinje150 opp150ForFørsteOppdrag : opp150ForFørsteOppdragFeriepengerListe) {
            assertThat(opp150AndreRevurderingFeriepengerListe).anySatisfy(oppdragslinje150 ->
                assertThat(oppdragslinje150.getDelytelseId()).isEqualTo(opp150ForFørsteOppdrag.getDelytelseId())
            );
            verifiserFeriepengeår(opp150AndreRevurderingFeriepengerListe);
        }
    }

    @Test
    public void skalLagesOppdragForFeriepengerPåAndreRevurderingSomSenderOpphørPåBeggeFeriepengeårEtterFørsteRevurderingHaddeEndringIÅrsbeløpForBeggeToÅr() {
        //Arrange
        //Førstegangsbehandling
        opprettBeregningsresultatOgFørstegangsoppdragForFeriepenger(true, 6000L, 7000L);
        //Revurdering #1
        Behandling revurdering_1 = oppsettBeregningsresultatFPRevurderingForFeriepenger(true, 6500L, 7500L);
        Oppdragskontroll oppdragRevurdering_1 = OppdragMedPositivKvitteringTestUtil.opprett(getOppdragTjeneste(), revurdering_1);
        //Revurdering #2
        Behandling revurdering_2 = oppsettBeregningsresultatFPRevurderingForFeriepenger(false, 0L, 0L, revurdering_1);

        //Act
        Oppdragskontroll oppdragRevurdering_2 = OppdragMedPositivKvitteringTestUtil.opprett(getOppdragTjeneste(), revurdering_2);

        //Assert
        assertThat(oppdragRevurdering_2.getOppdrag110Liste()).hasSize(2);
        List<Oppdragslinje150> opp150FørsteRevurderingFeriepengerListe = getOppdragslinje150Feriepenger(oppdragRevurdering_1);
        List<Oppdragslinje150> opp150AndreRevurderingFeriepengerListe = getOppdragslinje150Feriepenger(oppdragRevurdering_2);
        assertThat(opp150AndreRevurderingFeriepengerListe).hasSize(4);
        assertThat(opp150AndreRevurderingFeriepengerListe).allSatisfy(opp150 ->
            assertThat(opp150.gjelderOpphør()).isTrue());
        for (Oppdragslinje150 opp150FørsteRevurdering : opp150FørsteRevurderingFeriepengerListe) {
            assertThat(opp150AndreRevurderingFeriepengerListe).anySatisfy(oppdragslinje150 ->
                assertThat(oppdragslinje150.getDelytelseId()).isEqualTo(opp150FørsteRevurdering.getDelytelseId())
            );
            verifiserFeriepengeår(opp150AndreRevurderingFeriepengerListe);
        }
    }

    @Test
    public void skalLagesOppdragForFeriepengerPåAndreRevurderingSomSenderOpphørPåAndreFeriepengeår() {
        //Arrange
        //Førstegangsbehandling
        opprettBeregningsresultatOgFørstegangsoppdragForFeriepenger(true, 6000L, 7000L);
        //Revurdering #1
        Behandling revurdering_1 = oppsettBeregningsresultatFPRevurderingForFeriepenger(true, 6200L, 7500L);
        Oppdragskontroll oppdragRevurdering_1 = OppdragMedPositivKvitteringTestUtil.opprett(getOppdragTjeneste(), revurdering_1);
        //Revurdering #2
        Behandling revurdering_2 = oppsettBeregningsresultatFPRevurderingForFeriepenger(false, 6200L, 0L, revurdering_1);

        //Act
        Oppdragskontroll oppdragRevurdering_2 = OppdragMedPositivKvitteringTestUtil.opprett(getOppdragTjeneste(), revurdering_2);

        //Assert
        assertThat(oppdragRevurdering_2.getOppdrag110Liste()).hasSize(2);
        List<Oppdragslinje150> opp150FørsteRevurderingFeriepengerListe = getOppdragslinje150Feriepenger(oppdragRevurdering_1);
        List<Oppdragslinje150> opp150AndreRevurderingFeriepengerListe = getOppdragslinje150Feriepenger(oppdragRevurdering_2);
        assertThat(opp150AndreRevurderingFeriepengerListe).hasSize(2);
        assertThat(opp150AndreRevurderingFeriepengerListe).allSatisfy(opp150 -> {
            assertThat(opp150.gjelderOpphør()).isTrue();
            assertThat(opp150.getDatoVedtakFom().getYear()).isEqualTo(FERIEPENGEÅR_LISTE.get(1));
        });
        for (Oppdragslinje150 opp150ForAndreRevurdering : opp150AndreRevurderingFeriepengerListe) {
            assertThat(opp150FørsteRevurderingFeriepengerListe).anySatisfy(oppdragslinje150 ->
                assertThat(oppdragslinje150.getDelytelseId()).isEqualTo(opp150ForAndreRevurdering.getDelytelseId())
            );
        }
    }

    @Test
    public void skalLagesOppdragForFeriepengerPåAndreRevurderingSomSenderOpphørPåFørsteFeriepengeårNårDetErEndringForDetteFeriepengeåretIFørsteRevurdering() {
        //Arrange
        //Førstegangsbehandling
        opprettBeregningsresultatOgFørstegangsoppdragForFeriepenger(true, 6000L, 7000L);
        //Revurdering #1
        Behandling revurdering_1 = oppsettBeregningsresultatFPRevurderingForFeriepenger(true, 6200L, 7000L);
        Oppdragskontroll oppdragRevurdering_1 = OppdragMedPositivKvitteringTestUtil.opprett(getOppdragTjeneste(), revurdering_1);
        //Revurdering #2
        Behandling revurdering_2 = oppsettBeregningsresultatFPRevurderingForFeriepenger(false, 0L, 7000L, revurdering_1);

        //Act
        Oppdragskontroll oppdragRevurdering_2 = OppdragMedPositivKvitteringTestUtil.opprett(getOppdragTjeneste(), revurdering_2);

        //Assert
        assertThat(oppdragRevurdering_2.getOppdrag110Liste()).hasSize(2);
        List<Oppdragslinje150> opp150FørsteRevurderingFeriepengerListe = getOppdragslinje150Feriepenger(oppdragRevurdering_1);
        List<Oppdragslinje150> opp150AndreRevurderingFeriepengerListe = getOppdragslinje150Feriepenger(oppdragRevurdering_2);
        assertThat(opp150AndreRevurderingFeriepengerListe).hasSize(2);
        assertThat(opp150AndreRevurderingFeriepengerListe).allSatisfy(opp150 -> {
            assertThat(opp150.gjelderOpphør()).isTrue();
            assertThat(opp150.getDatoVedtakFom().getYear()).isEqualTo(FERIEPENGEÅR_LISTE.get(0));
        });
        for (Oppdragslinje150 opp150ForFørsteRevurdering : opp150FørsteRevurderingFeriepengerListe) {
            assertThat(opp150AndreRevurderingFeriepengerListe).anySatisfy(oppdragslinje150 ->
                assertThat(oppdragslinje150.getDelytelseId()).isEqualTo(opp150ForFørsteRevurdering.getDelytelseId())
            );
        }
    }

    @Test
    public void skalLagesOppdragForFeriepengerPåAndreRevurderingSomSenderOpphørPåAndreFeriepengeårEtterEndringIAndreÅrsbeløpIFørsteRevurdering() {
        //Arrange
        //Førstegangsbehandling
        opprettBeregningsresultatOgFørstegangsoppdragForFeriepenger(true, 6000L, 7000L);
        //Revurdering #1
        Behandling revurdering_1 = oppsettBeregningsresultatFPRevurderingForFeriepenger(true, 5999L, 7200L);
        Oppdragskontroll oppdragRevurdering_1 = OppdragMedPositivKvitteringTestUtil.opprett(getOppdragTjeneste(), revurdering_1);
        //Revurdering #2
        Behandling revurdering_2 = oppsettBeregningsresultatFPRevurderingForFeriepenger(false, 0L, 5999L, revurdering_1);

        //Act
        Oppdragskontroll oppdragRevurdering_2 = OppdragMedPositivKvitteringTestUtil.opprett(getOppdragTjeneste(), revurdering_2);

        //Assert
        assertThat(oppdragRevurdering_2.getOppdrag110Liste()).hasSize(2);
        List<Oppdragslinje150> opp150FørsteRevurderingFeriepengerListe = getOppdragslinje150Feriepenger(oppdragRevurdering_1);
        List<Oppdragslinje150> opp150AndreRevurderingFeriepengerListe = getOppdragslinje150Feriepenger(oppdragRevurdering_2);
        assertThat(opp150AndreRevurderingFeriepengerListe).hasSize(4);
        List<Oppdragslinje150> opp150FørsteRevurderingAndreÅrListe = opp150FørsteRevurderingFeriepengerListe.stream()
            .filter(opp150 -> opp150.getDatoVedtakFom().getYear() == FERIEPENGEÅR_LISTE.get(1))
            .collect(Collectors.toList());
        List<Oppdragslinje150> opp150AndreRevurderingFeriepengerOpphList = opp150AndreRevurderingFeriepengerListe.stream()
            .filter(Oppdragslinje150::gjelderOpphør)
            .collect(Collectors.toList());

        assertThat(opp150AndreRevurderingFeriepengerOpphList).hasSize(2);

        for (Oppdragslinje150 opp150AndreRevurderingFeriepenger : opp150AndreRevurderingFeriepengerListe) {
            if (opp150AndreRevurderingFeriepenger.gjelderOpphør()) {
                assertThat(opp150AndreRevurderingFeriepenger.getDatoVedtakFom().getYear()).isEqualTo(FERIEPENGEÅR_LISTE.get(0));
                assertThat(opp150FørsteRevurderingFeriepengerListe).anySatisfy(opp150 ->
                    assertThat(opp150.getDelytelseId()).isEqualTo(opp150AndreRevurderingFeriepenger.getDelytelseId()));
            } else {
                assertThat(opp150AndreRevurderingFeriepenger.getDatoVedtakFom().getYear()).isEqualTo(FERIEPENGEÅR_LISTE.get(1));
                assertThat(opp150FørsteRevurderingAndreÅrListe).anySatisfy(opp150 ->
                    assertThat(opp150.getDelytelseId()).isEqualTo(opp150AndreRevurderingFeriepenger.getRefDelytelseId()));
            }
        }
    }

    @Test
    public void skalLagesOppdragForFeriepengerPåAndreRevurderingSomSenderOpphørPåAndreFeriepengeårNårDetErEndringForDetteFeriepengeåretIFørsteRevurdering() {
        //Arrange
        //Førstegangsbehandling
        opprettBeregningsresultatOgFørstegangsoppdragForFeriepenger(true, 6000L, 7000L);
        //Revurdering #1
        Behandling revurdering_1 = oppsettBeregningsresultatFPRevurderingForFeriepenger(true, 7000L, 6200L);
        Oppdragskontroll oppdragRevurdering_1 = OppdragMedPositivKvitteringTestUtil.opprett(getOppdragTjeneste(), revurdering_1);
        //Revurdering #2
        Behandling revurdering_2 = oppsettBeregningsresultatFPRevurderingForFeriepenger(false, 6000L, 0L, revurdering_1);

        //Act
        Oppdragskontroll oppdragRevurdering_2 = OppdragMedPositivKvitteringTestUtil.opprett(getOppdragTjeneste(), revurdering_2);

        //Assert
        assertThat(oppdragRevurdering_2.getOppdrag110Liste()).hasSize(2);
        List<Oppdragslinje150> opp150FørsteRevurderingFeriepengerListe = getOppdragslinje150Feriepenger(oppdragRevurdering_1);
        List<Oppdragslinje150> opp150AndreRevurderingFeriepengerListe = getOppdragslinje150Feriepenger(oppdragRevurdering_2);
        assertThat(opp150AndreRevurderingFeriepengerListe).hasSize(4);
        List<Oppdragslinje150> opp150FørsteRevurderingFørsteÅrListe = opp150FørsteRevurderingFeriepengerListe.stream()
            .filter(opp150 -> opp150.getDatoVedtakFom().getYear() == FERIEPENGEÅR_LISTE.get(0))
            .collect(Collectors.toList());
        List<Oppdragslinje150> opp150AndreRevurderingFeriepengerOpphList = opp150AndreRevurderingFeriepengerListe.stream()
            .filter(Oppdragslinje150::gjelderOpphør)
            .collect(Collectors.toList());

        assertThat(opp150AndreRevurderingFeriepengerOpphList).hasSize(2);

        for (Oppdragslinje150 opp150AndreRevurderingFeriepenger : opp150AndreRevurderingFeriepengerListe) {
            if (opp150AndreRevurderingFeriepenger.gjelderOpphør()) {
                assertThat(opp150AndreRevurderingFeriepenger.getDatoVedtakFom().getYear()).isEqualTo(FERIEPENGEÅR_LISTE.get(1));
                assertThat(opp150FørsteRevurderingFeriepengerListe).anySatisfy(opp150 ->
                    assertThat(opp150.getDelytelseId()).isEqualTo(opp150AndreRevurderingFeriepenger.getDelytelseId()));
            } else {
                assertThat(opp150AndreRevurderingFeriepenger.getDatoVedtakFom().getYear()).isEqualTo(FERIEPENGEÅR_LISTE.get(0));
                assertThat(opp150FørsteRevurderingFørsteÅrListe).anySatisfy(opp150 ->
                    assertThat(opp150.getDelytelseId()).isEqualTo(opp150AndreRevurderingFeriepenger.getRefDelytelseId()));
            }
        }
    }

}
