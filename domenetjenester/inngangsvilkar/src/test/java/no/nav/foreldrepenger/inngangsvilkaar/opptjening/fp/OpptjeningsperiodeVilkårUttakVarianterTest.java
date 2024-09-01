package no.nav.foreldrepenger.inngangsvilkaar.opptjening.fp;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.when;

import java.time.LocalDate;
import java.time.Month;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import no.nav.foreldrepenger.behandling.BehandlingReferanse;
import no.nav.foreldrepenger.behandling.FagsakRelasjonTjeneste;
import no.nav.foreldrepenger.behandling.YtelseMaksdatoTjeneste;
import no.nav.foreldrepenger.behandlingslager.behandling.Behandling;
import no.nav.foreldrepenger.behandlingslager.behandling.personopplysning.RelasjonsRolleType;
import no.nav.foreldrepenger.behandlingslager.behandling.personopplysning.SivilstandType;
import no.nav.foreldrepenger.behandlingslager.behandling.repository.BehandlingRepositoryProvider;
import no.nav.foreldrepenger.behandlingslager.behandling.ytelsefordeling.periode.OppgittFordelingEntitet;
import no.nav.foreldrepenger.behandlingslager.behandling.ytelsefordeling.periode.OppgittPeriodeBuilder;
import no.nav.foreldrepenger.behandlingslager.behandling.ytelsefordeling.periode.UttakPeriodeType;
import no.nav.foreldrepenger.behandlingslager.geografisk.Landkoder;
import no.nav.foreldrepenger.behandlingslager.testutilities.behandling.ScenarioFarSøkerForeldrepenger;
import no.nav.foreldrepenger.dbstoette.EntityManagerAwareTest;
import no.nav.foreldrepenger.domene.typer.AktørId;
import no.nav.foreldrepenger.inngangsvilkaar.regelmodell.opptjeningsperiode.OpptjeningsPeriode;
import no.nav.foreldrepenger.skjæringstidspunkt.fp.SkjæringstidspunktTjenesteImpl;
import no.nav.foreldrepenger.skjæringstidspunkt.overganger.MinsterettBehandling2022;
import no.nav.foreldrepenger.skjæringstidspunkt.overganger.UtsettelseCore2021;

@ExtendWith(MockitoExtension.class)
class OpptjeningsperiodeVilkårUttakVarianterTest extends EntityManagerAwareTest {

    private BehandlingRepositoryProvider repositoryProvider;
    private FagsakRelasjonTjeneste fagsakRelasjonTjeneste;
    @Mock
    private YtelseMaksdatoTjeneste ytelseMaksdatoTjeneste;

    @BeforeEach
    void setUp() {
        repositoryProvider = new BehandlingRepositoryProvider(getEntityManager());
        fagsakRelasjonTjeneste = new FagsakRelasjonTjeneste(repositoryProvider);
    }

    @Test
    void skal_fastsette_periode_til_mors_maksdato_far_sammenhengende_uttak_start_etter_mor_maks() {
        var fødselsdato = UtsettelseCore2021.IKRAFT_FRA_DATO.minusMonths(6);
        var morsmaksdato = fødselsdato.plusWeeks(31);
        var førsteUttaksdato = morsmaksdato.plusWeeks(4);
        var minsterett2022 = new MinsterettBehandling2022(repositoryProvider, fagsakRelasjonTjeneste);
        var skjæringstidspunktTjeneste = new SkjæringstidspunktTjenesteImpl(repositoryProvider, ytelseMaksdatoTjeneste, minsterett2022);
        var opptjeningsperiodeVilkårTjeneste = new InngangsvilkårOpptjeningsperiode(
            repositoryProvider.getFamilieHendelseRepository(), ytelseMaksdatoTjeneste, skjæringstidspunktTjeneste);

        when(ytelseMaksdatoTjeneste.beregnMorsMaksdato(any(), any())).thenReturn(Optional.of(morsmaksdato));

        var oppgittPeriodeBuilder = OppgittPeriodeBuilder.ny()
            .medPeriode(førsteUttaksdato, førsteUttaksdato.plusWeeks(5))
            .medPeriodeType(UttakPeriodeType.FEDREKVOTE);
        var scenario = ScenarioFarSøkerForeldrepenger.forFødsel()
            .medFordeling(new OppgittFordelingEntitet(List.of(oppgittPeriodeBuilder.build()), true));
        scenario.medBekreftetHendelse().medFødselsDato(fødselsdato).medAntallBarn(1);
        scenario.medSøknadHendelse().medFødselsDato(fødselsdato).medAntallBarn(1);
        var builderForRegisteropplysninger = scenario.opprettBuilderForRegisteropplysninger();
        var barnAktørId = AktørId.dummy();
        var søkerAktørId = scenario.getDefaultBrukerAktørId();

        var fødtBarn = builderForRegisteropplysninger.medPersonas()
            .fødtBarn(barnAktørId, fødselsdato)
            .relasjonTil(søkerAktørId, RelasjonsRolleType.MORA, null)
            .build();
        var søker = builderForRegisteropplysninger.medPersonas()
            .mann(søkerAktørId, SivilstandType.GIFT)
            .statsborgerskap(Landkoder.NOR)
            .relasjonTil(barnAktørId, RelasjonsRolleType.BARN, true)
            .build();
        scenario.medRegisterOpplysninger(søker);
        scenario.medRegisterOpplysninger(fødtBarn);
        var behandling = scenario.lagre(repositoryProvider);

        var data = opptjeningsperiodeVilkårTjeneste.vurderVilkår(lagRef(behandling));

        var op = (OpptjeningsPeriode) data.ekstraVilkårresultat();
        assertThat(skjæringstidspunktTjeneste.getSkjæringstidspunkter(behandling.getId()).kreverSammenhengendeUttak()).isFalse();
        assertThat(op.getOpptjeningsperiodeTom()).isEqualTo(førsteUttaksdato.minusDays(1));
    }

    @Test
    void skal_fastsette_periode_til_mors_maksdato_far_sammenhengende_uttak_start_etter_mor_maks_før_fri_utsettelse() {
        var fødselsdato = UtsettelseCore2021.IKRAFT_FRA_DATO.minusMonths(12);
        var morsmaksdato = fødselsdato.plusWeeks(31);
        var førsteUttaksdato = morsmaksdato.plusWeeks(4);
        var minsterett2022 = new MinsterettBehandling2022(repositoryProvider, fagsakRelasjonTjeneste);
        var skjæringstidspunktTjeneste = new SkjæringstidspunktTjenesteImpl(repositoryProvider, ytelseMaksdatoTjeneste, minsterett2022);
        var opptjeningsperiodeVilkårTjeneste = new InngangsvilkårOpptjeningsperiode(
            repositoryProvider.getFamilieHendelseRepository(), ytelseMaksdatoTjeneste, skjæringstidspunktTjeneste);

        when(ytelseMaksdatoTjeneste.beregnMorsMaksdato(any(), any())).thenReturn(Optional.of(morsmaksdato));

        var oppgittPeriodeBuilder = OppgittPeriodeBuilder.ny()
            .medPeriode(førsteUttaksdato, førsteUttaksdato.plusWeeks(5))
            .medPeriodeType(UttakPeriodeType.FEDREKVOTE);
        var scenario = ScenarioFarSøkerForeldrepenger.forFødsel()
            .medFordeling(new OppgittFordelingEntitet(List.of(oppgittPeriodeBuilder.build()), true));
        scenario.medBekreftetHendelse().medFødselsDato(fødselsdato).medAntallBarn(1);
        scenario.medSøknadHendelse().medFødselsDato(fødselsdato).medAntallBarn(1);
        var builderForRegisteropplysninger = scenario.opprettBuilderForRegisteropplysninger();
        var barnAktørId = AktørId.dummy();
        var søkerAktørId = scenario.getDefaultBrukerAktørId();

        var fødtBarn = builderForRegisteropplysninger.medPersonas()
            .fødtBarn(barnAktørId, fødselsdato)
            .relasjonTil(søkerAktørId, RelasjonsRolleType.MORA, null)
            .build();
        var søker = builderForRegisteropplysninger.medPersonas()
            .mann(søkerAktørId, SivilstandType.GIFT)
            .statsborgerskap(Landkoder.NOR)
            .relasjonTil(barnAktørId, RelasjonsRolleType.BARN, true)
            .build();
        scenario.medRegisterOpplysninger(søker);
        scenario.medRegisterOpplysninger(fødtBarn);
        var behandling = scenario.lagre(repositoryProvider);

        var data = opptjeningsperiodeVilkårTjeneste.vurderVilkår(lagRef(behandling));

        var op = (OpptjeningsPeriode) data.ekstraVilkårresultat();
        assertThat(skjæringstidspunktTjeneste.getSkjæringstidspunkter(behandling.getId()).kreverSammenhengendeUttak()).isTrue();
        assertThat(op.getOpptjeningsperiodeTom()).isEqualTo(morsmaksdato);
    }

    @Test
    void skal_fastsette_periode_til_første_uttak_far_sammenhengende_uttak_start_før_mor_maks_før_fri_utsettelse() {
        var fødselsdato = UtsettelseCore2021.IKRAFT_FRA_DATO.minusMonths(12);
        var morsmaksdato = fødselsdato.plusWeeks(31);
        var førsteUttaksdato = morsmaksdato.minusWeeks(4);
        var minsterett2022 = new MinsterettBehandling2022(repositoryProvider, fagsakRelasjonTjeneste);
        var skjæringstidspunktTjeneste = new SkjæringstidspunktTjenesteImpl(repositoryProvider, ytelseMaksdatoTjeneste, minsterett2022);
        var opptjeningsperiodeVilkårTjeneste = new InngangsvilkårOpptjeningsperiode(
            repositoryProvider.getFamilieHendelseRepository(), ytelseMaksdatoTjeneste, skjæringstidspunktTjeneste);

        when(ytelseMaksdatoTjeneste.beregnMorsMaksdato(any(), any())).thenReturn(Optional.of(morsmaksdato));

        var oppgittPeriodeBuilder = OppgittPeriodeBuilder.ny()
            .medPeriode(førsteUttaksdato, førsteUttaksdato.plusWeeks(5))
            .medPeriodeType(UttakPeriodeType.FEDREKVOTE);
        var scenario = ScenarioFarSøkerForeldrepenger.forFødsel()
            .medFordeling(new OppgittFordelingEntitet(List.of(oppgittPeriodeBuilder.build()), true));
        scenario.medBekreftetHendelse().medFødselsDato(fødselsdato).medAntallBarn(1);
        scenario.medSøknadHendelse().medFødselsDato(fødselsdato).medAntallBarn(1);
        var builderForRegisteropplysninger = scenario.opprettBuilderForRegisteropplysninger();
        var barnAktørId = AktørId.dummy();
        var søkerAktørId = scenario.getDefaultBrukerAktørId();

        var fødtBarn = builderForRegisteropplysninger.medPersonas()
            .fødtBarn(barnAktørId, fødselsdato)
            .relasjonTil(søkerAktørId, RelasjonsRolleType.MORA, null)
            .build();
        var søker = builderForRegisteropplysninger.medPersonas()
            .mann(søkerAktørId, SivilstandType.GIFT)
            .statsborgerskap(Landkoder.NOR)
            .relasjonTil(barnAktørId, RelasjonsRolleType.BARN, true)
            .build();
        scenario.medRegisterOpplysninger(søker);
        scenario.medRegisterOpplysninger(fødtBarn);
        var behandling = scenario.lagre(repositoryProvider);

        var data = opptjeningsperiodeVilkårTjeneste.vurderVilkår(lagRef(behandling));

        var op = (OpptjeningsPeriode) data.ekstraVilkårresultat();
        assertThat(skjæringstidspunktTjeneste.getSkjæringstidspunkter(behandling.getId()).kreverSammenhengendeUttak()).isTrue();
        assertThat(op.getOpptjeningsperiodeTom()).isEqualTo(førsteUttaksdato.minusDays(1));
    }

    @Test
    void skal_fastsette_periode_til_første_uttak_far_fritt_uttak() {
        var fødselsdato = LocalDate.of(2022, Month.JANUARY, 1);
        var morsmaksdato = fødselsdato.plusWeeks(31);
        var førsteUttaksdato = morsmaksdato.plusWeeks(4);
        var minsterett2022 = new MinsterettBehandling2022(repositoryProvider, fagsakRelasjonTjeneste);
        var skjæringstidspunktTjeneste = new SkjæringstidspunktTjenesteImpl(repositoryProvider, ytelseMaksdatoTjeneste, minsterett2022);
        var opptjeningsperiodeVilkårTjeneste = new InngangsvilkårOpptjeningsperiode(
            repositoryProvider.getFamilieHendelseRepository(), ytelseMaksdatoTjeneste, skjæringstidspunktTjeneste);

        lenient().when(ytelseMaksdatoTjeneste.beregnMorsMaksdato(any(), any())).thenReturn(Optional.of(morsmaksdato));

        var oppgittPeriodeBuilder = OppgittPeriodeBuilder.ny()
            .medPeriode(morsmaksdato.plusWeeks(4), morsmaksdato.plusWeeks(9))
            .medPeriodeType(UttakPeriodeType.FEDREKVOTE);
        var scenario = ScenarioFarSøkerForeldrepenger.forFødsel()
            .medFordeling(new OppgittFordelingEntitet(List.of(oppgittPeriodeBuilder.build()), true));
        scenario.medBekreftetHendelse().medFødselsDato(fødselsdato).medAntallBarn(1);
        scenario.medSøknadHendelse().medFødselsDato(fødselsdato).medAntallBarn(1);
        var builderForRegisteropplysninger = scenario.opprettBuilderForRegisteropplysninger();
        var barnAktørId = AktørId.dummy();
        var søkerAktørId = scenario.getDefaultBrukerAktørId();

        var fødtBarn = builderForRegisteropplysninger.medPersonas()
            .fødtBarn(barnAktørId, fødselsdato)
            .relasjonTil(søkerAktørId, RelasjonsRolleType.MORA, null)
            .build();
        var søker = builderForRegisteropplysninger.medPersonas()
            .mann(søkerAktørId, SivilstandType.GIFT)
            .statsborgerskap(Landkoder.NOR)
            .relasjonTil(barnAktørId, RelasjonsRolleType.BARN, true)
            .build();
        scenario.medRegisterOpplysninger(søker);
        scenario.medRegisterOpplysninger(fødtBarn);
        var behandling = scenario.lagre(repositoryProvider);

        var data = opptjeningsperiodeVilkårTjeneste.vurderVilkår(lagRef(behandling));

        var op = (OpptjeningsPeriode) data.ekstraVilkårresultat();
        assertThat(skjæringstidspunktTjeneste.getSkjæringstidspunkter(behandling.getId()).kreverSammenhengendeUttak()).isFalse();
        assertThat(op.getOpptjeningsperiodeTom()).isEqualTo(førsteUttaksdato.minusDays(1));
    }

    @Test
    void skal_fastsette_periode_til_første_uttak_far_wlb() {
        var fødselsdato = LocalDate.now();
        var termindato = LocalDate.now();
        var førsteUttaksdato = termindato.minusDays(3);
        var minsterett2022 = new MinsterettBehandling2022(repositoryProvider, fagsakRelasjonTjeneste);
        var skjæringstidspunktTjeneste = new SkjæringstidspunktTjenesteImpl(repositoryProvider, ytelseMaksdatoTjeneste, minsterett2022);
        var opptjeningsperiodeVilkårTjeneste = new InngangsvilkårOpptjeningsperiode(
            repositoryProvider.getFamilieHendelseRepository(), ytelseMaksdatoTjeneste, skjæringstidspunktTjeneste);

        var oppgittPeriodeBuilder = OppgittPeriodeBuilder.ny()
            .medPeriode(førsteUttaksdato, førsteUttaksdato.plusDays(8))
            .medPeriodeType(UttakPeriodeType.FEDREKVOTE);
        var scenario = ScenarioFarSøkerForeldrepenger.forFødsel()
            .medFordeling(new OppgittFordelingEntitet(List.of(oppgittPeriodeBuilder.build()), true));
        scenario.medBekreftetHendelse().medFødselsDato(fødselsdato).medAntallBarn(1)
            .medTerminbekreftelse(scenario.medBekreftetHendelse().getTerminbekreftelseBuilder().medTermindato(termindato));
        scenario.medSøknadHendelse().medFødselsDato(fødselsdato).medAntallBarn(1)
            .medTerminbekreftelse(scenario.medSøknadHendelse().getTerminbekreftelseBuilder().medTermindato(termindato));
        var builderForRegisteropplysninger = scenario.opprettBuilderForRegisteropplysninger();
        var barnAktørId = AktørId.dummy();
        var søkerAktørId = scenario.getDefaultBrukerAktørId();

        var fødtBarn = builderForRegisteropplysninger.medPersonas()
            .fødtBarn(barnAktørId, fødselsdato)
            .relasjonTil(søkerAktørId, RelasjonsRolleType.MORA, null)
            .build();
        var søker = builderForRegisteropplysninger.medPersonas()
            .kvinne(søkerAktørId, SivilstandType.GIFT)
            .statsborgerskap(Landkoder.NOR)
            .relasjonTil(barnAktørId, RelasjonsRolleType.BARN, true)
            .build();
        scenario.medRegisterOpplysninger(søker);
        scenario.medRegisterOpplysninger(fødtBarn);
        var behandling = scenario.lagre(repositoryProvider);

        var data = opptjeningsperiodeVilkårTjeneste.vurderVilkår(lagRef(behandling));

        var op = (OpptjeningsPeriode) data.ekstraVilkårresultat();
        assertThat(skjæringstidspunktTjeneste.getSkjæringstidspunkter(behandling.getId()).utenMinsterett()).isFalse();
        assertThat(op.getOpptjeningsperiodeTom()).isEqualTo(førsteUttaksdato.minusDays(1));
    }

    private BehandlingReferanse lagRef(Behandling behandling) {
        return BehandlingReferanse.fra(behandling);
    }

}
