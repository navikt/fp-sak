package no.nav.foreldrepenger.økonomistøtte.ny;

import static no.nav.foreldrepenger.økonomistøtte.OppdragskontrollFeriepengerTestUtil.verifiserFeriepengeår;
import static no.nav.foreldrepenger.økonomistøtte.OppdragskontrollFeriepengerTestUtil.verifiserOpp150NårEndringGjelderEttFeriepengeår;
import static no.nav.foreldrepenger.økonomistøtte.OppdragskontrollFeriepengerTestUtil.verifiserOppdr150NårDetErEndringForToFeriepengeår;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.fail;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import no.nav.foreldrepenger.behandlingslager.behandling.beregning.BeregningsresultatEntitet;
import no.nav.foreldrepenger.behandlingslager.økonomioppdrag.FamilieYtelseType;
import no.nav.foreldrepenger.behandlingslager.økonomioppdrag.Oppdragslinje150;
import no.nav.foreldrepenger.behandlingslager.økonomioppdrag.Sats;
import no.nav.foreldrepenger.økonomistøtte.ny.mapper.TilkjentYtelseMapper;

public class NyOppdragskontrollTjenesteFeriepengerMedFlereRevurderingerTest extends NyOppdragskontrollTjenesteTestBase {

    @BeforeEach
    public void setUp() {
        super.setUp();
    }

    @Test
    public void skalIkkeLagesOppdragForFeriepengerPåAndreRevurderingNårDetBlirIngenEndringIÅrsbeløpForBeggeToFeriepengeår() {
        //Arrange
        //Førstegangsbehandling
        var originaltOppdrag = opprettBeregningsresultatOgFørstegangsoppdragForFeriepenger(true, 6000L, 7000L);

        //Revurdering #1
        var beregningsresultatRevurderingFP = oppsettBeregningsresultatForFeriepenger(true, 6000L, 7000L);

        var mapper = TilkjentYtelseMapper.lagFor(FamilieYtelseType.FØDSEL);
        var gruppertYtelse2 = mapper.fordelPåNøkler(beregningsresultatRevurderingFP);
        var builder2 = getInputStandardBuilder(gruppertYtelse2).medTidligereOppdrag(mapTidligereOppdrag(List.of(originaltOppdrag)));

        var oppdragRevurdering = nyOppdragskontrollTjeneste.opprettOppdrag(builder2.build());

        assertThat(oppdragRevurdering).isNotPresent();

        //Revurdering #2
        var beregningsresultatRevurdering2FP = oppsettBeregningsresultatForFeriepenger(true, 6000L, 7000L);

        var gruppertYtelse3 = mapper.fordelPåNøkler(beregningsresultatRevurdering2FP);
        var builder3 = getInputStandardBuilder(gruppertYtelse3).medTidligereOppdrag(mapTidligereOppdrag(List.of(originaltOppdrag)));

        var oppdragRevurdering2 = nyOppdragskontrollTjeneste.opprettOppdrag(builder3.build());
        //Act

        //Assert
        assertThat(oppdragRevurdering2).isNotPresent();
    }

    private BeregningsresultatEntitet oppsettBeregningsresultatForFeriepenger(boolean erOpptjentOverFlereÅr,
                                                                              Long årsbeløp1,
                                                                              Long årsbeløp2) {
        var beregningsresultatRevurderingFP = buildBeregningsresultatFPForVerifiseringAvOpp150MedFeriepenger(erOpptjentOverFlereÅr, årsbeløp1, årsbeløp2);
        return beregningsresultatRevurderingFP;
    }

    @Test
    public void skalLagesOppdragForFeriepengerPåAndreRevurderingNårDetBlirEndringForBeggeToFeriepengeår() {
        //Arrange
        //Førstegangsbehandling
        var originaltOppdrag = opprettBeregningsresultatOgFørstegangsoppdragForFeriepenger(true, 6000L, 7000L);
        //Revurdering #1
        var beregningsresultatRevurderingFP = oppsettBeregningsresultatForFeriepenger(true, 6000L, 7000L);

        var mapper = TilkjentYtelseMapper.lagFor(FamilieYtelseType.FØDSEL);
        var gruppertYtelse2 = mapper.fordelPåNøkler(beregningsresultatRevurderingFP);
        var builder2 = getInputStandardBuilder(gruppertYtelse2).medTidligereOppdrag(mapTidligereOppdrag(List.of(originaltOppdrag)));

        var oppdragRevurdering = nyOppdragskontrollTjeneste.opprettOppdrag(builder2.build());

        assertThat(oppdragRevurdering).isNotPresent();

        //Revurdering #2
        var beregningsresultatRevurdering2FP = oppsettBeregningsresultatForFeriepenger(true, 5999L, 7001L);

        //Act
        var gruppertYtelse3 = mapper.fordelPåNøkler(beregningsresultatRevurdering2FP);
        var builder3 = getInputStandardBuilder(gruppertYtelse3).medTidligereOppdrag(mapTidligereOppdrag(List.of(originaltOppdrag)));

        var oppdragRevurdering2 = nyOppdragskontrollTjeneste.opprettOppdrag(builder3.build());

        if (oppdragRevurdering2.isPresent()) {
            var oppdrag2 = oppdragRevurdering2.get();
            //Assert
            assertThat(oppdrag2.getOppdrag110Liste()).hasSize(2);
            var førstegangsopp150FeriepengerListe = getOppdragslinje150Feriepenger(originaltOppdrag);
            var opp150AndreRevurderingFeriepengerListe = getOppdragslinje150Feriepenger(oppdrag2);
            assertThat(opp150AndreRevurderingFeriepengerListe).hasSize(4);

            for (var førstegangsopp150 : førstegangsopp150FeriepengerListe) {
                assertThat(opp150AndreRevurderingFeriepengerListe).anySatisfy(oppdragslinje150 ->
                    assertThat(oppdragslinje150.getRefDelytelseId()).isEqualTo(førstegangsopp150.getDelytelseId()));
            }
            verifiserOppdr150NårDetErEndringForToFeriepengeår(førstegangsopp150FeriepengerListe, opp150AndreRevurderingFeriepengerListe);
            verifiserFeriepengeår(opp150AndreRevurderingFeriepengerListe);
        } else {
            fail();
        }
    }

    @Test
    public void skalLagesOppdragForFeriepengerPåAndreRevurderingNårDetBlirEndringKunIBeregningForAndreFeriepengeåretOgFørsteRevurderingHarBeregningForBeggeToÅr() {
        //Arrange
        //Førstegangsbehandling
        var originaltOppdrag = opprettBeregningsresultatOgFørstegangsoppdragForFeriepenger(true, 6000L, 7000L);
        //Revurdering #1
        var beregningsresultatRevurderingFP = oppsettBeregningsresultatForFeriepenger(true, 6000L, 7000L);

        var mapper = TilkjentYtelseMapper.lagFor(FamilieYtelseType.FØDSEL);
        var gruppertYtelse2 = mapper.fordelPåNøkler(beregningsresultatRevurderingFP);
        var builder2 = getInputStandardBuilder(gruppertYtelse2).medTidligereOppdrag(mapTidligereOppdrag(List.of(originaltOppdrag)));

        var oppdragRevurdering = nyOppdragskontrollTjeneste.opprettOppdrag(builder2.build());

        assertThat(oppdragRevurdering).isNotPresent();

        //Revurdering #2
        var beregningsresultatRevurdering2FP = oppsettBeregningsresultatForFeriepenger(true, 6000L, 7001L);

        //Act
        var gruppertYtelse3 = mapper.fordelPåNøkler(beregningsresultatRevurdering2FP);
        var builder3 = getInputStandardBuilder(gruppertYtelse3).medTidligereOppdrag(mapTidligereOppdrag(List.of(originaltOppdrag)));

        var oppdragRevurdering2 = nyOppdragskontrollTjeneste.opprettOppdrag(builder3.build());
        if (oppdragRevurdering2.isPresent()) {
            var oppdrag2 = oppdragRevurdering2.get();
            //Assert
            assertThat(oppdrag2.getOppdrag110Liste()).hasSize(2);
            var førstegangsopp150FeriepengerListe = getOppdragslinje150Feriepenger(originaltOppdrag);
            var opp150AndreRevurderingFeriepengerListe = getOppdragslinje150Feriepenger(oppdrag2);
            assertThat(opp150AndreRevurderingFeriepengerListe).hasSize(2);
            verifiserOpp150NårEndringGjelderEttFeriepengeår(førstegangsopp150FeriepengerListe, opp150AndreRevurderingFeriepengerListe, false);
        }
    }

    @Test
    public void skalLagesOppdragForFeriepengerPåAndreRevurderingNårDetBlirEndringKunIBeregningForFørsteFeriepengeåretOgFørsteRevurderingHarBeregningForBeggeToÅr() {
        //Arrange
        //Førstegangsbehandling
        var originaltOppdrag = opprettBeregningsresultatOgFørstegangsoppdragForFeriepenger(true, 6000L, 7000L);
        //Revurdering #1
        var beregningsresultatRevurderingFP = oppsettBeregningsresultatForFeriepenger(true, 6000L, 7000L);

        var mapper = TilkjentYtelseMapper.lagFor(FamilieYtelseType.FØDSEL);
        var gruppertYtelse2 = mapper.fordelPåNøkler(beregningsresultatRevurderingFP);
        var builder2 = getInputStandardBuilder(gruppertYtelse2).medTidligereOppdrag(mapTidligereOppdrag(List.of(originaltOppdrag)));

        var oppdragRevurdering = nyOppdragskontrollTjeneste.opprettOppdrag(builder2.build());

        assertThat(oppdragRevurdering).isNotPresent();

        //Revurdering #2
        var beregningsresultatRevurdering2FP = oppsettBeregningsresultatForFeriepenger(true, 5999L, 7000L);

        //Act
        var gruppertYtelse3 = mapper.fordelPåNøkler(beregningsresultatRevurdering2FP);
        var builder3 = getInputStandardBuilder(gruppertYtelse3).medTidligereOppdrag(mapTidligereOppdrag(List.of(originaltOppdrag)));

        var oppdragRevurdering2 = nyOppdragskontrollTjeneste.opprettOppdrag(builder3.build());

        if (oppdragRevurdering2.isPresent()) {
            var oppdragRevurdering_2 = oppdragRevurdering2.get();
            //Assert
            assertThat(oppdragRevurdering_2.getOppdrag110Liste()).hasSize(2);
            var førstegangsopp150FeriepengerListe = getOppdragslinje150Feriepenger(originaltOppdrag);
            var opp150AndreRevurderingFeriepengerListe = getOppdragslinje150Feriepenger(oppdragRevurdering_2);
            assertThat(opp150AndreRevurderingFeriepengerListe).hasSize(2);
            verifiserOpp150NårEndringGjelderEttFeriepengeår(førstegangsopp150FeriepengerListe, opp150AndreRevurderingFeriepengerListe, true);
        }
    }

    @Test
    public void skalLagesOppdragForFeriepengerPåAndreRevurderingNårBeregningEndrerSegForFørsteFeriepengeårOgFørsteRevurderingHarOpphørPåAndreÅr() {
        //Arrange
        //Førstegangsbehandling
        var originaltOppdrag = opprettBeregningsresultatOgFørstegangsoppdragForFeriepenger(true, 6000L, 7000L);
        //Revurdering #1
        var beregningsresultatRevurderingFP = oppsettBeregningsresultatForFeriepenger(false, 7000L, 0L);

        var mapper = TilkjentYtelseMapper.lagFor(FamilieYtelseType.FØDSEL);
        var gruppertYtelse2 = mapper.fordelPåNøkler(beregningsresultatRevurderingFP);
        var builder2 = getInputStandardBuilder(gruppertYtelse2).medTidligereOppdrag(mapTidligereOppdrag(List.of(originaltOppdrag)));

        var oppdragRevurdering = nyOppdragskontrollTjeneste.opprettOppdrag(builder2.build());

        assertThat(oppdragRevurdering).isPresent();
        var oppdragRevurdering_1 = oppdragRevurdering.get();

        //Revurdering #2
        var beregningsresultatRevurdering2FP = oppsettBeregningsresultatForFeriepenger(true, 5999L, 7001L);

        //Act
        var gruppertYtelse3 = mapper.fordelPåNøkler(beregningsresultatRevurdering2FP);
        var builder3 = getInputStandardBuilder(gruppertYtelse3).medTidligereOppdrag(mapTidligereOppdrag(List.of(originaltOppdrag, oppdragRevurdering_1)));

        var oppdragRevurdering2 = nyOppdragskontrollTjeneste.opprettOppdrag(builder3.build());

        if (oppdragRevurdering2.isPresent()) {
            var oppdragRevurdering_2 = oppdragRevurdering2.get();

            //Assert
            assertThat(oppdragRevurdering_2.getOppdrag110Liste()).hasSize(2);
            var opp150FørsteRevurderingFeriepengerListe = getOppdragslinje150Feriepenger(oppdragRevurdering_1);
            var opp150AndreRevurderingFeriepengerListe = getOppdragslinje150Feriepenger(oppdragRevurdering_2);
            assertThat(opp150AndreRevurderingFeriepengerListe).hasSize(4);
            for (var førsterevurderingopp150 : opp150FørsteRevurderingFeriepengerListe) {
                if (!førsterevurderingopp150.gjelderOpphør()) {
                    assertThat(opp150AndreRevurderingFeriepengerListe).anySatisfy(oppdragslinje150 ->
                        assertThat(oppdragslinje150.getRefDelytelseId()).isEqualTo(førsterevurderingopp150.getDelytelseId()));
                }
            }
            verifiserFeriepengeår(opp150AndreRevurderingFeriepengerListe);
        }
    }

    @Test
    public void skalLagesOppdragForFeriepengerPåAndreRevurderingNårBeregningEndrerSegForFørsteFeriepengeårOgFørsteRevurderingHarOpphørPåFørsteÅr() {
        //Arrange
        //Førstegangsbehandling
        var oppdragskontroll = opprettBeregningsresultatOgFørstegangsoppdragForFeriepenger(true, 6000L, 7000L);
        //Revurdering #1
        var beregningsresultatRevurderingFP = oppsettBeregningsresultatForFeriepenger(false, 0L, 7000L);
        var mapper = TilkjentYtelseMapper.lagFor(FamilieYtelseType.FØDSEL);
        var gruppertYtelse2 = mapper.fordelPåNøkler(beregningsresultatRevurderingFP);
        var builder2 = getInputStandardBuilder(gruppertYtelse2).medTidligereOppdrag(mapTidligereOppdrag(List.of(oppdragskontroll)));

        var oppdragRevurdering = nyOppdragskontrollTjeneste.opprettOppdrag(builder2.build());

        assertThat(oppdragRevurdering).isPresent();
        var oppdragRevurdering_1 = oppdragRevurdering.get();

        //Revurdering #2
        var beregningsresultatRevurdering2FP = oppsettBeregningsresultatForFeriepenger(true, 5999L, 6999L);

        //Act
        var gruppertYtelse3 = mapper.fordelPåNøkler(beregningsresultatRevurdering2FP);
        var builder3 = getInputStandardBuilder(gruppertYtelse3).medTidligereOppdrag(mapTidligereOppdrag(List.of(oppdragskontroll, oppdragRevurdering_1)));

        var oppdragRevurdering2 = nyOppdragskontrollTjeneste.opprettOppdrag(builder3.build());

        if (oppdragRevurdering2.isPresent()) {
            var oppdragRevurdering_2 = oppdragRevurdering2.get();

            //Assert
            assertThat(oppdragRevurdering_2.getOppdrag110Liste()).hasSize(2);
            var opp150FørsteRevurderingFeriepengerListe = getOppdragslinje150Feriepenger(oppdragRevurdering_1);
            var opp150AndreRevurderingFeriepengerListe = getOppdragslinje150Feriepenger(oppdragRevurdering_2);
            assertThat(opp150AndreRevurderingFeriepengerListe).hasSize(4);
            for (var opp150ForFørsteRevurdering : opp150FørsteRevurderingFeriepengerListe) {
                if (!opp150ForFørsteRevurdering.gjelderOpphør())
                    assertThat(opp150AndreRevurderingFeriepengerListe).anySatisfy(oppdragslinje150 ->
                        assertThat(oppdragslinje150.getRefDelytelseId()).isEqualTo(opp150ForFørsteRevurdering.getDelytelseId()));
            }
            verifiserFeriepengeår(opp150AndreRevurderingFeriepengerListe);
        }
    }

    @Test
    public void skalLagesOppdragForFeriepengerPåAndreRevurderingNårDetBlirNyFeriepengerBeregningForFørsteÅrOgIngenEndringForAndreÅr() {
        //Arrange
        //Førstegangsbehandling
        var oppdragskontroll = opprettBeregningsresultatOgFørstegangsoppdragForFeriepenger(true, 6000L, 7000L);
        //Revurdering #1
        var beregningsresultatRevurderingFP = oppsettBeregningsresultatForFeriepenger(false, 0L, 7000L);

        var mapper = TilkjentYtelseMapper.lagFor(FamilieYtelseType.FØDSEL);
        var gruppertYtelse2 = mapper.fordelPåNøkler(beregningsresultatRevurderingFP);
        var builder2 = getInputStandardBuilder(gruppertYtelse2).medTidligereOppdrag(mapTidligereOppdrag(List.of(oppdragskontroll)));

        var oppdragRevurdering = nyOppdragskontrollTjeneste.opprettOppdrag(builder2.build());

        assertThat(oppdragRevurdering).isPresent();
        var oppdragRevurdering_1 = oppdragRevurdering.get();

        //Revurdering #2
        var beregningsresultatRevurdering2FP = oppsettBeregningsresultatForFeriepenger(true, 5999L, 7000L);

        //Act
        var gruppertYtelse3 = mapper.fordelPåNøkler(beregningsresultatRevurdering2FP);
        var builder3 = getInputStandardBuilder(gruppertYtelse3).medTidligereOppdrag(mapTidligereOppdrag(List.of(oppdragskontroll, oppdragRevurdering_1)));

        var oppdragRevurdering2 = nyOppdragskontrollTjeneste.opprettOppdrag(builder3.build());

        if (oppdragRevurdering2.isPresent()) {
            var oppdragRevurdering_2 = oppdragRevurdering2.get();

            //Assert
            assertThat(oppdragRevurdering_2.getOppdrag110Liste()).hasSize(2);
            var opp150AndreRevurderingFeriepengerListe = getOppdragslinje150Feriepenger(oppdragRevurdering_2);
            assertThat(opp150AndreRevurderingFeriepengerListe).hasSize(2);
            assertThat(opp150AndreRevurderingFeriepengerListe).allSatisfy(opp150 -> {
                assertThat(opp150.gjelderOpphør()).isFalse();
                assertThat(opp150.getDatoVedtakFom().getYear()).isEqualTo(FERIEPENGEÅR_LISTE.get(0));
                assertThat(opp150.getSats()).isEqualTo(Sats.på(5999L));
                assertThat(opp150.getRefDelytelseId()).isNotNull();
                assertThat(opp150.getRefFagsystemId()).isNotNull();
            });
        }
    }

    @Test
    public void skalLagesOppdragForFeriepengerPåAndreRevurderingNårDetBlirNyFeriepengerBeregningForAndreÅrOgIngenEndringForFørsteÅr() {
        //Arrange
        //Førstegangsbehandling
        var oppdragskontroll = opprettBeregningsresultatOgFørstegangsoppdragForFeriepenger(true, 6000L, 7000L);
        //Revurdering #1
        var beregningsresultatRevurderingFP = oppsettBeregningsresultatForFeriepenger(false, 7000L, 0L);
        var mapper = TilkjentYtelseMapper.lagFor(FamilieYtelseType.FØDSEL);
        var gruppertYtelse2 = mapper.fordelPåNøkler(beregningsresultatRevurderingFP);
        var builder2 = getInputStandardBuilder(gruppertYtelse2).medTidligereOppdrag(mapTidligereOppdrag(List.of(oppdragskontroll)));

        var oppdragRevurdering = nyOppdragskontrollTjeneste.opprettOppdrag(builder2.build());

        assertThat(oppdragRevurdering).isPresent();
        var oppdragRevurdering_1 = oppdragRevurdering.get();

        //Revurdering #2
        var beregningsresultatRevurdering2FP = oppsettBeregningsresultatForFeriepenger(true, 7000L, 5999L);

        //Act
        var gruppertYtelse3 = mapper.fordelPåNøkler(beregningsresultatRevurdering2FP);
        var builder3 = getInputStandardBuilder(gruppertYtelse3).medTidligereOppdrag(mapTidligereOppdrag(List.of(oppdragskontroll, oppdragRevurdering_1)));

        var oppdragRevurdering2 = nyOppdragskontrollTjeneste.opprettOppdrag(builder3.build());

        if (oppdragRevurdering2.isPresent()) {
            var oppdragRevurdering_2 = oppdragRevurdering2.get();

            //Assert
            assertThat(oppdragRevurdering_2.getOppdrag110Liste()).hasSize(2);
            var opp150AndreRevurderingFeriepengerListe = getOppdragslinje150Feriepenger(oppdragRevurdering_2);
            assertThat(opp150AndreRevurderingFeriepengerListe).hasSize(2);
            assertThat(opp150AndreRevurderingFeriepengerListe).allSatisfy(opp150 -> {
                assertThat(opp150.gjelderOpphør()).isFalse();
                assertThat(opp150.getDatoVedtakFom().getYear()).isEqualTo(FERIEPENGEÅR_LISTE.get(1));
                assertThat(opp150.getSats()).isEqualTo(Sats.på(5999L));
                assertThat(opp150.getRefDelytelseId()).isNotNull();
                assertThat(opp150.getRefFagsystemId()).isNotNull();
            });
        }
    }

    @Test
    public void skalLagesOppdragForFeriepengerPåAndreRevurderingEtterFullstendingOpphørPåBeggeFeriepengeårPåFørsteRevurdering() {
        //Arrange
        //Førstegangsbehandling
        var oppdragskontroll = opprettBeregningsresultatOgFørstegangsoppdragForFeriepenger(true, 6000L, 7000L);
        //Revurdering #1
        var beregningsresultatRevurderingFP = oppsettBeregningsresultatForFeriepenger(false, 0L, 0L);

        var mapper = TilkjentYtelseMapper.lagFor(FamilieYtelseType.FØDSEL);
        var gruppertYtelse2 = mapper.fordelPåNøkler(beregningsresultatRevurderingFP);
        var builder2 = getInputStandardBuilder(gruppertYtelse2).medTidligereOppdrag(mapTidligereOppdrag(List.of(oppdragskontroll)));

        var oppdragRevurdering = nyOppdragskontrollTjeneste.opprettOppdrag(builder2.build());

        assertThat(oppdragRevurdering).isPresent();
        var oppdragRevurdering_1 = oppdragRevurdering.get();

        //Revurdering #2
        var beregningsresultatRevurdering2FP = oppsettBeregningsresultatForFeriepenger(true, 6000L, 7000L);

        //Act
        var gruppertYtelse3 = mapper.fordelPåNøkler(beregningsresultatRevurdering2FP);
        var builder3 = getInputStandardBuilder(gruppertYtelse3).medTidligereOppdrag(mapTidligereOppdrag(List.of(oppdragskontroll, oppdragRevurdering_1)));

        var oppdragRevurdering2 = nyOppdragskontrollTjeneste.opprettOppdrag(builder3.build());

        if (oppdragRevurdering2.isPresent()) {
            var oppdragRevurdering_2 = oppdragRevurdering2.get();

            //Assert
            assertThat(oppdragRevurdering_2.getOppdrag110Liste()).hasSize(2);
            var opp150AndreRevurderingFeriepengerListe = getOppdragslinje150Feriepenger(oppdragRevurdering_2);
            assertThat(opp150AndreRevurderingFeriepengerListe).hasSize(4);
            assertThat(opp150AndreRevurderingFeriepengerListe).allSatisfy(opp150 -> {
                assertThat(opp150.gjelderOpphør()).isFalse();
                assertThat(opp150.getRefDelytelseId()).isNotNull();
                assertThat(opp150.getRefFagsystemId()).isNotNull();
            });
            verifiserFeriepengeår(opp150AndreRevurderingFeriepengerListe);
        }
    }

    @Test
    public void skalLagesOppdragForFeriepengerPåAndreRevurderingSomSenderOpphørPåBeggeFeriepengeår() {
        //Arrange
        //Førstegangsbehandling
        var oppdragskontroll = opprettBeregningsresultatOgFørstegangsoppdragForFeriepenger(true, 6000L, 7000L);
        //Revurdering #1
        var beregningsresultatRevurderingFP = oppsettBeregningsresultatForFeriepenger(true, 6000L, 7000L);

        var mapper = TilkjentYtelseMapper.lagFor(FamilieYtelseType.FØDSEL);
        var gruppertYtelse2 = mapper.fordelPåNøkler(beregningsresultatRevurderingFP);
        var builder2 = getInputStandardBuilder(gruppertYtelse2).medTidligereOppdrag(mapTidligereOppdrag(List.of(oppdragskontroll)));

        var oppdragRevurdering = nyOppdragskontrollTjeneste.opprettOppdrag(builder2.build());

        assertThat(oppdragRevurdering).isNotPresent();

        //Revurdering #2
        var beregningsresultatRevurdering2FP = oppsettBeregningsresultatForFeriepenger(false, 0L, 0L);

        //Act
        var gruppertYtelse3 = mapper.fordelPåNøkler(beregningsresultatRevurdering2FP);
        var builder3 = getInputStandardBuilder(gruppertYtelse3).medTidligereOppdrag(mapTidligereOppdrag(List.of(oppdragskontroll)));

        var oppdragRevurdering2 = nyOppdragskontrollTjeneste.opprettOppdrag(builder3.build());

        if (oppdragRevurdering2.isPresent()) {
            var oppdragRevurdering_2 = oppdragRevurdering2.get();

            //Assert
            assertThat(oppdragRevurdering_2.getOppdrag110Liste()).hasSize(2);
            var opp150ForFørsteOppdragFeriepengerListe = getOppdragslinje150Feriepenger(oppdragskontroll);
            var opp150AndreRevurderingFeriepengerListe = getOppdragslinje150Feriepenger(oppdragRevurdering_2);
            assertThat(opp150AndreRevurderingFeriepengerListe).hasSize(4);
            assertThat(opp150AndreRevurderingFeriepengerListe).allSatisfy(opp150 ->
                assertThat(opp150.gjelderOpphør()).isTrue());
            for (var opp150ForFørsteOppdrag : opp150ForFørsteOppdragFeriepengerListe) {
                assertThat(opp150AndreRevurderingFeriepengerListe).anySatisfy(oppdragslinje150 ->
                    assertThat(oppdragslinje150.getDelytelseId()).isEqualTo(opp150ForFørsteOppdrag.getDelytelseId())
                );
                verifiserFeriepengeår(opp150AndreRevurderingFeriepengerListe);
            }
        }
    }

    @Test
    public void skalLagesOppdragForFeriepengerPåAndreRevurderingSomSenderOpphørPåBeggeFeriepengeårEtterFørsteRevurderingHaddeEndringIÅrsbeløpForBeggeToÅr() {
        //Arrange
        //Førstegangsbehandling
        var oppdragskontroll = opprettBeregningsresultatOgFørstegangsoppdragForFeriepenger(true, 6000L, 7000L);
        //Revurdering #1
        var beregningsresultatRevurderingFP = oppsettBeregningsresultatForFeriepenger(true, 6500L, 7500L);
        var mapper = TilkjentYtelseMapper.lagFor(FamilieYtelseType.FØDSEL);
        var gruppertYtelse2 = mapper.fordelPåNøkler(beregningsresultatRevurderingFP);
        var builder2 = getInputStandardBuilder(gruppertYtelse2).medTidligereOppdrag(mapTidligereOppdrag(List.of(oppdragskontroll)));

        var oppdragRevurdering = nyOppdragskontrollTjeneste.opprettOppdrag(builder2.build());

        assertThat(oppdragRevurdering).isPresent();
        var oppdragRevurdering_1 = oppdragRevurdering.get();
        //Revurdering #2
        var beregningsresultatRevurdering2FP = oppsettBeregningsresultatForFeriepenger(false, 0L, 0L);

        //Act
        var gruppertYtelse3 = mapper.fordelPåNøkler(beregningsresultatRevurdering2FP);
        var builder3 = getInputStandardBuilder(gruppertYtelse3).medTidligereOppdrag(mapTidligereOppdrag(List.of(oppdragskontroll, oppdragRevurdering_1)));

        var oppdragRevurdering2 = nyOppdragskontrollTjeneste.opprettOppdrag(builder3.build());

        if (oppdragRevurdering2.isPresent()) {
            var oppdragRevurdering_2 = oppdragRevurdering2.get();
            //Assert
            assertThat(oppdragRevurdering_2.getOppdrag110Liste()).hasSize(2);
            var opp150FørsteRevurderingFeriepengerListe = getOppdragslinje150Feriepenger(oppdragRevurdering_1);
            var opp150AndreRevurderingFeriepengerListe = getOppdragslinje150Feriepenger(oppdragRevurdering_2);
            assertThat(opp150AndreRevurderingFeriepengerListe).hasSize(4);
            assertThat(opp150AndreRevurderingFeriepengerListe).allSatisfy(opp150 ->
                assertThat(opp150.gjelderOpphør()).isTrue());
            for (var opp150FørsteRevurdering : opp150FørsteRevurderingFeriepengerListe) {
                assertThat(opp150AndreRevurderingFeriepengerListe).anySatisfy(oppdragslinje150 ->
                    assertThat(oppdragslinje150.getDelytelseId()).isEqualTo(opp150FørsteRevurdering.getDelytelseId())
                );
                verifiserFeriepengeår(opp150AndreRevurderingFeriepengerListe);
            }
        }
    }

    @Test
    public void skalLagesOppdragForFeriepengerPåAndreRevurderingSomSenderOpphørPåAndreFeriepengeår() {
        //Arrange
        //Førstegangsbehandling
        var oppdragskontroll = opprettBeregningsresultatOgFørstegangsoppdragForFeriepenger(true, 6000L, 7000L);
        //Revurdering #1
        var beregningsresultatRevurderingFP = oppsettBeregningsresultatForFeriepenger(true, 6200L, 7500L);
        var mapper = TilkjentYtelseMapper.lagFor(FamilieYtelseType.FØDSEL);
        var gruppertYtelse2 = mapper.fordelPåNøkler(beregningsresultatRevurderingFP);
        var builder2 = getInputStandardBuilder(gruppertYtelse2).medTidligereOppdrag(mapTidligereOppdrag(List.of(oppdragskontroll)));

        var oppdragRevurdering = nyOppdragskontrollTjeneste.opprettOppdrag(builder2.build());

        assertThat(oppdragRevurdering).isPresent();
        var oppdragRevurdering_1 = oppdragRevurdering.get();

        //Revurdering #2
        var beregningsresultatRevurdering2FP = oppsettBeregningsresultatForFeriepenger(false, 6200L, 0L);

        //Act
        var gruppertYtelse3 = mapper.fordelPåNøkler(beregningsresultatRevurdering2FP);
        var builder3 = getInputStandardBuilder(gruppertYtelse3).medTidligereOppdrag(mapTidligereOppdrag(List.of(oppdragskontroll, oppdragRevurdering_1)));

        var oppdragRevurdering2 = nyOppdragskontrollTjeneste.opprettOppdrag(builder3.build());

        if (oppdragRevurdering2.isPresent()) {
            var oppdragRevurdering_2 = oppdragRevurdering2.get();

            //Assert
            assertThat(oppdragRevurdering_2.getOppdrag110Liste()).hasSize(2);
            var opp150FørsteRevurderingFeriepengerListe = getOppdragslinje150Feriepenger(oppdragRevurdering_1);
            var opp150AndreRevurderingFeriepengerListe = getOppdragslinje150Feriepenger(oppdragRevurdering_2);
            assertThat(opp150AndreRevurderingFeriepengerListe).hasSize(2);
            assertThat(opp150AndreRevurderingFeriepengerListe).allSatisfy(opp150 -> {
                assertThat(opp150.gjelderOpphør()).isTrue();
                assertThat(opp150.getDatoVedtakFom().getYear()).isEqualTo(FERIEPENGEÅR_LISTE.get(1));
            });
            for (var opp150ForAndreRevurdering : opp150AndreRevurderingFeriepengerListe) {
                assertThat(opp150FørsteRevurderingFeriepengerListe).anySatisfy(oppdragslinje150 ->
                    assertThat(oppdragslinje150.getDelytelseId()).isEqualTo(opp150ForAndreRevurdering.getDelytelseId())
                );
            }
        }
    }

    @Test
    public void skalLagesOppdragForFeriepengerPåAndreRevurderingSomSenderOpphørPåFørsteFeriepengeårNårDetErEndringForDetteFeriepengeåretIFørsteRevurdering() {
        //Arrange
        //Førstegangsbehandling
        var oppdragskontroll = opprettBeregningsresultatOgFørstegangsoppdragForFeriepenger(true, 6000L, 7000L);
        //Revurdering #1
        var beregningsresultatRevurderingFP = oppsettBeregningsresultatForFeriepenger(true, 6200L, 7000L);
        var mapper = TilkjentYtelseMapper.lagFor(FamilieYtelseType.FØDSEL);
        var gruppertYtelse2 = mapper.fordelPåNøkler(beregningsresultatRevurderingFP);
        var builder2 = getInputStandardBuilder(gruppertYtelse2).medTidligereOppdrag(mapTidligereOppdrag(List.of(oppdragskontroll)));

        var oppdragRevurdering = nyOppdragskontrollTjeneste.opprettOppdrag(builder2.build());

        assertThat(oppdragRevurdering).isPresent();
        var oppdragRevurdering_1 = oppdragRevurdering.get();

        //Revurdering #2
        var beregningsresultatRevurdering2FP = oppsettBeregningsresultatForFeriepenger(false, 0L, 7000L);

        //Act
        var gruppertYtelse3 = mapper.fordelPåNøkler(beregningsresultatRevurdering2FP);
        var builder3 = getInputStandardBuilder(gruppertYtelse3).medTidligereOppdrag(mapTidligereOppdrag(List.of(oppdragskontroll, oppdragRevurdering_1)));

        var oppdragRevurdering2 = nyOppdragskontrollTjeneste.opprettOppdrag(builder3.build());

        if (oppdragRevurdering2.isPresent()) {
            var oppdragRevurdering_2 = oppdragRevurdering2.get();

            //Assert
            assertThat(oppdragRevurdering_2.getOppdrag110Liste()).hasSize(2);
            var opp150FørsteRevurderingFeriepengerListe = getOppdragslinje150Feriepenger(oppdragRevurdering_1);
            var opp150AndreRevurderingFeriepengerListe = getOppdragslinje150Feriepenger(oppdragRevurdering_2);
            assertThat(opp150AndreRevurderingFeriepengerListe).hasSize(2);
            assertThat(opp150AndreRevurderingFeriepengerListe).allSatisfy(opp150 -> {
                assertThat(opp150.gjelderOpphør()).isTrue();
                assertThat(opp150.getDatoVedtakFom().getYear()).isEqualTo(FERIEPENGEÅR_LISTE.get(0));
            });
            for (var opp150ForFørsteRevurdering : opp150FørsteRevurderingFeriepengerListe) {
                assertThat(opp150AndreRevurderingFeriepengerListe).anySatisfy(oppdragslinje150 ->
                    assertThat(oppdragslinje150.getDelytelseId()).isEqualTo(opp150ForFørsteRevurdering.getDelytelseId())
                );
            }
        }
    }

    @Test
    public void skalLagesOppdragForFeriepengerPåAndreRevurderingSomSenderOpphørPåAndreFeriepengeårEtterEndringIAndreÅrsbeløpIFørsteRevurdering() {
        //Arrange
        //Førstegangsbehandling
        var oppdragskontroll = opprettBeregningsresultatOgFørstegangsoppdragForFeriepenger(true, 6000L, 7000L);
        //Revurdering #1
        var beregningsresultatRevurderingFP = oppsettBeregningsresultatForFeriepenger(true, 5999L, 7200L);
        var mapper = TilkjentYtelseMapper.lagFor(FamilieYtelseType.FØDSEL);
        var gruppertYtelse2 = mapper.fordelPåNøkler(beregningsresultatRevurderingFP);
        var builder2 = getInputStandardBuilder(gruppertYtelse2).medTidligereOppdrag(mapTidligereOppdrag(List.of(oppdragskontroll)));

        var oppdragRevurdering = nyOppdragskontrollTjeneste.opprettOppdrag(builder2.build());

        assertThat(oppdragRevurdering).isPresent();
        var oppdragRevurdering_1 = oppdragRevurdering.get();

        //Revurdering #2
        var beregningsresultatRevurdering2FP = oppsettBeregningsresultatForFeriepenger(false, 0L, 5999L);

        //Act
        var gruppertYtelse3 = mapper.fordelPåNøkler(beregningsresultatRevurdering2FP);
        var builder3 = getInputStandardBuilder(gruppertYtelse3).medTidligereOppdrag(mapTidligereOppdrag(List.of(oppdragskontroll, oppdragRevurdering_1)));

        var oppdragRevurdering2 = nyOppdragskontrollTjeneste.opprettOppdrag(builder3.build());

        if (oppdragRevurdering2.isPresent()) {
            var oppdragRevurdering_2 = oppdragRevurdering2.get();

            //Assert
            assertThat(oppdragRevurdering_2.getOppdrag110Liste()).hasSize(2);
            var opp150FørsteRevurderingFeriepengerListe = getOppdragslinje150Feriepenger(oppdragRevurdering_1);
            var opp150AndreRevurderingFeriepengerListe = getOppdragslinje150Feriepenger(oppdragRevurdering_2);
            assertThat(opp150AndreRevurderingFeriepengerListe).hasSize(4);
            var opp150FørsteRevurderingAndreÅrListe = opp150FørsteRevurderingFeriepengerListe.stream()
                .filter(opp150 -> opp150.getDatoVedtakFom().getYear() == FERIEPENGEÅR_LISTE.get(1))
                .collect(Collectors.toList());
            var opp150AndreRevurderingFeriepengerOpphList = opp150AndreRevurderingFeriepengerListe.stream()
                .filter(Oppdragslinje150::gjelderOpphør)
                .collect(Collectors.toList());

            assertThat(opp150AndreRevurderingFeriepengerOpphList).hasSize(2);

            for (var opp150AndreRevurderingFeriepenger : opp150AndreRevurderingFeriepengerListe) {
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
    }

    @Test
    public void skalLagesOppdragForFeriepengerPåAndreRevurderingSomSenderOpphørPåAndreFeriepengeårNårDetErEndringForDetteFeriepengeåretIFørsteRevurdering() {
        //Arrange
        //Førstegangsbehandling
        var oppdragskontroll = opprettBeregningsresultatOgFørstegangsoppdragForFeriepenger(true, 6000L, 7000L);
        //Revurdering #1
        var beregningsresultatRevurderingFP = oppsettBeregningsresultatForFeriepenger(true, 7000L, 6200L);
        var mapper = TilkjentYtelseMapper.lagFor(FamilieYtelseType.FØDSEL);
        var gruppertYtelse2 = mapper.fordelPåNøkler(beregningsresultatRevurderingFP);
        var builder2 = getInputStandardBuilder(gruppertYtelse2).medTidligereOppdrag(mapTidligereOppdrag(List.of(oppdragskontroll)));

        var oppdragRevurdering = nyOppdragskontrollTjeneste.opprettOppdrag(builder2.build());

        assertThat(oppdragRevurdering).isPresent();
        var oppdragRevurdering_1 = oppdragRevurdering.get();

        //Revurdering #2
        var beregningsresultatRevurdering2FP = oppsettBeregningsresultatForFeriepenger(false, 6000L, 0L);

        //Act
        var gruppertYtelse3 = mapper.fordelPåNøkler(beregningsresultatRevurdering2FP);
        var builder3 = getInputStandardBuilder(gruppertYtelse3).medTidligereOppdrag(mapTidligereOppdrag(List.of(oppdragskontroll, oppdragRevurdering_1)));

        var oppdragRevurdering2 = nyOppdragskontrollTjeneste.opprettOppdrag(builder3.build());

        if (oppdragRevurdering2.isPresent()) {
            var oppdragRevurdering_2 = oppdragRevurdering2.get();

            //Assert
            assertThat(oppdragRevurdering_2.getOppdrag110Liste()).hasSize(2);
            var opp150FørsteRevurderingFeriepengerListe = getOppdragslinje150Feriepenger(oppdragRevurdering_1);
            var opp150AndreRevurderingFeriepengerListe = getOppdragslinje150Feriepenger(oppdragRevurdering_2);
            assertThat(opp150AndreRevurderingFeriepengerListe).hasSize(4);
            var opp150FørsteRevurderingFørsteÅrListe = opp150FørsteRevurderingFeriepengerListe.stream()
                .filter(opp150 -> opp150.getDatoVedtakFom().getYear() == FERIEPENGEÅR_LISTE.get(0))
                .collect(Collectors.toList());
            var opp150AndreRevurderingFeriepengerOpphList = opp150AndreRevurderingFeriepengerListe.stream()
                .filter(Oppdragslinje150::gjelderOpphør)
                .collect(Collectors.toList());

            assertThat(opp150AndreRevurderingFeriepengerOpphList).hasSize(2);

            for (var opp150AndreRevurderingFeriepenger : opp150AndreRevurderingFeriepengerListe) {
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

}
