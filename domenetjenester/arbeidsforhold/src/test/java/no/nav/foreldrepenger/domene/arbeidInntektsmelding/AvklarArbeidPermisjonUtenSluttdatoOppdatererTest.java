package no.nav.foreldrepenger.domene.arbeidInntektsmelding;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import jakarta.inject.Inject;
import jakarta.persistence.EntityManager;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.Mockito;

import no.nav.foreldrepenger.behandling.BehandlingReferanse;
import no.nav.foreldrepenger.behandling.Skjæringstidspunkt;
import no.nav.foreldrepenger.behandling.aksjonspunkt.AksjonspunktOppdaterParameter;
import no.nav.foreldrepenger.behandlingslager.behandling.Behandling;
import no.nav.foreldrepenger.behandlingslager.behandling.aksjonspunkt.AksjonspunktDefinisjon;
import no.nav.foreldrepenger.behandlingslager.behandling.aksjonspunkt.AksjonspunktTestSupport;
import no.nav.foreldrepenger.behandlingslager.behandling.historikk.HistorikkinnslagRepository;
import no.nav.foreldrepenger.behandlingslager.fagsak.FagsakYtelseType;
import no.nav.foreldrepenger.behandlingslager.virksomhet.ArbeidType;
import no.nav.foreldrepenger.behandlingslager.virksomhet.Arbeidsgiver;
import no.nav.foreldrepenger.behandlingslager.virksomhet.OrgNummer;
import no.nav.foreldrepenger.behandlingslager.virksomhet.Virksomhet;
import no.nav.foreldrepenger.dbstoette.CdiDbAwareTest;
import no.nav.foreldrepenger.domene.abakus.AbakusInMemoryInntektArbeidYtelseTjeneste;
import no.nav.foreldrepenger.domene.arbeidInntektsmelding.historikk.ArbeidPermHistorikkInnslagTjeneste;
import no.nav.foreldrepenger.domene.arbeidsforhold.InntektArbeidYtelseTjeneste;
import no.nav.foreldrepenger.domene.arbeidsforhold.impl.ArbeidsforholdAdministrasjonTjeneste;
import no.nav.foreldrepenger.domene.arbeidsforhold.person.PersonIdentTjeneste;
import no.nav.foreldrepenger.domene.arbeidsforhold.testutilities.behandling.IAYRepositoryProvider;
import no.nav.foreldrepenger.domene.arbeidsforhold.testutilities.behandling.IAYScenarioBuilder;
import no.nav.foreldrepenger.domene.arbeidsgiver.ArbeidsgiverTjeneste;
import no.nav.foreldrepenger.domene.arbeidsgiver.VirksomhetTjeneste;
import no.nav.foreldrepenger.domene.iay.modell.ArbeidsforholdInformasjonBuilder;
import no.nav.foreldrepenger.domene.iay.modell.ArbeidsforholdOverstyringBuilder;
import no.nav.foreldrepenger.domene.iay.modell.InntektArbeidYtelseAggregatBuilder;
import no.nav.foreldrepenger.domene.iay.modell.Opptjeningsnøkkel;
import no.nav.foreldrepenger.domene.iay.modell.Permisjon;
import no.nav.foreldrepenger.domene.iay.modell.YrkesaktivitetBuilder;
import no.nav.foreldrepenger.domene.iay.modell.kodeverk.ArbeidsforholdHandlingType;
import no.nav.foreldrepenger.domene.iay.modell.kodeverk.BekreftetPermisjonStatus;
import no.nav.foreldrepenger.domene.iay.modell.kodeverk.PermisjonsbeskrivelseType;
import no.nav.foreldrepenger.domene.tid.DatoIntervallEntitet;
import no.nav.foreldrepenger.domene.typer.InternArbeidsforholdRef;
import no.nav.vedtak.konfig.Tid;

@CdiDbAwareTest
class AvklarArbeidPermisjonUtenSluttdatoOppdatererTest {
    private static final String NAV_ORGNR = "889640782";
    private static final String KUNSTIG_ORG = OrgNummer.KUNSTIG_ORG;
    private static final String INTERN_ARBEIDSFORHOLD_ID = "a6ea6724-868f-11e9-bc42-526af7764f64";
    private static final String INTERN_ARBEIDSFORHOLD_ID_2 = "a6ea6724-868f-11e9-bc42-526af7764f65";

    @Inject
    private IAYRepositoryProvider provider;
    @Mock
    private VirksomhetTjeneste virksomhetTjeneste;
    @Mock
    private PersonIdentTjeneste personIdentTjeneste;
    @Mock
    private ArbeidsgiverTjeneste arbeidsgiverTjeneste;

    private final InntektArbeidYtelseTjeneste iayTjeneste = new AbakusInMemoryInntektArbeidYtelseTjeneste();

    private AvklarArbeidPermisjonUtenSluttdatoOppdaterer avklarArbeidPermisjonUtenSluttdatoOppdaterer;
    private Behandling behandling;

    @BeforeEach
    void setUp(EntityManager entityManager) {
        provider = new IAYRepositoryProvider(entityManager);
        Mockito.lenient().when(virksomhetTjeneste.hentOrganisasjon(any())).thenReturn(lagVirksomhet(NAV_ORGNR));
        arbeidsgiverTjeneste = new ArbeidsgiverTjeneste(personIdentTjeneste, virksomhetTjeneste);
        var arbeidsforholdAdministrasjonTjeneste = new ArbeidsforholdAdministrasjonTjeneste(
                iayTjeneste);
        var historikkRepository = new HistorikkinnslagRepository(entityManager);
        var arbeidsforholdHistorikkTjeneste = new ArbeidPermHistorikkInnslagTjeneste(historikkRepository, arbeidsgiverTjeneste);

        avklarArbeidPermisjonUtenSluttdatoOppdaterer = new AvklarArbeidPermisjonUtenSluttdatoOppdaterer(arbeidsforholdAdministrasjonTjeneste, arbeidsforholdHistorikkTjeneste, iayTjeneste);

        var iayScenarioBuilder = IAYScenarioBuilder.morSøker(FagsakYtelseType.FORELDREPENGER);
        behandling = iayScenarioBuilder.lagre(provider);
    }

    @Test
    void bekrefte_avklart_permisjon_uten_sluttdato() {
        // Arrange
        var inntektArbeidYtelseAggregatBuilder = iayTjeneste.opprettBuilderForRegister(behandling.getId());
        leggTilArbeidsforholdPåBehandling(behandling, NAV_ORGNR, InternArbeidsforholdRef.ref(INTERN_ARBEIDSFORHOLD_ID), inntektArbeidYtelseAggregatBuilder);
        iayTjeneste.lagreIayAggregat(behandling.getId(), inntektArbeidYtelseAggregatBuilder);

        var inntektArbeidYtelseAggregatBuilder1 = iayTjeneste.opprettBuilderForRegister(behandling.getId());
        leggTilArbeidsforholdPåBehandling(behandling, KUNSTIG_ORG, InternArbeidsforholdRef.ref(INTERN_ARBEIDSFORHOLD_ID_2), inntektArbeidYtelseAggregatBuilder1);
        iayTjeneste.lagreIayAggregat(behandling.getId(), inntektArbeidYtelseAggregatBuilder1);

        var aksjonspunkt = AksjonspunktTestSupport.leggTilAksjonspunkt(behandling,
            AksjonspunktDefinisjon.VURDER_PERMISJON_UTEN_SLUTTDATO);

        var bekreftetArbeidMedPermisjonUtenSluttdato = new BekreftArbeidMedPermisjonUtenSluttdatoDto("Har tatt stilling til dette",
            List.of(new AvklarPermisjonUtenSluttdatoDto(NAV_ORGNR, INTERN_ARBEIDSFORHOLD_ID, BekreftetPermisjonStatus.BRUK_PERMISJON),
                new AvklarPermisjonUtenSluttdatoDto(KUNSTIG_ORG, INTERN_ARBEIDSFORHOLD_ID_2, BekreftetPermisjonStatus.IKKE_BRUK_PERMISJON)));

        //Act
        var resultat = avklarArbeidPermisjonUtenSluttdatoOppdaterer.oppdater(bekreftetArbeidMedPermisjonUtenSluttdato, new AksjonspunktOppdaterParameter(
            BehandlingReferanse.fra(behandling), bekreftetArbeidMedPermisjonUtenSluttdato, aksjonspunkt));

        //Assert
        var behandlingRepository = provider.getBehandlingRepository();
        var overstyring = iayTjeneste.hentGrunnlag(behandling.getId()).getArbeidsforholdOverstyringer();
        var behandling1 = behandlingRepository.hentBehandling(behandling.getId());
        var aksjonspunkter = behandling1.getAksjonspunkter();

        assertThat(aksjonspunkter).hasSize(1);
        assertThat(resultat.kreverTotrinnsKontroll()).isFalse();
        assertThat(overstyring.get(0).getBekreftetPermisjon().get().getStatus()).isEqualTo(BekreftetPermisjonStatus.BRUK_PERMISJON);
        assertThat(overstyring.get(0).getArbeidsforholdRef()).isEqualTo(InternArbeidsforholdRef.ref(INTERN_ARBEIDSFORHOLD_ID));
        assertThat(overstyring.get(1).getBekreftetPermisjon().get().getStatus()).isEqualTo(BekreftetPermisjonStatus.IKKE_BRUK_PERMISJON);
        assertThat(overstyring.get(1).getArbeidsforholdRef()).isEqualTo(InternArbeidsforholdRef.ref(INTERN_ARBEIDSFORHOLD_ID_2));

    }

    @Test
    void ikke_overstyre_eksisterende_overstyringer_gjort_i_5085_5080() {
        // Arrange
        var inntektArbeidYtelseAggregatBuilder = iayTjeneste.opprettBuilderForRegister(behandling.getId());
        leggTilArbeidsforholdPåBehandling(behandling, NAV_ORGNR, InternArbeidsforholdRef.ref(INTERN_ARBEIDSFORHOLD_ID), inntektArbeidYtelseAggregatBuilder);
        iayTjeneste.lagreIayAggregat(behandling.getId(), inntektArbeidYtelseAggregatBuilder);

        var arbeidsforholdOverstyringBuilder = ArbeidsforholdOverstyringBuilder.oppdatere(Optional.empty())
            .medArbeidsgiver(Arbeidsgiver.virksomhet(NAV_ORGNR))
            .medArbeidsforholdRef(InternArbeidsforholdRef.ref(INTERN_ARBEIDSFORHOLD_ID))
            .medHandling(ArbeidsforholdHandlingType.BASERT_PÅ_INNTEKTSMELDING)
            .leggTilOverstyrtPeriode(LocalDate.now(), LocalDate.now().plusDays(2));

        var arbeidsforholdInformasjonBuilder = ArbeidsforholdInformasjonBuilder.builder(Optional.empty()).leggTil(arbeidsforholdOverstyringBuilder);

        iayTjeneste.lagreOverstyrtArbeidsforhold(behandling.getId(), arbeidsforholdInformasjonBuilder);

        var aksjonspunkt = AksjonspunktTestSupport.leggTilAksjonspunkt(behandling,
            AksjonspunktDefinisjon.VURDER_PERMISJON_UTEN_SLUTTDATO);

        var bekreftetArbeidMedPermisjonUtenSluttdato = new BekreftArbeidMedPermisjonUtenSluttdatoDto("Har tatt stilling til dette",
            List.of(new AvklarPermisjonUtenSluttdatoDto(NAV_ORGNR, INTERN_ARBEIDSFORHOLD_ID, BekreftetPermisjonStatus.BRUK_PERMISJON)));

        var skjæringstidspunkt = Skjæringstidspunkt.builder()
            .medUtledetSkjæringstidspunkt(LocalDate.of(2019, 1, 1))
            .build();

        //Act
        var resultat = avklarArbeidPermisjonUtenSluttdatoOppdaterer.oppdater(bekreftetArbeidMedPermisjonUtenSluttdato, new AksjonspunktOppdaterParameter(
            BehandlingReferanse.fra(behandling), bekreftetArbeidMedPermisjonUtenSluttdato, aksjonspunkt));

        //Assert
        var behandlingRepository = provider.getBehandlingRepository();
        var overstyring = iayTjeneste.hentGrunnlag(behandling.getId()).getArbeidsforholdOverstyringer();
        var behandling1 = behandlingRepository.hentBehandling(behandling.getId());
        var aksjonspunkter = behandling1.getAksjonspunkter();

        assertThat(aksjonspunkter).hasSize(1);
        assertThat(resultat.kreverTotrinnsKontroll()).isFalse();
        assertThat(overstyring.get(0).getBekreftetPermisjon().get().getStatus()).isEqualTo(BekreftetPermisjonStatus.BRUK_PERMISJON);
        assertThat(overstyring.get(0).getHandling()).isEqualTo(ArbeidsforholdHandlingType.BASERT_PÅ_INNTEKTSMELDING);
        assertThat(overstyring.get(0).getArbeidsforholdRef()).isEqualTo(InternArbeidsforholdRef.ref(INTERN_ARBEIDSFORHOLD_ID));
        assertThat(overstyring.get(0).getArbeidsforholdOverstyrtePerioder()).isNotEmpty();
    }

    private void leggTilArbeidsforholdPåBehandling(Behandling behandling, String virksomhetOrgnr, InternArbeidsforholdRef ref,
                                                   InntektArbeidYtelseAggregatBuilder builder) {
        var arbeidsgiver = Arbeidsgiver.virksomhet(virksomhetOrgnr);
        var arbeidBuilder = builder.getAktørArbeidBuilder(behandling.getAktørId());
        var nøkkel = Opptjeningsnøkkel.forArbeidsforholdIdMedArbeidgiver(ref, arbeidsgiver);
        var yrkesaktivitetBuilderForType = arbeidBuilder.getYrkesaktivitetBuilderForNøkkelAvType(nøkkel, ArbeidType.ORDINÆRT_ARBEIDSFORHOLD);
        yrkesaktivitetBuilderForType.medArbeidsgiver(arbeidsgiver)
            .medArbeidsforholdId(ref)
            .leggTilPermisjon(byggPermisjon(yrkesaktivitetBuilderForType, LocalDate.now(), Tid.TIDENES_ENDE))
            .leggTilAktivitetsAvtale(
                yrkesaktivitetBuilderForType.getAktivitetsAvtaleBuilder(DatoIntervallEntitet.fraOgMed(LocalDate.now().minusMonths(3)), false)
                    .medSisteLønnsendringsdato(LocalDate.now().minusMonths(3))
                    .medProsentsats(BigDecimal.valueOf(100)))
            .leggTilAktivitetsAvtale(yrkesaktivitetBuilderForType
                .getAktivitetsAvtaleBuilder(DatoIntervallEntitet.fraOgMed(LocalDate.now().minusMonths(3)), true));
        arbeidBuilder.leggTilYrkesaktivitet(yrkesaktivitetBuilderForType);
        builder.leggTilAktørArbeid(arbeidBuilder);



    }

    private Permisjon byggPermisjon(YrkesaktivitetBuilder yrkesaktivitetBuilder, LocalDate fom, LocalDate tom) {
        return yrkesaktivitetBuilder.getPermisjonBuilder()
            .medProsentsats(BigDecimal.valueOf(100))
            .medPeriode(fom, tom)
            .medPermisjonsbeskrivelseType(PermisjonsbeskrivelseType.PERMITTERING)
            .build();
    }
    private static Virksomhet lagVirksomhet(String orgnr) {
        var b = new Virksomhet.Builder();
        b.medOrgnr(orgnr).medNavn(NAV_ORGNR);
        return b.build();
    }
}
