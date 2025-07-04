package no.nav.foreldrepenger.domene.registerinnhenting.impl.behandlingårsak;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.time.LocalDate;

import jakarta.enterprise.inject.Any;
import jakarta.enterprise.inject.Instance;
import jakarta.inject.Inject;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;

import no.nav.foreldrepenger.behandling.Skjæringstidspunkt;
import no.nav.foreldrepenger.behandlingslager.aktør.NavBrukerKjønn;
import no.nav.foreldrepenger.behandlingslager.behandling.Behandling;
import no.nav.foreldrepenger.behandlingslager.behandling.BehandlingType;
import no.nav.foreldrepenger.behandlingslager.behandling.BehandlingÅrsakType;
import no.nav.foreldrepenger.behandlingslager.behandling.EndringsresultatDiff;
import no.nav.foreldrepenger.behandlingslager.behandling.familiehendelse.FamilieHendelseGrunnlagEntitet;
import no.nav.foreldrepenger.behandlingslager.behandling.medlemskap.MedlemskapAggregat;
import no.nav.foreldrepenger.behandlingslager.behandling.personopplysning.PersonInformasjonEntitet;
import no.nav.foreldrepenger.behandlingslager.behandling.personopplysning.PersonopplysningGrunnlagEntitet;
import no.nav.foreldrepenger.behandlingslager.behandling.repository.BehandlingRepositoryProvider;
import no.nav.foreldrepenger.behandlingslager.behandling.ytelsefordeling.AvklarteUttakDatoerEntitet;
import no.nav.foreldrepenger.behandlingslager.behandling.ytelsefordeling.YtelseFordelingAggregat;
import no.nav.foreldrepenger.behandlingslager.diff.DiffResult;
import no.nav.foreldrepenger.behandlingslager.testutilities.behandling.ScenarioMorSøkerForeldrepenger;
import no.nav.foreldrepenger.dbstoette.CdiDbAwareTest;
import no.nav.foreldrepenger.domene.iay.modell.InntektArbeidYtelseGrunnlag;
import no.nav.foreldrepenger.domene.registerinnhenting.BehandlingÅrsakTjeneste;
import no.nav.foreldrepenger.domene.registerinnhenting.EndringsresultatSjekker;
import no.nav.foreldrepenger.domene.registerinnhenting.impl.RegisterinnhentingHistorikkinnslagTjeneste;
import no.nav.foreldrepenger.domene.typer.AktørId;
import no.nav.foreldrepenger.skjæringstidspunkt.SkjæringstidspunktTjeneste;

@CdiDbAwareTest
class BehandlingÅrsakTjenesteTest {

    private final AktørId AKTØRID = AktørId.dummy();
    private final Skjæringstidspunkt skjæringstidspunkt = Skjæringstidspunkt.builder()
        .medUtledetSkjæringstidspunkt(LocalDate.now())
        .build();

    @Inject
    private BehandlingRepositoryProvider repositoryProvider;
    @Inject
    private EndringsresultatSjekker endringsresultatSjekker;
    @Inject
    @Any
    private Instance<BehandlingÅrsakUtleder> utledere;

    @Mock
    private DiffResult diffResult;
    @Mock
    private RegisterinnhentingHistorikkinnslagTjeneste historikkinnslagTjeneste;
    @Mock
    private SkjæringstidspunktTjeneste skjæringstidspunktTjeneste;

    private BehandlingÅrsakTjeneste tjeneste;

    @BeforeEach
    void setup() {
        when(skjæringstidspunktTjeneste.getSkjæringstidspunkter(any())).thenReturn(skjæringstidspunkt);
        tjeneste = new BehandlingÅrsakTjeneste(utledere, endringsresultatSjekker, historikkinnslagTjeneste,
            skjæringstidspunktTjeneste);
    }

    private Behandling opprettBehandling() {
        var scenario = ScenarioMorSøkerForeldrepenger.forFødsel()
            .medBruker(AKTØRID, NavBrukerKjønn.KVINNE)
            .medBehandlingType(BehandlingType.FØRSTEGANGSSØKNAD);
        scenario.medSøknadHendelse().medAntallBarn(1).medFødselsDato(LocalDate.now().minusMonths(1));
        scenario.medAvklarteUttakDatoer(
            new AvklarteUttakDatoerEntitet.Builder().medFørsteUttaksdato(LocalDate.now().minusMonths(1)).build());
        return scenario.lagre(repositoryProvider);
    }

    @Test
    void test_skal_ikke_returnere_behandlingsårsaker_hvis_ikke_endringer() {
        var behandling = opprettBehandling();

        var endringsresultat = EndringsresultatDiff.opprett();
        endringsresultat.leggTilSporetEndring(EndringsresultatDiff.medDiff(PersonInformasjonEntitet.class, 1L, 1L),
            () -> diffResult);
        endringsresultat.leggTilSporetEndring(
            EndringsresultatDiff.medDiff(FamilieHendelseGrunnlagEntitet.class, 1L, 1L), () -> diffResult);
        endringsresultat.leggTilSporetEndring(EndringsresultatDiff.medDiff(MedlemskapAggregat.class, 1L, 1L),
            () -> diffResult);
        endringsresultat.leggTilSporetEndring(EndringsresultatDiff.medDiff(InntektArbeidYtelseGrunnlag.class, 1L, 1L),
            () -> diffResult);
        endringsresultat.leggTilSporetEndring(EndringsresultatDiff.medDiff(YtelseFordelingAggregat.class, 1L, 1L),
            () -> diffResult);

        // Assert
        tjeneste.lagHistorikkForRegisterEndringsResultat(behandling, endringsresultat);

        // Assert
        verifyNoInteractions(historikkinnslagTjeneste);
    }

    @Test
    void test_behandlingsårsaker_når_endring_i_familiehendelse() {
        var behandling = opprettBehandling();
        var endringsresultat = EndringsresultatDiff.opprett();
        when(diffResult.isEmpty()).thenReturn(false); // Indikerer at det finnes diff
        endringsresultat.leggTilSporetEndring(
            EndringsresultatDiff.medDiff(FamilieHendelseGrunnlagEntitet.class, 1L, 2L), () -> diffResult);

        // Assert
        tjeneste.lagHistorikkForRegisterEndringsResultat(behandling, endringsresultat);

        // Assert
        verify(historikkinnslagTjeneste).opprettHistorikkinnslagForNyeRegisteropplysninger(any());
    }

    @Test
    void test_behandlingsårsaker_når_endring_dødsdato_søker() {
        var behandling = opprettBehandling();
        var dødsdato = LocalDate.now().minusDays(10);
        var personopplysningGrunnlag1 = opprettPersonopplysningGrunnlag(behandling, null);
        var personopplysningGrunnlag2 = opprettPersonopplysningGrunnlag(behandling,
            dødsdato);

        var endringsresultat = EndringsresultatDiff.opprett();
        when(diffResult.isEmpty()).thenReturn(false); // Indikerer at det finnes diff
        endringsresultat.leggTilSporetEndring(
            EndringsresultatDiff.medDiff(PersonInformasjonEntitet.class, personopplysningGrunnlag1.getId(),
                personopplysningGrunnlag2.getId()), () -> diffResult);

        // Assert
        tjeneste.lagHistorikkForRegisterEndringsResultat(behandling, endringsresultat);

        // Assert
        verify(historikkinnslagTjeneste).opprettHistorikkinnslagForBehandlingMedNyeOpplysninger(any(),
            eq(BehandlingÅrsakType.RE_OPPLYSNINGER_OM_DØD));
    }

    private PersonopplysningGrunnlagEntitet opprettPersonopplysningGrunnlag(Behandling behandling, LocalDate dødsdato) {
        var personopplysningRepository = repositoryProvider.getPersonopplysningRepository();
        var behandlingId = behandling.getId();
        var builder = personopplysningRepository.opprettBuilderForRegisterdata(behandlingId);
        var personopplysningBuilder = builder.getPersonopplysningBuilder(behandling.getAktørId());
        personopplysningBuilder.medDødsdato(dødsdato);
        builder.leggTil(personopplysningBuilder);
        personopplysningRepository.lagre(behandlingId, builder);
        return personopplysningRepository.hentPersonopplysninger(behandlingId);
    }
}
