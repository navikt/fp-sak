package no.nav.foreldrepenger.domene.uttak.fakta.v2;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.LocalDate;
import java.util.Comparator;
import java.util.List;

import org.junit.jupiter.api.Test;

import no.nav.foreldrepenger.behandling.BehandlingReferanse;
import no.nav.foreldrepenger.behandlingslager.behandling.aksjonspunkt.AksjonspunktDefinisjon;
import no.nav.foreldrepenger.behandlingslager.behandling.ytelsefordeling.MorsAktivitet;
import no.nav.foreldrepenger.behandlingslager.behandling.ytelsefordeling.OppgittRettighetEntitet;
import no.nav.foreldrepenger.behandlingslager.behandling.ytelsefordeling.periode.FordelingPeriodeKilde;
import no.nav.foreldrepenger.behandlingslager.behandling.ytelsefordeling.periode.OppgittFordelingEntitet;
import no.nav.foreldrepenger.behandlingslager.behandling.ytelsefordeling.periode.OppgittPeriodeBuilder;
import no.nav.foreldrepenger.behandlingslager.behandling.ytelsefordeling.periode.UttakPeriodeType;
import no.nav.foreldrepenger.behandlingslager.behandling.ytelsefordeling.årsak.OverføringÅrsak;
import no.nav.foreldrepenger.behandlingslager.behandling.ytelsefordeling.årsak.UtsettelseÅrsak;
import no.nav.foreldrepenger.domene.uttak.ForeldrepengerUttakTjeneste;
import no.nav.foreldrepenger.domene.uttak.input.FamilieHendelse;
import no.nav.foreldrepenger.domene.uttak.input.FamilieHendelser;
import no.nav.foreldrepenger.domene.uttak.input.ForeldrepengerGrunnlag;
import no.nav.foreldrepenger.domene.uttak.input.UttakInput;
import no.nav.foreldrepenger.domene.uttak.testutilities.behandling.ScenarioFarSøkerForeldrepenger;
import no.nav.foreldrepenger.domene.uttak.testutilities.behandling.UttakRepositoryStubProvider;
import no.nav.foreldrepenger.domene.ytelsefordeling.YtelseFordelingTjeneste;

class VurderUttakDokumentasjonAksjonspunktUtlederTest {

    private final UttakRepositoryStubProvider uttakRepositoryProvider = new UttakRepositoryStubProvider();
    private final VurderUttakDokumentasjonAksjonspunktUtleder utleder = new VurderUttakDokumentasjonAksjonspunktUtleder(new YtelseFordelingTjeneste(uttakRepositoryProvider.getYtelsesFordelingRepository()),
        new AktivitetskravDokumentasjonUtleder(new ForeldrepengerUttakTjeneste(uttakRepositoryProvider.getFpUttakRepository())));

    @Test
    void skal_utlede_aksjonspunkt_for_alle_typer() {
        var fødselsdato = LocalDate.of(2022, 11, 16);
        var tidligOppstart = OppgittPeriodeBuilder.ny()
            .medPeriodeType(UttakPeriodeType.FEDREKVOTE)
            .medPeriode(fødselsdato, fødselsdato.plusWeeks(4).minusDays(1))
            .medPeriodeKilde(FordelingPeriodeKilde.SØKNAD)
            .build();
        var utsettelseSykdom = OppgittPeriodeBuilder.ny()
            .medPeriode(fødselsdato.plusWeeks(4), fødselsdato.plusWeeks(6).minusDays(1))
            .medPeriodeKilde(FordelingPeriodeKilde.SØKNAD)
            .medÅrsak(UtsettelseÅrsak.SYKDOM)
            .build();
        var fellesperiode = OppgittPeriodeBuilder.ny()
            .medPeriodeType(UttakPeriodeType.FELLESPERIODE)
            .medPeriode(fødselsdato.plusWeeks(6), fødselsdato.plusWeeks(10).minusDays(1))
            .medPeriodeKilde(FordelingPeriodeKilde.SØKNAD)
            .medMorsAktivitet(MorsAktivitet.ARBEID)
            .build();
        var overføring = OppgittPeriodeBuilder.ny()
            .medPeriodeType(UttakPeriodeType.MØDREKVOTE)
            .medPeriode(fødselsdato.plusWeeks(10), fødselsdato.plusWeeks(12).minusDays(1))
            .medPeriodeKilde(FordelingPeriodeKilde.SØKNAD)
            .medÅrsak(OverføringÅrsak.INSTITUSJONSOPPHOLD_ANNEN_FORELDER)
            .build();
        var fedrekvote = OppgittPeriodeBuilder.ny()
            .medPeriodeType(UttakPeriodeType.FEDREKVOTE)
            .medPeriode(fødselsdato.plusWeeks(12), fødselsdato.plusWeeks(13).minusDays(1))
            .medPeriodeKilde(FordelingPeriodeKilde.SØKNAD)
            .build();
        var perioder = List.of(tidligOppstart, utsettelseSykdom, fellesperiode, overføring, fedrekvote);
        var scenario = ScenarioFarSøkerForeldrepenger.forFødsel()
            .medOppgittRettighet(new OppgittRettighetEntitet(true, false, false, false))
            .medJustertFordeling(new OppgittFordelingEntitet(perioder, true));
        var behandling = scenario.lagre(uttakRepositoryProvider);

        var familieHendelse = FamilieHendelse.forFødsel(fødselsdato, fødselsdato, List.of(), 0);
        var fpGrunnlag = new ForeldrepengerGrunnlag()
            .medFamilieHendelser(new FamilieHendelser().medBekreftetHendelse(familieHendelse));
        var input = new UttakInput(BehandlingReferanse.fra(behandling), null, fpGrunnlag).medSkalBrukeNyFaktaOmUttak(true);
        var aksjonspunktDefinisjon = utleder.utledAksjonspunkterFor(input);

        assertThat(aksjonspunktDefinisjon).isPresent();
        assertThat(aksjonspunktDefinisjon.get()).isEqualTo(AksjonspunktDefinisjon.VURDER_UTTAK_DOKUMENTASJON);

        var behov = utleder.utledDokumentasjonVurderingBehov(input)
            .stream().sorted(Comparator.comparing(dokumentasjonVurderingBehov -> dokumentasjonVurderingBehov.oppgittPeriode().getFom()))
            .toList();
        assertThat(behov).hasSize(5);
        assertThat(behov.get(0).måVurderes()).isTrue();
        assertThat(behov.get(1).måVurderes()).isTrue();
        assertThat(behov.get(2).måVurderes()).isTrue();
        assertThat(behov.get(3).måVurderes()).isTrue();
        assertThat(behov.get(4).måVurderes()).isFalse(); //Fedrevoten

        assertThat(behov.get(0).behov().årsak()).isEqualTo(DokumentasjonVurderingBehov.Behov.Årsak.TIDLIG_OPPSTART_FAR);
        assertThat(behov.get(0).behov().type()).isEqualTo(DokumentasjonVurderingBehov.Behov.Type.UTTAK);

        assertThat(behov.get(1).behov().årsak()).isEqualTo(DokumentasjonVurderingBehov.Behov.Årsak.SYKDOM_SØKER);
        assertThat(behov.get(1).behov().type()).isEqualTo(DokumentasjonVurderingBehov.Behov.Type.UTSETTELSE);

        assertThat(behov.get(2).behov().årsak()).isEqualTo(DokumentasjonVurderingBehov.Behov.Årsak.AKTIVITETSKRAV_ARBEID);
        assertThat(behov.get(2).behov().type()).isEqualTo(DokumentasjonVurderingBehov.Behov.Type.UTTAK);

        assertThat(behov.get(3).behov().årsak()).isEqualTo(DokumentasjonVurderingBehov.Behov.Årsak.INNLEGGELSE_ANNEN_FORELDER);
        assertThat(behov.get(3).behov().type()).isEqualTo(DokumentasjonVurderingBehov.Behov.Type.OVERFØRING);

        assertThat(behov.get(4).behov()).isNull();
    }

    @Test
    void skal_håndtere_manglende_yfa() {
        var scenario = ScenarioFarSøkerForeldrepenger.forFødsel();
        var behandling = scenario.lagre(uttakRepositoryProvider);

        var familieHendelse = FamilieHendelse.forFødsel(LocalDate.now(), LocalDate.now(), List.of(), 0);
        var fpGrunnlag = new ForeldrepengerGrunnlag()
            .medFamilieHendelser(new FamilieHendelser().medBekreftetHendelse(familieHendelse));
        var input = new UttakInput(BehandlingReferanse.fra(behandling), null, fpGrunnlag).medSkalBrukeNyFaktaOmUttak(true);
        var aksjonspunktDefinisjon = utleder.utledAksjonspunkterFor(input);
        var behov = utleder.utledDokumentasjonVurderingBehov(input);

        assertThat(aksjonspunktDefinisjon).isEmpty();
        assertThat(behov).isEmpty();
    }
}
