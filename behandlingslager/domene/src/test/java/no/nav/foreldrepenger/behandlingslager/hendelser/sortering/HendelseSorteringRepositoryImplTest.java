package no.nav.foreldrepenger.behandlingslager.hendelser.sortering;

import static java.util.Collections.singletonList;
import static org.assertj.core.api.Assertions.assertThat;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import org.junit.Rule;
import org.junit.Test;

import no.nav.foreldrepenger.behandlingslager.aktør.NavBruker;
import no.nav.foreldrepenger.behandlingslager.aktør.NavBrukerKjønn;
import no.nav.foreldrepenger.behandlingslager.aktør.PersoninfoSpråk;
import no.nav.foreldrepenger.behandlingslager.behandling.Behandling;
import no.nav.foreldrepenger.behandlingslager.behandling.personopplysning.PersonInformasjonBuilder;
import no.nav.foreldrepenger.behandlingslager.behandling.personopplysning.PersonopplysningRepository;
import no.nav.foreldrepenger.behandlingslager.behandling.personopplysning.RelasjonsRolleType;
import no.nav.foreldrepenger.behandlingslager.behandling.repository.BehandlingRepositoryProvider;
import no.nav.foreldrepenger.behandlingslager.fagsak.Fagsak;
import no.nav.foreldrepenger.behandlingslager.fagsak.FagsakYtelseType;
import no.nav.foreldrepenger.behandlingslager.geografisk.Språkkode;
import no.nav.foreldrepenger.behandlingslager.hendelser.HendelseSorteringRepository;
import no.nav.foreldrepenger.dbstoette.UnittestRepositoryRule;
import no.nav.foreldrepenger.domene.typer.AktørId;
import no.nav.vedtak.felles.testutilities.db.Repository;

public class HendelseSorteringRepositoryImplTest {

    @Rule
    public UnittestRepositoryRule repositoryRule = new UnittestRepositoryRule();
    public Repository repository = repositoryRule.getRepository();

    private HendelseSorteringRepository sorteringRepository = new HendelseSorteringRepository(repositoryRule.getEntityManager());
    private BehandlingRepositoryProvider repositoryProvider = new BehandlingRepositoryProvider(repositoryRule.getEntityManager());
    private PersonopplysningRepository personopplysningRepository = repositoryProvider.getPersonopplysningRepository();


    @Test
    public void skal_hente_1_aktørId_fra_fagsak() {
        var personer = genererFagsaker(1);

        AktørId aktørId = personer.get(0).getAktørId();

        List<AktørId> finnAktørIder = List.of(aktørId);
        List<AktørId> resultat = sorteringRepository.hentEksisterendeAktørIderMedSak(finnAktørIder);

        assertThat(resultat).isNotEmpty();
        assertThat(resultat).containsExactly(aktørId);
    }

    @Test
    public void skal_returnere_tom_liste_når_aktør_id_ikke_er_knyttet_til_sak() {
        // setup
        @SuppressWarnings("unused")
        var personer = genererFagsaker(4); // aktør ID: 100 - 103

        List<AktørId> finnAktørIder = List.of(AktørId.dummy());

        // act
        List<AktørId> resultat = sorteringRepository.hentEksisterendeAktørIderMedSak(finnAktørIder);

        // assert
        assertThat(resultat).isEmpty();
    }

    @Test
    public void skal_returnere_4_aktør_ider_fra_fagsaker() {
        var personer = genererFagsaker(6);

        List<AktørId> finnAktørIder = personer.stream().map(PersoninfoSpråk::getAktørId).limit(4).collect(Collectors.toList());

        List<AktørId> resultat = sorteringRepository.hentEksisterendeAktørIderMedSak(finnAktørIder);
        assertThat(resultat).hasSize(4);
    }

    @Test
    public void skal_ikke_publisere_videre_hendelser_på_avsluttede_saker() {
        var personinfoList = genererPersonInfo(3);

        NavBruker navBrukerMedAvsluttetSak = NavBruker.opprettNy(personinfoList.get(0));
        Fagsak fagsak1 = opprettFagsak(navBrukerMedAvsluttetSak, FagsakYtelseType.FORELDREPENGER);
        fagsak1.setAvsluttet();

        NavBruker navBrukerMedÅpenSak = NavBruker.opprettNy(personinfoList.get(1));
        Fagsak fagsak2 = opprettFagsak(navBrukerMedÅpenSak, FagsakYtelseType.FORELDREPENGER);

        NavBruker navBrukerMedÅpenOgAvsluttetSak = NavBruker.opprettNy(personinfoList.get(2));
        Fagsak fagsak3 = opprettFagsak(navBrukerMedÅpenOgAvsluttetSak, FagsakYtelseType.FORELDREPENGER);
        Fagsak fagsak4 = opprettFagsak(navBrukerMedÅpenOgAvsluttetSak, FagsakYtelseType.FORELDREPENGER);
        fagsak4.setAvsluttet();

        repository.lagre(navBrukerMedAvsluttetSak);
        repository.lagre(navBrukerMedÅpenSak);
        repository.lagre(navBrukerMedÅpenOgAvsluttetSak);
        repository.lagre(fagsak1);
        repository.lagre(fagsak2);
        repository.lagre(fagsak3);
        repository.lagre(fagsak4);
        repository.flushAndClear();

        List<AktørId> aktørList = personinfoList.stream().map(PersoninfoSpråk::getAktørId).collect(Collectors.toList());
        List<AktørId> resultat = sorteringRepository.hentEksisterendeAktørIderMedSak(aktørList);

        assertThat(resultat).hasSize(2);
        assertThat(resultat).contains(navBrukerMedÅpenSak.getAktørId());
        assertThat(resultat).contains(navBrukerMedÅpenOgAvsluttetSak.getAktørId());
    }

    @Test
    public void skal_publisere_videre_hendelser_på_saker_om_engangsstønad() {
        // Arrange
        var personinfoList = genererPersonInfo(1);
        NavBruker navBruker = NavBruker.opprettNy(personinfoList.get(0));
        Fagsak fagsak = opprettFagsak(navBruker, FagsakYtelseType.ENGANGSTØNAD);

        repository.lagre(navBruker);
        repository.lagre(fagsak);
        repository.flushAndClear();

        List<AktørId> finnAktørIder = List.of(navBruker.getAktørId());

        // Act
        List<AktørId> resultat = sorteringRepository.hentEksisterendeAktørIderMedSak(finnAktørIder);

        // Assert
        assertThat(resultat).contains(navBruker.getAktørId());
    }

    @Test
    public void skal_publisere_videre_hendelser_på_saker_om_svangerskapspenger() {
        // Arrange
        var personinfoList = genererPersonInfo(1);
        NavBruker navBruker = NavBruker.opprettNy(personinfoList.get(0));
        Fagsak fagsak = opprettFagsak(navBruker, FagsakYtelseType.SVANGERSKAPSPENGER);

        repository.lagre(navBruker);
        repository.lagre(fagsak);
        repository.flushAndClear();

        List<AktørId> finnAktørIder = List.of(navBruker.getAktørId());

        // Act
        List<AktørId> resultat = sorteringRepository.hentEksisterendeAktørIderMedSak(finnAktørIder);

        // Assert
        assertThat(resultat).contains(navBruker.getAktørId());
    }

    @Test
    public void skal_finne_match_på_både_mor_og_barn_i_behandlingsgrunnlaget() {
        // Arrange
        AktørId barnAktørId = AktørId.dummy();
        LocalDate fødselsdato = LocalDate.now();

        var personinfoList = genererPersonInfo(1);
        NavBruker navBruker = NavBruker.opprettNy(personinfoList.get(0));
        AktørId morAktørId = navBruker.getAktørId();
        Fagsak fagsak = opprettFagsak(navBruker, FagsakYtelseType.FORELDREPENGER);
        repository.lagre(navBruker);
        repository.lagre(fagsak);
        repository.flushAndClear();

        Behandling.Builder behandlingBuilder = Behandling.forFørstegangssøknad(fagsak);
        Behandling behandling = behandlingBuilder.build();
        repository.lagre(behandling);
        repository.flushAndClear();

        Long behandlingId = behandling.getId();
        PersonInformasjonBuilder informasjonBuilder = personopplysningRepository.opprettBuilderForRegisterdata(behandlingId);
        informasjonBuilder
            .leggTil(
                informasjonBuilder.getPersonopplysningBuilder(barnAktørId)
                    .medKjønn(NavBrukerKjønn.MANN)
                    .medNavn("Barn Hansen")
                    .medFødselsdato(fødselsdato))
            .leggTil(
                informasjonBuilder.getPersonopplysningBuilder(morAktørId)
                    .medKjønn(NavBrukerKjønn.KVINNE)
                    .medNavn("Mor Hansen")
                    .medFødselsdato(fødselsdato.minusYears(25)))
            .leggTil(
                informasjonBuilder
                    .getRelasjonBuilder(morAktørId, barnAktørId, RelasjonsRolleType.BARN))
            .leggTil(
                informasjonBuilder
                    .getRelasjonBuilder(barnAktørId, morAktørId, RelasjonsRolleType.MORA));

        personopplysningRepository.lagre(behandlingId, informasjonBuilder);

        // Act
        List<AktørId> resultat1 = sorteringRepository.hentEksisterendeAktørIderMedSak(singletonList(morAktørId));
        List<AktørId> resultat2 = sorteringRepository.hentEksisterendeAktørIderMedSak(singletonList(barnAktørId));

        // Assert
        assertThat(resultat1).hasSize(1);
        assertThat(resultat1).contains(morAktørId);
        assertThat(resultat2).hasSize(1);
        assertThat(resultat2).contains(barnAktørId);
    }

    @Test
    public void skal_ikke_finne_match_på_barn_i_behandlingsgrunnlaget_når_relasjonen_mangler() {
        // Arrange
        AktørId barnAktørId = AktørId.dummy();
        LocalDate fødselsdato = LocalDate.now();

        var personinfoList = genererPersonInfo(1);
        NavBruker navBruker = NavBruker.opprettNy(personinfoList.get(0));
        AktørId morAktørId = navBruker.getAktørId();

        Fagsak fagsak = opprettFagsak(navBruker, FagsakYtelseType.FORELDREPENGER);
        repository.lagre(navBruker);
        repository.lagre(fagsak);
        repository.flushAndClear();

        Behandling.Builder behandlingBuilder = Behandling.forFørstegangssøknad(fagsak);
        Behandling behandling = behandlingBuilder.build();
        repository.lagre(behandling);
        repository.flushAndClear();

        Long behandlingId = behandling.getId();
        PersonInformasjonBuilder informasjonBuilder = personopplysningRepository.opprettBuilderForRegisterdata(behandlingId);
        informasjonBuilder
            .leggTil(
                informasjonBuilder.getPersonopplysningBuilder(barnAktørId)
                    .medKjønn(NavBrukerKjønn.MANN)
                    .medNavn("Barn Hansen")
                    .medFødselsdato(fødselsdato))
            .leggTil(
                informasjonBuilder.getPersonopplysningBuilder(morAktørId)
                    .medKjønn(NavBrukerKjønn.KVINNE)
                    .medNavn("Mor Hansen")
                    .medFødselsdato(fødselsdato.minusYears(25)));

        personopplysningRepository.lagre(behandlingId, informasjonBuilder);

        // Act
        List<AktørId> resultat1 = sorteringRepository.hentEksisterendeAktørIderMedSak(singletonList(morAktørId));
        List<AktørId> resultat2 = sorteringRepository.hentEksisterendeAktørIderMedSak(singletonList(barnAktørId));

        // Assert
        assertThat(resultat1).hasSize(1);
        assertThat(resultat1).contains(morAktørId);
        assertThat(resultat2).isEmpty();
    }

    private Fagsak opprettFagsak(NavBruker bruker, FagsakYtelseType fagsakYtelseType) {
        return Fagsak.opprettNy(fagsakYtelseType, bruker);
    }

    private List<PersoninfoSpråk> genererFagsaker(int antall) {
        var personinfoList = genererPersonInfo(antall);

        List<Fagsak> fagsaker = new ArrayList<>();
        List<NavBruker> navBrukere = new ArrayList<>();

        for (PersoninfoSpråk pInfo : personinfoList) {
            NavBruker bruker = NavBruker.opprettNy(pInfo);
            navBrukere.add(bruker);

            fagsaker.add(opprettFagsak(bruker, FagsakYtelseType.FORELDREPENGER));
        }

        if (!fagsaker.isEmpty()) {
            repository.lagre(navBrukere);
            repository.lagre(fagsaker);
            repository.flushAndClear();
        }

        return personinfoList;
    }

    private List<PersoninfoSpråk> genererPersonInfo(int antall) {
        List<PersoninfoSpråk> personinfoList = new ArrayList<>();
        for (int i = 0; i < antall; i++) {
            personinfoList.add(new PersoninfoSpråk(AktørId.dummy(), Språkkode.NB));
        }
        return personinfoList;
    }
}
