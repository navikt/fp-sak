package no.nav.foreldrepenger.domene.uttak.fastsetteperioder.grunnlagbyggere;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.LocalDate;
import java.util.List;

import org.junit.jupiter.api.Test;

import no.nav.foreldrepenger.behandling.BehandlingReferanse;
import no.nav.foreldrepenger.behandlingslager.behandling.ytelsefordeling.AktivitetskravPeriodeEntitet;
import no.nav.foreldrepenger.behandlingslager.behandling.ytelsefordeling.KontrollerAktivitetskravAvklaring;
import no.nav.foreldrepenger.behandlingslager.behandling.ytelsefordeling.periode.OppgittFordelingEntitet;
import no.nav.foreldrepenger.behandlingslager.behandling.ytelsefordeling.periode.OppgittPeriodeBuilder;
import no.nav.foreldrepenger.behandlingslager.behandling.ytelsefordeling.periode.UttakPeriodeType;
import no.nav.foreldrepenger.domene.uttak.input.FamilieHendelse;
import no.nav.foreldrepenger.domene.uttak.input.FamilieHendelser;
import no.nav.foreldrepenger.domene.uttak.input.ForeldrepengerGrunnlag;
import no.nav.foreldrepenger.domene.uttak.input.UttakInput;
import no.nav.foreldrepenger.domene.uttak.testutilities.behandling.ScenarioFarSøkerForeldrepenger;
import no.nav.foreldrepenger.domene.uttak.testutilities.behandling.UttakRepositoryStubProvider;
import no.nav.foreldrepenger.regler.uttak.fastsetteperiode.grunnlag.Stønadskontotype;

class SøknadGrunnlagByggerTest {

    @Test
    public void byggerSøknadsperioder() {
        var repositoryProvider = new UttakRepositoryStubProvider();
        var ytelsesFordelingRepository = repositoryProvider.getYtelsesFordelingRepository();
        var søknadGrunnlagBygger = new SøknadGrunnlagBygger(ytelsesFordelingRepository);

        var fom = LocalDate.of(2020, 12, 12);
        var tom = LocalDate.of(2020, 12, 13);
        var søknadsperiode = OppgittPeriodeBuilder.ny()
            .medPeriodeType(UttakPeriodeType.FELLESPERIODE)
            .medPeriode(fom, tom)
            .build();
        var behandling = ScenarioFarSøkerForeldrepenger.forFødsel()
            .medFordeling(new OppgittFordelingEntitet(List.of(søknadsperiode), true))
            .lagre(repositoryProvider);
        var ytelsespesifiktGrunnlag = fpGrunnlag(fom);
        var input = new UttakInput(BehandlingReferanse.fra(behandling), null, ytelsespesifiktGrunnlag);
        var grunnlag = søknadGrunnlagBygger.byggGrunnlag(input);

        assertThat(grunnlag.build().getOppgittePerioder()).hasSize(1);
        assertThat(grunnlag.build().getOppgittePerioder().get(0).getStønadskontotype()).isEqualTo(Stønadskontotype.FELLESPERIODE);
        assertThat(grunnlag.build().getOppgittePerioder().get(0).getFom()).isEqualTo(fom);
        assertThat(grunnlag.build().getOppgittePerioder().get(0).getTom()).isEqualTo(tom);
    }

    private ForeldrepengerGrunnlag fpGrunnlag(LocalDate fødselsdato) {
        return new ForeldrepengerGrunnlag().medFamilieHendelser(
            new FamilieHendelser().medSøknadHendelse(FamilieHendelse.forFødsel(fødselsdato, fødselsdato, List.of(), 1)));
    }

    @Test
    public void byggerAktivitetskravPerioder() {
        var repositoryProvider = new UttakRepositoryStubProvider();
        var ytelsesFordelingRepository = repositoryProvider.getYtelsesFordelingRepository();
        var søknadGrunnlagBygger = new SøknadGrunnlagBygger(ytelsesFordelingRepository);

        var fom = LocalDate.of(2020, 12, 12);
        var tom = LocalDate.of(2020, 12, 13);
        var søknadsperiode = OppgittPeriodeBuilder.ny()
            .medPeriodeType(UttakPeriodeType.FELLESPERIODE)
            .medPeriode(fom, tom)
            .build();
        var aktivitetskravPeriode = new AktivitetskravPeriodeEntitet(fom, tom, KontrollerAktivitetskravAvklaring.IKKE_I_AKTIVITET_DOKUMENTERT,
            "oki.");
        var behandling = ScenarioFarSøkerForeldrepenger.forFødsel()
            .medFordeling(new OppgittFordelingEntitet(List.of(søknadsperiode), true))
            .medAktivitetskravPerioder(List.of(aktivitetskravPeriode))
            .lagre(repositoryProvider);
        var ytelsespesifiktGrunnlag = fpGrunnlag(fom);
        var input = new UttakInput(BehandlingReferanse.fra(behandling), null, ytelsespesifiktGrunnlag);
        var grunnlag = søknadGrunnlagBygger.byggGrunnlag(input);

        var perioderMedAvklartMorsAktivitet = grunnlag.build().getDokumentasjon().getPerioderMedAvklartMorsAktivitet();
        assertThat(perioderMedAvklartMorsAktivitet).hasSize(1);
        assertThat(perioderMedAvklartMorsAktivitet.get(0).getFom()).isEqualTo(aktivitetskravPeriode.getTidsperiode().getFomDato());
        assertThat(perioderMedAvklartMorsAktivitet.get(0).getTom()).isEqualTo(aktivitetskravPeriode.getTidsperiode().getTomDato());
        assertThat(perioderMedAvklartMorsAktivitet.get(0).erIAktivitet()).isFalse();
        assertThat(perioderMedAvklartMorsAktivitet.get(0).erDokumentert()).isTrue();
    }

}
