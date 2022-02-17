package no.nav.foreldrepenger.behandling.revurdering.felles;

import static no.nav.foreldrepenger.behandlingslager.virksomhet.OrgNummer.KUNSTIG_ORG;
import static org.assertj.core.api.Assertions.assertThat;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import javax.inject.Inject;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import no.nav.foreldrepenger.behandling.revurdering.BeregningRevurderingTestUtil;
import no.nav.foreldrepenger.behandling.revurdering.RevurderingEndring;
import no.nav.foreldrepenger.behandling.revurdering.RevurderingTjenesteFelles;
import no.nav.foreldrepenger.behandling.revurdering.ytelse.fp.RevurderingTjenesteImpl;
import no.nav.foreldrepenger.behandlingskontroll.FagsakYtelseTypeRef;
import no.nav.foreldrepenger.behandlingskontroll.impl.BehandlingskontrollTjenesteImpl;
import no.nav.foreldrepenger.behandlingskontroll.spi.BehandlingskontrollServiceProvider;
import no.nav.foreldrepenger.behandlingslager.aktør.OrganisasjonsEnhet;
import no.nav.foreldrepenger.behandlingslager.behandling.Behandling;
import no.nav.foreldrepenger.behandlingslager.behandling.BehandlingResultatType;
import no.nav.foreldrepenger.behandlingslager.behandling.BehandlingStegType;
import no.nav.foreldrepenger.behandlingslager.behandling.Behandlingsresultat;
import no.nav.foreldrepenger.behandlingslager.behandling.BehandlingÅrsakType;
import no.nav.foreldrepenger.behandlingslager.behandling.KonsekvensForYtelsen;
import no.nav.foreldrepenger.behandlingslager.behandling.RettenTil;
import no.nav.foreldrepenger.behandlingslager.behandling.aksjonspunkt.AksjonspunktDefinisjon;
import no.nav.foreldrepenger.behandlingslager.behandling.repository.BehandlingGrunnlagRepositoryProvider;
import no.nav.foreldrepenger.behandlingslager.behandling.repository.BehandlingRepositoryProvider;
import no.nav.foreldrepenger.behandlingslager.behandling.vedtak.VedtakResultatType;
import no.nav.foreldrepenger.behandlingslager.behandling.vedtak.Vedtaksbrev;
import no.nav.foreldrepenger.behandlingslager.behandling.verge.VergeRepository;
import no.nav.foreldrepenger.behandlingslager.testutilities.behandling.ScenarioMorSøkerForeldrepenger;
import no.nav.foreldrepenger.behandlingslager.virksomhet.Arbeidsgiver;
import no.nav.foreldrepenger.dbstoette.CdiDbAwareTest;
import no.nav.foreldrepenger.domene.arbeidsforhold.InntektArbeidYtelseTjeneste;
import no.nav.foreldrepenger.domene.modell.AktivitetStatus;
import no.nav.foreldrepenger.domene.modell.BGAndelArbeidsforhold;
import no.nav.foreldrepenger.domene.modell.BeregningsgrunnlagAktivitetStatus;
import no.nav.foreldrepenger.domene.modell.BeregningsgrunnlagEntitet;
import no.nav.foreldrepenger.domene.modell.BeregningsgrunnlagPeriode;
import no.nav.foreldrepenger.domene.modell.BeregningsgrunnlagPrStatusOgAndel;
import no.nav.foreldrepenger.domene.tid.ÅpenDatoIntervallEntitet;
import no.nav.foreldrepenger.domene.typer.InternArbeidsforholdRef;
import no.nav.fpsak.tidsserie.LocalDateInterval;

@CdiDbAwareTest
public class ErKunEndringIFordelingAvYtelsenTest {
    public static final String ORGNR = KUNSTIG_ORG;
    private static final LocalDate SKJÆRINGSTIDSPUNKT_BEREGNING = LocalDate.now();
    private static final List<InternArbeidsforholdRef> ARBEIDSFORHOLDLISTE = List.of(
            InternArbeidsforholdRef.nyRef(), InternArbeidsforholdRef.nyRef(), InternArbeidsforholdRef.nyRef(),
            InternArbeidsforholdRef.nyRef());

    @Inject
    private BehandlingskontrollServiceProvider serviceProvider;

    @Inject
    private InntektArbeidYtelseTjeneste iayTjeneste;
    @Inject
    private BeregningRevurderingTestUtil revurderingTestUtil;
    @Inject
    @FagsakYtelseTypeRef("FP")
    private RevurderingEndring revurderingEndring;

    @Inject
    private VergeRepository vergeRepository;

    @Inject
    private BehandlingRepositoryProvider repositoryProvider;
    @Inject
    private BehandlingGrunnlagRepositoryProvider grunnlagRepositoryProvider;
    private Behandling behandlingSomSkalRevurderes;
    private Behandling revurdering;
    private Behandlingsresultat revurderingResultat;
    private BeregningsgrunnlagEntitet beregningsgrunnlag;

    @BeforeEach
    public void setUp() {
        var scenario = ScenarioMorSøkerForeldrepenger.forFødsel();
        scenario.leggTilAksjonspunkt(AksjonspunktDefinisjon.AVKLAR_TERMINBEKREFTELSE,
                BehandlingStegType.KONTROLLER_FAKTA);
        scenario.medBehandlingVedtak()
                .medVedtakstidspunkt(LocalDateTime.now())
                .medVedtakResultatType(VedtakResultatType.INNVILGET);
        behandlingSomSkalRevurderes = scenario.lagre(repositoryProvider);
        repositoryProvider.getOpptjeningRepository()
                .lagreOpptjeningsperiode(behandlingSomSkalRevurderes, LocalDate.now().minusYears(1), LocalDate.now(),
                        false);
        revurderingTestUtil.avsluttBehandling(behandlingSomSkalRevurderes);
        var behandlingskontrollTjeneste = new BehandlingskontrollTjenesteImpl(serviceProvider);
        var revurderingTjenesteFelles = new RevurderingTjenesteFelles(repositoryProvider);
        var revurderingTjeneste = new RevurderingTjenesteImpl(repositoryProvider, grunnlagRepositoryProvider,
            behandlingskontrollTjeneste, iayTjeneste, revurderingEndring, revurderingTjenesteFelles, vergeRepository);
        revurdering = revurderingTjeneste
                .opprettAutomatiskRevurdering(behandlingSomSkalRevurderes.getFagsak(),
                        BehandlingÅrsakType.RE_HENDELSE_FØDSEL, new OrganisasjonsEnhet("1234", "Test"));
        revurderingResultat = repositoryProvider.getBehandlingsresultatRepository()
                .hentHvisEksisterer(revurdering.getId())
                .orElse(null);
    }

    @Test
    public void skal_gi_endring_i_ytelse_ved_forskjellig_fordeling_av_andeler() {
        // Arrange
        var bgPeriode = Collections.singletonList(
                ÅpenDatoIntervallEntitet.fraOgMedTilOgMed(SKJÆRINGSTIDSPUNKT_BEREGNING, null));
        var originalGrunnlag = byggBeregningsgrunnlagForBehandling(false, false, bgPeriode);
        var revurderingGrunnlag = byggBeregningsgrunnlagForBehandling(false, true, bgPeriode);

        // Act
        var endring = ErKunEndringIFordelingAvYtelsen.vurder(false, false, Optional.of(revurderingGrunnlag),
                Optional.of(originalGrunnlag), false);

        // Assert
        assertThat(endring).isTrue();
    }

    @Test
    public void skal_gi_ingen_endring_i_ytelse_ved_lik_fordeling_av_andeler() {
        // Arrange
        var bgPeriode = Collections.singletonList(
                ÅpenDatoIntervallEntitet.fraOgMedTilOgMed(SKJÆRINGSTIDSPUNKT_BEREGNING, null));
        var originalGrunnlag = byggBeregningsgrunnlagForBehandling(false, false, bgPeriode);
        var revurderingGrunnlag = byggBeregningsgrunnlagForBehandling(false, false, bgPeriode);

        // Act
        var endring = ErKunEndringIFordelingAvYtelsen.vurder(false, false, Optional.of(revurderingGrunnlag),
                Optional.of(originalGrunnlag), false);

        // Assert
        assertThat(endring).isFalse();
    }

    @Test
    public void case_fra_prod_skal_oppdage_endring_fordeling() {
        // Arrange
        var dagsatserBR = List.of(2304, 0, 2304);
        var dagsatserAG = List.of(0, 2304, 0);
        var basis = LocalDate.of(2020, 4, 18);
        var tom1 = LocalDate.of(2020, 4, 22);
        var tom2 = LocalDate.of(2020, 6, 10);

        List<LocalDateInterval> originaleBGPerioder = new ArrayList<>();
        originaleBGPerioder.add(new LocalDateInterval(basis, null));

        List<LocalDateInterval> revurderingBGPerioder = new ArrayList<>();
        revurderingBGPerioder.add(new LocalDateInterval(basis, tom1));
        revurderingBGPerioder.add(new LocalDateInterval(tom1.plusDays(1), tom2));
        revurderingBGPerioder.add(new LocalDateInterval(tom2.plusDays(1), null));

        var originalGrunnlag = forProdCase(originaleBGPerioder, List.of(2304), List.of(0));
        var revurderingGrunnlag = forProdCase(revurderingBGPerioder, dagsatserBR, dagsatserAG);

        // Act
        var endring = ErKunEndringIFordelingAvYtelsen.vurder(false, false,
                Optional.of(revurderingGrunnlag), Optional.of(originalGrunnlag), false);

        // Assert
        assertThat(endring).isTrue();
    }

    private BeregningsgrunnlagEntitet forProdCase(List<LocalDateInterval> perioder,
            List<Integer> dagsatserBR,
            List<Integer> dagsatserAG) {
        var grunnlag = BeregningsgrunnlagEntitet.ny()
                .medSkjæringstidspunkt(SKJÆRINGSTIDSPUNKT_BEREGNING)
                .medGrunnbeløp(BigDecimal.valueOf(91425L))
                .build();
        byggBeregningsgrunnlagPeriodeOgAndeler(grunnlag, perioder, dagsatserBR, dagsatserAG);

        return grunnlag;
    }

    @Test
    public void skal_gi_ingen_endring_i_ytelse_ved_lik_fordeling_av_andeler_ved_ulike_perioder() {
        // Arrange
        var dagsatser = List.of(123, 5781, 5781);

        List<LocalDateInterval> originaleBGPerioder = new ArrayList<>();
        originaleBGPerioder.add(
                new LocalDateInterval(SKJÆRINGSTIDSPUNKT_BEREGNING, SKJÆRINGSTIDSPUNKT_BEREGNING.plusDays(10)));
        originaleBGPerioder.add(new LocalDateInterval(SKJÆRINGSTIDSPUNKT_BEREGNING.plusDays(11),
                SKJÆRINGSTIDSPUNKT_BEREGNING.plusDays(20)));
        originaleBGPerioder.add(new LocalDateInterval(SKJÆRINGSTIDSPUNKT_BEREGNING.plusDays(21), null));

        List<LocalDateInterval> revurderingBGPerioder = new ArrayList<>();
        revurderingBGPerioder.add(
                new LocalDateInterval(SKJÆRINGSTIDSPUNKT_BEREGNING, SKJÆRINGSTIDSPUNKT_BEREGNING.plusDays(10)));
        revurderingBGPerioder.add(new LocalDateInterval(SKJÆRINGSTIDSPUNKT_BEREGNING.plusDays(11),
                SKJÆRINGSTIDSPUNKT_BEREGNING.plusDays(15)));
        revurderingBGPerioder.add(new LocalDateInterval(SKJÆRINGSTIDSPUNKT_BEREGNING.plusDays(16), null));

        var originalGrunnlag = byggBeregningsgrunnlagForBehandling(false, originaleBGPerioder,
                dagsatser);
        var revurderingGrunnlag = byggBeregningsgrunnlagForBehandling(false,
                revurderingBGPerioder, dagsatser);

        // Act
        var endring = ErKunEndringIFordelingAvYtelsen.vurder(false, false,
                Optional.of(revurderingGrunnlag), Optional.of(originalGrunnlag), false);

        // Assert
        assertThat(endring).isFalse();
    }

    @Test
    public void skal_gi_endring_i_ytelse_ved_ulik_fordeling_av_andeler_ved_ulike_perioder() {
        // Arrange
        var dagsatser = List.of(123, 5781, 5781);
        var dagsatserRevurdering = List.of(123, 3183, 5781);

        List<LocalDateInterval> originaleBGPerioder = new ArrayList<>();
        originaleBGPerioder.add(
                new LocalDateInterval(SKJÆRINGSTIDSPUNKT_BEREGNING, SKJÆRINGSTIDSPUNKT_BEREGNING.plusDays(10)));
        originaleBGPerioder.add(new LocalDateInterval(SKJÆRINGSTIDSPUNKT_BEREGNING.plusDays(10),
                SKJÆRINGSTIDSPUNKT_BEREGNING.plusDays(20)));
        originaleBGPerioder.add(new LocalDateInterval(SKJÆRINGSTIDSPUNKT_BEREGNING.plusDays(20), null));

        List<LocalDateInterval> revurderingBGPerioder = new ArrayList<>();
        revurderingBGPerioder.add(
                new LocalDateInterval(SKJÆRINGSTIDSPUNKT_BEREGNING, SKJÆRINGSTIDSPUNKT_BEREGNING.plusDays(10)));
        revurderingBGPerioder.add(new LocalDateInterval(SKJÆRINGSTIDSPUNKT_BEREGNING.plusDays(10),
                SKJÆRINGSTIDSPUNKT_BEREGNING.plusDays(15)));
        revurderingBGPerioder.add(new LocalDateInterval(SKJÆRINGSTIDSPUNKT_BEREGNING.plusDays(15), null));

        var originalGrunnlag = byggBeregningsgrunnlagForBehandling(false, originaleBGPerioder,
                dagsatser);
        var revurderingGrunnlag = byggBeregningsgrunnlagForBehandling(false,
                revurderingBGPerioder, dagsatserRevurdering);

        // Act
        var endring = ErKunEndringIFordelingAvYtelsen.vurder(false, false,
                Optional.of(revurderingGrunnlag), Optional.of(originalGrunnlag), false);

        // Assert
        assertThat(endring).isTrue();
    }

    @Test
    public void skal_teste_fastsettelse_av_behandlingsresultatet_ved_varsel_om_revurdering_sendt() {
        // Act
        var behandlingsresultat = ErKunEndringIFordelingAvYtelsen.fastsett(revurdering,
                revurderingResultat, true);

        // Assert
        assertThat(behandlingsresultat.getVedtaksbrev()).isEqualTo(Vedtaksbrev.AUTOMATISK);
        assertThat(behandlingsresultat.getKonsekvenserForYtelsen()).hasSize(1);
        assertThat(behandlingsresultat.getKonsekvenserForYtelsen().get(0)).isEqualTo(
                KonsekvensForYtelsen.ENDRING_I_FORDELING_AV_YTELSEN);
        assertThat(behandlingsresultat.getRettenTil()).isEqualTo(RettenTil.HAR_RETT_TIL_FP);
        assertThat(behandlingsresultat.getBehandlingResultatType()).isEqualTo(
                BehandlingResultatType.FORELDREPENGER_ENDRET);
    }

    @Test
    public void skal_teste_fastsettelse_av_behandlingsresultatet_ved_varsel_om_revurdering_ikke_sendt() {
        // Act
        var behandlingsresultat = ErKunEndringIFordelingAvYtelsen.fastsett(revurdering,
                revurderingResultat, false);

        // Assert
        assertThat(behandlingsresultat.getVedtaksbrev()).isEqualTo(Vedtaksbrev.INGEN);
        assertThat(behandlingsresultat.getKonsekvenserForYtelsen()).hasSize(1);
        assertThat(behandlingsresultat.getKonsekvenserForYtelsen().get(0)).isEqualTo(
                KonsekvensForYtelsen.ENDRING_I_FORDELING_AV_YTELSEN);
        assertThat(behandlingsresultat.getRettenTil()).isEqualTo(RettenTil.HAR_RETT_TIL_FP);
        assertThat(behandlingsresultat.getBehandlingResultatType()).isEqualTo(
                BehandlingResultatType.FORELDREPENGER_ENDRET);
    }

    @Test
    public void skal_gi_ingen_endring_dersom_begge_grunnlag_mangler() {
        // Act
        var endringIBeregning = ErKunEndringIFordelingAvYtelsen.vurder(false, false,
                Optional.empty(), Optional.empty(), false);

        // Assert
        assertThat(endringIBeregning).isFalse();
    }

    @Test
    public void skal_gi_endring_dersom_et_grunnlag_mangler() {
        var dagsatserRevurdering = Collections.singletonList(123);
        List<LocalDateInterval> revurderingBGPerioder = new ArrayList<>();
        revurderingBGPerioder.add(
                new LocalDateInterval(SKJÆRINGSTIDSPUNKT_BEREGNING, SKJÆRINGSTIDSPUNKT_BEREGNING.plusDays(10)));
        var revurderingGrunnlag = byggBeregningsgrunnlagForBehandling(false,
                revurderingBGPerioder, dagsatserRevurdering);

        // Act
        var endringIBeregning = ErKunEndringIFordelingAvYtelsen.vurder(false, false,
                Optional.of(revurderingGrunnlag), Optional.empty(), false);

        // Assert
        assertThat(endringIBeregning).isTrue();
    }

    @Test
    public void skal_gi_ingen_endring_når_revurderingsgrunnlag_starter_før_originalgrunnlag() {
        var dagsatserRevurdering = Collections.singletonList(123);

        List<LocalDateInterval> originalePerioder = new ArrayList<>();
        originalePerioder.add(
                new LocalDateInterval(SKJÆRINGSTIDSPUNKT_BEREGNING.plusDays(1), SKJÆRINGSTIDSPUNKT_BEREGNING.plusDays(11)));
        var originaltGrunnlag = byggBeregningsgrunnlagForBehandling(false, originalePerioder,
                dagsatserRevurdering);

        List<LocalDateInterval> revurderingBGPerioder = new ArrayList<>();
        revurderingBGPerioder.add(
                new LocalDateInterval(SKJÆRINGSTIDSPUNKT_BEREGNING, SKJÆRINGSTIDSPUNKT_BEREGNING.plusDays(10)));
        var revurderingGrunnlag = byggBeregningsgrunnlagForBehandling(false,
                revurderingBGPerioder, dagsatserRevurdering);

        // Act
        var endringIBeregning = ErKunEndringIFordelingAvYtelsen.vurder(false, false,
                Optional.of(revurderingGrunnlag), Optional.of(originaltGrunnlag), false);

        // Assert
        assertThat(endringIBeregning).isFalse();
    }

    @Test
    public void skal_gi_endring_når_revurderingsgrunnlag_starter_før_originalgrunnlag_men_erEndringISkalHindreTilbakketrekk_true() {
        var dagsatserRevurdering = Collections.singletonList(123);

        List<LocalDateInterval> originalePerioder = new ArrayList<>();
        originalePerioder.add(
                new LocalDateInterval(SKJÆRINGSTIDSPUNKT_BEREGNING.plusDays(1), SKJÆRINGSTIDSPUNKT_BEREGNING.plusDays(11)));
        var originaltGrunnlag = byggBeregningsgrunnlagForBehandling(false, originalePerioder,
                dagsatserRevurdering);

        List<LocalDateInterval> revurderingBGPerioder = new ArrayList<>();
        revurderingBGPerioder.add(
                new LocalDateInterval(SKJÆRINGSTIDSPUNKT_BEREGNING, SKJÆRINGSTIDSPUNKT_BEREGNING.plusDays(10)));
        var revurderingGrunnlag = byggBeregningsgrunnlagForBehandling(false,
                revurderingBGPerioder, dagsatserRevurdering);

        // Act
        var endringIBeregning = ErKunEndringIFordelingAvYtelsen.vurder(false, false,
                Optional.of(revurderingGrunnlag), Optional.of(originaltGrunnlag), true);

        // Assert
        assertThat(endringIBeregning).isTrue();
    }

    private BeregningsgrunnlagEntitet byggBeregningsgrunnlagForBehandling(boolean skalDeleAndelMellomArbeidsgiverOgBruker,
            List<LocalDateInterval> perioder,
            List<Integer> dagsatser) {
        var dagsatsBruker = skalDeleAndelMellomArbeidsgiverOgBruker ? dagsatser.stream()
                .map(dagsats -> BigDecimal.valueOf(dagsats)
                        .divide(BigDecimal.valueOf(2), 0, RoundingMode.HALF_UP)
                        .intValue())
                .collect(Collectors.toList())
                : dagsatser;
        var dagsatsArbeidstaker = skalDeleAndelMellomArbeidsgiverOgBruker ? dagsatser.stream()
                .map(dagsats -> BigDecimal.valueOf(dagsats)
                        .divide(BigDecimal.valueOf(2), 0, RoundingMode.HALF_UP)
                        .intValue())
                .collect(Collectors.toList())
                : Collections.nCopies(dagsatser.size(), 0);

        var grunnlag = BeregningsgrunnlagEntitet.ny()
                .medSkjæringstidspunkt(SKJÆRINGSTIDSPUNKT_BEREGNING)
                .medGrunnbeløp(BigDecimal.valueOf(91425L))
                .build();
        byggBeregningsgrunnlagPeriodeOgAndeler(grunnlag, perioder, dagsatsBruker, dagsatsArbeidstaker);

        return grunnlag;
    }

    private void byggBeregningsgrunnlagPeriodeOgAndeler(BeregningsgrunnlagEntitet grunnlag,
            List<LocalDateInterval> perioder,
            List<Integer> dagsatserBruker,
            List<Integer> dagsatserArbeidstaker) {
        for (var i = 0; i < perioder.size(); i++) {
            var datoIntervall = perioder.get(i);
            var dagsatsBruker = dagsatserBruker.get(i);
            var dagsatsArbeidstaker = dagsatserArbeidstaker.get(i);
            var periode = BeregningsgrunnlagPeriode.ny()
                    .medBeregningsgrunnlagPeriode(datoIntervall.getFomDato(), datoIntervall.getTomDato())
                    .build(grunnlag);
            byggBeregningsgrunnlagAndel(periode, dagsatsBruker, dagsatsArbeidstaker);
            BeregningsgrunnlagPeriode.oppdater(periode)
                    .build(grunnlag);
        }
    }

    private void byggBeregningsgrunnlagAndel(BeregningsgrunnlagPeriode periode,
            Integer dagsatsBruker,
            Integer dagsatsArbeidsgiver) {
        var bga = BGAndelArbeidsforhold
                .builder()
                .medArbeidsgiver(Arbeidsgiver.virksomhet(ORGNR))
                .medArbeidsforholdRef(ARBEIDSFORHOLDLISTE.get(0))
                .medArbeidsperiodeFom(LocalDate.now().minusYears(1))
                .medArbeidsperiodeTom(LocalDate.now().plusYears(2));
        BeregningsgrunnlagPrStatusOgAndel.builder()
                .medBGAndelArbeidsforhold(bga)
                .medAktivitetStatus(AktivitetStatus.ARBEIDSTAKER)
                .medBeregnetPrÅr(BigDecimal.valueOf(240000))
                .medRedusertBrukersAndelPrÅr(BigDecimal.valueOf(dagsatsBruker * 260))
                .medRedusertRefusjonPrÅr(BigDecimal.valueOf(dagsatsArbeidsgiver * 260))
                .build(periode);
    }

    private BeregningsgrunnlagEntitet byggBeregningsgrunnlagForBehandling(boolean medOppjustertDagsat,
            boolean skalDeleAndelMellomArbeidsgiverOgBruker,
            List<ÅpenDatoIntervallEntitet> perioder) {
        return byggBeregningsgrunnlagForBehandling(medOppjustertDagsat, skalDeleAndelMellomArbeidsgiverOgBruker,
                perioder, new LagEnAndelTjeneste());
    }

    private BeregningsgrunnlagEntitet byggBeregningsgrunnlagForBehandling(boolean medOppjustertDagsat,
            boolean skalDeleAndelMellomArbeidsgiverOgBruker,
            List<ÅpenDatoIntervallEntitet> perioder,
            LagAndelTjeneste lagAndelTjeneste) {

        beregningsgrunnlag = BeregningsgrunnlagEntitet.ny()
                .medSkjæringstidspunkt(SKJÆRINGSTIDSPUNKT_BEREGNING)
                .medGrunnbeløp(BigDecimal.valueOf(91425L))
                .build();
        BeregningsgrunnlagAktivitetStatus.builder()
                .medAktivitetStatus(AktivitetStatus.ARBEIDSTAKER)
                .build(beregningsgrunnlag);
        for (var datoPeriode : perioder) {
            var periode = byggBGPeriode(datoPeriode, medOppjustertDagsat,
                    skalDeleAndelMellomArbeidsgiverOgBruker, lagAndelTjeneste);
            BeregningsgrunnlagPeriode.oppdater(periode)
                    .build(beregningsgrunnlag);
        }
        return beregningsgrunnlag;
    }

    private BeregningsgrunnlagPeriode byggBGPeriode(ÅpenDatoIntervallEntitet datoPeriode,
            boolean medOppjustertDagsat,
            boolean skalDeleAndelMellomArbeidsgiverOgBruker,
            LagAndelTjeneste lagAndelTjeneste) {
        var periode = BeregningsgrunnlagPeriode.ny()
                .medBeregningsgrunnlagPeriode(datoPeriode.getFomDato(), datoPeriode.getTomDato())
                .build(beregningsgrunnlag);
        lagAndelTjeneste.lagAndeler(periode, medOppjustertDagsat, skalDeleAndelMellomArbeidsgiverOgBruker);
        return periode;

    }
}
