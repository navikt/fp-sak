package no.nav.foreldrepenger.inngangsvilkaar.opptjening;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.LocalDate;
import java.time.Period;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import no.nav.foreldrepenger.behandling.BehandlingReferanse;
import no.nav.foreldrepenger.behandling.RelatertBehandlingTjeneste;
import no.nav.foreldrepenger.behandling.YtelseMaksdatoTjeneste;
import no.nav.foreldrepenger.behandlingslager.aktør.NavBrukerKjønn;
import no.nav.foreldrepenger.behandlingslager.behandling.Behandling;
import no.nav.foreldrepenger.behandlingslager.behandling.personopplysning.RelasjonsRolleType;
import no.nav.foreldrepenger.behandlingslager.behandling.personopplysning.SivilstandType;
import no.nav.foreldrepenger.behandlingslager.behandling.repository.BehandlingRepositoryProvider;
import no.nav.foreldrepenger.behandlingslager.behandling.ytelsefordeling.periode.OppgittFordelingEntitet;
import no.nav.foreldrepenger.behandlingslager.behandling.ytelsefordeling.periode.OppgittPeriodeBuilder;
import no.nav.foreldrepenger.behandlingslager.behandling.ytelsefordeling.periode.UttakPeriodeType;
import no.nav.foreldrepenger.behandlingslager.geografisk.Landkoder;
import no.nav.foreldrepenger.behandlingslager.geografisk.Region;
import no.nav.foreldrepenger.behandlingslager.testutilities.behandling.ScenarioFarSøkerForeldrepenger;
import no.nav.foreldrepenger.behandlingslager.testutilities.behandling.ScenarioMorSøkerForeldrepenger;
import no.nav.foreldrepenger.dbstoette.EntityManagerAwareTest;
import no.nav.foreldrepenger.domene.abakus.AbakusInMemoryInntektArbeidYtelseTjeneste;
import no.nav.foreldrepenger.domene.arbeidsforhold.InntektArbeidYtelseTjeneste;
import no.nav.foreldrepenger.domene.personopplysning.BasisPersonopplysningTjeneste;
import no.nav.foreldrepenger.domene.typer.AktørId;
import no.nav.foreldrepenger.inngangsvilkaar.VilkårData;
import no.nav.foreldrepenger.inngangsvilkaar.impl.InngangsvilkårOversetter;
import no.nav.foreldrepenger.inngangsvilkaar.opptjening.fp.InngangsvilkårOpptjeningsperiode;
import no.nav.foreldrepenger.inngangsvilkaar.opptjening.fp.OpptjeningsperiodeVilkårTjenesteImpl;
import no.nav.foreldrepenger.inngangsvilkaar.regelmodell.opptjening.OpptjeningsPeriode;
import no.nav.foreldrepenger.skjæringstidspunkt.SkjæringstidspunktTjeneste;
import no.nav.foreldrepenger.skjæringstidspunkt.fp.SkjæringstidspunktTjenesteImpl;
import no.nav.foreldrepenger.skjæringstidspunkt.fp.SkjæringstidspunktUtils;

public class OpptjeningsperiodeVilkårTest extends EntityManagerAwareTest {

    private BehandlingRepositoryProvider repositoryProvider;
    private SkjæringstidspunktTjeneste skjæringstidspunktTjeneste;
    private OpptjeningsperiodeVilkårTjeneste opptjeningsperiodeVilkårTjeneste;

    @BeforeEach
    void setUp() {
        repositoryProvider = new BehandlingRepositoryProvider(getEntityManager());
        InntektArbeidYtelseTjeneste iayTjeneste = new AbakusInMemoryInntektArbeidYtelseTjeneste();
        SkjæringstidspunktUtils stputil = new SkjæringstidspunktUtils(Period.parse("P10M"), Period.parse("P6M"),
            Period.parse("P1Y"), Period.parse("P6M"));
        YtelseMaksdatoTjeneste ytelseMaksdatoTjeneste = new YtelseMaksdatoTjeneste(repositoryProvider,
            new RelatertBehandlingTjeneste(repositoryProvider));
        skjæringstidspunktTjeneste = new SkjæringstidspunktTjenesteImpl(repositoryProvider, ytelseMaksdatoTjeneste,
            stputil);
        BasisPersonopplysningTjeneste personopplysningTjeneste = new BasisPersonopplysningTjeneste(
            repositoryProvider.getPersonopplysningRepository());
        YtelseMaksdatoTjeneste beregnMorsMaksdatoTjeneste = new YtelseMaksdatoTjeneste(repositoryProvider,
            new RelatertBehandlingTjeneste(repositoryProvider));
        InngangsvilkårOversetter oversetter = new InngangsvilkårOversetter(repositoryProvider, personopplysningTjeneste,
            beregnMorsMaksdatoTjeneste, iayTjeneste, null);
        opptjeningsperiodeVilkårTjeneste = new OpptjeningsperiodeVilkårTjenesteImpl(oversetter,
            repositoryProvider.getFamilieHendelseRepository(), beregnMorsMaksdatoTjeneste, Period.parse("P10M"),
            Period.parse("P12W"));
    }

    @Test
    public void skal_fastsette_periode_med_termindato() {
        var skjæringstidspunkt = LocalDate.now().plusWeeks(1L).minusDays(1L);
        var oppgittPeriodeBuilder = OppgittPeriodeBuilder.ny()
            .medPeriode(LocalDate.now().plusWeeks(2), LocalDate.now().plusWeeks(4))
            .medPeriodeType(UttakPeriodeType.FORELDREPENGER_FØR_FØDSEL);
        var scenario = ScenarioMorSøkerForeldrepenger.forFødsel()
            .medFordeling(new OppgittFordelingEntitet(List.of(oppgittPeriodeBuilder.build()), true));
        scenario.medSøknadHendelse()
            .medTerminbekreftelse(scenario.medSøknadHendelse()
                .getTerminbekreftelseBuilder()
                .medTermindato(LocalDate.now().plusWeeks(4L))
                .medUtstedtDato(LocalDate.now())
                .medNavnPå("Doktor Dankel"));
        scenario.medBekreftetHendelse()
            .medTerminbekreftelse(scenario.medBekreftetHendelse()
                .getTerminbekreftelseBuilder()
                .medTermindato(LocalDate.now().plusWeeks(4L))
                .medUtstedtDato(LocalDate.now())
                .medNavnPå("Doktor Dankel"));
        var behandling = scenario.lagre(repositoryProvider);

        var data = new InngangsvilkårOpptjeningsperiode(opptjeningsperiodeVilkårTjeneste).vurderVilkår(
            lagRef(behandling));

        var op = (OpptjeningsPeriode) data.getEkstraVilkårresultat();
        assertThat(op.getOpptjeningsperiodeTom()).isEqualTo(skjæringstidspunkt);
        assertThat(op.getOpptjeningsperiodeFom()).isEqualTo(op.getOpptjeningsperiodeTom().plusDays(1).minusMonths(10L));
    }

    @Test
    public void skal_fastsette_periode_ved_fødsel_mor() {
        var oppgittPeriodeBuilder = OppgittPeriodeBuilder.ny()
            .medPeriode(LocalDate.now().plusWeeks(2), LocalDate.now().plusWeeks(4))
            .medPeriodeType(UttakPeriodeType.FORELDREPENGER_FØR_FØDSEL);
        var scenario = ScenarioMorSøkerForeldrepenger.forFødsel()
            .medFordeling(new OppgittFordelingEntitet(List.of(oppgittPeriodeBuilder.build()), true));
        scenario.medSøknadHendelse().medFødselsDato(LocalDate.now()).medAntallBarn(1);
        scenario.medBekreftetHendelse().medFødselsDato(LocalDate.now()).medAntallBarn(1);
        var behandling = scenario.lagre(repositoryProvider);

        var data = new InngangsvilkårOpptjeningsperiode(opptjeningsperiodeVilkårTjeneste).vurderVilkår(
            lagRef(behandling));

        var op = (OpptjeningsPeriode) data.getEkstraVilkårresultat();
        assertThat(op.getOpptjeningsperiodeTom()).isEqualTo(LocalDate.now().minusDays(1L));
    }

    @Test
    public void skal_fastsette_periode_ved_tidlig_uttak_termin_fødsel_mor() {
        var skjæringstidspunkt = LocalDate.now().plusWeeks(1L).minusDays(1);
        var oppgittPeriodeBuilder = OppgittPeriodeBuilder.ny()
            .medPeriode(LocalDate.now().plusWeeks(1), LocalDate.now().plusWeeks(10).minusDays(1))
            .medPeriodeType(UttakPeriodeType.FELLESPERIODE);
        var oppgittPeriodeBuilder2 = OppgittPeriodeBuilder.ny()
            .medPeriode(LocalDate.now().plusWeeks(10), LocalDate.now().plusWeeks(13))
            .medPeriodeType(UttakPeriodeType.FORELDREPENGER_FØR_FØDSEL);
        var oppgittFordeling = new OppgittFordelingEntitet(
            List.of(oppgittPeriodeBuilder.build(), oppgittPeriodeBuilder2.build()), true);
        var scenario = ScenarioMorSøkerForeldrepenger.forFødsel().medFordeling(oppgittFordeling);
        scenario.medSøknadHendelse()
            .medTerminbekreftelse(scenario.medSøknadHendelse()
                .getTerminbekreftelseBuilder()
                .medTermindato(LocalDate.now().plusWeeks(13L))
                .medUtstedtDato(LocalDate.now())
                .medNavnPå("Doktor Dankel"));
        scenario.medBekreftetHendelse()
            .medTerminbekreftelse(scenario.medBekreftetHendelse()
                .getTerminbekreftelseBuilder()
                .medTermindato(LocalDate.now().plusWeeks(13L))
                .medUtstedtDato(LocalDate.now())
                .medNavnPå("Doktor Dankel"));
        scenario.medBekreftetHendelse().medFødselsDato(LocalDate.now().plusWeeks(14)).medAntallBarn(1);
        var behandling = scenario.lagre(repositoryProvider);

        var data = new InngangsvilkårOpptjeningsperiode(opptjeningsperiodeVilkårTjeneste).vurderVilkår(
            lagRef(behandling));

        var op = (OpptjeningsPeriode) data.getEkstraVilkårresultat();
        assertThat(op.getOpptjeningsperiodeTom()).isEqualTo(skjæringstidspunkt);
        assertThat(op.getOpptjeningsperiodeFom()).isEqualTo(op.getOpptjeningsperiodeTom().plusDays(1).minusMonths(10L));
    }

    @Test
    public void skal_fastsette_periode_ved_fødsel_far() {
        var fødselsdato = LocalDate.now();
        var oppgittPeriodeBuilder = OppgittPeriodeBuilder.ny()
            .medPeriode(LocalDate.now().minusDays(1L), LocalDate.now().plusWeeks(4))
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
            .kvinne(søkerAktørId, SivilstandType.GIFT, Region.NORDEN)
            .statsborgerskap(Landkoder.NOR)
            .relasjonTil(barnAktørId, RelasjonsRolleType.BARN, true)
            .build();
        scenario.medRegisterOpplysninger(søker);
        scenario.medRegisterOpplysninger(fødtBarn);
        var behandling = scenario.lagre(repositoryProvider);

        VilkårData data = new InngangsvilkårOpptjeningsperiode(opptjeningsperiodeVilkårTjeneste).vurderVilkår(
            lagRef(behandling));

        OpptjeningsPeriode op = (OpptjeningsPeriode) data.getEkstraVilkårresultat();
        assertThat(op.getOpptjeningsperiodeTom()).isEqualTo(LocalDate.now().minusDays(1L));
    }

    @Test
    public void skal_fastsette_periode_ved_adopsjon_mor_søker() {
        var behandling = settOppAdopsjonBehandlingForMor(10, false, NavBrukerKjønn.KVINNE, false);
        var oppgittPeriodeBuilder = OppgittPeriodeBuilder.ny()
            .medPeriode(LocalDate.of(2018, 1, 1).minusDays(1L), LocalDate.now().plusWeeks(4))
            .medPeriodeType(UttakPeriodeType.FORELDREPENGER_FØR_FØDSEL);
        var oppgittFordeling = new OppgittFordelingEntitet(List.of(oppgittPeriodeBuilder.build()), true);
        var yfBuilder = repositoryProvider.getYtelsesFordelingRepository()
            .opprettBuilder(behandling.getId())
            .medOppgittFordeling(oppgittFordeling);
        repositoryProvider.getYtelsesFordelingRepository().lagre(behandling.getId(), yfBuilder.build());

        var data = new InngangsvilkårOpptjeningsperiode(opptjeningsperiodeVilkårTjeneste).vurderVilkår(
            lagRef(behandling));

        var op = (OpptjeningsPeriode) data.getEkstraVilkårresultat();
        assertThat(op.getOpptjeningsperiodeTom()).isEqualTo(LocalDate.of(2018, 1, 1).minusDays(1L));
    }

    @Test
    public void skal_fastsette_periode_ved_adopsjon_far_søker() {
        var behandling = settOppAdopsjonBehandlingForMor(10, false, NavBrukerKjønn.MANN, false);
        var oppgittPeriodeBuilder = OppgittPeriodeBuilder.ny()
            .medPeriode(LocalDate.of(2018, 1, 1).minusDays(1L), LocalDate.now().plusWeeks(4))
            .medPeriodeType(UttakPeriodeType.FORELDREPENGER_FØR_FØDSEL);
        var oppgittFordeling = new OppgittFordelingEntitet(List.of(oppgittPeriodeBuilder.build()), true);
        var yfBuilder = repositoryProvider.getYtelsesFordelingRepository()
            .opprettBuilder(behandling.getId())
            .medOppgittFordeling(oppgittFordeling);
        repositoryProvider.getYtelsesFordelingRepository().lagre(behandling.getId(), yfBuilder.build());

        var data = new InngangsvilkårOpptjeningsperiode(opptjeningsperiodeVilkårTjeneste).vurderVilkår(
            lagRef(behandling));

        var op = (OpptjeningsPeriode) data.getEkstraVilkårresultat();
        assertThat(op.getOpptjeningsperiodeTom()).isEqualTo(LocalDate.of(2018, 1, 1).minusDays(1L));
    }

    private Behandling settOppAdopsjonBehandlingForMor(int alder,
                                                       boolean ektefellesBarn,
                                                       NavBrukerKjønn kjønn,
                                                       boolean adoptererAlene) {
        LocalDate omsorgsovertakelsedato = LocalDate.of(2018, 1, 1);
        if (kjønn.equals(NavBrukerKjønn.KVINNE)) {
            ScenarioMorSøkerForeldrepenger scenario = ScenarioMorSøkerForeldrepenger.forAdopsjon();
            scenario.medSøknadHendelse()
                .medAdopsjon(scenario.medSøknadHendelse()
                    .getAdopsjonBuilder()
                    .medOmsorgsovertakelseDato(omsorgsovertakelsedato)
                    .medErEktefellesBarn(ektefellesBarn)
                    .medAdoptererAlene(adoptererAlene))
                .leggTilBarn(omsorgsovertakelsedato.minusYears(alder));
            scenario.medBekreftetHendelse()
                .medAdopsjon(scenario.medBekreftetHendelse()
                    .getAdopsjonBuilder()
                    .medOmsorgsovertakelseDato(omsorgsovertakelsedato)
                    .medErEktefellesBarn(ektefellesBarn)
                    .medAdoptererAlene(adoptererAlene))
                .leggTilBarn(omsorgsovertakelsedato.minusYears(alder));
            Behandling behandling = scenario.lagre(repositoryProvider);
            return behandling;
        } else {
            ScenarioFarSøkerForeldrepenger scenario = ScenarioFarSøkerForeldrepenger.forAdopsjon();
            scenario.medSøknadHendelse()
                .medAdopsjon(scenario.medSøknadHendelse()
                    .getAdopsjonBuilder()
                    .medOmsorgsovertakelseDato(omsorgsovertakelsedato)
                    .medErEktefellesBarn(ektefellesBarn)
                    .medAdoptererAlene(adoptererAlene))
                .leggTilBarn(omsorgsovertakelsedato.minusYears(alder));
            scenario.medBekreftetHendelse()
                .medAdopsjon(scenario.medBekreftetHendelse()
                    .getAdopsjonBuilder()
                    .medOmsorgsovertakelseDato(omsorgsovertakelsedato)
                    .medErEktefellesBarn(ektefellesBarn)
                    .medAdoptererAlene(adoptererAlene))
                .leggTilBarn(omsorgsovertakelsedato.minusYears(alder));
            Behandling behandling = scenario.lagre(repositoryProvider);
            return behandling;
        }
    }

    private BehandlingReferanse lagRef(Behandling behandling) {
        return BehandlingReferanse.fra(behandling,
            skjæringstidspunktTjeneste.getSkjæringstidspunkter(behandling.getId()));
    }

}
