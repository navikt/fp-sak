package no.nav.foreldrepenger.domene.arbeidInntektsmelding;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import no.nav.foreldrepenger.behandling.BehandlingReferanse;
import no.nav.foreldrepenger.behandling.Skjæringstidspunkt;
import no.nav.foreldrepenger.behandling.aksjonspunkt.AksjonspunktOppdaterParameter;
import no.nav.foreldrepenger.behandlingslager.behandling.BehandlingStatus;
import no.nav.foreldrepenger.behandlingslager.behandling.BehandlingType;
import no.nav.foreldrepenger.behandlingslager.behandling.arbeidsforhold.ArbeidsforholdKomplettVurderingType;
import no.nav.foreldrepenger.behandlingslager.behandling.arbeidsforhold.ArbeidsforholdValg;
import no.nav.foreldrepenger.behandlingslager.behandling.personopplysning.RelasjonsRolleType;
import no.nav.foreldrepenger.behandlingslager.fagsak.FagsakYtelseType;
import no.nav.foreldrepenger.behandlingslager.virksomhet.Arbeidsgiver;
import no.nav.foreldrepenger.domene.arbeidsforhold.InntektArbeidYtelseTjeneste;
import no.nav.foreldrepenger.domene.arbeidsforhold.impl.AksjonspunktÅrsak;
import no.nav.foreldrepenger.domene.iay.modell.InntektArbeidYtelseGrunnlag;
import no.nav.foreldrepenger.domene.typer.AktørId;
import no.nav.foreldrepenger.domene.typer.InternArbeidsforholdRef;
import no.nav.foreldrepenger.domene.typer.Saksnummer;
import no.nav.foreldrepenger.skjæringstidspunkt.SkjæringstidspunktTjeneste;

@ExtendWith(MockitoExtension.class)
class AvklarArbeidInntektsmeldingOppdatererTest {

    private static final Arbeidsgiver ARBEIDSGIVER = Arbeidsgiver.virksomhet("999999999");

    @Mock
    private ArbeidsforholdInntektsmeldingMangelTjeneste mangelTjeneste;
    @Mock
    private SkjæringstidspunktTjeneste skjæringstidspunktTjeneste;
    @Mock
    private InntektArbeidYtelseTjeneste inntektArbeidYtelseTjeneste;

    private AvklarArbeidInntektsmeldingOppdaterer oppdaterer;
    private AksjonspunktOppdaterParameter param;
    private final BekreftArbeidInntektsmeldingAksjonspunktDto dto = mock(BekreftArbeidInntektsmeldingAksjonspunktDto.class);

    @BeforeEach
    void setUp() {
        oppdaterer = new AvklarArbeidInntektsmeldingOppdaterer(mangelTjeneste, skjæringstidspunktTjeneste, inntektArbeidYtelseTjeneste);
        var ref = new BehandlingReferanse(new Saksnummer("123"), 321L, FagsakYtelseType.FORELDREPENGER, 123L, UUID.randomUUID(),
            BehandlingStatus.UTREDES, BehandlingType.FØRSTEGANGSSØKNAD, null, AktørId.dummy(), RelasjonsRolleType.MORA);
        param = new AksjonspunktOppdaterParameter(ref, dto);
        var stp = Skjæringstidspunkt.builder().build();
        when(skjæringstidspunktTjeneste.getSkjæringstidspunkter(ref.behandlingId())).thenReturn(stp);
        when(mangelTjeneste.utledAlleManglerPåArbeidsforholdInntektsmelding(ref, stp)).thenReturn(
            List.of(new ArbeidsforholdMangel(ARBEIDSGIVER, InternArbeidsforholdRef.nullRef(), AksjonspunktÅrsak.MANGLENDE_INNTEKTSMELDING)));
        var grunnlag = mock(InntektArbeidYtelseGrunnlag.class);
        lenient().when(grunnlag.getArbeidsforholdInformasjon()).thenReturn(Optional.empty());
        lenient().when(inntektArbeidYtelseTjeneste.hentGrunnlag(any(UUID.class))).thenReturn(grunnlag);
    }

    @Test
    void godtar_valg_på_arbeidsgiver() {
        when(mangelTjeneste.hentArbeidsforholdValgForSak(any())).thenReturn(
            List.of(lagValg(InternArbeidsforholdRef.nullRef(), ArbeidsforholdKomplettVurderingType.FORTSETT_UTEN_INNTEKTSMELDING)));

        assertThatCode(() -> oppdaterer.oppdater(dto, param)).doesNotThrowAnyException();
    }

    @Test
    void godtar_eldre_valg_lagret_pr_arbeidsforhold_når_mangel_gjelder_arbeidsgiver() {
        when(mangelTjeneste.hentArbeidsforholdValgForSak(any())).thenReturn(List.of(
            lagValg(InternArbeidsforholdRef.nyRef(), ArbeidsforholdKomplettVurderingType.KONTAKT_ARBEIDSGIVER_VED_MANGLENDE_INNTEKTSMELDING),
            lagValg(InternArbeidsforholdRef.nyRef(), ArbeidsforholdKomplettVurderingType.FORTSETT_UTEN_INNTEKTSMELDING)));

        assertThatCode(() -> oppdaterer.oppdater(dto, param)).doesNotThrowAnyException();
    }

    @Test
    void avviser_når_det_ikke_er_valgt_å_fortsette_uten_inntektsmelding() {
        when(mangelTjeneste.hentArbeidsforholdValgForSak(any())).thenReturn(
            List.of(lagValg(InternArbeidsforholdRef.nullRef(), ArbeidsforholdKomplettVurderingType.KONTAKT_ARBEIDSGIVER_VED_MANGLENDE_INNTEKTSMELDING)));

        assertThatThrownBy(() -> oppdaterer.oppdater(dto, param)).isInstanceOf(IllegalStateException.class);
    }

    private static ArbeidsforholdValg lagValg(InternArbeidsforholdRef ref, ArbeidsforholdKomplettVurderingType vurdering) {
        return ArbeidsforholdValg.builder()
            .medArbeidsgiver(ARBEIDSGIVER.getIdentifikator())
            .medArbeidsforholdRef(ref)
            .medVurdering(vurdering)
            .medBegrunnelse("begrunnelse")
            .build();
    }
}
