package no.nav.foreldrepenger.domene.uttak.fastsetteperioder.grunnlagbyggere;

import static no.nav.foreldrepenger.regler.uttak.fastsetteperiode.grunnlag.DokumentasjonVurdering.HV_OVELSE_DOKUMENTERT;
import static no.nav.foreldrepenger.regler.uttak.fastsetteperiode.grunnlag.DokumentasjonVurdering.INNLEGGELSE_BARN_DOKUMENTERT;
import static no.nav.foreldrepenger.regler.uttak.fastsetteperiode.grunnlag.DokumentasjonVurdering.MORS_AKTIVITET_DOKUMENTERT_AKTIVITET;
import static no.nav.foreldrepenger.regler.uttak.fastsetteperiode.grunnlag.DokumentasjonVurdering.MORS_AKTIVITET_DOKUMENTERT_IKKE_AKTIVITET;
import static no.nav.foreldrepenger.regler.uttak.fastsetteperiode.grunnlag.DokumentasjonVurdering.MORS_AKTIVITET_IKKE_DOKUMENTERT;
import static no.nav.foreldrepenger.regler.uttak.fastsetteperiode.grunnlag.DokumentasjonVurdering.SYKDOM_SØKER_DOKUMENTERT;
import static no.nav.foreldrepenger.regler.uttak.fastsetteperiode.grunnlag.DokumentasjonVurdering.TIDLIG_OPPSTART_FEDREKVOTE_DOKUMENTERT;
import static org.assertj.core.api.Assertions.assertThat;

import java.time.LocalDate;
import java.util.List;

import org.junit.jupiter.api.Test;

import no.nav.foreldrepenger.behandling.BehandlingReferanse;
import no.nav.foreldrepenger.behandlingslager.behandling.ytelsefordeling.AktivitetskravPeriodeEntitet;
import no.nav.foreldrepenger.behandlingslager.behandling.ytelsefordeling.AktivitetskravPerioderEntitet;
import no.nav.foreldrepenger.behandlingslager.behandling.ytelsefordeling.KontrollerAktivitetskravAvklaring;
import no.nav.foreldrepenger.behandlingslager.behandling.ytelsefordeling.PeriodeUttakDokumentasjonEntitet;
import no.nav.foreldrepenger.behandlingslager.behandling.ytelsefordeling.PerioderUttakDokumentasjonEntitet;
import no.nav.foreldrepenger.behandlingslager.behandling.ytelsefordeling.UttakDokumentasjonType;
import no.nav.foreldrepenger.behandlingslager.behandling.ytelsefordeling.periode.OppgittFordelingEntitet;
import no.nav.foreldrepenger.behandlingslager.behandling.ytelsefordeling.periode.OppgittPeriodeBuilder;
import no.nav.foreldrepenger.behandlingslager.behandling.ytelsefordeling.periode.UttakPeriodeType;
import no.nav.foreldrepenger.behandlingslager.behandling.ytelsefordeling.periode.UttakPeriodeVurderingType;
import no.nav.foreldrepenger.behandlingslager.behandling.ytelsefordeling.årsak.UtsettelseÅrsak;
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
        var grunnlag = søknadGrunnlagBygger.byggGrunnlag(input).build();

        assertThat(grunnlag.getOppgittePerioder()).hasSize(1);
        assertThat(grunnlag.getOppgittePerioder().get(0).getStønadskontotype()).isEqualTo(Stønadskontotype.FELLESPERIODE);
        assertThat(grunnlag.getOppgittePerioder().get(0).getFom()).isEqualTo(fom);
        assertThat(grunnlag.getOppgittePerioder().get(0).getTom()).isEqualTo(tom);
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

        var søknadsperiode1 = OppgittPeriodeBuilder.ny()
            .medPeriodeType(UttakPeriodeType.FELLESPERIODE)
            .medPeriode(LocalDate.of(2020, 12, 12), LocalDate.of(2020, 12, 13))
            .build();
        var søknadsperiode2 = OppgittPeriodeBuilder.ny()
            .medPeriodeType(UttakPeriodeType.FELLESPERIODE)
            .medPeriode(LocalDate.of(2021, 1, 1), LocalDate.of(2021, 2, 1))
            .build();
        var aktivitetskravPeriode1 = new AktivitetskravPeriodeEntitet(søknadsperiode1.getFom(), søknadsperiode1.getTom(),
            KontrollerAktivitetskravAvklaring.IKKE_I_AKTIVITET_DOKUMENTERT, "oki.");
        var aktivitetskravPeriode2 = new AktivitetskravPeriodeEntitet(søknadsperiode2.getFom().plusWeeks(1), søknadsperiode2.getFom().plusWeeks(2),
            KontrollerAktivitetskravAvklaring.I_AKTIVITET, "oki.");
        var aktivitetskravPeriode3 = new AktivitetskravPeriodeEntitet(søknadsperiode2.getFom().plusWeeks(3), søknadsperiode2.getTom().plusWeeks(1),
            KontrollerAktivitetskravAvklaring.IKKE_I_AKTIVITET_IKKE_DOKUMENTERT, "oki.");
        var behandling = ScenarioFarSøkerForeldrepenger.forFødsel()
            .medFordeling(new OppgittFordelingEntitet(List.of(søknadsperiode1, søknadsperiode2), true))
            .medAktivitetskravPerioder(List.of(aktivitetskravPeriode1, aktivitetskravPeriode2, aktivitetskravPeriode3))
            .lagre(repositoryProvider);
        var ytelsespesifiktGrunnlag = fpGrunnlag(søknadsperiode1.getFom());
        var input = new UttakInput(BehandlingReferanse.fra(behandling), null, ytelsespesifiktGrunnlag);
        var grunnlag = søknadGrunnlagBygger.byggGrunnlag(input).build();

        var perioderMedAvklartMorsAktivitet = grunnlag.getOppgittePerioder();

        assertThat(perioderMedAvklartMorsAktivitet).hasSize(5);
        assertThat(perioderMedAvklartMorsAktivitet.get(0).getFom()).isEqualTo(aktivitetskravPeriode1.getTidsperiode().getFomDato());
        assertThat(perioderMedAvklartMorsAktivitet.get(0).getTom()).isEqualTo(aktivitetskravPeriode1.getTidsperiode().getTomDato());
        assertThat(perioderMedAvklartMorsAktivitet.get(0).getDokumentasjonVurdering()).isEqualTo(MORS_AKTIVITET_DOKUMENTERT_IKKE_AKTIVITET);

        assertThat(perioderMedAvklartMorsAktivitet.get(1).getFom()).isEqualTo(søknadsperiode2.getFom());
        assertThat(perioderMedAvklartMorsAktivitet.get(1).getTom()).isEqualTo(aktivitetskravPeriode2.getTidsperiode().getFomDato().minusDays(1));
        assertThat(perioderMedAvklartMorsAktivitet.get(1).getDokumentasjonVurdering()).isNull();

        assertThat(perioderMedAvklartMorsAktivitet.get(2).getFom()).isEqualTo(aktivitetskravPeriode2.getTidsperiode().getFomDato());
        assertThat(perioderMedAvklartMorsAktivitet.get(2).getTom()).isEqualTo(aktivitetskravPeriode2.getTidsperiode().getTomDato());
        assertThat(perioderMedAvklartMorsAktivitet.get(2).getDokumentasjonVurdering()).isEqualTo(MORS_AKTIVITET_DOKUMENTERT_AKTIVITET);

        assertThat(perioderMedAvklartMorsAktivitet.get(3).getFom()).isEqualTo(aktivitetskravPeriode2.getTidsperiode().getTomDato().plusDays(1));
        assertThat(perioderMedAvklartMorsAktivitet.get(3).getTom()).isEqualTo(aktivitetskravPeriode3.getTidsperiode().getFomDato().minusDays(1));
        assertThat(perioderMedAvklartMorsAktivitet.get(3).getDokumentasjonVurdering()).isNull();

        assertThat(perioderMedAvklartMorsAktivitet.get(4).getFom()).isEqualTo(aktivitetskravPeriode3.getTidsperiode().getFomDato());
        assertThat(perioderMedAvklartMorsAktivitet.get(4).getTom()).isEqualTo(søknadsperiode2.getTom());
        assertThat(perioderMedAvklartMorsAktivitet.get(4).getDokumentasjonVurdering()).isEqualTo(MORS_AKTIVITET_IKKE_DOKUMENTERT);
    }

    @Test
    public void byggerAktivitetskravPerioderPåFriUtsettelse() {
        var repositoryProvider = new UttakRepositoryStubProvider();
        var ytelsesFordelingRepository = repositoryProvider.getYtelsesFordelingRepository();
        var søknadGrunnlagBygger = new SøknadGrunnlagBygger(ytelsesFordelingRepository);

        var søknadsperiode1 = OppgittPeriodeBuilder.ny()
            .medÅrsak(UtsettelseÅrsak.FRI)
            .medPeriode(LocalDate.of(2020, 12, 12), LocalDate.of(2020, 12, 13))
            .build();
        var søknadsperiode2 = OppgittPeriodeBuilder.ny()
            .medÅrsak(UtsettelseÅrsak.FRI)
            .medPeriode(LocalDate.of(2021, 1, 1), LocalDate.of(2021, 2, 1))
            .build();
        var aktivitetskravPeriode1 = new AktivitetskravPeriodeEntitet(søknadsperiode1.getFom(), søknadsperiode1.getTom(),
            KontrollerAktivitetskravAvklaring.IKKE_I_AKTIVITET_DOKUMENTERT, "oki.");
        var aktivitetskravPeriode2 = new AktivitetskravPeriodeEntitet(søknadsperiode2.getFom().plusWeeks(1), søknadsperiode2.getFom().plusWeeks(2),
            KontrollerAktivitetskravAvklaring.I_AKTIVITET, "oki.");
        var aktivitetskravPeriode3 = new AktivitetskravPeriodeEntitet(søknadsperiode2.getFom().plusWeeks(3), søknadsperiode2.getTom(),
            KontrollerAktivitetskravAvklaring.IKKE_I_AKTIVITET_IKKE_DOKUMENTERT, "oki.");
        var behandling = ScenarioFarSøkerForeldrepenger.forFødsel()
            .medFordeling(new OppgittFordelingEntitet(List.of(søknadsperiode1, søknadsperiode2), true))
            .medAktivitetskravPerioder(List.of(aktivitetskravPeriode1, aktivitetskravPeriode2, aktivitetskravPeriode3))
            .lagre(repositoryProvider);
        var ytelsespesifiktGrunnlag = fpGrunnlag(søknadsperiode1.getFom());
        var input = new UttakInput(BehandlingReferanse.fra(behandling), null, ytelsespesifiktGrunnlag);
        var grunnlag = søknadGrunnlagBygger.byggGrunnlag(input).build();

        var perioderMedAvklartMorsAktivitet = grunnlag.getOppgittePerioder();

        assertThat(perioderMedAvklartMorsAktivitet).hasSize(5);
        assertThat(perioderMedAvklartMorsAktivitet.get(0).getFom()).isEqualTo(aktivitetskravPeriode1.getTidsperiode().getFomDato());
        assertThat(perioderMedAvklartMorsAktivitet.get(0).getTom()).isEqualTo(aktivitetskravPeriode1.getTidsperiode().getTomDato());
        assertThat(perioderMedAvklartMorsAktivitet.get(0).getDokumentasjonVurdering()).isEqualTo(MORS_AKTIVITET_DOKUMENTERT_IKKE_AKTIVITET);

        assertThat(perioderMedAvklartMorsAktivitet.get(1).getFom()).isEqualTo(søknadsperiode2.getFom());
        assertThat(perioderMedAvklartMorsAktivitet.get(1).getTom()).isEqualTo(aktivitetskravPeriode2.getTidsperiode().getFomDato().minusDays(1));
        assertThat(perioderMedAvklartMorsAktivitet.get(1).getDokumentasjonVurdering()).isNull();

        assertThat(perioderMedAvklartMorsAktivitet.get(2).getFom()).isEqualTo(aktivitetskravPeriode2.getTidsperiode().getFomDato());
        assertThat(perioderMedAvklartMorsAktivitet.get(2).getTom()).isEqualTo(aktivitetskravPeriode2.getTidsperiode().getTomDato());
        assertThat(perioderMedAvklartMorsAktivitet.get(2).getDokumentasjonVurdering()).isEqualTo(MORS_AKTIVITET_DOKUMENTERT_AKTIVITET);

        assertThat(perioderMedAvklartMorsAktivitet.get(3).getFom()).isEqualTo(aktivitetskravPeriode2.getTidsperiode().getTomDato().plusDays(1));
        assertThat(perioderMedAvklartMorsAktivitet.get(3).getTom()).isEqualTo(aktivitetskravPeriode3.getTidsperiode().getFomDato().minusDays(1));
        assertThat(perioderMedAvklartMorsAktivitet.get(3).getDokumentasjonVurdering()).isNull();

        assertThat(perioderMedAvklartMorsAktivitet.get(4).getFom()).isEqualTo(aktivitetskravPeriode3.getTidsperiode().getFomDato());
        assertThat(perioderMedAvklartMorsAktivitet.get(4).getTom()).isEqualTo(søknadsperiode2.getTom());
        assertThat(perioderMedAvklartMorsAktivitet.get(4).getDokumentasjonVurdering()).isEqualTo(MORS_AKTIVITET_IKKE_DOKUMENTERT);
    }

    @Test
    public void oversetterUttakDokRiktig() {
        var repositoryProvider = new UttakRepositoryStubProvider();
        var ytelsesFordelingRepository = repositoryProvider.getYtelsesFordelingRepository();
        var søknadGrunnlagBygger = new SøknadGrunnlagBygger(ytelsesFordelingRepository);

        var fom = LocalDate.of(2020, 12, 12);
        var tom = LocalDate.of(2021, 12, 12);
        var utsettelseSykdom = OppgittPeriodeBuilder.ny()
            .medÅrsak(UtsettelseÅrsak.SYKDOM)
            .medPeriode(fom, tom)
            .build();

        var utsettelseInnleggelse = OppgittPeriodeBuilder.ny()
            .medÅrsak(UtsettelseÅrsak.INSTITUSJON_BARN)
            .medPeriode(tom.plusDays(1), tom.plusWeeks(10))
            .build();
        var behandling = ScenarioFarSøkerForeldrepenger.forFødsel()
            .medFordeling(new OppgittFordelingEntitet(List.of(utsettelseSykdom, utsettelseInnleggelse), true))
            .lagre(repositoryProvider);
        var dokPerioder = new PerioderUttakDokumentasjonEntitet();
        dokPerioder.leggTil(new PeriodeUttakDokumentasjonEntitet(utsettelseSykdom.getFom(), utsettelseSykdom.getTom(), UttakDokumentasjonType.SYK_SØKER));
        dokPerioder.leggTil(new PeriodeUttakDokumentasjonEntitet(utsettelseInnleggelse.getFom().plusWeeks(1), utsettelseInnleggelse.getFom().plusWeeks(2), UttakDokumentasjonType.INNLAGT_BARN));
        dokPerioder.leggTil(new PeriodeUttakDokumentasjonEntitet(utsettelseInnleggelse.getFom().plusWeeks(3), utsettelseInnleggelse.getTom().plusWeeks(2), UttakDokumentasjonType.INNLAGT_BARN));
        var ytelseFordelingAggregat = ytelsesFordelingRepository.opprettBuilder(behandling.getId())
            .medPerioderUttakDokumentasjon(new PerioderUttakDokumentasjonEntitet(dokPerioder))
            .build();
        ytelsesFordelingRepository.lagre(behandling.getId(), ytelseFordelingAggregat);

        var ytelsespesifiktGrunnlag = fpGrunnlag(fom);
        var input = new UttakInput(BehandlingReferanse.fra(behandling), null, ytelsespesifiktGrunnlag);
        var grunnlag = søknadGrunnlagBygger.byggGrunnlag(input).build();

        assertThat(grunnlag.getOppgittePerioder()).hasSize(5);
        assertThat(grunnlag.getOppgittePerioder().get(0).getDokumentasjonVurdering()).isEqualTo(SYKDOM_SØKER_DOKUMENTERT);
        assertThat(grunnlag.getOppgittePerioder().get(0).getFom()).isEqualTo(utsettelseSykdom.getFom());
        assertThat(grunnlag.getOppgittePerioder().get(0).getTom()).isEqualTo(utsettelseSykdom.getTom());
        assertThat(grunnlag.getOppgittePerioder().get(1).getDokumentasjonVurdering()).isNull();
        assertThat(grunnlag.getOppgittePerioder().get(1).getFom()).isEqualTo(utsettelseInnleggelse.getFom());
        assertThat(grunnlag.getOppgittePerioder().get(1).getTom()).isEqualTo(utsettelseInnleggelse.getFom().plusWeeks(1).minusDays(1));
        assertThat(grunnlag.getOppgittePerioder().get(2).getDokumentasjonVurdering()).isEqualTo(INNLEGGELSE_BARN_DOKUMENTERT);
        assertThat(grunnlag.getOppgittePerioder().get(2).getFom()).isEqualTo(utsettelseInnleggelse.getFom().plusWeeks(1));
        assertThat(grunnlag.getOppgittePerioder().get(2).getTom()).isEqualTo(utsettelseInnleggelse.getFom().plusWeeks(2));
        assertThat(grunnlag.getOppgittePerioder().get(3).getDokumentasjonVurdering()).isNull();
        assertThat(grunnlag.getOppgittePerioder().get(3).getFom()).isEqualTo(utsettelseInnleggelse.getFom().plusWeeks(2).plusDays(1));
        assertThat(grunnlag.getOppgittePerioder().get(3).getTom()).isEqualTo(utsettelseInnleggelse.getFom().plusWeeks(3).minusDays(1));
        assertThat(grunnlag.getOppgittePerioder().get(4).getDokumentasjonVurdering()).isEqualTo(INNLEGGELSE_BARN_DOKUMENTERT);
        assertThat(grunnlag.getOppgittePerioder().get(4).getFom()).isEqualTo(utsettelseInnleggelse.getFom().plusWeeks(3));
        assertThat(grunnlag.getOppgittePerioder().get(4).getTom()).isEqualTo(utsettelseInnleggelse.getTom());
    }

    @Test
    public void prioritererUtsettelseDokOverAktkravHvisOverlapp() {
        var repositoryProvider = new UttakRepositoryStubProvider();
        var ytelsesFordelingRepository = repositoryProvider.getYtelsesFordelingRepository();
        var søknadGrunnlagBygger = new SøknadGrunnlagBygger(ytelsesFordelingRepository);

        var fom = LocalDate.of(2020, 12, 12);
        var tom = LocalDate.of(2021, 12, 12);
        var utsettelseSykdom = OppgittPeriodeBuilder.ny()
            .medÅrsak(UtsettelseÅrsak.SYKDOM)
            .medPeriode(fom, tom)
            .build();
        var utsettelseHV = OppgittPeriodeBuilder.ny()
            .medÅrsak(UtsettelseÅrsak.HV_OVELSE)
            .medPeriode(tom.plusDays(1), tom.plusWeeks(5))
            .build();

        var behandling = ScenarioFarSøkerForeldrepenger.forFødsel()
            .medFordeling(new OppgittFordelingEntitet(List.of(utsettelseSykdom, utsettelseHV), true))
            .lagre(repositoryProvider);
        var dokPerioder = new PerioderUttakDokumentasjonEntitet();
        dokPerioder.leggTil(new PeriodeUttakDokumentasjonEntitet(utsettelseSykdom.getFom(), utsettelseSykdom.getTom(), UttakDokumentasjonType.SYK_SØKER));
        dokPerioder.leggTil(new PeriodeUttakDokumentasjonEntitet(utsettelseHV.getFom(), utsettelseHV.getTom(), UttakDokumentasjonType.HV_OVELSE));
        var aktivitetskravPeriode1 = new AktivitetskravPeriodeEntitet(utsettelseSykdom.getFom(), utsettelseSykdom.getTom(),
            KontrollerAktivitetskravAvklaring.I_AKTIVITET, "oki.");
        var aktivitetskravPeriode2 = new AktivitetskravPeriodeEntitet(utsettelseHV.getFom().plusWeeks(1), utsettelseHV.getTom().minusWeeks(1),
            KontrollerAktivitetskravAvklaring.I_AKTIVITET, "oki.");
        var aktivitetskravPerioder = new AktivitetskravPerioderEntitet();
        aktivitetskravPerioder.leggTil(aktivitetskravPeriode1);
        aktivitetskravPerioder.leggTil(aktivitetskravPeriode2);
        var ytelseFordelingAggregat = ytelsesFordelingRepository.opprettBuilder(behandling.getId())
            .medPerioderUttakDokumentasjon(new PerioderUttakDokumentasjonEntitet(dokPerioder))
            .medSaksbehandledeAktivitetskravPerioder(aktivitetskravPerioder)
            .build();
        ytelsesFordelingRepository.lagre(behandling.getId(), ytelseFordelingAggregat);

        var ytelsespesifiktGrunnlag = fpGrunnlag(fom);
        var input = new UttakInput(BehandlingReferanse.fra(behandling), null, ytelsespesifiktGrunnlag);
        var grunnlag = søknadGrunnlagBygger.byggGrunnlag(input).build();

        assertThat(grunnlag.getOppgittePerioder()).hasSize(4);
        assertThat(grunnlag.getOppgittePerioder().get(0).getDokumentasjonVurdering()).isEqualTo(SYKDOM_SØKER_DOKUMENTERT);
        assertThat(grunnlag.getOppgittePerioder().get(0).getFom()).isEqualTo(utsettelseSykdom.getFom());
        assertThat(grunnlag.getOppgittePerioder().get(0).getTom()).isEqualTo(utsettelseSykdom.getTom());

        assertThat(grunnlag.getOppgittePerioder().get(1).getDokumentasjonVurdering()).isEqualTo(HV_OVELSE_DOKUMENTERT);
        assertThat(grunnlag.getOppgittePerioder().get(1).getFom()).isEqualTo(utsettelseHV.getFom());
        assertThat(grunnlag.getOppgittePerioder().get(1).getTom()).isEqualTo(aktivitetskravPeriode2.getTidsperiode().getFomDato().minusDays(1));

        assertThat(grunnlag.getOppgittePerioder().get(2).getDokumentasjonVurdering()).isEqualTo(HV_OVELSE_DOKUMENTERT);
        assertThat(grunnlag.getOppgittePerioder().get(2).getFom()).isEqualTo(aktivitetskravPeriode2.getTidsperiode().getFomDato());
        assertThat(grunnlag.getOppgittePerioder().get(2).getTom()).isEqualTo(aktivitetskravPeriode2.getTidsperiode().getTomDato());

        assertThat(grunnlag.getOppgittePerioder().get(3).getDokumentasjonVurdering()).isEqualTo(HV_OVELSE_DOKUMENTERT);
        assertThat(grunnlag.getOppgittePerioder().get(3).getFom()).isEqualTo(aktivitetskravPeriode2.getTidsperiode().getTomDato().plusDays(1));
        assertThat(grunnlag.getOppgittePerioder().get(3).getTom()).isEqualTo(utsettelseHV.getTom());
    }

    @Test
    public void farTidligFedrekvoteSkalGiRiktigDokumentasjonVurdering() {
        var repositoryProvider = new UttakRepositoryStubProvider();
        var ytelsesFordelingRepository = repositoryProvider.getYtelsesFordelingRepository();
        var søknadGrunnlagBygger = new SøknadGrunnlagBygger(ytelsesFordelingRepository);

        var fødselsdato = LocalDate.of(2020, 12, 12);
        var fedrekvote = OppgittPeriodeBuilder.ny()
            .medPeriodeType(UttakPeriodeType.FEDREKVOTE)
            .medVurdering(UttakPeriodeVurderingType.PERIODE_OK)
            .medPeriode(fødselsdato, fødselsdato.plusWeeks(4))
            .build();

        var behandling = ScenarioFarSøkerForeldrepenger.forFødsel()
            .medFordeling(new OppgittFordelingEntitet(List.of(fedrekvote), true))
            .lagre(repositoryProvider);

        var ytelsespesifiktGrunnlag = fpGrunnlag(fødselsdato);
        var input = new UttakInput(BehandlingReferanse.fra(behandling), null, ytelsespesifiktGrunnlag);
        var grunnlag = søknadGrunnlagBygger.byggGrunnlag(input).build();

        assertThat(grunnlag.getOppgittePerioder()).hasSize(1);
        assertThat(grunnlag.getOppgittePerioder().get(0).getDokumentasjonVurdering()).isEqualTo(TIDLIG_OPPSTART_FEDREKVOTE_DOKUMENTERT);
        assertThat(grunnlag.getOppgittePerioder().get(0).getFom()).isEqualTo(fedrekvote.getFom());
        assertThat(grunnlag.getOppgittePerioder().get(0).getTom()).isEqualTo(fedrekvote.getTom());
    }
}
