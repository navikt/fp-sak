package no.nav.foreldrepenger.domene.registerinnhenting.impl.behandlingårsak;

import static org.assertj.core.api.Java6Assertions.assertThat;
import static org.mockito.Mockito.when;
import static org.mockito.MockitoAnnotations.initMocks;

import java.time.LocalDate;
import java.util.Set;

import javax.enterprise.inject.Any;
import javax.enterprise.inject.Instance;
import javax.inject.Inject;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;

import no.nav.foreldrepenger.behandling.BehandlingReferanse;
import no.nav.foreldrepenger.behandling.Skjæringstidspunkt;
import no.nav.foreldrepenger.behandlingslager.aktør.NavBrukerKjønn;
import no.nav.foreldrepenger.behandlingslager.behandling.Behandling;
import no.nav.foreldrepenger.behandlingslager.behandling.BehandlingType;
import no.nav.foreldrepenger.behandlingslager.behandling.BehandlingÅrsakType;
import no.nav.foreldrepenger.behandlingslager.behandling.EndringsresultatDiff;
import no.nav.foreldrepenger.behandlingslager.behandling.familiehendelse.FamilieHendelseGrunnlagEntitet;
import no.nav.foreldrepenger.behandlingslager.behandling.medlemskap.MedlemskapAggregat;
import no.nav.foreldrepenger.behandlingslager.behandling.personopplysning.PersonInformasjonBuilder;
import no.nav.foreldrepenger.behandlingslager.behandling.personopplysning.PersonInformasjonEntitet;
import no.nav.foreldrepenger.behandlingslager.behandling.personopplysning.PersonopplysningGrunnlagEntitet;
import no.nav.foreldrepenger.behandlingslager.behandling.personopplysning.PersonopplysningRepository;
import no.nav.foreldrepenger.behandlingslager.behandling.repository.BehandlingRepositoryProvider;
import no.nav.foreldrepenger.behandlingslager.behandling.ytelsefordeling.AvklarteUttakDatoerEntitet;
import no.nav.foreldrepenger.behandlingslager.behandling.ytelsefordeling.YtelseFordelingAggregat;
import no.nav.foreldrepenger.behandlingslager.diff.DiffResult;
import no.nav.foreldrepenger.dbstoette.UnittestRepositoryRule;
import no.nav.foreldrepenger.domene.iay.modell.InntektArbeidYtelseGrunnlag;
import no.nav.foreldrepenger.domene.registerinnhenting.BehandlingÅrsakTjeneste;
import no.nav.foreldrepenger.domene.registerinnhenting.EndringsresultatSjekker;
import no.nav.foreldrepenger.domene.typer.AktørId;
import no.nav.foreldrepenger.behandlingslager.testutilities.behandling.ScenarioMorSøkerForeldrepenger;
import no.nav.vedtak.felles.testutilities.cdi.CdiRunner;

@RunWith(CdiRunner.class)
public class BehandlingÅrsakTjenesteTest {

    private AktørId AKTØRID = AktørId.dummy();

    @Rule
    public final UnittestRepositoryRule repoRule = new UnittestRepositoryRule();
    private BehandlingRepositoryProvider repositoryProvider = new BehandlingRepositoryProvider(repoRule.getEntityManager());

    private BehandlingÅrsakTjeneste tjeneste;

    @Inject
    private EndringsresultatSjekker endringsresultatSjekker;
    private Behandling behandling;

    @Inject
    @Any
    Instance<BehandlingÅrsakUtleder> utledere;

    @Mock
    private DiffResult diffResult;

    private Skjæringstidspunkt skjæringstidspunkt = Skjæringstidspunkt.builder().medUtledetSkjæringstidspunkt(LocalDate.now()).build();

    @Before
    public void setup() {
        initMocks(this);

        tjeneste = new BehandlingÅrsakTjeneste(utledere, endringsresultatSjekker);
        ScenarioMorSøkerForeldrepenger scenario = ScenarioMorSøkerForeldrepenger.forFødsel()
            .medBruker(AKTØRID, NavBrukerKjønn.KVINNE)
            .medBehandlingType(BehandlingType.FØRSTEGANGSSØKNAD);
        scenario.medSøknadHendelse().medAntallBarn(1).medFødselsDato(LocalDate.now().minusMonths(1));
        scenario.medAvklarteUttakDatoer(new AvklarteUttakDatoerEntitet.Builder().medFørsteUttaksdato(LocalDate.now().minusMonths(1)).build());
        behandling = scenario.lagre(repositoryProvider);
    }

    @Test
    public void test_skal_ikke_returnere_behandlingsårsaker_hvis_ikke_endringer() {
        when(diffResult.isEmpty()).thenReturn(true); // Indikerer at det ikke finnes diff

        EndringsresultatDiff endringsresultat = EndringsresultatDiff.opprett();
        endringsresultat.leggTilSporetEndring(EndringsresultatDiff.medDiff(PersonInformasjonEntitet.class, 1L, 1L), () -> diffResult);
        endringsresultat.leggTilSporetEndring(EndringsresultatDiff.medDiff(FamilieHendelseGrunnlagEntitet.class, 1L, 1L), () -> diffResult);
        endringsresultat.leggTilSporetEndring(EndringsresultatDiff.medDiff(MedlemskapAggregat.class, 1L, 1L), () -> diffResult);
        endringsresultat.leggTilSporetEndring(EndringsresultatDiff.medDiff(InntektArbeidYtelseGrunnlag.class, 1L, 1L), () -> diffResult);
        endringsresultat.leggTilSporetEndring(EndringsresultatDiff.medDiff(YtelseFordelingAggregat.class, 1L, 1L), () -> diffResult);

        // Act/Assert
        assertThat(tjeneste.utledBehandlingÅrsakerBasertPåDiff(lagRef(behandling), endringsresultat)).isEmpty();
    }

    @Test
    public void test_behandlingsårsaker_når_endring_i_familiehendelse() {
        EndringsresultatDiff endringsresultat = EndringsresultatDiff.opprett();
        when(diffResult.isEmpty()).thenReturn(false); // Indikerer at det finnes diff
        endringsresultat.leggTilSporetEndring(EndringsresultatDiff.medDiff(FamilieHendelseGrunnlagEntitet.class, 1L, 2L), () -> diffResult);

        // Act/Assert
        Set<BehandlingÅrsakType> behandlingÅrsaker = tjeneste.utledBehandlingÅrsakerBasertPåDiff(lagRef(behandling), endringsresultat);
        assertThat(behandlingÅrsaker).hasSize(1);
        assertThat(behandlingÅrsaker).as("Begge utlederne hvor endring skal returnere samme årsak.").contains(BehandlingÅrsakType.RE_REGISTEROPPLYSNING);
    }

    @Test
    public void test_behandlingsårsaker_når_endring_dødsdato_søker() {
        final LocalDate dødsdato = LocalDate.now().minusDays(10);
        PersonopplysningGrunnlagEntitet personopplysningGrunnlag1 = opprettPersonopplysningGrunnlag(null);
        PersonopplysningGrunnlagEntitet personopplysningGrunnlag2 = opprettPersonopplysningGrunnlag(dødsdato);

        EndringsresultatDiff endringsresultat = EndringsresultatDiff.opprett();
        when(diffResult.isEmpty()).thenReturn(false); // Indikerer at det finnes diff
        endringsresultat.leggTilSporetEndring(EndringsresultatDiff.medDiff(PersonInformasjonEntitet.class, personopplysningGrunnlag1.getId(), personopplysningGrunnlag2.getId()), () -> diffResult);

        Set<BehandlingÅrsakType> behandlingÅrsaker = tjeneste.utledBehandlingÅrsakerBasertPåDiff(lagRef(behandling), endringsresultat);
        assertThat(behandlingÅrsaker).hasSize(1);
        assertThat(behandlingÅrsaker).as("Forventer behandlingsårsak").contains(BehandlingÅrsakType.RE_OPPLYSNINGER_OM_DØD);
    }

    private BehandlingReferanse lagRef(Behandling behandling) {
        return BehandlingReferanse.fra(behandling, skjæringstidspunkt);
    }

    private PersonopplysningGrunnlagEntitet opprettPersonopplysningGrunnlag(LocalDate dødsdato) {
        PersonopplysningRepository personopplysningRepository = repositoryProvider.getPersonopplysningRepository();
        Long behandlingId = behandling.getId();
        final PersonInformasjonBuilder builder = personopplysningRepository.opprettBuilderForRegisterdata(behandlingId);
        final PersonInformasjonBuilder.PersonopplysningBuilder personopplysningBuilder = builder.getPersonopplysningBuilder(behandling.getAktørId());
        personopplysningBuilder.medDødsdato(dødsdato);
        builder.leggTil(personopplysningBuilder);
        personopplysningRepository.lagre(behandlingId, builder);
        return personopplysningRepository.hentPersonopplysninger(behandlingId);
    }
}
