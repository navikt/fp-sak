package no.nav.foreldrepenger.domene.arbeidsforhold.impl;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import no.nav.foreldrepenger.behandling.BehandlingReferanse;
import no.nav.foreldrepenger.behandling.Skjæringstidspunkt;
import no.nav.foreldrepenger.behandlingslager.behandling.BehandlingStatus;
import no.nav.foreldrepenger.behandlingslager.behandling.BehandlingType;
import no.nav.foreldrepenger.behandlingslager.behandling.personopplysning.RelasjonsRolleType;
import no.nav.foreldrepenger.behandlingslager.fagsak.FagsakYtelseType;
import no.nav.foreldrepenger.behandlingslager.virksomhet.ArbeidType;
import no.nav.foreldrepenger.behandlingslager.virksomhet.Arbeidsgiver;
import no.nav.foreldrepenger.domene.abakus.ArbeidsforholdTjeneste;
import no.nav.foreldrepenger.domene.arbeidsforhold.InntektArbeidYtelseTjeneste;
import no.nav.foreldrepenger.domene.arbeidsforhold.InntektsmeldingTjeneste;
import no.nav.foreldrepenger.domene.arbeidsforhold.fp.InntektsmeldingFilterYtelseImpl;
import no.nav.foreldrepenger.domene.iay.modell.ArbeidsforholdMedPermisjon;
import no.nav.foreldrepenger.domene.iay.modell.kodeverk.PermisjonsbeskrivelseType;
import no.nav.foreldrepenger.domene.tid.DatoIntervallEntitet;
import no.nav.foreldrepenger.domene.typer.AktørId;
import no.nav.foreldrepenger.domene.typer.EksternArbeidsforholdRef;
import no.nav.foreldrepenger.domene.typer.Saksnummer;
import no.nav.vedtak.felles.testutilities.cdi.UnitTestLookupInstanceImpl;
// Denne testen tester kun InnteksmeldingRegisterTjeneste.utledManglendeInntektsmeldingerFraAAregVurderPermisjon()
@ExtendWith(MockitoExtension.class)
class InntektsmeldingRegisterTjenesteFpForAargTest {
    private static final LocalDate SKJÆRINGSTIDSPUNKT = LocalDate.now();
    private InntektsmeldingRegisterTjeneste inntektsmeldingRegisterTjeneste;
    @Mock
    private InntektArbeidYtelseTjeneste inntektArbeidYtelseTjeneste;
    @Mock
    private InntektsmeldingTjeneste inntektsmeldingTjeneste;
    @Mock
    private ArbeidsforholdTjeneste abakusArbeidsforholdTjeneste;

    private static BehandlingReferanse behandlingReferanse;
    private static final AktørId aktørId = AktørId.dummy();

    @BeforeEach
    void setUp() {
        var foreldrepengerFilter = new InntektsmeldingFilterYtelseImpl();
        inntektsmeldingRegisterTjeneste = new InntektsmeldingRegisterTjeneste(inntektArbeidYtelseTjeneste, inntektsmeldingTjeneste, abakusArbeidsforholdTjeneste,
            new UnitTestLookupInstanceImpl<>(foreldrepengerFilter));
        behandlingReferanse = new BehandlingReferanse(new Saksnummer("123"), 321L, FagsakYtelseType.FORELDREPENGER, 123L,
            UUID.randomUUID(), BehandlingStatus.UTREDES, BehandlingType.FØRSTEGANGSSØKNAD, null, aktørId, RelasjonsRolleType.MORA);
    }


    @Test
    void krever_ikke_inntektsmelding_for_et_av_arbeidsforholdene_fordi_det_er_100_prosent_permisjon() {
        var skjæringstidspunkt = Skjæringstidspunkt.builder().medUtledetSkjæringstidspunkt(SKJÆRINGSTIDSPUNKT).build();
        var arbeidsgiverMedRelevantPermisjon = Arbeidsgiver.virksomhet("123456789");
        var eksternRef = EksternArbeidsforholdRef.ref("123");
        var eksternRef2 = EksternArbeidsforholdRef.ref("456");

        var aktivitetsAvtalePeriode = DatoIntervallEntitet.fraOgMed(SKJÆRINGSTIDSPUNKT);
        var permisjonsPeriode = DatoIntervallEntitet.fraOgMedTilOgMed(SKJÆRINGSTIDSPUNKT.minusDays(2), SKJÆRINGSTIDSPUNKT.plusWeeks(2));
        var aktivitetsavtaler = List.of(lagAktivitetsavtale(aktivitetsAvtalePeriode, BigDecimal.valueOf(50)));
        var permisjoner = List.of(lagPermisjon(permisjonsPeriode, BigDecimal.valueOf(100)));

        var aktivitetsavtaler2 = List.of(lagAktivitetsavtale(aktivitetsAvtalePeriode, BigDecimal.valueOf(50)));

        var arbeidsforholdMedPermisjon = List.of(lagArbeidsforholdMedPermisjon(arbeidsgiverMedRelevantPermisjon, eksternRef, aktivitetsavtaler, permisjoner),
            lagArbeidsforholdMedPermisjon(arbeidsgiverMedRelevantPermisjon, eksternRef2, aktivitetsavtaler2, Collections.emptyList()));

        when(abakusArbeidsforholdTjeneste.hentArbeidsforholdInfoForEnPeriode(behandlingReferanse.aktørId(), SKJÆRINGSTIDSPUNKT, SKJÆRINGSTIDSPUNKT, FagsakYtelseType.FORELDREPENGER)).thenReturn(arbeidsforholdMedPermisjon);

        var manglendeInntektsmeldinger = inntektsmeldingRegisterTjeneste.utledManglendeInntektsmeldingerFraAAregVurderPermisjon(behandlingReferanse, skjæringstidspunkt);

        assertThat(manglendeInntektsmeldinger).hasSize(1);
        assertThat(manglendeInntektsmeldinger.values().stream().toList().getFirst()).isEqualTo(Set.of(eksternRef2));
    }

    @Test
    void krever_inntektsmelding_begge_arbeidsforholdene_når_permisjon_er_under_100_prosent() {
        var skjæringstidspunkt = Skjæringstidspunkt.builder().medUtledetSkjæringstidspunkt(SKJÆRINGSTIDSPUNKT).build();
        var arbeidsgiverMedRelevantPermisjon = Arbeidsgiver.virksomhet("123456789");

        var aktivitetsAvtalePeriode = DatoIntervallEntitet.fraOgMed(SKJÆRINGSTIDSPUNKT);
        var permisjonsPeriode = DatoIntervallEntitet.fraOgMedTilOgMed(SKJÆRINGSTIDSPUNKT.minusDays(2), SKJÆRINGSTIDSPUNKT.plusWeeks(2));
        var aktivitetsavtaler = List.of(lagAktivitetsavtale(aktivitetsAvtalePeriode, BigDecimal.valueOf(50)));
        var permisjoner = List.of(lagPermisjon(permisjonsPeriode, BigDecimal.valueOf(50)));

        var arbeidsgiver2 = Arbeidsgiver.virksomhet("987654321");
        var aktivitetsavtaler2 = List.of(lagAktivitetsavtale(aktivitetsAvtalePeriode, BigDecimal.valueOf(50)));

        var arbeidsforholdMedPermisjon = List.of(lagArbeidsforholdMedPermisjon(arbeidsgiverMedRelevantPermisjon, null, aktivitetsavtaler, permisjoner),
            lagArbeidsforholdMedPermisjon(arbeidsgiver2, null, aktivitetsavtaler2, Collections.emptyList()));

        when(abakusArbeidsforholdTjeneste.hentArbeidsforholdInfoForEnPeriode(behandlingReferanse.aktørId(), SKJÆRINGSTIDSPUNKT, SKJÆRINGSTIDSPUNKT, FagsakYtelseType.FORELDREPENGER)).thenReturn(arbeidsforholdMedPermisjon);

        var manglendeInntektsmeldinger = inntektsmeldingRegisterTjeneste.utledManglendeInntektsmeldingerFraAAregVurderPermisjon(behandlingReferanse, skjæringstidspunkt);

        assertThat(manglendeInntektsmeldinger).hasSize(2);
        assertThat(manglendeInntektsmeldinger.keySet().stream().toList()).contains(arbeidsgiver2, arbeidsgiverMedRelevantPermisjon);
    }

    @Test
    void krever_inntektsmelding_begge_arbeidsforholdene_når_permisjon_ikke_inkluderer_stp() {
        var skjæringstidspunkt = Skjæringstidspunkt.builder().medUtledetSkjæringstidspunkt(SKJÆRINGSTIDSPUNKT).build();
        var arbeidsgiverMedRelevantPermisjon = Arbeidsgiver.virksomhet("123456789");

        var aktivitetsAvtalePeriode = DatoIntervallEntitet.fraOgMed(SKJÆRINGSTIDSPUNKT);
        var permisjonsPeriode = DatoIntervallEntitet.fraOgMedTilOgMed(SKJÆRINGSTIDSPUNKT.minusDays(15), SKJÆRINGSTIDSPUNKT.minusDays(2));
        var aktivitetsavtaler = List.of(lagAktivitetsavtale(aktivitetsAvtalePeriode, BigDecimal.valueOf(50)));
        var permisjoner = List.of(lagPermisjon(permisjonsPeriode, BigDecimal.valueOf(100)));

        var arbeidsgiver2 = Arbeidsgiver.virksomhet("987654321");
        var aktivitetsavtaler2 = List.of(lagAktivitetsavtale(aktivitetsAvtalePeriode, BigDecimal.valueOf(50)));

        var arbeidsforholdMedPermisjon = List.of(lagArbeidsforholdMedPermisjon(arbeidsgiverMedRelevantPermisjon, null, aktivitetsavtaler, permisjoner),
            lagArbeidsforholdMedPermisjon(arbeidsgiver2, null, aktivitetsavtaler2, Collections.emptyList()));

        when(abakusArbeidsforholdTjeneste.hentArbeidsforholdInfoForEnPeriode(behandlingReferanse.aktørId(), SKJÆRINGSTIDSPUNKT, SKJÆRINGSTIDSPUNKT, FagsakYtelseType.FORELDREPENGER)).thenReturn(arbeidsforholdMedPermisjon);

        var manglendeInntektsmeldinger = inntektsmeldingRegisterTjeneste.utledManglendeInntektsmeldingerFraAAregVurderPermisjon(behandlingReferanse, skjæringstidspunkt);

        assertThat(manglendeInntektsmeldinger).hasSize(2);
        assertThat(manglendeInntektsmeldinger.keySet().stream().toList()).contains(arbeidsgiver2, arbeidsgiverMedRelevantPermisjon);
    }

    private ArbeidsforholdTjeneste.AktivitetAvtale lagAktivitetsavtale(DatoIntervallEntitet periode, BigDecimal stillingsprosent) {
        return new ArbeidsforholdTjeneste.AktivitetAvtale(periode, stillingsprosent);
    }

    private ArbeidsforholdMedPermisjon lagArbeidsforholdMedPermisjon(Arbeidsgiver arbeidsgiver, EksternArbeidsforholdRef ref, List<ArbeidsforholdTjeneste.AktivitetAvtale> aktivitetsavtaler, List<ArbeidsforholdTjeneste.Permisjon> permisjoner) {
        return new ArbeidsforholdMedPermisjon(arbeidsgiver, ArbeidType.ORDINÆRT_ARBEIDSFORHOLD, ref, aktivitetsavtaler, permisjoner );
    }
    private ArbeidsforholdTjeneste.Permisjon lagPermisjon(DatoIntervallEntitet periode, BigDecimal permisjonsprosent) {
        return new ArbeidsforholdTjeneste.Permisjon(periode, PermisjonsbeskrivelseType.ANNEN_PERMISJON_LOVFESTET, permisjonsprosent);
    }
}
