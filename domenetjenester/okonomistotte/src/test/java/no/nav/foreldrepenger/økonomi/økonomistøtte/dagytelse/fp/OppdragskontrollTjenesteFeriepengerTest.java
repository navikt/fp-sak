package no.nav.foreldrepenger.økonomi.økonomistøtte.dagytelse.fp;

import static no.nav.foreldrepenger.økonomi.økonomistøtte.dagytelse.fp.OppdragskontrollFeriepengerTestUtil.getOppdr150ForFeriepengerForEnMottaker;
import static no.nav.foreldrepenger.økonomi.økonomistøtte.dagytelse.fp.OppdragskontrollFeriepengerTestUtil.verifiserFeriepengeår;
import static no.nav.foreldrepenger.økonomi.økonomistøtte.dagytelse.fp.OppdragskontrollFeriepengerTestUtil.verifiserOpp150NårEndringGjelderEttFeriepengeår;
import static no.nav.foreldrepenger.økonomi.økonomistøtte.dagytelse.fp.OppdragskontrollFeriepengerTestUtil.verifiserOpp150NårEttFPÅretOpphørerOgAndreIkkeEndrerSeg;
import static no.nav.foreldrepenger.økonomi.økonomistøtte.dagytelse.fp.OppdragskontrollFeriepengerTestUtil.verifiserOppdr150MedEttFeriepengeårKunIRevurdering;
import static no.nav.foreldrepenger.økonomi.økonomistøtte.dagytelse.fp.OppdragskontrollFeriepengerTestUtil.verifiserOppdr150NårDetErEndringForToFeriepengeår;
import static no.nav.foreldrepenger.økonomi.økonomistøtte.dagytelse.fp.OppdragskontrollFeriepengerTestUtil.verifiserOppdr150NårDetIkkeErFeriepengerIRevurdering;
import static no.nav.foreldrepenger.økonomi.økonomistøtte.dagytelse.fp.OppdragskontrollFeriepengerTestUtil.verifiserOppdr150NårEttFeriepengeårSkalOpphøre;
import static no.nav.foreldrepenger.økonomi.økonomistøtte.dagytelse.fp.OppdragskontrollFeriepengerTestUtil.verifiserRefDelytelseId;
import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import java.util.stream.Collectors;

import org.junit.Before;
import org.junit.Test;

import no.nav.foreldrepenger.behandlingslager.behandling.Behandling;
import no.nav.foreldrepenger.behandlingslager.økonomioppdrag.Oppdrag110;
import no.nav.foreldrepenger.behandlingslager.økonomioppdrag.Oppdragskontroll;
import no.nav.foreldrepenger.behandlingslager.økonomioppdrag.Oppdragslinje150;
import no.nav.foreldrepenger.behandlingslager.økonomioppdrag.ØkonomiKodeKlassifik;
import no.nav.foreldrepenger.økonomi.økonomistøtte.dagytelse.oppdrag110.KodeFagområdeTjeneste;

public class OppdragskontrollTjenesteFeriepengerTest extends OppdragskontrollTjenesteImplBaseTest {

    private static final Long PROSESS_TASK_ID_2 = 89L;

    @Override
    @Before
    public void setUp() {
        super.setUp();
    }

    /**
     * Forrige behandling: Har feriepenger for to feriepengeår;
     * Ny behandling: Har feriepenger for to feriepengeår;
     * Endring i årsbeløp: erBeløpForskjelligeForFPÅr1 = Nei, erBeløpForskjelligeForFPÅr2 = Nei
     */
    @Test
    public void skalIkkeSendeOppdragForFeriepengerNårDetIkkeErEndringIÅrsbeløp() {
        //Arrange
        //Førstegangsbehandling
        Oppdragskontroll førsteOppdrag = opprettBeregningsresultatOgFørstegangsoppdragForFeriepenger(true, 15000L, 10000L);
        //Revurdering
        Behandling revurdering = oppsettBeregningsresultatFPRevurderingForFeriepenger(true, 15000L, 10000L);

        //Act
        Oppdragskontroll oppdragRevurdering = oppdragskontrollTjeneste.opprettOppdrag(revurdering.getId(), PROSESS_TASK_ID_2).get();

        //Assert
        int førsteFeriepengeår = FERIEPENGEÅR_LISTE.get(0);
        int andreFeriepengeår = FERIEPENGEÅR_LISTE.get(1);
        //Original behandling
        assertThat(førsteOppdrag.getOppdrag110Liste()).hasSize(2);
        List<Oppdragslinje150> forrigeOpp150FeriepengerListe = getOppdragslinje150Feriepenger(førsteOppdrag);
        assertThat(forrigeOpp150FeriepengerListe).hasSize(4);
        List<Oppdragslinje150> forrigeopp150FeriepengerArbgvrList = getOppdr150ForFeriepengerForEnMottaker(forrigeOpp150FeriepengerListe, false);
        assertThat(forrigeopp150FeriepengerArbgvrList).hasSize(2);
        assertThat(forrigeopp150FeriepengerArbgvrList).anySatisfy(opp150 ->
            assertThat(opp150.getDatoVedtakFom().getYear()).isEqualTo(førsteFeriepengeår));
        assertThat(forrigeopp150FeriepengerArbgvrList).anySatisfy(opp150 ->
            assertThat(opp150.getDatoVedtakFom().getYear()).isEqualTo(andreFeriepengeår));
        //Revurdering
        assertThat(oppdragRevurdering.getOppdrag110Liste()).hasSize(2);
        List<Oppdragslinje150> opp150RevurderingFeriepengerListe = getOppdragslinje150Feriepenger(oppdragRevurdering);
        verifiserOppdr150NårDetErEndringForToFeriepengeår(forrigeOpp150FeriepengerListe, opp150RevurderingFeriepengerListe);
        assertThat(opp150RevurderingFeriepengerListe).isEmpty();
    }

    /**
     * Forrige behandling: Har feriepenger for to feriepengeår;
     * Ny behandling: Har feriepenger for to feriepengeår;
     * Endring i årsbeløp: erBeløpForskjelligeForFPÅr1 = Nei, erBeløpForskjelligeForFPÅr2 = Ja
     */
    @Test
    public void skalSendeOppdragKunForAndreFeriepengeårSomHaddeEndringAvÅrsbeløpIRevurdering() {
        //Arrange
        //Førstegangsbehandling
        Oppdragskontroll forrigeOppdrag = opprettBeregningsresultatOgFørstegangsoppdragForFeriepenger(true, 15000L, 10000L);
        //Revurdering
        Behandling revurdering = oppsettBeregningsresultatFPRevurderingForFeriepenger(true, 15000L, 11000L);

        //Act
        Oppdragskontroll oppdragRevurdering = oppdragskontrollTjeneste.opprettOppdrag(revurdering.getId(), PROSESS_TASK_ID_2).get();

        //Assert
        int førsteFeriepengeår = FERIEPENGEÅR_LISTE.get(0);
        int andreFeriepengeår = FERIEPENGEÅR_LISTE.get(1);
        //Original behandling
        assertThat(forrigeOppdrag.getOppdrag110Liste()).hasSize(2);
        List<Oppdragslinje150> forrigeOpp150FeriepengerListe = getOppdragslinje150Feriepenger(forrigeOppdrag);
        assertThat(forrigeOpp150FeriepengerListe).hasSize(4);
        //Bruker
        List<Oppdragslinje150> forrigeopp150FeriepengerBrukerList = getOppdr150ForFeriepengerForEnMottaker(forrigeOpp150FeriepengerListe, true);
        assertThat(forrigeopp150FeriepengerBrukerList).hasSize(2);
        assertThat(forrigeopp150FeriepengerBrukerList).anySatisfy(opp150 -> {
            assertThat(opp150.getDatoVedtakFom().getYear()).isEqualTo(førsteFeriepengeår);
            assertThat(opp150.getSats()).isEqualTo(15000L);
        });
        assertThat(forrigeopp150FeriepengerBrukerList).anySatisfy(opp150 -> {
            assertThat(opp150.getDatoVedtakFom().getYear()).isEqualTo(andreFeriepengeår);
            assertThat(opp150.getSats()).isEqualTo(10000L);
        });
        //Arbeidsgiver
        List<Oppdragslinje150> forrigeopp150FeriepengerArbgvrList = getOppdr150ForFeriepengerForEnMottaker(forrigeOpp150FeriepengerListe, false);
        assertThat(forrigeopp150FeriepengerArbgvrList).hasSize(2);
        assertThat(forrigeopp150FeriepengerArbgvrList).anySatisfy(opp150 -> {
            assertThat(opp150.getDatoVedtakFom().getYear()).isEqualTo(førsteFeriepengeår);
            assertThat(opp150.getSats()).isEqualTo(15000L);
        });
        assertThat(forrigeopp150FeriepengerArbgvrList).anySatisfy(opp150 -> {
            assertThat(opp150.getDatoVedtakFom().getYear()).isEqualTo(andreFeriepengeår);
            assertThat(opp150.getSats()).isEqualTo(10000L);
        });
        //Revurdering
        assertThat(oppdragRevurdering.getOppdrag110Liste()).hasSize(2);
        List<Oppdragslinje150> opp150RevurderingFeriepengerListe = getOppdragslinje150Feriepenger(oppdragRevurdering);
        assertThat(opp150RevurderingFeriepengerListe).hasSize(2);
        //Arbeidsgiver revurdering
        List<Oppdragslinje150> opp150RevurderingFeriepengerArbgvrList = getOppdr150ForFeriepengerForEnMottaker(opp150RevurderingFeriepengerListe, false);
        assertThat(opp150RevurderingFeriepengerArbgvrList).hasSize(1);
        assertThat(opp150RevurderingFeriepengerArbgvrList.get(0).getSats()).isEqualTo(11000L);
        //Bruker revurdering
        List<Oppdragslinje150> opp150RevurderingFeriepengerBrukerList = getOppdr150ForFeriepengerForEnMottaker(opp150RevurderingFeriepengerListe, true);
        assertThat(opp150RevurderingFeriepengerBrukerList).hasSize(1);
        assertThat(opp150RevurderingFeriepengerBrukerList.get(0).getSats()).isEqualTo(11000L);
        verifiserOpp150NårEndringGjelderEttFeriepengeår(forrigeOpp150FeriepengerListe, opp150RevurderingFeriepengerListe, false);
    }

    /**
     * Forrige behandling: Har feriepenger for to feriepengeår;
     * Ny behandling: Har feriepenger for to feriepengeår;
     * Endring i årsbeløp: erBeløpForskjelligeForFPÅr1 = Ja, erBeløpForskjelligeForFPÅr2 = Nei
     */
    @Test
    public void skalSendeOppdragKunForFørsteFeriepengeårSomHaddeEndringAvÅrsbeløpIRevurdering() {
        // Arrange
        //Førstegangsbehandling
        Oppdragskontroll forrigeOppdrag = opprettBeregningsresultatOgFørstegangsoppdragForFeriepenger(true, 9000L, 11000L);
        //Revurdering
        Behandling revurdering = oppsettBeregningsresultatFPRevurderingForFeriepenger(true, 7000L, 11000L);

        //Act
        Oppdragskontroll oppdragRevurdering = oppdragskontrollTjeneste.opprettOppdrag(revurdering.getId(), PROSESS_TASK_ID_2).get();

        //Assert
        //Original behandling
        assertThat(forrigeOppdrag.getOppdrag110Liste()).hasSize(2);
        List<Oppdragslinje150> forrigeOpp150FeriepengerListe = getOppdragslinje150Feriepenger(forrigeOppdrag);
        assertThat(forrigeOpp150FeriepengerListe).hasSize(4);

        //Revurdering
        assertThat(oppdragRevurdering.getOppdrag110Liste()).hasSize(2);
        List<Oppdragslinje150> opp150RevurderingFeriepengerListe = getOppdragslinje150Feriepenger(oppdragRevurdering);
        assertThat(opp150RevurderingFeriepengerListe).hasSize(2);
        //Arbeidsgiver revurdering
        List<Oppdragslinje150> opp150RevurderingFeriepengerArbgvrList = getOppdr150ForFeriepengerForEnMottaker(opp150RevurderingFeriepengerListe, false);
        assertThat(opp150RevurderingFeriepengerArbgvrList).hasSize(1);
        assertThat(opp150RevurderingFeriepengerArbgvrList.get(0).getSats()).isEqualTo(7000L);
        //Bruker revurdering
        List<Oppdragslinje150> opp150RevurderingFeriepengerBrukerList = getOppdr150ForFeriepengerForEnMottaker(opp150RevurderingFeriepengerListe, true);
        assertThat(opp150RevurderingFeriepengerBrukerList).hasSize(1);
        assertThat(opp150RevurderingFeriepengerBrukerList.get(0).getSats()).isEqualTo(7000L);
        verifiserOpp150NårEndringGjelderEttFeriepengeår(forrigeOpp150FeriepengerListe, opp150RevurderingFeriepengerListe, true);
    }

    /**
     * Forrige behandling: Har feriepenger for to feriepengeår;
     * Ny behandling: Har feriepenger for to feriepengeår;
     * Endring i årsbeløp: erBeløpForskjelligeForFPÅr1 = Ja, erBeløpForskjelligeForFPÅr2 = Ja
     */
    @Test
    public void skalSendeOppdragForAlleFeriepengeårNårDetBlirEndringAvÅrsbeløpForAlleFeriepengeårIRevurdering() {
        //Arrange
        //Førstegangsbehandling
        Oppdragskontroll forrigeOppdrag = opprettBeregningsresultatOgFørstegangsoppdragForFeriepenger(true, 6000L, 8000L);
        //Revurdering
        Behandling revurdering = oppsettBeregningsresultatFPRevurderingForFeriepenger(true, 9000L, 7000L);

        //Act
        Oppdragskontroll oppdragRevurdering = oppdragskontrollTjeneste.opprettOppdrag(revurdering.getId(), PROSESS_TASK_ID_2).get();

        //Assert
        //Original behandling
        assertThat(forrigeOppdrag.getOppdrag110Liste()).hasSize(2);
        List<Oppdragslinje150> forrigeOpp150FeriepengerListe = getOppdragslinje150Feriepenger(forrigeOppdrag);
        assertThat(forrigeOpp150FeriepengerListe).hasSize(4);
        //Revurdering
        assertThat(oppdragRevurdering.getOppdrag110Liste()).hasSize(2);
        List<Oppdragslinje150> opp150RevurderingFeriepengerListe = getOppdragslinje150Feriepenger(oppdragRevurdering);
        assertThat(opp150RevurderingFeriepengerListe).hasSize(4);
        //Bruker
        List<Oppdragslinje150> opp150RevurderingFeriepengerBrukerList = getOppdr150ForFeriepengerForEnMottaker(opp150RevurderingFeriepengerListe, true);
        assertThat(opp150RevurderingFeriepengerBrukerList).hasSize(2);
        assertThat(opp150RevurderingFeriepengerBrukerList).anySatisfy(opp150 ->
            assertThat(opp150.getSats()).isEqualTo(9000L));
        assertThat(opp150RevurderingFeriepengerBrukerList).anySatisfy(opp150 ->
            assertThat(opp150.getSats()).isEqualTo(7000L));
        //Arbeidsgiver
        List<Oppdragslinje150> opp150RevurderingFeriepengerArbgvrList = getOppdr150ForFeriepengerForEnMottaker(opp150RevurderingFeriepengerListe, false);
        assertThat(opp150RevurderingFeriepengerArbgvrList).hasSize(2);
        assertThat(opp150RevurderingFeriepengerArbgvrList).anySatisfy(opp150 ->
            assertThat(opp150.getSats()).isEqualTo(9000L));
        assertThat(opp150RevurderingFeriepengerArbgvrList).anySatisfy(opp150 ->
            assertThat(opp150.getSats()).isEqualTo(7000L));
        for (Oppdragslinje150 forrigeOpp150 : forrigeOpp150FeriepengerListe) {
            assertThat(opp150RevurderingFeriepengerListe).anySatisfy(oppdragslinje150 ->
                assertThat(oppdragslinje150.getRefDelytelseId()).isEqualTo(forrigeOpp150.getDelytelseId()));
        }
        verifiserOppdr150NårDetErEndringForToFeriepengeår(forrigeOpp150FeriepengerListe, opp150RevurderingFeriepengerListe);
        verifiserFeriepengeår(opp150RevurderingFeriepengerListe);
    }

    /**
     * Forrige behandling: Har ikke feriepenger;
     * Ny behandling: Har feriepenger for år1 - i_År.plusYears(1) og år2 - i_År.plusYears(2);
     * Endring i årsbeløp: erBeløpForskjelligeForFPÅr1 = Ja, erBeløpForskjelligeForFPÅr2 = Ja
     */
    @Test
    public void skalSendeOppdragForAlleFeriepengeårNårDetEksistererIngenFeriepengerIForrigeBehandling() {
        //Arrange
        //Førstegangsbehandling
        Oppdragskontroll forrigeOppdrag = opprettBeregningsresultatOgFørstegangsoppdragForFeriepenger(false, 0L, 0L);
        //Revurdering
        Behandling revurdering = oppsettBeregningsresultatFPRevurderingForFeriepenger(true, 11000L, 13000L);

        //Act
        Oppdragskontroll oppdragRevurdering = oppdragskontrollTjeneste.opprettOppdrag(revurdering.getId(), PROSESS_TASK_ID_2).get();

        //Assert
        //Original behandling
        assertThat(forrigeOppdrag.getOppdrag110Liste()).hasSize(2);
        List<Oppdragslinje150> forrigeOpp150FeriepengerListe = getOppdragslinje150Feriepenger(forrigeOppdrag);
        assertThat(forrigeOpp150FeriepengerListe).isEmpty();
        //Revurdering
        assertThat(oppdragRevurdering.getOppdrag110Liste()).hasSize(2);
        List<Oppdragslinje150> opp150RevurderingFeriepengerListe = getOppdragslinje150Feriepenger(oppdragRevurdering);
        assertThat(opp150RevurderingFeriepengerListe).hasSize(4);
        //Bruker
        List<Oppdragslinje150> opp150RevurderingFeriepengerBrukerList = getOppdr150ForFeriepengerForEnMottaker(opp150RevurderingFeriepengerListe, true);
        assertThat(opp150RevurderingFeriepengerBrukerList).hasSize(2);
        assertThat(opp150RevurderingFeriepengerBrukerList).anySatisfy(opp150 ->
            assertThat(opp150.getSats()).isEqualTo(11000L));
        assertThat(opp150RevurderingFeriepengerBrukerList).anySatisfy(opp150 ->
            assertThat(opp150.getSats()).isEqualTo(13000L));
        //Arbeidsgiver
        List<Oppdragslinje150> opp150RevurderingFeriepengerArbgvrList = getOppdr150ForFeriepengerForEnMottaker(opp150RevurderingFeriepengerListe, false);
        assertThat(opp150RevurderingFeriepengerArbgvrList).hasSize(2);
        assertThat(opp150RevurderingFeriepengerArbgvrList).anySatisfy(opp150 ->
            assertThat(opp150.getSats()).isEqualTo(11000L));
        assertThat(opp150RevurderingFeriepengerArbgvrList).anySatisfy(opp150 ->
            assertThat(opp150.getSats()).isEqualTo(13000L));
        verifiserOppdr150NårDetErEndringForToFeriepengeår(forrigeOpp150FeriepengerListe, opp150RevurderingFeriepengerListe);
        verifiserFeriepengeår(opp150RevurderingFeriepengerListe);
    }

    /**
     * Forrige behandling: Har ikke feriepenger;
     * Ny behandling: Har feriepenger for år2 - i_År.plusYears(2);
     * Endring i årsbeløp: erBeløpForskjelligeForFPÅr1 = Nei, erBeløpForskjelligeForFPÅr2 = Ja
     */
    @Test
    public void skalSendeOppdragKunForAndreFeriepengeårNårForrigeBehandlingIkkeHarFeriepengerOgRevurderingHarDetIAndreÅr() {
        //Arrange
        //Førstegangsbehandling
        Oppdragskontroll forrigeOppdrag = opprettBeregningsresultatOgFørstegangsoppdragForFeriepenger(false, 0L, 0L);
        //Revurdering
        Behandling revurdering = oppsettBeregningsresultatFPRevurderingForFeriepenger(false, 0L, 13000L);

        //Act
        Oppdragskontroll oppdragRevurdering = oppdragskontrollTjeneste.opprettOppdrag(revurdering.getId(), PROSESS_TASK_ID_2).get();

        //Assert
        //Original behandling
        assertThat(forrigeOppdrag.getOppdrag110Liste()).hasSize(2);
        List<Oppdragslinje150> opp150FeriepengerListe = getOppdragslinje150Feriepenger(forrigeOppdrag);
        assertThat(opp150FeriepengerListe).isEmpty();
        //Revurdering
        assertThat(oppdragRevurdering.getOppdrag110Liste()).hasSize(2);
        List<Oppdragslinje150> opp150RevurderingFeriepengerListe = getOppdragslinje150Feriepenger(oppdragRevurdering);
        assertThat(opp150RevurderingFeriepengerListe).hasSize(2);
        //Bruker
        List<Oppdragslinje150> opp150RevurderingFeriepengerBrukerList = getOppdr150ForFeriepengerForEnMottaker(opp150RevurderingFeriepengerListe, true);
        assertThat(opp150RevurderingFeriepengerBrukerList).hasSize(1);
        assertThat(opp150RevurderingFeriepengerBrukerList).anySatisfy(opp150 ->
            assertThat(opp150.getSats()).isEqualTo(13000L));
        //Arbeidsgiver
        List<Oppdragslinje150> opp150RevurderingFeriepengerArbgvrList = getOppdr150ForFeriepengerForEnMottaker(opp150RevurderingFeriepengerListe, false);
        assertThat(opp150RevurderingFeriepengerArbgvrList).hasSize(1);
        assertThat(opp150RevurderingFeriepengerArbgvrList).anySatisfy(opp150 ->
            assertThat(opp150.getSats()).isEqualTo(13000L));
        verifiserOppdr150MedEttFeriepengeårKunIRevurdering(opp150RevurderingFeriepengerListe, false);
    }

    /**
     * Forrige behandling: Har ikke feriepenger;
     * Ny behandling: Har feriepenger for år1 - i_År.plusYears(1);
     * Endring i årsbeløp: erBeløpForskjelligeForFPÅr1 = Ja, erBeløpForskjelligeForFPÅr2 = Nei
     */
    @Test
    public void skalSendeOppdragKunForFørsteFeriepengeårNårForrigeBehandlingIkkeHarFeriepengerOgRevurderingHarDetIFørsteÅr() {
        //Arrange
        //Førstegangsbehandling
        Oppdragskontroll forrigeOppdrag = opprettBeregningsresultatOgFørstegangsoppdragForFeriepenger(false, 0L, 0L);
        //Revurdering
        Behandling revurdering = oppsettBeregningsresultatFPRevurderingForFeriepenger(false, 8000L, 0L);

        //Act
        Oppdragskontroll oppdragRevurdering = oppdragskontrollTjeneste.opprettOppdrag(revurdering.getId(), PROSESS_TASK_ID_2).get();

        //Assert
        //Original behandling
        assertThat(forrigeOppdrag.getOppdrag110Liste()).hasSize(2);
        List<Oppdragslinje150> opp150FeriepengerListe = getOppdragslinje150Feriepenger(forrigeOppdrag);
        assertThat(opp150FeriepengerListe).isEmpty();
        //Revurdering
        assertThat(oppdragRevurdering.getOppdrag110Liste()).hasSize(2);
        List<Oppdragslinje150> opp150RevurderingFeriepengerListe = getOppdragslinje150Feriepenger(oppdragRevurdering);
        assertThat(opp150RevurderingFeriepengerListe).hasSize(2);
        //Bruker
        List<Oppdragslinje150> opp150RevurderingFeriepengerBrukerList = getOppdr150ForFeriepengerForEnMottaker(opp150RevurderingFeriepengerListe, true);
        assertThat(opp150RevurderingFeriepengerBrukerList).hasSize(1);
        assertThat(opp150RevurderingFeriepengerBrukerList).anySatisfy(opp150 ->
            assertThat(opp150.getSats()).isEqualTo(8000L));
        //Arbeidsgiver
        List<Oppdragslinje150> opp150RevurderingFeriepengerArbgvrList = getOppdr150ForFeriepengerForEnMottaker(opp150RevurderingFeriepengerListe, false);
        assertThat(opp150RevurderingFeriepengerArbgvrList).hasSize(1);
        assertThat(opp150RevurderingFeriepengerArbgvrList).anySatisfy(opp150 ->
            assertThat(opp150.getSats()).isEqualTo(8000L));
        verifiserOppdr150MedEttFeriepengeårKunIRevurdering(opp150RevurderingFeriepengerListe, true);
    }

    /**
     * Forrige behandling: Har feriepenger for år1 - i_År.plusYears(1);
     * Ny behandling: Har feriepenger for år1 - i_År.plusYears(1) og år2 - i_År.plusYears(1);
     * Endring i årsbeløp: erBeløpForskjelligeForFPÅr1 = Ja, erBeløpForskjelligeForFPÅr2 = Ja
     */
    @Test
    public void skalSendeOppdragForAlleFeriepengeårNårForrigeBehandlingIkkeHarFeriepengerForAndreÅrOgRevurderingHarDetForBeggeToÅr() {
        //Arrange
        //Førstegangsbehandling
        Oppdragskontroll forrigeOppdrag = opprettBeregningsresultatOgFørstegangsoppdragForFeriepenger(false, 6500L, 0L);
        //Revurdering
        Behandling revurdering = oppsettBeregningsresultatFPRevurderingForFeriepenger(true, 8000L, 7000L);

        //Act
        Oppdragskontroll oppdragRevurdering = oppdragskontrollTjeneste.opprettOppdrag(revurdering.getId(), PROSESS_TASK_ID_2).get();

        //Assert
        List<Oppdragslinje150> forrigeOpp150FeriepengerListe = getOppdragslinje150Feriepenger(forrigeOppdrag);
        List<Oppdragslinje150> opp150RevurderingFeriepengerListe = getOppdragslinje150Feriepenger(oppdragRevurdering);
        int førsteFeriepengeår = FERIEPENGEÅR_LISTE.get(0);
        //Original  behandling
        assertThat(forrigeOppdrag.getOppdrag110Liste()).hasSize(2);
        assertThat(forrigeOpp150FeriepengerListe).hasSize(2);
        //Arbeidsgiver forrige behandling
        List<Oppdragslinje150> forrigeopp150FeriepengerArbgvrList = getOppdr150ForFeriepengerForEnMottaker(forrigeOpp150FeriepengerListe, false);
        assertThat(forrigeopp150FeriepengerArbgvrList).hasSize(1);
        assertThat(forrigeopp150FeriepengerArbgvrList.get(0).getDatoVedtakFom().getYear()).isEqualTo(førsteFeriepengeår);
        assertThat(forrigeopp150FeriepengerArbgvrList.get(0).getSats()).isEqualTo(6500L);
        //Bruker forrige behandling
        List<Oppdragslinje150> forrigeopp150FeriepengerBrukerList = getOppdr150ForFeriepengerForEnMottaker(forrigeOpp150FeriepengerListe, true);
        assertThat(forrigeopp150FeriepengerBrukerList).hasSize(1);
        assertThat(forrigeopp150FeriepengerBrukerList.get(0).getDatoVedtakFom().getYear()).isEqualTo(førsteFeriepengeår);
        assertThat(forrigeopp150FeriepengerBrukerList.get(0).getSats()).isEqualTo(6500L);
        //Revurdering
        assertThat(oppdragRevurdering.getOppdrag110Liste()).hasSize(2);
        //Bruker
        List<Oppdragslinje150> opp150RevurderingFeriepengerBrukerList = getOppdr150ForFeriepengerForEnMottaker(opp150RevurderingFeriepengerListe, true);
        assertThat(opp150RevurderingFeriepengerBrukerList).hasSize(2);
        assertThat(opp150RevurderingFeriepengerBrukerList).anySatisfy(opp150 ->
            assertThat(opp150.getSats()).isEqualTo(8000L));
        assertThat(opp150RevurderingFeriepengerBrukerList).anySatisfy(opp150 ->
            assertThat(opp150.getSats()).isEqualTo(7000L));
        //Arbeidsgiver
        List<Oppdragslinje150> opp150RevurderingFeriepengerArbgvrList = getOppdr150ForFeriepengerForEnMottaker(opp150RevurderingFeriepengerListe, false);
        assertThat(opp150RevurderingFeriepengerArbgvrList).hasSize(2);
        assertThat(opp150RevurderingFeriepengerArbgvrList).anySatisfy(opp150 ->
            assertThat(opp150.getSats()).isEqualTo(8000L));
        assertThat(opp150RevurderingFeriepengerArbgvrList).anySatisfy(opp150 ->
            assertThat(opp150.getSats()).isEqualTo(7000L));
        verifiserRefDelytelseId(forrigeOpp150FeriepengerListe, opp150RevurderingFeriepengerListe, true);
        verifiserFeriepengeår(opp150RevurderingFeriepengerListe);
        assertThat(opp150RevurderingFeriepengerListe).anySatisfy(oppdragslinje150 -> {
            assertThat(oppdragslinje150.getRefDelytelseId()).isNull();
            assertThat(oppdragslinje150.getRefDelytelseId()).isNull();
        });
    }

    /**
     * Forrige behandling: Har feriepenger for år1 - i_År.plusYears(1);
     * Ny behandling: Har feriepenger for år1 - i_År.plusYears(1) og år2 - i_År.plusYears(2);
     * Endring i årsbeløp: erBeløpForskjelligeForFPÅr1 = Nei, erBeløpForskjelligeForFPÅr2 = Ja
     */
    @Test
    public void skalSendeOppdragKunForAndreFeriepengeårNårForrigeBehandlingHarFeriepengerForFørsteÅrOgRevurderingHarDetForBeggeToÅrUtenEndringForFørsteÅr() {
        //Arrange
        //Førstegangsbehandling
        Oppdragskontroll forrigeOppdrag = opprettBeregningsresultatOgFørstegangsoppdragForFeriepenger(false, 6500L, 0L);
        //Revurdering
        Behandling revurdering = oppsettBeregningsresultatFPRevurderingForFeriepenger(true, 6500L, 7000L);

        //Act
        Oppdragskontroll oppdragRevurdering = oppdragskontrollTjeneste.opprettOppdrag(revurdering.getId(), PROSESS_TASK_ID_2).get();

        //Assert
        List<Oppdragslinje150> forrigeOpp150FeriepengerListe = getOppdragslinje150Feriepenger(forrigeOppdrag);
        List<Oppdragslinje150> opp150RevurderingFeriepengerListe = getOppdragslinje150Feriepenger(oppdragRevurdering);
        int andreFeriepengeår = FERIEPENGEÅR_LISTE.get(1);
        //Original  behandling
        assertThat(forrigeOppdrag.getOppdrag110Liste()).hasSize(2);
        assertThat(forrigeOpp150FeriepengerListe).hasSize(2);

        //Revurdering
        assertThat(oppdragRevurdering.getOppdrag110Liste()).hasSize(2);
        assertThat(opp150RevurderingFeriepengerListe).hasSize(2);
        //Arbeidsgiver revurdering
        List<Oppdragslinje150> opp150RevurderingFeriepengerArbgvrList = getOppdr150ForFeriepengerForEnMottaker(opp150RevurderingFeriepengerListe, false);
        assertThat(opp150RevurderingFeriepengerArbgvrList).hasSize(1);
        assertThat(opp150RevurderingFeriepengerArbgvrList.get(0).getDatoVedtakFom().getYear()).isEqualTo(andreFeriepengeår);
        assertThat(opp150RevurderingFeriepengerArbgvrList.get(0).getSats()).isEqualTo(7000L);
        //Bruker revurdering
        List<Oppdragslinje150> opp150RevurderingFeriepengerBrukerList = getOppdr150ForFeriepengerForEnMottaker(opp150RevurderingFeriepengerListe, true);
        assertThat(opp150RevurderingFeriepengerBrukerList).hasSize(1);
        assertThat(opp150RevurderingFeriepengerBrukerList.get(0).getDatoVedtakFom().getYear()).isEqualTo(andreFeriepengeår);
        assertThat(opp150RevurderingFeriepengerBrukerList.get(0).getSats()).isEqualTo(7000L);
    }

    /**
     * Forrige behandling: Har feriepenger for år2 - i_År.plusYears(2);
     * Ny behandling: Har feriepenger for år1 - i_År.plusYears(1) og år2 - i_År.plusYears(2);
     * Endring i årsbeløp: erBeløpForskjelligeForFPÅr1 = Ja, erBeløpForskjelligeForFPÅr2 = Ja
     */
    @Test
    public void skalSendeOppdragForAlleFeriepengeårNårForrigeBehandlingIkkeHarFeriepengerForFørsteÅrOgRevurderingHarDetForBeggeToÅr() {
        //Arrange
        //Førstegangsbehandling
        Oppdragskontroll forrigeOppdrag = opprettBeregningsresultatOgFørstegangsoppdragForFeriepenger(false, 0L, 7500L);
        //Revurdering
        Behandling revurdering = oppsettBeregningsresultatFPRevurderingForFeriepenger(true, 9500L, 7499L);

        //Act
        Oppdragskontroll oppdragRevurdering = oppdragskontrollTjeneste.opprettOppdrag(revurdering.getId(), PROSESS_TASK_ID_2).get();

        //Assert
        int andreFeriepengeår = FERIEPENGEÅR_LISTE.get(1);
        List<Oppdragslinje150> forrigeOpp150FeriepengerListe = getOppdragslinje150Feriepenger(forrigeOppdrag);
        List<Oppdragslinje150> opp150RevurderingFeriepengerListe = getOppdragslinje150Feriepenger(oppdragRevurdering);
        //Original  behandling
        assertThat(forrigeOppdrag.getOppdrag110Liste()).hasSize(2);
        assertThat(forrigeOpp150FeriepengerListe).hasSize(2);
        //Arbeidsgiver forrige behandling
        List<Oppdragslinje150> forrigeopp150FeriepengerArbgvrList = getOppdr150ForFeriepengerForEnMottaker(forrigeOpp150FeriepengerListe, false);
        assertThat(forrigeopp150FeriepengerArbgvrList).hasSize(1);
        assertThat(forrigeopp150FeriepengerArbgvrList.get(0).getDatoVedtakFom().getYear()).isEqualTo(andreFeriepengeår);
        assertThat(forrigeopp150FeriepengerArbgvrList.get(0).getSats()).isEqualTo(7500L);
        //Bruker forrige behandling
        List<Oppdragslinje150> forrigeopp150FeriepengerBrukerList = getOppdr150ForFeriepengerForEnMottaker(forrigeOpp150FeriepengerListe, true);
        assertThat(forrigeopp150FeriepengerBrukerList).hasSize(1);
        assertThat(forrigeopp150FeriepengerBrukerList.get(0).getDatoVedtakFom().getYear()).isEqualTo(andreFeriepengeår);
        assertThat(forrigeopp150FeriepengerBrukerList.get(0).getSats()).isEqualTo(7500L);
        List<Oppdragslinje150> opp150FeriepengerListe = getOppdragslinje150Feriepenger(forrigeOppdrag);
        //Revurdering
        assertThat(oppdragRevurdering.getOppdrag110Liste()).hasSize(2);
        //Bruker
        List<Oppdragslinje150> opp150RevurderingFeriepengerBrukerList = getOppdr150ForFeriepengerForEnMottaker(opp150RevurderingFeriepengerListe, true);
        assertThat(opp150RevurderingFeriepengerBrukerList).hasSize(2);
        assertThat(opp150RevurderingFeriepengerBrukerList).anySatisfy(opp150 ->
            assertThat(opp150.getSats()).isEqualTo(9500L));
        assertThat(opp150RevurderingFeriepengerBrukerList).anySatisfy(opp150 ->
            assertThat(opp150.getSats()).isEqualTo(7499L));
        //Arbeidsgiver
        List<Oppdragslinje150> opp150RevurderingFeriepengerArbgvrList = getOppdr150ForFeriepengerForEnMottaker(opp150RevurderingFeriepengerListe, false);
        assertThat(opp150RevurderingFeriepengerArbgvrList).hasSize(2);
        assertThat(opp150RevurderingFeriepengerArbgvrList).anySatisfy(opp150 ->
            assertThat(opp150.getSats()).isEqualTo(9500L));
        assertThat(opp150RevurderingFeriepengerArbgvrList).anySatisfy(opp150 ->
            assertThat(opp150.getSats()).isEqualTo(7499L));
        verifiserRefDelytelseId(opp150FeriepengerListe, opp150RevurderingFeriepengerListe, false);
        verifiserFeriepengeår(opp150RevurderingFeriepengerListe);
        assertThat(opp150RevurderingFeriepengerListe).anySatisfy(oppdragslinje150 -> {
            assertThat(oppdragslinje150.getRefDelytelseId()).isNull();
            assertThat(oppdragslinje150.getRefDelytelseId()).isNull();
        });
    }

    /**
     * Forrige behandling: Har feriepenger for år2 - i_År.plusYears(2);
     * Ny behandling: Har feriepenger for år1 - i_År.plusYears(1) og år2 - i_År.plusYears(2);
     * Endring i årsbeløp: erBeløpForskjelligeForFPÅr1 = Ja, erBeløpForskjelligeForFPÅr2 = Nei
     */
    @Test
    public void skalSendeOppdragKunForFørsteFeriepengeårNårForrigeBehandlingHarFeriepengerForFørsteÅrOgRevurderingHarDetForBeggeToÅrUtenEndringForAndreÅr() {
        //Arrange
        //Førstegangsbehandling
        Oppdragskontroll forrigeOppdrag = opprettBeregningsresultatOgFørstegangsoppdragForFeriepenger(false, 0L, 7000L);
        //Revurdering
        Behandling revurdering = oppsettBeregningsresultatFPRevurderingForFeriepenger(true, 6500L, 7000L);

        //Act
        Oppdragskontroll oppdragRevurdering = oppdragskontrollTjeneste.opprettOppdrag(revurdering.getId(), PROSESS_TASK_ID_2).get();

        //Assert
        int førsteFeriepengeår = FERIEPENGEÅR_LISTE.get(0);
        List<Oppdragslinje150> forrigeOpp150FeriepengerListe = getOppdragslinje150Feriepenger(forrigeOppdrag);
        List<Oppdragslinje150> opp150RevurderingFeriepengerListe = getOppdragslinje150Feriepenger(oppdragRevurdering);
        //Original  behandling
        assertThat(forrigeOppdrag.getOppdrag110Liste()).hasSize(2);
        assertThat(forrigeOpp150FeriepengerListe).hasSize(2);

        //Revurdering
        assertThat(oppdragRevurdering.getOppdrag110Liste()).hasSize(2);
        assertThat(opp150RevurderingFeriepengerListe).hasSize(2);
        //Arbeidsgiver revurdering
        List<Oppdragslinje150> opp150RevurderingFeriepengerArbgvrList = getOppdr150ForFeriepengerForEnMottaker(opp150RevurderingFeriepengerListe, false);
        assertThat(opp150RevurderingFeriepengerArbgvrList).hasSize(1);
        assertThat(opp150RevurderingFeriepengerArbgvrList.get(0).getDatoVedtakFom().getYear()).isEqualTo(førsteFeriepengeår);
        assertThat(opp150RevurderingFeriepengerArbgvrList.get(0).getSats()).isEqualTo(6500L);
        //Bruker revurdering
        List<Oppdragslinje150> opp150RevurderingFeriepengerBrukerList = getOppdr150ForFeriepengerForEnMottaker(opp150RevurderingFeriepengerListe, true);
        assertThat(opp150RevurderingFeriepengerBrukerList).hasSize(1);
        assertThat(opp150RevurderingFeriepengerBrukerList.get(0).getDatoVedtakFom().getYear()).isEqualTo(førsteFeriepengeår);
        assertThat(opp150RevurderingFeriepengerBrukerList.get(0).getSats()).isEqualTo(6500L);
    }

    /**
     * Forrige behandling: Har feriepenger for år1 - i_År.plusYears(1) og år2 - i_År.plusYears(2);
     * Ny behandling: Har feriepenger for år1 - i_År.plusYears(1);
     * Endring i årsbeløp: erBeløpForskjelligeForFPÅr1 = Ja, erBeløpForskjelligeForFPÅr2 = Ja
     */
    @Test
    public void skalSendeEnOppdragForOpphørOgEnForEndringNårFørsteFeriepengeårEndrerSegOgAndreFeriepengeårOpphørerIRevurdering() {
        //Arrange
        //Førstegangsbehandling
        Oppdragskontroll forrigeOppdrag = opprettBeregningsresultatOgFørstegangsoppdragForFeriepenger(true, 12000L, 10000L);
        //Revurdering
        Behandling revurdering = oppsettBeregningsresultatFPRevurderingForFeriepenger(true, 13000L, 0L);

        //Act
        Oppdragskontroll oppdragRevurdering = oppdragskontrollTjeneste.opprettOppdrag(revurdering.getId(), PROSESS_TASK_ID_2).get();

        //Assert
        List<Oppdragslinje150> forrigeOpp150FeriepengerListe = getOppdragslinje150Feriepenger(forrigeOppdrag);
        List<Oppdragslinje150> opp150RevurderingFeriepengerListe = getOppdragslinje150Feriepenger(oppdragRevurdering);
        int andreFeriepengeår = FERIEPENGEÅR_LISTE.get(1);
        //Original behandling
        assertThat(forrigeOppdrag.getOppdrag110Liste()).hasSize(2);
        assertThat(forrigeOpp150FeriepengerListe).hasSize(4);

        //Revurdering
        assertThat(opp150RevurderingFeriepengerListe).hasSize(4);
        //Arbeidsgiver revurdering
        List<Oppdragslinje150> opp150RevurderingFeriepengerArbgvrList = getOppdr150ForFeriepengerForEnMottaker(opp150RevurderingFeriepengerListe, false);
        List<Oppdragslinje150> opp150RevurdArbgvrUtenOpph = opp150RevurderingFeriepengerArbgvrList.stream()
            .filter(opp150 -> !opp150.gjelderOpphør()).collect(Collectors.toList());
        assertThat(opp150RevurdArbgvrUtenOpph).hasSize(1);
        assertThat(opp150RevurdArbgvrUtenOpph.get(0).getSats()).isEqualTo(13000L);
        //Bruker revurdering
        assertThat(oppdragRevurdering.getOppdrag110Liste()).hasSize(2);
        List<Oppdragslinje150> opp150RevurderingFeriepengerBrukerList = getOppdr150ForFeriepengerForEnMottaker(opp150RevurderingFeriepengerListe, true);
        List<Oppdragslinje150> opp150RevurdBrukerUtenOpph = opp150RevurderingFeriepengerBrukerList.stream()
            .filter(opp150 -> !opp150.gjelderOpphør()).collect(Collectors.toList());
        assertThat(opp150RevurdBrukerUtenOpph).hasSize(1);
        assertThat(opp150RevurdBrukerUtenOpph.get(0).getSats()).isEqualTo(13000L);
        //Opphør
        List<Oppdragslinje150> opp150ForOpphListe = opp150RevurderingFeriepengerListe.stream()
            .filter(Oppdragslinje150::gjelderOpphør).collect(Collectors.toList());
        assertThat(opp150ForOpphListe).hasSize(2);
        assertThat(opp150ForOpphListe).allSatisfy(opp150 ->
            assertThat(opp150.getDatoVedtakFom().getYear()).isEqualTo(andreFeriepengeår));
        verifiserFeriepengeår(opp150RevurderingFeriepengerListe);
        verifiserOppdr150NårEttFeriepengeårSkalOpphøre(forrigeOpp150FeriepengerListe, opp150RevurderingFeriepengerListe, false);
    }

    /**
     * Forrige behandling: Har feriepenger for år1 - i_År.plusYears(1) og år2 - i_År.plusYears(2);
     * Ny behandling: Har feriepenger for år1 - i_År.plusYears(1);
     * Endring i årsbeløp: erBeløpForskjelligeForFPÅr1 = Nei, erBeløpForskjelligeForFPÅr2 = Ja
     */
    @Test
    public void skalSendeKunEnOppdragForOpphørNårFørsteFeriepengeårIkkeEndrerSegOgAndreFeriepengeårOpphørerIRevurdering() {
        //Arrange
        //Førstegangsbehandling
        Oppdragskontroll forrigeOppdrag = opprettBeregningsresultatOgFørstegangsoppdragForFeriepenger(true, 11000L, 10000L);
        //Revurdering
        Behandling revurdering = oppsettBeregningsresultatFPRevurderingForFeriepenger(true, 11000L, 0L);

        //Act
        Oppdragskontroll oppdragRevurdering = oppdragskontrollTjeneste.opprettOppdrag(revurdering.getId(), PROSESS_TASK_ID_2).get();

        //Assert
        List<Oppdragslinje150> forrigeOpp150FeriepengerListe = getOppdragslinje150Feriepenger(forrigeOppdrag);
        List<Oppdragslinje150> opp150RevurderingFeriepengerListe = getOppdragslinje150Feriepenger(oppdragRevurdering);
        //Original behandling
        assertThat(forrigeOppdrag.getOppdrag110Liste()).hasSize(2);
        assertThat(forrigeOpp150FeriepengerListe).hasSize(4);

        //Revurdering
        assertThat(oppdragRevurdering.getOppdrag110Liste()).hasSize(2);
        assertThat(opp150RevurderingFeriepengerListe).hasSize(2);
        //Opphør
        List<Oppdragslinje150> opp150ForOpphListe = opp150RevurderingFeriepengerListe.stream()
            .filter(Oppdragslinje150::gjelderOpphør).collect(Collectors.toList());
        assertThat(opp150ForOpphListe).hasSize(2);
        verifiserOpp150NårEttFPÅretOpphørerOgAndreIkkeEndrerSeg(opp150RevurderingFeriepengerListe, opp150RevurderingFeriepengerListe, false);
    }

    /**
     * Forrige behandling: Har feriepenger for år1 - i_År.plusYears(1) og år2 - i_År.plusYears(2);
     * Ny behandling: Har feriepenger for år1 - i_År.plusYears(1);
     * Endring i årsbeløp: erBeløpForskjelligeForFPÅr1 = Ja, erBeløpForskjelligeForFPÅr2 = Ja
     */
    @Test
    public void skalSendeEnOppdragForOpphørOgEnForEndringNårAndreFeriepengeårEndrerSegOgFørsteFeriepengeårOpphørerIRevurdering() {
        //Arrange
        //Førstegangsbehandling
        Oppdragskontroll forrigeOppdrag = opprettBeregningsresultatOgFørstegangsoppdragForFeriepenger(true, 12000L, 10000L);
        //Revurdering
        Behandling revurdering = oppsettBeregningsresultatFPRevurderingForFeriepenger(true, 0L, 11000L);

        //Act
        Oppdragskontroll oppdragRevurdering = oppdragskontrollTjeneste.opprettOppdrag(revurdering.getId(), PROSESS_TASK_ID_2).get();

        //Assert
        List<Oppdragslinje150> forrigeOpp150FeriepengerListe = getOppdragslinje150Feriepenger(forrigeOppdrag);
        assertThat(forrigeOppdrag.getOppdrag110Liste()).hasSize(2);
        List<Oppdragslinje150> opp150RevurderingFeriepengerListe = getOppdragslinje150Feriepenger(oppdragRevurdering);
        int førsteFeriepengeår = FERIEPENGEÅR_LISTE.get(0);

        //Revurdering
        assertThat(oppdragRevurdering.getOppdrag110Liste()).hasSize(2);
        //Arbeidsgiver revurdering
        List<Oppdragslinje150> opp150RevurderingFeriepengerArbgvrList = getOppdr150ForFeriepengerForEnMottaker(opp150RevurderingFeriepengerListe, false);
        List<Oppdragslinje150> opp150RevurdArbgvrUtenOpph = opp150RevurderingFeriepengerArbgvrList.stream()
            .filter(opp150 -> !opp150.gjelderOpphør()).collect(Collectors.toList());
        assertThat(opp150RevurdArbgvrUtenOpph).hasSize(1);
        assertThat(opp150RevurdArbgvrUtenOpph.get(0).getSats()).isEqualTo(11000L);
        //Bruker revurdering
        List<Oppdragslinje150> opp150RevurderingFeriepengerBrukerList = getOppdr150ForFeriepengerForEnMottaker(opp150RevurderingFeriepengerListe, true);
        List<Oppdragslinje150> opp150RevurdBrukerUtenOpph = opp150RevurderingFeriepengerBrukerList.stream()
            .filter(opp150 -> !opp150.gjelderOpphør()).collect(Collectors.toList());
        assertThat(opp150RevurdBrukerUtenOpph).hasSize(1);
        assertThat(opp150RevurdBrukerUtenOpph.get(0).getSats()).isEqualTo(11000L);
        //Opphør
        List<Oppdragslinje150> opp150ForOpphListe = opp150RevurderingFeriepengerListe.stream()
            .filter(Oppdragslinje150::gjelderOpphør).collect(Collectors.toList());
        assertThat(opp150ForOpphListe).hasSize(2);
        assertThat(opp150ForOpphListe).allSatisfy(opp150 ->
            assertThat(opp150.getDatoVedtakFom().getYear()).isEqualTo(førsteFeriepengeår));
        verifiserFeriepengeår(opp150RevurderingFeriepengerListe);
        verifiserOppdr150NårEttFeriepengeårSkalOpphøre(forrigeOpp150FeriepengerListe, opp150RevurderingFeriepengerListe, true);
    }

    /**
     * Forrige behandling: Har feriepenger for år1 - i_År.plusYears(1) og år2 - i_År.plusYears(2);
     * Ny behandling: Har feriepenger for år1 - i_År.plusYears(1);
     * Endring i årsbeløp: erBeløpForskjelligeForFPÅr1 = Nei, erBeløpForskjelligeForFPÅr2 = Ja
     */
    @Test
    public void skalSendeKunEnOppdragForOpphørNårAndreFeriepengeårIkkeEndrerSegOgFørsteFeriepengeårOpphørerIRevurdering() {
        //Arrange
        //Førstegangsbehandling
        Oppdragskontroll forrigeOppdrag = opprettBeregningsresultatOgFørstegangsoppdragForFeriepenger(true, 11000L, 10000L);

        //Revurdering
        Behandling revurdering = oppsettBeregningsresultatFPRevurderingForFeriepenger(true, 0L, 10000L);

        //Act
        Oppdragskontroll oppdragRevurdering = oppdragskontrollTjeneste.opprettOppdrag(revurdering.getId(), PROSESS_TASK_ID_2).get();

        //Assert
        //Original behandling
        assertThat(forrigeOppdrag.getOppdrag110Liste()).hasSize(2);
        List<Oppdragslinje150> forrigeOpp150FeriepengerListe = getOppdragslinje150Feriepenger(forrigeOppdrag);
        List<Oppdragslinje150> opp150RevurderingFeriepengerListe = getOppdragslinje150Feriepenger(oppdragRevurdering);

        //Revurdering
        assertThat(oppdragRevurdering.getOppdrag110Liste()).hasSize(2);
        assertThat(opp150RevurderingFeriepengerListe).hasSize(2);
        //Opphør
        List<Oppdragslinje150> opp150ForOpphListe = opp150RevurderingFeriepengerListe.stream()
            .filter(Oppdragslinje150::gjelderOpphør).collect(Collectors.toList());
        assertThat(opp150ForOpphListe).hasSize(2);
        verifiserOpp150NårEttFPÅretOpphørerOgAndreIkkeEndrerSeg(forrigeOpp150FeriepengerListe, opp150RevurderingFeriepengerListe, true);
    }

    /**
     * Forrige behandling: Har feriepenger for år1 - i_År.plusYears(1) og år2 - i_År.plusYears(2);
     * Ny behandling: Har ikke feriepenger;
     * Endring i årsbeløp: erBeløpForskjelligeForFPÅr1 = Ja, erBeløpForskjelligeForFPÅr2 = Ja
     */
    @Test
    public void skalSendeOpphørPåForrigeOppdragForFeriepengerNårDetIkkeErFeriepengerIRevurdering() {
        //Arrange
        //Førstegangsbehandling
        Oppdragskontroll forrigeOppdrag = opprettBeregningsresultatOgFørstegangsoppdragForFeriepenger(true, 11000L, 10000L);

        //Revurdering
        Behandling revurdering = oppsettBeregningsresultatFPRevurderingForFeriepenger(false, 0L, 0L);

        //Act
        Oppdragskontroll oppdragRevurdering = oppdragskontrollTjeneste.opprettOppdrag(revurdering.getId(), PROSESS_TASK_ID_2).get();

        //Assert
        //Original behandling
        assertThat(forrigeOppdrag.getOppdrag110Liste()).hasSize(2);
        List<Oppdragslinje150> opp150FeriepengerListe = getOppdragslinje150Feriepenger(forrigeOppdrag);
        assertThat(opp150FeriepengerListe).allSatisfy(oppdragslinje150 -> {
            assertThat(oppdragslinje150.getRefDelytelseId()).isNull();
            assertThat(oppdragslinje150.getRefFagsystemId()).isNull();
        });
        //Revurdering
        assertThat(oppdragRevurdering.getOppdrag110Liste()).hasSize(2);
        List<Oppdragslinje150> opp150RevurdFeriepengerListe = getOppdragslinje150Feriepenger(oppdragRevurdering);
        assertThat(opp150RevurdFeriepengerListe).hasSize(4);
        assertThat(opp150RevurdFeriepengerListe).allSatisfy(oppdragslinje150 ->
            assertThat(oppdragslinje150.gjelderOpphør()).isTrue());
        for (Oppdragslinje150 opp150Revurd : opp150RevurdFeriepengerListe) {
            assertThat(opp150FeriepengerListe).anySatisfy(oppdragslinje150 ->
                assertThat(oppdragslinje150.getDelytelseId()).isEqualTo(opp150Revurd.getDelytelseId()));
        }
        verifiserFeriepengeår(opp150RevurdFeriepengerListe);
    }

    /**
     * Forrige behandling: Har feriepenger for år1 - i_År.plusYears(1);
     * Ny behandling: Har ikke feriepenger;
     * Endring i årsbeløp: erBeløpForskjelligeForFPÅr1 = Ja, erBeløpForskjelligeForFPÅr2 = N/A
     */
    @Test
    public void skalSendeOpphørPåFørsteFeriepengeårNårDetIkkeErFeriepengerIRevurdering() {
        //Arrange
        //Førstegangsbehandling
        Oppdragskontroll forrigeOppdrag = opprettBeregningsresultatOgFørstegangsoppdragForFeriepenger(false, 11000L, 0L);
        //Revurdering
        Behandling revurdering = oppsettBeregningsresultatFPRevurderingForFeriepenger(false, 0L, 0L);

        //Act
        Oppdragskontroll oppdragRevurdering = oppdragskontrollTjeneste.opprettOppdrag(revurdering.getId(), PROSESS_TASK_ID_2).get();

        //Assert
        //Original behandling
        assertThat(forrigeOppdrag.getOppdrag110Liste()).hasSize(2);
        //Revurdering
        assertThat(oppdragRevurdering.getOppdrag110Liste()).hasSize(2);
        List<Oppdragslinje150> opp150RevurdFeriepengerListe = getOppdragslinje150Feriepenger(oppdragRevurdering);
        assertThat(opp150RevurdFeriepengerListe).hasSize(2);
        verifiserOppdr150NårDetIkkeErFeriepengerIRevurdering(forrigeOppdrag, oppdragRevurdering, true);
    }

    /**
     * Forrige behandling: Har feriepenger for år1 - i_År.plusYears(1);
     * Ny behandling: Har ikke feriepenger;
     * Endring i årsbeløp: erBeløpForskjelligeForFPÅr1 = Ja, erBeløpForskjelligeForFPÅr2 = N/A
     */
    @Test
    public void skalSendeOpphørPåAndreFeriepengeårNårDetIkkeErFeriepengerIRevurdering() {
        //Arrange
        //Førstegangsbehandling
        Oppdragskontroll forrigeOppdrag = opprettBeregningsresultatOgFørstegangsoppdragForFeriepenger(false, 0L, 9500L);
        //Revurdering
        Behandling revurdering = oppsettBeregningsresultatFPRevurderingForFeriepenger(false, 0L, 0L);

        //Act
        Oppdragskontroll oppdragRevurdering = oppdragskontrollTjeneste.opprettOppdrag(revurdering.getId(), PROSESS_TASK_ID_2).get();

        //Assert
        //Original behandling
        assertThat(forrigeOppdrag.getOppdrag110Liste()).hasSize(2);
        //Revurdering
        assertThat(oppdragRevurdering.getOppdrag110Liste()).hasSize(2);
        List<Oppdragslinje150> opp150RevurdFeriepengerListe = getOppdragslinje150Feriepenger(oppdragRevurdering);
        assertThat(opp150RevurdFeriepengerListe).hasSize(2);
        verifiserOppdr150NårDetIkkeErFeriepengerIRevurdering(forrigeOppdrag, oppdragRevurdering, false);
    }

    /**
     * Forrige behandling: Har ikke feriepenger;
     * Ny behandling: Har ikke feriepenger;
     * ErBeløpForskjelligeForFPÅr1 = N/A, erBeløpForskjelligeForFPÅr2 = N/A
     */
    @Test
    public void skalIkkeLagesOppdragHvisDetFinnesIngenFeriepengerIFørstegangsbehandlingOgRevurdering() {
        //Arrange
        //Førstegangsbehandling
        Oppdragskontroll forrigeOppdrag = opprettBeregningsresultatOgFørstegangsoppdragForFeriepenger(false, 0L, 0L);
        //Revurdering
        Behandling revurdering = oppsettBeregningsresultatFPRevurderingForFeriepenger(false, 0L, 0L);

        //Act
        Oppdragskontroll oppdragRevurdering = oppdragskontrollTjeneste.opprettOppdrag(revurdering.getId(), PROSESS_TASK_ID_2).get();

        //Assert
        List<Oppdragslinje150> forrigeOpp150FeriepengerListe = getOppdragslinje150Feriepenger(forrigeOppdrag);
        List<Oppdragslinje150> opp150RevurderingFeriepengerListe = getOppdragslinje150Feriepenger(oppdragRevurdering);
        assertThat(forrigeOpp150FeriepengerListe).isEmpty();
        assertThat(opp150RevurderingFeriepengerListe).isEmpty();
    }

    @Test
    public void skalSendeOppdragForFeriepengerNårDetGjelderAdopsjon() {
        //Arrange
        //Act
        //Førstegangsbehandling
        Oppdragskontroll oppdrag = opprettBeregningsresultatOgFørstegangsoppdragForFeriepenger(true, false, 11000L, 6000L);

        //Assert
        assertThat(oppdrag.getOppdrag110Liste()).hasSize(2);
        //Bruker
        Oppdrag110 oppdrag110Bruker = oppdrag.getOppdrag110Liste()
            .stream()
            .filter(o110 -> KodeFagområdeTjeneste.forForeldrepenger().gjelderBruker(o110))
            .findFirst()
            .get();
        List<Oppdragslinje150> opp150FeriepengerBruker = getOppdragslinje150Feriepenger(oppdrag110Bruker);
        assertThat(opp150FeriepengerBruker).allSatisfy(opp150 ->
            assertThat(opp150.getKodeKlassifik()).isEqualTo(ØkonomiKodeKlassifik.FPATFER.getKodeKlassifik()));
        // Arbeidsgiver
        Oppdrag110 oppdrag110Arbeidsgiver = oppdrag.getOppdrag110Liste()
            .stream()
            .filter(o110 -> !KodeFagområdeTjeneste.forForeldrepenger().gjelderBruker(o110))
            .findFirst()
            .get();
        List<Oppdragslinje150> opp150FeriepengerArbeidsgiver = getOppdragslinje150Feriepenger(oppdrag110Arbeidsgiver);
        assertThat(opp150FeriepengerArbeidsgiver).allSatisfy(opp150 ->
            assertThat(opp150.getKodeKlassifik()).isEqualTo(ØkonomiKodeKlassifik.FPADREFAGFER_IOP.getKodeKlassifik()));
    }
}
