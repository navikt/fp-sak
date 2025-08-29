package no.nav.foreldrepenger.domene.registerinnhenting.impl.startpunkt;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.lenient;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import no.nav.foreldrepenger.behandling.Skjæringstidspunkt;
import no.nav.foreldrepenger.domene.prosess.BeregningTjeneste;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import no.nav.foreldrepenger.behandling.BehandlingReferanse;
import no.nav.foreldrepenger.behandlingslager.behandling.Behandling;
import no.nav.foreldrepenger.behandlingslager.behandling.BehandlingType;
import no.nav.foreldrepenger.behandlingslager.behandling.BehandlingÅrsakType;
import no.nav.foreldrepenger.behandlingslager.behandling.repository.BehandlingRepository;
import no.nav.foreldrepenger.behandlingslager.behandling.repository.BehandlingRepositoryProvider;
import no.nav.foreldrepenger.behandlingslager.hendelser.StartpunktType;
import no.nav.foreldrepenger.behandlingslager.testutilities.behandling.ScenarioMorSøkerForeldrepenger;
import no.nav.foreldrepenger.behandlingslager.testutilities.behandling.ScenarioMorSøkerSvangerskapspenger;
import no.nav.foreldrepenger.behandlingslager.virksomhet.ArbeidType;
import no.nav.foreldrepenger.behandlingslager.virksomhet.Arbeidsgiver;
import no.nav.foreldrepenger.dbstoette.EntityManagerAwareTest;
import no.nav.foreldrepenger.domene.arbeidInntektsmelding.ArbeidsforholdInntektsmeldingMangelTjeneste;
import no.nav.foreldrepenger.domene.arbeidsforhold.InntektArbeidYtelseTjeneste;
import no.nav.foreldrepenger.domene.iay.modell.InntektArbeidYtelseAggregatBuilder;
import no.nav.foreldrepenger.domene.iay.modell.InntektArbeidYtelseGrunnlag;
import no.nav.foreldrepenger.domene.iay.modell.Inntektsmelding;
import no.nav.foreldrepenger.domene.iay.modell.InntektsmeldingAggregat;
import no.nav.foreldrepenger.domene.iay.modell.InntektsmeldingBuilder;
import no.nav.foreldrepenger.domene.iay.modell.NaturalYtelse;
import no.nav.foreldrepenger.domene.iay.modell.Refusjon;
import no.nav.foreldrepenger.domene.iay.modell.YrkesaktivitetBuilder;
import no.nav.foreldrepenger.domene.iay.modell.kodeverk.InntektsmeldingInnsendingsårsak;
import no.nav.foreldrepenger.domene.typer.AktørId;
import no.nav.foreldrepenger.domene.typer.InternArbeidsforholdRef;

@ExtendWith(MockitoExtension.class)
class StartpunktUtlederInntektsmeldingTest extends EntityManagerAwareTest {

    private static final BigDecimal INNTEKTBELØP_DEFAULT = new BigDecimal(30000);
    private static final InternArbeidsforholdRef ARBEIDSID_DEFAULT = InternArbeidsforholdRef.nyRef();
    private static final InternArbeidsforholdRef ARBEIDSID_EKSTRA = InternArbeidsforholdRef.nyRef();

    private BehandlingRepositoryProvider repositoryProvider;

    private BehandlingRepository behandlingRepository;

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

    @Mock
    ArbeidsforholdInntektsmeldingMangelTjeneste arbeidsforholdInntektsmeldingMangelTjeneste;

    @Mock
    BeregningTjeneste beregningTjeneste;

    private StartpunktUtlederInntektsmelding utleder;

    @BeforeEach
    void oppsett() {
        var entityManager = getEntityManager();
        repositoryProvider = new BehandlingRepositoryProvider(entityManager);
        behandlingRepository = new BehandlingRepository(entityManager);
        lenient().when(førstegangsbehandlingGrunnlagIAY.getInntektsmeldinger()).thenReturn(Optional.of(førstegangsbehandlingIMAggregat));
        lenient().when(revurderingGrunnlagIAY.getInntektsmeldinger()).thenReturn(Optional.of(revurderingIMAggregat));
        utleder = new StartpunktUtlederInntektsmelding(inntektArbeidYtelseTjeneste, arbeidsforholdInntektsmeldingMangelTjeneste, beregningTjeneste);
    }

    @Test
    void skal_ikke_returnere_inngangsvilkår_dersom_endring_på_første_permisjonsdag_mellom_ny_IM_og_vedtaksgrunnlaget() {
        // Arrange - opprette avsluttet førstegangsbehandling
        var førstegangsbehandling = opprettFørstegangsbehandling();

        var førsteUttaksdato = LocalDate.now();

        // Arrange - opprette revurderingsbehandling
        var revurdering = opprettRevurdering(førstegangsbehandling);

        var endretUttaksdato = førsteUttaksdato.plusWeeks(1);
        var revurderingIM = lagInntektsmelding(InntektsmeldingInnsendingsårsak.NY, INNTEKTBELØP_DEFAULT, endretUttaksdato,
                ARBEIDSID_DEFAULT);
        lenient().when(inntektArbeidYtelseTjeneste.finnGrunnlag(revurdering.getId())).thenReturn(Optional.of(revurderingGrunnlagIAY));
        var ref = lagReferanse(revurdering);

        lenient().when(førstegangsbehandlingIMAggregat.getInntektsmeldingerSomSkalBrukes()).thenReturn(Collections.emptyList());
        lenient().when(revurderingIMAggregat.getInntektsmeldingerSomSkalBrukes()).thenReturn(revurderingIM);

        // Act/Assert
        assertThat(utledStartpunkt(ref)).isEqualTo(StartpunktType.BEREGNING);
    }

    private StartpunktType utledStartpunkt(BehandlingReferanse ref) {
        var stp = Skjæringstidspunkt.builder().medUtledetSkjæringstidspunkt(LocalDate.now()).build();
        return utleder.utledStartpunkt(ref, stp, førstegangsbehandlingGrunnlagIAY, revurderingGrunnlagIAY);
    }

    @Test
    void skal_returnere_beregning_dersom_innsendingsårsak_er_ny() {
        // Arrange - opprette avsluttet førstegangsbehandling
        var førstegangsbehandling = opprettFørstegangsbehandling();

        var førsteUttaksdato = LocalDate.now();

        // Arrange - opprette revurderingsbehandling
        var revurdering = opprettRevurdering(førstegangsbehandling);

        var inntektsmeldingerMottattEtterVedtak = lagInntektsmelding(InntektsmeldingInnsendingsårsak.NY, INNTEKTBELØP_DEFAULT,
                førsteUttaksdato, ARBEIDSID_DEFAULT);
        var ref = lagReferanse(revurdering);
        lenient().when(inntektArbeidYtelseTjeneste.finnGrunnlag(revurdering.getId())).thenReturn(Optional.of(revurderingGrunnlagIAY));
        lenient().when(førstegangsbehandlingIMAggregat.getInntektsmeldingerSomSkalBrukes()).thenReturn(Collections.emptyList());
        lenient().when(revurderingIMAggregat.getInntektsmeldingerSomSkalBrukes()).thenReturn(inntektsmeldingerMottattEtterVedtak);

        // Act/Assert
        assertThat(utledStartpunkt(ref)).isEqualTo(StartpunktType.BEREGNING);
    }

    @Test
    void skal_returnere_beregning_dersom_endring_arbeidsforholdRef() {
        // Arrange - opprette avsluttet førstegangsbehandling
        var behandling = opprettFørstegangsbehandling();

        var førsteUttaksdato = LocalDate.now();

        var førstegangsbehandlingInntekt = new BigDecimal(30000);
        var førstegangsbehandlingIM = lagInntektsmelding(InntektsmeldingInnsendingsårsak.NY, førstegangsbehandlingInntekt,
            førsteUttaksdato, ARBEIDSID_DEFAULT);
        lenient().when(førstegangsbehandlingIMAggregat.getInntektsmeldingerSomSkalBrukes()).thenReturn(førstegangsbehandlingIM);

        // Arrange - opprette revurderingsbehandling
        var revurdering = opprettRevurdering(behandling);

        var inntektsmeldingerMottattEtterVedtak = lagInntektsmelding(InntektsmeldingInnsendingsårsak.NY, førstegangsbehandlingInntekt,
            førsteUttaksdato, InternArbeidsforholdRef.nullRef());
        var ref = lagReferanse(revurdering);
        lenient().when(inntektArbeidYtelseTjeneste.finnGrunnlag(behandling.getId())).thenReturn(Optional.of(førstegangsbehandlingGrunnlagIAY));
        lenient().when(inntektArbeidYtelseTjeneste.finnGrunnlag(revurdering.getId())).thenReturn(Optional.of(revurderingGrunnlagIAY));
        lenient().when(revurderingIMAggregat.getInntektsmeldingerSomSkalBrukes()).thenReturn(inntektsmeldingerMottattEtterVedtak);
        var arbeid = InntektArbeidYtelseAggregatBuilder.AktørArbeidBuilder.oppdatere(Optional.empty())
            .medAktørId(AktørId.dummy())
            .leggTilYrkesaktivitet(YrkesaktivitetBuilder.oppdatere(Optional.empty()).medArbeidsgiver(lagArbeidsgiver())
                .medArbeidsforholdId(ARBEIDSID_DEFAULT).medArbeidType(ArbeidType.ORDINÆRT_ARBEIDSFORHOLD))
            .leggTilYrkesaktivitet(YrkesaktivitetBuilder.oppdatere(Optional.empty()).medArbeidsgiver(lagArbeidsgiver())
                .medArbeidsforholdId(ARBEIDSID_EKSTRA).medArbeidType(ArbeidType.ORDINÆRT_ARBEIDSFORHOLD));
        lenient().when(revurderingGrunnlagIAY.getAktørArbeidFraRegister(any())).thenReturn(Optional.of(arbeid.build()));

        // Act/Assert
        assertThat(utledStartpunkt(ref)).isEqualTo(StartpunktType.BEREGNING);
    }

    @Test
    void skal_ikke_returnere_beregning_dersom_ingen_endring_arbeidsforholdRef() {
        // Arrange - opprette avsluttet førstegangsbehandling
        var behandling = opprettFørstegangsbehandling();

        var førsteUttaksdato = LocalDate.now();

        var førstegangsbehandlingInntekt = new BigDecimal(30000);
        var førstegangsbehandlingIM = lagInntektsmelding(InntektsmeldingInnsendingsårsak.NY, førstegangsbehandlingInntekt,
            førsteUttaksdato, ARBEIDSID_DEFAULT);
        lenient().when(førstegangsbehandlingIMAggregat.getInntektsmeldingerSomSkalBrukes()).thenReturn(førstegangsbehandlingIM);

        // Arrange - opprette revurderingsbehandling
        var revurdering = opprettRevurdering(behandling);

        var inntektsmeldingerMottattEtterVedtak = lagInntektsmelding(InntektsmeldingInnsendingsårsak.NY, førstegangsbehandlingInntekt,
            førsteUttaksdato, ARBEIDSID_DEFAULT);
        var ref = lagReferanse(revurdering);
        lenient().when(inntektArbeidYtelseTjeneste.finnGrunnlag(behandling.getId())).thenReturn(Optional.of(førstegangsbehandlingGrunnlagIAY));
        lenient().when(inntektArbeidYtelseTjeneste.finnGrunnlag(revurdering.getId())).thenReturn(Optional.of(revurderingGrunnlagIAY));
        lenient().when(revurderingIMAggregat.getInntektsmeldingerSomSkalBrukes()).thenReturn(inntektsmeldingerMottattEtterVedtak);

        // Act/Assert
        assertThat(utledStartpunkt(ref)).isEqualTo(StartpunktType.UDEFINERT);
    }

    @Test
    void skal_ikke_returnere_beregning_dersom_ingen_endring_arbeidsforholdNullRef() {
        // Arrange - opprette avsluttet førstegangsbehandling
        var behandling = opprettFørstegangsbehandling();

        var førsteUttaksdato = LocalDate.now();

        var førstegangsbehandlingInntekt = new BigDecimal(30000);
        var førstegangsbehandlingIM = lagInntektsmelding(InntektsmeldingInnsendingsårsak.NY, førstegangsbehandlingInntekt,
            førsteUttaksdato, InternArbeidsforholdRef.nullRef());
        lenient().when(førstegangsbehandlingIMAggregat.getInntektsmeldingerSomSkalBrukes()).thenReturn(førstegangsbehandlingIM);

        // Arrange - opprette revurderingsbehandling
        var revurdering = opprettRevurdering(behandling);

        var inntektsmeldingerMottattEtterVedtak = lagInntektsmelding(InntektsmeldingInnsendingsårsak.NY, førstegangsbehandlingInntekt,
            førsteUttaksdato, InternArbeidsforholdRef.nullRef());
        var ref = lagReferanse(revurdering);
        lenient().when(inntektArbeidYtelseTjeneste.finnGrunnlag(behandling.getId())).thenReturn(Optional.of(førstegangsbehandlingGrunnlagIAY));
        lenient().when(inntektArbeidYtelseTjeneste.finnGrunnlag(revurdering.getId())).thenReturn(Optional.of(revurderingGrunnlagIAY));
        lenient().when(revurderingIMAggregat.getInntektsmeldingerSomSkalBrukes()).thenReturn(inntektsmeldingerMottattEtterVedtak);

        // Act/Assert
        assertThat(utledStartpunkt(ref)).isEqualTo(StartpunktType.UDEFINERT);
    }

    @Test
    void skal_returnere_beregning_dersom_det_er_endring_i_refusjonsendring() {
        // Arrange - opprette avsluttet førstegangsbehandling
        var behandling = opprettFørstegangsbehandling();

        var førsteUttaksdato = LocalDate.now();

        var førstegangsbehandlingInntekt = new BigDecimal(30000);
        var førstegangsbehandlingIM = lagInntektsmelding(InntektsmeldingInnsendingsårsak.NY, førstegangsbehandlingInntekt,
                førsteUttaksdato, ARBEIDSID_DEFAULT);
        lenient().when(førstegangsbehandlingIMAggregat.getInntektsmeldingerSomSkalBrukes()).thenReturn(førstegangsbehandlingIM);

        // Arrange - opprette revurderingsbehandling
        var revurdering = opprettRevurdering(behandling);

        var revurderingInntekt = new BigDecimal(30000);
        var inntektsmeldingerMottattEtterVedtak = lagInntektsmeldingMedEndringIRefusjon(InntektsmeldingInnsendingsårsak.ENDRING,
                revurderingInntekt, førsteUttaksdato, ARBEIDSID_DEFAULT, førsteUttaksdato.plusWeeks(2));
        var ref = lagReferanse(revurdering);
        lenient().when(inntektArbeidYtelseTjeneste.finnGrunnlag(behandling.getId())).thenReturn(Optional.of(førstegangsbehandlingGrunnlagIAY));
        lenient().when(inntektArbeidYtelseTjeneste.finnGrunnlag(revurdering.getId())).thenReturn(Optional.of(revurderingGrunnlagIAY));
        lenient().when(revurderingIMAggregat.getInntektsmeldingerSomSkalBrukes()).thenReturn(inntektsmeldingerMottattEtterVedtak);

        // Act/Assert
        assertThat(utledStartpunkt(ref)).isEqualTo(StartpunktType.BEREGNING);
    }

    @Test
    void skal_returnere_beregning_dersom_endring_på_inntekt_mellom_ny_IM_og_grunnlag_IM() {
        // Arrange - opprette avsluttet førstegangsbehandling
        var behandling = opprettFørstegangsbehandling();

        var førsteUttaksdato = LocalDate.now();

        var førstegangsbehandlingInntekt = new BigDecimal(30000);
        var førstegangsbehandlingIM = lagInntektsmelding(InntektsmeldingInnsendingsårsak.NY, førstegangsbehandlingInntekt,
                førsteUttaksdato, ARBEIDSID_DEFAULT);
        lenient().when(førstegangsbehandlingIMAggregat.getInntektsmeldingerSomSkalBrukes()).thenReturn(førstegangsbehandlingIM);

        // Arrange - opprette revurderingsbehandling
        var revurdering = opprettRevurdering(behandling);

        var revurderingInntekt = førstegangsbehandlingInntekt.add(new BigDecimal(1000));
        var inntektsmeldingerMottattEtterVedtak = lagInntektsmelding(InntektsmeldingInnsendingsårsak.ENDRING, revurderingInntekt,
                førsteUttaksdato, ARBEIDSID_DEFAULT);
        var ref = lagReferanse(revurdering);
        lenient().when(inntektArbeidYtelseTjeneste.finnGrunnlag(behandling.getId())).thenReturn(Optional.of(førstegangsbehandlingGrunnlagIAY));
        lenient().when(inntektArbeidYtelseTjeneste.finnGrunnlag(revurdering.getId())).thenReturn(Optional.of(revurderingGrunnlagIAY));
        lenient().when(revurderingIMAggregat.getInntektsmeldingerSomSkalBrukes()).thenReturn(inntektsmeldingerMottattEtterVedtak);

        // Act/Assert
        assertThat(utledStartpunkt(ref)).isEqualTo(StartpunktType.BEREGNING);
    }

    @Test
    void skal_returnere_beregning_dersom_endring_på_inntekt_mellom_ny_IM_og_endret_IM() {
        // Arrange - opprette avsluttet førstegangsbehandling
        var behandling = opprettFørstegangsbehandling();

        var ekstraArbeidsgiver = Arbeidsgiver.virksomhet("456");

        var førsteUttaksdato = LocalDate.now();

        var førstegangsbehandlingInntekt = new BigDecimal(30000);
        var revurderingInntekt = førstegangsbehandlingInntekt.add(new BigDecimal(1000));

        var førstegangsbehandlingIM = lagInntektsmelding(InntektsmeldingInnsendingsårsak.ENDRING, revurderingInntekt,
                førsteUttaksdato, ARBEIDSID_DEFAULT);
        lagEkstraInntektsmelding(InntektsmeldingInnsendingsårsak.NY, førstegangsbehandlingInntekt, førsteUttaksdato, ekstraArbeidsgiver,
            ARBEIDSID_EKSTRA,
                førstegangsbehandlingIM);
        lenient().when(førstegangsbehandlingIMAggregat.getInntektsmeldingerSomSkalBrukes()).thenReturn(førstegangsbehandlingIM);

        // Arrange - opprette revurderingsbehandling
        var revurdering = opprettRevurdering(behandling);

        var inntektsmeldingerMottattEtterVedtak = lagInntektsmelding(InntektsmeldingInnsendingsårsak.ENDRING, revurderingInntekt,
                førsteUttaksdato, ARBEIDSID_DEFAULT);
        lagEkstraInntektsmelding(InntektsmeldingInnsendingsårsak.ENDRING, revurderingInntekt, førsteUttaksdato, ekstraArbeidsgiver, ARBEIDSID_EKSTRA,
                inntektsmeldingerMottattEtterVedtak);
        var ref = lagReferanse(revurdering);
        lenient().when(inntektArbeidYtelseTjeneste.finnGrunnlag(behandling.getId())).thenReturn(Optional.of(førstegangsbehandlingGrunnlagIAY));
        lenient().when(inntektArbeidYtelseTjeneste.finnGrunnlag(revurdering.getId())).thenReturn(Optional.of(revurderingGrunnlagIAY));
        lenient().when(revurderingIMAggregat.getInntektsmeldingerSomSkalBrukes()).thenReturn(inntektsmeldingerMottattEtterVedtak);

        // Act/Assert
        assertThat(utledStartpunkt(ref)).isEqualTo(StartpunktType.BEREGNING);
    }

    @Test
    void skal_returnere_beregning_dersom_endring_på_natural_ytelser_mellom_ny_IM_og_grunnlag_IM() {
        // Arrange - opprette avsluttet førstegangsbehandling
        var behandling = opprettFørstegangsbehandling();

        var førsteUttaksdato = LocalDate.now();

        var førstegangsbehandlingIM = lagInntektsmelding(InntektsmeldingInnsendingsårsak.NY, INNTEKTBELØP_DEFAULT, førsteUttaksdato,
                ARBEIDSID_DEFAULT);
        lenient().when(førstegangsbehandlingIMAggregat.getInntektsmeldingerSomSkalBrukes()).thenReturn(førstegangsbehandlingIM);

        // Arrange - opprette revurderingsbehandling
        var revurdering = opprettRevurdering(behandling);

        var inntektsmeldingerMottattEtterVedtak = lagInntektsmeldingMedNaturalytelse(InntektsmeldingInnsendingsårsak.ENDRING,
                INNTEKTBELØP_DEFAULT, førsteUttaksdato, ARBEIDSID_DEFAULT,
                new NaturalYtelse(LocalDate.now(), LocalDate.now().plusDays(1), new BigDecimal(30), null));
        var ref = lagReferanse(revurdering);
        lenient().when(inntektArbeidYtelseTjeneste.finnGrunnlag(behandling.getId())).thenReturn(Optional.of(førstegangsbehandlingGrunnlagIAY));
        lenient().when(inntektArbeidYtelseTjeneste.finnGrunnlag(revurdering.getId())).thenReturn(Optional.of(revurderingGrunnlagIAY));
        lenient().when(revurderingIMAggregat.getInntektsmeldingerSomSkalBrukes()).thenReturn(inntektsmeldingerMottattEtterVedtak);

        // Act/Assert
        assertThat(utledStartpunkt(ref)).isEqualTo(StartpunktType.BEREGNING);
    }

    @Test
    void skal_returnere_uttak_dersom_ingen_endring_i_permisjonsdag_inntekt_naturytelser_eller_refusjon() {
        // Arrange - opprette avsluttet førstegangsbehandling
        var behandling = opprettFørstegangsbehandling();

        var førsteUttaksdato = LocalDate.now();

        var førstegangsbehandlingIM = lagInntektsmelding(InntektsmeldingInnsendingsårsak.NY, INNTEKTBELØP_DEFAULT, førsteUttaksdato,
                ARBEIDSID_DEFAULT);
        lenient().when(førstegangsbehandlingIMAggregat.getInntektsmeldingerSomSkalBrukes()).thenReturn(førstegangsbehandlingIM);

        // Arrange - opprette revurderingsbehandling
        var revurdering = opprettRevurdering(behandling);

        var inntektsmeldingerMottattEtterVedtak = lagInntektsmelding(InntektsmeldingInnsendingsårsak.ENDRING, INNTEKTBELØP_DEFAULT,
                førsteUttaksdato, ARBEIDSID_DEFAULT);
        var ref = lagReferanse(revurdering);
        lenient().when(inntektArbeidYtelseTjeneste.finnGrunnlag(behandling.getId())).thenReturn(Optional.of(førstegangsbehandlingGrunnlagIAY));
        lenient().when(inntektArbeidYtelseTjeneste.finnGrunnlag(revurdering.getId())).thenReturn(Optional.of(revurderingGrunnlagIAY));
        lenient().when(revurderingIMAggregat.getInntektsmeldingerSomSkalBrukes()).thenReturn(inntektsmeldingerMottattEtterVedtak);

        // Act/Assert
        assertThat(utledStartpunkt(ref)).isEqualTo(StartpunktType.UDEFINERT);
    }

    @Test
    void skal_returnere_opplysningsplikt_dersom_svp_og_nytt_arbforhold_i_samme_virksomhet_ved_revurdering() {
        // Arrange - opprette avsluttet førstegangsbehandling
        var behandlingSvp = opprettFørstegangsbehandlingSvp();

        var førsteUttaksdato = LocalDate.now();

        var førstegangsbehandlingIM = lagInntektsmelding(InntektsmeldingInnsendingsårsak.NY, INNTEKTBELØP_DEFAULT, førsteUttaksdato,
            ARBEIDSID_DEFAULT);
        lenient().when(førstegangsbehandlingIMAggregat.getInntektsmeldingerSomSkalBrukes()).thenReturn(førstegangsbehandlingIM);

        // Arrange - opprette revurderingsbehandling
        var revurdering = opprettRevurdering(behandlingSvp);

        var inntektsmeldingerMottattEtterVedtak = lagInntektsmelding(InntektsmeldingInnsendingsårsak.ENDRING, INNTEKTBELØP_DEFAULT,
            førsteUttaksdato, ARBEIDSID_EKSTRA);
        var ref = lagReferanse(revurdering);
        lenient().when(inntektArbeidYtelseTjeneste.finnGrunnlag(behandlingSvp.getId())).thenReturn(Optional.of(førstegangsbehandlingGrunnlagIAY));
        lenient().when(inntektArbeidYtelseTjeneste.finnGrunnlag(revurdering.getId())).thenReturn(Optional.of(revurderingGrunnlagIAY));
        lenient().when(revurderingIMAggregat.getInntektsmeldingerSomSkalBrukes()).thenReturn(inntektsmeldingerMottattEtterVedtak);

        // Act/Assert
        assertThat(utledStartpunkt(ref)).isEqualTo(StartpunktType.INNGANGSVILKÅR_OPPLYSNINGSPLIKT);
    }

    @Test
    void skal_returnere_opplysningsplikt_dersom_svp_og_nytt_arbforhold_i_samme_virksomhet_ved_førstegangsbehandling() {
        // Arrange - opprette avsluttet førstegangsbehandling
        var behandlingSvp = opprettFørstegangsbehandlingSvp();

        var førsteUttaksdato = LocalDate.now();

        var førstegangsbehandlingIM = lagInntektsmelding(InntektsmeldingInnsendingsårsak.NY, INNTEKTBELØP_DEFAULT, førsteUttaksdato,
            ARBEIDSID_DEFAULT);
        lenient().when(førstegangsbehandlingIMAggregat.getInntektsmeldingerSomSkalBrukes()).thenReturn(førstegangsbehandlingIM);

        var inntektsmeldingerMottattEtterVedtak = lagInntektsmelding(InntektsmeldingInnsendingsårsak.ENDRING, INNTEKTBELØP_DEFAULT,
            førsteUttaksdato, ARBEIDSID_EKSTRA);
        var ref = lagReferanse(behandlingSvp);
        lenient().when(inntektArbeidYtelseTjeneste.finnGrunnlag(behandlingSvp.getId())).thenReturn(Optional.of(førstegangsbehandlingGrunnlagIAY));
        lenient().when(revurderingIMAggregat.getInntektsmeldingerSomSkalBrukes()).thenReturn(inntektsmeldingerMottattEtterVedtak);

        // Act/Assert
        assertThat(utledStartpunkt(ref)).isEqualTo(StartpunktType.INNGANGSVILKÅR_OPPLYSNINGSPLIKT);
    }

    @Test
    void skal_returnere_opplysningsplikt_dersom_svp_og_endring_i_arbeidsforholdsId_ved_førstegangsbehandling() {
        // Arrange - opprette avsluttet førstegangsbehandling
        var behandlingSvp = opprettFørstegangsbehandlingSvp();

        var førsteUttaksdato = LocalDate.now();

        var førstegangsbehandlingIM = lagInntektsmelding(InntektsmeldingInnsendingsårsak.NY, INNTEKTBELØP_DEFAULT, førsteUttaksdato,
            ARBEIDSID_DEFAULT);
        lenient().when(førstegangsbehandlingIMAggregat.getInntektsmeldingerSomSkalBrukes()).thenReturn(førstegangsbehandlingIM);

        var inntektsmeldingerMottattEtterVedtak = lagInntektsmelding(InntektsmeldingInnsendingsårsak.ENDRING, INNTEKTBELØP_DEFAULT,
            førsteUttaksdato, InternArbeidsforholdRef.nullRef());
        var ref = lagReferanse(behandlingSvp);
        lenient().when(inntektArbeidYtelseTjeneste.finnGrunnlag(behandlingSvp.getId())).thenReturn(Optional.of(førstegangsbehandlingGrunnlagIAY));
        lenient().when(revurderingIMAggregat.getInntektsmeldingerSomSkalBrukes()).thenReturn(inntektsmeldingerMottattEtterVedtak);

        // Act/Assert
        assertThat(utledStartpunkt(ref)).isEqualTo(StartpunktType.INNGANGSVILKÅR_OPPLYSNINGSPLIKT);
    }

    private Behandling opprettRevurdering(Behandling førstegangsbehandling) {
        var revurderingScenario = ScenarioMorSøkerForeldrepenger.forFødsel()
                .medBehandlingType(BehandlingType.REVURDERING)
                .medOriginalBehandling(førstegangsbehandling, BehandlingÅrsakType.UDEFINERT);
        return revurderingScenario.lagre(repositoryProvider);
    }

    private Behandling opprettFørstegangsbehandling() {
        var førstegangScenario = ScenarioMorSøkerForeldrepenger.forFødsel();
        var førstegangsbehandling = førstegangScenario.lagre(repositoryProvider);
        førstegangsbehandling.avsluttBehandling();
        var behandlingLås = behandlingRepository.taSkriveLås(førstegangsbehandling);
        behandlingRepository.lagre(førstegangsbehandling, behandlingLås);
        return førstegangsbehandling;
    }

    private Behandling opprettFørstegangsbehandlingSvp() {
        var førstegangScenario = ScenarioMorSøkerSvangerskapspenger.forSvangerskapspenger();
        var førstegangsbehandling = førstegangScenario.lagre(repositoryProvider);
        førstegangsbehandling.avsluttBehandling();
        var behandlingLås = behandlingRepository.taSkriveLås(førstegangsbehandling);
        behandlingRepository.lagre(førstegangsbehandling, behandlingLås);
        return førstegangsbehandling;
    }

    private List<Inntektsmelding> lagInntektsmelding(InntektsmeldingInnsendingsårsak innsendingsårsak, BigDecimal beløp, LocalDate førsteUttaksdato,
            InternArbeidsforholdRef arbeidID) {
        List<Inntektsmelding> inntektsmeldingerGrunnlag = new ArrayList<>();
        var inntektsmelding = getInntektsmeldingBuilder()
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
        var inntektsmelding = getInntektsmeldingBuilder()
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
                                          Arbeidsgiver arbeidsgiver,
                                          InternArbeidsforholdRef arbeidID, List<Inntektsmelding> im) {
        var inntektsmelding = getInntektsmeldingBuilder()
                .medBeløp(beløp)
                .medArbeidsforholdId(arbeidID)
                .medStartDatoPermisjon(førsteUttaksdato)
                .medArbeidsgiver(arbeidsgiver)
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
        var inntektsmelding = getInntektsmeldingBuilder()
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

    private BehandlingReferanse lagReferanse(Behandling behandling) {
        return BehandlingReferanse.fra(behandling);
    }
}
