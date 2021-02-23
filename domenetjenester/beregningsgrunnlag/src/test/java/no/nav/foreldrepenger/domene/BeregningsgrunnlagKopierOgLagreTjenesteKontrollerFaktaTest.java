package no.nav.foreldrepenger.domene;

import static java.util.Optional.empty;
import static no.nav.foreldrepenger.domene.modell.AktivitetStatus.ARBEIDSTAKER;
import static no.nav.foreldrepenger.domene.modell.BeregningsgrunnlagTilstand.FASTSATT_BEREGNINGSAKTIVITETER;
import static no.nav.foreldrepenger.domene.modell.BeregningsgrunnlagTilstand.KOFAKBER_UT;
import static no.nav.foreldrepenger.domene.modell.BeregningsgrunnlagTilstand.OPPDATERT_MED_ANDELER;
import static org.assertj.core.api.Assertions.assertThat;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import javax.inject.Inject;
import javax.persistence.EntityManager;

import no.nav.folketrygdloven.kalkulator.tid.Intervall;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import no.nav.folketrygdloven.kalkulator.input.ForeldrepengerGrunnlag;
import no.nav.folketrygdloven.kalkulator.input.BeregningsgrunnlagInput;
import no.nav.folketrygdloven.kalkulator.modell.gradering.AktivitetGradering;
import no.nav.folketrygdloven.kalkulator.modell.opptjening.OpptjeningAktiviteterDto;
import no.nav.folketrygdloven.kalkulator.modell.typer.InternArbeidsforholdRefDto;
import no.nav.folketrygdloven.kalkulator.output.BeregningAksjonspunktResultat;
import no.nav.folketrygdloven.kalkulator.steg.BeregningsgrunnlagTjeneste;
import no.nav.foreldrepenger.behandling.BehandlingReferanse;
import no.nav.foreldrepenger.behandling.Skjæringstidspunkt;
import no.nav.foreldrepenger.behandlingslager.behandling.opptjening.OpptjeningAktivitetType;
import no.nav.foreldrepenger.behandlingslager.virksomhet.ArbeidType;
import no.nav.foreldrepenger.behandlingslager.virksomhet.Arbeidsgiver;
import no.nav.foreldrepenger.dbstoette.CdiDbAwareTest;
import no.nav.foreldrepenger.domene.input.KalkulatorStegProsesseringInputTjeneste;
import no.nav.foreldrepenger.domene.mappers.til_kalkulus.IAYMapperTilKalkulus;
import no.nav.foreldrepenger.domene.mappers.til_kalkulus.MapBehandlingRef;
import no.nav.foreldrepenger.domene.testutilities.behandling.ScenarioForeldrepenger;
import no.nav.foreldrepenger.domene.modell.BGAndelArbeidsforhold;
import no.nav.foreldrepenger.domene.modell.BeregningAktivitetAggregatEntitet;
import no.nav.foreldrepenger.domene.modell.BeregningAktivitetEntitet;
import no.nav.foreldrepenger.domene.modell.BeregningsgrunnlagAktivitetStatus;
import no.nav.foreldrepenger.domene.modell.BeregningsgrunnlagEntitet;
import no.nav.foreldrepenger.domene.modell.BeregningsgrunnlagGrunnlagBuilder;
import no.nav.foreldrepenger.domene.modell.BeregningsgrunnlagGrunnlagEntitet;
import no.nav.foreldrepenger.domene.modell.BeregningsgrunnlagPeriode;
import no.nav.foreldrepenger.domene.modell.BeregningsgrunnlagPrStatusOgAndel;
import no.nav.foreldrepenger.domene.modell.BeregningsgrunnlagRepository;
import no.nav.foreldrepenger.domene.modell.FaktaOmBeregningTilfelle;
import no.nav.foreldrepenger.domene.iay.modell.AktivitetsAvtaleBuilder;
import no.nav.foreldrepenger.domene.iay.modell.ArbeidsforholdInformasjonBuilder;
import no.nav.foreldrepenger.domene.iay.modell.ArbeidsforholdOverstyringBuilder;
import no.nav.foreldrepenger.domene.iay.modell.InntektArbeidYtelseAggregatBuilder;
import no.nav.foreldrepenger.domene.iay.modell.InntektArbeidYtelseGrunnlag;
import no.nav.foreldrepenger.domene.iay.modell.InntektArbeidYtelseGrunnlagBuilder;
import no.nav.foreldrepenger.domene.iay.modell.VersjonType;
import no.nav.foreldrepenger.domene.iay.modell.YrkesaktivitetBuilder;
import no.nav.foreldrepenger.domene.iay.modell.kodeverk.ArbeidsforholdHandlingType;
import no.nav.foreldrepenger.domene.tid.DatoIntervallEntitet;
import no.nav.foreldrepenger.domene.tid.ÅpenDatoIntervallEntitet;

@CdiDbAwareTest
public class BeregningsgrunnlagKopierOgLagreTjenesteKontrollerFaktaTest {

    private static final String ORG_NUMMER = "915933149";

    public static final LocalDate SKJÆRINGSTIDSPUNKT = LocalDate.now();
    private RepositoryProvider repositoryProvider;
    private BeregningsgrunnlagRepository beregningsgrunnlagRepository;
    private BeregningsgrunnlagKopierOgLagreTjeneste beregningsgrunnlagKopierOgLagreTjeneste;
    private BehandlingReferanse behandlingReferanse;
    private BeregningsgrunnlagInput input;

    @Inject
    private KalkulatorStegProsesseringInputTjeneste kalkulatorStegProsesseringInputTjeneste;
    @Inject
    private BeregningsgrunnlagTjeneste beregningsgrunnlagTjeneste;

    @BeforeEach
    public void setUp(EntityManager em) {
        repositoryProvider = new RepositoryProvider(em);
        beregningsgrunnlagRepository = repositoryProvider.getBeregningsgrunnlagRepository();
        beregningsgrunnlagKopierOgLagreTjeneste = new BeregningsgrunnlagKopierOgLagreTjeneste(
            beregningsgrunnlagRepository,
            beregningsgrunnlagTjeneste,
            kalkulatorStegProsesseringInputTjeneste);

        Arbeidsgiver virksomhet = Arbeidsgiver.virksomhet(ORG_NUMMER);
        behandlingReferanse = lagBehandlingReferanse();
        LocalDate arbeidsperiodeFom = SKJÆRINGSTIDSPUNKT.minusYears(1);
        LocalDate arbeidsperiodeTom = SKJÆRINGSTIDSPUNKT.plusMonths(10);
        InntektArbeidYtelseGrunnlag iayGr = lagIAYGrunnlagForArbeidUtenInntektsmeldingMedLønnsendring(virksomhet, behandlingReferanse,
            arbeidsperiodeFom, arbeidsperiodeTom);
        BeregningsgrunnlagGrunnlagBuilder bgGrunnlag = lagBeregningsgrunnlagMedAndelOgAktiviteter(virksomhet, arbeidsperiodeFom, arbeidsperiodeTom);
        beregningsgrunnlagRepository.lagre(behandlingReferanse.getBehandlingId(), bgGrunnlag, FASTSATT_BEREGNINGSAKTIVITETER);
        input = lagBeregningsgrunnlagInput(behandlingReferanse, iayGr);

    }

    @Test
    public void skal_kunne_kjøre_fakta_beregning_første_gang_med_aksjonspunkt() {
        // Act
        List<BeregningAksjonspunktResultat> ap = beregningsgrunnlagKopierOgLagreTjeneste.kontrollerFaktaBeregningsgrunnlag(input);

        // Assert
        Optional<BeregningsgrunnlagGrunnlagEntitet> bgMedAndeler = beregningsgrunnlagRepository
            .hentBeregningsgrunnlagGrunnlagEntitet(behandlingReferanse.getBehandlingId());
        assertThat(bgMedAndeler).isPresent();
        assertThat(ap).hasSize(1);
        List<FaktaOmBeregningTilfelle> faktaOmBeregningTilfelles = finnTilfeller(bgMedAndeler);
        assertThat(faktaOmBeregningTilfelles).containsExactlyInAnyOrder(FaktaOmBeregningTilfelle.VURDER_MOTTAR_YTELSE,
            FaktaOmBeregningTilfelle.VURDER_LØNNSENDRING);

    }

    private List<FaktaOmBeregningTilfelle> finnTilfeller(Optional<BeregningsgrunnlagGrunnlagEntitet> bgMedAndeler) {
        return bgMedAndeler
            .flatMap(BeregningsgrunnlagGrunnlagEntitet::getBeregningsgrunnlag)
            .map(BeregningsgrunnlagEntitet::getFaktaOmBeregningTilfeller)
            .orElse(Collections.emptyList());
    }

    @Test
    public void skal_kunne_kjøre_fakta_beregning_andre_gang_uten_endringer_med_aksjonspunkt() {
        // Kjører steg første gang
        beregningsgrunnlagKopierOgLagreTjeneste.kontrollerFaktaBeregningsgrunnlag(input);

        // Lager bekreftet
        lagreBekreftetLønnsendring();

        BeregningsgrunnlagEntitet bgFraFastsattAktiviteter = beregningsgrunnlagRepository
            .hentSisteBeregningsgrunnlagGrunnlagEntitet(behandlingReferanse.getBehandlingId(), FASTSATT_BEREGNINGSAKTIVITETER)
            .orElseThrow().getBeregningsgrunnlag().orElseThrow();

        // Simulerer tilbakehopp ved å lagre grunnlag fra forrige steg på nytt slik at
        // dette bli aktivt
        beregningsgrunnlagRepository.lagre(behandlingReferanse.getBehandlingId(), bgFraFastsattAktiviteter, FASTSATT_BEREGNINGSAKTIVITETER);

        // Act
        List<BeregningAksjonspunktResultat> ap = beregningsgrunnlagKopierOgLagreTjeneste.kontrollerFaktaBeregningsgrunnlag(input);

        // Assert
        Optional<BeregningsgrunnlagGrunnlagEntitet> bgMedAndeler = beregningsgrunnlagRepository
            .hentBeregningsgrunnlagGrunnlagEntitet(behandlingReferanse.getBehandlingId());
        assertThat(bgMedAndeler).isPresent();
        assertThat(ap).hasSize(1);
        List<FaktaOmBeregningTilfelle> faktaOmBeregningTilfelles = finnTilfeller(bgMedAndeler);
        assertThat(faktaOmBeregningTilfelles).containsExactlyInAnyOrder(FaktaOmBeregningTilfelle.VURDER_MOTTAR_YTELSE,
            FaktaOmBeregningTilfelle.VURDER_LØNNSENDRING);
        Boolean erLønnsendring = bgMedAndeler.get().getBeregningsgrunnlag().get()
            .getBeregningsgrunnlagPerioder().get(0).getBeregningsgrunnlagPrStatusOgAndelList().get(0)
            .getBgAndelArbeidsforhold().get().erLønnsendringIBeregningsperioden();
        assertThat(erLønnsendring).isTrue();

    }

    private void lagreBekreftetLønnsendring() {
        BeregningsgrunnlagEntitet bgFraFastsattAktiviteter = beregningsgrunnlagRepository
            .hentSisteBeregningsgrunnlagGrunnlagEntitet(behandlingReferanse.getBehandlingId(), OPPDATERT_MED_ANDELER)
            .orElseThrow().getBeregningsgrunnlag().orElseThrow();
        BeregningsgrunnlagEntitet bekreftetBg = new BeregningsgrunnlagEntitet(bgFraFastsattAktiviteter);
        BGAndelArbeidsforhold
            .builder(bekreftetBg.getBeregningsgrunnlagPerioder().get(0).getBeregningsgrunnlagPrStatusOgAndelList().get(0)
                .getBgAndelArbeidsforhold().get())
            .medLønnsendringIBeregningsperioden(true);
        beregningsgrunnlagRepository.lagre(behandlingReferanse.getBehandlingId(), bekreftetBg, KOFAKBER_UT);
    }

    private BeregningsgrunnlagInput lagBeregningsgrunnlagInput(BehandlingReferanse behandlingReferanse, InntektArbeidYtelseGrunnlag iayGr) {
        return new BeregningsgrunnlagInput(MapBehandlingRef.mapRef(behandlingReferanse), IAYMapperTilKalkulus.mapGrunnlag(iayGr, behandlingReferanse.getAktørId()),
                new OpptjeningAktiviteterDto(
                        List.of(OpptjeningAktiviteterDto.nyPeriode(no.nav.folketrygdloven.kalkulus.kodeverk.OpptjeningAktivitetType.ARBEID,
                                Intervall.fraOgMedTilOgMed(SKJÆRINGSTIDSPUNKT.minusMonths(10), SKJÆRINGSTIDSPUNKT), ORG_NUMMER, null,
                                InternArbeidsforholdRefDto.nullRef()))),
                List.of(),
                new ForeldrepengerGrunnlag(100, false, AktivitetGradering.INGEN_GRADERING));
    }

    private BehandlingReferanse lagBehandlingReferanse() {
        return ScenarioForeldrepenger.nyttScenario().lagre(repositoryProvider).medSkjæringstidspunkt(
            Skjæringstidspunkt.builder()
                .medFørsteUttaksdato(SKJÆRINGSTIDSPUNKT)
                .medSkjæringstidspunktOpptjening(SKJÆRINGSTIDSPUNKT)
                .medSkjæringstidspunktBeregning(SKJÆRINGSTIDSPUNKT)
                .medUtledetSkjæringstidspunkt(SKJÆRINGSTIDSPUNKT)
                .build());
    }

    private BeregningsgrunnlagGrunnlagBuilder lagBeregningsgrunnlagMedAndelOgAktiviteter(Arbeidsgiver virksomhet, LocalDate arbeidsperiodeFom,
                                                                                         LocalDate arbeidsperiodeTom) {
        BeregningsgrunnlagEntitet bg = BeregningsgrunnlagEntitet.ny()
            .medSkjæringstidspunkt(SKJÆRINGSTIDSPUNKT)
            .medGrunnbeløp(BigDecimal.valueOf(99_000))
            .leggTilAktivitetStatus(BeregningsgrunnlagAktivitetStatus.builder().medAktivitetStatus(ARBEIDSTAKER))
            .build();
        BeregningsgrunnlagPeriode periode = BeregningsgrunnlagPeriode.ny()
            .medBeregningsgrunnlagPeriode(SKJÆRINGSTIDSPUNKT, null)
            .build(bg);
        BeregningsgrunnlagPrStatusOgAndel.builder()
            .medAktivitetStatus(ARBEIDSTAKER)
            .medBGAndelArbeidsforhold(BGAndelArbeidsforhold.builder()
                .medArbeidsperiodeFom(arbeidsperiodeFom)
                .medArbeidsperiodeTom(arbeidsperiodeTom)
                .medArbeidsgiver(virksomhet))
            .build(periode);
        return BeregningsgrunnlagGrunnlagBuilder.oppdatere(empty())
            .medBeregningsgrunnlag(bg)
            .medRegisterAktiviteter(BeregningAktivitetAggregatEntitet.builder()
                .leggTilAktivitet(BeregningAktivitetEntitet.builder()
                    .medPeriode(ÅpenDatoIntervallEntitet.fraOgMedTilOgMed(arbeidsperiodeFom, arbeidsperiodeTom))
                    .medArbeidsgiver(virksomhet)
                    .medOpptjeningAktivitetType(OpptjeningAktivitetType.ARBEID).build())
                .medSkjæringstidspunktOpptjening(SKJÆRINGSTIDSPUNKT).build());
    }

    private InntektArbeidYtelseGrunnlag lagIAYGrunnlagForArbeidUtenInntektsmeldingMedLønnsendring(Arbeidsgiver virksomhet,
                                                                                                  BehandlingReferanse behandlingReferanse, LocalDate arbeidsperiodeFom, LocalDate arbeidsperiodeTom) {
        InntektArbeidYtelseAggregatBuilder oppdatere = InntektArbeidYtelseAggregatBuilder.oppdatere(empty(), VersjonType.REGISTER)
            .leggTilAktørArbeid(InntektArbeidYtelseAggregatBuilder.AktørArbeidBuilder.oppdatere(empty())
                .medAktørId(behandlingReferanse.getAktørId())
                .leggTilYrkesaktivitet(YrkesaktivitetBuilder.oppdatere(empty())
                    .medArbeidsgiver(virksomhet)
                    .leggTilAktivitetsAvtale(AktivitetsAvtaleBuilder.ny()
                        .medPeriode(DatoIntervallEntitet.fraOgMedTilOgMed(arbeidsperiodeFom, arbeidsperiodeTom)))
                    .leggTilAktivitetsAvtale(AktivitetsAvtaleBuilder.ny()
                        .medPeriode(DatoIntervallEntitet.fraOgMedTilOgMed(arbeidsperiodeFom, arbeidsperiodeTom))
                        .medProsentsats(BigDecimal.valueOf(100))
                        .medSisteLønnsendringsdato(SKJÆRINGSTIDSPUNKT.minusMonths(1)))
                    .medArbeidType(ArbeidType.ORDINÆRT_ARBEIDSFORHOLD)));

        return InntektArbeidYtelseGrunnlagBuilder.nytt()
            .medInformasjon(ArbeidsforholdInformasjonBuilder.oppdatere(empty()).leggTil(ArbeidsforholdOverstyringBuilder.oppdatere(empty())
                .medArbeidsgiver(virksomhet).medHandling(ArbeidsforholdHandlingType.BRUK_UTEN_INNTEKTSMELDING)).build())
            .medData(oppdatere)
            .build();
    }
}
