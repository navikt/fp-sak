package no.nav.foreldrepenger.web.app.tjenester.behandling.vedtak;

import static no.nav.foreldrepenger.behandlingslager.virksomhet.OrgNummer.KUNSTIG_ORG;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.mockito.MockitoAnnotations.initMocks;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import javax.inject.Inject;
import javax.persistence.EntityManager;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mockito;

import no.nav.foreldrepenger.behandlingslager.behandling.Behandling;
import no.nav.foreldrepenger.behandlingslager.behandling.aksjonspunkt.AksjonspunktDefinisjon;
import no.nav.foreldrepenger.behandlingslager.behandling.repository.BehandlingRepositoryProvider;
import no.nav.foreldrepenger.behandlingslager.testutilities.behandling.AbstractTestScenario;
import no.nav.foreldrepenger.behandlingslager.testutilities.behandling.ScenarioMorSøkerForeldrepenger;
import no.nav.foreldrepenger.behandlingslager.virksomhet.Arbeidsgiver;
import no.nav.foreldrepenger.behandlingslager.virksomhet.Virksomhet;
import no.nav.foreldrepenger.dbstoette.UnittestRepositoryRule;
import no.nav.foreldrepenger.domene.arbeidsforhold.InntektArbeidYtelseTjeneste;
import no.nav.foreldrepenger.domene.arbeidsforhold.impl.ArbeidsforholdAdministrasjonTjeneste;
import no.nav.foreldrepenger.domene.arbeidsgiver.ArbeidsgiverOpplysninger;
import no.nav.foreldrepenger.domene.arbeidsgiver.ArbeidsgiverTjeneste;
import no.nav.foreldrepenger.domene.iay.modell.ArbeidsforholdInformasjonBuilder;
import no.nav.foreldrepenger.domene.iay.modell.ArbeidsforholdOverstyringBuilder;
import no.nav.foreldrepenger.domene.iay.modell.BekreftetPermisjon;
import no.nav.foreldrepenger.domene.iay.modell.kodeverk.ArbeidsforholdHandlingType;
import no.nav.foreldrepenger.domene.iay.modell.kodeverk.BekreftetPermisjonStatus;
import no.nav.foreldrepenger.domene.typer.AktørId;
import no.nav.foreldrepenger.domene.typer.InternArbeidsforholdRef;
import no.nav.foreldrepenger.produksjonsstyring.totrinn.Totrinnsvurdering;
import no.nav.foreldrepenger.web.app.tjenester.behandling.vedtak.app.TotrinnArbeidsforholdDtoTjeneste;
import no.nav.foreldrepenger.web.app.tjenester.behandling.vedtak.dto.TotrinnsArbeidsforholdDto;
import no.nav.vedtak.felles.testutilities.cdi.CdiRunner;
import no.nav.vedtak.felles.testutilities.db.RepositoryRule;

@RunWith(CdiRunner.class)
public class TotrinnArbeidsforholdDtoTjenesteTest {

    private static final InternArbeidsforholdRef ARBEIDSFORHOLD_ID = InternArbeidsforholdRef.namedRef("TEST-REF");
    private static final String ORGNR = KUNSTIG_ORG;
    private static final String PRIVATPERSON_NAVN = "Mikke Mus";

    @Rule
    public final RepositoryRule repoRule = new UnittestRepositoryRule();
    private final EntityManager entityManager = repoRule.getEntityManager();
    private final BehandlingRepositoryProvider repositoryProvider = new BehandlingRepositoryProvider(entityManager);

    @Inject
    private InntektArbeidYtelseTjeneste iayTjeneste;

    @Inject
    private ArbeidsforholdAdministrasjonTjeneste arbeidsforholdTjeneste;

    private TotrinnArbeidsforholdDtoTjeneste totrinnArbeidsforholdDtoTjeneste;
    private Virksomhet virksomhet = getVirksomheten();
    private Behandling behandling;
    private Totrinnsvurdering vurdering;

    @Before
    public void setup() {
        initMocks(this);
        ArbeidsgiverTjeneste arbeidsgiverTjeneste = mock(ArbeidsgiverTjeneste.class);
        when(arbeidsgiverTjeneste.hent(Mockito.any())).thenReturn(new ArbeidsgiverOpplysninger(null, PRIVATPERSON_NAVN));
        when(arbeidsgiverTjeneste.hentVirksomhet(Mockito.any())).thenReturn(virksomhet);
        totrinnArbeidsforholdDtoTjeneste = new TotrinnArbeidsforholdDtoTjeneste(iayTjeneste, arbeidsgiverTjeneste);
        ScenarioMorSøkerForeldrepenger scenario = ScenarioMorSøkerForeldrepenger.forFødsel();
        behandling = lagre(scenario);
        Totrinnsvurdering.Builder vurderingBuilder = new Totrinnsvurdering.Builder(behandling, AksjonspunktDefinisjon.VURDER_ARBEIDSFORHOLD);
        vurdering = vurderingBuilder.medGodkjent(true).medBegrunnelse("").build();
    }
    private Behandling lagre(AbstractTestScenario<?> scenario) {
        return scenario.lagre(repositoryProvider);
    }

    @Test
    public void skal_opprette_arbeidsforholdDto_for_virksomhet_som_arbeidsgiver_med_bekreftet_permisjon_med_status_BRUK_PERMISJON() {
        // Arrange
        BekreftetPermisjon bekreftetPermisjon = new BekreftetPermisjon(LocalDate.now(), LocalDate.now(), BekreftetPermisjonStatus.BRUK_PERMISJON);
        opprettArbeidsforholdInformasjon(Arbeidsgiver.virksomhet(ORGNR), Optional.of(bekreftetPermisjon));
        // Act
        List<TotrinnsArbeidsforholdDto> dtoer = totrinnArbeidsforholdDtoTjeneste.hentArbeidsforhold(behandling, vurdering, Optional.empty());
        // Assert
        assertThat(dtoer).hasSize(1);
        assertThat(dtoer.get(0).getNavn()).isEqualTo("Virksomheten");
        assertThat(dtoer.get(0).getOrganisasjonsnummer()).isEqualTo(ORGNR);
        assertThat(dtoer.get(0).getBrukPermisjon()).isTrue();
    }

    @Test
    public void skal_opprette_arbeidsforholdDto_for_privatperson_som_arbeidsgiver_med_bekreftet_permisjon_med_status_IKKE_BRUK_PERMISJON() {
        // Arrange
        BekreftetPermisjon bekreftetPermisjon = new BekreftetPermisjon(LocalDate.now(), LocalDate.now(), BekreftetPermisjonStatus.IKKE_BRUK_PERMISJON);
        opprettArbeidsforholdInformasjon(Arbeidsgiver.person(AktørId.dummy()), Optional.of(bekreftetPermisjon));
        // Act
        List<TotrinnsArbeidsforholdDto> dtoer = totrinnArbeidsforholdDtoTjeneste.hentArbeidsforhold(behandling, vurdering, Optional.empty());
        // Assert
        assertThat(dtoer).hasSize(1);
        assertThat(dtoer.get(0).getNavn()).isEqualTo("Mikke Mus");
        assertThat(dtoer.get(0).getBrukPermisjon()).isFalse();
    }

    @Test
    public void skal_opprette_arbeidsforholdDto_for_privatperson_som_arbeidsgiver_med_bekreftet_permisjon_med_status_UGYLDIGE_PERIODER() {
        // Arrange
        BekreftetPermisjon bekreftetPermisjon = new BekreftetPermisjon(LocalDate.now(), LocalDate.now(), BekreftetPermisjonStatus.UGYLDIGE_PERIODER);
        opprettArbeidsforholdInformasjon(Arbeidsgiver.person(AktørId.dummy()), Optional.of(bekreftetPermisjon));
        // Act
        List<TotrinnsArbeidsforholdDto> dtoer = totrinnArbeidsforholdDtoTjeneste.hentArbeidsforhold(behandling, vurdering, Optional.empty());
        // Assert
        assertThat(dtoer).hasSize(1);
        assertThat(dtoer.get(0).getNavn()).isEqualTo("Mikke Mus");
        assertThat(dtoer.get(0).getBrukPermisjon()).isFalse();
    }

    @Test
    public void skal_opprette_arbeidsforholdDto_for_privatperson_som_arbeidsgiver_uten_bekreftet_permisjon() {
        // Arrange
        opprettArbeidsforholdInformasjon(Arbeidsgiver.person(AktørId.dummy()), Optional.empty());
        // Act
        List<TotrinnsArbeidsforholdDto> dtoer = totrinnArbeidsforholdDtoTjeneste.hentArbeidsforhold(behandling, vurdering, Optional.empty());
        // Assert
        assertThat(dtoer).hasSize(1);
        assertThat(dtoer.get(0).getNavn()).isEqualTo("Mikke Mus");
        assertThat(dtoer.get(0).getBrukPermisjon()).isNull();
    }

    private void opprettArbeidsforholdInformasjon(Arbeidsgiver arbeidsgiver, Optional<BekreftetPermisjon> bekreftetPermisjon) {
        ArbeidsforholdInformasjonBuilder informasjonBuilder = arbeidsforholdTjeneste.opprettBuilderFor(behandling.getId());
        ArbeidsforholdOverstyringBuilder overstyringBuilder = informasjonBuilder.getOverstyringBuilderFor(arbeidsgiver, ARBEIDSFORHOLD_ID);
        overstyringBuilder.medHandling(ArbeidsforholdHandlingType.BRUK);
        bekreftetPermisjon.ifPresent(overstyringBuilder::medBekreftetPermisjon);
        overstyringBuilder.medArbeidsgiver(arbeidsgiver);
        informasjonBuilder.leggTil(overstyringBuilder);
        arbeidsforholdTjeneste.lagre(behandling.getId(), behandling.getAktørId(), informasjonBuilder);
    }

    private Virksomhet getVirksomheten() {
        return new Virksomhet.Builder()
            .medOrgnr(ORGNR)
            .medNavn("Virksomheten")
            .medRegistrert(LocalDate.now().minusYears(2L))
            .medOppstart(LocalDate.now().minusYears(1L))
            .build();
    }
}
