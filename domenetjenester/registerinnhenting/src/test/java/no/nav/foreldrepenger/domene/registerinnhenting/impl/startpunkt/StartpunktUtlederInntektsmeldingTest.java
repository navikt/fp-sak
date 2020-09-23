package no.nav.foreldrepenger.domene.registerinnhenting.impl.startpunkt;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;
import static org.mockito.MockitoAnnotations.initMocks;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.mockito.Mock;

import no.nav.foreldrepenger.behandling.BehandlingReferanse;
import no.nav.foreldrepenger.behandling.Skjæringstidspunkt;
import no.nav.foreldrepenger.behandlingslager.behandling.Behandling;
import no.nav.foreldrepenger.behandlingslager.behandling.BehandlingType;
import no.nav.foreldrepenger.behandlingslager.behandling.BehandlingÅrsakType;
import no.nav.foreldrepenger.behandlingslager.behandling.repository.BehandlingLås;
import no.nav.foreldrepenger.behandlingslager.behandling.repository.BehandlingRepository;
import no.nav.foreldrepenger.behandlingslager.behandling.repository.BehandlingRepositoryProvider;
import no.nav.foreldrepenger.behandlingslager.hendelser.StartpunktType;
import no.nav.foreldrepenger.behandlingslager.testutilities.behandling.ScenarioMorSøkerForeldrepenger;
import no.nav.foreldrepenger.behandlingslager.virksomhet.Arbeidsgiver;
import no.nav.foreldrepenger.dbstoette.UnittestRepositoryRule;
import no.nav.foreldrepenger.domene.arbeidsforhold.InntektArbeidYtelseTjeneste;
import no.nav.foreldrepenger.domene.iay.modell.InntektArbeidYtelseGrunnlag;
import no.nav.foreldrepenger.domene.iay.modell.Inntektsmelding;
import no.nav.foreldrepenger.domene.iay.modell.InntektsmeldingAggregat;
import no.nav.foreldrepenger.domene.iay.modell.InntektsmeldingBuilder;
import no.nav.foreldrepenger.domene.iay.modell.NaturalYtelse;
import no.nav.foreldrepenger.domene.iay.modell.Refusjon;
import no.nav.foreldrepenger.domene.iay.modell.kodeverk.InntektsmeldingInnsendingsårsak;
import no.nav.foreldrepenger.domene.typer.InternArbeidsforholdRef;

public class StartpunktUtlederInntektsmeldingTest {

    private static final BigDecimal INNTEKTBELØP_DEFAULT = new BigDecimal(30000);
    private static final InternArbeidsforholdRef ARBEIDSID_DEFAULT = InternArbeidsforholdRef.nyRef();
    private static final InternArbeidsforholdRef ARBEIDSID_EKSTRA = InternArbeidsforholdRef.nyRef();

    @Rule
    public final UnittestRepositoryRule repoRule = new UnittestRepositoryRule();

    private BehandlingRepositoryProvider repositoryProvider = new BehandlingRepositoryProvider(repoRule.getEntityManager());

    private BehandlingRepository behandlingRepository = repositoryProvider.getBehandlingRepository();

    @Mock
    private InntektArbeidYtelseTjeneste inntektArbeidYtelseTjeneste;

    @Mock
    InntektsmeldingAggregat førstegangsbehandlingIMAggregat;
    @Mock
    InntektsmeldingAggregat revurderingIMAggregat;

    @Mock
    InntektArbeidYtelseGrunnlag førstegangsbehandlingGrunnlagIAY;

    @Mock
    InntektArbeidYtelseGrunnlag revurderingGrunnlagIAY;

    private StartpunktUtlederInntektsmelding utleder;

    @Before
    public void oppsett() {
        initMocks(this);
        when(førstegangsbehandlingGrunnlagIAY.getInntektsmeldinger()).thenReturn(Optional.of(førstegangsbehandlingIMAggregat));
        when(revurderingGrunnlagIAY.getInntektsmeldinger()).thenReturn(Optional.of(revurderingIMAggregat));
        utleder = new StartpunktUtlederInntektsmelding(inntektArbeidYtelseTjeneste);
    }

    @Test
    public void skal_ikke_returnere_inngangsvilkår_dersom_endring_på_første_permisjonsdag_mellom_ny_IM_og_vedtaksgrunnlaget() {
        // Arrange - opprette avsluttet førstegangsbehandling
        Behandling førstegangsbehandling = opprettFørstegangsbehandling();

        LocalDate førsteUttaksdato = LocalDate.now();

        // Arrange - opprette revurderingsbehandling
        Behandling revurdering = opprettRevurdering(førstegangsbehandling);

        LocalDate endretUttaksdato = førsteUttaksdato.plusWeeks(1);
        List<Inntektsmelding> revurderingIM = lagInntektsmelding(InntektsmeldingInnsendingsårsak.NY, INNTEKTBELØP_DEFAULT, endretUttaksdato,
                ARBEIDSID_DEFAULT);
        when(inntektArbeidYtelseTjeneste.finnGrunnlag(revurdering.getId())).thenReturn(Optional.of(revurderingGrunnlagIAY));
        BehandlingReferanse ref = lagReferanse(revurdering, førsteUttaksdato);

        when(førstegangsbehandlingIMAggregat.getInntektsmeldingerSomSkalBrukes()).thenReturn(Collections.emptyList());
        when(revurderingIMAggregat.getInntektsmeldingerSomSkalBrukes()).thenReturn(revurderingIM);

        // Act/Assert
        assertThat(utledStartpunkt(ref)).isEqualTo(StartpunktType.BEREGNING);
    }

    private StartpunktType utledStartpunkt(BehandlingReferanse ref) {
        return utleder.utledStartpunkt(ref, førstegangsbehandlingGrunnlagIAY, revurderingGrunnlagIAY);
    }

    @Test
    public void skal_returnere_beregning_dersom_innsendingsårsak_er_ny() {
        // Arrange - opprette avsluttet førstegangsbehandling
        Behandling førstegangsbehandling = opprettFørstegangsbehandling();

        LocalDate førsteUttaksdato = LocalDate.now();

        // Arrange - opprette revurderingsbehandling
        Behandling revurdering = opprettRevurdering(førstegangsbehandling);

        List<Inntektsmelding> inntektsmeldingerMottattEtterVedtak = lagInntektsmelding(InntektsmeldingInnsendingsårsak.NY, INNTEKTBELØP_DEFAULT,
                førsteUttaksdato, ARBEIDSID_DEFAULT);
        BehandlingReferanse ref = lagReferanse(revurdering, førsteUttaksdato);
        when(inntektArbeidYtelseTjeneste.finnGrunnlag(revurdering.getId())).thenReturn(Optional.of(revurderingGrunnlagIAY));
        when(førstegangsbehandlingIMAggregat.getInntektsmeldingerSomSkalBrukes()).thenReturn(Collections.emptyList());
        when(revurderingIMAggregat.getInntektsmeldingerSomSkalBrukes()).thenReturn(inntektsmeldingerMottattEtterVedtak);

        // Act/Assert
        assertThat(utledStartpunkt(ref)).isEqualTo(StartpunktType.BEREGNING);
    }

    @Test
    public void skal_returnere_beregning_dersom_det_er_endring_i_refusjonsendring() {
        // Arrange - opprette avsluttet førstegangsbehandling
        Behandling behandling = opprettFørstegangsbehandling();

        LocalDate førsteUttaksdato = LocalDate.now();

        BigDecimal førstegangsbehandlingInntekt = new BigDecimal(30000);
        List<Inntektsmelding> førstegangsbehandlingIM = lagInntektsmelding(InntektsmeldingInnsendingsårsak.NY, førstegangsbehandlingInntekt,
                førsteUttaksdato, ARBEIDSID_DEFAULT);
        when(førstegangsbehandlingIMAggregat.getInntektsmeldingerSomSkalBrukes()).thenReturn(førstegangsbehandlingIM);

        // Arrange - opprette revurderingsbehandling
        Behandling revurdering = opprettRevurdering(behandling);

        BigDecimal revurderingInntekt = new BigDecimal(30000);
        List<Inntektsmelding> inntektsmeldingerMottattEtterVedtak = lagInntektsmeldingMedEndringIRefusjon(InntektsmeldingInnsendingsårsak.ENDRING,
                revurderingInntekt, førsteUttaksdato, ARBEIDSID_DEFAULT, førsteUttaksdato.plusWeeks(2));
        BehandlingReferanse ref = lagReferanse(revurdering, førsteUttaksdato);
        when(inntektArbeidYtelseTjeneste.finnGrunnlag(behandling.getId())).thenReturn(Optional.of(førstegangsbehandlingGrunnlagIAY));
        when(inntektArbeidYtelseTjeneste.finnGrunnlag(revurdering.getId())).thenReturn(Optional.of(revurderingGrunnlagIAY));
        when(revurderingIMAggregat.getInntektsmeldingerSomSkalBrukes()).thenReturn(inntektsmeldingerMottattEtterVedtak);

        // Act/Assert
        assertThat(utledStartpunkt(ref)).isEqualTo(StartpunktType.BEREGNING);
    }

    @Test
    public void skal_returnere_beregning_dersom_endring_på_inntekt_mellom_ny_IM_og_grunnlag_IM() {
        // Arrange - opprette avsluttet førstegangsbehandling
        Behandling behandling = opprettFørstegangsbehandling();

        LocalDate førsteUttaksdato = LocalDate.now();

        BigDecimal førstegangsbehandlingInntekt = new BigDecimal(30000);
        List<Inntektsmelding> førstegangsbehandlingIM = lagInntektsmelding(InntektsmeldingInnsendingsårsak.NY, førstegangsbehandlingInntekt,
                førsteUttaksdato, ARBEIDSID_DEFAULT);
        when(førstegangsbehandlingIMAggregat.getInntektsmeldingerSomSkalBrukes()).thenReturn(førstegangsbehandlingIM);

        // Arrange - opprette revurderingsbehandling
        Behandling revurdering = opprettRevurdering(behandling);

        BigDecimal revurderingInntekt = førstegangsbehandlingInntekt.add(new BigDecimal(1000));
        List<Inntektsmelding> inntektsmeldingerMottattEtterVedtak = lagInntektsmelding(InntektsmeldingInnsendingsårsak.ENDRING, revurderingInntekt,
                førsteUttaksdato, ARBEIDSID_DEFAULT);
        BehandlingReferanse ref = lagReferanse(revurdering, førsteUttaksdato);
        when(inntektArbeidYtelseTjeneste.finnGrunnlag(behandling.getId())).thenReturn(Optional.of(førstegangsbehandlingGrunnlagIAY));
        when(inntektArbeidYtelseTjeneste.finnGrunnlag(revurdering.getId())).thenReturn(Optional.of(revurderingGrunnlagIAY));
        when(revurderingIMAggregat.getInntektsmeldingerSomSkalBrukes()).thenReturn(inntektsmeldingerMottattEtterVedtak);

        // Act/Assert
        assertThat(utledStartpunkt(ref)).isEqualTo(StartpunktType.BEREGNING);
    }

    @Test
    public void skal_returnere_beregning_dersom_endring_på_inntekt_mellom_ny_IM_og_endret_IM() {
        // Arrange - opprette avsluttet førstegangsbehandling
        Behandling behandling = opprettFørstegangsbehandling();

        LocalDate førsteUttaksdato = LocalDate.now();

        BigDecimal førstegangsbehandlingInntekt = new BigDecimal(30000);
        BigDecimal revurderingInntekt = førstegangsbehandlingInntekt.add(new BigDecimal(1000));

        List<Inntektsmelding> førstegangsbehandlingIM = lagInntektsmelding(InntektsmeldingInnsendingsårsak.ENDRING, revurderingInntekt,
                førsteUttaksdato, ARBEIDSID_DEFAULT);
        lagEkstraInntektsmelding(InntektsmeldingInnsendingsårsak.NY, førstegangsbehandlingInntekt, førsteUttaksdato, ARBEIDSID_EKSTRA,
                førstegangsbehandlingIM);
        when(førstegangsbehandlingIMAggregat.getInntektsmeldingerSomSkalBrukes()).thenReturn(førstegangsbehandlingIM);

        // Arrange - opprette revurderingsbehandling
        Behandling revurdering = opprettRevurdering(behandling);

        List<Inntektsmelding> inntektsmeldingerMottattEtterVedtak = lagInntektsmelding(InntektsmeldingInnsendingsårsak.ENDRING, revurderingInntekt,
                førsteUttaksdato, ARBEIDSID_DEFAULT);
        lagEkstraInntektsmelding(InntektsmeldingInnsendingsårsak.ENDRING, revurderingInntekt, førsteUttaksdato, ARBEIDSID_EKSTRA,
                inntektsmeldingerMottattEtterVedtak);
        BehandlingReferanse ref = lagReferanse(revurdering, førsteUttaksdato);
        when(inntektArbeidYtelseTjeneste.finnGrunnlag(behandling.getId())).thenReturn(Optional.of(førstegangsbehandlingGrunnlagIAY));
        when(inntektArbeidYtelseTjeneste.finnGrunnlag(revurdering.getId())).thenReturn(Optional.of(revurderingGrunnlagIAY));
        when(revurderingIMAggregat.getInntektsmeldingerSomSkalBrukes()).thenReturn(inntektsmeldingerMottattEtterVedtak);

        // Act/Assert
        assertThat(utledStartpunkt(ref)).isEqualTo(StartpunktType.BEREGNING);
    }

    @Test
    public void skal_returnere_beregning_dersom_endring_på_natural_ytelser_mellom_ny_IM_og_grunnlag_IM() {
        // Arrange - opprette avsluttet førstegangsbehandling
        Behandling behandling = opprettFørstegangsbehandling();

        LocalDate førsteUttaksdato = LocalDate.now();

        List<Inntektsmelding> førstegangsbehandlingIM = lagInntektsmelding(InntektsmeldingInnsendingsårsak.NY, INNTEKTBELØP_DEFAULT, førsteUttaksdato,
                ARBEIDSID_DEFAULT);
        when(førstegangsbehandlingIMAggregat.getInntektsmeldingerSomSkalBrukes()).thenReturn(førstegangsbehandlingIM);

        // Arrange - opprette revurderingsbehandling
        Behandling revurdering = opprettRevurdering(behandling);

        List<Inntektsmelding> inntektsmeldingerMottattEtterVedtak = lagInntektsmeldingMedNaturalytelse(InntektsmeldingInnsendingsårsak.ENDRING,
                INNTEKTBELØP_DEFAULT, førsteUttaksdato, ARBEIDSID_DEFAULT,
                new NaturalYtelse(LocalDate.now(), LocalDate.now().plusDays(1), new BigDecimal(30), null));
        BehandlingReferanse ref = lagReferanse(revurdering, førsteUttaksdato);
        when(inntektArbeidYtelseTjeneste.finnGrunnlag(behandling.getId())).thenReturn(Optional.of(førstegangsbehandlingGrunnlagIAY));
        when(inntektArbeidYtelseTjeneste.finnGrunnlag(revurdering.getId())).thenReturn(Optional.of(revurderingGrunnlagIAY));
        when(revurderingIMAggregat.getInntektsmeldingerSomSkalBrukes()).thenReturn(inntektsmeldingerMottattEtterVedtak);

        // Act/Assert
        assertThat(utledStartpunkt(ref)).isEqualTo(StartpunktType.BEREGNING);
    }

    @Test
    public void skal_returnere_uttak_dersom_ingen_endring_i_permisjonsdag_inntekt_naturytelser_eller_refusjon() {
        // Arrange - opprette avsluttet førstegangsbehandling
        Behandling behandling = opprettFørstegangsbehandling();

        LocalDate førsteUttaksdato = LocalDate.now();

        List<Inntektsmelding> førstegangsbehandlingIM = lagInntektsmelding(InntektsmeldingInnsendingsårsak.NY, INNTEKTBELØP_DEFAULT, førsteUttaksdato,
                ARBEIDSID_DEFAULT);
        when(førstegangsbehandlingIMAggregat.getInntektsmeldingerSomSkalBrukes()).thenReturn(førstegangsbehandlingIM);

        // Arrange - opprette revurderingsbehandling
        Behandling revurdering = opprettRevurdering(behandling);

        List<Inntektsmelding> inntektsmeldingerMottattEtterVedtak = lagInntektsmelding(InntektsmeldingInnsendingsårsak.ENDRING, INNTEKTBELØP_DEFAULT,
                førsteUttaksdato, ARBEIDSID_DEFAULT);
        BehandlingReferanse ref = lagReferanse(revurdering, førsteUttaksdato);
        when(inntektArbeidYtelseTjeneste.finnGrunnlag(behandling.getId())).thenReturn(Optional.of(førstegangsbehandlingGrunnlagIAY));
        when(inntektArbeidYtelseTjeneste.finnGrunnlag(revurdering.getId())).thenReturn(Optional.of(revurderingGrunnlagIAY));
        when(revurderingIMAggregat.getInntektsmeldingerSomSkalBrukes()).thenReturn(inntektsmeldingerMottattEtterVedtak);

        // Act/Assert
        assertThat(utledStartpunkt(ref)).isEqualTo(StartpunktType.UTTAKSVILKÅR);
    }

    private Behandling opprettRevurdering(Behandling førstegangsbehandling) {
        ScenarioMorSøkerForeldrepenger revurderingScenario = ScenarioMorSøkerForeldrepenger.forFødsel()
                .medBehandlingType(BehandlingType.REVURDERING)
                .medOriginalBehandling(førstegangsbehandling, BehandlingÅrsakType.UDEFINERT);
        return revurderingScenario.lagre(repositoryProvider);
    }

    private Behandling opprettFørstegangsbehandling() {
        ScenarioMorSøkerForeldrepenger førstegangScenario = ScenarioMorSøkerForeldrepenger.forFødsel();
        Behandling førstegangsbehandling = førstegangScenario.lagre(repositoryProvider);
        førstegangsbehandling.avsluttBehandling();
        BehandlingLås behandlingLås = behandlingRepository.taSkriveLås(førstegangsbehandling);
        behandlingRepository.lagre(førstegangsbehandling, behandlingLås);
        return førstegangsbehandling;
    }

    private List<Inntektsmelding> lagInntektsmelding(InntektsmeldingInnsendingsårsak innsendingsårsak, BigDecimal beløp, LocalDate førsteUttaksdato,
            InternArbeidsforholdRef arbeidID) {
        List<Inntektsmelding> inntektsmeldingerGrunnlag = new ArrayList<>();
        Inntektsmelding inntektsmelding = getInntektsmeldingBuilder()
                .medBeløp(beløp)
                .medArbeidsforholdId(arbeidID)
                .medStartDatoPermisjon(førsteUttaksdato)
                .medJournalpostId(InntektsmeldingInnsendingsårsak.NY.equals(innsendingsårsak) ? lagArbeidsgiver().getIdentifikator()
                        : lagArbeidsgiver().getIdentifikator() + "E")
                .medArbeidsgiver(lagArbeidsgiver())
                .medInntektsmeldingaarsak(innsendingsårsak)
                .build();

        inntektsmeldingerGrunnlag.add(inntektsmelding);
        return inntektsmeldingerGrunnlag;
    }

    private List<Inntektsmelding> lagInntektsmeldingMedEndringIRefusjon(InntektsmeldingInnsendingsårsak innsendingsårsak, BigDecimal beløp,
            LocalDate førsteUttaksdato, InternArbeidsforholdRef arbeidID, LocalDate refusjonsEndringFra) {
        List<Inntektsmelding> inntektsmeldingerGrunnlag = new ArrayList<>();
        Inntektsmelding inntektsmelding = getInntektsmeldingBuilder()
                .medBeløp(beløp)
                .medArbeidsforholdId(arbeidID)
                .medJournalpostId(InntektsmeldingInnsendingsårsak.NY.equals(innsendingsårsak) ? lagArbeidsgiver().getIdentifikator()
                        : lagArbeidsgiver().getIdentifikator() + "E")
                .medStartDatoPermisjon(førsteUttaksdato)
                .medArbeidsgiver(lagArbeidsgiver())
                .medInntektsmeldingaarsak(innsendingsårsak)
                .leggTil(new Refusjon(BigDecimal.valueOf(10000), refusjonsEndringFra))
                .build();

        inntektsmeldingerGrunnlag.add(inntektsmelding);
        return inntektsmeldingerGrunnlag;
    }

    private void lagEkstraInntektsmelding(InntektsmeldingInnsendingsårsak innsendingsårsak, BigDecimal beløp, LocalDate førsteUttaksdato,
            InternArbeidsforholdRef arbeidID, List<Inntektsmelding> im) {
        Inntektsmelding inntektsmelding = getInntektsmeldingBuilder()
                .medBeløp(beløp)
                .medArbeidsforholdId(arbeidID)
                .medStartDatoPermisjon(førsteUttaksdato)
                .medArbeidsgiver(Arbeidsgiver.virksomhet("456"))
                .medJournalpostId(InntektsmeldingInnsendingsårsak.NY.equals(innsendingsårsak) ? "456" : "456E")
                .medInntektsmeldingaarsak(innsendingsårsak)
                .build();

        im.add(inntektsmelding);
    }

    private Arbeidsgiver lagArbeidsgiver() {
        return Arbeidsgiver.virksomhet("123");
    }

    private InntektsmeldingBuilder getInntektsmeldingBuilder() {
        return InntektsmeldingBuilder.builder().medInnsendingstidspunkt(LocalDateTime.now());
    }

    private List<Inntektsmelding> lagInntektsmeldingMedNaturalytelse(InntektsmeldingInnsendingsårsak innsendingsårsak, BigDecimal beløp,
            LocalDate førsteUttaksdato, InternArbeidsforholdRef arbeidID, NaturalYtelse naturalYtelse) {
        List<Inntektsmelding> inntektsmeldingerGrunnlag = new ArrayList<>();
        Inntektsmelding inntektsmelding = getInntektsmeldingBuilder()
                .medBeløp(beløp)
                .medArbeidsforholdId(arbeidID)
                .medArbeidsgiver(lagArbeidsgiver())
                .medStartDatoPermisjon(førsteUttaksdato)
                .medInntektsmeldingaarsak(innsendingsårsak)
                .leggTil(naturalYtelse)
                .build();

        inntektsmeldingerGrunnlag.add(inntektsmelding);
        return inntektsmeldingerGrunnlag;
    }

    private BehandlingReferanse lagReferanse(Behandling behandling, LocalDate førsteUttaksdato) {
        Skjæringstidspunkt skjæringstidspunkt = Skjæringstidspunkt.builder().medFørsteUttaksdato(førsteUttaksdato)
                .medUtledetSkjæringstidspunkt(førsteUttaksdato).build();
        return BehandlingReferanse.fra(behandling, skjæringstidspunkt);
    }
}
