package no.nav.foreldrepenger.domene.arbeidsforhold.impl;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.Mockito.when;

import java.time.LocalDate;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import javax.persistence.EntityManager;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mockito;

import no.nav.foreldrepenger.behandling.BehandlingReferanse;
import no.nav.foreldrepenger.behandlingslager.behandling.Behandling;
import no.nav.foreldrepenger.behandlingslager.behandling.familiehendelse.FamilieHendelseBuilder;
import no.nav.foreldrepenger.behandlingslager.behandling.familiehendelse.FamilieHendelseEntitet;
import no.nav.foreldrepenger.behandlingslager.behandling.søknad.SøknadEntitet;
import no.nav.foreldrepenger.behandlingslager.fagsak.FagsakYtelseType;
import no.nav.foreldrepenger.behandlingslager.virksomhet.Arbeidsgiver;
import no.nav.foreldrepenger.dbstoette.FPsakEntityManagerAwareExtension;
import no.nav.foreldrepenger.domene.arbeidsforhold.testutilities.behandling.IAYRepositoryProvider;
import no.nav.foreldrepenger.domene.arbeidsforhold.testutilities.behandling.IAYScenarioBuilder;
import no.nav.foreldrepenger.domene.typer.AktørId;
import no.nav.foreldrepenger.domene.typer.InternArbeidsforholdRef;

@ExtendWith(FPsakEntityManagerAwareExtension.class)
public class PåkrevdeInntektsmeldingerTjenesteTest {

    private final InntektsmeldingRegisterTjeneste inntektsmeldingArkivTjeneste = Mockito.mock(InntektsmeldingRegisterTjeneste.class);

    private IAYRepositoryProvider repositoryProvider;
    private PåkrevdeInntektsmeldingerTjeneste påkrevdeInntektsmeldingerTjeneste;
    private Map<Arbeidsgiver, Set<ArbeidsforholdMedÅrsak>> result;

    @BeforeEach
    void setUp(EntityManager entityManager) {
        repositoryProvider = new IAYRepositoryProvider(entityManager);
        påkrevdeInntektsmeldingerTjeneste = new PåkrevdeInntektsmeldingerTjeneste(inntektsmeldingArkivTjeneste,
                repositoryProvider.getSøknadRepository());
        result = new HashMap<>();
    }

    @Test
    public void skal_returne_tomt_result_hvis_endringssøknad() {

        // Arrange
        IAYScenarioBuilder scenario = IAYScenarioBuilder.morSøker(FagsakYtelseType.FORELDREPENGER);
        Behandling behandling = scenario.lagre(repositoryProvider);
        lagreSøknad(behandling, true);

        Arbeidsgiver virksomhet = Arbeidsgiver.virksomhet("999999999");
        InternArbeidsforholdRef ref = InternArbeidsforholdRef.nyRef();

        Map<Arbeidsgiver, Set<InternArbeidsforholdRef>> arbeidsforhold = Map.of(virksomhet, Set.of(ref));
        when(inntektsmeldingArkivTjeneste.utledManglendeInntektsmeldingerFraGrunnlagForVurdering(any(), anyBoolean())).thenReturn(arbeidsforhold);

        // Act
        påkrevdeInntektsmeldingerTjeneste.leggTilArbeidsforholdHvorPåkrevdeInntektsmeldingMangler(BehandlingReferanse.fra(behandling), result);

        // Assert
        assertThat(result).isEmpty();

    }

    @Test
    public void skal_legge_til_arbeidsforhold_for_virksomhet() {

        // Arrange
        IAYScenarioBuilder scenario = IAYScenarioBuilder.morSøker(FagsakYtelseType.FORELDREPENGER);
        Behandling behandling = scenario.lagre(repositoryProvider);
        lagreSøknad(behandling, false);

        Arbeidsgiver virksomhet = Arbeidsgiver.virksomhet("999999999");
        InternArbeidsforholdRef ref1 = InternArbeidsforholdRef.nyRef();
        InternArbeidsforholdRef ref2 = InternArbeidsforholdRef.nyRef();

        Map<Arbeidsgiver, Set<InternArbeidsforholdRef>> arbeidsforhold = Map.of(virksomhet, Set.of(ref1, ref2));
        when(inntektsmeldingArkivTjeneste.utledManglendeInntektsmeldingerFraGrunnlagForVurdering(any(), anyBoolean())).thenReturn(arbeidsforhold);

        // Act
        påkrevdeInntektsmeldingerTjeneste.leggTilArbeidsforholdHvorPåkrevdeInntektsmeldingMangler(BehandlingReferanse.fra(behandling), result);

        // Assert
        assertMap(virksomhet, ref1, ref2);

    }

    @Test
    public void skal_legge_til_arbeidsforhold_for_personlig_foretak() {

        // Arrange
        IAYScenarioBuilder scenario = IAYScenarioBuilder.morSøker(FagsakYtelseType.FORELDREPENGER);
        Behandling behandling = scenario.lagre(repositoryProvider);
        lagreSøknad(behandling, false);

        Arbeidsgiver person = Arbeidsgiver.person(AktørId.dummy());
        InternArbeidsforholdRef ref1 = InternArbeidsforholdRef.nyRef();
        InternArbeidsforholdRef ref2 = InternArbeidsforholdRef.nyRef();

        Map<Arbeidsgiver, Set<InternArbeidsforholdRef>> arbeidsforhold = Map.of(person, Set.of(ref1, ref2));
        when(inntektsmeldingArkivTjeneste.utledManglendeInntektsmeldingerFraGrunnlagForVurdering(any(), anyBoolean())).thenReturn(arbeidsforhold);

        // Act
        påkrevdeInntektsmeldingerTjeneste.leggTilArbeidsforholdHvorPåkrevdeInntektsmeldingMangler(BehandlingReferanse.fra(behandling), result);

        // Assert
        assertMap(person, ref1, ref2);

    }

    private void lagreSøknad(Behandling behandling, boolean erEndringssøknad) {
        byggFamilieHendelse(behandling);
        SøknadEntitet søknad = new SøknadEntitet.Builder()
                .medElektroniskRegistrert(true)
                .medSøknadsdato(LocalDate.now())
                .medMottattDato(LocalDate.now())
                .medErEndringssøknad(erEndringssøknad)
                .build();
        repositoryProvider.getSøknadRepository().lagreOgFlush(behandling, søknad);
    }

    private FamilieHendelseEntitet byggFamilieHendelse(Behandling behandling) {
        FamilieHendelseBuilder søknadHendelse = repositoryProvider.getFamilieHendelseRepository()
                .opprettBuilderFor(behandling)
                .medAntallBarn(1);
        søknadHendelse.medTerminbekreftelse(søknadHendelse.getTerminbekreftelseBuilder()
                .medTermindato(LocalDate.now())
                .medUtstedtDato(LocalDate.now()));
        repositoryProvider.getFamilieHendelseRepository().lagre(behandling, søknadHendelse);
        return repositoryProvider.getFamilieHendelseRepository().hentAggregat(behandling.getId()).getSøknadVersjon();
    }

    private void assertMap(Arbeidsgiver arbeidsgiver, InternArbeidsforholdRef ref1, InternArbeidsforholdRef ref2) {
        assertThat(result).hasEntrySatisfying(arbeidsgiver, årsaker -> {
            assertThat(årsaker).hasSize(2);
            assertThat(årsaker).anySatisfy(årsak -> {
                assertThat(årsak.getRef()).isEqualTo(ref1);
                assertThat(årsak.getÅrsaker()).containsExactlyInAnyOrder(AksjonspunktÅrsak.MANGLENDE_INNTEKTSMELDING);
            });
            assertThat(årsaker).anySatisfy(årsak -> {
                assertThat(årsak.getRef()).isEqualTo(ref2);
                assertThat(årsak.getÅrsaker()).containsExactlyInAnyOrder(AksjonspunktÅrsak.MANGLENDE_INNTEKTSMELDING);
            });
        });
    }

}
