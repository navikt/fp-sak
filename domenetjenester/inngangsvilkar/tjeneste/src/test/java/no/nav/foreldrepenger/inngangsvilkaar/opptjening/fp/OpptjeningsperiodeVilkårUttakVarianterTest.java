package no.nav.foreldrepenger.inngangsvilkaar.opptjening.fp;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.when;

import java.time.LocalDate;
import java.time.Period;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import no.nav.foreldrepenger.behandling.BehandlingReferanse;
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
import no.nav.foreldrepenger.inngangsvilkaar.opptjening.OpptjeningsperiodeVilkårTjeneste;
import no.nav.foreldrepenger.inngangsvilkaar.regelmodell.opptjeningsperiode.OpptjeningsPeriode;
import no.nav.foreldrepenger.skjæringstidspunkt.SkjæringstidspunktTjeneste;
import no.nav.foreldrepenger.skjæringstidspunkt.fp.SkjæringstidspunktTjenesteImpl;
import no.nav.foreldrepenger.skjæringstidspunkt.fp.SkjæringstidspunktUtils;
import no.nav.foreldrepenger.skjæringstidspunkt.overganger.MinsterettBehandling2022;
import no.nav.foreldrepenger.skjæringstidspunkt.overganger.MinsterettCore2022;
import no.nav.foreldrepenger.skjæringstidspunkt.overganger.UtsettelseBehandling2021;
import no.nav.foreldrepenger.skjæringstidspunkt.overganger.UtsettelseCore2021;

@ExtendWith(MockitoExtension.class)
public class OpptjeningsperiodeVilkårUttakVarianterTest extends EntityManagerAwareTest {

    private BehandlingRepositoryProvider repositoryProvider;
    private SkjæringstidspunktTjeneste skjæringstidspunktTjeneste;
    private OpptjeningsperiodeVilkårTjeneste opptjeningsperiodeVilkårTjeneste;
    private UtsettelseBehandling2021 utsettelse2021;
    private MinsterettBehandling2022 minsterett2022;
    @Mock
    private YtelseMaksdatoTjeneste ytelseMaksdatoTjeneste;
    private SkjæringstidspunktUtils stputil = new SkjæringstidspunktUtils(
        Period.parse("P1Y"), Period.parse("P6M"));

    @BeforeEach
    void setUp() {
        repositoryProvider = new BehandlingRepositoryProvider(getEntityManager());
    }

    @Test
    public void skal_fastsette_periode_til_mors_maksdato_far_sammenhengende_uttak_start_etter_mor_maks() {
        var fødselsdato = LocalDate.now().minusMonths(4);
        var morsmaksdato = fødselsdato.plusWeeks(31);
        var førsteUttaksdato = morsmaksdato.plusWeeks(4);
        utsettelse2021 = new UtsettelseBehandling2021(new UtsettelseCore2021(fødselsdato.plusMonths(1)), repositoryProvider);
        minsterett2022 = new MinsterettBehandling2022(new MinsterettCore2022(fødselsdato.plusMonths(1)), repositoryProvider);
        skjæringstidspunktTjeneste = new SkjæringstidspunktTjenesteImpl(repositoryProvider, ytelseMaksdatoTjeneste,
            stputil, utsettelse2021, minsterett2022);
        opptjeningsperiodeVilkårTjeneste = new OpptjeningsperiodeVilkårTjenesteImpl(
            repositoryProvider.getFamilieHendelseRepository(), ytelseMaksdatoTjeneste);

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

        var ref = lagRef(behandling);
        var data = new InngangsvilkårOpptjeningsperiode(opptjeningsperiodeVilkårTjeneste)
            .vurderVilkår(ref);

        var op = (OpptjeningsPeriode) data.ekstraVilkårresultat();
        assertThat(ref.getSkjæringstidspunkt().kreverSammenhengendeUttak()).isTrue();
        assertThat(op.getOpptjeningsperiodeTom()).isEqualTo(morsmaksdato);
    }

    @Test
    public void skal_fastsette_periode_til_første_uttak_far_sammenhengende_uttak_start_før_mor_maks() {
        var fødselsdato = LocalDate.now().minusMonths(4);
        var morsmaksdato = fødselsdato.plusWeeks(31);
        var førsteUttaksdato = morsmaksdato.minusWeeks(4);
        utsettelse2021 = new UtsettelseBehandling2021(new UtsettelseCore2021(fødselsdato.plusMonths(1)), repositoryProvider);
        minsterett2022 = new MinsterettBehandling2022(new MinsterettCore2022(fødselsdato.plusMonths(1)), repositoryProvider);
        skjæringstidspunktTjeneste = new SkjæringstidspunktTjenesteImpl(repositoryProvider, ytelseMaksdatoTjeneste,
            stputil, utsettelse2021, minsterett2022);
        opptjeningsperiodeVilkårTjeneste = new OpptjeningsperiodeVilkårTjenesteImpl(
            repositoryProvider.getFamilieHendelseRepository(), ytelseMaksdatoTjeneste);

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

        var ref = lagRef(behandling);
        var data = new InngangsvilkårOpptjeningsperiode(opptjeningsperiodeVilkårTjeneste)
            .vurderVilkår(ref);

        var op = (OpptjeningsPeriode) data.ekstraVilkårresultat();
        assertThat(ref.getSkjæringstidspunkt().kreverSammenhengendeUttak()).isTrue();
        assertThat(op.getOpptjeningsperiodeTom()).isEqualTo(førsteUttaksdato.minusDays(1));
    }

    @Test
    public void skal_fastsette_periode_til_første_uttak_far_fritt_uttak() {
        var fødselsdato = LocalDate.now().minusWeeks(4);
        var morsmaksdato = fødselsdato.plusWeeks(31);
        var førsteUttaksdato = morsmaksdato.plusWeeks(4);
        utsettelse2021 = new UtsettelseBehandling2021(new UtsettelseCore2021(fødselsdato.minusMonths(1)), repositoryProvider);
        minsterett2022 = new MinsterettBehandling2022(new MinsterettCore2022(fødselsdato.plusMonths(1)), repositoryProvider);
        skjæringstidspunktTjeneste = new SkjæringstidspunktTjenesteImpl(repositoryProvider, ytelseMaksdatoTjeneste,
            stputil, utsettelse2021, minsterett2022);
        opptjeningsperiodeVilkårTjeneste = new OpptjeningsperiodeVilkårTjenesteImpl(
            repositoryProvider.getFamilieHendelseRepository(), ytelseMaksdatoTjeneste);

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

        var ref = lagRef(behandling);
        var data = new InngangsvilkårOpptjeningsperiode(opptjeningsperiodeVilkårTjeneste)
            .vurderVilkår(ref);

        var op = (OpptjeningsPeriode) data.ekstraVilkårresultat();
        assertThat(ref.getSkjæringstidspunkt().kreverSammenhengendeUttak()).isFalse();
        assertThat(op.getOpptjeningsperiodeTom()).isEqualTo(førsteUttaksdato.minusDays(1));
    }

    @Test
    public void skal_fastsette_periode_til_første_uttak_far_wlb() {
        var fødselsdato = LocalDate.now();
        var termindato = LocalDate.now();
        var førsteUttaksdato = termindato.minusDays(3);
        utsettelse2021 = new UtsettelseBehandling2021(new UtsettelseCore2021(fødselsdato.minusYears(1)), repositoryProvider);
        minsterett2022 = new MinsterettBehandling2022(new MinsterettCore2022(fødselsdato.minusYears(1)), repositoryProvider);
        skjæringstidspunktTjeneste = new SkjæringstidspunktTjenesteImpl(repositoryProvider, ytelseMaksdatoTjeneste,
            stputil, utsettelse2021, minsterett2022);
        opptjeningsperiodeVilkårTjeneste = new OpptjeningsperiodeVilkårTjenesteImpl(
            repositoryProvider.getFamilieHendelseRepository(), ytelseMaksdatoTjeneste);

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

        var ref = lagRef(behandling);
        var data = new InngangsvilkårOpptjeningsperiode(opptjeningsperiodeVilkårTjeneste)
            .vurderVilkår(ref);

        var op = (OpptjeningsPeriode) data.ekstraVilkårresultat();
        assertThat(ref.getSkjæringstidspunkt().utenMinsterett()).isFalse();
        assertThat(op.getOpptjeningsperiodeTom()).isEqualTo(førsteUttaksdato.minusDays(1));
    }

    private BehandlingReferanse lagRef(Behandling behandling) {
        return BehandlingReferanse.fra(behandling,
            skjæringstidspunktTjeneste.getSkjæringstidspunkter(behandling.getId()));
    }

}
