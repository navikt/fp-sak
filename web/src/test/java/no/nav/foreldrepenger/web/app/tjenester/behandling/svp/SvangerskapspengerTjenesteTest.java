package no.nav.foreldrepenger.web.app.tjenester.behandling.svp;

import static org.assertj.core.api.Assertions.assertThat;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;

import no.nav.foreldrepenger.behandlingslager.behandling.familiehendelse.FamilieHendelseRepository;
import no.nav.foreldrepenger.behandlingslager.behandling.tilrettelegging.SvangerskapspengerRepository;
import no.nav.foreldrepenger.behandlingslager.behandling.tilrettelegging.SvpTilretteleggingEntitet;
import no.nav.foreldrepenger.behandlingslager.behandling.tilrettelegging.TilretteleggingFOM;
import no.nav.foreldrepenger.behandlingslager.behandling.tilrettelegging.TilretteleggingType;
import no.nav.foreldrepenger.behandlingslager.virksomhet.ArbeidType;
import no.nav.foreldrepenger.behandlingslager.virksomhet.Arbeidsgiver;
import no.nav.foreldrepenger.domene.arbeidsforhold.InntektArbeidYtelseTjeneste;
import no.nav.foreldrepenger.domene.arbeidsforhold.InntektsmeldingTjeneste;
import no.nav.foreldrepenger.domene.iay.modell.AktivitetsAvtaleBuilder;
import no.nav.foreldrepenger.domene.iay.modell.ArbeidsforholdInformasjon;
import no.nav.foreldrepenger.domene.iay.modell.Permisjon;
import no.nav.foreldrepenger.domene.iay.modell.Yrkesaktivitet;
import no.nav.foreldrepenger.domene.iay.modell.YrkesaktivitetBuilder;
import no.nav.foreldrepenger.domene.iay.modell.YrkesaktivitetFilter;
import no.nav.foreldrepenger.domene.tid.DatoIntervallEntitet;
import no.nav.foreldrepenger.domene.typer.InternArbeidsforholdRef;
import no.nav.foreldrepenger.skjæringstidspunkt.SkjæringstidspunktTjeneste;

class SvangerskapspengerTjenesteTest {
    private static final LocalDate SKJÆRINGSTIDSPUNKT = LocalDate.now();

    private SvangerskapspengerTjeneste svangerskapspengerTjeneste;
    @Mock
    private SvangerskapspengerRepository svangerskapspengerRepository;
    @Mock
    private FamilieHendelseRepository familieHendelseRepository;
    @Mock
    private InntektArbeidYtelseTjeneste iayTjeneste;
    @Mock
    private InntektsmeldingTjeneste inntektsmeldingTjeneste;
    @Mock
    private SkjæringstidspunktTjeneste skjæringstidspunktTjeneste;

    @BeforeEach
    void setUp() {
        svangerskapspengerTjeneste = new SvangerskapspengerTjeneste();
    }

    @Test
    void skal_utlede_riktig_stillingsprosent() {
        var arbeidsgiver = Arbeidsgiver.virksomhet("123456789");
        var forventetStillingsprosent = BigDecimal.valueOf(80);

        var avtaler = List.of(AktivitetsAvtaleBuilder.ny()
            .medPeriode(DatoIntervallEntitet.fraOgMed(SKJÆRINGSTIDSPUNKT.minusYears(1).minusWeeks(1)))
            .medProsentsats(BigDecimal.valueOf(80)));

        var ref = InternArbeidsforholdRef.nyRef();

        var yrkesaktivitet1 = lagYrkesaktivitet(avtaler, arbeidsgiver, ref, List.of());
        var registerFilter = new YrkesaktivitetFilter(new ArbeidsforholdInformasjon(), List.of(yrkesaktivitet1));

        var tilrettelegging= new SvpTilretteleggingEntitet.Builder().medArbeidType(ArbeidType.ORDINÆRT_ARBEIDSFORHOLD)
            .medArbeidsgiver(arbeidsgiver)
            .medInternArbeidsforholdRef(ref)
            .medTilretteleggingFraDatoer(List.of(new TilretteleggingFOM.Builder()
                .medStillingsprosent(BigDecimal.ZERO)
                .medTilretteleggingType(TilretteleggingType.DELVIS_TILRETTELEGGING)
                .medFomDato(SKJÆRINGSTIDSPUNKT.minusMonths(2))
                .build()))
            .build();

        var resultat = svangerskapspengerTjeneste.utledStillingsprosentForTilrPeriode(registerFilter, tilrettelegging);

        assertThat(resultat).isPresent();
        assertThat(resultat.get()).isEqualByComparingTo(forventetStillingsprosent);
    }

    @Test
    void skal_utlede_riktig_stillingsprosent_med_flere_avtaleperioder() {
        var arbeidsgiver = Arbeidsgiver.virksomhet("123456789");
        var forventetStillingsprosent = BigDecimal.valueOf(50);

        var avtaler = List.of(AktivitetsAvtaleBuilder.ny()
            .medPeriode(DatoIntervallEntitet.fraOgMedTilOgMed(SKJÆRINGSTIDSPUNKT.minusYears(1).minusWeeks(1), SKJÆRINGSTIDSPUNKT.minusMonths(6)))
            .medProsentsats(BigDecimal.valueOf(100)),
            AktivitetsAvtaleBuilder.ny()
                .medPeriode(DatoIntervallEntitet.fraOgMed(SKJÆRINGSTIDSPUNKT.minusMonths(6).plusDays(1)))
                .medProsentsats(BigDecimal.valueOf(50)));

        var ref = InternArbeidsforholdRef.nyRef();

        var yrkesaktivitet1 = lagYrkesaktivitet(avtaler, arbeidsgiver, ref, List.of());
        var registerFilter = new YrkesaktivitetFilter(new ArbeidsforholdInformasjon(), List.of(yrkesaktivitet1));

        var tilrettelegging= new SvpTilretteleggingEntitet.Builder().medArbeidType(ArbeidType.ORDINÆRT_ARBEIDSFORHOLD)
            .medArbeidsgiver(arbeidsgiver)
            .medInternArbeidsforholdRef(ref)
            .medTilretteleggingFraDatoer(List.of(new TilretteleggingFOM.Builder()
                .medStillingsprosent(BigDecimal.ZERO)
                .medTilretteleggingType(TilretteleggingType.DELVIS_TILRETTELEGGING)
                .medFomDato(SKJÆRINGSTIDSPUNKT.minusMonths(2))
                .build()))
            .build();

        var resultat = svangerskapspengerTjeneste.utledStillingsprosentForTilrPeriode(registerFilter, tilrettelegging);

        assertThat(resultat).isPresent();
        assertThat(resultat.get()).isEqualByComparingTo(forventetStillingsprosent);
    }

    @Test
    void skal_summere_stillingsprosent_hvis_flere_arbeidsforhold_i_samme_virksomhet() {
        var arbeidsgiver = Arbeidsgiver.virksomhet("123456789");
        var forventetStillingsprosent = BigDecimal.valueOf(150);

        var avtaler1 = List.of(AktivitetsAvtaleBuilder.ny()
            .medPeriode(DatoIntervallEntitet.fraOgMed(SKJÆRINGSTIDSPUNKT.minusYears(1).minusWeeks(1)))
            .medProsentsats(BigDecimal.valueOf(100)));
        var avtaler2 = List.of(AktivitetsAvtaleBuilder.ny()
            .medPeriode(DatoIntervallEntitet.fraOgMed(SKJÆRINGSTIDSPUNKT.minusYears(1).minusWeeks(1)))
            .medProsentsats(BigDecimal.valueOf(50)));

        var ref1 = InternArbeidsforholdRef.nyRef();
        var ref2 = InternArbeidsforholdRef.nyRef();

        var yrkesaktivitet1 = lagYrkesaktivitet(avtaler1, arbeidsgiver, ref1, List.of());
        var yrkesaktivitet2 = lagYrkesaktivitet(avtaler2, arbeidsgiver, ref2, List.of());
        var registerFilter = new YrkesaktivitetFilter(new ArbeidsforholdInformasjon(), List.of(yrkesaktivitet1, yrkesaktivitet2));

        var tilrettelegging= new SvpTilretteleggingEntitet.Builder().medArbeidType(ArbeidType.ORDINÆRT_ARBEIDSFORHOLD)
            .medArbeidsgiver(arbeidsgiver)
            .medTilretteleggingFraDatoer(List.of(new TilretteleggingFOM.Builder()
                .medStillingsprosent(BigDecimal.ZERO)
                .medTilretteleggingType(TilretteleggingType.DELVIS_TILRETTELEGGING)
                .medFomDato(SKJÆRINGSTIDSPUNKT.minusMonths(6))
                .build()))
            .build();

        var resultat = svangerskapspengerTjeneste.utledStillingsprosentForTilrPeriode(registerFilter, tilrettelegging);

        assertThat(resultat).isPresent();
        assertThat(resultat.get()).isEqualByComparingTo(forventetStillingsprosent);
    }

    @Test
    void skal_finne_stillingsprosent_for_arbeidsforholdet_om_tilrettelegging_har_arbeidsforholdsid() {
        var arbeidsgiver = Arbeidsgiver.virksomhet("123456789");
        var forventetStillingsprosentRef1 = BigDecimal.valueOf(50);

        var avtaler1 = List.of(AktivitetsAvtaleBuilder.ny()
            .medPeriode(DatoIntervallEntitet.fraOgMed(SKJÆRINGSTIDSPUNKT.minusYears(1).minusWeeks(1)))
            .medProsentsats(BigDecimal.valueOf(50)));
        var avtaler2 = List.of(AktivitetsAvtaleBuilder.ny()
            .medPeriode(DatoIntervallEntitet.fraOgMed(SKJÆRINGSTIDSPUNKT.minusYears(1).minusWeeks(1)))
            .medProsentsats(BigDecimal.valueOf(50)));

        var ref1 = InternArbeidsforholdRef.nyRef();
        var ref2 = InternArbeidsforholdRef.nyRef();

        var yrkesaktivitet1 = lagYrkesaktivitet(avtaler1, arbeidsgiver, ref1, List.of());
        var yrkesaktivitet2 = lagYrkesaktivitet(avtaler2, arbeidsgiver, ref2, List.of());
        var registerFilter = new YrkesaktivitetFilter(new ArbeidsforholdInformasjon(), List.of(yrkesaktivitet1, yrkesaktivitet2));

        var tilretteleggingRef1= new SvpTilretteleggingEntitet.Builder().medArbeidType(ArbeidType.ORDINÆRT_ARBEIDSFORHOLD)
            .medArbeidsgiver(arbeidsgiver)
            .medInternArbeidsforholdRef(ref1)
            .medTilretteleggingFraDatoer(List.of(new TilretteleggingFOM.Builder()
                .medStillingsprosent(BigDecimal.ZERO)
                .medTilretteleggingType(TilretteleggingType.DELVIS_TILRETTELEGGING)
                .medFomDato(SKJÆRINGSTIDSPUNKT.minusMonths(6))
                .build()))
            .build();

        var resultat = svangerskapspengerTjeneste.utledStillingsprosentForTilrPeriode(registerFilter, tilretteleggingRef1);

        assertThat(resultat).isPresent();
        assertThat(resultat.get()).isEqualByComparingTo(forventetStillingsprosentRef1);
    }

    private Yrkesaktivitet lagYrkesaktivitet(List<AktivitetsAvtaleBuilder> aktivitetsAvtale,
                                             Arbeidsgiver arbeidsgiver, InternArbeidsforholdRef ref, List<Permisjon> permisjoner) {
        var builder = YrkesaktivitetBuilder.oppdatere(Optional.empty())
            .medArbeidsforholdId(ref)
            .medArbeidsgiver(arbeidsgiver)
            .medArbeidType(ArbeidType.ORDINÆRT_ARBEIDSFORHOLD);
        aktivitetsAvtale.forEach(builder::leggTilAktivitetsAvtale);
        permisjoner.forEach(builder::leggTilPermisjon);
        return builder.build();
    }
}
