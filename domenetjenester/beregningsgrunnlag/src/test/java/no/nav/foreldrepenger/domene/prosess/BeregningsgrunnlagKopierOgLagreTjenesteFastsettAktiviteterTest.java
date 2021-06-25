package no.nav.foreldrepenger.domene.prosess;

import static java.util.Optional.empty;
import static org.assertj.core.api.Assertions.assertThat;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import javax.inject.Inject;
import javax.persistence.EntityManager;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import no.nav.folketrygdloven.kalkulator.input.BeregningsgrunnlagInput;
import no.nav.folketrygdloven.kalkulator.input.ForeldrepengerGrunnlag;
import no.nav.folketrygdloven.kalkulator.modell.gradering.AktivitetGradering;
import no.nav.folketrygdloven.kalkulator.modell.opptjening.OpptjeningAktiviteterDto;
import no.nav.folketrygdloven.kalkulator.modell.typer.InternArbeidsforholdRefDto;
import no.nav.folketrygdloven.kalkulator.steg.BeregningsgrunnlagTjeneste;
import no.nav.folketrygdloven.kalkulator.tid.Intervall;
import no.nav.foreldrepenger.behandling.BehandlingReferanse;
import no.nav.foreldrepenger.behandling.Skjæringstidspunkt;
import no.nav.foreldrepenger.behandlingslager.behandling.opptjening.OpptjeningAktivitetType;
import no.nav.foreldrepenger.behandlingslager.behandling.repository.BehandlingRepository;
import no.nav.foreldrepenger.behandlingslager.virksomhet.ArbeidType;
import no.nav.foreldrepenger.behandlingslager.virksomhet.Arbeidsgiver;
import no.nav.foreldrepenger.dbstoette.CdiDbAwareTest;
import no.nav.foreldrepenger.domene.iay.modell.AktivitetsAvtaleBuilder;
import no.nav.foreldrepenger.domene.iay.modell.InntektArbeidYtelseAggregatBuilder;
import no.nav.foreldrepenger.domene.iay.modell.InntektArbeidYtelseGrunnlag;
import no.nav.foreldrepenger.domene.iay.modell.InntektArbeidYtelseGrunnlagBuilder;
import no.nav.foreldrepenger.domene.iay.modell.Inntektsmelding;
import no.nav.foreldrepenger.domene.iay.modell.InntektsmeldingBuilder;
import no.nav.foreldrepenger.domene.iay.modell.OppgittAnnenAktivitet;
import no.nav.foreldrepenger.domene.iay.modell.OppgittOpptjeningBuilder;
import no.nav.foreldrepenger.domene.iay.modell.VersjonType;
import no.nav.foreldrepenger.domene.iay.modell.YrkesaktivitetBuilder;
import no.nav.foreldrepenger.domene.input.BeregningTilInputTjeneste;
import no.nav.foreldrepenger.domene.input.KalkulatorStegProsesseringInputTjeneste;
import no.nav.foreldrepenger.domene.mappers.til_kalkulus.IAYMapperTilKalkulus;
import no.nav.foreldrepenger.domene.mappers.til_kalkulus.MapBehandlingRef;
import no.nav.foreldrepenger.domene.modell.BeregningAktivitetAggregatEntitet;
import no.nav.foreldrepenger.domene.modell.BeregningAktivitetEntitet;
import no.nav.foreldrepenger.domene.modell.BeregningAktivitetHandlingType;
import no.nav.foreldrepenger.domene.modell.BeregningAktivitetOverstyringEntitet;
import no.nav.foreldrepenger.domene.modell.BeregningAktivitetOverstyringerEntitet;
import no.nav.foreldrepenger.domene.modell.BeregningsgrunnlagGrunnlagBuilder;
import no.nav.foreldrepenger.domene.modell.BeregningsgrunnlagGrunnlagEntitet;
import no.nav.foreldrepenger.domene.modell.BeregningsgrunnlagRepository;
import no.nav.foreldrepenger.domene.modell.BeregningsgrunnlagTilstand;
import no.nav.foreldrepenger.domene.prosess.testutilities.behandling.ScenarioForeldrepenger;
import no.nav.foreldrepenger.domene.tid.DatoIntervallEntitet;
import no.nav.foreldrepenger.domene.tid.ÅpenDatoIntervallEntitet;

@CdiDbAwareTest
public class BeregningsgrunnlagKopierOgLagreTjenesteFastsettAktiviteterTest {

    private static final String ORG_NUMMER = "915933149";
    private static final String ORG_NUMMER2 = "915933148";

    private static final Arbeidsgiver VIRKSOMHET = Arbeidsgiver.virksomhet(ORG_NUMMER);
    private static final Arbeidsgiver VIRKSOMHET2 = Arbeidsgiver.virksomhet(ORG_NUMMER2);

    private static final LocalDate SKJÆRINGSTIDSPUNKT = LocalDate.now();
    private final RepositoryProvider repositoryProvider;
    private final BeregningsgrunnlagRepository beregningsgrunnlagRepository;
    private final BehandlingRepository behandlingRepository;

    private BeregningsgrunnlagKopierOgLagreTjeneste beregningsgrunnlagKopierOgLagreTjeneste;
    private BehandlingReferanse behandlingReferanse;

    @Inject
    private BeregningsgrunnlagTjeneste beregningsgrunnlagTjeneste;

    public BeregningsgrunnlagKopierOgLagreTjenesteFastsettAktiviteterTest(EntityManager em) {
        repositoryProvider = new RepositoryProvider(em);
        beregningsgrunnlagRepository = repositoryProvider.getBeregningsgrunnlagRepository();
        behandlingRepository = repositoryProvider.getBehandlingRepository();
    }

    @BeforeEach
    public void setUp() {
        var kalkulusKonfigInjecter = new KalkulusKonfigInjecter(5);
        var beregningTilInputTjeneste = new BeregningTilInputTjeneste(beregningsgrunnlagRepository,
            kalkulusKonfigInjecter);
        var kalkulatorStegProsesseringInputTjeneste = new KalkulatorStegProsesseringInputTjeneste(
            beregningsgrunnlagRepository, behandlingRepository, beregningTilInputTjeneste,
            new GrunnbeløpTjeneste(beregningsgrunnlagRepository), kalkulusKonfigInjecter);
        behandlingReferanse = lagBehandlingReferanse();
        beregningsgrunnlagKopierOgLagreTjeneste = new BeregningsgrunnlagKopierOgLagreTjeneste(
            beregningsgrunnlagRepository, beregningsgrunnlagTjeneste, kalkulatorStegProsesseringInputTjeneste);
    }

    @Test
    public void skal_kunne_kjøre_fastsett_aktiviteter_første_gang_med_aksjonspunkt() {
        // Arrange
        var iayGr = lagIAYGrunnlagForArbeidOgVentelønnVartpenger();
        var input = lagBeregningsgrunnlagInput(behandlingReferanse, iayGr,
            lagOpptjeningAktiviteterMedArbeidOgVentelønnVartpenger());

        // Act
        var ap = beregningsgrunnlagKopierOgLagreTjeneste.fastsettBeregningsaktiviteter(input);

        // Assert
        var bgMedAktiviteter = beregningsgrunnlagRepository.hentBeregningsgrunnlagGrunnlagEntitet(
            behandlingReferanse.getBehandlingId());
        assertThat(bgMedAktiviteter).isPresent();
        assertThat(bgMedAktiviteter.get().getBeregningsgrunnlagTilstand()).isEqualTo(
            BeregningsgrunnlagTilstand.OPPRETTET);
        assertThat(ap).hasSize(1);
    }

    @Test
    public void skal_kunne_kjøre_fastsett_aktiviteter_andre_gang_med_aksjonspunkt_uten_endringer_i_input() {
        // Arrange
        var iayGr = lagIAYGrunnlagForArbeidOgVentelønnVartpenger();
        var input = lagBeregningsgrunnlagInput(behandlingReferanse, iayGr,
            lagOpptjeningAktiviteterMedArbeidOgVentelønnVartpenger());

        // Act: kjør første gang
        var ap = beregningsgrunnlagKopierOgLagreTjeneste.fastsettBeregningsaktiviteter(input);

        // Assert
        var bgMedAktiviteter = beregningsgrunnlagRepository.hentBeregningsgrunnlagGrunnlagEntitet(
            behandlingReferanse.getBehandlingId());
        assertThat(bgMedAktiviteter).isPresent();
        assertThat(ap).hasSize(1);

        // Arrange: lag bekreftet aggregat
        lagreSaksbehandletFjernArbeidOgDeaktiver(bgMedAktiviteter);

        // Act: kjør andre gang
        var ap2 = beregningsgrunnlagKopierOgLagreTjeneste.fastsettBeregningsaktiviteter(input);

        // Assert
        var bgMedSaksbehandlet = beregningsgrunnlagRepository.hentBeregningsgrunnlagGrunnlagEntitet(
            behandlingReferanse.getBehandlingId());
        assertThat(bgMedSaksbehandlet).isPresent();
        assertThat(ap2).hasSize(1);
        assertThat(bgMedSaksbehandlet.get().getSaksbehandletAktiviteter()).isPresent();
        assertThat(bgMedSaksbehandlet.get().getBeregningsgrunnlagTilstand()).isEqualTo(
            BeregningsgrunnlagTilstand.FASTSATT_BEREGNINGSAKTIVITETER);
    }

    @Test
    public void skal_ikkje_kopiere_om_man_ikkje_har_aksjonspunkt_eller_overstyring_i_input() {
        // Arrange
        var iayGr = lagIAYGrunnlagForToArbeidsforhold();
        var input = lagBeregningsgrunnlagInput(behandlingReferanse, iayGr,
            lagOpptjeningAktiviteterMedToArbeidsforhold());

        // Act: kjør første gang
        var ap = beregningsgrunnlagKopierOgLagreTjeneste.fastsettBeregningsaktiviteter(input);

        // Assert
        var bgMedAktiviteter = beregningsgrunnlagRepository.hentBeregningsgrunnlagGrunnlagEntitet(
            behandlingReferanse.getBehandlingId());
        assertThat(bgMedAktiviteter).isPresent();
        assertThat(ap).hasSize(0);

        // Act: kjør andre gang
        var ap2 = beregningsgrunnlagKopierOgLagreTjeneste.fastsettBeregningsaktiviteter(input);

        // Assert
        var bgMedAktiviteter2 = beregningsgrunnlagRepository.hentBeregningsgrunnlagGrunnlagEntitet(
            behandlingReferanse.getBehandlingId());
        assertThat(bgMedAktiviteter2).isPresent();
        assertThat(ap2).hasSize(0);
        assertThat(bgMedAktiviteter2.get().getBeregningsgrunnlagTilstand()).isEqualTo(
            BeregningsgrunnlagTilstand.OPPRETTET);
    }

    @Test
    public void skal_kunne_kjøre_fastsett_aktiviteter_andre_gang_med_aksjonspunkt_med_endringer_i_input() {
        // Arrange
        var iayGr = lagIAYGrunnlagForArbeidOgVentelønnVartpenger();
        var input = lagBeregningsgrunnlagInput(behandlingReferanse, iayGr,
            lagOpptjeningAktiviteterMedArbeidOgVentelønnVartpenger());

        // Act: kjør første gang
        var ap = beregningsgrunnlagKopierOgLagreTjeneste.fastsettBeregningsaktiviteter(input);

        // Assert
        var bgMedAktiviteter = beregningsgrunnlagRepository.hentBeregningsgrunnlagGrunnlagEntitet(
            behandlingReferanse.getBehandlingId());
        assertThat(bgMedAktiviteter).isPresent();
        assertThat(ap).hasSize(1);

        // Arrange: lag bekreftet aggregat
        lagreSaksbehandletFjernArbeidOgDeaktiver(bgMedAktiviteter);

        // Endre input
        var iayGr2 = lagIAYGrunnlagForToArbeidsforholdOgVentelønnVartpenger();
        var input2 = lagBeregningsgrunnlagInput(behandlingReferanse, iayGr2,
            lagOpptjeningAktiviteterMedToArbeidsforholdOgVentelønnVartpenger());

        // Act: kjør andre gang
        var ap2 = beregningsgrunnlagKopierOgLagreTjeneste.fastsettBeregningsaktiviteter(input2);

        // Assert
        var bgUtenSaksbehandlet = beregningsgrunnlagRepository.hentBeregningsgrunnlagGrunnlagEntitet(
            behandlingReferanse.getBehandlingId());
        assertThat(bgUtenSaksbehandlet).isPresent();
        assertThat(ap2).hasSize(1);
        assertThat(bgUtenSaksbehandlet.get().getSaksbehandletAktiviteter()).isEmpty();
        assertThat(bgUtenSaksbehandlet.get().getBeregningsgrunnlagTilstand()).isEqualTo(
            BeregningsgrunnlagTilstand.OPPRETTET);
    }

    @Test
    public void skal_ta_vare_på_overstyringer_for_andre_kjøring_med_endringer_i_input() {
        // Arrange
        var iayGr = lagIAYGrunnlagForToArbeidsforhold();
        var input = lagBeregningsgrunnlagInput(behandlingReferanse, iayGr,
            lagOpptjeningAktiviteterMedToArbeidsforhold());

        // Act: kjør første gang
        var ap = beregningsgrunnlagKopierOgLagreTjeneste.fastsettBeregningsaktiviteter(input);

        // Assert
        var bgMedAktiviteter = beregningsgrunnlagRepository.hentBeregningsgrunnlagGrunnlagEntitet(
            behandlingReferanse.getBehandlingId());
        assertThat(bgMedAktiviteter).isPresent();
        assertThat(ap).hasSize(0);

        // Arrange: lag overstyrt aggregat
        lagreOverstyrtFjernEttArbeidsforholdOgDeaktiver(bgMedAktiviteter);

        // Endre input
        var iayGr2 = lagIAYGrunnlagForToArbeidsforholdOgVentelønnVartpenger();
        var input2 = lagBeregningsgrunnlagInput(behandlingReferanse, iayGr2,
            lagOpptjeningAktiviteterMedToArbeidsforholdOgVentelønnVartpenger());

        // Act: kjør andre gang
        var ap2 = beregningsgrunnlagKopierOgLagreTjeneste.fastsettBeregningsaktiviteter(input2);

        // Assert
        var bgMedOverstyring = beregningsgrunnlagRepository.hentBeregningsgrunnlagGrunnlagEntitet(
            behandlingReferanse.getBehandlingId());
        assertThat(bgMedOverstyring).isPresent();
        assertThat(ap2).hasSize(0);
        assertThat(bgMedOverstyring.get().getOverstyring()).isPresent();
        assertThat(bgMedOverstyring.get().getBeregningsgrunnlagTilstand()).isEqualTo(
            BeregningsgrunnlagTilstand.OPPRETTET);
    }

    @Test
    public void skal_ta_vare_på_overstyringer_for_andre_kjøring_uten_endringer_i_input() {
        // Arrange
        var iayGr = lagIAYGrunnlagForToArbeidsforhold();
        var input = lagBeregningsgrunnlagInput(behandlingReferanse, iayGr,
            lagOpptjeningAktiviteterMedToArbeidsforhold());

        // Act: kjør første gang
        var ap = beregningsgrunnlagKopierOgLagreTjeneste.fastsettBeregningsaktiviteter(input);

        // Assert
        var bgMedAktiviteter = beregningsgrunnlagRepository.hentBeregningsgrunnlagGrunnlagEntitet(
            behandlingReferanse.getBehandlingId());
        assertThat(bgMedAktiviteter).isPresent();
        assertThat(ap).hasSize(0);

        // Arrange: lag overstyrt aggregat
        lagreOverstyrtFjernEttArbeidsforholdOgDeaktiver(bgMedAktiviteter);

        // Act: kjør andre gang
        var ap2 = beregningsgrunnlagKopierOgLagreTjeneste.fastsettBeregningsaktiviteter(input);

        // Assert
        var bgMedOverstyring = beregningsgrunnlagRepository.hentBeregningsgrunnlagGrunnlagEntitet(
            behandlingReferanse.getBehandlingId());
        assertThat(bgMedOverstyring).isPresent();
        assertThat(ap2).hasSize(0);
        assertThat(bgMedOverstyring.get().getOverstyring()).isPresent();
        assertThat(bgMedOverstyring.get().getBeregningsgrunnlagTilstand()).isEqualTo(
            BeregningsgrunnlagTilstand.FASTSATT_BEREGNINGSAKTIVITETER);
    }

    private void lagreSaksbehandletFjernArbeidOgDeaktiver(Optional<BeregningsgrunnlagGrunnlagEntitet> bgMedAktiviteter) {
        var saksbehandletGrunnlag = BeregningsgrunnlagGrunnlagBuilder.kopi(bgMedAktiviteter)
            .medSaksbehandletAktiviteter(BeregningAktivitetAggregatEntitet.builder()
                .medSkjæringstidspunktOpptjening(SKJÆRINGSTIDSPUNKT)
                .leggTilAktivitet(BeregningAktivitetEntitet.builder()
                    .medOpptjeningAktivitetType(OpptjeningAktivitetType.VENTELØNN_VARTPENGER)
                    .medPeriode(ÅpenDatoIntervallEntitet.fraOgMedTilOgMed(SKJÆRINGSTIDSPUNKT.minusYears(1),
                        SKJÆRINGSTIDSPUNKT.plusMonths(1)))
                    .build())
                .build());
        beregningsgrunnlagRepository.lagre(behandlingReferanse.getBehandlingId(), saksbehandletGrunnlag,
            BeregningsgrunnlagTilstand.FASTSATT_BEREGNINGSAKTIVITETER);
        beregningsgrunnlagRepository.deaktiverBeregningsgrunnlagGrunnlagEntitet(behandlingReferanse.getBehandlingId());
    }

    private void lagreOverstyrtFjernEttArbeidsforholdOgDeaktiver(Optional<BeregningsgrunnlagGrunnlagEntitet> bgMedAktiviteter) {
        var saksbehandletGrunnlag = BeregningsgrunnlagGrunnlagBuilder.kopi(bgMedAktiviteter)
            .medOverstyring(BeregningAktivitetOverstyringerEntitet.builder()
                .leggTilOverstyring(BeregningAktivitetOverstyringEntitet.builder()
                    .medArbeidsgiver(VIRKSOMHET)
                    .medOpptjeningAktivitetType(OpptjeningAktivitetType.ARBEID)
                    .medHandling(BeregningAktivitetHandlingType.BENYTT)
                    .medPeriode(ÅpenDatoIntervallEntitet.fraOgMedTilOgMed(SKJÆRINGSTIDSPUNKT.minusYears(1),
                        SKJÆRINGSTIDSPUNKT.plusMonths(1)))
                    .build())
                .leggTilOverstyring(BeregningAktivitetOverstyringEntitet.builder()
                    .medArbeidsgiver(VIRKSOMHET2)
                    .medOpptjeningAktivitetType(OpptjeningAktivitetType.ARBEID)
                    .medHandling(BeregningAktivitetHandlingType.IKKE_BENYTT)
                    .medPeriode(ÅpenDatoIntervallEntitet.fraOgMedTilOgMed(SKJÆRINGSTIDSPUNKT.minusYears(1),
                        SKJÆRINGSTIDSPUNKT.plusMonths(1)))
                    .build())
                .build());
        beregningsgrunnlagRepository.lagre(behandlingReferanse.getBehandlingId(), saksbehandletGrunnlag,
            BeregningsgrunnlagTilstand.FASTSATT_BEREGNINGSAKTIVITETER);
        beregningsgrunnlagRepository.deaktiverBeregningsgrunnlagGrunnlagEntitet(behandlingReferanse.getBehandlingId());
    }

    private BehandlingReferanse lagBehandlingReferanse() {
        var behandling = ScenarioForeldrepenger.nyttScenario().lagre(repositoryProvider);
        return BehandlingReferanse.fra(behandling)
            .medSkjæringstidspunkt(Skjæringstidspunkt.builder()
                .medFørsteUttaksdato(SKJÆRINGSTIDSPUNKT)
                .medFørsteUttaksdatoGrunnbeløp(SKJÆRINGSTIDSPUNKT)
                .medSkjæringstidspunktOpptjening(SKJÆRINGSTIDSPUNKT)
                .medSkjæringstidspunktBeregning(SKJÆRINGSTIDSPUNKT)
                .medUtledetSkjæringstidspunkt(SKJÆRINGSTIDSPUNKT)
                .build());
    }

    private InntektArbeidYtelseGrunnlag lagIAYGrunnlagForToArbeidsforholdOgVentelønnVartpenger() {
        var arbeid = lagArbeidYA(VIRKSOMHET);
        var arbeid2 = lagArbeidYA(VIRKSOMHET2);
        var oppdatere = InntektArbeidYtelseAggregatBuilder.oppdatere(empty(), VersjonType.REGISTER)
            .leggTilAktørArbeid(InntektArbeidYtelseAggregatBuilder.AktørArbeidBuilder.oppdatere(empty())
                .medAktørId(behandlingReferanse.getAktørId())
                .leggTilYrkesaktivitet(arbeid2)
                .leggTilYrkesaktivitet(arbeid));

        return InntektArbeidYtelseGrunnlagBuilder.nytt()
            .medOppgittOpptjening(lagVentelønnVartpengerOppgittOpptjening())
            .medData(oppdatere)
            .medInntektsmeldinger(List.of(lagInntektsmelding(VIRKSOMHET), lagInntektsmelding(VIRKSOMHET2)))
            .build();
    }

    private InntektArbeidYtelseGrunnlag lagIAYGrunnlagForArbeidOgVentelønnVartpenger() {
        var arbeid = lagArbeidYA(VIRKSOMHET);
        var oppdatere = InntektArbeidYtelseAggregatBuilder.oppdatere(empty(), VersjonType.REGISTER)
            .leggTilAktørArbeid(InntektArbeidYtelseAggregatBuilder.AktørArbeidBuilder.oppdatere(empty())
                .medAktørId(behandlingReferanse.getAktørId())
                .leggTilYrkesaktivitet(arbeid));

        return InntektArbeidYtelseGrunnlagBuilder.nytt()
            .medOppgittOpptjening(lagVentelønnVartpengerOppgittOpptjening())
            .medData(oppdatere)
            .medInntektsmeldinger(List.of(lagInntektsmelding(VIRKSOMHET)))
            .build();
    }

    private InntektArbeidYtelseGrunnlag lagIAYGrunnlagForToArbeidsforhold() {
        var arbeid = lagArbeidYA(VIRKSOMHET);
        var arbeid2 = lagArbeidYA(VIRKSOMHET2);
        var oppdatere = InntektArbeidYtelseAggregatBuilder.oppdatere(empty(), VersjonType.REGISTER)
            .leggTilAktørArbeid(InntektArbeidYtelseAggregatBuilder.AktørArbeidBuilder.oppdatere(empty())
                .medAktørId(behandlingReferanse.getAktørId())
                .leggTilYrkesaktivitet(arbeid)
                .leggTilYrkesaktivitet(arbeid2));
        return InntektArbeidYtelseGrunnlagBuilder.nytt()
            .medData(oppdatere)
            .medInntektsmeldinger(List.of(lagInntektsmelding(VIRKSOMHET), lagInntektsmelding(VIRKSOMHET2)))
            .build();
    }

    private Inntektsmelding lagInntektsmelding(Arbeidsgiver virksomhet) {
        return InntektsmeldingBuilder.builder().medBeløp(BigDecimal.TEN).medArbeidsgiver(virksomhet).build();
    }

    private YrkesaktivitetBuilder lagArbeidYA(Arbeidsgiver virksomhet) {
        return YrkesaktivitetBuilder.oppdatere(empty())
            .medArbeidsgiver(virksomhet)
            .leggTilAktivitetsAvtale(AktivitetsAvtaleBuilder.ny()
                .medPeriode(DatoIntervallEntitet.fraOgMedTilOgMed(
                    SKJÆRINGSTIDSPUNKT.minusYears(1), SKJÆRINGSTIDSPUNKT.plusMonths(10))))
            .leggTilAktivitetsAvtale(AktivitetsAvtaleBuilder.ny()
                .medPeriode(DatoIntervallEntitet.fraOgMedTilOgMed(SKJÆRINGSTIDSPUNKT.minusYears(1),
                    SKJÆRINGSTIDSPUNKT.plusMonths(10)))
                .medProsentsats(BigDecimal.valueOf(100)))
            .medArbeidType(ArbeidType.ORDINÆRT_ARBEIDSFORHOLD);
    }

    private OppgittOpptjeningBuilder lagVentelønnVartpengerOppgittOpptjening() {
        return OppgittOpptjeningBuilder.ny()
            .leggTilAnnenAktivitet(new OppgittAnnenAktivitet(
                DatoIntervallEntitet.fraOgMedTilOgMed(SKJÆRINGSTIDSPUNKT.minusYears(1),
                    SKJÆRINGSTIDSPUNKT.plusMonths(1)), ArbeidType.VENTELØNN_VARTPENGER));
    }

    private BeregningsgrunnlagInput lagBeregningsgrunnlagInput(BehandlingReferanse behandlingReferanse,
                                                               InntektArbeidYtelseGrunnlag iayGr,
                                                               OpptjeningAktiviteterDto opptjeningAktiviteter) {
        var iayGrunnlag = IAYMapperTilKalkulus.mapGrunnlag(iayGr, behandlingReferanse.getAktørId());
        var behandlingReferanse1 = MapBehandlingRef.mapRef(behandlingReferanse);
        return new BeregningsgrunnlagInput(behandlingReferanse1, iayGrunnlag, opptjeningAktiviteter, List.of(),
            new ForeldrepengerGrunnlag(100, false, AktivitetGradering.INGEN_GRADERING));
    }

    private OpptjeningAktiviteterDto lagOpptjeningAktiviteterMedArbeidOgVentelønnVartpenger() {
        return new OpptjeningAktiviteterDto(
            List.of(lagOpptjeningAktivitetArbeid(ORG_NUMMER), lagOpptjeningAktivitetVentelønnVartpenger()));
    }

    private OpptjeningAktiviteterDto lagOpptjeningAktiviteterMedToArbeidsforholdOgVentelønnVartpenger() {
        return new OpptjeningAktiviteterDto(
            List.of(lagOpptjeningAktivitetArbeid(ORG_NUMMER), lagOpptjeningAktivitetArbeid(ORG_NUMMER2),
                lagOpptjeningAktivitetVentelønnVartpenger()));
    }

    private OpptjeningAktiviteterDto lagOpptjeningAktiviteterMedToArbeidsforhold() {
        return new OpptjeningAktiviteterDto(
            List.of(lagOpptjeningAktivitetArbeid(ORG_NUMMER), lagOpptjeningAktivitetArbeid(ORG_NUMMER2)));
    }

    private OpptjeningAktiviteterDto.OpptjeningPeriodeDto lagOpptjeningAktivitetArbeid(String orgNummer) {
        return OpptjeningAktiviteterDto.nyPeriode(
            no.nav.folketrygdloven.kalkulus.kodeverk.OpptjeningAktivitetType.ARBEID,
            Intervall.fraOgMedTilOgMed(SKJÆRINGSTIDSPUNKT.minusMonths(10), SKJÆRINGSTIDSPUNKT), orgNummer, null,
            InternArbeidsforholdRefDto.nyRef());
    }

    private OpptjeningAktiviteterDto.OpptjeningPeriodeDto lagOpptjeningAktivitetVentelønnVartpenger() {
        return OpptjeningAktiviteterDto.nyPeriode(
            no.nav.folketrygdloven.kalkulus.kodeverk.OpptjeningAktivitetType.VENTELØNN_VARTPENGER,
            Intervall.fraOgMedTilOgMed(SKJÆRINGSTIDSPUNKT.minusMonths(10), SKJÆRINGSTIDSPUNKT));
    }

}
