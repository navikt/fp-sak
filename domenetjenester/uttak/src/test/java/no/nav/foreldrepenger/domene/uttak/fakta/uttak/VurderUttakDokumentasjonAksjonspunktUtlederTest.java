package no.nav.foreldrepenger.domene.uttak.fakta.uttak;

import static org.assertj.core.api.Assertions.assertThat;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.Month;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.Test;

import no.nav.foreldrepenger.behandling.BehandlingReferanse;
import no.nav.foreldrepenger.behandlingslager.behandling.aktivitetskrav.AktivitetskravArbeidPeriodeEntitet;
import no.nav.foreldrepenger.behandlingslager.behandling.aktivitetskrav.AktivitetskravArbeidPerioderEntitet;
import no.nav.foreldrepenger.behandlingslager.behandling.aktivitetskrav.AktivitetskravGrunnlagEntitet;
import no.nav.foreldrepenger.behandlingslager.behandling.aktivitetskrav.AktivitetskravPermisjonType;
import no.nav.foreldrepenger.behandlingslager.behandling.ytelsefordeling.MorsAktivitet;
import no.nav.foreldrepenger.behandlingslager.behandling.ytelsefordeling.OppgittRettighetEntitet;
import no.nav.foreldrepenger.behandlingslager.behandling.ytelsefordeling.periode.FordelingPeriodeKilde;
import no.nav.foreldrepenger.behandlingslager.behandling.ytelsefordeling.periode.OppgittFordelingEntitet;
import no.nav.foreldrepenger.behandlingslager.behandling.ytelsefordeling.periode.OppgittPeriodeBuilder;
import no.nav.foreldrepenger.behandlingslager.behandling.ytelsefordeling.periode.UttakPeriodeType;
import no.nav.foreldrepenger.behandlingslager.behandling.ytelsefordeling.årsak.OverføringÅrsak;
import no.nav.foreldrepenger.behandlingslager.behandling.ytelsefordeling.årsak.UtsettelseÅrsak;
import no.nav.foreldrepenger.behandlingslager.virksomhet.OrgNummer;
import no.nav.foreldrepenger.domene.tid.DatoIntervallEntitet;
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
            .medOppgittRettighet(OppgittRettighetEntitet.beggeRett())
            .medJustertFordeling(new OppgittFordelingEntitet(perioder, true));
        var behandling = scenario.lagre(uttakRepositoryProvider);

        var familieHendelse = FamilieHendelse.forFødsel(fødselsdato, fødselsdato, List.of(), 0);
        var fpGrunnlag = new ForeldrepengerGrunnlag()
            .medFamilieHendelser(new FamilieHendelser().medBekreftetHendelse(familieHendelse));
        var input = new UttakInput(BehandlingReferanse.fra(behandling), null, null, fpGrunnlag);
        var utledetAp = utleder.utledAksjonspunktFor(input);

        assertThat(utledetAp).isTrue();

        var behov = utleder.utledDokumentasjonVurderingBehov(input)
            .stream().sorted(Comparator.comparing(dokumentasjonVurderingBehov -> dokumentasjonVurderingBehov.oppgittPeriode().getFom()))
            .toList();
        assertThat(behov).hasSize(4);
        assertThat(behov.get(0).måVurderes()).isTrue();
        assertThat(behov.get(1).måVurderes()).isTrue();
        assertThat(behov.get(2).måVurderes()).isTrue();
        assertThat(behov.get(3).måVurderes()).isTrue();

        assertThat(behov.get(0).behov().årsak()).isEqualTo(DokumentasjonVurderingBehov.Behov.Årsak.TIDLIG_OPPSTART_FAR);
        assertThat(behov.get(0).behov().type()).isEqualTo(DokumentasjonVurderingBehov.Behov.Type.UTTAK);

        assertThat(behov.get(1).behov().årsak()).isEqualTo(DokumentasjonVurderingBehov.Behov.Årsak.SYKDOM_SØKER);
        assertThat(behov.get(1).behov().type()).isEqualTo(DokumentasjonVurderingBehov.Behov.Type.UTSETTELSE);

        assertThat(behov.get(2).behov().årsak()).isEqualTo(DokumentasjonVurderingBehov.Behov.Årsak.AKTIVITETSKRAV_ARBEID);
        assertThat(behov.get(2).behov().type()).isEqualTo(DokumentasjonVurderingBehov.Behov.Type.UTTAK);

        assertThat(behov.get(3).behov().årsak()).isEqualTo(DokumentasjonVurderingBehov.Behov.Årsak.INNLEGGELSE_ANNEN_FORELDER);
        assertThat(behov.get(3).behov().type()).isEqualTo(DokumentasjonVurderingBehov.Behov.Type.OVERFØRING);
    }

    @Test
    void skal_håndtere_manglende_yfa() {
        var scenario = ScenarioFarSøkerForeldrepenger.forFødsel();
        var behandling = scenario.lagre(uttakRepositoryProvider);

        var familieHendelse = FamilieHendelse.forFødsel(LocalDate.now(), LocalDate.now(), List.of(), 0);
        var fpGrunnlag = new ForeldrepengerGrunnlag()
            .medFamilieHendelser(new FamilieHendelser().medBekreftetHendelse(familieHendelse));
        var input = new UttakInput(BehandlingReferanse.fra(behandling), null, null, fpGrunnlag);
        var aksjonspunktDefinisjon = utleder.utledAksjonspunktFor(input);
        var behov = utleder.utledDokumentasjonVurderingBehov(input);

        assertThat(aksjonspunktDefinisjon).isFalse();
        assertThat(behov).isEmpty();
    }

    @Test
    void utleder_ikke_ap_hvis_far_fellesperiode_og_mor_er_i_arbeid() {
        var fødselsdato = LocalDate.of(2025, 3, 14);

        var fellesperiode = OppgittPeriodeBuilder.ny()
            .medPeriodeType(UttakPeriodeType.FELLESPERIODE)
            .medPeriode(fødselsdato.plusWeeks(6), fødselsdato.plusWeeks(10).minusDays(1))
            .medPeriodeKilde(FordelingPeriodeKilde.SØKNAD)
            .medMorsAktivitet(MorsAktivitet.ARBEID)
            .build();

        var scenario = ScenarioFarSøkerForeldrepenger.forFødsel()
            .medOppgittRettighet(OppgittRettighetEntitet.beggeRett())
            .medJustertFordeling(new OppgittFordelingEntitet(List.of(fellesperiode), true));
        var behandling = scenario.lagre(uttakRepositoryProvider);

        var familieHendelse = FamilieHendelse.forFødsel(fødselsdato, fødselsdato, List.of(), 0);
        var fpGrunnlag = new ForeldrepengerGrunnlag()
            .medMottattMorsArbeidDokument(true)
            .medAktivitetskravGrunnlag(AktivitetskravGrunnlagEntitet.Builder.oppdatere(Optional.empty())
                .medPerioderMedAktivitetskravArbeid(new AktivitetskravArbeidPerioderEntitet.Builder()
                    .leggTil(new AktivitetskravArbeidPeriodeEntitet.Builder()
                        .medPeriode(fellesperiode.getFom(), fellesperiode.getTom())
                        .medSumStillingsprosent(BigDecimal.valueOf(100))
                        .medOrgNummer(OrgNummer.KUNSTIG_ORG)
                        .medPermisjon(BigDecimal.ZERO, AktivitetskravPermisjonType.UDEFINERT))
                    .build())
                .build())
            .medFamilieHendelser(new FamilieHendelser().medBekreftetHendelse(familieHendelse));
        var input = new UttakInput(BehandlingReferanse.fra(behandling), null, null, fpGrunnlag);
        var utledetAp = utleder.utledAksjonspunktFor(input);

        assertThat(utledetAp).isFalse();

        var behov = utleder.utledDokumentasjonVurderingBehov(input)
            .stream().sorted(Comparator.comparing(dokumentasjonVurderingBehov -> dokumentasjonVurderingBehov.oppgittPeriode().getFom()))
            .toList();
        assertThat(behov).hasSize(1);
        assertThat(behov.getFirst().måVurderes()).isFalse();

        assertThat(behov.getFirst().behov().årsak()).isEqualTo(DokumentasjonVurderingBehov.Behov.Årsak.AKTIVITETSKRAV_ARBEID);
        assertThat(behov.getFirst().behov().type()).isEqualTo(DokumentasjonVurderingBehov.Behov.Type.UTTAK);

        assertThat(behov.getFirst().registerVurdering()).isEqualTo(RegisterVurdering.MORS_AKTIVITET_GODKJENT);
        assertThat(behov.getFirst().vurdering()).isNull();
    }

    @Test
    void utleder_ikke_ap_hvis_bare_far_foreldrepenger_og_mor_er_i_arbeid() {
        var fødselsdato = LocalDate.of(2025, Month.MAY, 14);

        var foreldrepengerMinsterett = OppgittPeriodeBuilder.ny()
            .medPeriodeType(UttakPeriodeType.FORELDREPENGER)
            .medPeriode(fødselsdato, fødselsdato.plusWeeks(2).minusDays(1))
            .medPeriodeKilde(FordelingPeriodeKilde.SØKNAD)
            .medMorsAktivitet(MorsAktivitet.IKKE_OPPGITT)
            .build();
        var foreldrepengerUtsettelse = OppgittPeriodeBuilder.ny()
            .medÅrsak(UtsettelseÅrsak.FRI)
            .medPeriode(fødselsdato.plusWeeks(6), fødselsdato.plusWeeks(10).minusDays(1))
            .medPeriodeKilde(FordelingPeriodeKilde.SØKNAD)
            .medMorsAktivitet(MorsAktivitet.ARBEID)
            .build();
        var foreldrepengerMedArbeid = OppgittPeriodeBuilder.ny()
            .medPeriodeType(UttakPeriodeType.FORELDREPENGER)
            .medPeriode(fødselsdato.plusWeeks(10), fødselsdato.plusWeeks(14).minusDays(1))
            .medPeriodeKilde(FordelingPeriodeKilde.SØKNAD)
            .medMorsAktivitet(MorsAktivitet.ARBEID)
            .build();

        var scenario = ScenarioFarSøkerForeldrepenger.forFødsel()
            .medOppgittRettighet(OppgittRettighetEntitet.bareSøkerRett())
            .medJustertFordeling(new OppgittFordelingEntitet(List.of(foreldrepengerMinsterett, foreldrepengerUtsettelse, foreldrepengerMedArbeid), true));
        var behandling = scenario.lagre(uttakRepositoryProvider);

        var familieHendelse = FamilieHendelse.forFødsel(fødselsdato, fødselsdato, List.of(), 0);
        var fpGrunnlag = new ForeldrepengerGrunnlag()
            .medMottattMorsArbeidDokument(true)
            .medAktivitetskravGrunnlag(AktivitetskravGrunnlagEntitet.Builder.oppdatere(Optional.empty())
                .medPerioderMedAktivitetskravArbeid(new AktivitetskravArbeidPerioderEntitet.Builder()
                    .leggTil(new AktivitetskravArbeidPeriodeEntitet.Builder()
                        .medPeriode(foreldrepengerUtsettelse.getFom(), foreldrepengerMedArbeid.getTom())
                        .medSumStillingsprosent(BigDecimal.valueOf(100))
                        .medOrgNummer(OrgNummer.KUNSTIG_ORG)
                        .medPermisjon(BigDecimal.ZERO, AktivitetskravPermisjonType.UDEFINERT))
                    .build())
                .build())
            .medFamilieHendelser(new FamilieHendelser().medBekreftetHendelse(familieHendelse));
        var input = new UttakInput(BehandlingReferanse.fra(behandling), null, null, fpGrunnlag);
        var utledetAp = utleder.utledAksjonspunktFor(input);

        assertThat(utledetAp).isFalse();

        var behov = utleder.utledDokumentasjonVurderingBehov(input)
            .stream().sorted(Comparator.comparing(dokumentasjonVurderingBehov -> dokumentasjonVurderingBehov.oppgittPeriode().getFom()))
            .toList();
        assertThat(behov).hasSize(2);
        assertThat(behov.getFirst().måVurderes()).isFalse();
        assertThat(behov.getLast().måVurderes()).isFalse();

        assertThat(behov.getFirst().behov().årsak()).isEqualTo(DokumentasjonVurderingBehov.Behov.Årsak.AKTIVITETSKRAV_ARBEID);
        assertThat(behov.getFirst().behov().type()).isEqualTo(DokumentasjonVurderingBehov.Behov.Type.UTSETTELSE);

        assertThat(behov.getFirst().registerVurdering()).isEqualTo(RegisterVurdering.MORS_AKTIVITET_GODKJENT);
        assertThat(behov.getFirst().vurdering()).isNull();

        assertThat(behov.getLast().behov().årsak()).isEqualTo(DokumentasjonVurderingBehov.Behov.Årsak.AKTIVITETSKRAV_ARBEID);
        assertThat(behov.getLast().behov().type()).isEqualTo(DokumentasjonVurderingBehov.Behov.Type.UTTAK);

        assertThat(behov.getLast().registerVurdering()).isEqualTo(RegisterVurdering.MORS_AKTIVITET_GODKJENT);
        assertThat(behov.getLast().vurdering()).isNull();
    }

    @Test
    void skal_finne_over_75_ingen_permisjoner_ett_arbeid() {
        var aaPerioder = new AktivitetskravArbeidPerioderEntitet.Builder()
            .leggTil(new AktivitetskravArbeidPeriodeEntitet.Builder()
                .medPeriode(LocalDate.now(), LocalDate.now().plusWeeks(1))
                .medOrgNummer(OrgNummer.KUNSTIG_ORG)
                .medSumStillingsprosent(BigDecimal.valueOf(100))
                .medPermisjon(BigDecimal.ZERO, AktivitetskravPermisjonType.UDEFINERT))
            .build();
        var aaGrunnlag = AktivitetskravGrunnlagEntitet.Builder.oppdatere(Optional.empty())
            .medPerioderMedAktivitetskravArbeid(aaPerioder)
            .build();
        assertThat(aaGrunnlag.mor75ProsentStillingOgIngenPermisjoner(DatoIntervallEntitet.fraOgMedTilOgMed(LocalDate.now(), LocalDate.now()))).isTrue();
    }

    @Test
    void skal_finne_50_ingen_permisjoner_to_arbeid() {
        var aaPerioder = new AktivitetskravArbeidPerioderEntitet.Builder()
            .leggTil(new AktivitetskravArbeidPeriodeEntitet.Builder()
                .medPeriode(LocalDate.now(), LocalDate.now().plusWeeks(1))
                .medOrgNummer(OrgNummer.KUNSTIG_ORG)
                .medSumStillingsprosent(BigDecimal.valueOf(50))
                .medPermisjon(BigDecimal.ZERO, AktivitetskravPermisjonType.UDEFINERT))
            .leggTil(new AktivitetskravArbeidPeriodeEntitet.Builder()
                .medPeriode(LocalDate.now(), LocalDate.now().plusWeeks(1))
                .medOrgNummer(OrgNummer.KUNSTIG_ORG)
                .medSumStillingsprosent(BigDecimal.ZERO)
                .medPermisjon(BigDecimal.ZERO, AktivitetskravPermisjonType.UDEFINERT))
            .build();
        var aaGrunnlag = AktivitetskravGrunnlagEntitet.Builder.oppdatere(Optional.empty())
            .medPerioderMedAktivitetskravArbeid(aaPerioder)
            .build();
        assertThat(aaGrunnlag.mor75ProsentStillingOgIngenPermisjoner(DatoIntervallEntitet.fraOgMedTilOgMed(LocalDate.now(), LocalDate.now()))).isFalse();
    }

    @Test
    void skal_finne_80_ingen_permisjoner_ett_arbeid() {
        var aaPerioder = new AktivitetskravArbeidPerioderEntitet.Builder()
            .leggTil(new AktivitetskravArbeidPeriodeEntitet.Builder()
                .medPeriode(LocalDate.now(), LocalDate.now().plusWeeks(1))
                .medOrgNummer(OrgNummer.KUNSTIG_ORG)
                .medSumStillingsprosent(BigDecimal.valueOf(40))
                .medPermisjon(BigDecimal.ZERO, AktivitetskravPermisjonType.UDEFINERT))
            .leggTil(new AktivitetskravArbeidPeriodeEntitet.Builder()
                .medPeriode(LocalDate.now(), LocalDate.now().plusWeeks(1))
                .medOrgNummer(OrgNummer.KUNSTIG_ORG)
                .medSumStillingsprosent(BigDecimal.valueOf(40))
                .medPermisjon(BigDecimal.ZERO, AktivitetskravPermisjonType.UDEFINERT))
            .build();
        var aaGrunnlag = AktivitetskravGrunnlagEntitet.Builder.oppdatere(Optional.empty())
            .medPerioderMedAktivitetskravArbeid(aaPerioder)
            .build();
        assertThat(aaGrunnlag.mor75ProsentStillingOgIngenPermisjoner(DatoIntervallEntitet.fraOgMedTilOgMed(LocalDate.now(), LocalDate.now()))).isTrue();
    }

    @Test
    void skal_ikke_finne_over_75_permisjon_ett_arbeid() {
        var aaPerioder = new AktivitetskravArbeidPerioderEntitet.Builder()
            .leggTil(new AktivitetskravArbeidPeriodeEntitet.Builder()
                .medPeriode(LocalDate.now(), LocalDate.now().plusWeeks(1))
                .medOrgNummer(OrgNummer.KUNSTIG_ORG)
                .medSumStillingsprosent(BigDecimal.valueOf(100))
                .medPermisjon(BigDecimal.valueOf(50), AktivitetskravPermisjonType.FORELDREPENGER))
            .build();
        var aaGrunnlag = AktivitetskravGrunnlagEntitet.Builder.oppdatere(Optional.empty())
            .medPerioderMedAktivitetskravArbeid(aaPerioder)
            .build();
        assertThat(aaGrunnlag.mor75ProsentStillingOgIngenPermisjoner(DatoIntervallEntitet.fraOgMedTilOgMed(LocalDate.now(), LocalDate.now()))).isFalse();
    }

    @Test
    void skal_finne_over_75_permisjon_to_arbeid_perm_0_prosent() {
        var aaPerioder = new AktivitetskravArbeidPerioderEntitet.Builder()
            .leggTil(new AktivitetskravArbeidPeriodeEntitet.Builder()
                .medPeriode(LocalDate.now(), LocalDate.now().plusWeeks(1))
                .medOrgNummer(OrgNummer.KUNSTIG_ORG)
                .medSumStillingsprosent(BigDecimal.valueOf(100))
                .medPermisjon(BigDecimal.ZERO, AktivitetskravPermisjonType.UDEFINERT))
            .leggTil(new AktivitetskravArbeidPeriodeEntitet.Builder()
                .medPeriode(LocalDate.now(), LocalDate.now().plusWeeks(1))
                .medOrgNummer(OrgNummer.KUNSTIG_ORG)
                .medSumStillingsprosent(BigDecimal.ZERO)
                .medPermisjon(BigDecimal.valueOf(50), AktivitetskravPermisjonType.FORELDREPENGER))
            .build();
        var aaGrunnlag = AktivitetskravGrunnlagEntitet.Builder.oppdatere(Optional.empty())
            .medPerioderMedAktivitetskravArbeid(aaPerioder)
            .build();
        assertThat(aaGrunnlag.mor75ProsentStillingOgIngenPermisjoner(DatoIntervallEntitet.fraOgMedTilOgMed(LocalDate.now(), LocalDate.now()))).isTrue();
    }

    @Test
    void skal_finne_over_1_prosent_ingen_permisjoner_ett_arbeid() {
        var aaPerioder = new AktivitetskravArbeidPerioderEntitet.Builder()
            .leggTil(new AktivitetskravArbeidPeriodeEntitet.Builder()
                .medPeriode(LocalDate.now(), LocalDate.now().plusWeeks(1))
                .medOrgNummer(OrgNummer.KUNSTIG_ORG)
                .medSumStillingsprosent(BigDecimal.ONE)
                .medPermisjon(BigDecimal.ZERO, AktivitetskravPermisjonType.UDEFINERT))
            .build();
        var aaGrunnlag = AktivitetskravGrunnlagEntitet.Builder.oppdatere(Optional.empty())
            .medPerioderMedAktivitetskravArbeid(aaPerioder)
            .build();
        assertThat(aaGrunnlag.mor1ProsentStillingOgIngenPermisjoner(DatoIntervallEntitet.fraOgMedTilOgMed(LocalDate.now(), LocalDate.now()))).isTrue();
    }

    @Test
    void skal_finne_0_prosent_ingen_permisjoner_to_arbeid() {
        var aaPerioder = new AktivitetskravArbeidPerioderEntitet.Builder()
            .leggTil(new AktivitetskravArbeidPeriodeEntitet.Builder()
                .medPeriode(LocalDate.now(), LocalDate.now().plusWeeks(1))
                .medOrgNummer(OrgNummer.KUNSTIG_ORG)
                .medSumStillingsprosent(BigDecimal.ZERO)
                .medPermisjon(BigDecimal.ZERO, AktivitetskravPermisjonType.UDEFINERT))
            .leggTil(new AktivitetskravArbeidPeriodeEntitet.Builder()
                .medPeriode(LocalDate.now(), LocalDate.now().plusWeeks(1))
                .medOrgNummer(OrgNummer.KUNSTIG_ORG)
                .medSumStillingsprosent(BigDecimal.ZERO)
                .medPermisjon(BigDecimal.ZERO, AktivitetskravPermisjonType.UDEFINERT))
            .build();
        var aaGrunnlag = AktivitetskravGrunnlagEntitet.Builder.oppdatere(Optional.empty())
            .medPerioderMedAktivitetskravArbeid(aaPerioder)
            .build();
        assertThat(aaGrunnlag.mor1ProsentStillingOgIngenPermisjoner(DatoIntervallEntitet.fraOgMedTilOgMed(LocalDate.now(), LocalDate.now()))).isFalse();
    }
}
