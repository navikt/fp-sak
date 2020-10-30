package no.nav.foreldrepenger.domene.registerinnhenting.impl.behandlingårsak;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.time.LocalDate;

import javax.enterprise.inject.spi.CDI;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import no.nav.foreldrepenger.behandling.Skjæringstidspunkt;
import no.nav.foreldrepenger.behandlingslager.aktør.NavBrukerKjønn;
import no.nav.foreldrepenger.behandlingslager.behandling.Behandling;
import no.nav.foreldrepenger.behandlingslager.behandling.BehandlingType;
import no.nav.foreldrepenger.behandlingslager.behandling.BehandlingÅrsakType;
import no.nav.foreldrepenger.behandlingslager.behandling.EndringsresultatDiff;
import no.nav.foreldrepenger.behandlingslager.behandling.familiehendelse.FamilieHendelseGrunnlagEntitet;
import no.nav.foreldrepenger.behandlingslager.behandling.familiehendelse.FamilieHendelseRepository;
import no.nav.foreldrepenger.behandlingslager.behandling.medlemskap.MedlemskapAggregat;
import no.nav.foreldrepenger.behandlingslager.behandling.personopplysning.PersonInformasjonBuilder;
import no.nav.foreldrepenger.behandlingslager.behandling.personopplysning.PersonInformasjonEntitet;
import no.nav.foreldrepenger.behandlingslager.behandling.personopplysning.PersonopplysningGrunnlagEntitet;
import no.nav.foreldrepenger.behandlingslager.behandling.personopplysning.PersonopplysningRepository;
import no.nav.foreldrepenger.behandlingslager.behandling.repository.BehandlingRepositoryProvider;
import no.nav.foreldrepenger.behandlingslager.behandling.ytelsefordeling.AvklarteUttakDatoerEntitet;
import no.nav.foreldrepenger.behandlingslager.behandling.ytelsefordeling.YtelseFordelingAggregat;
import no.nav.foreldrepenger.behandlingslager.behandling.ytelsefordeling.YtelsesFordelingRepository;
import no.nav.foreldrepenger.behandlingslager.diff.DiffResult;
import no.nav.foreldrepenger.behandlingslager.testutilities.behandling.ScenarioMorSøkerForeldrepenger;
import no.nav.foreldrepenger.dbstoette.EntityManagerAwareTest;
import no.nav.foreldrepenger.domene.abakus.AbakusInMemoryInntektArbeidYtelseTjeneste;
import no.nav.foreldrepenger.domene.iay.modell.InntektArbeidYtelseGrunnlag;
import no.nav.foreldrepenger.domene.medlem.MedlemTjeneste;
import no.nav.foreldrepenger.domene.personopplysning.PersonopplysningTjeneste;
import no.nav.foreldrepenger.domene.registerinnhenting.BehandlingÅrsakTjeneste;
import no.nav.foreldrepenger.domene.registerinnhenting.EndringsresultatSjekker;
import no.nav.foreldrepenger.domene.registerinnhenting.impl.RegisterinnhentingHistorikkinnslagTjeneste;
import no.nav.foreldrepenger.domene.typer.AktørId;
import no.nav.foreldrepenger.domene.ytelsefordeling.YtelseFordelingTjeneste;
import no.nav.foreldrepenger.familiehendelse.FamilieHendelseTjeneste;
import no.nav.foreldrepenger.skjæringstidspunkt.SkjæringstidspunktTjeneste;

@ExtendWith(MockitoExtension.class)
public class BehandlingÅrsakTjenesteTest extends EntityManagerAwareTest {

    private final AktørId AKTØRID = AktørId.dummy();

    private BehandlingRepositoryProvider repositoryProvider;

    private BehandlingÅrsakTjeneste tjeneste;

    @Mock
    private DiffResult diffResult;
    @Mock
    private RegisterinnhentingHistorikkinnslagTjeneste historikkinnslagTjeneste;
    @Mock
    private SkjæringstidspunktTjeneste skjæringstidspunktTjeneste;
    @Mock
    private MedlemTjeneste medlemTjeneste;

    private final Skjæringstidspunkt skjæringstidspunkt = Skjæringstidspunkt.builder().medUtledetSkjæringstidspunkt(LocalDate.now()).build();

    @BeforeEach
    public void setup() {
        repositoryProvider = new BehandlingRepositoryProvider(getEntityManager());
        var personopplysningRepository = new PersonopplysningRepository(getEntityManager());
        var personopplysningTjeneste = new PersonopplysningTjeneste(personopplysningRepository);
        var familieHendelseTjeneste = new FamilieHendelseTjeneste(null, new FamilieHendelseRepository(getEntityManager()));
        var ytelsesFordelingRepository = new YtelsesFordelingRepository(getEntityManager());
        var ytelseFordelingTjeneste = new YtelseFordelingTjeneste(ytelsesFordelingRepository);
        var endringsresultatSjekker = new EndringsresultatSjekker(personopplysningTjeneste,
            familieHendelseTjeneste, medlemTjeneste, new AbakusInMemoryInntektArbeidYtelseTjeneste(), ytelseFordelingTjeneste);
        when(skjæringstidspunktTjeneste.getSkjæringstidspunkter(any())).thenReturn(skjæringstidspunkt);
        var utledere = CDI.current().select(BehandlingÅrsakUtleder.class);
        tjeneste = new BehandlingÅrsakTjeneste(utledere, endringsresultatSjekker, historikkinnslagTjeneste, skjæringstidspunktTjeneste);
    }

    private Behandling opprettBehandling() {
        ScenarioMorSøkerForeldrepenger scenario = ScenarioMorSøkerForeldrepenger.forFødsel()
            .medBruker(AKTØRID, NavBrukerKjønn.KVINNE)
            .medBehandlingType(BehandlingType.FØRSTEGANGSSØKNAD);
        scenario.medSøknadHendelse().medAntallBarn(1).medFødselsDato(LocalDate.now().minusMonths(1));
        scenario.medAvklarteUttakDatoer(new AvklarteUttakDatoerEntitet.Builder().medFørsteUttaksdato(LocalDate.now().minusMonths(1)).build());
        return scenario.lagre(repositoryProvider);
    }

    @Test
    public void test_skal_ikke_returnere_behandlingsårsaker_hvis_ikke_endringer() {
        var behandling = opprettBehandling();

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
        var behandling = opprettBehandling();
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
        var behandling = opprettBehandling();
        final LocalDate dødsdato = LocalDate.now().minusDays(10);
        PersonopplysningGrunnlagEntitet personopplysningGrunnlag1 = opprettPersonopplysningGrunnlag(behandling, null);
        PersonopplysningGrunnlagEntitet personopplysningGrunnlag2 = opprettPersonopplysningGrunnlag(behandling, dødsdato);

        EndringsresultatDiff endringsresultat = EndringsresultatDiff.opprett();
        when(diffResult.isEmpty()).thenReturn(false); // Indikerer at det finnes diff
        endringsresultat.leggTilSporetEndring(EndringsresultatDiff.medDiff(PersonInformasjonEntitet.class, personopplysningGrunnlag1.getId(), personopplysningGrunnlag2.getId()), () -> diffResult);

        // Assert
        tjeneste.lagHistorikkForRegisterEndringsResultat(behandling, endringsresultat);

        // Assert
        verify(historikkinnslagTjeneste).opprettHistorikkinnslagForBehandlingMedNyeOpplysninger(any(), eq(BehandlingÅrsakType.RE_OPPLYSNINGER_OM_DØD));
    }

    private PersonopplysningGrunnlagEntitet opprettPersonopplysningGrunnlag(Behandling behandling, LocalDate dødsdato) {
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
