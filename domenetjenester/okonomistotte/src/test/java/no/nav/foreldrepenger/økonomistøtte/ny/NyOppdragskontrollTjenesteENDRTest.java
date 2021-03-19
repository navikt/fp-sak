package no.nav.foreldrepenger.økonomistøtte.ny;

import static no.nav.foreldrepenger.økonomistøtte.ny.OppdragskontrollTestVerktøy.endreTilElleveSiffer;
import static no.nav.foreldrepenger.økonomistøtte.ny.OppdragskontrollTestVerktøy.verifiserOppdr150SomErNy;
import static no.nav.foreldrepenger.økonomistøtte.ny.OppdragskontrollTestVerktøy.verifiserOppdr150SomErOpphørt;
import static org.assertj.core.api.Assertions.assertThat;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import org.assertj.core.util.Sets;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

import no.nav.foreldrepenger.behandlingslager.behandling.beregning.AktivitetStatus;
import no.nav.foreldrepenger.behandlingslager.behandling.beregning.BeregningsresultatAndel;
import no.nav.foreldrepenger.behandlingslager.behandling.beregning.BeregningsresultatEntitet;
import no.nav.foreldrepenger.behandlingslager.behandling.beregning.BeregningsresultatFeriepenger;
import no.nav.foreldrepenger.behandlingslager.behandling.beregning.BeregningsresultatPeriode;
import no.nav.foreldrepenger.behandlingslager.behandling.beregning.Inntektskategori;
import no.nav.foreldrepenger.behandlingslager.økonomioppdrag.FamilieYtelseType;
import no.nav.foreldrepenger.behandlingslager.økonomioppdrag.Ompostering116;
import no.nav.foreldrepenger.behandlingslager.økonomioppdrag.Oppdrag110;
import no.nav.foreldrepenger.behandlingslager.økonomioppdrag.Oppdragskontroll;
import no.nav.foreldrepenger.behandlingslager.økonomioppdrag.Oppdragslinje150;
import no.nav.foreldrepenger.behandlingslager.økonomioppdrag.Sats;
import no.nav.foreldrepenger.behandlingslager.økonomioppdrag.koder.KodeEndring;
import no.nav.foreldrepenger.behandlingslager.økonomioppdrag.koder.KodeFagområde;
import no.nav.foreldrepenger.behandlingslager.økonomioppdrag.koder.KodeKlassifik;
import no.nav.foreldrepenger.økonomistøtte.OppdragMedPositivKvitteringTestUtil;
import no.nav.foreldrepenger.økonomistøtte.ny.domene.samlinger.GruppertYtelse;
import no.nav.foreldrepenger.økonomistøtte.ny.mapper.TilkjentYtelseMapper;

public class NyOppdragskontrollTjenesteENDRTest extends NyOppdragskontrollTjenesteTestBase {

    public static final String ANSVARLIG_SAKSBEHANDLER = "Antonina";

    @BeforeEach
    public void setUp() {
        super.setUp();
    }

    @Test
    public void skalSendeEndringsoppdragOppdragMedFeriepengerNårEndringsdatoErFørsteUttaksdag() {
        // Arrange
        BeregningsresultatEntitet beregningsresultat = buildBeregningsresultatFP(true);

        TilkjentYtelseMapper mapper = new TilkjentYtelseMapper(FamilieYtelseType.FØDSEL);
        GruppertYtelse gruppertYtelse = mapper.fordelPåNøkler(beregningsresultat);
        var builder = getInputStandardBuilder(gruppertYtelse);

        Oppdragskontroll originaltOppdrag = OppdragMedPositivKvitteringTestUtil.opprett(nyOppdragskontrollTjeneste, builder.build());
        List<Oppdrag110> originaltOppdrag110Liste = originaltOppdrag.getOppdrag110Liste();
        List<Oppdragslinje150> originaltOppdragslinje150 = OppdragskontrollTestVerktøy.getOppdragslinje150Liste(originaltOppdrag);

        BeregningsresultatEntitet beregningsresultatRevurderingFP = buildBeregningsresultatRevurderingFP(true);

        GruppertYtelse gruppertYtelse2 = mapper.fordelPåNøkler(beregningsresultatRevurderingFP);
        var builder2 = getInputStandardBuilder(gruppertYtelse2).medTidligereOppdrag(mapTidligereOppdrag(List.of(originaltOppdrag)));

        // Act
        Oppdragskontroll oppdragRevurdering = OppdragMedPositivKvitteringTestUtil.opprett(nyOppdragskontrollTjeneste, builder2.build());

        //Assert
        OppdragskontrollTestVerktøy.verifiserAvstemming(oppdragRevurdering);
        verifiserOppdrag110_ENDR(oppdragRevurdering.getOppdrag110Liste(), originaltOppdrag110Liste, true);
        verifiserOppdragslinje150_ENDR(oppdragRevurdering, originaltOppdragslinje150, true, false, 80);
    }

    @Test
    public void skalSendeOppdragMedOmpostering116HvisAvslåttInntrekk() {
        // Arrange
        LocalDate b1fom = LocalDate.of(I_ÅR, 1, 1);
        LocalDate b1tom = LocalDate.of(I_ÅR, 8, 20);
        BeregningsresultatEntitet beregningsresultat = buildBeregningsresultatBrukerFP(null, 400, 400, b1fom, b1tom);

        TilkjentYtelseMapper mapper = new TilkjentYtelseMapper(FamilieYtelseType.FØDSEL);
        GruppertYtelse gruppertYtelse = mapper.fordelPåNøkler(beregningsresultat);
        var builder = getInputStandardBuilder(gruppertYtelse);
        Oppdragskontroll originaltOppdrag = OppdragMedPositivKvitteringTestUtil.opprett(nyOppdragskontrollTjeneste, builder.build());

        BeregningsresultatEntitet beregningsresultatRevurderingFP = buildBeregningsresultatBrukerFP(b1fom, 300, 300, b1fom, b1tom);

        GruppertYtelse gruppertYtelse2 = mapper.fordelPåNøkler(beregningsresultatRevurderingFP);
        var builder2 = getInputStandardBuilder(gruppertYtelse2).medTidligereOppdrag(mapTidligereOppdrag(List.of(originaltOppdrag))).medBrukInntrekk(false);

        // Act
        Oppdragskontroll oppdragRevurdering = OppdragMedPositivKvitteringTestUtil.opprett(nyOppdragskontrollTjeneste, builder2.build());

        // Assert
        Oppdrag110 oppdrag110 = OppdragskontrollTestVerktøy.getOppdrag110ForBruker(oppdragRevurdering.getOppdrag110Liste());
        assertThat(oppdrag110.getKodeEndring()).isEqualTo(KodeEndring.ENDRING);
        assertThat(oppdrag110.getOmpostering116()).isPresent();
        Ompostering116 ompostering116 = oppdrag110.getOmpostering116().get();
        assertThat(ompostering116.getOmPostering()).isFalse();
        assertThat(ompostering116.getDatoOmposterFom()).isNull();
    }

    @Test
    public void skalSendeOppdragMedOmpostering116HvisIkkeAvslåttInntrekkOgDetFinnesForrigeOppdrag() {
        // Arrange
        BeregningsresultatEntitet beregningsresultat = buildBeregningsresultatFP(Optional.empty());
        BeregningsresultatPeriode b1Periode_1 = buildBeregningsresultatPeriode(beregningsresultat, 1, 10);
        buildBeregningsresultatAndel(b1Periode_1, true, 1500, BigDecimal.valueOf(100), virksomhet);
        BeregningsresultatPeriode b1Periode_2 = buildBeregningsresultatPeriode(beregningsresultat, 11, 20);
        buildBeregningsresultatAndel(b1Periode_1, true, 1500, BigDecimal.valueOf(100), virksomhet);
        buildBeregningsresultatAndel(b1Periode_2, false, 1500, BigDecimal.valueOf(100), virksomhet);

        TilkjentYtelseMapper mapper = new TilkjentYtelseMapper(FamilieYtelseType.FØDSEL);
        GruppertYtelse gruppertYtelse = mapper.fordelPåNøkler(beregningsresultat);
        var builder = getInputStandardBuilder(gruppertYtelse);
        Oppdragskontroll originaltOppdrag = OppdragMedPositivKvitteringTestUtil.opprett(nyOppdragskontrollTjeneste, builder.build());

        LocalDate endringsdato = b1Periode_2.getBeregningsresultatPeriodeFom();
        BeregningsresultatEntitet beregningsresultatRevurderingFP = buildBeregningsresultatFP(Optional.of(endringsdato));
        BeregningsresultatPeriode b2Periode_1 = buildBeregningsresultatPeriode(beregningsresultatRevurderingFP, 1, 10);
        buildBeregningsresultatAndel(b2Periode_1, true, 1500, BigDecimal.valueOf(100), virksomhet);
        BeregningsresultatPeriode b2Periode_2 = buildBeregningsresultatPeriode(beregningsresultatRevurderingFP, 11, 20);
        buildBeregningsresultatAndel(b2Periode_2, true, 1200, BigDecimal.valueOf(100), virksomhet);
        buildBeregningsresultatAndel(b2Periode_2, false, 1000, BigDecimal.valueOf(100), virksomhet);

        GruppertYtelse gruppertYtelse2 = mapper.fordelPåNøkler(beregningsresultatRevurderingFP);
        var builder2 = getInputStandardBuilder(gruppertYtelse2).medTidligereOppdrag(mapTidligereOppdrag(List.of(originaltOppdrag)));

        // Act
        Oppdragskontroll oppdragRevurdering = OppdragMedPositivKvitteringTestUtil.opprett(nyOppdragskontrollTjeneste, builder2.build());

        // Assert
        //Bruker
        Oppdrag110 oppdrag110Bruker = OppdragskontrollTestVerktøy.getOppdrag110ForBruker(oppdragRevurdering.getOppdrag110Liste());
        assertThat(oppdrag110Bruker.getKodeEndring()).isEqualTo(KodeEndring.ENDRING);
        assertThat(oppdrag110Bruker.getOmpostering116()).isPresent();
        Ompostering116 ompostering116 = oppdrag110Bruker.getOmpostering116().get();
        assertThat(ompostering116.getOmPostering()).isTrue();
        assertThat(ompostering116.getDatoOmposterFom()).isEqualTo(endringsdato);
        //Arbeidsgiver
        Oppdrag110 oppdrag110Arbeidsgiver = OppdragskontrollTestVerktøy.getOppdrag110ForArbeidsgiver(oppdragRevurdering.getOppdrag110Liste(), virksomhet);
        assertThat(oppdrag110Arbeidsgiver.getKodeEndring()).isEqualTo(KodeEndring.ENDRING);
        assertThat(oppdrag110Arbeidsgiver.getOmpostering116()).isNotPresent();
    }

    @Test
    public void skalSendeOppdragMedOmpostering116OgSetteDatoOmposterFomTilFørsteUttaksdatoFraForrigeBehandlingForBrukerNårEndringsdatoErTidligere() {
        // Arrange
        BeregningsresultatEntitet beregningsresultat = buildBeregningsresultatFP(Optional.empty());
        BeregningsresultatPeriode b1Periode = buildBeregningsresultatPeriode(beregningsresultat, 11, 20);
        buildBeregningsresultatAndel(b1Periode, true, 1500, BigDecimal.valueOf(100), virksomhet);

        TilkjentYtelseMapper mapper = new TilkjentYtelseMapper(FamilieYtelseType.FØDSEL);
        GruppertYtelse gruppertYtelse = mapper.fordelPåNøkler(beregningsresultat);
        var builder = getInputStandardBuilder(gruppertYtelse);
        Oppdragskontroll originaltOppdrag = OppdragMedPositivKvitteringTestUtil.opprett(nyOppdragskontrollTjeneste, builder.build());

        LocalDate endringsdato = b1Periode.getBeregningsresultatPeriodeFom().minusDays(1);
        BeregningsresultatEntitet beregningsresultatRevurderingFP = buildBeregningsresultatFP(Optional.of(endringsdato));
        BeregningsresultatPeriode b2Periode = buildBeregningsresultatPeriode(beregningsresultatRevurderingFP, 10, 20);
        buildBeregningsresultatAndel(b2Periode, true, 1500, BigDecimal.valueOf(100), virksomhet);

        GruppertYtelse gruppertYtelse2 = mapper.fordelPåNøkler(beregningsresultatRevurderingFP);
        var builder2 = getInputStandardBuilder(gruppertYtelse2).medTidligereOppdrag(mapTidligereOppdrag(List.of(originaltOppdrag)));

        // Act
        Oppdragskontroll oppdragRevurdering = OppdragMedPositivKvitteringTestUtil.opprett(nyOppdragskontrollTjeneste, builder2.build());

        //Assert
        Oppdrag110 oppdrag110Bruker = OppdragskontrollTestVerktøy.getOppdrag110ForBruker(oppdragRevurdering.getOppdrag110Liste());
        assertThat(oppdrag110Bruker.getOmpostering116()).isPresent();
        Ompostering116 ompostering116 = oppdrag110Bruker.getOmpostering116().get();
        assertThat(ompostering116.getOmPostering()).isTrue();
        assertThat(ompostering116.getDatoOmposterFom()).isEqualTo(b1Periode.getBeregningsresultatPeriodeFom());
    }

    /**
     * Førstegangsbehandling med en periode <br>
     * Periode 1: Dagsats bruker 400 kr<br>
     * Revurdering med to perioder<br>
     * Periode 1: Dagsats bruker 400 kr<br>
     * Periode 2: Dagsats bruker 300 kr
     */
    @Test
    public void skalSendeOppdragFomEndringsdatoNårDetErEndringFraAndrePeriodeIRevurdering() {
        // Arrange
        LocalDate b1fom = LocalDate.of(I_ÅR, 8, 1);
        LocalDate b1tom = LocalDate.of(I_ÅR, 8, 20);
        BeregningsresultatEntitet beregningsresultat = buildBeregningsresultatBrukerFP(null, 400, 400, b1fom, b1tom);

        TilkjentYtelseMapper mapper = new TilkjentYtelseMapper(FamilieYtelseType.FØDSEL);
        GruppertYtelse gruppertYtelse = mapper.fordelPåNøkler(beregningsresultat);
        var builder = getInputStandardBuilder(gruppertYtelse);
        Oppdragskontroll originaltOppdrag = OppdragMedPositivKvitteringTestUtil.opprett(nyOppdragskontrollTjeneste, builder.build());

        LocalDate endringsdato = beregningsresultat.getBeregningsresultatPerioder().get(0).getBeregningsresultatPeriodeFom().plusDays(10);

        LocalDate b2p1fom = LocalDate.of(I_ÅR, 8, 1);
        LocalDate b2p1tom = LocalDate.of(I_ÅR, 8, 10);
        LocalDate b2p2fom = LocalDate.of(I_ÅR, 8, 11);
        LocalDate b2p2tom = LocalDate.of(I_ÅR, 8, 20);
        BeregningsresultatEntitet beregningsresultatRevurderingFP = buildBeregningsresultatBrukerFP(endringsdato, List.of(400, 300), List.of(400, 300), b2p1fom, b2p1tom, b2p2fom, b2p2tom);

        GruppertYtelse gruppertYtelse2 = mapper.fordelPåNøkler(beregningsresultatRevurderingFP);
        var builder2 = getInputStandardBuilder(gruppertYtelse2).medTidligereOppdrag(mapTidligereOppdrag(List.of(originaltOppdrag)));

        // Act
        Oppdragskontroll oppdragRevurdering = OppdragMedPositivKvitteringTestUtil.opprett(nyOppdragskontrollTjeneste, builder2.build());

        //Assert
        List<Oppdragslinje150> opp150RevurderingListe = oppdragRevurdering.getOppdrag110Liste().stream()
            .flatMap(oppdrag110 -> oppdrag110.getOppdragslinje150Liste().stream())
            .collect(Collectors.toList());

        assertThat(opp150RevurderingListe).hasSize(6);
        assertThat(opp150RevurderingListe).anySatisfy(linjeOpphør -> {
            assertThat(linjeOpphør.gjelderOpphør()).isTrue();
            assertThat(linjeOpphør.getDatoStatusFom()).isEqualTo(endringsdato);
        });
        assertThat(opp150RevurderingListe).anySatisfy(linjeEndring -> {
            assertThat(linjeEndring.gjelderOpphør()).isFalse();
            assertThat(linjeEndring.getSats()).isEqualTo(Sats.på(300));
        });
        List<Oppdragslinje150> opp150RevurderingListeForBruker = getOppdragslinje150ForMottaker(oppdragRevurdering, true);
        assertThat(opp150RevurderingListeForBruker).anySatisfy(feriepenger ->
            assertThat(feriepenger.getKodeKlassifik()).isEqualTo(KodeKlassifik.FERIEPENGER_BRUKER));
        List<Oppdragslinje150> opp150RevurderingListeForArbeidsgiver = getOppdragslinje150ForMottaker(oppdragRevurdering, false);
        assertThat(opp150RevurderingListeForArbeidsgiver).anySatisfy(feriepenger ->
            assertThat(feriepenger.getKodeKlassifik()).isEqualTo(KodeKlassifik.FPF_FERIEPENGER_AG));
    }

    private List<Oppdragslinje150> getOppdragslinje150ForMottaker(Oppdragskontroll oppdragRevurdering, boolean erBruker) {
        return oppdragRevurdering.getOppdrag110Liste()
            .stream()
            .filter(oppdrag110 -> !oppdrag110.getKodeFagomrade().gjelderRefusjonTilArbeidsgiver() == erBruker)
            .flatMap(oppdrag110 -> oppdrag110.getOppdragslinje150Liste().stream())
            .collect(Collectors.toList());
    }

    @Test
    public void skal_sende_oppdrag_hvor_den_første_perioden_i_original_behandling_ikke_har_en_korresponderende_periode() {
        // Arrange
        LocalDate b1fom = LocalDate.of(I_ÅR, 8, 1);
        LocalDate b1tom = LocalDate.of(I_ÅR, 8, 10);
        LocalDate b1p2fom = LocalDate.of(I_ÅR, 8, 11);
        LocalDate b1p2tom = LocalDate.of(I_ÅR, 8, 20);
        BeregningsresultatEntitet beregningsresultat = buildBeregningsresultatBrukerFP(null, List.of(400, 400), List.of(400, 400), b1fom, b1tom, b1p2fom, b1p2tom);

        TilkjentYtelseMapper mapper = new TilkjentYtelseMapper(FamilieYtelseType.FØDSEL);
        GruppertYtelse gruppertYtelse = mapper.fordelPåNøkler(beregningsresultat);
        var builder = getInputStandardBuilder(gruppertYtelse);
        Oppdragskontroll originaltOppdrag = OppdragMedPositivKvitteringTestUtil.opprett(nyOppdragskontrollTjeneste, builder.build());

        LocalDate endringsdato = beregningsresultat.getBeregningsresultatPerioder().get(0).getBeregningsresultatPeriodeFom();

        LocalDate b2p1fom = LocalDate.of(I_ÅR, 8, 11);
        LocalDate b2p1tom = LocalDate.of(I_ÅR, 8, 20);

        BeregningsresultatEntitet beregningsresultatRevurderingFP = buildBeregningsresultatBrukerFP(endringsdato, List.of(400), List.of(400), b2p1fom, b2p1tom);

        GruppertYtelse gruppertYtelse2 = mapper.fordelPåNøkler(beregningsresultatRevurderingFP);
        var builder2 = getInputStandardBuilder(gruppertYtelse2).medTidligereOppdrag(mapTidligereOppdrag(List.of(originaltOppdrag)));

        // Act
        Oppdragskontroll oppdragRevurdering = OppdragMedPositivKvitteringTestUtil.opprett(nyOppdragskontrollTjeneste, builder2.build());

        //Assert
        List<Oppdragslinje150> opp150RevurderingListe = oppdragRevurdering.getOppdrag110Liste().stream()
            .flatMap(oppdrag110 -> oppdrag110.getOppdragslinje150Liste().stream())
            .collect(Collectors.toList());

        assertThat(opp150RevurderingListe).hasSize(6);
        List<Oppdragslinje150> opp150RevurderingListeForBruker = getOppdragslinje150ForMottaker(oppdragRevurdering, true);
        assertThat(opp150RevurderingListeForBruker).hasSize(3);
        List<Oppdragslinje150> opp150RevurderingListeForArbeidsgiver = getOppdragslinje150ForMottaker(oppdragRevurdering, false);
        assertThat(opp150RevurderingListeForArbeidsgiver).hasSize(3);
        assertThat(opp150RevurderingListe).anySatisfy(linjeOpphør -> {
            assertThat(linjeOpphør.gjelderOpphør()).isTrue();
            assertThat(linjeOpphør.getDatoStatusFom()).isEqualTo(endringsdato);
        });
        assertThat(opp150RevurderingListe).anySatisfy(linjeEndring -> {
            assertThat(linjeEndring.gjelderOpphør()).isFalse();
            assertThat(linjeEndring.getSats()).isEqualTo(Sats.på(400));
        });
        assertThat(opp150RevurderingListeForBruker).anySatisfy(feriepenger ->
            assertThat(feriepenger.getKodeKlassifik()).isEqualTo(KodeKlassifik.FERIEPENGER_BRUKER));
        assertThat(opp150RevurderingListeForArbeidsgiver).anySatisfy(feriepenger ->
            assertThat(feriepenger.getKodeKlassifik()).isEqualTo(KodeKlassifik.FPF_FERIEPENGER_AG));
    }

    @Test
    public void skal_sende_oppdrag_hvor_den_siste_perioden_i_revurderingen_ikke_har_en_korresponderende_periode() {
        // Arrange
        LocalDate b1fom = LocalDate.of(I_ÅR, 8, 1);
        LocalDate b1tom = LocalDate.of(I_ÅR, 8, 10);
        BeregningsresultatEntitet beregningsresultat = buildBeregningsresultatBrukerFP(null, List.of(400), List.of(400), b1fom, b1tom);

        TilkjentYtelseMapper mapper = new TilkjentYtelseMapper(FamilieYtelseType.FØDSEL);
        GruppertYtelse gruppertYtelse = mapper.fordelPåNøkler(beregningsresultat);
        var builder = getInputStandardBuilder(gruppertYtelse);
        Oppdragskontroll originaltOppdrag = OppdragMedPositivKvitteringTestUtil.opprett(nyOppdragskontrollTjeneste, builder.build());

        LocalDate endringsdato = LocalDate.of(I_ÅR, 8, 11);

        LocalDate b2p1fom = LocalDate.of(I_ÅR, 8, 1);
        LocalDate b2p1tom = LocalDate.of(I_ÅR, 8, 10);
        LocalDate b2p2fom = LocalDate.of(I_ÅR, 8, 11);
        LocalDate b2p2tom = LocalDate.of(I_ÅR, 8, 20);

        BeregningsresultatEntitet beregningsresultatRevurderingFP = buildBeregningsresultatBrukerFP(endringsdato, List.of(400, 300), List.of(400, 300),
            b2p1fom, b2p1tom, b2p2fom, b2p2tom);

        GruppertYtelse gruppertYtelse2 = mapper.fordelPåNøkler(beregningsresultatRevurderingFP);
        var builder2 = getInputStandardBuilder(gruppertYtelse2).medTidligereOppdrag(mapTidligereOppdrag(List.of(originaltOppdrag)));

        // Act
        Oppdragskontroll oppdragRevurdering = OppdragMedPositivKvitteringTestUtil.opprett(nyOppdragskontrollTjeneste, builder2.build());

        //Assert
        List<Oppdragslinje150> opp150RevurderingListe = oppdragRevurdering.getOppdrag110Liste().stream()
            .flatMap(oppdrag110 -> oppdrag110.getOppdragslinje150Liste().stream())
            .collect(Collectors.toList());

        assertThat(opp150RevurderingListe).hasSize(4);
        List<Oppdragslinje150> opp150RevurderingListeForBruker = getOppdragslinje150ForMottaker(oppdragRevurdering, true);
        assertThat(opp150RevurderingListeForBruker).hasSize(2);
        List<Oppdragslinje150> opp150RevurderingListeForArbeidsgiver = getOppdragslinje150ForMottaker(oppdragRevurdering, false);
        assertThat(opp150RevurderingListeForArbeidsgiver).hasSize(2);
        assertThat(opp150RevurderingListe).anySatisfy(linjeEndring -> {
            assertThat(linjeEndring.gjelderOpphør()).isFalse();
            assertThat(linjeEndring.getDatoVedtakFom()).isEqualTo(endringsdato);
            assertThat(linjeEndring.getSats()).isEqualTo(Sats.på(300));
        });
        assertThat(opp150RevurderingListeForBruker).anySatisfy(feriepenger ->
            assertThat(feriepenger.getKodeKlassifik()).isEqualTo(KodeKlassifik.FERIEPENGER_BRUKER));
        assertThat(opp150RevurderingListeForArbeidsgiver).anySatisfy(feriepenger ->
            assertThat(feriepenger.getKodeKlassifik()).isEqualTo(KodeKlassifik.FPF_FERIEPENGER_AG));
        //Kjeding for bruker
        List<Oppdragslinje150> forrigeOpp150ListeForBruker = getOppdragslinje150ForMottaker(originaltOppdrag, true);
        List<Oppdragslinje150> forrigeOpp150ListeUtenFeriepgForBruker = getOpp150MedKodeklassifik(forrigeOpp150ListeForBruker, KodeKlassifik.FPF_ARBEIDSTAKER);
        Oppdragslinje150 forrigeOpp150ForBruker = forrigeOpp150ListeUtenFeriepgForBruker.get(0);
        List<Oppdragslinje150> opp150RevurderingListeUtenFeriepgForBruker = getOpp150MedKodeklassifik(opp150RevurderingListeForBruker, KodeKlassifik.FPF_ARBEIDSTAKER);
        Oppdragslinje150 opp150RevurderingForBruker = opp150RevurderingListeUtenFeriepgForBruker.get(0);
        assertThat(forrigeOpp150ForBruker.getDelytelseId()).isEqualTo(opp150RevurderingForBruker.getRefDelytelseId());
        //Kjeding for arbeidsgiver
        List<Oppdragslinje150> forrigeOpp150ListeForArbeidsgiver = getOppdragslinje150ForMottaker(originaltOppdrag, false);
        List<Oppdragslinje150> forrigeOpp150ListeUtenFeriepgForArbeidsgiver = getOpp150MedKodeklassifik(forrigeOpp150ListeForArbeidsgiver, KodeKlassifik.FPF_REFUSJON_AG);
        Oppdragslinje150 forrigeOpp150ForArbeidsgiver = forrigeOpp150ListeUtenFeriepgForArbeidsgiver.get(0);
        List<Oppdragslinje150> opp150RevurderingListeUtenFeriepgForArbeidsgiver = getOpp150MedKodeklassifik(opp150RevurderingListeForArbeidsgiver, KodeKlassifik.FPF_REFUSJON_AG);
        Oppdragslinje150 opp150RevurderingForArbeidsgiver = opp150RevurderingListeUtenFeriepgForArbeidsgiver.get(0);
        assertThat(forrigeOpp150ForArbeidsgiver.getDelytelseId()).isEqualTo(opp150RevurderingForArbeidsgiver.getRefDelytelseId());
    }

    /*
     * Førstegangsbehandling med to perioder
     * Periode 1: Dagsats bruker 400 kr; fom-tom: 01.05 - 15.05
     * Periode 2: Dagsats bruker 300 kr; fom-tom: 16.05 - 30.05
     * Revurdering med to perioder
     * Periode 1: Dagsats bruker 400 kr; fom-tom: 01.05 - 15.05
     * Periode 2: Dagsats bruker 200 kr; fom-tom: 16.05 - 30.05
     */
    @Test
    public void skal_sende_oppdrag_når_forrige_og_ny_behanling_har_to_perioder_og_det_blir_endring_i_andel_i_andre_periode_i_revurdering() {

        // Arrange
        LocalDate b1p1fom = LocalDate.of(I_ÅR, 5, 1);
        LocalDate b1p1tom = LocalDate.of(I_ÅR, 5, 15);
        LocalDate b1p2fom = LocalDate.of(I_ÅR, 5, 16);
        LocalDate b1p2tom = LocalDate.of(I_ÅR, 5, 30);
        BeregningsresultatEntitet beregningsresultat = buildBeregningsresultatBrukerFP(null, List.of(400, 300), List.of(400, 300),
            b1p1fom, b1p1tom, b1p2fom, b1p2tom);

        TilkjentYtelseMapper mapper = new TilkjentYtelseMapper(FamilieYtelseType.FØDSEL);
        GruppertYtelse gruppertYtelse = mapper.fordelPåNøkler(beregningsresultat);
        var builder = getInputStandardBuilder(gruppertYtelse);
        Oppdragskontroll originaltOppdrag = OppdragMedPositivKvitteringTestUtil.opprett(nyOppdragskontrollTjeneste, builder.build());

        LocalDate endringsdato = LocalDate.of(I_ÅR, 5, 16);
        LocalDate b2p1fom = LocalDate.of(I_ÅR, 5, 1);
        LocalDate b2p1tom = LocalDate.of(I_ÅR, 5, 15);
        LocalDate b2p2fom = LocalDate.of(I_ÅR, 5, 16);
        LocalDate b2p2tom = LocalDate.of(I_ÅR, 5, 30);
        BeregningsresultatEntitet beregningsresultatRevurderingFP = buildBeregningsresultatBrukerFP(endringsdato, List.of(400, 200), List.of(400, 200),
            b2p1fom, b2p1tom, b2p2fom, b2p2tom);

        GruppertYtelse gruppertYtelse2 = mapper.fordelPåNøkler(beregningsresultatRevurderingFP);
        var builder2 = getInputStandardBuilder(gruppertYtelse2).medTidligereOppdrag(mapTidligereOppdrag(List.of(originaltOppdrag)));

        // Act
        Oppdragskontroll oppdragRevurdering = OppdragMedPositivKvitteringTestUtil.opprett(nyOppdragskontrollTjeneste, builder2.build());

        // Assert
        List<Oppdragslinje150> opp150RevurderingListe = oppdragRevurdering.getOppdrag110Liste().stream()
            .flatMap(oppdrag110 -> oppdrag110.getOppdragslinje150Liste().stream())
            .collect(Collectors.toList());

        assertThat(opp150RevurderingListe).hasSize(4);
        List<Oppdragslinje150> opp150RevurderingListeForBruker = getOppdragslinje150ForMottaker(oppdragRevurdering, true);
        assertThat(opp150RevurderingListeForBruker).hasSize(2);
        List<Oppdragslinje150> opp150RevurderingListeForArbeidsgiver = getOppdragslinje150ForMottaker(oppdragRevurdering, false);
        assertThat(opp150RevurderingListeForArbeidsgiver).hasSize(2);
        assertThat(opp150RevurderingListe).anySatisfy(linjeOpphør -> {
            assertThat(linjeOpphør.gjelderOpphør()).isTrue();
            assertThat(linjeOpphør.getDatoStatusFom()).isEqualTo(endringsdato);
        });
        assertThat(opp150RevurderingListe).anySatisfy(linjeEndring -> {
            assertThat(linjeEndring.gjelderOpphør()).isFalse();
            assertThat(linjeEndring.getSats()).isEqualTo(Sats.på(200));
        });

        assertThat(opp150RevurderingListeForBruker).allSatisfy(oppdragslinje150 ->
            assertThat(oppdragslinje150.getKodeKlassifik()).isEqualTo(KodeKlassifik.FPF_ARBEIDSTAKER));
        assertThat(opp150RevurderingListeForArbeidsgiver).allSatisfy(oppdragslinje150 ->
            assertThat(oppdragslinje150.getKodeKlassifik()).isEqualTo(KodeKlassifik.FPF_REFUSJON_AG));
    }


    /*
     * Førstegangsbehandling med to perioder
     * Periode 1: Dagsats bruker 400 kr; fom-tom: 01.05 - 15.05
     * Periode 2: Dagsats bruker 300 kr; fom-tom: 16.05 - 30.05
     * Revurdering med to perioder
     * Periode 1: Dagsats bruker 400 kr; fom-tom: 01.05 - 10.05
     * Periode 2: Dagsats bruker 200 kr; fom-tom: 11.05 - 30.05
     */
    @Test
    public void skal_sende_oppdrag_når_forrige_og_ny_behandling_har_to_perioder_og_det_blir_endring_midt_i_første_periode_i_forrige() {

        // Arrange
        LocalDate b1p1fom = LocalDate.of(I_ÅR, 5, 1);
        LocalDate b1p1tom = LocalDate.of(I_ÅR, 5, 15);
        LocalDate b1p2fom = LocalDate.of(I_ÅR, 5, 16);
        LocalDate b1p2tom = LocalDate.of(I_ÅR, 5, 30);
        BeregningsresultatEntitet beregningsresultat = buildBeregningsresultatBrukerFP(null, List.of(400, 300), List.of(400, 300),
            b1p1fom, b1p1tom, b1p2fom, b1p2tom);
        TilkjentYtelseMapper mapper = new TilkjentYtelseMapper(FamilieYtelseType.FØDSEL);
        GruppertYtelse gruppertYtelse = mapper.fordelPåNøkler(beregningsresultat);
        var builder = getInputStandardBuilder(gruppertYtelse);
        Oppdragskontroll originaltOppdrag = OppdragMedPositivKvitteringTestUtil.opprett(nyOppdragskontrollTjeneste, builder.build());

        LocalDate endringsdato = LocalDate.of(I_ÅR, 5, 11);
        LocalDate b2p1fom = LocalDate.of(I_ÅR, 5, 1);
        LocalDate b2p1tom = LocalDate.of(I_ÅR, 5, 10);
        LocalDate b2p2fom = LocalDate.of(I_ÅR, 5, 11);
        LocalDate b2p2tom = LocalDate.of(I_ÅR, 5, 30);
        BeregningsresultatEntitet beregningsresultatRevurderingFP = buildBeregningsresultatBrukerFP(endringsdato, List.of(400, 200), List.of(400, 200),
            b2p1fom, b2p1tom, b2p2fom, b2p2tom);
        GruppertYtelse gruppertYtelse2 = mapper.fordelPåNøkler(beregningsresultatRevurderingFP);
        var builder2 = getInputStandardBuilder(gruppertYtelse2).medTidligereOppdrag(mapTidligereOppdrag(List.of(originaltOppdrag)));

        // Act
        Oppdragskontroll oppdragRevurdering = OppdragMedPositivKvitteringTestUtil.opprett(nyOppdragskontrollTjeneste, builder2.build());

        // Assert
        List<Oppdragslinje150> opp150RevurderingListe = oppdragRevurdering.getOppdrag110Liste().stream()
            .flatMap(oppdrag110 -> oppdrag110.getOppdragslinje150Liste().stream())
            .collect(Collectors.toList());

        assertThat(opp150RevurderingListe).hasSize(4);
        List<Oppdragslinje150> opp150RevurderingListeForBruker = getOppdragslinje150ForMottaker(oppdragRevurdering, true);
        assertThat(opp150RevurderingListeForBruker).hasSize(2);
        List<Oppdragslinje150> opp150RevurderingListeForArbeidsgiver = getOppdragslinje150ForMottaker(oppdragRevurdering, false);
        assertThat(opp150RevurderingListeForArbeidsgiver).hasSize(2);
        assertThat(opp150RevurderingListe).anySatisfy(linjeOpphør -> {
            assertThat(linjeOpphør.gjelderOpphør()).isTrue();
            assertThat(linjeOpphør.getDatoStatusFom()).isEqualTo(endringsdato);
        });
        assertThat(opp150RevurderingListe).anySatisfy(linjeEndring -> {
            assertThat(linjeEndring.gjelderOpphør()).isFalse();
            assertThat(linjeEndring.getSats()).isEqualTo(Sats.på(200));
        });
        assertThat(opp150RevurderingListeForBruker).allSatisfy(oppdragslinje150 ->
            assertThat(oppdragslinje150.getKodeKlassifik()).isEqualTo(KodeKlassifik.FPF_ARBEIDSTAKER));
        assertThat(opp150RevurderingListeForArbeidsgiver).allSatisfy(oppdragslinje150 ->
            assertThat(oppdragslinje150.getKodeKlassifik()).isEqualTo(KodeKlassifik.FPF_REFUSJON_AG));
    }

    /*
     * Førstegangsbehandling med to perioder
     * Periode 1: Dagsats bruker 400 kr; fom-tom: 01.05 - 15.05
     * Periode 2: Dagsats bruker 300 kr; fom-tom: 16.05 - 30.05
     * Revurdering med to perioder
     * Periode 1: Dagsats bruker 400 kr; fom-tom: 01.05 - 20.05
     * Periode 2: Dagsats bruker 200 kr; fom-tom: 21.05 - 30.05
     */
    @Test
    public void skal_sende_oppdrag_når_forrige_og_ny_behanling_har_to_perioder_og_andre_periode_i_original_behandlingen_blir_til_2_perioder_i_revurdering() {

        // Arrange
        LocalDate b1p1fom = LocalDate.of(I_ÅR, 5, 1);
        LocalDate b1p1tom = LocalDate.of(I_ÅR, 5, 15);
        LocalDate b1p2fom = LocalDate.of(I_ÅR, 5, 16);
        LocalDate b1p2tom = LocalDate.of(I_ÅR, 5, 30);
        BeregningsresultatEntitet beregningsresultat = buildBeregningsresultatBrukerFP(null, List.of(400, 300), List.of(400, 300),
            b1p1fom, b1p1tom, b1p2fom, b1p2tom);
        TilkjentYtelseMapper mapper = new TilkjentYtelseMapper(FamilieYtelseType.FØDSEL);
        GruppertYtelse gruppertYtelse = mapper.fordelPåNøkler(beregningsresultat);
        var builder = getInputStandardBuilder(gruppertYtelse);
        Oppdragskontroll originaltOppdrag = OppdragMedPositivKvitteringTestUtil.opprett(nyOppdragskontrollTjeneste, builder.build());

        LocalDate endringsdato = LocalDate.of(I_ÅR, 5, 21);
        LocalDate b2p1fom = LocalDate.of(I_ÅR, 5, 1);
        LocalDate b2p1tom = LocalDate.of(I_ÅR, 5, 20);
        LocalDate b2p2fom = LocalDate.of(I_ÅR, 5, 21);
        LocalDate b2p2tom = LocalDate.of(I_ÅR, 5, 30);

        BeregningsresultatEntitet beregningsresultatRevurderingFP = buildBeregningsresultatBrukerFP(endringsdato, List.of(400, 200), List.of(400, 200),
            b2p1fom, b2p1tom, b2p2fom, b2p2tom);
        GruppertYtelse gruppertYtelse2 = mapper.fordelPåNøkler(beregningsresultatRevurderingFP);
        var builder2 = getInputStandardBuilder(gruppertYtelse2).medTidligereOppdrag(mapTidligereOppdrag(List.of(originaltOppdrag)));

        // Act
        Oppdragskontroll oppdragRevurdering = OppdragMedPositivKvitteringTestUtil.opprett(nyOppdragskontrollTjeneste, builder2.build());

        // Assert
        List<Oppdragslinje150> opp150RevurderingListe = oppdragRevurdering.getOppdrag110Liste().stream()
            .flatMap(oppdrag110 -> oppdrag110.getOppdragslinje150Liste().stream())
            .collect(Collectors.toList());

        assertThat(opp150RevurderingListe).hasSize(6);
        List<Oppdragslinje150> opp150RevurderingListeForBruker = getOppdragslinje150ForMottaker(oppdragRevurdering, true);
        assertThat(opp150RevurderingListeForBruker).hasSize(3);
        List<Oppdragslinje150> opp150RevurderingListeForArbeidsgiver = getOppdragslinje150ForMottaker(oppdragRevurdering, false);
        assertThat(opp150RevurderingListeForArbeidsgiver).hasSize(3);
        assertThat(opp150RevurderingListe).anySatisfy(linjeOpphør -> {
            assertThat(linjeOpphør.gjelderOpphør()).isTrue();
            assertThat(linjeOpphør.getDatoStatusFom()).isEqualTo(b1p2fom);
        });
        assertThat(opp150RevurderingListe).anySatisfy(linjeEndring -> {
            assertThat(linjeEndring.gjelderOpphør()).isFalse();
            assertThat(linjeEndring.getSats()).isEqualTo(Sats.på(200));
        });
        assertThat(opp150RevurderingListeForBruker).allSatisfy(oppdragslinje150 ->
            assertThat(oppdragslinje150.getKodeKlassifik()).isEqualTo(KodeKlassifik.FPF_ARBEIDSTAKER));
        assertThat(opp150RevurderingListeForArbeidsgiver).allSatisfy(oppdragslinje150 ->
            assertThat(oppdragslinje150.getKodeKlassifik()).isEqualTo(KodeKlassifik.FPF_REFUSJON_AG));
    }

    /*
     * Førstegangsbehandling med to perioder
     * Periode 1: Dagsats bruker 400 kr; fom-tom: 01.05 - 15.05
     * Periode 2: Dagsats bruker 300 kr; fom-tom: 16.05 - 30.05
     * Revurdering med to perioder
     * Periode 1: Dagsats bruker 400 kr; fom-tom: 20.04 - 20.05
     * Periode 2: Dagsats bruker 200 kr; fom-tom: 21.05 - 10.06
     */
    @Test
    public void skal_opphøre_hele_forrige_oppdrag_og_sende_ny_oppdrag_når_første_uttaksdato_av_revurdering_blir_tidligere_enn_første_uttaksdato_av_forrige() {

        // Arrange
        LocalDate b1p1fom = LocalDate.of(I_ÅR, 5, 1);
        LocalDate b1p1tom = LocalDate.of(I_ÅR, 5, 15);
        LocalDate b1p2fom = LocalDate.of(I_ÅR, 5, 16);
        LocalDate b1p2tom = LocalDate.of(I_ÅR, 5, 30);
        BeregningsresultatEntitet beregningsresultat = buildBeregningsresultatBrukerFP(null, List.of(400, 300), List.of(400, 300),
            b1p1fom, b1p1tom, b1p2fom, b1p2tom);
        TilkjentYtelseMapper mapper = new TilkjentYtelseMapper(FamilieYtelseType.FØDSEL);
        GruppertYtelse gruppertYtelse = mapper.fordelPåNøkler(beregningsresultat);
        var builder = getInputStandardBuilder(gruppertYtelse);
        Oppdragskontroll originaltOppdrag = OppdragMedPositivKvitteringTestUtil.opprett(nyOppdragskontrollTjeneste, builder.build());

        LocalDate endringsdato = LocalDate.of(I_ÅR, 4, 20);
        LocalDate b2p1fom = LocalDate.of(I_ÅR, 4, 20);
        LocalDate b2p1tom = LocalDate.of(I_ÅR, 5, 20);
        LocalDate b2p2fom = LocalDate.of(I_ÅR, 5, 21);
        LocalDate b2p2tom = LocalDate.of(I_ÅR, 6, 10);
        BeregningsresultatEntitet beregningsresultatRevurderingFP = buildBeregningsresultatBrukerFP(endringsdato, List.of(400, 200), List.of(400, 200),
            b2p1fom, b2p1tom, b2p2fom, b2p2tom);

        GruppertYtelse gruppertYtelse2 = mapper.fordelPåNøkler(beregningsresultatRevurderingFP);
        var builder2 = getInputStandardBuilder(gruppertYtelse2).medTidligereOppdrag(mapTidligereOppdrag(List.of(originaltOppdrag)));

        // Act
        Oppdragskontroll oppdragRevurdering = OppdragMedPositivKvitteringTestUtil.opprett(nyOppdragskontrollTjeneste, builder2.build());

        // Assert
        List<Oppdragslinje150> opp150RevurderingListe = oppdragRevurdering.getOppdrag110Liste().stream()
            .flatMap(oppdrag110 -> oppdrag110.getOppdragslinje150Liste().stream())
            .collect(Collectors.toList());

        assertThat(opp150RevurderingListe).hasSize(6);
        List<Oppdragslinje150> opp150RevurderingListeForBruker = getOppdragslinje150ForMottaker(oppdragRevurdering, true);
        assertThat(opp150RevurderingListeForBruker).hasSize(3);
        List<Oppdragslinje150> opp150RevurderingListeForArbeidsgiver = getOppdragslinje150ForMottaker(oppdragRevurdering, false);
        assertThat(opp150RevurderingListeForArbeidsgiver).hasSize(3);
        LocalDate forventetDatoStatusFom = beregningsresultat.getBeregningsresultatPerioder().get(0).getBeregningsresultatPeriodeFom();
        assertThat(opp150RevurderingListe).anySatisfy(linjeOpphør -> {
            assertThat(linjeOpphør.gjelderOpphør()).isTrue();
            assertThat(linjeOpphør.getDatoStatusFom()).isEqualTo(forventetDatoStatusFom);
        });
        assertThat(opp150RevurderingListe).anySatisfy(linjeEndring -> {
            assertThat(linjeEndring.gjelderOpphør()).isFalse();
            assertThat(linjeEndring.getSats()).isEqualTo(Sats.på(400));
        });
        assertThat(opp150RevurderingListe).anySatisfy(linjeEndring -> {
            assertThat(linjeEndring.gjelderOpphør()).isFalse();
            assertThat(linjeEndring.getSats()).isEqualTo(Sats.på(200));
        });
        assertThat(opp150RevurderingListeForBruker).allSatisfy(oppdragslinje150 ->
            assertThat(oppdragslinje150.getKodeKlassifik()).isEqualTo(KodeKlassifik.FPF_ARBEIDSTAKER));
        assertThat(opp150RevurderingListeForArbeidsgiver).allSatisfy(oppdragslinje150 ->
            assertThat(oppdragslinje150.getKodeKlassifik()).isEqualTo(KodeKlassifik.FPF_REFUSJON_AG));
    }

    @Test
    public void skal_sende_oppdrag_hvor_det_blir_en_ny_mottaker_i_revurdering() {
        // Arrange
        LocalDate b1fom = LocalDate.of(I_ÅR, 8, 1);
        LocalDate b1tom = LocalDate.of(I_ÅR, 8, 20);
        BeregningsresultatEntitet beregningsresultat = buildBeregningsresultatBrukerFP(null, List.of(400), List.of(0), b1fom, b1tom);
        TilkjentYtelseMapper mapper = new TilkjentYtelseMapper(FamilieYtelseType.FØDSEL);
        GruppertYtelse gruppertYtelse = mapper.fordelPåNøkler(beregningsresultat);
        var builder = getInputStandardBuilder(gruppertYtelse);
        Oppdragskontroll originaltOppdrag = OppdragMedPositivKvitteringTestUtil.opprett(nyOppdragskontrollTjeneste, builder.build());

        LocalDate endringsdato = LocalDate.of(I_ÅR, 8, 11);

        LocalDate b2p1fom = LocalDate.of(I_ÅR, 8, 1);
        LocalDate b2p1tom = LocalDate.of(I_ÅR, 8, 10);
        LocalDate b2p2fom = LocalDate.of(I_ÅR, 8, 11);
        LocalDate b2p2tom = LocalDate.of(I_ÅR, 8, 20);

        BeregningsresultatEntitet beregningsresultatRevurderingFP = buildBeregningsresultatBrukerFP(endringsdato, List.of(400, 0), List.of(0, 400), b2p1fom, b2p1tom, b2p2fom, b2p2tom);
        GruppertYtelse gruppertYtelse2 = mapper.fordelPåNøkler(beregningsresultatRevurderingFP);
        var builder2 = getInputStandardBuilder(gruppertYtelse2).medTidligereOppdrag(mapTidligereOppdrag(List.of(originaltOppdrag)));

        // Act
        Oppdragskontroll oppdragRevurdering = OppdragMedPositivKvitteringTestUtil.opprett(nyOppdragskontrollTjeneste, builder2.build());

        //Assert
        List<Oppdrag110> oppdrag110Liste = oppdragRevurdering.getOppdrag110Liste();
        assertThat(oppdrag110Liste).hasSize(2);
        Oppdrag110 oppdrag110Bruker = OppdragskontrollTestVerktøy.getOppdrag110ForBruker(oppdrag110Liste);
        assertThat(oppdrag110Bruker.getKodeEndring()).isEqualTo(KodeEndring.ENDRING);
        Oppdrag110 oppdrag110Arbeidsgiver = OppdragskontrollTestVerktøy.getOppdrag110ForArbeidsgiver(oppdrag110Liste, virksomhet);
        assertThat(oppdrag110Arbeidsgiver.getKodeEndring()).isEqualTo(KodeEndring.NY);

        List<Oppdragslinje150> opp150RevurderingListe = oppdragRevurdering.getOppdrag110Liste().stream()
            .flatMap(oppdrag110 -> oppdrag110.getOppdragslinje150Liste().stream())
            .collect(Collectors.toList());

        assertThat(opp150RevurderingListe).hasSize(3);
        assertThat(opp150RevurderingListe).anySatisfy(linjeOpphør -> {
            assertThat(linjeOpphør.gjelderOpphør()).isTrue();
            assertThat(linjeOpphør.getDatoStatusFom()).isEqualTo(endringsdato);
        });
        assertThat(opp150RevurderingListe).anySatisfy(linjeEndring -> {
            assertThat(linjeEndring.gjelderOpphør()).isFalse();
            assertThat(linjeEndring.getSats()).isEqualTo(Sats.på(400));
            assertThat(linjeEndring.getKodeKlassifik()).isEqualTo(KodeKlassifik.FPF_REFUSJON_AG);
        });
        List<Oppdragslinje150> opp150RevurderingListeForArbeidsgiver = getOppdragslinje150ForMottaker(oppdragRevurdering, false);
        assertThat(opp150RevurderingListeForArbeidsgiver).anySatisfy(oppdragslinje150 ->
            assertThat(oppdragslinje150.getKodeKlassifik()).isEqualTo(KodeKlassifik.FPF_FERIEPENGER_AG));
    }

    @Test
    public void skalSendeEndringsoppdragNårEndringsdatoErMidtIFørstePeriodeIRevurderingOgDetErFlereMottakereSomBrukerOgArbeidsgiver() {
        // Arrange
        BeregningsresultatEntitet beregningsresultat = buildBeregningsresultatFP();

        TilkjentYtelseMapper mapper = new TilkjentYtelseMapper(FamilieYtelseType.FØDSEL);
        GruppertYtelse gruppertYtelse = mapper.fordelPåNøkler(beregningsresultat);
        var builder = getInputStandardBuilder(gruppertYtelse);
        Oppdragskontroll originaltOppdrag = OppdragMedPositivKvitteringTestUtil.opprett(nyOppdragskontrollTjeneste, builder.build());

        List<Oppdrag110> originaltOppdrag110Liste = originaltOppdrag.getOppdrag110Liste();
        List<Oppdragslinje150> originaltOppdragslinje150 = OppdragskontrollTestVerktøy.getOppdragslinje150Liste(originaltOppdrag);

        LocalDate endringsdato = beregningsresultat.getBeregningsresultatPerioder().get(0).getBeregningsresultatPeriodeFom().plusDays(5);
        BeregningsresultatEntitet beregningsresultatRevurderingFP = buildBeregningsresultatRevurderingFP(false);

        GruppertYtelse gruppertYtelse2 = mapper.fordelPåNøkler(beregningsresultatRevurderingFP);
        var builder2 = getInputStandardBuilder(gruppertYtelse2).medTidligereOppdrag(mapTidligereOppdrag(List.of(originaltOppdrag)));

        // Act
        Oppdragskontroll oppdragRevurdering = OppdragMedPositivKvitteringTestUtil.opprett(nyOppdragskontrollTjeneste, builder2.build());

        //Assert
        OppdragskontrollTestVerktøy.verifiserAvstemming(oppdragRevurdering);
        verifiserOppdrag110_ENDR(oppdragRevurdering.getOppdrag110Liste(), originaltOppdrag110Liste, false);
        verifiserOppdragslinje150_ENDR(oppdragRevurdering, originaltOppdragslinje150, false, false, 80);
    }

    @Test
    public void skalOppretteEndringsoppdragNårBehandlingsresultatErInnvilgetOgForrigeOppdragEksisterer() {
        // Arrange
        BeregningsresultatEntitet beregningsresultat = buildBeregningsresultatFP(true);
        TilkjentYtelseMapper mapper = new TilkjentYtelseMapper(FamilieYtelseType.FØDSEL);
        GruppertYtelse gruppertYtelse = mapper.fordelPåNøkler(beregningsresultat);
        var builder = getInputStandardBuilder(gruppertYtelse);
        Oppdragskontroll originaltOppdrag = OppdragMedPositivKvitteringTestUtil.opprett(nyOppdragskontrollTjeneste, builder.build());
        List<Oppdrag110> originaltOppdrag110Liste = originaltOppdrag.getOppdrag110Liste();
        List<Oppdragslinje150> originaltOppdragslinje150 = OppdragskontrollTestVerktøy.getOppdragslinje150Liste(originaltOppdrag);

        LocalDate endringsdato = beregningsresultat.getBeregningsresultatPerioder().get(0).getBeregningsresultatPeriodeFom();
        BeregningsresultatEntitet beregningsresultatRevurderingFP = buildBeregningsresultatRevurderingFP(true);
        GruppertYtelse gruppertYtelse2 = mapper.fordelPåNøkler(beregningsresultatRevurderingFP);
        var builder2 = getInputStandardBuilder(gruppertYtelse2).medTidligereOppdrag(mapTidligereOppdrag(List.of(originaltOppdrag)));

        // Act
        Oppdragskontroll oppdragRevurdering = OppdragMedPositivKvitteringTestUtil.opprett(nyOppdragskontrollTjeneste, builder2.build());

        //Assert
        OppdragskontrollTestVerktøy.verifiserAvstemming(oppdragRevurdering);
        verifiserOppdrag110_ENDR(oppdragRevurdering.getOppdrag110Liste(), originaltOppdrag110Liste, true);
        verifiserOppdragslinje150_ENDR(oppdragRevurdering, originaltOppdragslinje150, true, false, 80);
        verifiserOppdr150SomErUendret(oppdragRevurdering);
    }

    /**
     * Førstegangsbehandling: Mottaker:Bruker og Arbeidsgiver, Inntektskatagori: AT
     * Revurdering: Mottaker:Bruker og Arbeidsgiver, Inntektskatagori: AT og FL
     * Endringsdato: Første uttaksdato
     */
    @Test
    public void skalSendeEndringsoppdragNårDetErEnKlassekodeIForrigeOgFlereKlassekodeINyOppdrag() {
        // Arrange
        BeregningsresultatEntitet beregningsresultat = buildBeregningsresultatFP(Optional.empty());
        BeregningsresultatPeriode b1Periode_1 = buildBeregningsresultatPeriode(beregningsresultat, 1, 10);
        buildBeregningsresultatAndel(b1Periode_1, true, 1500, BigDecimal.valueOf(100), virksomhet);
        BeregningsresultatPeriode b1Periode_2 = buildBeregningsresultatPeriode(beregningsresultat, 11, 20);
        buildBeregningsresultatAndel(b1Periode_2, true, 1500, BigDecimal.valueOf(100), virksomhet);
        TilkjentYtelseMapper mapper = new TilkjentYtelseMapper(FamilieYtelseType.FØDSEL);
        GruppertYtelse gruppertYtelse = mapper.fordelPåNøkler(beregningsresultat);
        var builder = getInputStandardBuilder(gruppertYtelse);
        Oppdragskontroll originaltOppdrag = OppdragMedPositivKvitteringTestUtil.opprett(nyOppdragskontrollTjeneste, builder.build());
        List<Oppdrag110> originaltOppdrag110Liste = originaltOppdrag.getOppdrag110Liste();
        List<Oppdragslinje150> originaltOppdragslinje150 = OppdragskontrollTestVerktøy.getOppdragslinje150Liste(originaltOppdrag);

        LocalDate endringsdato = NyOppdragskontrollTjenesteTestBase.DAGENS_DATO.plusDays(1);
        BeregningsresultatEntitet beregningsresultatRevurderingFP = buildBeregningsresultatFP(Optional.of(endringsdato));
        BeregningsresultatPeriode b2Periode_1 = buildBeregningsresultatPeriode(beregningsresultatRevurderingFP, 1, 10);
        buildBeregningsresultatAndel(b2Periode_1, true, 1000, BigDecimal.valueOf(100), virksomhet);
        buildBeregningsresultatAndel(b2Periode_1, true, 1000, BigDecimal.valueOf(100), null,
            AktivitetStatus.FRILANSER, Inntektskategori.FRILANSER);
        BeregningsresultatPeriode b2Periode_2 = buildBeregningsresultatPeriode(beregningsresultatRevurderingFP, 11, 20);
        buildBeregningsresultatAndel(b2Periode_2, true, 1000, BigDecimal.valueOf(100), virksomhet);
        buildBeregningsresultatAndel(b2Periode_2, true, 1000, BigDecimal.valueOf(100), null,
            AktivitetStatus.FRILANSER, Inntektskategori.FRILANSER);
        GruppertYtelse gruppertYtelse2 = mapper.fordelPåNøkler(beregningsresultatRevurderingFP);
        var builder2 = getInputStandardBuilder(gruppertYtelse2).medTidligereOppdrag(mapTidligereOppdrag(List.of(originaltOppdrag)));

        // Act
        Oppdragskontroll oppdragRevurdering = OppdragMedPositivKvitteringTestUtil.opprett(nyOppdragskontrollTjeneste, builder2.build());

        //Assert
        OppdragskontrollTestVerktøy.verifiserAvstemming(oppdragRevurdering);
        List<Oppdrag110> oppdrag110Liste = oppdragRevurdering.getOppdrag110Liste();
        assertThat(oppdrag110Liste).hasSize(1);
        verifiserOppdrag110_ENDR(oppdragRevurdering.getOppdrag110Liste(), originaltOppdrag110Liste, false);
        List<Oppdragslinje150> opp150Liste = OppdragskontrollTestVerktøy.getOpp150ListeForBruker(oppdrag110Liste);
        //En Opphør på AT, To for ny AT, To for ny FL
        assertThat(opp150Liste).hasSize(5);
        List<Oppdragslinje150> opp150ForFLListe = OppdragskontrollTestVerktøy.getOppdragslinje150MedKlassekode(opp150Liste, KodeKlassifik.FPF_FRILANSER);
        assertThat(opp150ForFLListe).allSatisfy(opp150 ->
            assertThat(opp150.gjelderOpphør()).isFalse());
        verifiserOppdragslinje150_ENDR(oppdragRevurdering, originaltOppdragslinje150, false, true, 100);
        OppdragskontrollTestVerktøy.verifiserOppdragslinje150ForHverKlassekode(originaltOppdrag, oppdragRevurdering);
    }

    /**
     * Førstegangsbehandling: Mottaker:Bruker og Arbeidsgiver, Inntektskatagori: AT og FL
     * Revurdering: Mottaker:Bruker og Arbeidsgiver, Inntektskatagori: AT og FL
     * Endringsdato: Første uttaksdato
     */
    @Test
    public void skalSendeEndringsoppdragNårDetErFlereKlassekodeBådeIForrigeOgNyOppdragOgDeErLike() {
        // Arrange
        BeregningsresultatEntitet beregningsresultat = buildBeregningsresultatMedFlereInntektskategoriFP(true);
        TilkjentYtelseMapper mapper = new TilkjentYtelseMapper(FamilieYtelseType.FØDSEL);
        GruppertYtelse gruppertYtelse = mapper.fordelPåNøkler(beregningsresultat);
        var builder = getInputStandardBuilder(gruppertYtelse);
        Oppdragskontroll originaltOppdrag = OppdragMedPositivKvitteringTestUtil.opprett(nyOppdragskontrollTjeneste, builder.build());
        List<Oppdrag110> originaltOppdrag110Liste = originaltOppdrag.getOppdrag110Liste();
        List<Oppdragslinje150> originaltOppdragslinje150 = OppdragskontrollTestVerktøy.getOppdragslinje150Liste(originaltOppdrag);

        LocalDate endringsdato = beregningsresultat.getBeregningsresultatPerioder().get(0).getBeregningsresultatPeriodeFom();
        BeregningsresultatEntitet beregningsresultatRevurderingFP = buildBeregningsresultatRevurderingMedFlereInntektskategoriFP(AktivitetStatus.FRILANSER, Inntektskategori.FRILANSER, endringsdato);

        GruppertYtelse gruppertYtelse2 = mapper.fordelPåNøkler(beregningsresultatRevurderingFP);
        var builder2 = getInputStandardBuilder(gruppertYtelse2).medTidligereOppdrag(mapTidligereOppdrag(List.of(originaltOppdrag)));

        // Act
        Oppdragskontroll oppdragRevurdering = OppdragMedPositivKvitteringTestUtil.opprett(nyOppdragskontrollTjeneste, builder2.build());

        //Assert
        OppdragskontrollTestVerktøy.verifiserAvstemming(oppdragRevurdering);
        verifiserOppdrag110_ENDR(oppdragRevurdering.getOppdrag110Liste(), originaltOppdrag110Liste, true);
        verifiserOppdragslinje150_ENDR(oppdragRevurdering, originaltOppdragslinje150, true, true, 80);
        OppdragskontrollTestVerktøy.verifiserOppdragslinje150ForHverKlassekode(originaltOppdrag, oppdragRevurdering);
    }

    /**
     * Førstegangsbehandling: Bruker er mottaker, Inntektskatagori: SN og FL
     * Revurdering: Bruker er mottaker, Inntektskatagori: AT og FL
     * Endringsdato: Første uttaksdato
     */
    @Test
    public void skalSendeEndringsoppdragNårDetErFlereKlassekodeBådeIForrigeOgNyOppdragOgEnInntektskategoriIForrigeBehandlingBlirAnnerledesIRevurdering() {

        // Arrange : Førstegangsbehandling
        BeregningsresultatEntitet beregningsresultat = buildBeregningsresultatFP(Optional.empty());
        BeregningsresultatPeriode b1Periode_1 = buildBeregningsresultatPeriode(beregningsresultat, 1, 10);
        buildBeregningsresultatAndel(b1Periode_1, true, 1500, BigDecimal.valueOf(100), null,
            AktivitetStatus.FRILANSER, Inntektskategori.FRILANSER);
        buildBeregningsresultatAndel(b1Periode_1, true, 1500, BigDecimal.valueOf(100), virksomhet,
            AktivitetStatus.SELVSTENDIG_NÆRINGSDRIVENDE, Inntektskategori.SELVSTENDIG_NÆRINGSDRIVENDE);
        BeregningsresultatPeriode b1Periode_2 = buildBeregningsresultatPeriode(beregningsresultat, 11, 20);
        buildBeregningsresultatAndel(b1Periode_2, true, 1500, BigDecimal.valueOf(100), null,
            AktivitetStatus.FRILANSER, Inntektskategori.FRILANSER);
        buildBeregningsresultatAndel(b1Periode_2, true, 1500, BigDecimal.valueOf(100), virksomhet,
            AktivitetStatus.SELVSTENDIG_NÆRINGSDRIVENDE, Inntektskategori.SELVSTENDIG_NÆRINGSDRIVENDE);

        TilkjentYtelseMapper mapper = new TilkjentYtelseMapper(FamilieYtelseType.FØDSEL);
        GruppertYtelse gruppertYtelse = mapper.fordelPåNøkler(beregningsresultat);
        var builder = getInputStandardBuilder(gruppertYtelse);
        Oppdragskontroll originaltOppdrag = OppdragMedPositivKvitteringTestUtil.opprett(nyOppdragskontrollTjeneste, builder.build());

        // Arrange : Revurdering
        LocalDate endringsdato = NyOppdragskontrollTjenesteTestBase.DAGENS_DATO.plusDays(1);
        BeregningsresultatEntitet beregningsresultatRevurderingFP = buildBeregningsresultatFP(Optional.of(endringsdato));
        BeregningsresultatPeriode b2Periode_1 = buildBeregningsresultatPeriode(beregningsresultatRevurderingFP, 1, 5);
        buildBeregningsresultatAndel(b2Periode_1, true, 1500, BigDecimal.valueOf(100), virksomhet,
            AktivitetStatus.ARBEIDSTAKER, Inntektskategori.ARBEIDSTAKER);
        buildBeregningsresultatAndel(b2Periode_1, true, 1500, BigDecimal.valueOf(100), null,
            AktivitetStatus.FRILANSER, Inntektskategori.FRILANSER);
        BeregningsresultatPeriode b2Periode_2 = buildBeregningsresultatPeriode(beregningsresultatRevurderingFP, 6, 20);
        buildBeregningsresultatAndel(b2Periode_2, true, 1500, BigDecimal.valueOf(100), virksomhet,
            AktivitetStatus.ARBEIDSTAKER, Inntektskategori.ARBEIDSTAKER);
        buildBeregningsresultatAndel(b2Periode_2, true, 1500, BigDecimal.valueOf(100), null,
            AktivitetStatus.FRILANSER, Inntektskategori.FRILANSER);
        GruppertYtelse gruppertYtelse2 = mapper.fordelPåNøkler(beregningsresultatRevurderingFP);
        var builder2 = getInputStandardBuilder(gruppertYtelse2).medTidligereOppdrag(mapTidligereOppdrag(List.of(originaltOppdrag)));

        // Act
        Oppdragskontroll oppdragRevurdering = OppdragMedPositivKvitteringTestUtil.opprett(nyOppdragskontrollTjeneste, builder2.build());

        // Assert
        // Assert : Første førstegangsbehandling
        OppdragskontrollTestVerktøy.verifiserAvstemming(originaltOppdrag);
        List<Oppdrag110> oppdrag110Liste_1 = originaltOppdrag.getOppdrag110Liste();
        assertThat(oppdrag110Liste_1).hasSize(1);

        // Assert : Revurdering
        OppdragskontrollTestVerktøy.verifiserAvstemming(originaltOppdrag);
        List<Oppdrag110> oppdrag110Liste_2 = oppdragRevurdering.getOppdrag110Liste();
        assertThat(oppdrag110Liste_2).hasSize(1);
        verifiserOppdrag110_ENDR(oppdragRevurdering.getOppdrag110Liste(), oppdrag110Liste_2, false);
        List<Oppdragslinje150> originaltOpp150Liste = OppdragskontrollTestVerktøy.getOppdragslinje150Liste(originaltOppdrag);
        List<Oppdragslinje150> opp150RevurdListe = OppdragskontrollTestVerktøy.getOppdragslinje150Liste(oppdragRevurdering);
        verifiserKodeklassifikNårRevurderingHarNye(originaltOpp150Liste, opp150RevurdListe);
        verifiserKjeding(originaltOpp150Liste, opp150RevurdListe);
    }

    /**
     * Førstegangsbehandling: Bruker er mottaker, Inntektskatagori: SN og FL
     * Revurdering: Bruker er mottaker, Inntektskatagori: AT og Dagpenger
     * Endringsdato: Første uttaksdato
     */
    @Test
    public void skalSendeEndringsoppdragNårDetErFlereKlassekodeBådeIForrigeOgNyOppdragOgDeErUlike() {

        // Arrange : Førstegangsbehandling
        BeregningsresultatEntitet beregningsresultat = buildBeregningsresultatFP(Optional.empty());
        BeregningsresultatPeriode b1Periode_1 = buildBeregningsresultatPeriode(beregningsresultat, 1, 10);
        buildBeregningsresultatAndel(b1Periode_1, true, 1500, BigDecimal.valueOf(100), null,
            AktivitetStatus.FRILANSER, Inntektskategori.FRILANSER);
        buildBeregningsresultatAndel(b1Periode_1, true, 1500, BigDecimal.valueOf(100), null,
            AktivitetStatus.SELVSTENDIG_NÆRINGSDRIVENDE, Inntektskategori.SELVSTENDIG_NÆRINGSDRIVENDE);
        BeregningsresultatPeriode b1Periode_2 = buildBeregningsresultatPeriode(beregningsresultat, 11, 20);
        buildBeregningsresultatAndel(b1Periode_2, true, 1500, BigDecimal.valueOf(100), null,
            AktivitetStatus.FRILANSER, Inntektskategori.FRILANSER);
        buildBeregningsresultatAndel(b1Periode_2, true, 1500, BigDecimal.valueOf(100), null,
            AktivitetStatus.SELVSTENDIG_NÆRINGSDRIVENDE, Inntektskategori.SELVSTENDIG_NÆRINGSDRIVENDE);
        TilkjentYtelseMapper mapper = new TilkjentYtelseMapper(FamilieYtelseType.FØDSEL);
        GruppertYtelse gruppertYtelse = mapper.fordelPåNøkler(beregningsresultat);
        var builder = getInputStandardBuilder(gruppertYtelse);
        Oppdragskontroll originaltOppdrag = OppdragMedPositivKvitteringTestUtil.opprett(nyOppdragskontrollTjeneste, builder.build());

        // Arrange : Revurdering
        LocalDate endringsdato = b1Periode_1.getBeregningsresultatPeriodeFom();
        BeregningsresultatEntitet beregningsresultatRevurderingFP = buildBeregningsresultatFP(Optional.of(endringsdato));
        BeregningsresultatPeriode b2Periode_1 = buildBeregningsresultatPeriode(beregningsresultatRevurderingFP, 1, 5);
        buildBeregningsresultatAndel(b2Periode_1, true, 1500, BigDecimal.valueOf(100), virksomhet,
            AktivitetStatus.ARBEIDSTAKER, Inntektskategori.ARBEIDSTAKER);
        buildBeregningsresultatAndel(b2Periode_1, true, 1500, BigDecimal.valueOf(100), null,
            AktivitetStatus.DAGPENGER, Inntektskategori.DAGPENGER);
        BeregningsresultatPeriode b2Periode_2 = buildBeregningsresultatPeriode(beregningsresultatRevurderingFP, 6, 20);
        buildBeregningsresultatAndel(b2Periode_2, true, 1500, BigDecimal.valueOf(100), virksomhet,
            AktivitetStatus.ARBEIDSTAKER, Inntektskategori.ARBEIDSTAKER);
        buildBeregningsresultatAndel(b2Periode_2, true, 1500, BigDecimal.valueOf(100), null,
            AktivitetStatus.DAGPENGER, Inntektskategori.DAGPENGER);
        GruppertYtelse gruppertYtelse2 = mapper.fordelPåNøkler(beregningsresultatRevurderingFP);
        var builder2 = getInputStandardBuilder(gruppertYtelse2).medTidligereOppdrag(mapTidligereOppdrag(List.of(originaltOppdrag)));

        // Act
        Oppdragskontroll oppdragRevurdering = OppdragMedPositivKvitteringTestUtil.opprett(nyOppdragskontrollTjeneste, builder2.build());

        // Assert
        OppdragskontrollTestVerktøy.verifiserAvstemming(oppdragRevurdering);
        List<Oppdrag110> oppdrag110RevurderingListe = oppdragRevurdering.getOppdrag110Liste();
        assertThat(oppdrag110RevurderingListe).hasSize(1);
        List<Oppdragslinje150> opp150RevurderingListe = oppdrag110RevurderingListe.get(0).getOppdragslinje150Liste();
        assertThat(opp150RevurderingListe).hasSize(6);
        //Opphør for FL
        List<Oppdragslinje150> opp150OpphForFLListe = getOpp150MedKodeklassifik(opp150RevurderingListe, KodeKlassifik.FPF_FRILANSER);
        assertThat(opp150OpphForFLListe).hasSize(1);
        assertThat(opp150OpphForFLListe).allSatisfy(oppdragslinje150 ->
            assertThat(oppdragslinje150.gjelderOpphør()).isTrue());
        //Opphør for SN
        List<Oppdragslinje150> opp150OpphForSNListe = getOpp150MedKodeklassifik(opp150RevurderingListe, KodeKlassifik.FPF_SELVSTENDIG);
        assertThat(opp150OpphForSNListe).hasSize(1);
        assertThat(opp150OpphForSNListe).allSatisfy(oppdragslinje150 ->
            assertThat(oppdragslinje150.gjelderOpphør()).isTrue());
        //Oppdragslinje150 for AT
        List<Oppdragslinje150> opp150ForATListe = getOpp150MedKodeklassifik(opp150RevurderingListe, KodeKlassifik.FPF_ARBEIDSTAKER);
        assertThat(opp150ForATListe).hasSize(2);
        assertThat(opp150ForATListe).allSatisfy(oppdragslinje150 ->
            assertThat(oppdragslinje150.gjelderOpphør()).isFalse());
        List<Oppdragslinje150> sortertOpp150OpphForATListe = sortOppdragslinj150Liste(opp150ForATListe);
        Oppdragslinje150 førsteOpp150ForAT = sortertOpp150OpphForATListe.get(0);
        assertThat(førsteOpp150ForAT.getRefFagsystemId()).isNull();
        assertThat(førsteOpp150ForAT.getRefDelytelseId()).isNull();
        Oppdragslinje150 andreOpp150ForAT = sortertOpp150OpphForATListe.get(1);
        assertThat(andreOpp150ForAT.getRefDelytelseId()).isEqualTo(førsteOpp150ForAT.getDelytelseId());
        //Oppdragslinje150 for DP
        List<Oppdragslinje150> opp150OpphForDPListe = getOpp150MedKodeklassifik(opp150RevurderingListe, KodeKlassifik.FPF_DAGPENGER);
        assertThat(opp150OpphForDPListe).hasSize(2);
        assertThat(opp150OpphForDPListe).allSatisfy(oppdragslinje150 ->
            assertThat(oppdragslinje150.gjelderOpphør()).isFalse());
        List<Oppdragslinje150> sortertOpp150OpphForDPListe = sortOppdragslinj150Liste(opp150OpphForDPListe);
        Oppdragslinje150 førsteOpp150ForDP = sortertOpp150OpphForDPListe.get(0);
        assertThat(førsteOpp150ForDP.getRefFagsystemId()).isNull();
        assertThat(førsteOpp150ForDP.getRefDelytelseId()).isNull();
        Oppdragslinje150 andreOpp150ForDP = sortertOpp150OpphForDPListe.get(1);
        assertThat(andreOpp150ForDP.getRefDelytelseId()).isEqualTo(førsteOpp150ForDP.getDelytelseId());
        //Sjekk om delytelseId er unikt for oppdragslinje150
        List<Long> delytelseIdList = opp150RevurderingListe.stream()
            .map(Oppdragslinje150::getDelytelseId)
            .collect(Collectors.toList());
        Set<Long> delytelseIdSet = Sets.newHashSet(delytelseIdList);
        assertThat(delytelseIdList).hasSize(delytelseIdSet.size());
    }

    /**
     * Førstegangsbehandling: Mottaker:Bruker, En inntektskategori i periode 1(FL) og to i periode 2 (AT(orgnr1), FL)
     * Revurdering: Mottaker: Bruker, En inntektskategori i periode 1 (AT(orgnr2)) og to i periode 2 (AT(orgnr1)), AT(orgnr2))
     */
    @Test
    public void skalSendeEndringsOppdragOgSlåArbeidstakerAndelerSammenHvisBrukerHarFlereISammePeriode() {

        // Arrange : Førstegangsbehandling
        BeregningsresultatEntitet beregningsresultat = buildBeregningsresultatFP(Optional.empty());
        BeregningsresultatPeriode b1Periode_1 = buildBeregningsresultatPeriode(beregningsresultat, 1, 10);
        buildBeregningsresultatAndel(b1Periode_1, true, 1500, BigDecimal.valueOf(100), null,
            AktivitetStatus.FRILANSER, Inntektskategori.FRILANSER);
        BeregningsresultatPeriode b1Periode_2 = buildBeregningsresultatPeriode(beregningsresultat, 11, 20);
        buildBeregningsresultatAndel(b1Periode_2, true, 1500, BigDecimal.valueOf(100), virksomhet,
            AktivitetStatus.ARBEIDSTAKER, Inntektskategori.ARBEIDSTAKER);
        buildBeregningsresultatAndel(b1Periode_2, true, 1500, BigDecimal.valueOf(100), null,
            AktivitetStatus.FRILANSER, Inntektskategori.FRILANSER);
        TilkjentYtelseMapper mapper = new TilkjentYtelseMapper(FamilieYtelseType.FØDSEL);
        GruppertYtelse gruppertYtelse = mapper.fordelPåNøkler(beregningsresultat);
        var builder = getInputStandardBuilder(gruppertYtelse);
        Oppdragskontroll originaltOppdrag = OppdragMedPositivKvitteringTestUtil.opprett(nyOppdragskontrollTjeneste, builder.build());

        // Arrange : Revurdering
        LocalDate endringsdato = NyOppdragskontrollTjenesteTestBase.DAGENS_DATO.plusDays(1);
        BeregningsresultatEntitet beregningsresultatRevurderingFP = buildBeregningsresultatFP(Optional.of(endringsdato));
        BeregningsresultatPeriode b2Periode_1 = buildBeregningsresultatPeriode(beregningsresultatRevurderingFP, 1, 5);
        buildBeregningsresultatAndel(b2Periode_1, true, 1500, BigDecimal.valueOf(100), virksomhet2,
            AktivitetStatus.ARBEIDSTAKER, Inntektskategori.ARBEIDSTAKER);
        BeregningsresultatPeriode b2Periode_2 = buildBeregningsresultatPeriode(beregningsresultatRevurderingFP, 6, 20);
        buildBeregningsresultatAndel(b2Periode_2, true, 1500, BigDecimal.valueOf(100), virksomhet,
            AktivitetStatus.ARBEIDSTAKER, Inntektskategori.ARBEIDSTAKER);
        buildBeregningsresultatAndel(b2Periode_2, true, 1500, BigDecimal.valueOf(100), virksomhet2,
            AktivitetStatus.ARBEIDSTAKER, Inntektskategori.ARBEIDSTAKER);
        GruppertYtelse gruppertYtelse2 = mapper.fordelPåNøkler(beregningsresultatRevurderingFP);
        var builder2 = getInputStandardBuilder(gruppertYtelse2).medTidligereOppdrag(mapTidligereOppdrag(List.of(originaltOppdrag)));

        // Act
        Oppdragskontroll oppdragRevurdering = OppdragMedPositivKvitteringTestUtil.opprett(nyOppdragskontrollTjeneste, builder2.build());

        // Assert : Første førstegangsbehandling
        OppdragskontrollTestVerktøy.verifiserAvstemming(originaltOppdrag);
        List<Oppdrag110> oppdrag110Liste_1 = originaltOppdrag.getOppdrag110Liste();
        assertThat(oppdrag110Liste_1).hasSize(1);

        // Assert : Revurdering
        OppdragskontrollTestVerktøy.verifiserAvstemming(originaltOppdrag);
        List<Oppdragslinje150> originaltOpp150Liste = OppdragskontrollTestVerktøy.getOppdragslinje150Liste(originaltOppdrag);
        List<Oppdragslinje150> opp150RevurdListe = OppdragskontrollTestVerktøy.getOppdragslinje150Liste(oppdragRevurdering);
        verifiserKodeklassifik(originaltOpp150Liste, opp150RevurdListe);
        List<Oppdragslinje150> opp150IAndrePeriode = opp150RevurdListe.stream()
            .filter(opp150 -> opp150.getDatoVedtakFom().equals(b2Periode_2.getBeregningsresultatPeriodeFom()))
            .collect(Collectors.toList());
        assertThat(opp150IAndrePeriode).hasSize(1);
        assertThat(opp150IAndrePeriode.get(0).getSats()).isEqualTo(Sats.på(3000));
    }

    @Test
    public void skalSendeEndringsOppdragHvisEndringIUtbetalingsgrad() {

        // Arrange : Førstegangsbehandling
        BeregningsresultatEntitet beregningsresultat = buildBeregningsresultatFP(Optional.empty());
        BeregningsresultatPeriode b1Periode_1 = buildBeregningsresultatPeriode(beregningsresultat, 1, 10);
        buildBeregningsresultatAndel(b1Periode_1, true, 1500, BigDecimal.valueOf(100), null,
            AktivitetStatus.FRILANSER, Inntektskategori.FRILANSER);
        BeregningsresultatPeriode b1Periode_2 = buildBeregningsresultatPeriode(beregningsresultat, 11, 20);
        buildBeregningsresultatAndel(b1Periode_2, true, 1500, BigDecimal.valueOf(100), virksomhet,
            AktivitetStatus.ARBEIDSTAKER, Inntektskategori.ARBEIDSTAKER);
        buildBeregningsresultatAndel(b1Periode_2, true, 1500, BigDecimal.valueOf(100), null,
            AktivitetStatus.FRILANSER, Inntektskategori.FRILANSER);

        TilkjentYtelseMapper mapper = new TilkjentYtelseMapper(FamilieYtelseType.FØDSEL);
        GruppertYtelse gruppertYtelse = mapper.fordelPåNøkler(beregningsresultat);
        var builder = getInputStandardBuilder(gruppertYtelse);
        Oppdragskontroll originaltOppdrag = OppdragMedPositivKvitteringTestUtil.opprett(nyOppdragskontrollTjeneste, builder.build());

        // Arrange : Revurdering
        LocalDate endringsdato = NyOppdragskontrollTjenesteTestBase.DAGENS_DATO.plusDays(1);
        BeregningsresultatEntitet beregningsresultatRevurderingFP = buildBeregningsresultatFP(Optional.of(endringsdato));
        BeregningsresultatPeriode b2Periode_1 = buildBeregningsresultatPeriode(beregningsresultatRevurderingFP, 1, 10);
        buildBeregningsresultatAndel(b2Periode_1, true, 1500, BigDecimal.valueOf(80), null,
            AktivitetStatus.FRILANSER, Inntektskategori.FRILANSER);
        BeregningsresultatPeriode b2Periode_2 = buildBeregningsresultatPeriode(beregningsresultatRevurderingFP, 11, 20);
        buildBeregningsresultatAndel(b2Periode_2, true, 1500, BigDecimal.valueOf(80), virksomhet,
            AktivitetStatus.ARBEIDSTAKER, Inntektskategori.ARBEIDSTAKER);
        buildBeregningsresultatAndel(b2Periode_2, true, 1500, BigDecimal.valueOf(80), null,
            AktivitetStatus.FRILANSER, Inntektskategori.FRILANSER);

        GruppertYtelse gruppertYtelse2 = mapper.fordelPåNøkler(beregningsresultatRevurderingFP);
        var builder2 = getInputStandardBuilder(gruppertYtelse2).medTidligereOppdrag(mapTidligereOppdrag(List.of(originaltOppdrag)));

        // Act
        Oppdragskontroll oppdragRevurdering = OppdragMedPositivKvitteringTestUtil.opprett(nyOppdragskontrollTjeneste, builder2.build());

        // Assert : Første førstegangsbehandling
        OppdragskontrollTestVerktøy.verifiserAvstemming(originaltOppdrag);
        List<Oppdrag110> oppdrag110Liste_1 = originaltOppdrag.getOppdrag110Liste();
        assertThat(oppdrag110Liste_1).hasSize(1);

        // Assert : Revurdering
        OppdragskontrollTestVerktøy.verifiserAvstemming(originaltOppdrag);
        List<Oppdragslinje150> originaltOpp150Liste = OppdragskontrollTestVerktøy.getOppdragslinje150Liste(originaltOppdrag);
        List<Oppdragslinje150> opp150RevurdListe = OppdragskontrollTestVerktøy.getOppdragslinje150Liste(oppdragRevurdering);
        verifiserKodeklassifik(originaltOpp150Liste, opp150RevurdListe);
        List<Oppdragslinje150> opp150IAndrePeriode = opp150RevurdListe.stream()
            .filter(opp150 -> !opp150.gjelderOpphør())
            .collect(Collectors.toList());
        assertThat(opp150IAndrePeriode).hasSize(3);
        assertThat(opp150IAndrePeriode).allSatisfy(opp150 -> assertThat(opp150.getUtbetalingsgrad().getVerdi()).isEqualTo(80));
    }

    @Test
    public void skalSendeEndringsoppdragNårDetErFlereKlassekodeIForrigeOppdragOgEnNyKlassekodeINyOppdrag() {
        // Arrange
        BeregningsresultatEntitet beregningsresultat = buildBeregningsresultatMedFlereInntektskategoriFP(true);
        TilkjentYtelseMapper mapper = new TilkjentYtelseMapper(FamilieYtelseType.FØDSEL);
        GruppertYtelse gruppertYtelse = mapper.fordelPåNøkler(beregningsresultat);
        var builder = getInputStandardBuilder(gruppertYtelse);
        Oppdragskontroll originaltOppdrag = OppdragMedPositivKvitteringTestUtil.opprett(nyOppdragskontrollTjeneste, builder.build());
        List<Oppdrag110> originaltOppdrag110Liste = originaltOppdrag.getOppdrag110Liste();
        List<Oppdragslinje150> originaltOppdragslinje150 = OppdragskontrollTestVerktøy.getOppdragslinje150Liste(originaltOppdrag);

        LocalDate endringsdato = beregningsresultat.getBeregningsresultatPerioder().stream().min(Comparator.comparing(BeregningsresultatPeriode::getBeregningsresultatPeriodeFom))
            .map(BeregningsresultatPeriode::getBeregningsresultatPeriodeFom).get();
        BeregningsresultatEntitet beregningsresultatRevurderingFP = buildBeregningsresultatRevurderingFP(AktivitetStatus.DAGPENGER, Inntektskategori.DAGPENGER, endringsdato);
        GruppertYtelse gruppertYtelse2 = mapper.fordelPåNøkler(beregningsresultatRevurderingFP);
        var builder2 = getInputStandardBuilder(gruppertYtelse2).medTidligereOppdrag(mapTidligereOppdrag(List.of(originaltOppdrag)));

        // Act
        Oppdragskontroll oppdragRevurdering = OppdragMedPositivKvitteringTestUtil.opprett(nyOppdragskontrollTjeneste, builder2.build());

        //Assert
        List<Oppdragslinje150> opp150RevurdListe = OppdragskontrollTestVerktøy.getOppdragslinje150Liste(oppdragRevurdering);
        OppdragskontrollTestVerktøy.verifiserAvstemming(oppdragRevurdering);
        verifiserOppdrag110_ENDR(oppdragRevurdering.getOppdrag110Liste(), originaltOppdrag110Liste, true);
        OppdragskontrollTestVerktøy.verifiserOppdr150SomErOpphørt(opp150RevurdListe, originaltOppdragslinje150, true, true, false);
        OppdragskontrollTestVerktøy.verifiserOppdr150SomErNy(opp150RevurdListe, originaltOppdragslinje150, List.of(80));
        OppdragskontrollTestVerktøy.verifiserOppdr150MedNyKlassekode(opp150RevurdListe);
    }

    @Test
    public void skalSendeOppdragMedEnInntektskategoriIOriginalOgFlereIRevurdering() {
        // Førstegang behandling
        LocalDate fom = LocalDate.of(I_ÅR, 8, 1);
        LocalDate tom = LocalDate.of(I_ÅR, 8, 7);
        BeregningsresultatEntitet beregningsresultat = buildBeregningsresultatBrukerFP(null, 1500, 500, fom, tom);
        TilkjentYtelseMapper mapper = new TilkjentYtelseMapper(FamilieYtelseType.FØDSEL);
        GruppertYtelse gruppertYtelse = mapper.fordelPåNøkler(beregningsresultat);
        var builder = getInputStandardBuilder(gruppertYtelse);
        Oppdragskontroll originaltOppdrag = OppdragMedPositivKvitteringTestUtil.opprett(nyOppdragskontrollTjeneste, builder.build());

        // Ny revurdering behandling
        LocalDate endringsdato = beregningsresultat.getBeregningsresultatPerioder().get(0).getBeregningsresultatPeriodeFom();
        BeregningsresultatEntitet beregningsresultatRevurderingFP = buildBeregningsresultatRevurderingMedFlereInntektskategoriFP(AktivitetStatus.FRILANSER, Inntektskategori.FRILANSER, endringsdato);
        GruppertYtelse gruppertYtelse2 = mapper.fordelPåNøkler(beregningsresultatRevurderingFP);
        var builder2 = getInputStandardBuilder(gruppertYtelse2).medTidligereOppdrag(mapTidligereOppdrag(List.of(originaltOppdrag)));

        // Act
        Oppdragskontroll oppdragRevurdering = OppdragMedPositivKvitteringTestUtil.opprett(nyOppdragskontrollTjeneste, builder2.build());

        //Assert
        OppdragskontrollTestVerktøy.getOppdragslinje150Liste(oppdragRevurdering);
    }

    @Test
    public void skalSendeOppdragNårEnMottakerHarFlereAndelerMedSammeKlassekodeIEnPeriode() {
        // Arrange
        BeregningsresultatEntitet beregningsresultat = buildBeregningsresultatMedFlereInntektskategoriFP(true, AktivitetStatus.ARBEIDSTAKER,
            Inntektskategori.ARBEIDSTAKER);

        TilkjentYtelseMapper mapper = new TilkjentYtelseMapper(FamilieYtelseType.FØDSEL);
        GruppertYtelse gruppertYtelse = mapper.fordelPåNøkler(beregningsresultat);
        var builder = getInputStandardBuilder(gruppertYtelse);
        Oppdragskontroll originaltOppdrag = OppdragMedPositivKvitteringTestUtil.opprett(nyOppdragskontrollTjeneste, builder.build());

        List<Oppdrag110> originaltOppdrag110Liste = originaltOppdrag.getOppdrag110Liste();
        List<Oppdragslinje150> originaltOppdragslinje150 = OppdragskontrollTestVerktøy.getOppdragslinje150Liste(originaltOppdrag);

        LocalDate endringsdato = beregningsresultat.getBeregningsresultatPerioder().get(0).getBeregningsresultatPeriodeFom();
        BeregningsresultatEntitet beregningsresultatRevurderingFP = buildBeregningsresultatRevurderingMedFlereInntektskategoriFP(AktivitetStatus.ARBEIDSTAKER,
            Inntektskategori.ARBEIDSTAKER_UTEN_FERIEPENGER, endringsdato);

        GruppertYtelse gruppertYtelse2 = mapper.fordelPåNøkler(beregningsresultatRevurderingFP);
        var builder2 = getInputStandardBuilder(gruppertYtelse2).medTidligereOppdrag(mapTidligereOppdrag(List.of(originaltOppdrag)));

        // Act
        Oppdragskontroll oppdragRevurdering = OppdragMedPositivKvitteringTestUtil.opprett(nyOppdragskontrollTjeneste, builder2.build());

        //Assert
        List<Oppdragslinje150> opp150RevurdListe = OppdragskontrollTestVerktøy.getOppdragslinje150Liste(oppdragRevurdering);
        OppdragskontrollTestVerktøy.verifiserAvstemming(oppdragRevurdering);
        verifiserOppdrag110_ENDR(oppdragRevurdering.getOppdrag110Liste(), originaltOppdrag110Liste, true);
        OppdragskontrollTestVerktøy.verifiserOppdr150SomErOpphørt(opp150RevurdListe, originaltOppdragslinje150, true, true, false);
        OppdragskontrollTestVerktøy.verifiserOppdr150SomErNy(opp150RevurdListe, originaltOppdragslinje150, List.of(80, 100));
        OppdragskontrollTestVerktøy.verifiserOppdr150SomAndelerSlåSammen(originaltOppdrag, oppdragRevurdering);
    }

    @Test
    public void skalOppretteEndringsoppdragNårBehandlingsresultatErOpphørOgOpphørsdatoErEtterStp() {
        // Arrange
        BeregningsresultatEntitet beregningsresultat = buildBeregningsresultatMedFlereInntektskategoriFP(true, AktivitetStatus.ARBEIDSTAKER, Inntektskategori.ARBEIDSTAKER);
        TilkjentYtelseMapper mapper = new TilkjentYtelseMapper(FamilieYtelseType.FØDSEL);
        GruppertYtelse gruppertYtelse = mapper.fordelPåNøkler(beregningsresultat);
        var builder = getInputStandardBuilder(gruppertYtelse);
        Oppdragskontroll originaltOppdrag = OppdragMedPositivKvitteringTestUtil.opprett(nyOppdragskontrollTjeneste, builder.build());
        List<Oppdrag110> originaltOppdrag110Liste = originaltOppdrag.getOppdrag110Liste();
        List<Oppdragslinje150> originaltOppdragslinje150 = OppdragskontrollTestVerktøy.getOppdragslinje150Liste(originaltOppdrag);

        LocalDate endringsdato = beregningsresultat.getBeregningsresultatPerioder().get(1).getBeregningsresultatPeriodeFom();
        BeregningsresultatEntitet beregningsresultatRevurderingFP = buildBeregningsresultatRevurderingMedFlereInntektskategoriFP(AktivitetStatus.ARBEIDSTAKER,
            Inntektskategori.ARBEIDSTAKER_UTEN_FERIEPENGER, endringsdato);

        GruppertYtelse gruppertYtelse2 = mapper.fordelPåNøkler(beregningsresultatRevurderingFP);
        var builder2 = getInputStandardBuilder(gruppertYtelse2).medTidligereOppdrag(mapTidligereOppdrag(List.of(originaltOppdrag)));

        // Act
        Oppdragskontroll oppdragRevurdering = OppdragMedPositivKvitteringTestUtil.opprett(nyOppdragskontrollTjeneste, builder2.build());

        //Assert
        List<Oppdragslinje150> opp150RevurdListe = OppdragskontrollTestVerktøy.getOppdragslinje150Liste(oppdragRevurdering);
        OppdragskontrollTestVerktøy.verifiserAvstemming(oppdragRevurdering);
        verifiserOppdrag110_ENDR(oppdragRevurdering.getOppdrag110Liste(), originaltOppdrag110Liste, true);
        OppdragskontrollTestVerktøy.verifiserOppdr150SomErOpphørt(opp150RevurdListe, originaltOppdragslinje150, true,
            true, true);
        OppdragskontrollTestVerktøy.verifiserOppdr150SomAndelerSlåSammen(originaltOppdrag, oppdragRevurdering);
    }

    @Test
    public void skalSendeKunOpphørSomEnDelAvEndringsoppdragHvisEndringsdatoErEtterSisteDatoITidligereOppdragForBruker() {
        // Arrange
        BeregningsresultatEntitet beregningsresultat = buildBeregningsresultatEntenForBrukerEllerArbgvr(true, false);
        TilkjentYtelseMapper mapper = new TilkjentYtelseMapper(FamilieYtelseType.FØDSEL);
        GruppertYtelse gruppertYtelse = mapper.fordelPåNøkler(beregningsresultat);
        var builder = getInputStandardBuilder(gruppertYtelse);

        Oppdragskontroll originaltOppdrag = OppdragMedPositivKvitteringTestUtil.opprett(nyOppdragskontrollTjeneste, builder.build());
        Oppdrag110 originaltOppdrag110 = originaltOppdrag.getOppdrag110Liste().get(0);

        LocalDate sistePeriodeTom = beregningsresultat.getBeregningsresultatPerioder().get(1).getBeregningsresultatPeriodeTom();
        LocalDate endringsdato = sistePeriodeTom.minusDays(5);
        BeregningsresultatEntitet beregningsresultatRevurderingFP = buildBeregningsresultatFP(Optional.of(endringsdato));
        BeregningsresultatPeriode brPeriode_1 = buildBeregningsresultatPeriode(beregningsresultatRevurderingFP, NyOppdragskontrollTjenesteTestBase.DAGENS_DATO.plusDays(1), endringsdato.minusDays(1));
        buildBeregningsresultatAndel(brPeriode_1, true, 500, BigDecimal.valueOf(100), virksomhet);

        GruppertYtelse gruppertYtelse2 = mapper.fordelPåNøkler(beregningsresultatRevurderingFP);
        var builder2 = getInputStandardBuilder(gruppertYtelse2).medTidligereOppdrag(mapTidligereOppdrag(List.of(originaltOppdrag)));

        // Act
        Oppdragskontroll oppdragRevurdering = OppdragMedPositivKvitteringTestUtil.opprett(nyOppdragskontrollTjeneste, builder2.build());

        Oppdrag110 revurderingOppdrag110 = oppdragRevurdering.getOppdrag110Liste().get(0);

        //Assert
        OppdragskontrollTestVerktøy.verifiserAvstemming(oppdragRevurdering);
        assertThat(originaltOppdrag110.getKodeFagomrade()).isEqualTo(KodeFagområde.FORELDREPENGER_BRUKER);
        assertThat(originaltOppdrag110.getKodeEndring()).isEqualTo(KodeEndring.NY);
        assertThat(revurderingOppdrag110.getKodeFagomrade()).isEqualTo(KodeFagområde.FORELDREPENGER_BRUKER);
        assertThat(revurderingOppdrag110.getKodeEndring()).isEqualTo(KodeEndring.ENDRING);
        List<Oppdragslinje150> revurderingOpp150Liste = revurderingOppdrag110.getOppdragslinje150Liste();
        assertThat(revurderingOpp150Liste).hasSize(2);
        Oppdragslinje150 opp150Opphør = revurderingOpp150Liste.get(0);
        assertThat(opp150Opphør.getDatoStatusFom()).isEqualTo(NyOppdragskontrollTjenesteTestBase.DAGENS_DATO.plusDays(1));
        assertThat(opp150Opphør.getKodeKlassifik()).isEqualTo(KodeKlassifik.FPF_ARBEIDSTAKER);
        assertThat(opp150Opphør.gjelderOpphør()).isTrue();
    }

    @Test
    public void skalSendeKunOpphørSomEnDelAvEndringsoppdragHvisDetErFlereMottakereSomErArbeidsgivereOgEndringsdatoErEtterSisteDatoINyTilkjentYtelse() {
        // Arrange
        BeregningsresultatEntitet beregningsresultat = buildBeregningsresultatFP(Optional.empty());
        BeregningsresultatPeriode brPeriode_1 = buildBeregningsresultatPeriode(beregningsresultat, 1, 10);
        buildBeregningsresultatAndel(brPeriode_1, false, 500, BigDecimal.valueOf(100), virksomhet);
        buildBeregningsresultatAndel(brPeriode_1, false, 600, BigDecimal.valueOf(100), virksomhet2);
        BeregningsresultatPeriode brPeriode_2 = buildBeregningsresultatPeriode(beregningsresultat, 11, 20);
        buildBeregningsresultatAndel(brPeriode_2, false, 1000, BigDecimal.valueOf(100), virksomhet);
        buildBeregningsresultatAndel(brPeriode_2, false, 1100, BigDecimal.valueOf(100), virksomhet2);
        TilkjentYtelseMapper mapper = new TilkjentYtelseMapper(FamilieYtelseType.FØDSEL);
        GruppertYtelse gruppertYtelse = mapper.fordelPåNøkler(beregningsresultat);
        var builder = getInputStandardBuilder(gruppertYtelse);
        Oppdragskontroll originaltOppdrag = OppdragMedPositivKvitteringTestUtil.opprett(nyOppdragskontrollTjeneste, builder.build());
        List<Oppdrag110> originaltOppdrag110Liste = originaltOppdrag.getOppdrag110Liste();

        LocalDate sistePeriodeTom = beregningsresultat.getBeregningsresultatPerioder().get(1).getBeregningsresultatPeriodeTom();
        LocalDate endringsdato = sistePeriodeTom.minusDays(3);
        BeregningsresultatEntitet beregningsresultatRevurderingFP = buildBeregningsresultatFP(Optional.of(endringsdato));
        BeregningsresultatPeriode brPeriodeRevurdering_1 = buildBeregningsresultatPeriode(beregningsresultatRevurderingFP, 1, 10);
        buildBeregningsresultatAndel(brPeriodeRevurdering_1, false, 500, BigDecimal.valueOf(100), virksomhet);
        buildBeregningsresultatAndel(brPeriodeRevurdering_1, false, 600, BigDecimal.valueOf(100), virksomhet2);
        BeregningsresultatPeriode brPeriodeRevurdering_2 = buildBeregningsresultatPeriode(beregningsresultatRevurderingFP, 11, 16);
        buildBeregningsresultatAndel(brPeriodeRevurdering_2, false, 1000, BigDecimal.valueOf(100), virksomhet);
        buildBeregningsresultatAndel(brPeriodeRevurdering_2, false, 1100, BigDecimal.valueOf(100), virksomhet2);
        GruppertYtelse gruppertYtelse2 = mapper.fordelPåNøkler(beregningsresultatRevurderingFP);
        var builder2 = getInputStandardBuilder(gruppertYtelse2).medTidligereOppdrag(mapTidligereOppdrag(List.of(originaltOppdrag)));

        // Act
        Oppdragskontroll oppdragRevurdering = OppdragMedPositivKvitteringTestUtil.opprett(nyOppdragskontrollTjeneste, builder2.build());
        List<Oppdrag110> revurderingOppdrag110Liste = oppdragRevurdering.getOppdrag110Liste();

        //Assert
        OppdragskontrollTestVerktøy.verifiserAvstemming(oppdragRevurdering);
        assertThat(originaltOppdrag110Liste).hasSize(2);
        assertThat(originaltOppdrag110Liste).allSatisfy(oppdrag110 -> {
                assertThat(oppdrag110.getKodeFagomrade()).isEqualTo(KodeFagområde.FORELDREPENGER_ARBEIDSGIVER);
                assertThat(oppdrag110.getKodeEndring()).isEqualTo(KodeEndring.NY);
            }
        );
        assertThat(revurderingOppdrag110Liste).hasSize(2);
        assertThat(revurderingOppdrag110Liste).allSatisfy(oppdrag110 -> {
                assertThat(oppdrag110.getKodeFagomrade()).isEqualTo(KodeFagområde.FORELDREPENGER_ARBEIDSGIVER);
                assertThat(oppdrag110.getKodeEndring()).isEqualTo(KodeEndring.ENDRING);
            }
        );
        List<Oppdragslinje150> revurderingOpp150Liste = revurderingOppdrag110Liste.stream()
            .flatMap(opp110 -> opp110.getOppdragslinje150Liste().stream())
            .collect(Collectors.toList());
        assertThat(revurderingOpp150Liste).hasSize(2);
        assertThat(revurderingOpp150Liste).allSatisfy(opp150 -> {
            assertThat(opp150.getDatoStatusFom()).isEqualTo(endringsdato);
            assertThat(opp150.getKodeKlassifik()).isEqualTo(KodeKlassifik.FPF_REFUSJON_AG);
            assertThat(opp150.gjelderOpphør()).isTrue();
        });
        Oppdragslinje150 opp150ForVirksomhet1 = OppdragskontrollTestVerktøy.getOpp150ForEnVirksomhet(revurderingOpp150Liste, virksomhet);
        Oppdragslinje150 opp150ForVirksomhet2 = OppdragskontrollTestVerktøy.getOpp150ForEnVirksomhet(revurderingOpp150Liste, virksomhet2);
        assertThat(opp150ForVirksomhet1.getRefusjonsinfo156().getRefunderesId()).isEqualTo(endreTilElleveSiffer(virksomhet));
        assertThat(opp150ForVirksomhet2.getRefusjonsinfo156().getRefunderesId()).isEqualTo(endreTilElleveSiffer(virksomhet2));
    }

    /**
     * Førstegangsbehandling: Både bruker og arbeidsgiver er mottaker, Inntektskategori for bruker: AT og SN
     * Revurdering: Arbeidsgiver er eneste mottaker
     * Endringsdato: Første uttaksdato
     */
    @Test
    public void skalSendeFullstendigOpphørForBrukerMedFlereInntektskategoriIEndringsoppdragNårBrukerErIkkeMottakerIRevurderingLenger() {

        // Arrange : Førstegangsbehandling
        BeregningsresultatEntitet beregningsresultat = buildBeregningsresultatFP(Optional.empty());
        BeregningsresultatPeriode b1Periode_1 = buildBeregningsresultatPeriode(beregningsresultat, 1, 10);
        //Andeler for bruker i periode#1
        buildBeregningsresultatAndel(b1Periode_1, true, 1500, BigDecimal.valueOf(100), virksomhet,
            AktivitetStatus.ARBEIDSTAKER, Inntektskategori.ARBEIDSTAKER);
        buildBeregningsresultatAndel(b1Periode_1, true, 1500, BigDecimal.valueOf(100), null,
            AktivitetStatus.SELVSTENDIG_NÆRINGSDRIVENDE, Inntektskategori.SELVSTENDIG_NÆRINGSDRIVENDE);
        //Andel for arbeidsgiver i periode#1
        buildBeregningsresultatAndel(b1Periode_1, false, 1500, BigDecimal.valueOf(100), virksomhet,
            AktivitetStatus.ARBEIDSTAKER, Inntektskategori.ARBEIDSTAKER);
        //Andeler for bruker i periode#2
        BeregningsresultatPeriode b1Periode_2 = buildBeregningsresultatPeriode(beregningsresultat, 11, 20);
        buildBeregningsresultatAndel(b1Periode_2, true, 1500, BigDecimal.valueOf(100), virksomhet,
            AktivitetStatus.ARBEIDSTAKER, Inntektskategori.ARBEIDSTAKER);
        buildBeregningsresultatAndel(b1Periode_2, true, 1500, BigDecimal.valueOf(100), null,
            AktivitetStatus.SELVSTENDIG_NÆRINGSDRIVENDE, Inntektskategori.SELVSTENDIG_NÆRINGSDRIVENDE);
        //Andel for arbeidsgiver i periode#2
        buildBeregningsresultatAndel(b1Periode_2, false, 1500, BigDecimal.valueOf(100), virksomhet,
            AktivitetStatus.ARBEIDSTAKER, Inntektskategori.ARBEIDSTAKER);

        TilkjentYtelseMapper mapper = new TilkjentYtelseMapper(FamilieYtelseType.FØDSEL);
        GruppertYtelse gruppertYtelse = mapper.fordelPåNøkler(beregningsresultat);
        var builder = getInputStandardBuilder(gruppertYtelse);
        Oppdragskontroll originaltOppdrag = OppdragMedPositivKvitteringTestUtil.opprett(nyOppdragskontrollTjeneste, builder.build());

        // Arrange : Revurdering
        LocalDate endringsdato = b1Periode_1.getBeregningsresultatPeriodeFom();
        BeregningsresultatEntitet beregningsresultatRevurderingFP = buildBeregningsresultatFP(Optional.of(endringsdato));
        BeregningsresultatPeriode b2Periode_1 = buildBeregningsresultatPeriode(beregningsresultatRevurderingFP, 1, 10);
        //Andel for arbeidsgiver i periode#1
        buildBeregningsresultatAndel(b2Periode_1, false, 1500, BigDecimal.valueOf(100), virksomhet,
            AktivitetStatus.ARBEIDSTAKER, Inntektskategori.ARBEIDSTAKER);
        //Andel for arbeidsgiver i periode#2
        BeregningsresultatPeriode b2Periode_2 = buildBeregningsresultatPeriode(beregningsresultatRevurderingFP, 11, 20);
        buildBeregningsresultatAndel(b2Periode_2, false, 1500, BigDecimal.valueOf(100), virksomhet,
            AktivitetStatus.ARBEIDSTAKER, Inntektskategori.ARBEIDSTAKER);

        GruppertYtelse gruppertYtelse2 = mapper.fordelPåNøkler(beregningsresultatRevurderingFP);
        var builder2 = getInputStandardBuilder(gruppertYtelse2).medTidligereOppdrag(mapTidligereOppdrag(List.of(originaltOppdrag)));

        // Act
        Oppdragskontroll oppdragRevurdering = OppdragMedPositivKvitteringTestUtil.opprett(nyOppdragskontrollTjeneste, builder2.build());

        //Assert
        //Førstegangsbehandling
        OppdragskontrollTestVerktøy.verifiserAvstemming(oppdragRevurdering);
        List<Oppdrag110> originaltOppdrag110Liste = originaltOppdrag.getOppdrag110Liste();
        assertThat(originaltOppdrag110Liste).hasSize(2);

        //Revurdering
        List<Oppdrag110> revurderingOppdrag110Liste = oppdragRevurdering.getOppdrag110Liste();
        assertThat(revurderingOppdrag110Liste).hasSize(1); // Kun opphør for SND og Bruker siden AG uten endring.
        //Oppdrag110 for Bruker
        Oppdrag110 oppdrag110Bruker = OppdragskontrollTestVerktøy.getOppdrag110ForBruker(revurderingOppdrag110Liste);
        assertThat(oppdrag110Bruker.getKodeFagomrade()).isEqualTo(KodeFagområde.FORELDREPENGER_BRUKER);
        assertThat(oppdrag110Bruker.getKodeEndring()).isEqualTo(KodeEndring.ENDRING);

        //Oppdragslinj150 for Bruker
        List<Oppdragslinje150> revurderingOpp150ListeForBruker = oppdrag110Bruker.getOppdragslinje150Liste();
        assertThat(revurderingOpp150ListeForBruker).hasSize(2);
        assertThat(revurderingOpp150ListeForBruker).allSatisfy(opp150 -> {
            assertThat(opp150.getDatoStatusFom()).isEqualTo(endringsdato);
            assertThat(opp150.gjelderOpphør()).isTrue();
        });
        List<Oppdragslinje150> oppdragslinje150OpphPå_AT = revurderingOpp150ListeForBruker.stream()
            .filter(opp150 -> KodeKlassifik.FPF_ARBEIDSTAKER.equals(opp150.getKodeKlassifik()))
            .collect(Collectors.toList());
        assertThat(oppdragslinje150OpphPå_AT).hasSize(1);
        List<Oppdragslinje150> oppdragslinje150OpphPå_SN = revurderingOpp150ListeForBruker.stream()
            .filter(opp150 -> KodeKlassifik.FPF_SELVSTENDIG.equals(opp150.getKodeKlassifik()))
            .collect(Collectors.toList());
        assertThat(oppdragslinje150OpphPå_SN).hasSize(1);
    }

    /**
     * Førstegangsbehandling: Bruker er eneste mottaker, Inntektskategori for bruker: AT og SN
     * Revurdering: Bruker er eneste mottaker, Inntektskategori for bruker: AT
     * Endringsdato: Startdato av andre periode i revurdering
     */
    @Test
    public void skalIkkeSendeOpphørForBrukerMedFlereInntektskategoriIEndringsoppdragNårEndringsdatoErEtterSistePeriodeTomIForrigeBehandling() {

        // Arrange : Førstegangsbehandling
        BeregningsresultatEntitet beregningsresultat = buildBeregningsresultatFP(Optional.empty());
        BeregningsresultatPeriode b1Periode_1 = buildBeregningsresultatPeriode(beregningsresultat, 1, 10);
        //Andeler for bruker i periode#1
        BeregningsresultatAndel andelAT = buildBeregningsresultatAndel(b1Periode_1, true, 1500, BigDecimal.valueOf(100), virksomhet,
            AktivitetStatus.ARBEIDSTAKER, Inntektskategori.ARBEIDSTAKER);
        buildBeregningsresultatAndel(b1Periode_1, true, 1500, BigDecimal.valueOf(100), null,
            AktivitetStatus.SELVSTENDIG_NÆRINGSDRIVENDE, Inntektskategori.SELVSTENDIG_NÆRINGSDRIVENDE);
        BeregningsresultatFeriepenger feriepenger = buildBeregningsresultatFeriepenger(beregningsresultat);
        buildBeregningsresultatFeriepengerPrÅr(feriepenger, andelAT, 20000L, List.of(NyOppdragskontrollTjenesteTestBase.DAGENS_DATO));

        TilkjentYtelseMapper mapper = new TilkjentYtelseMapper(FamilieYtelseType.FØDSEL);
        GruppertYtelse gruppertYtelse = mapper.fordelPåNøkler(beregningsresultat);
        var builder = getInputStandardBuilder(gruppertYtelse);
        Oppdragskontroll originaltOppdrag = OppdragMedPositivKvitteringTestUtil.opprett(nyOppdragskontrollTjeneste, builder.build());

        // Arrange : Revurdering
        LocalDate endringsdato = b1Periode_1.getBeregningsresultatPeriodeTom().plusDays(1);

        BeregningsresultatEntitet beregningsresultatRevurderingFP = buildBeregningsresultatFP(Optional.of(endringsdato));
        BeregningsresultatPeriode b2Periode_1 = buildBeregningsresultatPeriode(beregningsresultatRevurderingFP, 1, 10);
        //Andel for bruker i periode#1
        BeregningsresultatAndel andelRevurderingAT = buildBeregningsresultatAndel(b2Periode_1, true, 1500, BigDecimal.valueOf(100), virksomhet,
            AktivitetStatus.ARBEIDSTAKER, Inntektskategori.ARBEIDSTAKER);
        //Andel for bruker i periode#2
        BeregningsresultatPeriode b2Periode_2 = buildBeregningsresultatPeriode(beregningsresultatRevurderingFP, 11, 20);
        buildBeregningsresultatAndel(b2Periode_2, true, 1500, BigDecimal.valueOf(100), virksomhet,
            AktivitetStatus.ARBEIDSTAKER, Inntektskategori.ARBEIDSTAKER);
        BeregningsresultatFeriepenger feriepengerRevurdering = buildBeregningsresultatFeriepenger(beregningsresultatRevurderingFP);
        buildBeregningsresultatFeriepengerPrÅr(feriepengerRevurdering, andelRevurderingAT, 20000L, List.of(NyOppdragskontrollTjenesteTestBase.DAGENS_DATO));

        GruppertYtelse gruppertYtelse2 = mapper.fordelPåNøkler(beregningsresultatRevurderingFP);
        var builder2 = getInputStandardBuilder(gruppertYtelse2).medTidligereOppdrag(mapTidligereOppdrag(List.of(originaltOppdrag)));

        // Act
        Oppdragskontroll oppdragRevurdering = OppdragMedPositivKvitteringTestUtil.opprett(nyOppdragskontrollTjeneste, builder2.build());

        //Assert
        //Revurdering
        List<Oppdrag110> revurderingOppdrag110Liste = oppdragRevurdering.getOppdrag110Liste();
        assertThat(revurderingOppdrag110Liste).hasSize(1);
        //Oppdrag110 for Bruker
        Oppdrag110 oppdrag110Bruker = OppdragskontrollTestVerktøy.getOppdrag110ForBruker(revurderingOppdrag110Liste);
        assertThat(oppdrag110Bruker.getKodeFagomrade()).isEqualTo(KodeFagområde.FORELDREPENGER_BRUKER);
        assertThat(oppdrag110Bruker.getKodeEndring()).isEqualTo(KodeEndring.ENDRING);
        //Oppdragslinj150 for Bruker
        List<Oppdragslinje150> revurderingOpp150ListeForBruker = oppdrag110Bruker.getOppdragslinje150Liste();
        assertThat(revurderingOpp150ListeForBruker).hasSize(2);
        assertThat(revurderingOpp150ListeForBruker).anySatisfy( oppdragslinje150 ->  assertThat(oppdragslinje150.gjelderOpphør()).isTrue());
        assertThat(revurderingOpp150ListeForBruker).anySatisfy( oppdragslinje150 ->  assertThat(oppdragslinje150.getKodeKlassifik()).isEqualTo(KodeKlassifik.FPF_ARBEIDSTAKER));
    }

    /**
     * Førstegangsbehandling: Bruker er eneste mottaker, Inntektskategori for bruker: AT og SN <br/>
     * <---AT---><---SN---><br/>
     * Revurdering: Bruker er eneste mottaker, Inntektskategori for bruker: AT<br/>
     * <---AT---><br/>
     * Endringsdato: En dag senere enn siste periode tom i revurdering
     */
    @Test
    public void skalSendeKunOpphørSomEnDelAvEndringsoppdragHvisEndringsdatoErEtterSistePeriodeTomIRevurderingForBrukerMedFlereInntektskategoriIForrigeBehandling() {

        // Arrange : Førstegangsbehandling
        BeregningsresultatEntitet beregningsresultat = buildBeregningsresultatFP(Optional.empty());
        BeregningsresultatPeriode b1Periode_1 = buildBeregningsresultatPeriode(beregningsresultat, 1, 10);
        //Andeler for bruker i periode#1
        buildBeregningsresultatAndel(b1Periode_1, true, 1500, BigDecimal.valueOf(100), virksomhet,
            AktivitetStatus.ARBEIDSTAKER, Inntektskategori.ARBEIDSTAKER);
        buildBeregningsresultatAndel(b1Periode_1, true, 1500, BigDecimal.valueOf(100), null,
            AktivitetStatus.SELVSTENDIG_NÆRINGSDRIVENDE, Inntektskategori.SELVSTENDIG_NÆRINGSDRIVENDE);
        //Andeler for bruker i periode#2
        BeregningsresultatPeriode b1Periode_2 = buildBeregningsresultatPeriode(beregningsresultat, 11, 20);
        buildBeregningsresultatAndel(b1Periode_2, true, 1500, BigDecimal.valueOf(100), virksomhet,
            AktivitetStatus.ARBEIDSTAKER, Inntektskategori.ARBEIDSTAKER);
        buildBeregningsresultatAndel(b1Periode_2, true, 1500, BigDecimal.valueOf(100), null,
            AktivitetStatus.SELVSTENDIG_NÆRINGSDRIVENDE, Inntektskategori.SELVSTENDIG_NÆRINGSDRIVENDE);
        TilkjentYtelseMapper mapper = new TilkjentYtelseMapper(FamilieYtelseType.FØDSEL);
        GruppertYtelse gruppertYtelse = mapper.fordelPåNøkler(beregningsresultat);
        var builder = getInputStandardBuilder(gruppertYtelse);
        Oppdragskontroll originaltOppdrag = OppdragMedPositivKvitteringTestUtil.opprett(nyOppdragskontrollTjeneste, builder.build());

        // Arrange : Revurdering
        LocalDate endringsdato = b1Periode_2.getBeregningsresultatPeriodeTom().minusDays(4);
        BeregningsresultatEntitet beregningsresultatRevurderingFP = buildBeregningsresultatFP(Optional.of(endringsdato));
        BeregningsresultatPeriode b2Periode_1 = buildBeregningsresultatPeriode(beregningsresultatRevurderingFP, 1, 10);
        //Andel for bruker i periode#1
        buildBeregningsresultatAndel(b2Periode_1, true, 1500, BigDecimal.valueOf(100), virksomhet,
            AktivitetStatus.ARBEIDSTAKER, Inntektskategori.ARBEIDSTAKER);
        buildBeregningsresultatAndel(b2Periode_1, true, 1500, BigDecimal.valueOf(100), null,
            AktivitetStatus.ARBEIDSTAKER, Inntektskategori.ARBEIDSTAKER);
        //Andel for bruker i periode#2
        BeregningsresultatPeriode b2Periode_2 = buildBeregningsresultatPeriode(beregningsresultatRevurderingFP, 11, 15);
        buildBeregningsresultatAndel(b2Periode_2, true, 1500, BigDecimal.valueOf(100), virksomhet,
            AktivitetStatus.ARBEIDSTAKER, Inntektskategori.ARBEIDSTAKER);
        buildBeregningsresultatAndel(b2Periode_2, true, 1500, BigDecimal.valueOf(100), null,
            AktivitetStatus.ARBEIDSTAKER, Inntektskategori.ARBEIDSTAKER);
        GruppertYtelse gruppertYtelse2 = mapper.fordelPåNøkler(beregningsresultatRevurderingFP);
        var builder2 = getInputStandardBuilder(gruppertYtelse2).medTidligereOppdrag(mapTidligereOppdrag(List.of(originaltOppdrag)));

        // Act
        Oppdragskontroll oppdragRevurdering = OppdragMedPositivKvitteringTestUtil.opprett(nyOppdragskontrollTjeneste, builder2.build());

        //Assert
        OppdragskontrollTestVerktøy.verifiserAvstemming(oppdragRevurdering);
        //Oppdrag110 for førstegangsbehandling
        List<Oppdrag110> oppdrag110ListeForBruker = originaltOppdrag.getOppdrag110Liste();
        assertThat(oppdrag110ListeForBruker).hasSize(1);
        assertThat(oppdrag110ListeForBruker.get(0).getKodeFagomrade()).isEqualTo(KodeFagområde.FORELDREPENGER_BRUKER);
        assertThat(oppdrag110ListeForBruker.get(0).getKodeEndring()).isEqualTo(KodeEndring.NY);
        //Oppdrag110 for revurdering
        List<Oppdrag110> oppdrag110ListeForBrukerIRevurdering = oppdragRevurdering.getOppdrag110Liste();
        assertThat(oppdrag110ListeForBrukerIRevurdering).hasSize(1);
        assertThat(oppdrag110ListeForBrukerIRevurdering.get(0).getKodeFagomrade()).isEqualTo(KodeFagområde.FORELDREPENGER_BRUKER);
        assertThat(oppdrag110ListeForBrukerIRevurdering.get(0).getKodeEndring()).isEqualTo(KodeEndring.ENDRING);
        //Oppdragslinje150 for bruker i revurdering
        List<Oppdragslinje150> opp150ListeForBrukerIRevurdering = oppdrag110ListeForBrukerIRevurdering.get(0).getOppdragslinje150Liste();
        assertThat(opp150ListeForBrukerIRevurdering).hasSize(4);
        assertThat(opp150ListeForBrukerIRevurdering.stream().filter(Oppdragslinje150::gjelderOpphør)).allSatisfy(opp150 -> {
            assertThat(opp150.getDatoStatusFom()).isEqualTo(b1Periode_1.getBeregningsresultatPeriodeFom());
            assertThat(opp150.gjelderOpphør()).isTrue();
        });
        Optional<Oppdragslinje150> opp150ForBrukerAT = opp150ListeForBrukerIRevurdering.stream()
            .filter(opp150 -> KodeKlassifik.FPF_ARBEIDSTAKER.equals(opp150.getKodeKlassifik()))
            .findFirst();
        assertThat(opp150ForBrukerAT).isPresent();
        Optional<Oppdragslinje150> opp150ForBrukerSN = opp150ListeForBrukerIRevurdering.stream()
            .filter(Oppdragslinje150::gjelderOpphør)
            .filter(opp150 -> KodeKlassifik.FPF_SELVSTENDIG.equals(opp150.getKodeKlassifik()))
            .findFirst();
        assertThat(opp150ForBrukerSN).isPresent();
    }

    /**
     * Førstegangsbehandling: Bruker er eneste mottaker, Inntektskategori for bruker: AT og SN
     * Revurdering: Bruker er eneste mottaker, Inntektskategori for bruker: AT og SN
     * Endringsdato: En dag senere enn siste periode tom i revurdering
     */
    @Test
    public void skalSendeKunOpphørSomEnDelAvEndringsoppdragHvisEndringsdatoErEtterSistePeriodeTomIRevurderingForBrukerMedFlereInntektskategoriIBådeForrigeOgNyBehandling() {

        // Arrange : Førstegangsbehandling
        BeregningsresultatEntitet beregningsresultat = buildBeregningsresultatFP(Optional.empty());
        BeregningsresultatPeriode b1Periode_1 = buildBeregningsresultatPeriode(beregningsresultat, 1, 10);
        //Andeler for bruker i periode#1
        buildBeregningsresultatAndel(b1Periode_1, true, 1500, BigDecimal.valueOf(100), virksomhet,
            AktivitetStatus.ARBEIDSTAKER, Inntektskategori.ARBEIDSTAKER);
        buildBeregningsresultatAndel(b1Periode_1, true, 1500, BigDecimal.valueOf(100), null,
            AktivitetStatus.SELVSTENDIG_NÆRINGSDRIVENDE, Inntektskategori.SELVSTENDIG_NÆRINGSDRIVENDE);
        //Andeler for bruker i periode#2
        BeregningsresultatPeriode b1Periode_2 = buildBeregningsresultatPeriode(beregningsresultat, 11, 20);
        buildBeregningsresultatAndel(b1Periode_2, true, 1500, BigDecimal.valueOf(100), virksomhet,
            AktivitetStatus.ARBEIDSTAKER, Inntektskategori.ARBEIDSTAKER);
        buildBeregningsresultatAndel(b1Periode_2, true, 1500, BigDecimal.valueOf(100), null,
            AktivitetStatus.SELVSTENDIG_NÆRINGSDRIVENDE, Inntektskategori.SELVSTENDIG_NÆRINGSDRIVENDE);
        TilkjentYtelseMapper mapper = new TilkjentYtelseMapper(FamilieYtelseType.FØDSEL);
        GruppertYtelse gruppertYtelse = mapper.fordelPåNøkler(beregningsresultat);
        var builder = getInputStandardBuilder(gruppertYtelse);
        Oppdragskontroll originaltOppdrag = OppdragMedPositivKvitteringTestUtil.opprett(nyOppdragskontrollTjeneste, builder.build());

        // Arrange : Revurdering
        LocalDate endringsdato = b1Periode_2.getBeregningsresultatPeriodeTom().minusDays(4);
        BeregningsresultatEntitet beregningsresultatRevurderingFP = buildBeregningsresultatFP(Optional.of(endringsdato));
        BeregningsresultatPeriode b2Periode_1 = buildBeregningsresultatPeriode(beregningsresultatRevurderingFP, 1, 10);
        //Andel for bruker i periode#1
        buildBeregningsresultatAndel(b2Periode_1, true, 1500, BigDecimal.valueOf(100), virksomhet,
            AktivitetStatus.ARBEIDSTAKER, Inntektskategori.ARBEIDSTAKER);
        buildBeregningsresultatAndel(b2Periode_1, true, 1500, BigDecimal.valueOf(100), null,
            AktivitetStatus.SELVSTENDIG_NÆRINGSDRIVENDE, Inntektskategori.SELVSTENDIG_NÆRINGSDRIVENDE);
        //Andel for bruker i periode#2
        BeregningsresultatPeriode b2Periode_2 = buildBeregningsresultatPeriode(beregningsresultatRevurderingFP, 11, 15);
        buildBeregningsresultatAndel(b2Periode_2, true, 1500, BigDecimal.valueOf(100), virksomhet,
            AktivitetStatus.ARBEIDSTAKER, Inntektskategori.ARBEIDSTAKER);
        buildBeregningsresultatAndel(b2Periode_2, true, 1500, BigDecimal.valueOf(100), null,
            AktivitetStatus.SELVSTENDIG_NÆRINGSDRIVENDE, Inntektskategori.SELVSTENDIG_NÆRINGSDRIVENDE);
        GruppertYtelse gruppertYtelse2 = mapper.fordelPåNøkler(beregningsresultatRevurderingFP);
        var builder2 = getInputStandardBuilder(gruppertYtelse2).medTidligereOppdrag(mapTidligereOppdrag(List.of(originaltOppdrag)));

        // Act
        Oppdragskontroll oppdragRevurdering = OppdragMedPositivKvitteringTestUtil.opprett(nyOppdragskontrollTjeneste, builder2.build());

        //Assert
        OppdragskontrollTestVerktøy.verifiserAvstemming(oppdragRevurdering);
        //Oppdrag110 for førstegangsbehandling
        List<Oppdrag110> oppdrag110ListeForBruker = originaltOppdrag.getOppdrag110Liste();
        assertThat(oppdrag110ListeForBruker).hasSize(1);
        assertThat(oppdrag110ListeForBruker.get(0).getKodeFagomrade()).isEqualTo(KodeFagområde.FORELDREPENGER_BRUKER);
        assertThat(oppdrag110ListeForBruker.get(0).getKodeEndring()).isEqualTo(KodeEndring.NY);
        //Oppdrag110 for revurdering
        List<Oppdrag110> oppdrag110ListeForBrukerIRevurdering = oppdragRevurdering.getOppdrag110Liste();
        assertThat(oppdrag110ListeForBrukerIRevurdering).hasSize(1);
        assertThat(oppdrag110ListeForBrukerIRevurdering.get(0).getKodeFagomrade()).isEqualTo(KodeFagområde.FORELDREPENGER_BRUKER);
        assertThat(oppdrag110ListeForBrukerIRevurdering.get(0).getKodeEndring()).isEqualTo(KodeEndring.ENDRING);
        //Oppdragslinje150 for bruker i revurdering
        List<Oppdragslinje150> opp150ListeForBrukerIRevurdering = oppdrag110ListeForBrukerIRevurdering.get(0).getOppdragslinje150Liste();
        assertThat(opp150ListeForBrukerIRevurdering).hasSize(2);
        assertThat(opp150ListeForBrukerIRevurdering).allSatisfy(opp150 -> {
            assertThat(opp150.getDatoStatusFom()).isEqualTo(b2Periode_2.getBeregningsresultatPeriodeTom().plusDays(1));
            assertThat(opp150.gjelderOpphør()).isTrue();
        });
        Optional<Oppdragslinje150> opp150ForBrukerAT = opp150ListeForBrukerIRevurdering.stream()
            .filter(opp150 -> KodeKlassifik.FPF_ARBEIDSTAKER.equals(opp150.getKodeKlassifik()))
            .findFirst();
        assertThat(opp150ForBrukerAT).isPresent();
        Optional<Oppdragslinje150> opp150ForBrukerSN = opp150ListeForBrukerIRevurdering.stream()
            .filter(Oppdragslinje150::gjelderOpphør)
            .filter(opp150 -> KodeKlassifik.FPF_SELVSTENDIG.equals(opp150.getKodeKlassifik()))
            .findFirst();
        assertThat(opp150ForBrukerSN).isPresent();
    }

    @Test
    public void skalSendeEndringsoppdragHvisDetErFlereMottakereSomErArbeidsgiverOgFinnesMerEnnToBeregningsresultatPerioder() {
        // Arrange
        BeregningsresultatEntitet beregningsresultat = buildBeregningsresultatFP(Optional.empty());
        BeregningsresultatPeriode brPeriode_1 = buildBeregningsresultatPeriode(beregningsresultat, 1, 5);
        buildBeregningsresultatAndel(brPeriode_1, false, 500, BigDecimal.valueOf(100), virksomhet);
        buildBeregningsresultatAndel(brPeriode_1, false, 600, BigDecimal.valueOf(100), virksomhet2);
        BeregningsresultatPeriode brPeriode_2 = buildBeregningsresultatPeriode(beregningsresultat, 6, 10);
        buildBeregningsresultatAndel(brPeriode_2, false, 1000, BigDecimal.valueOf(100), virksomhet);
        buildBeregningsresultatAndel(brPeriode_2, false, 1100, BigDecimal.valueOf(100), virksomhet2);
        BeregningsresultatPeriode brPeriode_3 = buildBeregningsresultatPeriode(beregningsresultat, 11, 15);
        buildBeregningsresultatAndel(brPeriode_3, false, 1000, BigDecimal.valueOf(100), virksomhet);
        buildBeregningsresultatAndel(brPeriode_3, false, 1100, BigDecimal.valueOf(100), virksomhet2);
        BeregningsresultatPeriode brPeriode_4 = buildBeregningsresultatPeriode(beregningsresultat, 16, 20);
        buildBeregningsresultatAndel(brPeriode_4, false, 1000, BigDecimal.valueOf(100), virksomhet);
        buildBeregningsresultatAndel(brPeriode_4, false, 1100, BigDecimal.valueOf(100), virksomhet2);
        TilkjentYtelseMapper mapper = new TilkjentYtelseMapper(FamilieYtelseType.FØDSEL);
        GruppertYtelse gruppertYtelse = mapper.fordelPåNøkler(beregningsresultat);
        var builder = getInputStandardBuilder(gruppertYtelse);
        Oppdragskontroll originaltOppdrag = OppdragMedPositivKvitteringTestUtil.opprett(nyOppdragskontrollTjeneste, builder.build());
        List<Oppdrag110> originaltOppdrag110Liste = originaltOppdrag.getOppdrag110Liste();

        LocalDate førstePeriodeFom = beregningsresultat.getBeregningsresultatPerioder().get(0).getBeregningsresultatPeriodeFom();
        LocalDate endringsdato = førstePeriodeFom.plusDays(2);
        BeregningsresultatEntitet beregningsresultatRevurderingFP = buildBeregningsresultatFP(Optional.of(endringsdato));
        BeregningsresultatPeriode brPeriodeRevurdering_1 = buildBeregningsresultatPeriode(beregningsresultatRevurderingFP, 1, 6);
        buildBeregningsresultatAndel(brPeriodeRevurdering_1, false, 400, BigDecimal.valueOf(100), virksomhet);
        buildBeregningsresultatAndel(brPeriodeRevurdering_1, false, 300, BigDecimal.valueOf(100), virksomhet2);
        BeregningsresultatPeriode brPeriodeRevurdering_2 = buildBeregningsresultatPeriode(beregningsresultatRevurderingFP, 7, 11);
        buildBeregningsresultatAndel(brPeriodeRevurdering_2, false, 500, BigDecimal.valueOf(100), virksomhet);
        buildBeregningsresultatAndel(brPeriodeRevurdering_2, false, 700, BigDecimal.valueOf(100), virksomhet2);
        BeregningsresultatPeriode brPeriodeRevurdering_3 = buildBeregningsresultatPeriode(beregningsresultatRevurderingFP, 12, 15);
        buildBeregningsresultatAndel(brPeriodeRevurdering_3, false, 400, BigDecimal.valueOf(100), virksomhet);
        buildBeregningsresultatAndel(brPeriodeRevurdering_3, false, 300, BigDecimal.valueOf(100), virksomhet2);
        BeregningsresultatPeriode brPeriodeRevurdering_4 = buildBeregningsresultatPeriode(beregningsresultatRevurderingFP, 16, 20);
        buildBeregningsresultatAndel(brPeriodeRevurdering_4, false, 500, BigDecimal.valueOf(100), virksomhet);
        buildBeregningsresultatAndel(brPeriodeRevurdering_4, false, 700, BigDecimal.valueOf(100), virksomhet2);
        GruppertYtelse gruppertYtelse2 = mapper.fordelPåNøkler(beregningsresultatRevurderingFP);
        var builder2 = getInputStandardBuilder(gruppertYtelse2).medTidligereOppdrag(mapTidligereOppdrag(List.of(originaltOppdrag)));

        // Act
        Oppdragskontroll oppdragRevurdering = OppdragMedPositivKvitteringTestUtil.opprett(nyOppdragskontrollTjeneste, builder2.build());
        List<Oppdrag110> revurderingOppdrag110Liste = oppdragRevurdering.getOppdrag110Liste();

        //Assert
        OppdragskontrollTestVerktøy.verifiserAvstemming(oppdragRevurdering);
        assertThat(originaltOppdrag110Liste).hasSize(2);
        assertThat(originaltOppdrag110Liste).allSatisfy(oppdrag110 -> {
                assertThat(oppdrag110.getKodeFagomrade()).isEqualTo(KodeFagområde.FORELDREPENGER_ARBEIDSGIVER);
                assertThat(oppdrag110.getKodeEndring()).isEqualTo(KodeEndring.NY);
            }
        );
        assertThat(revurderingOppdrag110Liste).hasSize(2);
        assertThat(revurderingOppdrag110Liste).allSatisfy(oppdrag110 -> {
                assertThat(oppdrag110.getKodeFagomrade()).isEqualTo(KodeFagområde.FORELDREPENGER_ARBEIDSGIVER);
                assertThat(oppdrag110.getKodeEndring()).isEqualTo(KodeEndring.ENDRING);
            }
        );
        List<Oppdragslinje150> revurderingOpp150Arbgvr1 = OppdragskontrollTestVerktøy.getOpp150ListeForEnVirksomhet(revurderingOppdrag110Liste, virksomhet);
        List<Oppdragslinje150> revurderingOpp150Arbgvr2 = OppdragskontrollTestVerktøy.getOpp150ListeForEnVirksomhet(revurderingOppdrag110Liste, virksomhet2);
        verifiserOpp150NårDetErFlereArbeidsgivereSomMottaker(førstePeriodeFom, revurderingOpp150Arbgvr1);
        verifiserOpp150NårDetErFlereArbeidsgivereSomMottaker(førstePeriodeFom, revurderingOpp150Arbgvr2);
    }

    /**
     * <p>
     * Førstegangsbehandling <br>
     * Perioder: To perioder <br>
     * Mottaker: Bruker, Inntektskategori: AT og FL <br>
     * Feriepenger: Ja, Feriepengeår: i_År.plusYears(1), Beløp: 3000 <br>
     * <p>
     * Revurdering <br>
     * Perioder: Tre perioder <br>
     * Mottaker: Bruker, Inntektskategori: AT og FL <br>
     * Feriepenger: Ja, Feriepengeår: i_År.plusYears(1) og i_År.plusYears(2), Beløp: 3000 og 3000 <br>
     * Endringsdato: Start dato av siste periode i revurdering
     */
    @Test
    public void skalSendeEndringsoppdragUtenOpphørNårDetBlirLagtTilEnNyTilkjentYtelsePeriodeIRevurderingForBrukerMedFlereKlassekode() {

        // Arrange : Førstegangsbehandling
        BeregningsresultatEntitet beregningsresultatFP_1 = buildBeregningsresultatFP(Optional.empty());
        BeregningsresultatPeriode b1Periode_1 = buildBeregningsresultatPeriode(beregningsresultatFP_1, 1, 10);
        //Andeler for bruker i periode#1
        BeregningsresultatAndel b1Andel = buildBeregningsresultatAndel(b1Periode_1, true, 1500, BigDecimal.valueOf(100), virksomhet,
            AktivitetStatus.ARBEIDSTAKER, Inntektskategori.ARBEIDSTAKER);
        buildBeregningsresultatAndel(b1Periode_1, true, 1500, BigDecimal.valueOf(100), null,
            AktivitetStatus.FRILANSER, Inntektskategori.FRILANSER);
        //Andeler for bruker i periode#2
        BeregningsresultatPeriode b1Periode_2 = buildBeregningsresultatPeriode(beregningsresultatFP_1, 11, 20);
        buildBeregningsresultatAndel(b1Periode_2, true, 1500, BigDecimal.valueOf(100), virksomhet,
            AktivitetStatus.ARBEIDSTAKER, Inntektskategori.ARBEIDSTAKER);
        buildBeregningsresultatAndel(b1Periode_2, true, 1500, BigDecimal.valueOf(100), null,
            AktivitetStatus.FRILANSER, Inntektskategori.FRILANSER);
        BeregningsresultatFeriepenger b1_feriepenger = buildBeregningsresultatFeriepenger(beregningsresultatFP_1);
        buildBeregningsresultatFeriepengerPrÅr(b1_feriepenger, b1Andel, 3000L, List.of(NyOppdragskontrollTjenesteTestBase.DAGENS_DATO));
        TilkjentYtelseMapper mapper = new TilkjentYtelseMapper(FamilieYtelseType.FØDSEL);
        GruppertYtelse gruppertYtelse = mapper.fordelPåNøkler(beregningsresultatFP_1);
        var builder = getInputStandardBuilder(gruppertYtelse);
        Oppdragskontroll originaltOppdrag = OppdragMedPositivKvitteringTestUtil.opprett(nyOppdragskontrollTjeneste, builder.build());

        // Arrange : Revurdering
        LocalDate endringsdato = b1Periode_2.getBeregningsresultatPeriodeTom().plusDays(1);
        BeregningsresultatEntitet beregningsresultatRevurderingFP = buildBeregningsresultatFP(Optional.of(endringsdato));
        BeregningsresultatPeriode b2Periode_1 = buildBeregningsresultatPeriode(beregningsresultatRevurderingFP, 1, 10);
        //Andeler for bruker i periode#1
        BeregningsresultatAndel b2Andel = buildBeregningsresultatAndel(b2Periode_1, true, 1500, BigDecimal.valueOf(100), virksomhet,
            AktivitetStatus.ARBEIDSTAKER, Inntektskategori.ARBEIDSTAKER);
        buildBeregningsresultatAndel(b2Periode_1, true, 1500, BigDecimal.valueOf(100), null,
            AktivitetStatus.FRILANSER, Inntektskategori.FRILANSER);
        //Andeler for bruker i periode#2
        BeregningsresultatPeriode b2Periode_2 = buildBeregningsresultatPeriode(beregningsresultatRevurderingFP, 11, 20);
        buildBeregningsresultatAndel(b2Periode_2, true, 1500, BigDecimal.valueOf(100), virksomhet,
            AktivitetStatus.ARBEIDSTAKER, Inntektskategori.ARBEIDSTAKER);
        buildBeregningsresultatAndel(b2Periode_2, true, 1500, BigDecimal.valueOf(100), null,
            AktivitetStatus.FRILANSER, Inntektskategori.FRILANSER);
        //Andeler for bruker i periode#3
        BeregningsresultatPeriode b2Periode_3 = buildBeregningsresultatPeriode(beregningsresultatRevurderingFP, 21, 30);
        buildBeregningsresultatAndel(b2Periode_3, true, 1500, BigDecimal.valueOf(100), virksomhet,
            AktivitetStatus.ARBEIDSTAKER, Inntektskategori.ARBEIDSTAKER);
        buildBeregningsresultatAndel(b2Periode_3, true, 1500, BigDecimal.valueOf(100), null,
            AktivitetStatus.FRILANSER, Inntektskategori.FRILANSER);
        BeregningsresultatFeriepenger b2_feriepenger = buildBeregningsresultatFeriepenger(beregningsresultatRevurderingFP);
        buildBeregningsresultatFeriepengerPrÅr(b2_feriepenger, b2Andel, 3000L, List.of(NyOppdragskontrollTjenesteTestBase.DAGENS_DATO, NyOppdragskontrollTjenesteTestBase.DAGENS_DATO.plusYears(1)));

        GruppertYtelse gruppertYtelse2 = mapper.fordelPåNøkler(beregningsresultatRevurderingFP);
        var builder2 = getInputStandardBuilder(gruppertYtelse2).medTidligereOppdrag(mapTidligereOppdrag(List.of(originaltOppdrag)));

        // Act
        Oppdragskontroll oppdragRevurdering = OppdragMedPositivKvitteringTestUtil.opprett(nyOppdragskontrollTjeneste, builder2.build());

        //Assert
        assertThat(oppdragRevurdering).isNotNull();
        //Oppdrag110
        List<Oppdrag110> originalOpp110Liste = originaltOppdrag.getOppdrag110Liste();
        assertThat(originalOpp110Liste).hasSize(1);
        List<Oppdrag110> opp110ListeForRevurdering = oppdragRevurdering.getOppdrag110Liste();
        assertThat(opp110ListeForRevurdering).hasSize(1);
        Oppdrag110 oppdrag110ForBruker = opp110ListeForRevurdering.get(0);
        //Oppdragslinje150
        List<Oppdragslinje150> opp150ListeForBruker = oppdrag110ForBruker.getOppdragslinje150Liste();
        assertThat(opp150ListeForBruker).hasSize(3);
        //Oppdragslinje150 for feriepenger
        List<Oppdragslinje150> opp150ForFeriepengerList = OppdragskontrollTestVerktøy.getOppdragslinje150MedKlassekode(opp150ListeForBruker,
            KodeKlassifik.FERIEPENGER_BRUKER);
        assertThat(opp150ForFeriepengerList).hasSize(1);
        Oppdragslinje150 opp150ForFeriepenger = opp150ForFeriepengerList.get(0);
        assertThat(opp150ForFeriepenger.gjelderOpphør()).isFalse();
        assertThat(opp150ForFeriepenger.getDatoVedtakFom().getYear()).isEqualTo(I_ÅR + 2);
        //Oppdragslinje150 for AT
        List<Oppdragslinje150> opp150ForATIRevurderingList = OppdragskontrollTestVerktøy.getOppdragslinje150MedKlassekode(opp150ListeForBruker,
            KodeKlassifik.FPF_ARBEIDSTAKER);
        assertThat(opp150ForATIRevurderingList).hasSize(1);
        Oppdragslinje150 opp150ForATIRevurdering = opp150ForATIRevurderingList.get(0);
        assertThat(opp150ForATIRevurdering.gjelderOpphør()).isFalse();
        List<Oppdragslinje150> tidligereOpp150ForATList = OppdragskontrollTestVerktøy.getOppdragslinje150MedKlassekode(originalOpp110Liste.get(0).getOppdragslinje150Liste(),
            KodeKlassifik.FPF_ARBEIDSTAKER);
        Oppdragslinje150 sisteOpp150ForAT = tidligereOpp150ForATList.stream().max(Comparator.comparing(Oppdragslinje150::getDelytelseId)).get();
        assertThat(opp150ForATIRevurdering.getRefDelytelseId()).isEqualTo(sisteOpp150ForAT.getDelytelseId());
        //Oppdragslinje150 for FL
        List<Oppdragslinje150> opp150ForFLIRevurderingList = OppdragskontrollTestVerktøy.getOppdragslinje150MedKlassekode(opp150ListeForBruker,
            KodeKlassifik.FPF_FRILANSER);
        assertThat(opp150ForFLIRevurderingList).hasSize(1);
        Oppdragslinje150 opp150ForFLIRevurdering = opp150ForFLIRevurderingList.get(0);
        assertThat(opp150ForFLIRevurdering.gjelderOpphør()).isFalse();
        List<Oppdragslinje150> tidligereOpp150ForFLList = OppdragskontrollTestVerktøy.getOppdragslinje150MedKlassekode(originalOpp110Liste.get(0).getOppdragslinje150Liste(),
            KodeKlassifik.FPF_FRILANSER);
        Oppdragslinje150 sisteOpp150ForFL = tidligereOpp150ForFLList.stream().max(Comparator.comparing(Oppdragslinje150::getDelytelseId)).get();
        assertThat(opp150ForFLIRevurdering.getRefDelytelseId()).isEqualTo(sisteOpp150ForFL.getDelytelseId());
    }

    /**
     * <p>
     * Førstegangsbehandling <br>
     * Perioder: To perioder <br>
     * Mottaker: Bruker, Inntektskategori: AT<br>
     * Feriepenger: Ja, Feriepengeår: i_År.plusYears(1), Beløp: 3000 <br>
     * <p>
     * Revurdering <br>
     * Perioder: Tre perioder <br>
     * Mottaker: Bruker, Inntektskategori: AT<br>
     * Feriepenger: Ja, Feriepengeår: i_År.plusYears(1) og i_År.plusYears(2), Beløp: 3000 og 3000 <br>
     * Endringsdato: Start dato av siste periode i revurdering
     */
    @Test
    public void skalSendeEndringsoppdragUtenOpphørNårDetBlirLagtTilEnNyTilkjentYtelsePeriodeIRevurderingForBrukerMedEnKlassekode() {

        // Arrange : Førstegangsbehandling
        BeregningsresultatEntitet beregningsresultatFP_1 = buildBeregningsresultatFP(Optional.empty());
        BeregningsresultatPeriode b1Periode_1 = buildBeregningsresultatPeriode(beregningsresultatFP_1, 1, 10);
        //Andeler for bruker i periode#1
        BeregningsresultatAndel b1Andel = buildBeregningsresultatAndel(b1Periode_1, true, 1500, BigDecimal.valueOf(100), virksomhet,
            AktivitetStatus.ARBEIDSTAKER, Inntektskategori.ARBEIDSTAKER);
        //Andeler for bruker i periode#2
        BeregningsresultatPeriode b1Periode_2 = buildBeregningsresultatPeriode(beregningsresultatFP_1, 11, 20);
        buildBeregningsresultatAndel(b1Periode_2, true, 1500, BigDecimal.valueOf(100), virksomhet,
            AktivitetStatus.ARBEIDSTAKER, Inntektskategori.ARBEIDSTAKER);
        BeregningsresultatFeriepenger b1_feriepenger = buildBeregningsresultatFeriepenger(beregningsresultatFP_1);
        buildBeregningsresultatFeriepengerPrÅr(b1_feriepenger, b1Andel, 3000L, List.of(NyOppdragskontrollTjenesteTestBase.DAGENS_DATO));
        TilkjentYtelseMapper mapper = new TilkjentYtelseMapper(FamilieYtelseType.FØDSEL);
        GruppertYtelse gruppertYtelse = mapper.fordelPåNøkler(beregningsresultatFP_1);
        var builder = getInputStandardBuilder(gruppertYtelse);
        Oppdragskontroll originaltOppdrag = OppdragMedPositivKvitteringTestUtil.opprett(nyOppdragskontrollTjeneste, builder.build());

        // Arrange : Revurdering
        LocalDate endringsdato = b1Periode_2.getBeregningsresultatPeriodeTom().plusDays(1);
        BeregningsresultatEntitet beregningsresultatRevurderingFP = buildBeregningsresultatFP(Optional.of(endringsdato));
        BeregningsresultatPeriode b2Periode_1 = buildBeregningsresultatPeriode(beregningsresultatRevurderingFP, 1, 10);
        //Andeler for bruker i periode#1
        BeregningsresultatAndel b2Andel = buildBeregningsresultatAndel(b2Periode_1, true, 1500, BigDecimal.valueOf(100), virksomhet,
            AktivitetStatus.ARBEIDSTAKER, Inntektskategori.ARBEIDSTAKER);
        //Andeler for bruker i periode#2
        BeregningsresultatPeriode b2Periode_2 = buildBeregningsresultatPeriode(beregningsresultatRevurderingFP, 11, 20);
        buildBeregningsresultatAndel(b2Periode_2, true, 1500, BigDecimal.valueOf(100), virksomhet,
            AktivitetStatus.ARBEIDSTAKER, Inntektskategori.ARBEIDSTAKER);
        //Andeler for bruker i periode#3
        BeregningsresultatPeriode b2Periode_3 = buildBeregningsresultatPeriode(beregningsresultatRevurderingFP, 21, 30);
        buildBeregningsresultatAndel(b2Periode_3, true, 1500, BigDecimal.valueOf(100), virksomhet,
            AktivitetStatus.ARBEIDSTAKER, Inntektskategori.ARBEIDSTAKER);
        BeregningsresultatFeriepenger b2_feriepenger = buildBeregningsresultatFeriepenger(beregningsresultatRevurderingFP);
        buildBeregningsresultatFeriepengerPrÅr(b2_feriepenger, b2Andel, 3000L, List.of(NyOppdragskontrollTjenesteTestBase.DAGENS_DATO, NyOppdragskontrollTjenesteTestBase.DAGENS_DATO.plusYears(1)));
        GruppertYtelse gruppertYtelse2 = mapper.fordelPåNøkler(beregningsresultatRevurderingFP);
        var builder2 = getInputStandardBuilder(gruppertYtelse2).medTidligereOppdrag(mapTidligereOppdrag(List.of(originaltOppdrag)));

        // Act
        Oppdragskontroll oppdragRevurdering = OppdragMedPositivKvitteringTestUtil.opprett(nyOppdragskontrollTjeneste, builder2.build());

        //Assert
        assertThat(oppdragRevurdering).isNotNull();
        //Oppdrag110
        List<Oppdrag110> originalOpp110Liste = originaltOppdrag.getOppdrag110Liste();
        assertThat(originalOpp110Liste).hasSize(1);
        List<Oppdrag110> opp110ListeForRevurdering = oppdragRevurdering.getOppdrag110Liste();
        assertThat(opp110ListeForRevurdering).hasSize(1);
        Oppdrag110 oppdrag110ForBruker = opp110ListeForRevurdering.get(0);
        //Oppdragslinje150
        List<Oppdragslinje150> opp150ListeForBruker = oppdrag110ForBruker.getOppdragslinje150Liste();
        assertThat(opp150ListeForBruker).hasSize(2);
        //Oppdragslinje150 for feriepenger
        List<Oppdragslinje150> opp150ForFeriepengerList = OppdragskontrollTestVerktøy.getOppdragslinje150MedKlassekode(opp150ListeForBruker,
            KodeKlassifik.FERIEPENGER_BRUKER);
        assertThat(opp150ForFeriepengerList).hasSize(1);
        Oppdragslinje150 opp150ForFeriepenger = opp150ForFeriepengerList.get(0);
        assertThat(opp150ForFeriepenger.gjelderOpphør()).isFalse();
        assertThat(opp150ForFeriepenger.getDatoVedtakFom().getYear()).isEqualTo(I_ÅR + 2);
        //Oppdragslinje150 for AT
        List<Oppdragslinje150> opp150ForATIRevurderingList = OppdragskontrollTestVerktøy.getOppdragslinje150MedKlassekode(opp150ListeForBruker,
            KodeKlassifik.FPF_ARBEIDSTAKER);
        assertThat(opp150ForATIRevurderingList).hasSize(1);
        Oppdragslinje150 opp150ForATIRevurdering = opp150ForATIRevurderingList.get(0);
        assertThat(opp150ForATIRevurdering.gjelderOpphør()).isFalse();
        List<Oppdragslinje150> tidligereOpp150ForATList = OppdragskontrollTestVerktøy.getOppdragslinje150MedKlassekode(originalOpp110Liste.get(0).getOppdragslinje150Liste(),
            KodeKlassifik.FPF_ARBEIDSTAKER);
        Oppdragslinje150 sisteOpp150ForAT = tidligereOpp150ForATList.stream().max(Comparator.comparing(Oppdragslinje150::getDelytelseId)).get();
        assertThat(opp150ForATIRevurdering.getRefDelytelseId()).isEqualTo(sisteOpp150ForAT.getDelytelseId());
    }

    /**
     * <p>
     * Førstegangsbehandling <br>
     * Perioder: To perioder <br>
     * Mottakere: Bruker og arbeidsgiver(har andel kun før endringsdato), Inntektskategori: AT<br>
     * <p>
     * Revurdering <br>
     * Perioder: To perioder <br>
     * Mottakere: Bruker og arbeidsgiver(har andel kun før endringsdato), Inntektskategori: AT<br>
     * Endringsdato: Start dato av andre periode i revurdering
     */
    @Test
    public void skalIkkeSendeOppdragForArbeidsgiverHvisDetFinnesIngenAndelerFomEndringsdatoIRevurderingOgIngenAndelerSomSkalOpphøresIForrige() {

        // Arrange : Førstegangsbehandling
        BeregningsresultatEntitet beregningsresultatFP_1 = buildBeregningsresultatFP(Optional.empty());
        // Periode#1
        BeregningsresultatPeriode b1Periode_1 = buildBeregningsresultatPeriode(beregningsresultatFP_1, 1, 10);
        //Andel for bruker i periode#1
        buildBeregningsresultatAndel(b1Periode_1, true, 1100, BigDecimal.valueOf(100), virksomhet,
            AktivitetStatus.ARBEIDSTAKER, Inntektskategori.ARBEIDSTAKER);
        //Andel for arbeidsgiver i periode#1
        buildBeregningsresultatAndel(b1Periode_1, false, 1000, BigDecimal.valueOf(100), virksomhet,
            AktivitetStatus.ARBEIDSTAKER, Inntektskategori.ARBEIDSTAKER);
        //Periode#2
        BeregningsresultatPeriode b1Periode_2 = buildBeregningsresultatPeriode(beregningsresultatFP_1, 11, 20);
        //Andel for bruker i periode#2
        buildBeregningsresultatAndel(b1Periode_2, true, 1500, BigDecimal.valueOf(100), virksomhet,
            AktivitetStatus.ARBEIDSTAKER, Inntektskategori.ARBEIDSTAKER);
        TilkjentYtelseMapper mapper = new TilkjentYtelseMapper(FamilieYtelseType.FØDSEL);
        GruppertYtelse gruppertYtelse = mapper.fordelPåNøkler(beregningsresultatFP_1);
        var builder = getInputStandardBuilder(gruppertYtelse);
        Oppdragskontroll originaltOppdrag = OppdragMedPositivKvitteringTestUtil.opprett(nyOppdragskontrollTjeneste, builder.build());

        // Arrange : Revurdering
        LocalDate endringsdato = b1Periode_2.getBeregningsresultatPeriodeFom();
        BeregningsresultatEntitet beregningsresultatRevurderingFP = buildBeregningsresultatFP(Optional.of(endringsdato));
        BeregningsresultatPeriode b2Periode_1 = buildBeregningsresultatPeriode(beregningsresultatRevurderingFP, 1, 10);
        //Andel for bruker i periode#1
        buildBeregningsresultatAndel(b2Periode_1, true, 1100, BigDecimal.valueOf(100), virksomhet,
            AktivitetStatus.ARBEIDSTAKER, Inntektskategori.ARBEIDSTAKER);
        //Andel for arbeidsgiver i periode#1
        buildBeregningsresultatAndel(b2Periode_1, false, 1000, BigDecimal.valueOf(100), virksomhet,
            AktivitetStatus.ARBEIDSTAKER, Inntektskategori.ARBEIDSTAKER);
        //Andeler for bruker i periode#2
        BeregningsresultatPeriode b2Periode_2 = buildBeregningsresultatPeriode(beregningsresultatRevurderingFP, 11, 20);
        buildBeregningsresultatAndel(b2Periode_2, true, 800, BigDecimal.valueOf(100), virksomhet,
            AktivitetStatus.ARBEIDSTAKER, Inntektskategori.ARBEIDSTAKER);

        GruppertYtelse gruppertYtelse2 = mapper.fordelPåNøkler(beregningsresultatRevurderingFP);
        var builder2 = getInputStandardBuilder(gruppertYtelse2).medTidligereOppdrag(mapTidligereOppdrag(List.of(originaltOppdrag)));

        // Act
        Oppdragskontroll oppdragRevurdering = OppdragMedPositivKvitteringTestUtil.opprett(nyOppdragskontrollTjeneste, builder2.build());

        //Assert
        assertThat(oppdragRevurdering).isNotNull();
        //Oppdrag110
        List<Oppdrag110> originalOpp110Liste = originaltOppdrag.getOppdrag110Liste();
        assertThat(originalOpp110Liste).hasSize(2);
        List<Oppdrag110> opp110ListeForRevurdering = oppdragRevurdering.getOppdrag110Liste();
        assertThat(opp110ListeForRevurdering).hasSize(1);
        Oppdrag110 oppdrag110ForBruker = opp110ListeForRevurdering.get(0);
        assertThat(!oppdrag110ForBruker.getKodeFagomrade().gjelderRefusjonTilArbeidsgiver()).isTrue();
        //Oppdragslinje150
        List<Oppdragslinje150> opp150ListeForBruker = oppdrag110ForBruker.getOppdragslinje150Liste();
        assertThat(opp150ListeForBruker).hasSize(2);
    }

    /**
     * <p>
     * Førstegangsbehandling <br>
     * Perioder: To perioder <br>
     * Mottakere: Bruker(har andel kun før endringsdato) og arbeidsgiver, Inntektskategori: AT<br>
     * <p>
     * Revurdering <br>
     * Perioder: To perioder <br>
     * Mottakere: Bruker(har andel kun før endringsdato) og arbeidsgiver, Inntektskategori: AT<br>
     * Endringsdato: Start dato av andre periode i revurdering
     */
    @Test
    public void skalIkkeSendeOppdragForBrukerHvisDetFinnesIngenAndelerFomEndringsdatoIRevurderingOgIngenAndelerSomSkalOpphøresIForrige() {

        // Arrange : Førstegangsbehandling
        BeregningsresultatEntitet beregningsresultatFP_1 = buildBeregningsresultatFP(Optional.empty());
        // Periode#1
        BeregningsresultatPeriode b1Periode_1 = buildBeregningsresultatPeriode(beregningsresultatFP_1, 1, 10);
        //Andel for bruker i periode#1
        buildBeregningsresultatAndel(b1Periode_1, true, 1100, BigDecimal.valueOf(100), virksomhet,
            AktivitetStatus.ARBEIDSTAKER, Inntektskategori.ARBEIDSTAKER);
        //Andel for arbeidsgiver i periode#1
        buildBeregningsresultatAndel(b1Periode_1, false, 1000, BigDecimal.valueOf(100), virksomhet,
            AktivitetStatus.ARBEIDSTAKER, Inntektskategori.ARBEIDSTAKER);
        //Periode#2
        BeregningsresultatPeriode b1Periode_2 = buildBeregningsresultatPeriode(beregningsresultatFP_1, 11, 20);
        //Andel for arbeidsgiver i periode#2
        buildBeregningsresultatAndel(b1Periode_2, false, 1500, BigDecimal.valueOf(100), virksomhet,
            AktivitetStatus.ARBEIDSTAKER, Inntektskategori.ARBEIDSTAKER);
        TilkjentYtelseMapper mapper = new TilkjentYtelseMapper(FamilieYtelseType.FØDSEL);
        GruppertYtelse gruppertYtelse = mapper.fordelPåNøkler(beregningsresultatFP_1);
        var builder = getInputStandardBuilder(gruppertYtelse);
        Oppdragskontroll originaltOppdrag = OppdragMedPositivKvitteringTestUtil.opprett(nyOppdragskontrollTjeneste, builder.build());

        // Arrange : Revurdering
        LocalDate endringsdato = b1Periode_2.getBeregningsresultatPeriodeFom();
        BeregningsresultatEntitet beregningsresultatRevurderingFP = buildBeregningsresultatFP(Optional.of(endringsdato));
        BeregningsresultatPeriode b2Periode_1 = buildBeregningsresultatPeriode(beregningsresultatRevurderingFP, 1, 10);
        //Andel for bruker i periode#1
        buildBeregningsresultatAndel(b2Periode_1, true, 1100, BigDecimal.valueOf(100), virksomhet,
            AktivitetStatus.ARBEIDSTAKER, Inntektskategori.ARBEIDSTAKER);
        //Andel for arbeidsgiver i periode#1
        buildBeregningsresultatAndel(b2Periode_1, false, 1000, BigDecimal.valueOf(100), virksomhet,
            AktivitetStatus.ARBEIDSTAKER, Inntektskategori.ARBEIDSTAKER);
        //Andeler for arbeidsgiver i periode#2
        BeregningsresultatPeriode b2Periode_2 = buildBeregningsresultatPeriode(beregningsresultatRevurderingFP, 11, 20);
        buildBeregningsresultatAndel(b2Periode_2, false, 800, BigDecimal.valueOf(100), virksomhet,
            AktivitetStatus.ARBEIDSTAKER, Inntektskategori.ARBEIDSTAKER);
        GruppertYtelse gruppertYtelse2 = mapper.fordelPåNøkler(beregningsresultatRevurderingFP);
        var builder2 = getInputStandardBuilder(gruppertYtelse2).medTidligereOppdrag(mapTidligereOppdrag(List.of(originaltOppdrag)));

        // Act
        Oppdragskontroll oppdragRevurdering = OppdragMedPositivKvitteringTestUtil.opprett(nyOppdragskontrollTjeneste, builder2.build());

        //Assert
        assertThat(oppdragRevurdering).isNotNull();
        //Oppdrag110
        List<Oppdrag110> originalOpp110Liste = originaltOppdrag.getOppdrag110Liste();
        assertThat(originalOpp110Liste).hasSize(2);
        List<Oppdrag110> opp110ListeForRevurdering = oppdragRevurdering.getOppdrag110Liste();
        assertThat(opp110ListeForRevurdering).hasSize(1);
        Oppdrag110 oppdrag110ForArbeidsgiver = opp110ListeForRevurdering.get(0);
        assertThat(oppdrag110ForArbeidsgiver.getKodeFagomrade()).isNotEqualTo(KodeFagområde.FORELDREPENGER_BRUKER);
        //Oppdragslinje150
        List<Oppdragslinje150> opp150ListeForArbeidsgiver = oppdrag110ForArbeidsgiver.getOppdragslinje150Liste();
        assertThat(opp150ListeForArbeidsgiver).hasSize(2);
    }

    /**
     * <p>
     * Førstegangsbehandling <br>
     * Perioder: To perioder <br>
     * Mottakere: Bruker(har andeler kun før endringsdato) og arbeidsgiver, Inntektskategori: AT og FL<br>
     * <p>
     * Revurdering <br>
     * Perioder: To perioder <br>
     * Mottakere: Bruker(har andel kun før endringsdato) og arbeidsgiver, Inntektskategori: AT og FL<br>
     * Endringsdato: Start dato av andre periode i revurdering
     */
    @Test
    public void skalIkkeSendeOppdragForBrukerMedFlereInntektskategoriHvisDetFinnesIngenAndelerFomEndringsdatoIRevurderingOgIngenAndelerSomSkalOpphøresIForrige() {

        // Arrange : Førstegangsbehandling
        BeregningsresultatEntitet beregningsresultatFP_1 = buildBeregningsresultatFP(Optional.empty());
        // Periode#1
        BeregningsresultatPeriode b1Periode_1 = buildBeregningsresultatPeriode(beregningsresultatFP_1, 1, 10);
        //Andeler for bruker i periode#1
        buildBeregningsresultatAndel(b1Periode_1, true, 1100, BigDecimal.valueOf(100), virksomhet,
            AktivitetStatus.ARBEIDSTAKER, Inntektskategori.ARBEIDSTAKER);
        buildBeregningsresultatAndel(b1Periode_1, true, 1100, BigDecimal.valueOf(100), null,
            AktivitetStatus.FRILANSER, Inntektskategori.FRILANSER);
        //Andel for arbeidsgiver i periode#1
        buildBeregningsresultatAndel(b1Periode_1, false, 1000, BigDecimal.valueOf(100), virksomhet,
            AktivitetStatus.ARBEIDSTAKER, Inntektskategori.ARBEIDSTAKER);
        //Periode#2
        BeregningsresultatPeriode b1Periode_2 = buildBeregningsresultatPeriode(beregningsresultatFP_1, 11, 20);
        //Andel for arbeidsgiver i periode#2
        buildBeregningsresultatAndel(b1Periode_2, false, 1500, BigDecimal.valueOf(100), virksomhet,
            AktivitetStatus.ARBEIDSTAKER, Inntektskategori.ARBEIDSTAKER);
        TilkjentYtelseMapper mapper = new TilkjentYtelseMapper(FamilieYtelseType.FØDSEL);
        GruppertYtelse gruppertYtelse = mapper.fordelPåNøkler(beregningsresultatFP_1);
        var builder = getInputStandardBuilder(gruppertYtelse);
        Oppdragskontroll originaltOppdrag = OppdragMedPositivKvitteringTestUtil.opprett(nyOppdragskontrollTjeneste, builder.build());

        // Arrange : Revurdering
        LocalDate endringsdato = b1Periode_2.getBeregningsresultatPeriodeFom();
        BeregningsresultatEntitet beregningsresultatRevurderingFP = buildBeregningsresultatFP(Optional.of(endringsdato));
        BeregningsresultatPeriode b2Periode_1 = buildBeregningsresultatPeriode(beregningsresultatRevurderingFP, 1, 10);
        //Andel for bruker i periode#1
        buildBeregningsresultatAndel(b2Periode_1, true, 1100, BigDecimal.valueOf(100), virksomhet,
            AktivitetStatus.ARBEIDSTAKER, Inntektskategori.ARBEIDSTAKER);
        buildBeregningsresultatAndel(b2Periode_1, true, 1100, BigDecimal.valueOf(100), null,
            AktivitetStatus.FRILANSER, Inntektskategori.FRILANSER);
        //Andel for arbeidsgiver i periode#1
        buildBeregningsresultatAndel(b2Periode_1, false, 1000, BigDecimal.valueOf(100), virksomhet,
            AktivitetStatus.ARBEIDSTAKER, Inntektskategori.ARBEIDSTAKER);
        //Andeler for arbeidsgiver i periode#2
        BeregningsresultatPeriode b2Periode_2 = buildBeregningsresultatPeriode(beregningsresultatRevurderingFP, 11, 20);
        buildBeregningsresultatAndel(b2Periode_2, false, 800, BigDecimal.valueOf(100), virksomhet,
            AktivitetStatus.ARBEIDSTAKER, Inntektskategori.ARBEIDSTAKER);
        GruppertYtelse gruppertYtelse2 = mapper.fordelPåNøkler(beregningsresultatRevurderingFP);
        var builder2 = getInputStandardBuilder(gruppertYtelse2).medTidligereOppdrag(mapTidligereOppdrag(List.of(originaltOppdrag)));

        // Act
        Oppdragskontroll oppdragRevurdering = OppdragMedPositivKvitteringTestUtil.opprett(nyOppdragskontrollTjeneste, builder2.build());

        //Assert
        assertThat(oppdragRevurdering).isNotNull();
        //Oppdrag110
        List<Oppdrag110> originalOpp110Liste = originaltOppdrag.getOppdrag110Liste();
        assertThat(originalOpp110Liste).hasSize(2);
        List<Oppdrag110> opp110ListeForRevurdering = oppdragRevurdering.getOppdrag110Liste();
        assertThat(opp110ListeForRevurdering).hasSize(1);
        Oppdrag110 oppdrag110ForArbeidsgiver = opp110ListeForRevurdering.get(0);
        assertThat(!oppdrag110ForArbeidsgiver.getKodeFagomrade().gjelderRefusjonTilArbeidsgiver()).isFalse();
        //Oppdragslinje150
        List<Oppdragslinje150> opp150ListeForArbeidsgiver = oppdrag110ForArbeidsgiver.getOppdragslinje150Liste();
        assertThat(opp150ListeForArbeidsgiver).hasSize(2);
    }

    /**
     * Førstegangsbehandling: Mottaker:Bruker
     * Andeler: AT(virksomhet) og AT(Privat arbgvr)
     * Revurdering: Mottaker: Bruker
     * Andeler: AT(virksomhet) og AT(Privat arbgvr)
     */
    @Test
    public void skalSendeEndringsOppdragOgSlåATAndelerSammenHvisBrukerHarArbeidsforholdHosBådeEnOrganisasjonOgPrivatArbeidsgiver() {

        // Arrange : Førstegangsbehandling
        BeregningsresultatEntitet beregningsresultat = buildBeregningsresultatFP(Optional.empty());
        BeregningsresultatPeriode b1Periode_1 = buildBeregningsresultatPeriode(beregningsresultat, 1, 10);
        buildBeregningsresultatAndel(b1Periode_1, true, 1000, BigDecimal.valueOf(100), virksomhet);
        buildBeregningsresultatAndel(b1Periode_1, true, 1200, BigDecimal.valueOf(100), null);
        BeregningsresultatPeriode b1Periode_2 = buildBeregningsresultatPeriode(beregningsresultat, 11, 20);
        buildBeregningsresultatAndel(b1Periode_2, true, 1000, BigDecimal.valueOf(100), virksomhet);
        buildBeregningsresultatAndel(b1Periode_2, true, 1200, BigDecimal.valueOf(100), null);
        TilkjentYtelseMapper mapper = new TilkjentYtelseMapper(FamilieYtelseType.FØDSEL);
        GruppertYtelse gruppertYtelse = mapper.fordelPåNøkler(beregningsresultat);
        var builder = getInputStandardBuilder(gruppertYtelse);
        Oppdragskontroll originaltOppdrag = OppdragMedPositivKvitteringTestUtil.opprett(nyOppdragskontrollTjeneste, builder.build());

        // Arrange : Revurdering
        LocalDate endringsdato = NyOppdragskontrollTjenesteTestBase.DAGENS_DATO.plusDays(1);
        BeregningsresultatEntitet beregningsresultatRevurderingFP = buildBeregningsresultatFP(Optional.of(endringsdato));
        BeregningsresultatPeriode b2Periode_1 = buildBeregningsresultatPeriode(beregningsresultatRevurderingFP, 1, 10);
        buildBeregningsresultatAndel(b2Periode_1, true, 1100, BigDecimal.valueOf(100), virksomhet);
        buildBeregningsresultatAndel(b2Periode_1, true, 1300, BigDecimal.valueOf(100), null);
        BeregningsresultatPeriode b2Periode_2 = buildBeregningsresultatPeriode(beregningsresultatRevurderingFP, 11, 20);
        buildBeregningsresultatAndel(b2Periode_2, true, 1100, BigDecimal.valueOf(100), virksomhet);
        buildBeregningsresultatAndel(b2Periode_2, true, 1300, BigDecimal.valueOf(100), null);
        GruppertYtelse gruppertYtelse2 = mapper.fordelPåNøkler(beregningsresultatRevurderingFP);
        var builder2 = getInputStandardBuilder(gruppertYtelse2).medTidligereOppdrag(mapTidligereOppdrag(List.of(originaltOppdrag)));

        // Act
        Oppdragskontroll oppdragRevurdering = OppdragMedPositivKvitteringTestUtil.opprett(nyOppdragskontrollTjeneste, builder2.build());

        // Assert : Revurdering
        List<Oppdrag110> oppdra110BrukerList = oppdragRevurdering.getOppdrag110Liste();
        assertThat(oppdra110BrukerList).hasSize(1);
        assertThat(oppdra110BrukerList.get(0).getKodeEndring()).isEqualTo(KodeEndring.ENDRING);
        List<Oppdragslinje150> alleOpp150BrukerListe = oppdra110BrukerList.get(0).getOppdragslinje150Liste();
        assertThat(alleOpp150BrukerListe).hasSize(3);
        assertThat(alleOpp150BrukerListe).anySatisfy(opp150 ->
            assertThat(opp150.gjelderOpphør()).isTrue());
        List<Oppdragslinje150> opp150UtenOpphBrukerListe = alleOpp150BrukerListe.stream()
            .filter(opp150 -> !opp150.gjelderOpphør())
            .collect(Collectors.toList());
        assertThat(opp150UtenOpphBrukerListe).hasSize(2);
        assertThat(opp150UtenOpphBrukerListe).allSatisfy(opp150 -> {
                assertThat(opp150.getSats()).isEqualTo(Sats.på(2400L));
                assertThat(opp150.getRefDelytelseId()).isNotNull();
                assertThat(opp150.getRefFagsystemId()).isNotNull();
            }
        );
    }

    /**
     * Førstegangsbehandling: Mottakere:Bruker og privat arbeidsgiver
     * Andeler: AT(virksomhet), AT(Privat arbgvr), Refusjon - AT(Privat arbgvr)
     * Revurdering: Mottakere: Bruker og privat arbeidsgiver
     * Andeler: AT(virksomhet), AT(Privat arbgvr), Refusjon - AT(Privat arbgvr)
     */
    @Test
    public void skalSendeEndringsOppdragOgSlåATAndelerSammenHvisBrukerHarArbeidsforholdHosBådeEnOrganisasjonOgPrivatArbgvrOgFinnesRefusjonTilPrivatArbgvr() {

        // Arrange : Førstegangsbehandling
        BeregningsresultatEntitet beregningsresultat = buildBeregningsresultatFP(Optional.empty());
        BeregningsresultatPeriode b1Periode_1 = buildBeregningsresultatPeriode(beregningsresultat, 1, 10);
        buildBeregningsresultatAndel(b1Periode_1, true, 1000, BigDecimal.valueOf(100), virksomhet);
        buildBeregningsresultatAndel(b1Periode_1, true, 1200, BigDecimal.valueOf(100), null);
        buildBeregningsresultatAndel(b1Periode_1, false, 1200, BigDecimal.valueOf(100), null);
        BeregningsresultatPeriode b1Periode_2 = buildBeregningsresultatPeriode(beregningsresultat, 11, 20);
        buildBeregningsresultatAndel(b1Periode_2, true, 1000, BigDecimal.valueOf(100), virksomhet);
        buildBeregningsresultatAndel(b1Periode_2, true, 1200, BigDecimal.valueOf(100), null);
        buildBeregningsresultatAndel(b1Periode_2, false, 1200, BigDecimal.valueOf(100), null);
        TilkjentYtelseMapper mapper = new TilkjentYtelseMapper(FamilieYtelseType.FØDSEL);
        GruppertYtelse gruppertYtelse = mapper.fordelPåNøkler(beregningsresultat);
        var builder = getInputStandardBuilder(gruppertYtelse);
        Oppdragskontroll originaltOppdrag = OppdragMedPositivKvitteringTestUtil.opprett(nyOppdragskontrollTjeneste, builder.build());

        // Arrange : Revurdering
        LocalDate endringsdato = NyOppdragskontrollTjenesteTestBase.DAGENS_DATO.plusDays(1);
        BeregningsresultatEntitet beregningsresultatRevurderingFP = buildBeregningsresultatFP(Optional.of(endringsdato));
        BeregningsresultatPeriode b2Periode_1 = buildBeregningsresultatPeriode(beregningsresultatRevurderingFP, 1, 10);
        buildBeregningsresultatAndel(b2Periode_1, true, 1100, BigDecimal.valueOf(100), virksomhet);
        buildBeregningsresultatAndel(b2Periode_1, true, 1300, BigDecimal.valueOf(100), null);
        buildBeregningsresultatAndel(b2Periode_1, false, 1300, BigDecimal.valueOf(100), null);
        BeregningsresultatPeriode b2Periode_2 = buildBeregningsresultatPeriode(beregningsresultatRevurderingFP, 11, 20);
        buildBeregningsresultatAndel(b2Periode_2, true, 1100, BigDecimal.valueOf(100), virksomhet);
        buildBeregningsresultatAndel(b2Periode_2, true, 1300, BigDecimal.valueOf(100), null);
        buildBeregningsresultatAndel(b2Periode_2, false, 1300, BigDecimal.valueOf(100), null);
        GruppertYtelse gruppertYtelse2 = mapper.fordelPåNøkler(beregningsresultatRevurderingFP);
        var builder2 = getInputStandardBuilder(gruppertYtelse2).medTidligereOppdrag(mapTidligereOppdrag(List.of(originaltOppdrag)));

        // Act
        Oppdragskontroll oppdragRevurdering = OppdragMedPositivKvitteringTestUtil.opprett(nyOppdragskontrollTjeneste, builder2.build());

        // Assert : Revurdering
        List<Oppdrag110> oppdra110BrukerList = oppdragRevurdering.getOppdrag110Liste();
        assertThat(oppdra110BrukerList).hasSize(1);
        assertThat(oppdra110BrukerList.get(0).getKodeEndring()).isEqualTo(KodeEndring.ENDRING);
        List<Oppdragslinje150> alleOpp150BrukerListe = oppdra110BrukerList.get(0).getOppdragslinje150Liste();
        assertThat(alleOpp150BrukerListe).hasSize(3);
        assertThat(alleOpp150BrukerListe).anySatisfy(opp150 ->
            assertThat(opp150.gjelderOpphør()).isTrue());
        List<Oppdragslinje150> opp150UtenOpphBrukerListe = alleOpp150BrukerListe.stream()
            .filter(opp150 -> !opp150.gjelderOpphør())
            .collect(Collectors.toList());
        assertThat(opp150UtenOpphBrukerListe).hasSize(2);
        assertThat(opp150UtenOpphBrukerListe).allSatisfy(opp150 -> {
                assertThat(opp150.getSats()).isEqualTo(Sats.på(3700L));
                assertThat(opp150.getRefDelytelseId()).isNotNull();
                assertThat(opp150.getRefFagsystemId()).isNotNull();
            }
        );
    }

    /**
     * Førstegangsbehandling: Mottaker: Privat arbeidsgiver
     * Andeler: Refusjon - AT(Privat arbgvr)
     * Revurdering: Mottaker: Bruker
     * Andeler: AT(Privat arbgvr)
     */
    @Test
    @Disabled // må vurdere hvordan man skal løse problemet - send oppgave tit NØS uansett?
    public void skalSendeEndringsOppdragNårPrivatArbgvrHarRefusjonIForrigeBehandlingOgBrukerBlirMottakerIRevurdering() {

        // Arrange : Førstegangsbehandling
        BeregningsresultatEntitet beregningsresultat = buildBeregningsresultatFP(Optional.empty());
        BeregningsresultatPeriode b1Periode_1 = buildBeregningsresultatPeriode(beregningsresultat, 1, 10);
        buildBeregningsresultatAndel(b1Periode_1, false, 1200, BigDecimal.valueOf(100), null);
        BeregningsresultatPeriode b1Periode_2 = buildBeregningsresultatPeriode(beregningsresultat, 11, 20);
        buildBeregningsresultatAndel(b1Periode_2, false, 1200, BigDecimal.valueOf(100), null);
        TilkjentYtelseMapper mapper = new TilkjentYtelseMapper(FamilieYtelseType.FØDSEL);
        GruppertYtelse gruppertYtelse = mapper.fordelPåNøkler(beregningsresultat);
        var builder = getInputStandardBuilder(gruppertYtelse);
        Oppdragskontroll originaltOppdrag = OppdragMedPositivKvitteringTestUtil.opprett(nyOppdragskontrollTjeneste, builder.build());

        // Arrange : Revurdering
        LocalDate endringsdato = NyOppdragskontrollTjenesteTestBase.DAGENS_DATO.plusDays(1);
        BeregningsresultatEntitet beregningsresultatRevurderingFP = buildBeregningsresultatFP(Optional.of(endringsdato));
        BeregningsresultatPeriode b2Periode_1 = buildBeregningsresultatPeriode(beregningsresultatRevurderingFP, 1, 10);
        buildBeregningsresultatAndel(b2Periode_1, true, 1200, BigDecimal.valueOf(100), null);
        BeregningsresultatPeriode b2Periode_2 = buildBeregningsresultatPeriode(beregningsresultatRevurderingFP, 11, 20);
        buildBeregningsresultatAndel(b2Periode_2, true, 1200, BigDecimal.valueOf(100), null);

        GruppertYtelse gruppertYtelse2 = mapper.fordelPåNøkler(beregningsresultatRevurderingFP);
        var builder2 = getInputStandardBuilder(gruppertYtelse2).medTidligereOppdrag(mapTidligereOppdrag(List.of(originaltOppdrag)));

        // Act
        Oppdragskontroll oppdragRevurdering = OppdragMedPositivKvitteringTestUtil.opprett(nyOppdragskontrollTjeneste, builder2.build());

        // Assert : Revurdering
        List<Oppdrag110> oppdra110BrukerList = oppdragRevurdering.getOppdrag110Liste();
        assertThat(oppdra110BrukerList).hasSize(1);
        assertThat(oppdra110BrukerList.get(0).getKodeEndring()).isEqualTo(KodeEndring.ENDRING);
        List<Oppdragslinje150> alleOpp150BrukerListe = oppdra110BrukerList.get(0).getOppdragslinje150Liste();
        assertThat(alleOpp150BrukerListe).hasSize(3);
        assertThat(alleOpp150BrukerListe).anySatisfy(opp150 ->
            assertThat(opp150.gjelderOpphør()).isTrue());
        List<Oppdragslinje150> opp150UtenOpphBrukerListe = alleOpp150BrukerListe.stream()
            .filter(opp150 -> !opp150.gjelderOpphør())
            .collect(Collectors.toList());
        assertThat(opp150UtenOpphBrukerListe).hasSize(2);
        assertThat(opp150UtenOpphBrukerListe).allSatisfy(opp150 -> {
                assertThat(opp150.getSats()).isEqualTo(Sats.på(1200));
                assertThat(opp150.getRefDelytelseId()).isNotNull();
                assertThat(opp150.getRefFagsystemId()).isNotNull();
            }
        );
    }

    /**
     * Førstegangsbehandling: Mottaker: Arbeidsgiver(Virksomhet)
     * Andeler: Refusjon - AT(Virksomhet)
     * Revurdering: Mottaker: Privat arbgvr
     * Andeler: Refusjon - AT(Privat arbgvr)
     */
    @Test
    public void skalSendeFørstegangsoppdragForBrukerSomEnDelAvEndringsOppdragNårPrivatArbgvrIkkeErMottakerIForrigeOgHarRefusjonFørstegangIRevurdering() {

        // Arrange : Førstegangsbehandling
        BeregningsresultatEntitet beregningsresultat = buildBeregningsresultatFP(Optional.empty());
        BeregningsresultatPeriode b1Periode_1 = buildBeregningsresultatPeriode(beregningsresultat, 1, 10);
        buildBeregningsresultatAndel(b1Periode_1, false, 1200, BigDecimal.valueOf(100), virksomhet);
        BeregningsresultatPeriode b1Periode_2 = buildBeregningsresultatPeriode(beregningsresultat, 11, 20);
        buildBeregningsresultatAndel(b1Periode_2, false, 1200, BigDecimal.valueOf(100), virksomhet);
        TilkjentYtelseMapper mapper = new TilkjentYtelseMapper(FamilieYtelseType.FØDSEL);

        GruppertYtelse gruppertYtelse = mapper.fordelPåNøkler(beregningsresultat);
        var builder = getInputStandardBuilder(gruppertYtelse);
        Oppdragskontroll originaltOppdrag = OppdragMedPositivKvitteringTestUtil.opprett(nyOppdragskontrollTjeneste, builder.build());

        // Arrange : Revurdering
        LocalDate endringsdato = NyOppdragskontrollTjenesteTestBase.DAGENS_DATO.plusDays(1);
        BeregningsresultatEntitet beregningsresultatRevurderingFP = buildBeregningsresultatFP(Optional.of(endringsdato));
        BeregningsresultatPeriode b2Periode_1 = buildBeregningsresultatPeriode(beregningsresultatRevurderingFP, 1, 10);
        buildBeregningsresultatAndel(b2Periode_1, false, 1200, BigDecimal.valueOf(100), virksomhet);
        buildBeregningsresultatAndel(b2Periode_1, false, 1000, BigDecimal.valueOf(100), null);
        BeregningsresultatPeriode b2Periode_2 = buildBeregningsresultatPeriode(beregningsresultatRevurderingFP, 11, 20);
        buildBeregningsresultatAndel(b2Periode_2, false, 1200, BigDecimal.valueOf(100), virksomhet);
        buildBeregningsresultatAndel(b2Periode_2, false, 1000, BigDecimal.valueOf(100), null);

        GruppertYtelse gruppertYtelse2 = mapper.fordelPåNøkler(beregningsresultatRevurderingFP);
        var builder2 = getInputStandardBuilder(gruppertYtelse2).medTidligereOppdrag(mapTidligereOppdrag(List.of(originaltOppdrag)));
        // Act
        Oppdragskontroll oppdragRevurdering = OppdragMedPositivKvitteringTestUtil.opprett(nyOppdragskontrollTjeneste, builder2.build());

        // Assert : Revurdering
        List<Oppdrag110> oppdra110List = oppdragRevurdering.getOppdrag110Liste();
        assertThat(oppdra110List).hasSize(1); // ny oppdag for privat arbeidsgiver
        //Oppdrag110 privat arbgvr
        Oppdrag110 oppdrag110ForPrivatArbgvr = OppdragskontrollTestVerktøy.getOppdrag110ForBruker(oppdra110List);
        assertThat(oppdrag110ForPrivatArbgvr.getKodeEndring()).isEqualTo(KodeEndring.NY);
        //Oppdragslinje150 privat arbgvr
        List<Oppdragslinje150> opp150PrivatArbgvrListe = oppdrag110ForPrivatArbgvr.getOppdragslinje150Liste();
        assertThat(opp150PrivatArbgvrListe).hasSize(2);
        assertThat(opp150PrivatArbgvrListe).allSatisfy(opp150 -> {
                assertThat(opp150.getSats()).isEqualTo(Sats.på(1000));
                assertThat(opp150.gjelderOpphør()).isFalse();
            }
        );
    }

    /**
     * Førstegangsbehandling med 2 perioder samme dagsatser til AG <br>
     * Revurdering med to perioder og bortfall av all ytelse<br
     * Ny revurdering med to ny oppfylte perioder med hhv refusjon og utbetaling til bruker<br
     */
    @Test
    public void skalSendeOppdragMedOpphørNårAllInnvilgetYtelseBortfaller() {
        // Arrange
        LocalDate b10fom = LocalDate.of(I_ÅR, 7, 1);
        LocalDate b10tom = LocalDate.of(I_ÅR, 7, 31);
        LocalDate b11fom = LocalDate.of(I_ÅR, 8, 1);
        LocalDate b11tom = LocalDate.of(I_ÅR, 8, 15);
        LocalDate b12fom = LocalDate.of(I_ÅR, 9, 16);
        LocalDate b12tom = LocalDate.of(I_ÅR, 9, 30);
        BeregningsresultatEntitet beregningsresultat = buildBeregningsresultatBrukerFP(null, List.of(0, 0, 0),  List.of(0, 800, 800), b10fom, b10tom, b11fom, b11tom, b12fom, b12tom);

        TilkjentYtelseMapper mapper = new TilkjentYtelseMapper(FamilieYtelseType.FØDSEL);
        GruppertYtelse gruppertYtelse = mapper.fordelPåNøkler(beregningsresultat);
        var builder = getInputStandardBuilder(gruppertYtelse);
        Oppdragskontroll originaltOppdrag = OppdragMedPositivKvitteringTestUtil.opprett(nyOppdragskontrollTjeneste, builder.build());

        LocalDate b2p1fom = LocalDate.of(I_ÅR, 8, 1);
        LocalDate b2p1tom = LocalDate.of(I_ÅR, 8, 15);
        LocalDate b2p2fom = LocalDate.of(I_ÅR, 9, 16);
        LocalDate b2p2tom = LocalDate.of(I_ÅR, 9, 30);
        BeregningsresultatEntitet beregningsresultatRevurderingFP = buildBeregningsresultatBrukerFP(null, List.of(0, 0), List.of(0, 0), b2p1fom, b2p1tom, b2p2fom, b2p2tom);

        GruppertYtelse gruppertYtelse2 = mapper.fordelPåNøkler(beregningsresultatRevurderingFP);
        var builder2 = getInputStandardBuilder(gruppertYtelse2).medTidligereOppdrag(mapTidligereOppdrag(List.of(originaltOppdrag)));

        // Act
        Oppdragskontroll oppdragRevurdering = OppdragMedPositivKvitteringTestUtil.opprett(nyOppdragskontrollTjeneste, builder2.build());

        //Assert
        List<Oppdragslinje150> opp150RevurderingListe = oppdragRevurdering.getOppdrag110Liste().stream()
            .flatMap(oppdrag110 -> oppdrag110.getOppdragslinje150Liste().stream())
            .collect(Collectors.toList());

        assertThat(opp150RevurderingListe).hasSize(2); // AG + FP
        assertThat(opp150RevurderingListe).allSatisfy(linje -> assertThat(linje.gjelderOpphør()).isTrue());
        assertThat(opp150RevurderingListe).anySatisfy(linje -> assertThat(linje.getDatoStatusFom()).isEqualTo(b2p1fom));

        // Arrange 2
        LocalDate endringsdato = LocalDate.of(I_ÅR, 9, 1);
        LocalDate b3p1fom = LocalDate.of(I_ÅR, 9, 1);
        LocalDate b3p1tom = LocalDate.of(I_ÅR, 9, 15);
        LocalDate b3p2fom = LocalDate.of(I_ÅR, 9, 16);
        LocalDate b3p2tom = LocalDate.of(I_ÅR, 9, 30);
        BeregningsresultatEntitet beregningsresultatRevurderingFP2 = buildBeregningsresultatBrukerFP(endringsdato, List.of(0, 820), List.of(820, 0), b3p1fom, b3p1tom, b3p2fom, b3p2tom);
        GruppertYtelse gruppertYtelse3 = mapper.fordelPåNøkler(beregningsresultatRevurderingFP2);
        var builder3 = getInputStandardBuilder(gruppertYtelse3).medTidligereOppdrag(mapTidligereOppdrag(List.of(originaltOppdrag, oppdragRevurdering)));

        // Act
        Oppdragskontroll oppdragRevurdering2 = OppdragMedPositivKvitteringTestUtil.opprett(nyOppdragskontrollTjeneste, builder3.build());

        //Assert 2
        List<Oppdragslinje150> opp150RevurderingListe2 = oppdragRevurdering2.getOppdrag110Liste().stream()
            .flatMap(oppdrag110 -> oppdrag110.getOppdragslinje150Liste().stream())
            .collect(Collectors.toList());

        assertThat(opp150RevurderingListe2).hasSize(4); // AG + Bruker + 2 * FP
        assertThat(opp150RevurderingListe2).noneSatisfy(linje -> assertThat(linje.gjelderOpphør()).isTrue());
    }

    /**
     * Prodscenario der ytelse omfordeles fra 1 ag til bruker, deretter omfordeles den andre ag til bruker. Oppretter ikke 110 for siste revurdering.
     */
    @Test
    public void skalSendeOmfordeleFlereArbeidsgivereSerielt() {
        // Arrange
        LocalDate b10fom = LocalDate.of(I_ÅR-1, 11, 2);
        LocalDate b10tom = LocalDate.of(I_ÅR-1, 11, 19);
        LocalDate b20fom = LocalDate.of(I_ÅR-1, 11, 20);
        LocalDate b20tom = LocalDate.of(I_ÅR-1, 11, 30);
        LocalDate b21fom = LocalDate.of(I_ÅR-1, 12, 1);
        LocalDate b21tom = LocalDate.of(I_ÅR-1, 12, 31);
        LocalDate b30fom = LocalDate.of(I_ÅR, 1, 1);
        LocalDate b30tom = LocalDate.of(I_ÅR, 3, 4);
        LocalDate b40fom = LocalDate.of(I_ÅR, 3, 5);
        LocalDate b40tom = LocalDate.of(I_ÅR, 5, 28);
        LocalDate opptjeningsårFeriepenger = LocalDate.of(I_ÅR-1, 12, 31);

        BeregningsresultatEntitet beregningsresultat = BeregningsresultatEntitet.builder()
            .medRegelInput("clob1")
            .medRegelSporing("clob2")
            .build();

        BeregningsresultatFeriepenger feriepenger = buildBeregningsresultatFeriepenger(beregningsresultat);

        BeregningsresultatPeriode brPeriode1 = buildBeregningsresultatPeriode(beregningsresultat, b10fom, b10tom.plusDays(1));
        buildBeregningsresultatAndel(brPeriode1, true, 0, BigDecimal.valueOf(100), virksomhet);
        buildBeregningsresultatAndel(brPeriode1, true, 0, BigDecimal.valueOf(100), virksomhet2);
        var andelB1P1Org1 = buildBeregningsresultatAndel(brPeriode1, false, 789, BigDecimal.valueOf(100), virksomhet);
        buildBeregningsresultatFeriepengerPrÅr(feriepenger, andelB1P1Org1, 1207L, opptjeningsårFeriepenger);
        var andelB1P1Org2 = buildBeregningsresultatAndel(brPeriode1, false, 154, BigDecimal.valueOf(100), virksomhet2);
        buildBeregningsresultatFeriepengerPrÅr(feriepenger, andelB1P1Org2, 236L, opptjeningsårFeriepenger);

        BeregningsresultatPeriode brPeriode2 = buildBeregningsresultatPeriode(beregningsresultat, b20fom.plusDays(3), b21tom.plusDays(2));
        buildBeregningsresultatAndel(brPeriode2, true, 0, BigDecimal.valueOf(100), virksomhet);
        buildBeregningsresultatAndel(brPeriode2, true, 0, BigDecimal.valueOf(100), virksomhet2);
        var andelB1P2Org1 = buildBeregningsresultatAndel(brPeriode2, false, 789, BigDecimal.valueOf(100), virksomhet);
        buildBeregningsresultatFeriepengerPrÅr(feriepenger, andelB1P2Org1, 2334L, opptjeningsårFeriepenger);
        buildBeregningsresultatFeriepengerPrÅr(feriepenger, andelB1P2Org1, 80L, opptjeningsårFeriepenger.plusYears(1));
        var andelB1P2Org2 = buildBeregningsresultatAndel(brPeriode2, false, 154, BigDecimal.valueOf(100), virksomhet2);
        buildBeregningsresultatFeriepengerPrÅr(feriepenger, andelB1P2Org2, 456L, opptjeningsårFeriepenger);
        buildBeregningsresultatFeriepengerPrÅr(feriepenger, andelB1P2Org2, 16L, opptjeningsårFeriepenger.plusYears(1));

        BeregningsresultatPeriode brPeriode3 = buildBeregningsresultatPeriode(beregningsresultat, b30fom.plusDays(2), b30tom.plusDays(1));
        buildBeregningsresultatAndel(brPeriode3, true, 0, BigDecimal.valueOf(100), virksomhet);
        buildBeregningsresultatAndel(brPeriode3, true, 0, BigDecimal.valueOf(100), virksomhet2);
        var andelB1P3Org1 = buildBeregningsresultatAndel(brPeriode3, false, 789, BigDecimal.valueOf(100), virksomhet);
        buildBeregningsresultatFeriepengerPrÅr(feriepenger, andelB1P3Org1, 1207L, opptjeningsårFeriepenger.plusYears(1));
        var andelB1P3Org2 = buildBeregningsresultatAndel(brPeriode3, false, 154, BigDecimal.valueOf(100), virksomhet2);
        buildBeregningsresultatFeriepengerPrÅr(feriepenger, andelB1P3Org2, 236L, opptjeningsårFeriepenger.plusYears(1));

        BeregningsresultatPeriode brPeriode4 = buildBeregningsresultatPeriode(beregningsresultat, b40fom.plusDays(3), b40tom);
        buildBeregningsresultatAndel(brPeriode4, false, 789, BigDecimal.valueOf(100), virksomhet);
        buildBeregningsresultatAndel(brPeriode4, false, 154, BigDecimal.valueOf(100), virksomhet2);
        buildBeregningsresultatAndel(brPeriode4, true, 0, BigDecimal.valueOf(100), virksomhet);
        buildBeregningsresultatAndel(brPeriode4, true, 0, BigDecimal.valueOf(100), virksomhet2);

        TilkjentYtelseMapper mapper = new TilkjentYtelseMapper(FamilieYtelseType.FØDSEL);
        GruppertYtelse gruppertYtelse = mapper.fordelPåNøkler(beregningsresultat);
        var builder = getInputStandardBuilder(gruppertYtelse);
        Oppdragskontroll originaltOppdrag = OppdragMedPositivKvitteringTestUtil.opprett(nyOppdragskontrollTjeneste, builder.build());

        // To arbeidsgivere som mottakere ingen oppdrag til bruker
        assertThat(originaltOppdrag.getOppdrag110Liste().size()).isEqualTo(2);
        assertThat(originaltOppdrag.getOppdrag110Liste().stream().allMatch(oppdrag110 -> oppdrag110.getKodeFagomrade().equals(KodeFagområde.FORELDREPENGER_ARBEIDSGIVER))).isTrue();

        // Arrange 1 - første revurdering2

        BeregningsresultatEntitet beregningsresultat1 = BeregningsresultatEntitet.builder()
            .medRegelInput("clob1")
            .medRegelSporing("clob2")
            .medEndringsdato(b20fom.plusDays(1))
            .build();

        BeregningsresultatFeriepenger feriepenger1 = buildBeregningsresultatFeriepenger(beregningsresultat1);

        BeregningsresultatPeriode brR0Periode1 = buildBeregningsresultatPeriode(beregningsresultat1, b10fom, b10tom);
        buildBeregningsresultatAndel(brR0Periode1, true, 0, BigDecimal.valueOf(100), virksomhet);
        buildBeregningsresultatAndel(brR0Periode1, true, 0, BigDecimal.valueOf(100), virksomhet2);
        var andelB2P1Org1 = buildBeregningsresultatAndel(brR0Periode1, false, 789, BigDecimal.valueOf(100), virksomhet);
        buildBeregningsresultatFeriepengerPrÅr(feriepenger1, andelB2P1Org1, 1127L, opptjeningsårFeriepenger);
        var andelB2P1Org2 = buildBeregningsresultatAndel(brR0Periode1, false, 154, BigDecimal.valueOf(100), virksomhet2);
        buildBeregningsresultatFeriepengerPrÅr(feriepenger1, andelB2P1Org2, 220L, opptjeningsårFeriepenger);

        BeregningsresultatPeriode brR0Periode2 = buildBeregningsresultatPeriode(beregningsresultat1, b20fom, b21tom);
        buildBeregningsresultatAndel(brR0Periode2, true, 0, BigDecimal.valueOf(100), virksomhet);
        buildBeregningsresultatAndel(brR0Periode2, true, 0, BigDecimal.valueOf(100), virksomhet2);
        var andelB2P2Org1 = buildBeregningsresultatAndel(brR0Periode2, false, 789, BigDecimal.valueOf(100), virksomhet);
        buildBeregningsresultatFeriepengerPrÅr(feriepenger1, andelB2P2Org1, 2414L, opptjeningsårFeriepenger);
        var andelB2P2Org2 = buildBeregningsresultatAndel(brR0Periode2, false, 154, BigDecimal.valueOf(100), virksomhet2);
        buildBeregningsresultatFeriepengerPrÅr(feriepenger1, andelB2P2Org2, 471L, opptjeningsårFeriepenger);

        BeregningsresultatPeriode brR0Periode3 = buildBeregningsresultatPeriode(beregningsresultat1, b30fom, b30tom);
        buildBeregningsresultatAndel(brR0Periode3, true, 0, BigDecimal.valueOf(100), virksomhet);
        buildBeregningsresultatAndel(brR0Periode3, true, 0, BigDecimal.valueOf(100), virksomhet2);
        var andelB2P3Org1 = buildBeregningsresultatAndel(brR0Periode3, false, 789, BigDecimal.valueOf(100), virksomhet);
        buildBeregningsresultatFeriepengerPrÅr(feriepenger1, andelB2P3Org1, 1288L, opptjeningsårFeriepenger.plusYears(1));
        var andelB2P3Org2 = buildBeregningsresultatAndel(brR0Periode3, false, 154, BigDecimal.valueOf(100), virksomhet2);
        buildBeregningsresultatFeriepengerPrÅr(feriepenger1, andelB2P3Org2, 251L, opptjeningsårFeriepenger.plusYears(1));

        BeregningsresultatPeriode brR0Periode4 = buildBeregningsresultatPeriode(beregningsresultat1, b40fom, b40tom);
        buildBeregningsresultatAndel(brR0Periode4, true, 0, BigDecimal.valueOf(100), virksomhet);
        buildBeregningsresultatAndel(brR0Periode4, true, 0, BigDecimal.valueOf(100), virksomhet2);
        buildBeregningsresultatAndel(brR0Periode4, false, 789, BigDecimal.valueOf(100), virksomhet);
        buildBeregningsresultatAndel(brR0Periode4, false, 154, BigDecimal.valueOf(100), virksomhet2);

        GruppertYtelse gruppertYtelse2 = mapper.fordelPåNøkler(beregningsresultat1);
        var builder2 = getInputStandardBuilder(gruppertYtelse2).medTidligereOppdrag(mapTidligereOppdrag(List.of(originaltOppdrag)));

        // Act
        Oppdragskontroll oppdragRevurdering = OppdragMedPositivKvitteringTestUtil.opprett(nyOppdragskontrollTjeneste, builder2.build());

        // To arbeidsgivere som mottakere ingen oppdrag til bruker
        assertThat(oppdragRevurdering.getOppdrag110Liste().size()).isEqualTo(2);
        assertThat(oppdragRevurdering.getOppdrag110Liste().stream().allMatch(oppdrag110 -> oppdrag110.getKodeFagomrade().equals(KodeFagområde.FORELDREPENGER_ARBEIDSGIVER))).isTrue();


        // Arrange 2 - andre revurdering med omfordeling av 1 ag til bruker

        BeregningsresultatEntitet beregningsresultat2 = BeregningsresultatEntitet.builder()
            .medRegelInput("clob1")
            .medRegelSporing("clob2")
            .medEndringsdato(b20fom)
            .build();

        BeregningsresultatFeriepenger feriepenger2 = buildBeregningsresultatFeriepenger(beregningsresultat2);

        BeregningsresultatPeriode brRPeriode1 = buildBeregningsresultatPeriode(beregningsresultat2, b10fom, b10tom);
        buildBeregningsresultatAndel(brRPeriode1, true, 0, BigDecimal.valueOf(100), virksomhet);
        buildBeregningsresultatAndel(brRPeriode1, true, 0, BigDecimal.valueOf(100), virksomhet2);
        var andelB3P1Org1 = buildBeregningsresultatAndel(brRPeriode1, false, 789, BigDecimal.valueOf(100), virksomhet);
        buildBeregningsresultatFeriepengerPrÅr(feriepenger2, andelB3P1Org1, 1127L, opptjeningsårFeriepenger);
        var andelB3P1Org2 = buildBeregningsresultatAndel(brRPeriode1, false, 154, BigDecimal.valueOf(100), virksomhet2);
        buildBeregningsresultatFeriepengerPrÅr(feriepenger2, andelB3P1Org2, 220L, opptjeningsårFeriepenger);

        BeregningsresultatPeriode brRPeriode2a = buildBeregningsresultatPeriode(beregningsresultat2, b20fom, b20tom);
        buildBeregningsresultatAndel(brRPeriode2a, true, 0, BigDecimal.valueOf(100), virksomhet);
        buildBeregningsresultatAndel(brRPeriode2a, true, 0, BigDecimal.valueOf(100), virksomhet2);
        var andelB3P2Org1 = buildBeregningsresultatAndel(brRPeriode2a, false, 789, BigDecimal.valueOf(100), virksomhet);
        buildBeregningsresultatFeriepengerPrÅr(feriepenger2, andelB3P2Org1, 563L, opptjeningsårFeriepenger);
        var andelB3P2Org2 = buildBeregningsresultatAndel(brRPeriode2a, false, 154, BigDecimal.valueOf(100), virksomhet2);
        buildBeregningsresultatFeriepengerPrÅr(feriepenger2, andelB3P2Org2, 110L, opptjeningsårFeriepenger);

        BeregningsresultatPeriode brRPeriode2b = buildBeregningsresultatPeriode(beregningsresultat2, b21fom, b21tom);
        var andelB3P3Org2 = buildBeregningsresultatAndel(brRPeriode2b, true, 154, BigDecimal.valueOf(100), virksomhet2);
        buildBeregningsresultatFeriepengerPrÅr(feriepenger2, andelB3P3Org2, 361L, opptjeningsårFeriepenger);
        buildBeregningsresultatAndel(brRPeriode2b, true, 0, BigDecimal.valueOf(100), virksomhet);
        var andelB3P3Org1 = buildBeregningsresultatAndel(brRPeriode2b, false, 789, BigDecimal.valueOf(100), virksomhet);
        buildBeregningsresultatFeriepengerPrÅr(feriepenger2, andelB3P3Org1, 1851L, opptjeningsårFeriepenger);

        BeregningsresultatPeriode brRPeriode3 = buildBeregningsresultatPeriode(beregningsresultat2, b30fom, b30tom);
        var andelB3P4Org2 = buildBeregningsresultatAndel(brRPeriode3, true, 154, BigDecimal.valueOf(100), virksomhet2);
        buildBeregningsresultatFeriepengerPrÅr(feriepenger2, andelB3P4Org2, 251L, opptjeningsårFeriepenger.plusYears(1));
        buildBeregningsresultatAndel(brRPeriode3, true, 0, BigDecimal.valueOf(100), virksomhet);
        BeregningsresultatAndel andelB3P4Org1 = buildBeregningsresultatAndel(brRPeriode3, false, 789, BigDecimal.valueOf(100), virksomhet);
        buildBeregningsresultatFeriepengerPrÅr(feriepenger2, andelB3P4Org1, 1288L, opptjeningsårFeriepenger.plusYears(1));

        BeregningsresultatPeriode brRPeriode4 = buildBeregningsresultatPeriode(beregningsresultat2, b40fom, b40tom);
        buildBeregningsresultatAndel(brRPeriode4, true, 154, BigDecimal.valueOf(100), virksomhet2);
        buildBeregningsresultatAndel(brRPeriode4, true, 0, BigDecimal.valueOf(100), virksomhet);
        buildBeregningsresultatAndel(brRPeriode4, false, 789, BigDecimal.valueOf(100), virksomhet);

        GruppertYtelse gruppertYtelse3 = mapper.fordelPåNøkler(beregningsresultat2);
        var builder3 = getInputStandardBuilder(gruppertYtelse3).medTidligereOppdrag(mapTidligereOppdrag(List.of(originaltOppdrag, oppdragRevurdering)));

        // ActoppdragRevurdering = {Oppdragskontroll@3699} "Oppdragskontroll<behandlingId=123456, saksnummer=Saksnummer<101000>, venterKvittering=true, prosessTaskId=23, opprettetTs=null>"
        Oppdragskontroll oppdragRevurdering2 = OppdragMedPositivKvitteringTestUtil.opprett(nyOppdragskontrollTjeneste, builder3.build());

        // To arbeidsgivere som mottakere og bruker
        assertThat(oppdragRevurdering2.getOppdrag110Liste().size()).isEqualTo(2);

        //Assert -- opphør av bruker
        List<Oppdragslinje150> opp150RevurderingListe = oppdragRevurdering2.getOppdrag110Liste().stream()
            .flatMap(oppdrag110 -> oppdrag110.getOppdragslinje150Liste().stream())
            .collect(Collectors.toList());

        assertThat(opp150RevurderingListe).hasSize(8); // Bruker + FP
        assertThat(opp150RevurderingListe).anySatisfy(linje -> assertThat(linje.gjelderOpphør()).isTrue());


        // Arrange 3 - tredje revurdering med omfordeling av andre ag til bruker
        BeregningsresultatEntitet beregningsresultat3 = BeregningsresultatEntitet.builder()
            .medRegelInput("clob1")
            .medRegelSporing("clob2")
            .medEndringsdato(b21fom)
            .build();

        BeregningsresultatFeriepenger feriepenger3 = buildBeregningsresultatFeriepenger(beregningsresultat3);

        BeregningsresultatPeriode brR2Periode1 = buildBeregningsresultatPeriode(beregningsresultat3, b10fom, b10tom);
        buildBeregningsresultatAndel(brR2Periode1, true, 0, BigDecimal.valueOf(100), virksomhet);
        buildBeregningsresultatAndel(brR2Periode1, true, 0, BigDecimal.valueOf(100), virksomhet2);
        var andelB4P1Org1 = buildBeregningsresultatAndel(brR2Periode1, false, 789, BigDecimal.valueOf(100), virksomhet);
        buildBeregningsresultatFeriepengerPrÅr(feriepenger3, andelB4P1Org1, 1127L, opptjeningsårFeriepenger);
        var andelB4P1Org2 = buildBeregningsresultatAndel(brR2Periode1, false, 154, BigDecimal.valueOf(100), virksomhet2);
        buildBeregningsresultatFeriepengerPrÅr(feriepenger3, andelB4P1Org2, 220L, opptjeningsårFeriepenger);

        BeregningsresultatPeriode brR2Periode2a = buildBeregningsresultatPeriode(beregningsresultat3, b20fom, b20tom);
        buildBeregningsresultatAndel(brR2Periode2a, true, 0, BigDecimal.valueOf(100), virksomhet);
        buildBeregningsresultatAndel(brR2Periode2a, true, 0, BigDecimal.valueOf(100), virksomhet2);
        var andelB4P2Org1 = buildBeregningsresultatAndel(brR2Periode2a, false, 789, BigDecimal.valueOf(100), virksomhet);
        buildBeregningsresultatFeriepengerPrÅr(feriepenger3, andelB4P2Org1, 563L, opptjeningsårFeriepenger);
        var andelB4P2Org2 = buildBeregningsresultatAndel(brR2Periode2a, false, 154, BigDecimal.valueOf(100), virksomhet2);
        buildBeregningsresultatFeriepengerPrÅr(feriepenger3, andelB4P2Org2, 110L, opptjeningsårFeriepenger);

        BeregningsresultatPeriode brR2Periode2b = buildBeregningsresultatPeriode(beregningsresultat3, b21fom, b21tom);
        var andelB4P3Org1 = buildBeregningsresultatAndel(brR2Periode2b, true, 789, BigDecimal.valueOf(100), virksomhet);
        buildBeregningsresultatFeriepengerPrÅr(feriepenger3, andelB4P3Org1, 1851L, opptjeningsårFeriepenger);
        var andelB4P3Org2 = buildBeregningsresultatAndel(brR2Periode2b, true, 154, BigDecimal.valueOf(100), virksomhet2);
        buildBeregningsresultatFeriepengerPrÅr(feriepenger3, andelB4P3Org2, 361L, opptjeningsårFeriepenger);

        BeregningsresultatPeriode brR2Periode3 = buildBeregningsresultatPeriode(beregningsresultat3, b30fom, b30tom);
        var andelB4P4Org1 = buildBeregningsresultatAndel(brR2Periode3, true, 789, BigDecimal.valueOf(100), virksomhet);
        buildBeregningsresultatFeriepengerPrÅr(feriepenger3, andelB4P4Org1, 1288L, opptjeningsårFeriepenger.plusYears(1));
        var andelB4P4Org2 = buildBeregningsresultatAndel(brR2Periode3, true, 154, BigDecimal.valueOf(100), virksomhet2);
        buildBeregningsresultatFeriepengerPrÅr(feriepenger3, andelB4P4Org2, 251L, opptjeningsårFeriepenger.plusYears(1));

        BeregningsresultatPeriode brR2Periode4 = buildBeregningsresultatPeriode(beregningsresultat3, b40fom, b40tom);
        buildBeregningsresultatAndel(brR2Periode4, true, 789, BigDecimal.valueOf(100), virksomhet);
        buildBeregningsresultatAndel(brR2Periode4, true, 154, BigDecimal.valueOf(100), virksomhet2);

        GruppertYtelse gruppertYtelse4 = mapper.fordelPåNøkler(beregningsresultat3);
        var builder4 = getInputStandardBuilder(gruppertYtelse4).medTidligereOppdrag(mapTidligereOppdrag(List.of(originaltOppdrag, oppdragRevurdering, oppdragRevurdering2)));

        // Act
        Oppdragskontroll oppdragRevurdering3 = OppdragMedPositivKvitteringTestUtil.opprett(nyOppdragskontrollTjeneste, builder4.build());

        // Assert 3 -- opphør AG og endring for bruker
        assertThat(oppdragRevurdering3.getOppdrag110Liste().size()).isEqualTo(2);
    }


    /**
     * Prodscenario der bruker suksessivt mister ytelse. Til man til slutt står uten og det skal sendes opphørsoppdrag
     */
    @Test
    public void skalSendeOppdragMedOpphørNårAllInnvilgetYtelseBortfallerBrukerErOpphørtTidligere() {
        // Arrange
        LocalDate bminfom = LocalDate.of(I_ÅR, 7, 13);
        LocalDate bmaxtom = LocalDate.of(I_ÅR, 10, 23);
        LocalDate bmax2tom = LocalDate.of(I_ÅR, 12, 4);

        BeregningsresultatEntitet beregningsresultat = buildBeregningsresultatBrukerFP(null, List.of(2116),  List.of(0), bminfom, bmaxtom);

        TilkjentYtelseMapper mapper = new TilkjentYtelseMapper(FamilieYtelseType.FØDSEL);
        GruppertYtelse gruppertYtelse = mapper.fordelPåNøkler(beregningsresultat);
        var builder = getInputStandardBuilder(gruppertYtelse);
        Oppdragskontroll originaltOppdrag = OppdragMedPositivKvitteringTestUtil.opprett(nyOppdragskontrollTjeneste, builder.build());

        BeregningsresultatEntitet beregningsresultatRevurderingFP = buildBeregningsresultatBrukerFP(bminfom, List.of(0), List.of(2143), bminfom, bmaxtom);
        GruppertYtelse gruppertYtelse2 = mapper.fordelPåNøkler(beregningsresultatRevurderingFP);
        var builder2 = getInputStandardBuilder(gruppertYtelse2).medTidligereOppdrag(mapTidligereOppdrag(List.of(originaltOppdrag)));

        // Act
        Oppdragskontroll oppdragRevurdering = OppdragMedPositivKvitteringTestUtil.opprett(nyOppdragskontrollTjeneste, builder2.build());

        //Assert -- opphør av bruker
        List<Oppdragslinje150> opp150RevurderingListe = oppdragRevurdering.getOppdrag110Liste().stream()
            .flatMap(oppdrag110 -> oppdrag110.getOppdragslinje150Liste().stream())
            .filter(l -> l.getUtbetalesTilId() != null)
            .collect(Collectors.toList());

        assertThat(opp150RevurderingListe).hasSize(2); // Bruker + FP
        assertThat(opp150RevurderingListe).allSatisfy(linje -> assertThat(linje.gjelderOpphør()).isTrue());

        // Arrange 2 -- opphør deler av AG
        LocalDate b21fom = LocalDate.of(I_ÅR, 8, 24);
        LocalDate b20tom = LocalDate.of(I_ÅR, 8, 23);

        BeregningsresultatEntitet beregningsresultatRevurderingFP2 = buildBeregningsresultatBrukerFP(bminfom, List.of(0, 0), List.of(0, 2143),
            bminfom, b20tom, b21fom, bmax2tom);

        GruppertYtelse gruppertYtelse3 = mapper.fordelPåNøkler(beregningsresultatRevurderingFP2);
        var builder3 = getInputStandardBuilder(gruppertYtelse3).medTidligereOppdrag(mapTidligereOppdrag(List.of(originaltOppdrag, oppdragRevurdering)));

        // Act
        Oppdragskontroll oppdragRevurdering2 = OppdragMedPositivKvitteringTestUtil.opprett(nyOppdragskontrollTjeneste, builder3.build());

        // Arrange 3 -- opphør enda mer AG
        LocalDate b30tom = LocalDate.of(I_ÅR, 8, 23);
        LocalDate b31fom = LocalDate.of(I_ÅR, 8, 24);
        LocalDate b31tom = LocalDate.of(I_ÅR, 8, 31);
        LocalDate b32fom = LocalDate.of(I_ÅR, 9, 1);
        LocalDate b32tom = LocalDate.of(I_ÅR, 10, 18);
        LocalDate b33fom = LocalDate.of(I_ÅR, 10, 19);

        BeregningsresultatEntitet beregningsresultatRevurderingFP3 = buildBeregningsresultatBrukerFP(b31fom, List.of(0,0,0,0), List.of(0,0,2143,0),
            bminfom, b30tom, b31fom, b31tom, b32fom, b32tom, b33fom, bmax2tom);

        GruppertYtelse gruppertYtelse4 = mapper.fordelPåNøkler(beregningsresultatRevurderingFP3);
        var builder4 = getInputStandardBuilder(gruppertYtelse4).medTidligereOppdrag(mapTidligereOppdrag(List.of(originaltOppdrag, oppdragRevurdering, oppdragRevurdering2)));

        // Act
        Oppdragskontroll oppdragRevurdering3 = OppdragMedPositivKvitteringTestUtil.opprett(nyOppdragskontrollTjeneste, builder4.build());


        // Arrange 4 -- opphør enda mer AG
        LocalDate b41fom = LocalDate.of(I_ÅR, 9, 1);
        LocalDate b41tom = LocalDate.of(I_ÅR, 10, 2);
        LocalDate b42fom = LocalDate.of(I_ÅR, 10, 3);
        LocalDate b42tom = LocalDate.of(I_ÅR, 10, 18);
        LocalDate b43fom = LocalDate.of(I_ÅR, 10, 19);

        BeregningsresultatEntitet beregningsresultatRevurderingFP4 = buildBeregningsresultatBrukerFP(b41fom, List.of(0,0,0), List.of(0,2143,0),
            b41fom, b41tom, b42fom, b42tom, b43fom, bmax2tom);
        GruppertYtelse gruppertYtelse5 = mapper.fordelPåNøkler(beregningsresultatRevurderingFP4);
        var builder5 = getInputStandardBuilder(gruppertYtelse5).medTidligereOppdrag(mapTidligereOppdrag(List.of(originaltOppdrag, oppdragRevurdering, oppdragRevurdering2, oppdragRevurdering3)));

        // Act
        Oppdragskontroll oppdragRevurdering4 = OppdragMedPositivKvitteringTestUtil.opprett(nyOppdragskontrollTjeneste, builder5.build());


        // Arrange 5 -- opphør resten av AG
        LocalDate b51fom = LocalDate.of(I_ÅR, 10, 3);
        LocalDate b51tom = LocalDate.of(I_ÅR, 10, 18);
        LocalDate b52fom = LocalDate.of(I_ÅR, 10, 19);
        LocalDate b52tom = LocalDate.of(I_ÅR, 10, 23);
        LocalDate b53fom = LocalDate.of(I_ÅR, 10, 24);

        BeregningsresultatEntitet beregningsresultatRevurderingFP5 = buildBeregningsresultatBrukerFP(b51fom, List.of(0,0,0), List.of(0,0,0),
            b51fom, b51tom, b52fom, b52tom, b53fom, bmax2tom);
        GruppertYtelse gruppertYtelse6 = mapper.fordelPåNøkler(beregningsresultatRevurderingFP5);
        var builder6 = getInputStandardBuilder(gruppertYtelse6).medTidligereOppdrag(mapTidligereOppdrag(List.of(originaltOppdrag, oppdragRevurdering, oppdragRevurdering2, oppdragRevurdering3, oppdragRevurdering4)));

        // Act
        Oppdragskontroll oppdragRevurdering5 = OppdragMedPositivKvitteringTestUtil.opprett(nyOppdragskontrollTjeneste, builder6.build());

        //Assert 5 -- alt opphøres
        List<Oppdragslinje150> opp150RevurderingListe5 = oppdragRevurdering5.getOppdrag110Liste().stream()
            .flatMap(oppdrag110 -> oppdrag110.getOppdragslinje150Liste().stream())
            .collect(Collectors.toList());

        assertThat(opp150RevurderingListe5).hasSize(2); // AG +  FP
        assertThat(opp150RevurderingListe5).allSatisfy(l -> assertThat(l.gjelderOpphør()).isTrue());

    }

    /**
     * Prodscenario med omfordeling fra delvis ref til kun direkte utbetaling og så opphør
     */
    @Test
    public void skalSendeOppdragMedOpphørNårAllInnvilgetYtelseBortfallerArbeidsgiverErOpphørtTidligere() {
        // Arrange
        LocalDate bminfom = LocalDate.of(I_ÅR, 3, 23);
        LocalDate bmaxtom = LocalDate.of(I_ÅR, 7, 3);

        BeregningsresultatEntitet beregningsresultat = buildBeregningsresultatBrukerFP(null, List.of(897),  List.of(1265), bminfom, bmaxtom);
        TilkjentYtelseMapper mapper = new TilkjentYtelseMapper(FamilieYtelseType.FØDSEL);
        GruppertYtelse gruppertYtelse = mapper.fordelPåNøkler(beregningsresultat);
        var builder = getInputStandardBuilder(gruppertYtelse);
        Oppdragskontroll originaltOppdrag = OppdragMedPositivKvitteringTestUtil.opprett(nyOppdragskontrollTjeneste, builder.build());

        BeregningsresultatEntitet beregningsresultatRevurderingFP = buildBeregningsresultatBrukerFP(bminfom, List.of(2162), List.of(0), bminfom, bmaxtom);

        GruppertYtelse gruppertYtelse2 = mapper.fordelPåNøkler(beregningsresultatRevurderingFP);
        var builder2 = getInputStandardBuilder(gruppertYtelse2).medTidligereOppdrag(mapTidligereOppdrag(List.of(originaltOppdrag)));

        // Act
        Oppdragskontroll oppdragRevurdering = OppdragMedPositivKvitteringTestUtil.opprett(nyOppdragskontrollTjeneste, builder2.build());

        //Assert -- opphør av ag
        List<Oppdragslinje150> opp150RevurderingListe = oppdragRevurdering.getOppdrag110Liste().stream()
            .flatMap(oppdrag110 -> oppdrag110.getOppdragslinje150Liste().stream())
            .filter(l -> l.getUtbetalesTilId() == null)
            .collect(Collectors.toList());

        assertThat(opp150RevurderingListe).hasSize(2); // AG + FP
        assertThat(opp150RevurderingListe).allSatisfy(linje -> assertThat(linje.gjelderOpphør()).isTrue());

        // Arrange 2 -- opphør deler av AG

        BeregningsresultatEntitet beregningsresultatRevurderingFP3 = buildBeregningsresultatBrukerFP(bminfom, List.of(0), List.of(0), bminfom, bmaxtom);
        GruppertYtelse gruppertYtelse3 = mapper.fordelPåNøkler(beregningsresultatRevurderingFP3);
        var builder3 = getInputStandardBuilder(gruppertYtelse3).medTidligereOppdrag(mapTidligereOppdrag(List.of(originaltOppdrag, oppdragRevurdering)));

        // Act
        Oppdragskontroll oppdragRevurdering2 = OppdragMedPositivKvitteringTestUtil.opprett(nyOppdragskontrollTjeneste, builder3.build());


        //Assert 2 -- alt opphøres
        List<Oppdragslinje150> opp150RevurderingListe5 = oppdragRevurdering2.getOppdrag110Liste().stream()
            .flatMap(oppdrag110 -> oppdrag110.getOppdragslinje150Liste().stream())
            .filter(l -> l.getUtbetalesTilId() != null)
            .collect(Collectors.toList());

        assertThat(opp150RevurderingListe5).hasSize(2); // AG +  FP
        assertThat(opp150RevurderingListe5).allSatisfy(l -> assertThat(l.gjelderOpphør()).isTrue());

    }


    private List<Oppdragslinje150> sortOppdragslinj150Liste(List<Oppdragslinje150> opp150OpphForDPListe) {
        return opp150OpphForDPListe.stream().sorted(Comparator.comparing(Oppdragslinje150::getDelytelseId)).collect(Collectors.toList());
    }

    private void verifiserOpp150NårDetErFlereArbeidsgivereSomMottaker(LocalDate endringsdato, List<Oppdragslinje150> revurderingOpp150Arbgvr) {
        assertThat(revurderingOpp150Arbgvr).hasSize(5);
        Oppdragslinje150 opp150ForOpph = revurderingOpp150Arbgvr.stream()
            .filter(Oppdragslinje150::gjelderOpphør)
            .findFirst().get();
        List<Oppdragslinje150> opp150FomEndringsdato = revurderingOpp150Arbgvr.stream()
            .filter(opp150 -> !opp150.gjelderOpphør())
            .collect(Collectors.toList());
        assertThat(opp150ForOpph.getDatoStatusFom()).isEqualTo(endringsdato);
        assertThat(opp150ForOpph.getKodeKlassifik()).isEqualTo(KodeKlassifik.FPF_REFUSJON_AG);
        assertThat(opp150ForOpph.getRefusjonsinfo156().getRefunderesId()).isIn(List.of(endreTilElleveSiffer(virksomhet), endreTilElleveSiffer(virksomhet2)));
        assertThat(opp150FomEndringsdato).hasSize(4);
        List<Long> delytelseIdList = opp150FomEndringsdato.stream().map(Oppdragslinje150::getDelytelseId).collect(Collectors.toList());
        delytelseIdList.add(opp150ForOpph.getDelytelseId());
        Set<Long> delytelseIdSet = Sets.newHashSet(delytelseIdList);
        assertThat(delytelseIdList).hasSize(delytelseIdSet.size());
    }

    private void verifiserKjeding(List<Oppdragslinje150> originaltOpp150Liste, List<Oppdragslinje150> opp150RevurdListe) {
        List<Oppdragslinje150> opp150ForSNOriginalListe = getOpp150MedKodeklassifik(originaltOpp150Liste, KodeKlassifik.FPF_SELVSTENDIG);
        List<Oppdragslinje150> opp150ForSNRevurdListe = getOpp150MedKodeklassifik(opp150RevurdListe, KodeKlassifik.FPF_SELVSTENDIG);
        List<Oppdragslinje150> opp150ForFLRevurdListe = getOpp150MedKodeklassifik(opp150RevurdListe, KodeKlassifik.FPF_FRILANSER);

        assertThat(opp150ForFLRevurdListe).hasSize(0);
        assertThat(opp150ForSNRevurdListe).hasSize(1);
        Oppdragslinje150 opp150SN = opp150ForSNRevurdListe.get(0);
        assertThat(opp150SN.gjelderOpphør()).isTrue();
        assertThat(opp150ForSNOriginalListe).anySatisfy(opp150 ->
            assertThat(opp150.getDelytelseId()).isEqualTo(opp150SN.getDelytelseId()));
    }

    private List<Oppdragslinje150> getOpp150MedKodeklassifik(List<Oppdragslinje150> opp150RevurdListe, KodeKlassifik kodeKlassifik) {
        return opp150RevurdListe.stream()
            .filter(opp150 -> kodeKlassifik.equals(opp150.getKodeKlassifik()))
            .collect(Collectors.toList());
    }

    private void verifiserKodeklassifik(List<Oppdragslinje150> originaltOpp150Liste, List<Oppdragslinje150> opp150RevurdListe) {
        List<KodeKlassifik> kodeKlassifikForrigeListe = OppdragskontrollTestVerktøy.getKodeklassifikIOppdr150Liste(originaltOpp150Liste);
        List<KodeKlassifik> kodeKlassifikRevurderingListe = OppdragskontrollTestVerktøy.getKodeklassifikIOppdr150Liste(opp150RevurdListe);
        List<KodeKlassifik> kodeKlassifikRevurderingOpphListe = OppdragskontrollTestVerktøy.getKodeklassifikKunForOpp150MedOpph(opp150RevurdListe);
        assertThat(kodeKlassifikForrigeListe).containsAnyElementsOf(List.of(KodeKlassifik.FPF_FRILANSER, KodeKlassifik.FPF_ARBEIDSTAKER));
        assertThat(kodeKlassifikRevurderingListe).containsAnyElementsOf(List.of(KodeKlassifik.FPF_FRILANSER, KodeKlassifik.FPF_ARBEIDSTAKER));
        assertThat(kodeKlassifikRevurderingOpphListe).containsAnyElementsOf(List.of(KodeKlassifik.FPF_FRILANSER, KodeKlassifik.FPF_ARBEIDSTAKER));
    }

    private void verifiserKodeklassifikNårRevurderingHarNye(List<Oppdragslinje150> originaltOpp150Liste, List<Oppdragslinje150> opp150RevurdListe) {
        List<KodeKlassifik> kodeKlassifikForrigeListe = OppdragskontrollTestVerktøy.getKodeklassifikIOppdr150Liste(originaltOpp150Liste);
        List<KodeKlassifik> kodeKlassifikRevurderingListe = OppdragskontrollTestVerktøy.getKodeklassifikIOppdr150Liste(opp150RevurdListe);
        List<KodeKlassifik> kodeKlassifikRevurderingOpphListe = OppdragskontrollTestVerktøy.getKodeklassifikKunForOpp150MedOpph(opp150RevurdListe);
        assertThat(kodeKlassifikForrigeListe).containsAnyElementsOf(List.of(KodeKlassifik.FPF_FRILANSER, KodeKlassifik.FPF_SELVSTENDIG));
        assertThat(kodeKlassifikRevurderingListe).containsAnyElementsOf(List.of(KodeKlassifik.FPF_ARBEIDSTAKER, KodeKlassifik.FPF_SELVSTENDIG));
        assertThat(kodeKlassifikRevurderingOpphListe).containsExactly(KodeKlassifik.FPF_SELVSTENDIG);
    }


    private void verifiserOppdragslinje150_ENDR(Oppdragskontroll oppdragskontroll, List<Oppdragslinje150> originaltOpp150Liste, boolean medFeriepenger,
                                                boolean medFlereKlassekode, int gradering) {
        List<Oppdragslinje150> opp150RevurdListe = OppdragskontrollTestVerktøy.getOppdragslinje150Liste(oppdragskontroll);

        verifiserOppdr150SomErOpphørt(opp150RevurdListe, originaltOpp150Liste, medFeriepenger, medFlereKlassekode, false);
        verifiserOppdr150SomErNy(opp150RevurdListe, originaltOpp150Liste, List.of(gradering));
    }

    private void verifiserOppdr150SomErUendret(Oppdragskontroll oppdrag) {
        List<Oppdragslinje150> opp150RevurdListe = OppdragskontrollTestVerktøy.getOppdragslinje150Liste(oppdrag);
        List<Oppdragslinje150> opp150VirksomhetListe = opp150RevurdListe.stream()
            .filter(oppdragslinje150 -> oppdragslinje150.getRefusjonsinfo156() != null)
            .filter(oppdragslinje150 -> oppdragslinje150.getRefusjonsinfo156().getRefunderesId().equals(OppdragskontrollTestVerktøy.endreTilElleveSiffer(virksomhet)))
            .filter(oppdragslinje150 -> oppdragslinje150.getKodeKlassifik().equals(KodeKlassifik.FPF_FERIEPENGER_AG))
            .collect(Collectors.toList());
        assertThat(opp150VirksomhetListe).isEmpty();
    }
}
