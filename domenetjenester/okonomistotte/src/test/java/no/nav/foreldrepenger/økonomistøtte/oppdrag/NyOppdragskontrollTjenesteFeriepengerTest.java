package no.nav.foreldrepenger.økonomistøtte.oppdrag;

import static no.nav.foreldrepenger.økonomistøtte.OppdragskontrollFeriepengerTestUtil.getOppdr150ForFeriepengerForEnMottaker;
import static no.nav.foreldrepenger.økonomistøtte.OppdragskontrollFeriepengerTestUtil.verifiserFeriepengeår;
import static no.nav.foreldrepenger.økonomistøtte.OppdragskontrollFeriepengerTestUtil.verifiserOpp150NårEndringGjelderEttFeriepengeår;
import static no.nav.foreldrepenger.økonomistøtte.OppdragskontrollFeriepengerTestUtil.verifiserOpp150NårEttFPÅretOpphørerOgAndreIkkeEndrerSeg;
import static no.nav.foreldrepenger.økonomistøtte.OppdragskontrollFeriepengerTestUtil.verifiserOppdr150MedEttFeriepengeårKunIRevurdering;
import static no.nav.foreldrepenger.økonomistøtte.OppdragskontrollFeriepengerTestUtil.verifiserOppdr150NårDetErEndringForToFeriepengeår;
import static no.nav.foreldrepenger.økonomistøtte.OppdragskontrollFeriepengerTestUtil.verifiserOppdr150NårDetIkkeErFeriepengerIRevurdering;
import static no.nav.foreldrepenger.økonomistøtte.OppdragskontrollFeriepengerTestUtil.verifiserOppdr150NårEttFeriepengeårSkalOpphøre;
import static org.assertj.core.api.Assertions.assertThat;

import java.time.LocalDate;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import no.nav.foreldrepenger.behandlingslager.behandling.beregning.BehandlingBeregningsresultatEntitet;
import no.nav.foreldrepenger.behandlingslager.økonomioppdrag.FamilieYtelseType;
import no.nav.foreldrepenger.behandlingslager.økonomioppdrag.Oppdragslinje150;
import no.nav.foreldrepenger.behandlingslager.økonomioppdrag.Sats;
import no.nav.foreldrepenger.behandlingslager.økonomioppdrag.koder.KodeKlassifik;
import no.nav.foreldrepenger.økonomistøtte.OppdragMedPositivKvitteringTestUtil;
import no.nav.foreldrepenger.økonomistøtte.OppdragskontrollFeriepengerTestUtil;
import no.nav.foreldrepenger.økonomistøtte.oppdrag.mapper.TilkjentYtelseMapper;

class NyOppdragskontrollTjenesteFeriepengerTest extends NyOppdragskontrollTjenesteTestBase {

    @BeforeEach
    public void setUp() {
        super.setUp();
    }

    /**
     * Forrige behandling: Har feriepenger for to feriepengeår;
     * Ny behandling: Har feriepenger for to feriepengeår;
     * Endring i årsbeløp: erBeløpForskjelligeForFPÅr1 = Nei, erBeløpForskjelligeForFPÅr2 = Nei
     */
    @Test
    void skalIkkeSendeOppdragForFeriepengerNårDetIkkeErEndringIÅrsbeløp() {
        //Arrange
        //Førstegangsbehandling
        var originaltOppdrag = opprettBeregningsresultatOgFørstegangsoppdragForFeriepenger(true, 15000L, 10000L);
        //Revurdering

        var beregningsresultatRevurderingFP = oppsettBeregningsresultatForFeriepenger(true, 15000L, 10000L);

        //Act
        var mapper = TilkjentYtelseMapper.lagFor(FamilieYtelseType.FØDSEL);
        var gruppertYtelse2 = mapper.fordelPåNøkler(beregningsresultatRevurderingFP);
        var builder2 = getInputStandardBuilder(gruppertYtelse2).medTidligereOppdrag(mapTidligereOppdrag(List.of(originaltOppdrag)));

        // Act
        var oppdragRevurdering = nyOppdragskontrollTjeneste.opprettOppdrag(builder2.build());

        //Assert
        assertThat(oppdragRevurdering).isNotPresent();
    }

    private BehandlingBeregningsresultatEntitet oppsettBeregningsresultatForFeriepenger(boolean erOpptjentOverFlereÅr, Long årsbeløp1, Long årsbeløp2) {
        return buildBeregningsresultatFPForVerifiseringAvOpp150MedFeriepenger(erOpptjentOverFlereÅr, årsbeløp1,
            årsbeløp2, DAGENS_DATO);
    }

    /**
     * Forrige behandling: Har feriepenger for to feriepengeår;
     * Ny behandling: Har feriepenger for to feriepengeår;
     * Endring i årsbeløp: erBeløpForskjelligeForFPÅr1 = Nei, erBeløpForskjelligeForFPÅr2 = Ja
     */
    @Test
    void skalSendeOppdragKunForAndreFeriepengeårSomHaddeEndringAvÅrsbeløpIRevurdering() {
        //Arrange
        //Førstegangsbehandling
        var originaltOppdrag = opprettBeregningsresultatOgFørstegangsoppdragForFeriepenger(true, 15000L, 10000L);
        //Revurdering
        var beregningsresultatRevurderingFP = oppsettBeregningsresultatForFeriepenger(true, 15000L, 11000L);

        var mapper = TilkjentYtelseMapper.lagFor(FamilieYtelseType.FØDSEL);
        var gruppertYtelse2 = mapper.fordelPåNøkler(beregningsresultatRevurderingFP);
        var builder2 = getInputStandardBuilder(gruppertYtelse2).medTidligereOppdrag(mapTidligereOppdrag(List.of(originaltOppdrag)));

        // Act
        var oppdragRevurdering = OppdragMedPositivKvitteringTestUtil.opprett(nyOppdragskontrollTjeneste, builder2.build());

        //Assert
        int førsteFeriepengeår = FERIEPENGEÅR_LISTE.get(0);
        int andreFeriepengeår = FERIEPENGEÅR_LISTE.get(1);
        //Original behandling
        assertThat(originaltOppdrag.getOppdrag110Liste()).hasSize(2);
        var forrigeOpp150FeriepengerListe = getOppdragslinje150Feriepenger(originaltOppdrag);
        assertThat(forrigeOpp150FeriepengerListe).hasSize(4);
        //Bruker
        var forrigeopp150FeriepengerBrukerList = getOppdr150ForFeriepengerForEnMottaker(forrigeOpp150FeriepengerListe, true);
        assertThat(forrigeopp150FeriepengerBrukerList).hasSize(2).anySatisfy(opp150 -> {
            assertThat(opp150.getDatoVedtakFom().getYear()).isEqualTo(førsteFeriepengeår);
            assertThat(opp150.getSats()).isEqualTo(Sats.på(15000L));
        }).anySatisfy(opp150 -> {
            assertThat(opp150.getDatoVedtakFom().getYear()).isEqualTo(andreFeriepengeår);
            assertThat(opp150.getSats()).isEqualTo(Sats.på(10000L));
        });
        //Arbeidsgiver
        var forrigeopp150FeriepengerArbgvrList = getOppdr150ForFeriepengerForEnMottaker(forrigeOpp150FeriepengerListe, false);
        assertThat(forrigeopp150FeriepengerArbgvrList).hasSize(2).anySatisfy(opp150 -> {
            assertThat(opp150.getDatoVedtakFom().getYear()).isEqualTo(førsteFeriepengeår);
            assertThat(opp150.getSats()).isEqualTo(Sats.på(15000L));
        }).anySatisfy(opp150 -> {
            assertThat(opp150.getDatoVedtakFom().getYear()).isEqualTo(andreFeriepengeår);
            assertThat(opp150.getSats()).isEqualTo(Sats.på(10000L));
        });
        //Revurdering
        assertThat(oppdragRevurdering.getOppdrag110Liste()).hasSize(2);
        var opp150RevurderingFeriepengerListe = getOppdragslinje150Feriepenger(oppdragRevurdering);
        assertThat(opp150RevurderingFeriepengerListe).hasSize(2);
        //Arbeidsgiver revurdering
        var opp150RevurderingFeriepengerArbgvrList = getOppdr150ForFeriepengerForEnMottaker(opp150RevurderingFeriepengerListe, false);
        assertThat(opp150RevurderingFeriepengerArbgvrList).hasSize(1);
        assertThat(opp150RevurderingFeriepengerArbgvrList.get(0).getSats()).isEqualTo(Sats.på(11000L));
        //Bruker revurdering
        var opp150RevurderingFeriepengerBrukerList = getOppdr150ForFeriepengerForEnMottaker(opp150RevurderingFeriepengerListe, true);
        assertThat(opp150RevurderingFeriepengerBrukerList).hasSize(1);
        assertThat(opp150RevurderingFeriepengerBrukerList.get(0).getSats()).isEqualTo(Sats.på(11000L));
        verifiserOpp150NårEndringGjelderEttFeriepengeår(forrigeOpp150FeriepengerListe, opp150RevurderingFeriepengerListe, false);
    }

    /**
     * Forrige behandling: Har feriepenger for to feriepengeår;
     * Ny behandling: Har feriepenger for to feriepengeår;
     * Endring i årsbeløp: erBeløpForskjelligeForFPÅr1 = Ja, erBeløpForskjelligeForFPÅr2 = Nei
     */
    @Test
    void skalSendeOppdragKunForFørsteFeriepengeårSomHaddeEndringAvÅrsbeløpIRevurdering() {
        // Arrange
        //Førstegangsbehandling
        var originaltOppdrag = opprettBeregningsresultatOgFørstegangsoppdragForFeriepenger(true, 9000L, 11000L);
        //Revurdering
        var beregningsresultatRevurderingFP = oppsettBeregningsresultatForFeriepenger(true, 7000L, 11000L);

        var mapper = TilkjentYtelseMapper.lagFor(FamilieYtelseType.FØDSEL);
        var gruppertYtelse2 = mapper.fordelPåNøkler(beregningsresultatRevurderingFP);
        var builder2 = getInputStandardBuilder(gruppertYtelse2).medTidligereOppdrag(mapTidligereOppdrag(List.of(originaltOppdrag)));

        // Act
        var oppdragRevurdering = OppdragMedPositivKvitteringTestUtil.opprett(nyOppdragskontrollTjeneste, builder2.build());

        //Assert
        //Original behandling
        assertThat(originaltOppdrag.getOppdrag110Liste()).hasSize(2);
        var forrigeOpp150FeriepengerListe = getOppdragslinje150Feriepenger(originaltOppdrag);
        assertThat(forrigeOpp150FeriepengerListe).hasSize(4);

        //Revurdering
        assertThat(oppdragRevurdering.getOppdrag110Liste()).hasSize(2);
        var opp150RevurderingFeriepengerListe = getOppdragslinje150Feriepenger(oppdragRevurdering);
        assertThat(opp150RevurderingFeriepengerListe).hasSize(2);
        //Arbeidsgiver revurdering
        var opp150RevurderingFeriepengerArbgvrList = getOppdr150ForFeriepengerForEnMottaker(opp150RevurderingFeriepengerListe, false);
        assertThat(opp150RevurderingFeriepengerArbgvrList).hasSize(1);
        assertThat(opp150RevurderingFeriepengerArbgvrList.get(0).getSats()).isEqualTo(Sats.på(7000L));
        //Bruker revurdering
        var opp150RevurderingFeriepengerBrukerList = getOppdr150ForFeriepengerForEnMottaker(opp150RevurderingFeriepengerListe, true);
        assertThat(opp150RevurderingFeriepengerBrukerList).hasSize(1);
        assertThat(opp150RevurderingFeriepengerBrukerList.get(0).getSats()).isEqualTo(Sats.på(7000L));
        verifiserOpp150NårEndringGjelderEttFeriepengeår(forrigeOpp150FeriepengerListe, opp150RevurderingFeriepengerListe, true);
    }

    /**
     * Forrige behandling: Har feriepenger for to feriepengeår;
     * Ny behandling: Har feriepenger for to feriepengeår;
     * Endring i årsbeløp: erBeløpForskjelligeForFPÅr1 = Ja, erBeløpForskjelligeForFPÅr2 = Ja
     */
    @Test
    void skalSendeOppdragForAlleFeriepengeårNårDetBlirEndringAvÅrsbeløpForAlleFeriepengeårIRevurdering() {
        //Arrange
        //Førstegangsbehandling
        var originaltOppdrag = opprettBeregningsresultatOgFørstegangsoppdragForFeriepenger(true, 6000L, 8000L);
        //Revurdering
        var beregningsresultatRevurderingFP = oppsettBeregningsresultatForFeriepenger(true, 9000L, 7000L);

        var mapper = TilkjentYtelseMapper.lagFor(FamilieYtelseType.FØDSEL);
        var gruppertYtelse2 = mapper.fordelPåNøkler(beregningsresultatRevurderingFP);
        var builder2 = getInputStandardBuilder(gruppertYtelse2).medTidligereOppdrag(mapTidligereOppdrag(List.of(originaltOppdrag)));

        // Act
        var oppdragRevurdering = OppdragMedPositivKvitteringTestUtil.opprett(nyOppdragskontrollTjeneste, builder2.build());

        //Assert
        //Original behandling
        assertThat(originaltOppdrag.getOppdrag110Liste()).hasSize(2);
        var forrigeOpp150FeriepengerListe = getOppdragslinje150Feriepenger(originaltOppdrag);
        assertThat(forrigeOpp150FeriepengerListe).hasSize(4);
        //Revurdering
        assertThat(oppdragRevurdering.getOppdrag110Liste()).hasSize(2);
        var opp150RevurderingFeriepengerListe = getOppdragslinje150Feriepenger(oppdragRevurdering);
        assertThat(opp150RevurderingFeriepengerListe).hasSize(4);
        //Bruker
        var opp150RevurderingFeriepengerBrukerList = getOppdr150ForFeriepengerForEnMottaker(opp150RevurderingFeriepengerListe, true);
        assertThat(opp150RevurderingFeriepengerBrukerList).hasSize(2)
            .anySatisfy(opp150 -> assertThat(opp150.getSats()).isEqualTo(Sats.på(9000L)))
            .anySatisfy(opp150 -> assertThat(opp150.getSats()).isEqualTo(Sats.på(7000L)));
        //Arbeidsgiver
        var opp150RevurderingFeriepengerArbgvrList = getOppdr150ForFeriepengerForEnMottaker(opp150RevurderingFeriepengerListe, false);
        assertThat(opp150RevurderingFeriepengerArbgvrList).hasSize(2)
            .anySatisfy(opp150 -> assertThat(opp150.getSats()).isEqualTo(Sats.på(9000L)))
            .anySatisfy(opp150 -> assertThat(opp150.getSats()).isEqualTo(Sats.på(7000L)));
        for (var forrigeOpp150 : forrigeOpp150FeriepengerListe) {
            assertThat(opp150RevurderingFeriepengerListe).anySatisfy(
                oppdragslinje150 -> assertThat(oppdragslinje150.getRefDelytelseId()).isEqualTo(forrigeOpp150.getDelytelseId()));
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
    void skalSendeOppdragForAlleFeriepengeårNårDetEksistererIngenFeriepengerIForrigeBehandling() {
        //Arrange
        //Førstegangsbehandling
        var originaltOppdrag = opprettBeregningsresultatOgFørstegangsoppdragForFeriepenger(false, 0L, 0L);
        //Revurdering
        var beregningsresultatRevurderingFP = oppsettBeregningsresultatForFeriepenger(true, 11000L, 13000L);

        var mapper = TilkjentYtelseMapper.lagFor(FamilieYtelseType.FØDSEL);
        var gruppertYtelse2 = mapper.fordelPåNøkler(beregningsresultatRevurderingFP);
        var builder2 = getInputStandardBuilder(gruppertYtelse2).medTidligereOppdrag(mapTidligereOppdrag(List.of(originaltOppdrag)));

        // Act
        var oppdragRevurdering = OppdragMedPositivKvitteringTestUtil.opprett(nyOppdragskontrollTjeneste, builder2.build());

        //Assert
        //Original behandling
        assertThat(originaltOppdrag.getOppdrag110Liste()).hasSize(2);
        var forrigeOpp150FeriepengerListe = getOppdragslinje150Feriepenger(originaltOppdrag);
        assertThat(forrigeOpp150FeriepengerListe).isEmpty();
        //Revurdering
        assertThat(oppdragRevurdering.getOppdrag110Liste()).hasSize(2);
        var opp150RevurderingFeriepengerListe = getOppdragslinje150Feriepenger(oppdragRevurdering);
        assertThat(opp150RevurderingFeriepengerListe).hasSize(4);
        //Bruker
        var opp150RevurderingFeriepengerBrukerList = getOppdr150ForFeriepengerForEnMottaker(opp150RevurderingFeriepengerListe, true);
        assertThat(opp150RevurderingFeriepengerBrukerList).hasSize(2)
            .anySatisfy(opp150 -> assertThat(opp150.getSats()).isEqualTo(Sats.på(11000L)))
            .anySatisfy(opp150 -> assertThat(opp150.getSats()).isEqualTo(Sats.på(13000L)));
        //Arbeidsgiver
        var opp150RevurderingFeriepengerArbgvrList = getOppdr150ForFeriepengerForEnMottaker(opp150RevurderingFeriepengerListe, false);
        assertThat(opp150RevurderingFeriepengerArbgvrList).hasSize(2)
            .anySatisfy(opp150 -> assertThat(opp150.getSats()).isEqualTo(Sats.på(11000L)))
            .anySatisfy(opp150 -> assertThat(opp150.getSats()).isEqualTo(Sats.på(13000L)));
        verifiserOppdr150NårDetErEndringForToFeriepengeår(forrigeOpp150FeriepengerListe, opp150RevurderingFeriepengerListe);
        verifiserFeriepengeår(opp150RevurderingFeriepengerListe);
    }

    /**
     * Forrige behandling: Har ikke feriepenger;
     * Ny behandling: Har feriepenger for år2 - i_År.plusYears(2);
     * Endring i årsbeløp: erBeløpForskjelligeForFPÅr1 = Nei, erBeløpForskjelligeForFPÅr2 = Ja
     */
    @Test
    void skalSendeOppdragKunForAndreFeriepengeårNårForrigeBehandlingIkkeHarFeriepengerOgRevurderingHarDetIAndreÅr() {
        //Arrange
        //Førstegangsbehandling
        var originaltOppdrag = opprettBeregningsresultatOgFørstegangsoppdragForFeriepenger(false, 0L, 0L);
        //Revurdering
        var beregningsresultatRevurderingFP = oppsettBeregningsresultatForFeriepenger(false, 0L, 13000L);

        var mapper = TilkjentYtelseMapper.lagFor(FamilieYtelseType.FØDSEL);
        var gruppertYtelse2 = mapper.fordelPåNøkler(beregningsresultatRevurderingFP);
        var builder2 = getInputStandardBuilder(gruppertYtelse2).medTidligereOppdrag(mapTidligereOppdrag(List.of(originaltOppdrag)));

        // Act
        var oppdragRevurdering = OppdragMedPositivKvitteringTestUtil.opprett(nyOppdragskontrollTjeneste, builder2.build());

        //Assert
        //Original behandling
        assertThat(originaltOppdrag.getOppdrag110Liste()).hasSize(2);
        var opp150FeriepengerListe = getOppdragslinje150Feriepenger(originaltOppdrag);
        assertThat(opp150FeriepengerListe).isEmpty();
        //Revurdering
        assertThat(oppdragRevurdering.getOppdrag110Liste()).hasSize(2);
        var opp150RevurderingFeriepengerListe = getOppdragslinje150Feriepenger(oppdragRevurdering);
        assertThat(opp150RevurderingFeriepengerListe).hasSize(2);
        //Bruker
        var opp150RevurderingFeriepengerBrukerList = getOppdr150ForFeriepengerForEnMottaker(opp150RevurderingFeriepengerListe, true);
        assertThat(opp150RevurderingFeriepengerBrukerList).hasSize(1).anySatisfy(opp150 -> assertThat(opp150.getSats()).isEqualTo(Sats.på(13000L)));
        //Arbeidsgiver
        var opp150RevurderingFeriepengerArbgvrList = getOppdr150ForFeriepengerForEnMottaker(opp150RevurderingFeriepengerListe, false);
        assertThat(opp150RevurderingFeriepengerArbgvrList).hasSize(1).anySatisfy(opp150 -> assertThat(opp150.getSats()).isEqualTo(Sats.på(13000L)));
        verifiserOppdr150MedEttFeriepengeårKunIRevurdering(opp150RevurderingFeriepengerListe, false);
    }

    /**
     * Forrige behandling: Har ikke feriepenger;
     * Ny behandling: Har feriepenger for år1 - i_År.plusYears(1);
     * Endring i årsbeløp: erBeløpForskjelligeForFPÅr1 = Ja, erBeløpForskjelligeForFPÅr2 = Nei
     */
    @Test
    void skalSendeOppdragKunForFørsteFeriepengeårNårForrigeBehandlingIkkeHarFeriepengerOgRevurderingHarDetIFørsteÅr() {
        //Arrange
        //Førstegangsbehandling
        var originaltOppdrag = opprettBeregningsresultatOgFørstegangsoppdragForFeriepenger(false, 0L, 0L);
        //Revurdering
        var beregningsresultatRevurderingFP = oppsettBeregningsresultatForFeriepenger(false, 8000L, 0L);

        var mapper = TilkjentYtelseMapper.lagFor(FamilieYtelseType.FØDSEL);
        var gruppertYtelse2 = mapper.fordelPåNøkler(beregningsresultatRevurderingFP);
        var builder2 = getInputStandardBuilder(gruppertYtelse2).medTidligereOppdrag(mapTidligereOppdrag(List.of(originaltOppdrag)));

        // Act
        var oppdragRevurdering = OppdragMedPositivKvitteringTestUtil.opprett(nyOppdragskontrollTjeneste, builder2.build());

        //Assert
        //Original behandling
        assertThat(originaltOppdrag.getOppdrag110Liste()).hasSize(2);
        var opp150FeriepengerListe = getOppdragslinje150Feriepenger(originaltOppdrag);
        assertThat(opp150FeriepengerListe).isEmpty();
        //Revurdering
        assertThat(oppdragRevurdering.getOppdrag110Liste()).hasSize(2);
        var opp150RevurderingFeriepengerListe = getOppdragslinje150Feriepenger(oppdragRevurdering);
        assertThat(opp150RevurderingFeriepengerListe).hasSize(2);
        //Bruker
        var opp150RevurderingFeriepengerBrukerList = getOppdr150ForFeriepengerForEnMottaker(opp150RevurderingFeriepengerListe, true);
        assertThat(opp150RevurderingFeriepengerBrukerList).hasSize(1).anySatisfy(opp150 -> assertThat(opp150.getSats()).isEqualTo(Sats.på(8000L)));
        //Arbeidsgiver
        var opp150RevurderingFeriepengerArbgvrList = getOppdr150ForFeriepengerForEnMottaker(opp150RevurderingFeriepengerListe, false);
        assertThat(opp150RevurderingFeriepengerArbgvrList).hasSize(1).anySatisfy(opp150 -> assertThat(opp150.getSats()).isEqualTo(Sats.på(8000L)));
        verifiserOppdr150MedEttFeriepengeårKunIRevurdering(opp150RevurderingFeriepengerListe, true);
    }

    /**
     * Forrige behandling: Har feriepenger for år1 - i_År.plusYears(1);
     * Ny behandling: Har feriepenger for år1 - i_År.plusYears(1) og år2 - i_År.plusYears(1);
     * Endring i årsbeløp: erBeløpForskjelligeForFPÅr1 = Ja, erBeløpForskjelligeForFPÅr2 = Ja
     */
    @Test
    void skalSendeOppdragForAlleFeriepengeårNårForrigeBehandlingIkkeHarFeriepengerForAndreÅrOgRevurderingHarDetForBeggeToÅr() {
        //Arrange
        //Førstegangsbehandling
        var originaltOppdrag = opprettBeregningsresultatOgFørstegangsoppdragForFeriepenger(false, 6500L, 0L);
        //Revurdering
        var beregningsresultatRevurderingFP = oppsettBeregningsresultatForFeriepenger(true, 8000L, 7000L);

        var mapper = TilkjentYtelseMapper.lagFor(FamilieYtelseType.FØDSEL);
        var gruppertYtelse2 = mapper.fordelPåNøkler(beregningsresultatRevurderingFP);
        var builder2 = getInputStandardBuilder(gruppertYtelse2).medTidligereOppdrag(mapTidligereOppdrag(List.of(originaltOppdrag)));

        // Act
        var oppdragRevurdering = OppdragMedPositivKvitteringTestUtil.opprett(nyOppdragskontrollTjeneste, builder2.build());

        //Assert
        var forrigeOpp150FeriepengerListe = getOppdragslinje150Feriepenger(originaltOppdrag);
        var opp150RevurderingFeriepengerListe = getOppdragslinje150Feriepenger(oppdragRevurdering);
        int førsteFeriepengeår = FERIEPENGEÅR_LISTE.get(0);
        //Original  behandling
        assertThat(originaltOppdrag.getOppdrag110Liste()).hasSize(2);
        assertThat(forrigeOpp150FeriepengerListe).hasSize(2);
        //Arbeidsgiver forrige behandling
        var forrigeopp150FeriepengerArbgvrList = getOppdr150ForFeriepengerForEnMottaker(forrigeOpp150FeriepengerListe, false);
        assertThat(forrigeopp150FeriepengerArbgvrList).hasSize(1);
        assertThat(forrigeopp150FeriepengerArbgvrList.get(0).getDatoVedtakFom().getYear()).isEqualTo(førsteFeriepengeår);
        assertThat(forrigeopp150FeriepengerArbgvrList.get(0).getSats()).isEqualTo(Sats.på(6500L));
        //Bruker forrige behandling
        var forrigeopp150FeriepengerBrukerList = getOppdr150ForFeriepengerForEnMottaker(forrigeOpp150FeriepengerListe, true);
        assertThat(forrigeopp150FeriepengerBrukerList).hasSize(1);
        assertThat(forrigeopp150FeriepengerBrukerList.get(0).getDatoVedtakFom().getYear()).isEqualTo(førsteFeriepengeår);
        assertThat(forrigeopp150FeriepengerBrukerList.get(0).getSats()).isEqualTo(Sats.på(6500L));
        //Revurdering
        assertThat(oppdragRevurdering.getOppdrag110Liste()).hasSize(2);
        //Bruker
        var opp150RevurderingFeriepengerBrukerList = getOppdr150ForFeriepengerForEnMottaker(opp150RevurderingFeriepengerListe, true);
        assertThat(opp150RevurderingFeriepengerBrukerList).hasSize(2)
            .anySatisfy(opp150 -> assertThat(opp150.getSats()).isEqualTo(Sats.på(8000L)))
            .anySatisfy(opp150 -> assertThat(opp150.getSats()).isEqualTo(Sats.på(7000L)));
        //Arbeidsgiver
        var opp150RevurderingFeriepengerArbgvrList = getOppdr150ForFeriepengerForEnMottaker(opp150RevurderingFeriepengerListe, false);
        assertThat(opp150RevurderingFeriepengerArbgvrList).hasSize(2)
            .anySatisfy(opp150 -> assertThat(opp150.getSats()).isEqualTo(Sats.på(8000L)))
            .anySatisfy(opp150 -> assertThat(opp150.getSats()).isEqualTo(Sats.på(7000L)));
        OppdragskontrollFeriepengerTestUtil.verifiserRefDelytelseId(forrigeOpp150FeriepengerListe, opp150RevurderingFeriepengerListe, true);
        verifiserFeriepengeår(opp150RevurderingFeriepengerListe);
        assertThat(opp150RevurderingFeriepengerListe).anySatisfy(oppdragslinje150 ->
            assertThat(oppdragslinje150.getRefDelytelseId()).isNull());
    }

    /**
     * Forrige behandling: Har feriepenger for år1 - i_År.plusYears(1);
     * Ny behandling: Har feriepenger for år1 - i_År.plusYears(1) og år2 - i_År.plusYears(2);
     * Endring i årsbeløp: erBeløpForskjelligeForFPÅr1 = Nei, erBeløpForskjelligeForFPÅr2 = Ja
     */
    @Test
    void skalSendeOppdragKunForAndreFeriepengeårNårForrigeBehandlingHarFeriepengerForFørsteÅrOgRevurderingHarDetForBeggeToÅrUtenEndringForFørsteÅr() {
        //Arrange
        //Førstegangsbehandling
        var originaltOppdrag = opprettBeregningsresultatOgFørstegangsoppdragForFeriepenger(false, 6500L, 0L);
        //Revurdering
        var beregningsresultatRevurderingFP = oppsettBeregningsresultatForFeriepenger(true, 6500L, 7000L);

        var mapper = TilkjentYtelseMapper.lagFor(FamilieYtelseType.FØDSEL);
        var gruppertYtelse2 = mapper.fordelPåNøkler(beregningsresultatRevurderingFP);
        var builder2 = getInputStandardBuilder(gruppertYtelse2).medTidligereOppdrag(mapTidligereOppdrag(List.of(originaltOppdrag)));

        // Act
        var oppdragRevurdering = OppdragMedPositivKvitteringTestUtil.opprett(nyOppdragskontrollTjeneste, builder2.build());

        //Assert
        var forrigeOpp150FeriepengerListe = getOppdragslinje150Feriepenger(originaltOppdrag);
        var opp150RevurderingFeriepengerListe = getOppdragslinje150Feriepenger(oppdragRevurdering);
        int andreFeriepengeår = FERIEPENGEÅR_LISTE.get(1);
        //Original  behandling
        assertThat(originaltOppdrag.getOppdrag110Liste()).hasSize(2);
        assertThat(forrigeOpp150FeriepengerListe).hasSize(2);

        //Revurdering
        assertThat(oppdragRevurdering.getOppdrag110Liste()).hasSize(2);
        assertThat(opp150RevurderingFeriepengerListe).hasSize(2);
        //Arbeidsgiver revurdering
        var opp150RevurderingFeriepengerArbgvrList = getOppdr150ForFeriepengerForEnMottaker(opp150RevurderingFeriepengerListe, false);
        assertThat(opp150RevurderingFeriepengerArbgvrList).hasSize(1);
        assertThat(opp150RevurderingFeriepengerArbgvrList.get(0).getDatoVedtakFom().getYear()).isEqualTo(andreFeriepengeår);
        assertThat(opp150RevurderingFeriepengerArbgvrList.get(0).getSats()).isEqualTo(Sats.på(7000L));
        //Bruker revurdering
        var opp150RevurderingFeriepengerBrukerList = getOppdr150ForFeriepengerForEnMottaker(opp150RevurderingFeriepengerListe, true);
        assertThat(opp150RevurderingFeriepengerBrukerList).hasSize(1);
        assertThat(opp150RevurderingFeriepengerBrukerList.get(0).getDatoVedtakFom().getYear()).isEqualTo(andreFeriepengeår);
        assertThat(opp150RevurderingFeriepengerBrukerList.get(0).getSats()).isEqualTo(Sats.på(7000L));
    }

    /**
     * Forrige behandling: Har feriepenger for år2 - i_År.plusYears(2);
     * Ny behandling: Har feriepenger for år1 - i_År.plusYears(1) og år2 - i_År.plusYears(2);
     * Endring i årsbeløp: erBeløpForskjelligeForFPÅr1 = Ja, erBeløpForskjelligeForFPÅr2 = Ja
     */
    @Test
    void skalSendeOppdragForAlleFeriepengeårNårForrigeBehandlingIkkeHarFeriepengerForFørsteÅrOgRevurderingHarDetForBeggeToÅr() {
        //Arrange
        //Førstegangsbehandling
        var originaltOppdrag = opprettBeregningsresultatOgFørstegangsoppdragForFeriepenger(false, 0L, 7500L);
        //Revurdering
        var beregningsresultatRevurderingFP = oppsettBeregningsresultatForFeriepenger(true, 9500L, 7499L);

        var mapper = TilkjentYtelseMapper.lagFor(FamilieYtelseType.FØDSEL);
        var gruppertYtelse2 = mapper.fordelPåNøkler(beregningsresultatRevurderingFP);
        var builder2 = getInputStandardBuilder(gruppertYtelse2).medTidligereOppdrag(mapTidligereOppdrag(List.of(originaltOppdrag)));

        // Act
        var oppdragRevurdering = OppdragMedPositivKvitteringTestUtil.opprett(nyOppdragskontrollTjeneste, builder2.build());

        //Assert
        int andreFeriepengeår = FERIEPENGEÅR_LISTE.get(1);
        var forrigeOpp150FeriepengerListe = getOppdragslinje150Feriepenger(originaltOppdrag);
        var opp150RevurderingFeriepengerListe = getOppdragslinje150Feriepenger(oppdragRevurdering);
        //Original  behandling
        assertThat(originaltOppdrag.getOppdrag110Liste()).hasSize(2);
        assertThat(forrigeOpp150FeriepengerListe).hasSize(2);
        //Arbeidsgiver forrige behandling
        var forrigeopp150FeriepengerArbgvrList = getOppdr150ForFeriepengerForEnMottaker(forrigeOpp150FeriepengerListe, false);
        assertThat(forrigeopp150FeriepengerArbgvrList).hasSize(1);
        assertThat(forrigeopp150FeriepengerArbgvrList.get(0).getDatoVedtakFom().getYear()).isEqualTo(andreFeriepengeår);
        assertThat(forrigeopp150FeriepengerArbgvrList.get(0).getSats()).isEqualTo(Sats.på(7500L));
        //Bruker forrige behandling
        var forrigeopp150FeriepengerBrukerList = getOppdr150ForFeriepengerForEnMottaker(forrigeOpp150FeriepengerListe, true);
        assertThat(forrigeopp150FeriepengerBrukerList).hasSize(1);
        assertThat(forrigeopp150FeriepengerBrukerList.get(0).getDatoVedtakFom().getYear()).isEqualTo(andreFeriepengeår);
        assertThat(forrigeopp150FeriepengerBrukerList.get(0).getSats()).isEqualTo(Sats.på(7500L));
        var opp150FeriepengerListe = getOppdragslinje150Feriepenger(originaltOppdrag);
        //Revurdering
        assertThat(oppdragRevurdering.getOppdrag110Liste()).hasSize(2);
        //Bruker
        var opp150RevurderingFeriepengerBrukerList = getOppdr150ForFeriepengerForEnMottaker(opp150RevurderingFeriepengerListe, true);
        assertThat(opp150RevurderingFeriepengerBrukerList).hasSize(2)
            .anySatisfy(opp150 -> assertThat(opp150.getSats()).isEqualTo(Sats.på(9500L)))
            .anySatisfy(opp150 -> assertThat(opp150.getSats()).isEqualTo(Sats.på(7499L)));
        //Arbeidsgiver
        var opp150RevurderingFeriepengerArbgvrList = getOppdr150ForFeriepengerForEnMottaker(opp150RevurderingFeriepengerListe, false);
        assertThat(opp150RevurderingFeriepengerArbgvrList).hasSize(2)
            .anySatisfy(opp150 -> assertThat(opp150.getSats()).isEqualTo(Sats.på(9500L)))
            .anySatisfy(opp150 -> assertThat(opp150.getSats()).isEqualTo(Sats.på(7499L)));
        OppdragskontrollFeriepengerTestUtil.verifiserRefDelytelseId(opp150FeriepengerListe, opp150RevurderingFeriepengerListe, false);
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
    void skalSendeOppdragKunForFørsteFeriepengeårNårForrigeBehandlingHarFeriepengerForFørsteÅrOgRevurderingHarDetForBeggeToÅrUtenEndringForAndreÅr() {
        //Arrange
        //Førstegangsbehandling
        var originaltOppdrag = opprettBeregningsresultatOgFørstegangsoppdragForFeriepenger(false, 0L, 7000L);
        //Revurdering
        var beregningsresultatRevurderingFP = oppsettBeregningsresultatForFeriepenger(true, 6500L, 7000L);

        var mapper = TilkjentYtelseMapper.lagFor(FamilieYtelseType.FØDSEL);
        var gruppertYtelse2 = mapper.fordelPåNøkler(beregningsresultatRevurderingFP);
        var builder2 = getInputStandardBuilder(gruppertYtelse2).medTidligereOppdrag(mapTidligereOppdrag(List.of(originaltOppdrag)));

        // Act
        var oppdragRevurdering = OppdragMedPositivKvitteringTestUtil.opprett(nyOppdragskontrollTjeneste, builder2.build());

        //Assert
        int førsteFeriepengeår = FERIEPENGEÅR_LISTE.get(0);
        var forrigeOpp150FeriepengerListe = getOppdragslinje150Feriepenger(originaltOppdrag);
        var opp150RevurderingFeriepengerListe = getOppdragslinje150Feriepenger(oppdragRevurdering);
        //Original  behandling
        assertThat(originaltOppdrag.getOppdrag110Liste()).hasSize(2);
        assertThat(forrigeOpp150FeriepengerListe).hasSize(2);

        //Revurdering
        assertThat(oppdragRevurdering.getOppdrag110Liste()).hasSize(2);
        assertThat(opp150RevurderingFeriepengerListe).hasSize(2);
        //Arbeidsgiver revurdering
        var opp150RevurderingFeriepengerArbgvrList = getOppdr150ForFeriepengerForEnMottaker(opp150RevurderingFeriepengerListe, false);
        assertThat(opp150RevurderingFeriepengerArbgvrList).hasSize(1);
        assertThat(opp150RevurderingFeriepengerArbgvrList.get(0).getDatoVedtakFom().getYear()).isEqualTo(førsteFeriepengeår);
        assertThat(opp150RevurderingFeriepengerArbgvrList.get(0).getSats()).isEqualTo(Sats.på(6500L));
        //Bruker revurdering
        var opp150RevurderingFeriepengerBrukerList = getOppdr150ForFeriepengerForEnMottaker(opp150RevurderingFeriepengerListe, true);
        assertThat(opp150RevurderingFeriepengerBrukerList).hasSize(1);
        assertThat(opp150RevurderingFeriepengerBrukerList.get(0).getDatoVedtakFom().getYear()).isEqualTo(førsteFeriepengeår);
        assertThat(opp150RevurderingFeriepengerBrukerList.get(0).getSats()).isEqualTo(Sats.på(6500L));
    }

    /**
     * Forrige behandling: Har feriepenger for år1 - i_År.plusYears(1) og år2 - i_År.plusYears(2);
     * Ny behandling: Har feriepenger for år1 - i_År.plusYears(1);
     * Endring i årsbeløp: erBeløpForskjelligeForFPÅr1 = Ja, erBeløpForskjelligeForFPÅr2 = Ja
     */
    @Test
    void skalSendeEnOppdragForOpphørOgEnForEndringNårFørsteFeriepengeårEndrerSegOgAndreFeriepengeårOpphørerIRevurdering() {
        //Arrange
        //Førstegangsbehandling
        var originaltOppdrag = opprettBeregningsresultatOgFørstegangsoppdragForFeriepenger(true, 12000L, 10000L);
        //Revurdering
        var beregningsresultatRevurderingFP = oppsettBeregningsresultatForFeriepenger(true, 13000L, 0L);

        var mapper = TilkjentYtelseMapper.lagFor(FamilieYtelseType.FØDSEL);
        var gruppertYtelse2 = mapper.fordelPåNøkler(beregningsresultatRevurderingFP);
        var builder2 = getInputStandardBuilder(gruppertYtelse2).medTidligereOppdrag(mapTidligereOppdrag(List.of(originaltOppdrag)));

        // Act
        var oppdragRevurdering = OppdragMedPositivKvitteringTestUtil.opprett(nyOppdragskontrollTjeneste, builder2.build());

        //Assert
        var forrigeOpp150FeriepengerListe = getOppdragslinje150Feriepenger(originaltOppdrag);
        var opp150RevurderingFeriepengerListe = getOppdragslinje150Feriepenger(oppdragRevurdering);
        int andreFeriepengeår = FERIEPENGEÅR_LISTE.get(1);
        //Original behandling
        assertThat(originaltOppdrag.getOppdrag110Liste()).hasSize(2);
        assertThat(forrigeOpp150FeriepengerListe).hasSize(4);

        //Revurdering
        assertThat(opp150RevurderingFeriepengerListe).hasSize(4);
        //Arbeidsgiver revurdering
        var opp150RevurderingFeriepengerArbgvrList = getOppdr150ForFeriepengerForEnMottaker(opp150RevurderingFeriepengerListe, false);
        var opp150RevurdArbgvrUtenOpph = opp150RevurderingFeriepengerArbgvrList.stream()
            .filter(opp150 -> !opp150.gjelderOpphør())
            .toList();
        assertThat(opp150RevurdArbgvrUtenOpph).hasSize(1);
        assertThat(opp150RevurdArbgvrUtenOpph.get(0).getSats()).isEqualTo(Sats.på(13000L));
        //Bruker revurdering
        assertThat(oppdragRevurdering.getOppdrag110Liste()).hasSize(2);
        var opp150RevurderingFeriepengerBrukerList = getOppdr150ForFeriepengerForEnMottaker(opp150RevurderingFeriepengerListe, true);
        var opp150RevurdBrukerUtenOpph = opp150RevurderingFeriepengerBrukerList.stream()
            .filter(opp150 -> !opp150.gjelderOpphør())
            .toList();
        assertThat(opp150RevurdBrukerUtenOpph).hasSize(1);
        assertThat(opp150RevurdBrukerUtenOpph.get(0).getSats()).isEqualTo(Sats.på(13000L));
        //Opphør
        var opp150ForOpphListe = opp150RevurderingFeriepengerListe.stream().filter(Oppdragslinje150::gjelderOpphør).toList();
        assertThat(opp150ForOpphListe).hasSize(2).allSatisfy(opp150 -> assertThat(opp150.getDatoVedtakFom().getYear()).isEqualTo(andreFeriepengeår));
        verifiserFeriepengeår(opp150RevurderingFeriepengerListe);
        verifiserOppdr150NårEttFeriepengeårSkalOpphøre(forrigeOpp150FeriepengerListe, opp150RevurderingFeriepengerListe, false);
    }

    /**
     * Forrige behandling: Har feriepenger for år1 - i_År.plusYears(1) og år2 - i_År.plusYears(2);
     * Ny behandling: Har feriepenger for år1 - i_År.plusYears(1);
     * Endring i årsbeløp: erBeløpForskjelligeForFPÅr1 = Nei, erBeløpForskjelligeForFPÅr2 = Ja
     */
    @Test
    void skalSendeKunEnOppdragForOpphørNårFørsteFeriepengeårIkkeEndrerSegOgAndreFeriepengeårOpphørerIRevurdering() {
        //Arrange
        //Førstegangsbehandling
        var originaltOppdrag = opprettBeregningsresultatOgFørstegangsoppdragForFeriepenger(true, 11000L, 10000L);
        //Revurdering
        var beregningsresultatRevurderingFP = oppsettBeregningsresultatForFeriepenger(true, 11000L, 0L);

        var mapper = TilkjentYtelseMapper.lagFor(FamilieYtelseType.FØDSEL);
        var gruppertYtelse2 = mapper.fordelPåNøkler(beregningsresultatRevurderingFP);
        var builder2 = getInputStandardBuilder(gruppertYtelse2).medTidligereOppdrag(mapTidligereOppdrag(List.of(originaltOppdrag)));

        // Act
        var oppdragRevurdering = OppdragMedPositivKvitteringTestUtil.opprett(nyOppdragskontrollTjeneste, builder2.build());

        //Assert
        var forrigeOpp150FeriepengerListe = getOppdragslinje150Feriepenger(originaltOppdrag);
        var opp150RevurderingFeriepengerListe = getOppdragslinje150Feriepenger(oppdragRevurdering);
        //Original behandling
        assertThat(originaltOppdrag.getOppdrag110Liste()).hasSize(2);
        assertThat(forrigeOpp150FeriepengerListe).hasSize(4);

        //Revurdering
        assertThat(oppdragRevurdering.getOppdrag110Liste()).hasSize(2);
        assertThat(opp150RevurderingFeriepengerListe).hasSize(2);
        //Opphør
        var opp150ForOpphListe = opp150RevurderingFeriepengerListe.stream().filter(Oppdragslinje150::gjelderOpphør).toList();
        assertThat(opp150ForOpphListe).hasSize(2);
        verifiserOpp150NårEttFPÅretOpphørerOgAndreIkkeEndrerSeg(opp150RevurderingFeriepengerListe, opp150RevurderingFeriepengerListe, false);
    }

    /**
     * Forrige behandling: Har feriepenger for år1 - i_År.plusYears(1) og år2 - i_År.plusYears(2);
     * Ny behandling: Har feriepenger for år1 - i_År.plusYears(1);
     * Endring i årsbeløp: erBeløpForskjelligeForFPÅr1 = Ja, erBeløpForskjelligeForFPÅr2 = Ja
     */
    @Test
    void skalSendeEnOppdragForOpphørOgEnForEndringNårAndreFeriepengeårEndrerSegOgFørsteFeriepengeårOpphørerIRevurdering() {
        //Arrange
        //Førstegangsbehandling
        var originaltOppdrag = opprettBeregningsresultatOgFørstegangsoppdragForFeriepenger(true, 12000L, 10000L);
        //Revurdering
        var beregningsresultatRevurderingFP = oppsettBeregningsresultatForFeriepenger(true, 0L, 11000L);

        var mapper = TilkjentYtelseMapper.lagFor(FamilieYtelseType.FØDSEL);
        var gruppertYtelse2 = mapper.fordelPåNøkler(beregningsresultatRevurderingFP);
        var builder2 = getInputStandardBuilder(gruppertYtelse2).medTidligereOppdrag(mapTidligereOppdrag(List.of(originaltOppdrag)));

        // Act
        var oppdragRevurdering = OppdragMedPositivKvitteringTestUtil.opprett(nyOppdragskontrollTjeneste, builder2.build());

        //Assert
        var forrigeOpp150FeriepengerListe = getOppdragslinje150Feriepenger(originaltOppdrag);
        assertThat(originaltOppdrag.getOppdrag110Liste()).hasSize(2);
        var opp150RevurderingFeriepengerListe = getOppdragslinje150Feriepenger(oppdragRevurdering);
        int førsteFeriepengeår = FERIEPENGEÅR_LISTE.get(0);

        //Revurdering
        assertThat(oppdragRevurdering.getOppdrag110Liste()).hasSize(2);
        //Arbeidsgiver revurdering
        var opp150RevurderingFeriepengerArbgvrList = getOppdr150ForFeriepengerForEnMottaker(opp150RevurderingFeriepengerListe, false);
        var opp150RevurdArbgvrUtenOpph = opp150RevurderingFeriepengerArbgvrList.stream()
            .filter(opp150 -> !opp150.gjelderOpphør())
            .toList();
        assertThat(opp150RevurdArbgvrUtenOpph).hasSize(1);
        assertThat(opp150RevurdArbgvrUtenOpph.get(0).getSats()).isEqualTo(Sats.på(11000L));
        //Bruker revurdering
        var opp150RevurderingFeriepengerBrukerList = getOppdr150ForFeriepengerForEnMottaker(opp150RevurderingFeriepengerListe, true);
        var opp150RevurdBrukerUtenOpph = opp150RevurderingFeriepengerBrukerList.stream()
            .filter(opp150 -> !opp150.gjelderOpphør())
            .toList();
        assertThat(opp150RevurdBrukerUtenOpph).hasSize(1);
        assertThat(opp150RevurdBrukerUtenOpph.get(0).getSats()).isEqualTo(Sats.på(11000L));
        //Opphør
        var opp150ForOpphListe = opp150RevurderingFeriepengerListe.stream().filter(Oppdragslinje150::gjelderOpphør).toList();
        assertThat(opp150ForOpphListe).hasSize(2).allSatisfy(opp150 -> assertThat(opp150.getDatoVedtakFom().getYear()).isEqualTo(førsteFeriepengeår));
        verifiserFeriepengeår(opp150RevurderingFeriepengerListe);
        verifiserOppdr150NårEttFeriepengeårSkalOpphøre(forrigeOpp150FeriepengerListe, opp150RevurderingFeriepengerListe, true);
    }

    /**
     * Forrige behandling: Har feriepenger for år1 - i_År.plusYears(1) og år2 - i_År.plusYears(2);
     * Ny behandling: Har feriepenger for år1 - i_År.plusYears(1);
     * Endring i årsbeløp: erBeløpForskjelligeForFPÅr1 = Nei, erBeløpForskjelligeForFPÅr2 = Ja
     */
    @Test
    void skalSendeKunEnOppdragForOpphørNårAndreFeriepengeårIkkeEndrerSegOgFørsteFeriepengeårOpphørerIRevurdering() {
        //Arrange
        //Førstegangsbehandling
        var originaltOppdrag = opprettBeregningsresultatOgFørstegangsoppdragForFeriepenger(true, 11000L, 10000L);

        //Revurdering
        var beregningsresultatRevurderingFP = oppsettBeregningsresultatForFeriepenger(true, 0L, 10000L);

        var mapper = TilkjentYtelseMapper.lagFor(FamilieYtelseType.FØDSEL);
        var gruppertYtelse2 = mapper.fordelPåNøkler(beregningsresultatRevurderingFP);
        var builder2 = getInputStandardBuilder(gruppertYtelse2).medTidligereOppdrag(mapTidligereOppdrag(List.of(originaltOppdrag)));

        // Act
        var oppdragRevurdering = OppdragMedPositivKvitteringTestUtil.opprett(nyOppdragskontrollTjeneste, builder2.build());

        //Assert
        //Original behandling
        assertThat(originaltOppdrag.getOppdrag110Liste()).hasSize(2);
        var forrigeOpp150FeriepengerListe = getOppdragslinje150Feriepenger(originaltOppdrag);
        var opp150RevurderingFeriepengerListe = getOppdragslinje150Feriepenger(oppdragRevurdering);

        //Revurdering
        assertThat(oppdragRevurdering.getOppdrag110Liste()).hasSize(2);
        assertThat(opp150RevurderingFeriepengerListe).hasSize(2);
        //Opphør
        var opp150ForOpphListe = opp150RevurderingFeriepengerListe.stream().filter(Oppdragslinje150::gjelderOpphør).toList();
        assertThat(opp150ForOpphListe).hasSize(2);
        verifiserOpp150NårEttFPÅretOpphørerOgAndreIkkeEndrerSeg(forrigeOpp150FeriepengerListe, opp150RevurderingFeriepengerListe, true);
    }

    /**
     * Forrige behandling: Har feriepenger for år1 - i_År.plusYears(1) og år2 - i_År.plusYears(2);
     * Ny behandling: Har ikke feriepenger;
     * Endring i årsbeløp: erBeløpForskjelligeForFPÅr1 = Ja, erBeløpForskjelligeForFPÅr2 = Ja
     */
    @Test
    void skalSendeOpphørPåoriginaltOppdragForFeriepengerNårDetIkkeErFeriepengerIRevurdering() {
        //Arrange
        //Førstegangsbehandling
        var originaltOppdrag = opprettBeregningsresultatOgFørstegangsoppdragForFeriepenger(true, 11000L, 10000L);

        //Revurdering
        var beregningsresultatRevurderingFP = oppsettBeregningsresultatForFeriepenger(false, 0L, 0L);

        var mapper = TilkjentYtelseMapper.lagFor(FamilieYtelseType.FØDSEL);
        var gruppertYtelse2 = mapper.fordelPåNøkler(beregningsresultatRevurderingFP);
        var builder2 = getInputStandardBuilder(gruppertYtelse2).medTidligereOppdrag(mapTidligereOppdrag(List.of(originaltOppdrag)));

        // Act
        var oppdragRevurdering = OppdragMedPositivKvitteringTestUtil.opprett(nyOppdragskontrollTjeneste, builder2.build());

        //Assert
        //Original behandling
        assertThat(originaltOppdrag.getOppdrag110Liste()).hasSize(2);
        var opp150FeriepengerListe = getOppdragslinje150Feriepenger(originaltOppdrag);
        assertThat(opp150FeriepengerListe).allSatisfy(oppdragslinje150 -> {
            assertThat(oppdragslinje150.getRefDelytelseId()).isNull();
            assertThat(oppdragslinje150.getRefFagsystemId()).isNull();
        });
        //Revurdering
        assertThat(oppdragRevurdering.getOppdrag110Liste()).hasSize(2);
        var opp150RevurdFeriepengerListe = getOppdragslinje150Feriepenger(oppdragRevurdering);
        assertThat(opp150RevurdFeriepengerListe).hasSize(4).allSatisfy(oppdragslinje150 -> assertThat(oppdragslinje150.gjelderOpphør()).isTrue());
        for (var opp150Revurd : opp150RevurdFeriepengerListe) {
            assertThat(opp150FeriepengerListe).anySatisfy(
                oppdragslinje150 -> assertThat(oppdragslinje150.getDelytelseId()).isEqualTo(opp150Revurd.getDelytelseId()));
        }
        verifiserFeriepengeår(opp150RevurdFeriepengerListe);
    }

    /**
     * Forrige behandling: Har feriepenger for år1 - i_År.plusYears(1);
     * Ny behandling: Har ikke feriepenger;
     * Endring i årsbeløp: erBeløpForskjelligeForFPÅr1 = Ja, erBeløpForskjelligeForFPÅr2 = N/A
     */
    @Test
    void skalSendeOpphørPåFørsteFeriepengeårNårDetIkkeErFeriepengerIRevurdering() {
        //Arrange
        //Førstegangsbehandling
        var originaltOppdrag = opprettBeregningsresultatOgFørstegangsoppdragForFeriepenger(false, 11000L, 0L);
        //Revurdering
        var beregningsresultatRevurderingFP = oppsettBeregningsresultatForFeriepenger(false, 0L, 0L);

        var mapper = TilkjentYtelseMapper.lagFor(FamilieYtelseType.FØDSEL);
        var gruppertYtelse2 = mapper.fordelPåNøkler(beregningsresultatRevurderingFP);
        var builder2 = getInputStandardBuilder(gruppertYtelse2).medTidligereOppdrag(mapTidligereOppdrag(List.of(originaltOppdrag)));

        // Act
        var oppdragRevurdering = OppdragMedPositivKvitteringTestUtil.opprett(nyOppdragskontrollTjeneste, builder2.build());

        //Assert
        //Original behandling
        assertThat(originaltOppdrag.getOppdrag110Liste()).hasSize(2);
        //Revurdering
        assertThat(oppdragRevurdering.getOppdrag110Liste()).hasSize(2);
        var opp150RevurdFeriepengerListe = getOppdragslinje150Feriepenger(oppdragRevurdering);
        assertThat(opp150RevurdFeriepengerListe).hasSize(2);
        verifiserOppdr150NårDetIkkeErFeriepengerIRevurdering(originaltOppdrag, oppdragRevurdering, true);
    }

    /**
     * Forrige behandling: Har feriepenger for år1 - i_År.plusYears(1);
     * Ny behandling: Har ikke feriepenger;
     * Endring i årsbeløp: erBeløpForskjelligeForFPÅr1 = Ja, erBeløpForskjelligeForFPÅr2 = N/A
     */
    @Test
    void skalSendeOpphørPåAndreFeriepengeårNårDetIkkeErFeriepengerIRevurdering() {
        //Arrange
        //Førstegangsbehandling
        var originaltOppdrag = opprettBeregningsresultatOgFørstegangsoppdragForFeriepenger(false, 0L, 9500L);
        //Revurdering
        var beregningsresultatRevurderingFP = oppsettBeregningsresultatForFeriepenger(false, 0L, 0L);

        var mapper = TilkjentYtelseMapper.lagFor(FamilieYtelseType.FØDSEL);
        var gruppertYtelse2 = mapper.fordelPåNøkler(beregningsresultatRevurderingFP);
        var builder2 = getInputStandardBuilder(gruppertYtelse2).medTidligereOppdrag(mapTidligereOppdrag(List.of(originaltOppdrag)));

        // Act
        var oppdragRevurdering = OppdragMedPositivKvitteringTestUtil.opprett(nyOppdragskontrollTjeneste, builder2.build());

        //Assert
        //Original behandling
        assertThat(originaltOppdrag.getOppdrag110Liste()).hasSize(2);
        //Revurdering
        assertThat(oppdragRevurdering.getOppdrag110Liste()).hasSize(2);
        var opp150RevurdFeriepengerListe = getOppdragslinje150Feriepenger(oppdragRevurdering);
        assertThat(opp150RevurdFeriepengerListe).hasSize(2);
        verifiserOppdr150NårDetIkkeErFeriepengerIRevurdering(originaltOppdrag, oppdragRevurdering, false);
    }

    /**
     * Forrige behandling: Har ikke feriepenger;
     * Ny behandling: Har ikke feriepenger;
     * ErBeløpForskjelligeForFPÅr1 = N/A, erBeløpForskjelligeForFPÅr2 = N/A
     */
    @Test
    void skalIkkeLagesOppdragHvisDetFinnesIngenFeriepengerIFørstegangsbehandlingOgRevurdering() {
        //Arrange
        //Førstegangsbehandling
        var originaltOppdrag = opprettBeregningsresultatOgFørstegangsoppdragForFeriepenger(false, 0L, 0L);
        //Revurdering
        var beregningsresultatRevurderingFP = oppsettBeregningsresultatForFeriepenger(false, 0L, 0L);

        var mapper = TilkjentYtelseMapper.lagFor(FamilieYtelseType.FØDSEL);
        var gruppertYtelse2 = mapper.fordelPåNøkler(beregningsresultatRevurderingFP);
        var builder2 = getInputStandardBuilder(gruppertYtelse2).medTidligereOppdrag(mapTidligereOppdrag(List.of(originaltOppdrag)));

        // Act
        var oppdragRevurdering = nyOppdragskontrollTjeneste.opprettOppdrag(builder2.build());

        //Assert
        assertThat(oppdragRevurdering).isNotPresent();
    }

    @Test
    void skalSendeOppdragForFeriepengerNårDetGjelderAdopsjonStandard() {
        //Arrange
        //Act
        //Førstegangsbehandling
        var oppdrag = opprettBeregningsresultatOgFørstegangsoppdragForFeriepenger(true, false, 11000L, 6000L);

        //Assert
        assertThat(oppdrag.getOppdrag110Liste()).hasSize(2);
        //Bruker
        var oppdrag110Bruker = oppdrag.getOppdrag110Liste()
            .stream()
            .filter(o110 -> !o110.getKodeFagomrade().gjelderRefusjonTilArbeidsgiver())
            .findFirst()
            .get();
        var opp150FeriepengerBruker = getOppdragslinje150Feriepenger(oppdrag110Bruker);
        assertThat(opp150FeriepengerBruker).isNotEmpty()
            .allSatisfy(opp150 -> assertThat(opp150.getKodeKlassifik()).isEqualTo(KodeKlassifik.FPA_FERIEPENGER_BRUKER));
        // Arbeidsgiver
        var oppdrag110Arbeidsgiver = oppdrag.getOppdrag110Liste()
            .stream()
            .filter(o110 -> o110.getKodeFagomrade().gjelderRefusjonTilArbeidsgiver())
            .findFirst()
            .get();
        var opp150FeriepengerArbeidsgiver = getOppdragslinje150Feriepenger(oppdrag110Arbeidsgiver);
        assertThat(opp150FeriepengerArbeidsgiver).isNotEmpty()
            .allSatisfy(opp150 -> assertThat(opp150.getKodeKlassifik()).isEqualTo(KodeKlassifik.FPA_FERIEPENGER_AG));
    }

    @Test
    void skalSendeOppdragForFeriepengerNårDetGjelderAdopsjonStandardOvergang2022() {
        //Arrange
        //Act
        //Førstegangsbehandling
        var baseDato = LocalDate.of(2022, 7,1);
        var oppdrag = opprettBeregningsresultatOgFørstegangsoppdragForFeriepenger(true, false, 11000L, 6000L, baseDato);

        //Assert
        assertThat(oppdrag.getOppdrag110Liste()).hasSize(2);
        //Bruker
        var oppdrag110Bruker = oppdrag.getOppdrag110Liste()
            .stream()
            .filter(o110 -> !o110.getKodeFagomrade().gjelderRefusjonTilArbeidsgiver())
            .findFirst()
            .get();
        var opp150FeriepengerBruker = getOppdragslinje150Feriepenger(oppdrag110Bruker);
        assertThat(opp150FeriepengerBruker)
            .isNotEmpty()
            .satisfiesOnlyOnce(opp150 -> assertThat(opp150.getKodeKlassifik()).isEqualTo(KodeKlassifik.FERIEPENGER_BRUKER)) // Gammel konvensjon for opptjeningsår 2022
            .satisfiesOnlyOnce(opp150 -> assertThat(opp150.getKodeKlassifik()).isEqualTo(KodeKlassifik.FPA_FERIEPENGER_BRUKER)); // Ny konvensjon for opptjeningsår 2023
        // Arbeidsgiver
        var oppdrag110Arbeidsgiver = oppdrag.getOppdrag110Liste()
            .stream()
            .filter(o110 -> o110.getKodeFagomrade().gjelderRefusjonTilArbeidsgiver())
            .findFirst()
            .get();
        var opp150FeriepengerArbeidsgiver = getOppdragslinje150Feriepenger(oppdrag110Arbeidsgiver);
        assertThat(opp150FeriepengerArbeidsgiver).isNotEmpty()
            .allSatisfy(opp150 -> assertThat(opp150.getKodeKlassifik()).isEqualTo(KodeKlassifik.FPA_FERIEPENGER_AG));
    }
}
