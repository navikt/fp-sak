package no.nav.foreldrepenger.domene.registerinnhenting.impl.behandlingårsak;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.mockito.MockitoAnnotations.initMocks;

import java.time.LocalDate;

import javax.enterprise.inject.Any;
import javax.enterprise.inject.Instance;
import javax.inject.Inject;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;

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
import no.nav.foreldrepenger.behandlingslager.testutilities.behandling.ScenarioMorSøkerForeldrepenger;
import no.nav.foreldrepenger.dbstoette.UnittestRepositoryRule;
import no.nav.foreldrepenger.domene.iay.modell.InntektArbeidYtelseGrunnlag;
import no.nav.foreldrepenger.domene.registerinnhenting.BehandlingÅrsakTjeneste;
import no.nav.foreldrepenger.domene.registerinnhenting.EndringsresultatSjekker;
import no.nav.foreldrepenger.domene.registerinnhenting.impl.RegisterinnhentingHistorikkinnslagTjeneste;
import no.nav.foreldrepenger.domene.typer.AktørId;
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
    @Mock
    private RegisterinnhentingHistorikkinnslagTjeneste historikkinnslagTjeneste;

    private Skjæringstidspunkt skjæringstidspunkt = Skjæringstidspunkt.builder().medUtledetSkjæringstidspunkt(LocalDate.now()).build();

    @Before
    public void setup() {
        initMocks(this);

        tjeneste = new BehandlingÅrsakTjeneste(utledere, endringsresultatSjekker, historikkinnslagTjeneste);
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

        // Assert
        tjeneste.lagHistorikkForRegisterEndringsResultat(behandling, endringsresultat);

        // Assert
        verifyNoInteractions(historikkinnslagTjeneste);
    }

    @Test
    public void test_behandlingsårsaker_når_endring_i_familiehendelse() {
        EndringsresultatDiff endringsresultat = EndringsresultatDiff.opprett();
        when(diffResult.isEmpty()).thenReturn(false); // Indikerer at det finnes diff
        endringsresultat.leggTilSporetEndring(EndringsresultatDiff.medDiff(FamilieHendelseGrunnlagEntitet.class, 1L, 2L), () -> diffResult);

        // Assert
        tjeneste.lagHistorikkForRegisterEndringsResultat(behandling, endringsresultat);

        // Assert
        verify(historikkinnslagTjeneste).opprettHistorikkinnslagForNyeRegisteropplysninger(any());
    }

    @Test
    public void test_behandlingsårsaker_når_endring_dødsdato_søker() {
        final LocalDate dødsdato = LocalDate.now().minusDays(10);
        PersonopplysningGrunnlagEntitet personopplysningGrunnlag1 = opprettPersonopplysningGrunnlag(null);
        PersonopplysningGrunnlagEntitet personopplysningGrunnlag2 = opprettPersonopplysningGrunnlag(dødsdato);

        EndringsresultatDiff endringsresultat = EndringsresultatDiff.opprett();
        when(diffResult.isEmpty()).thenReturn(false); // Indikerer at det finnes diff
        endringsresultat.leggTilSporetEndring(EndringsresultatDiff.medDiff(PersonInformasjonEntitet.class, personopplysningGrunnlag1.getId(), personopplysningGrunnlag2.getId()), () -> diffResult);

        // Assert
        tjeneste.lagHistorikkForRegisterEndringsResultat(behandling, endringsresultat);

        // Assert
        verify(historikkinnslagTjeneste).opprettHistorikkinnslagForBehandlingMedNyeOpplysninger(any(), eq(BehandlingÅrsakType.RE_OPPLYSNINGER_OM_DØD));
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
