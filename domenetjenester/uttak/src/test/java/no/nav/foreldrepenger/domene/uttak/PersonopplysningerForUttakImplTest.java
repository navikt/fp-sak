package no.nav.foreldrepenger.domene.uttak;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.time.LocalDate;
import java.util.Optional;

import org.junit.jupiter.api.Test;

import no.nav.foreldrepenger.behandling.BehandlingReferanse;
import no.nav.foreldrepenger.behandling.Skjæringstidspunkt;
import no.nav.foreldrepenger.behandlingslager.aktør.AdresseType;
import no.nav.foreldrepenger.behandlingslager.behandling.Behandling;
import no.nav.foreldrepenger.behandlingslager.behandling.personopplysning.OppgittAnnenPartBuilder;
import no.nav.foreldrepenger.behandlingslager.behandling.personopplysning.PersonInformasjonBuilder;
import no.nav.foreldrepenger.behandlingslager.behandling.personopplysning.PersonopplysningGrunnlagBuilder;
import no.nav.foreldrepenger.behandlingslager.behandling.personopplysning.PersonopplysningVersjonType;
import no.nav.foreldrepenger.behandlingslager.behandling.personopplysning.PersonopplysningerAggregat;
import no.nav.foreldrepenger.behandlingslager.behandling.personopplysning.RelasjonsRolleType;
import no.nav.foreldrepenger.behandlingslager.behandling.personopplysning.SivilstandType;
import no.nav.foreldrepenger.domene.personopplysning.PersonopplysningTjeneste;
import no.nav.foreldrepenger.domene.tid.DatoIntervallEntitet;
import no.nav.foreldrepenger.domene.typer.AktørId;
import no.nav.foreldrepenger.domene.uttak.testutilities.behandling.ScenarioFarSøkerForeldrepenger;
import no.nav.foreldrepenger.domene.uttak.testutilities.behandling.UttakRepositoryStubProvider;
import no.nav.fpsak.tidsserie.LocalDateInterval;

class PersonopplysningerForUttakImplTest {

    @Test
    void skal_hente_søkers_dødsdato() {
        var personopplysningTjeneste = mock(PersonopplysningTjeneste.class);
        var personopplysninger = new PersonopplysningerForUttakImpl(personopplysningTjeneste);

        var dødsdato = LocalDate.of(2020, 1, 1);

        var behandling = ScenarioFarSøkerForeldrepenger.forFødsel().lagre(new UttakRepositoryStubProvider());
        var ref = BehandlingReferanse.fra(behandling);

        var personopplysningerAggregat = personOpplysningerMedDødsdato(dødsdato, behandling);
        when(personopplysningTjeneste.hentPersonopplysninger(ref)).thenReturn(personopplysningerAggregat);

        assertThat(personopplysninger.søkersDødsdato(ref).orElseThrow()).isEqualTo(dødsdato);
    }

    @Test
    void skal_returnere_empty_hvis_søker_ikke_er_død() {
        var personopplysningTjeneste = mock(PersonopplysningTjeneste.class);
        var personopplysninger = new PersonopplysningerForUttakImpl(personopplysningTjeneste);

        var behandling = ScenarioFarSøkerForeldrepenger.forFødsel().lagre(new UttakRepositoryStubProvider());
        var ref = BehandlingReferanse.fra(behandling);

        var personopplysningerAggregat = personOpplysningerMedDødsdato(null, behandling);
        when(personopplysningTjeneste.hentPersonopplysninger(ref)).thenReturn(personopplysningerAggregat);

        assertThat(personopplysninger.søkersDødsdato(ref)).isEmpty();
    }

    @Test
    void skal_returnere_har_oppgitt_annenpart() {
        var personopplysningTjeneste = mock(PersonopplysningTjeneste.class);
        var personopplysninger = new PersonopplysningerForUttakImpl(personopplysningTjeneste);

        var behandling = ScenarioFarSøkerForeldrepenger.forFødsel().lagre(new UttakRepositoryStubProvider());
        var ref = BehandlingReferanse.fra(behandling);

        var personopplysningGrunnlagBuilder = PersonopplysningGrunnlagBuilder.oppdatere(Optional.empty())
            .medOppgittAnnenPart(new OppgittAnnenPartBuilder().medAktørId(AktørId.dummy()).build());
        var personopplysningerAggregat = new PersonopplysningerAggregat(personopplysningGrunnlagBuilder.build(),
            behandling.getAktørId());
        when(personopplysningTjeneste.hentPersonopplysninger(ref)).thenReturn(personopplysningerAggregat);

        assertThat(personopplysninger.harOppgittAnnenpartMedNorskID(ref)).isTrue();
    }

    @Test
    void skal_ikke_returnere_har_oppgitt_annenpart() {
        var personopplysningTjeneste = mock(PersonopplysningTjeneste.class);
        var personopplysninger = new PersonopplysningerForUttakImpl(personopplysningTjeneste);

        var behandling = ScenarioFarSøkerForeldrepenger.forFødsel().lagre(new UttakRepositoryStubProvider());
        var ref = BehandlingReferanse.fra(behandling);

        var personopplysningGrunnlagBuilder = PersonopplysningGrunnlagBuilder.oppdatere(Optional.empty());
        //Ingen annenpart
        var personopplysningerAggregat = new PersonopplysningerAggregat(personopplysningGrunnlagBuilder.build(),
            behandling.getAktørId());
        when(personopplysningTjeneste.hentPersonopplysninger(ref)).thenReturn(personopplysningerAggregat);

        assertThat(personopplysninger.harOppgittAnnenpartMedNorskID(ref)).isFalse();
    }

    @Test
    void skal_returnere_ektefelle_har_samme_bosted_hvis_gift() {
        var personopplysningTjeneste = mock(PersonopplysningTjeneste.class);
        var personopplysninger = new PersonopplysningerForUttakImpl(personopplysningTjeneste);

        var behandling = ScenarioFarSøkerForeldrepenger.forFødsel().lagre(new UttakRepositoryStubProvider());
        var ref = BehandlingReferanse.fra(behandling, Skjæringstidspunkt.builder().medUtledetSkjæringstidspunkt(LocalDate.now())
            .medUttaksintervall(new LocalDateInterval(LocalDate.now().minusYears(1), LocalDate.now())).build());

        var aktørId = behandling.getAktørId();
        var ekteFelleAktørId = AktørId.dummy();
        var personInformasjonBuilder = PersonInformasjonBuilder.oppdater(Optional.empty(),
            PersonopplysningVersjonType.REGISTRERT);
        personInformasjonBuilder.leggTil(personInformasjonBuilder.getPersonopplysningBuilder(aktørId)
            .medSivilstand(SivilstandType.GIFT));
        personInformasjonBuilder.leggTil(personInformasjonBuilder.getPersonopplysningBuilder(ekteFelleAktørId)
            .medSivilstand(SivilstandType.GIFT));
        personInformasjonBuilder.leggTil(personInformasjonBuilder.getAdresseBuilder(aktørId,
            DatoIntervallEntitet.fraOgMed(LocalDate.MIN), AdresseType.BOSTEDSADRESSE)
        .medAdresselinje1("abc 12"));
        personInformasjonBuilder.leggTil(personInformasjonBuilder.getRelasjonBuilder(aktørId, ekteFelleAktørId,
            RelasjonsRolleType.EKTE).harSammeBosted(true));
        var personopplysningGrunnlagBuilder = PersonopplysningGrunnlagBuilder.oppdatere(Optional.empty())
            .medRegistrertVersjon(personInformasjonBuilder);
        var personopplysningerAggregat = new PersonopplysningerAggregat(personopplysningGrunnlagBuilder.build(), aktørId);
        when(personopplysningTjeneste.hentPersonopplysninger(ref)).thenReturn(personopplysningerAggregat);

        assertThat(personopplysninger.ektefelleHarSammeBosted(ref)).isTrue();
    }

    @Test
    void skal_returnere_at_annenpart_er_uten_norsk_id() {
        var personopplysningTjeneste = mock(PersonopplysningTjeneste.class);
        var personopplysninger = new PersonopplysningerForUttakImpl(personopplysningTjeneste);

        var behandling = ScenarioFarSøkerForeldrepenger.forFødsel().lagre(new UttakRepositoryStubProvider());
        var ref = BehandlingReferanse.fra(behandling);

        var personopplysningGrunnlagBuilder = PersonopplysningGrunnlagBuilder.oppdatere(Optional.empty())
            .medOppgittAnnenPart(new OppgittAnnenPartBuilder().medAktørId(null).medUtenlandskFnr("123").build());
        var personopplysningerAggregat = new PersonopplysningerAggregat(personopplysningGrunnlagBuilder.build(),
            behandling.getAktørId());
        when(personopplysningTjeneste.hentPersonopplysninger(ref)).thenReturn(personopplysningerAggregat);

        assertThat(personopplysninger.oppgittAnnenpartUtenNorskID(ref)).isTrue();
    }

    @Test
    void skal_returnere_at_annenpart_ikke_er_uten_norsk_id_hvis_ukjent_annenpart() {
        var personopplysningTjeneste = mock(PersonopplysningTjeneste.class);
        var personopplysninger = new PersonopplysningerForUttakImpl(personopplysningTjeneste);

        var behandling = ScenarioFarSøkerForeldrepenger.forFødsel().lagre(new UttakRepositoryStubProvider());
        var ref = BehandlingReferanse.fra(behandling);

        var personopplysningGrunnlagBuilder = PersonopplysningGrunnlagBuilder.oppdatere(Optional.empty())
            //Ukjent forelder lagres slik i db
            .medOppgittAnnenPart(new OppgittAnnenPartBuilder().medAktørId(null).medUtenlandskFnrLand(null).medUtenlandskFnr(null).build());
        var personopplysningerAggregat = new PersonopplysningerAggregat(personopplysningGrunnlagBuilder.build(),
            behandling.getAktørId());
        when(personopplysningTjeneste.hentPersonopplysninger(ref)).thenReturn(personopplysningerAggregat);

        assertThat(personopplysninger.oppgittAnnenpartUtenNorskID(ref)).isFalse();
    }

    @Test
    void skal_returnere_at_annenpart_ikke_er_uten_norsk_id() {
        var personopplysningTjeneste = mock(PersonopplysningTjeneste.class);
        var personopplysninger = new PersonopplysningerForUttakImpl(personopplysningTjeneste);

        var behandling = ScenarioFarSøkerForeldrepenger.forFødsel().lagre(new UttakRepositoryStubProvider());
        var ref = BehandlingReferanse.fra(behandling);

        var personopplysningGrunnlagBuilder = PersonopplysningGrunnlagBuilder.oppdatere(Optional.empty())
            .medOppgittAnnenPart(new OppgittAnnenPartBuilder().medAktørId(AktørId.dummy()).medAktørId(AktørId.dummy()).build());
        var personopplysningerAggregat = new PersonopplysningerAggregat(personopplysningGrunnlagBuilder.build(),
            behandling.getAktørId());
        when(personopplysningTjeneste.hentPersonopplysninger(ref)).thenReturn(personopplysningerAggregat);

        assertThat(personopplysninger.oppgittAnnenpartUtenNorskID(ref)).isFalse();
    }

    private PersonopplysningerAggregat personOpplysningerMedDødsdato(LocalDate dødsdato, Behandling behandling) {
        var personInformasjonBuilder = PersonInformasjonBuilder.oppdater(Optional.empty(),
            PersonopplysningVersjonType.REGISTRERT);
        personInformasjonBuilder.leggTil(
            personInformasjonBuilder.getPersonopplysningBuilder(behandling.getAktørId()).medDødsdato(dødsdato));
        var personopplysningGrunnlagBuilder = PersonopplysningGrunnlagBuilder.oppdatere(Optional.empty())
            .medRegistrertVersjon(personInformasjonBuilder);
        return new PersonopplysningerAggregat(personopplysningGrunnlagBuilder.build(), behandling.getAktørId()
        );
    }

}
