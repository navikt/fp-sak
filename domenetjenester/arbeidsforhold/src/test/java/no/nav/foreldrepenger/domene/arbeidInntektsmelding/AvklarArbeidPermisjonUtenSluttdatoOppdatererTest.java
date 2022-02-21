package no.nav.foreldrepenger.domene.arbeidInntektsmelding;

import static org.assertj.core.api.Assertions.assertThat;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import javax.inject.Inject;
import javax.persistence.EntityManager;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;

import no.nav.foreldrepenger.behandling.Skjæringstidspunkt;
import no.nav.foreldrepenger.behandling.aksjonspunkt.AksjonspunktOppdaterParameter;
import no.nav.foreldrepenger.behandlingslager.behandling.Behandling;
import no.nav.foreldrepenger.behandlingslager.behandling.aksjonspunkt.AksjonspunktDefinisjon;
import no.nav.foreldrepenger.behandlingslager.behandling.aksjonspunkt.AksjonspunktTestSupport;
import no.nav.foreldrepenger.behandlingslager.behandling.historikk.HistorikkRepository;
import no.nav.foreldrepenger.behandlingslager.fagsak.FagsakYtelseType;
import no.nav.foreldrepenger.behandlingslager.virksomhet.ArbeidType;
import no.nav.foreldrepenger.behandlingslager.virksomhet.Arbeidsgiver;
import no.nav.foreldrepenger.behandlingslager.virksomhet.OrgNummer;
import no.nav.foreldrepenger.dbstoette.CdiDbAwareTest;
import no.nav.foreldrepenger.dokumentarkiv.DokumentArkivTjeneste;
import no.nav.foreldrepenger.domene.abakus.AbakusInMemoryInntektArbeidYtelseTjeneste;
import no.nav.foreldrepenger.domene.arbeidsforhold.InntektArbeidYtelseTjeneste;
import no.nav.foreldrepenger.domene.arbeidsforhold.InntektsmeldingTjeneste;
import no.nav.foreldrepenger.domene.arbeidsforhold.VurderArbeidsforholdTjeneste;
import no.nav.foreldrepenger.domene.arbeidsforhold.impl.ArbeidsforholdAdministrasjonTjeneste;
import no.nav.foreldrepenger.domene.arbeidsforhold.person.PersonIdentTjeneste;
import no.nav.foreldrepenger.domene.arbeidsforhold.testutilities.behandling.IAYRepositoryProvider;
import no.nav.foreldrepenger.domene.arbeidsforhold.testutilities.behandling.IAYScenarioBuilder;
import no.nav.foreldrepenger.domene.arbeidsgiver.ArbeidsgiverTjeneste;
import no.nav.foreldrepenger.domene.arbeidsgiver.VirksomhetTjeneste;
import no.nav.foreldrepenger.domene.iay.modell.InntektArbeidYtelseAggregatBuilder;
import no.nav.foreldrepenger.domene.iay.modell.Permisjon;
import no.nav.foreldrepenger.domene.iay.modell.VersjonType;
import no.nav.foreldrepenger.domene.iay.modell.YrkesaktivitetBuilder;
import no.nav.foreldrepenger.domene.iay.modell.kodeverk.BekreftetPermisjonStatus;
import no.nav.foreldrepenger.domene.iay.modell.kodeverk.PermisjonsbeskrivelseType;
import no.nav.foreldrepenger.domene.tid.AbstractLocalDateInterval;
import no.nav.foreldrepenger.domene.tid.DatoIntervallEntitet;
import no.nav.foreldrepenger.domene.typer.InternArbeidsforholdRef;
import no.nav.foreldrepenger.historikk.HistorikkTjenesteAdapter;
import no.nav.vedtak.konfig.Tid;

@CdiDbAwareTest
class AvklarArbeidPermisjonUtenSluttdatoOppdatererTest {
    private static final String NAV_ORGNR = "889640782";
    private static final String KUNSTIG_ORG = OrgNummer.KUNSTIG_ORG;
    private static final String INTERN_ARBEIDSFORHOLD_ID = "a6ea6724-868f-11e9-bc42-526af7764f64";
    private static final String INTERN_ARBEIDSFORHOLD_ID_2 = "a6ea6724-868f-11e9-bc42-526af7764f65";

    @Inject
    private VurderArbeidsforholdTjeneste vurderArbeidsforholdTjeneste;
    @Inject
    private IAYRepositoryProvider provider;
    @Mock
    private VirksomhetTjeneste virksomhetTjeneste;
    @Mock
    private DokumentArkivTjeneste dokumentArkivTjeneste;
    @Mock
    private PersonIdentTjeneste personIdentTjeneste;

    private final InntektArbeidYtelseTjeneste iayTjeneste = new AbakusInMemoryInntektArbeidYtelseTjeneste();
    private final InntektsmeldingTjeneste inntektsmeldingTjeneste = new InntektsmeldingTjeneste(iayTjeneste);

    private AvklarArbeidPermisjonUtenSluttdatoOppdaterer avklarArbeidPermisjonUtenSluttdatoOppdaterer;

    @BeforeEach
    void setUp(EntityManager entityManager) {
        provider = new IAYRepositoryProvider(entityManager);

        var arbeidsgiverTjeneste = new ArbeidsgiverTjeneste(personIdentTjeneste, virksomhetTjeneste);
        var arbeidsforholdAdministrasjonTjeneste = new ArbeidsforholdAdministrasjonTjeneste(
            vurderArbeidsforholdTjeneste, inntektsmeldingTjeneste, iayTjeneste);

        var historikkRepository = new HistorikkRepository(entityManager);
        var historikkAdapter = new HistorikkTjenesteAdapter(historikkRepository, dokumentArkivTjeneste,
            provider.getBehandlingRepository());
        var arbeidsforholdHistorikkTjeneste = new ArbeidsforholdHistorikkTjeneste(historikkAdapter, arbeidsgiverTjeneste);

        avklarArbeidPermisjonUtenSluttdatoOppdaterer = new AvklarArbeidPermisjonUtenSluttdatoOppdaterer(arbeidsforholdAdministrasjonTjeneste, arbeidsforholdHistorikkTjeneste);
    }

    @Test
    public void bekrefte_avklart_permisjon_uten_sluttdato() {
        // Arrange
        var scenario = IAYScenarioBuilder.morSøker(FagsakYtelseType.FORELDREPENGER);
        var behandling = scenario.lagre(provider);
        opprettIAYAggregat(behandling, false, LocalDate.of(2021, 1, 1));

        var aksjonspunkt = AksjonspunktTestSupport.leggTilAksjonspunkt(behandling,
            AksjonspunktDefinisjon.VURDER_PERMISJON_UTEN_SLUTTDATO);

        var bekreftetArbeidMedPermisjonUtenSluttdato = new BekreftArbeidMedPermisjonUtenSluttdatoDto("Har tatt stilling til dette",
            List.of(new AvklarPermisjonUtenSluttdatoDto(NAV_ORGNR, INTERN_ARBEIDSFORHOLD_ID, BekreftetPermisjonStatus.BRUK_PERMISJON),
                new AvklarPermisjonUtenSluttdatoDto(KUNSTIG_ORG, INTERN_ARBEIDSFORHOLD_ID_2, BekreftetPermisjonStatus.IKKE_BRUK_PERMISJON)));

        var skjæringstidspunkt = Skjæringstidspunkt.builder()
            .medUtledetSkjæringstidspunkt(LocalDate.of(2019, 1, 1))
            .build();
        //Act
        var resultat = avklarArbeidPermisjonUtenSluttdatoOppdaterer.oppdater(bekreftetArbeidMedPermisjonUtenSluttdato, new AksjonspunktOppdaterParameter(behandling, aksjonspunkt, skjæringstidspunkt,
            bekreftetArbeidMedPermisjonUtenSluttdato.getBegrunnelse()));

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

    private void opprettIAYAggregat(Behandling behandling, boolean medArbeidsforholdRef, LocalDate fom) {
        var tom = AbstractLocalDateInterval.TIDENES_ENDE;
        var yrkesaktivitetBuilder = YrkesaktivitetBuilder.oppdatere(Optional.empty());
        var aktivitetsAvtaleBuilder = yrkesaktivitetBuilder.leggTilPermisjon(byggPermisjon(yrkesaktivitetBuilder, LocalDate.now(), Tid.TIDENES_ENDE )).getAktivitetsAvtaleBuilder();
        var aktivitetsAvtale = aktivitetsAvtaleBuilder.medPeriode(
            DatoIntervallEntitet.fraOgMedTilOgMed(fom, tom)).medProsentsats(BigDecimal.valueOf(100));
        var ansettelsesperiode = yrkesaktivitetBuilder.getAktivitetsAvtaleBuilder()
            .medPeriode(DatoIntervallEntitet.fraOgMedTilOgMed(fom, tom));
        yrkesaktivitetBuilder.medArbeidType(ArbeidType.ORDINÆRT_ARBEIDSFORHOLD)
            .medArbeidsgiver(Arbeidsgiver.virksomhet(NAV_ORGNR))
            .medArbeidsforholdId(medArbeidsforholdRef ? InternArbeidsforholdRef.ref(INTERN_ARBEIDSFORHOLD_ID): null)
            .leggTilAktivitetsAvtale(aktivitetsAvtale)
            .leggTilAktivitetsAvtale(ansettelsesperiode);
        var builder = InntektArbeidYtelseAggregatBuilder.oppdatere(Optional.empty(),
            VersjonType.REGISTER);
        var aktørArbeidBuilder = builder.getAktørArbeidBuilder(
            behandling.getAktørId());
        aktørArbeidBuilder.leggTilYrkesaktivitet(yrkesaktivitetBuilder);
        builder.leggTilAktørArbeid(aktørArbeidBuilder);
        iayTjeneste.lagreIayAggregat(behandling.getId(), builder);
    }

    private Permisjon byggPermisjon(YrkesaktivitetBuilder yrkesaktivitetBuilder, LocalDate fom, LocalDate tom) {
        return yrkesaktivitetBuilder.getPermisjonBuilder()
            .medProsentsats(BigDecimal.valueOf(100))
            .medPeriode(fom, tom)
            .medPermisjonsbeskrivelseType(PermisjonsbeskrivelseType.PERMITTERING)
            .build();
    }

}
