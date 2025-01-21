package no.nav.foreldrepenger.behandlingslager.hendelser.sortering;

import static java.util.Collections.singletonList;
import static org.assertj.core.api.Assertions.assertThat;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import no.nav.foreldrepenger.behandlingslager.aktør.NavBruker;
import no.nav.foreldrepenger.behandlingslager.aktør.NavBrukerKjønn;
import no.nav.foreldrepenger.behandlingslager.aktør.NavBrukerRepository;
import no.nav.foreldrepenger.behandlingslager.behandling.Behandling;
import no.nav.foreldrepenger.behandlingslager.behandling.personopplysning.PersonopplysningRepository;
import no.nav.foreldrepenger.behandlingslager.behandling.personopplysning.RelasjonsRolleType;
import no.nav.foreldrepenger.behandlingslager.behandling.repository.BehandlingLåsRepository;
import no.nav.foreldrepenger.behandlingslager.behandling.repository.BehandlingRepository;
import no.nav.foreldrepenger.behandlingslager.fagsak.Fagsak;
import no.nav.foreldrepenger.behandlingslager.fagsak.FagsakRepository;
import no.nav.foreldrepenger.behandlingslager.fagsak.FagsakYtelseType;
import no.nav.foreldrepenger.behandlingslager.hendelser.HendelseSorteringRepository;
import no.nav.foreldrepenger.dbstoette.EntityManagerAwareTest;
import no.nav.foreldrepenger.domene.typer.AktørId;

class HendelseSorteringRepositoryTest extends EntityManagerAwareTest {

    private HendelseSorteringRepository sorteringRepository;
    private PersonopplysningRepository personopplysningRepository;
    private FagsakRepository fagsakRepository;
    private NavBrukerRepository navBrukerRepository;
    private BehandlingRepository behandlingRepository;

    @BeforeEach
    void setUp() {
        var entityManager = getEntityManager();
        sorteringRepository = new HendelseSorteringRepository(entityManager);
        personopplysningRepository = new PersonopplysningRepository(entityManager);
        fagsakRepository = new FagsakRepository(entityManager);
        navBrukerRepository = new NavBrukerRepository(entityManager);
        behandlingRepository = new BehandlingRepository(entityManager);
    }

    @Test
    void skal_hente_1_aktørId_fra_fagsak() {
        var personer = genererFagsaker(1);

        var aktørId = personer.get(0);

        var finnAktørIder = List.of(aktørId);
        var resultat = sorteringRepository.hentEksisterendeAktørIderMedSak(finnAktørIder);

        assertThat(resultat)
            .isNotEmpty()
            .containsExactly(aktørId);
    }

    @Test
    void skal_returnere_tom_liste_når_aktør_id_ikke_er_knyttet_til_sak() {
        // setup
        @SuppressWarnings("unused")
        var personer = genererFagsaker(4); // aktør ID: 100 - 103

        var finnAktørIder = List.of(AktørId.dummy());

        // act
        var resultat = sorteringRepository.hentEksisterendeAktørIderMedSak(finnAktørIder);

        // assert
        assertThat(resultat).isEmpty();
    }

    @Test
    void skal_returnere_4_aktør_ider_fra_fagsaker() {
        var personer = genererFagsaker(6);

        var finnAktørIder = personer.stream().limit(4).toList();

        var resultat = sorteringRepository.hentEksisterendeAktørIderMedSak(finnAktørIder);
        assertThat(resultat).hasSize(4);
    }

    @Test
    void skal_ikke_publisere_videre_hendelser_på_avsluttede_saker() {
        var personinfoList = genererPersonInfo(3);

        var navBrukerMedAvsluttetSak = NavBruker.opprettNyNB(personinfoList.get(0));
        var fagsak1 = opprettFagsak(navBrukerMedAvsluttetSak, FagsakYtelseType.FORELDREPENGER);
        fagsak1.setAvsluttet();

        var navBrukerMedÅpenSak = NavBruker.opprettNyNB(personinfoList.get(1));
        var fagsak2 = opprettFagsak(navBrukerMedÅpenSak, FagsakYtelseType.FORELDREPENGER);

        var navBrukerMedÅpenOgAvsluttetSak = NavBruker.opprettNyNB(personinfoList.get(2));
        var fagsak3 = opprettFagsak(navBrukerMedÅpenOgAvsluttetSak, FagsakYtelseType.FORELDREPENGER);
        var fagsak4 = opprettFagsak(navBrukerMedÅpenOgAvsluttetSak, FagsakYtelseType.FORELDREPENGER);
        fagsak4.setAvsluttet();

        navBrukerRepository.lagre(navBrukerMedAvsluttetSak);
        navBrukerRepository.lagre(navBrukerMedÅpenSak);
        navBrukerRepository.lagre(navBrukerMedÅpenOgAvsluttetSak);

        fagsakRepository.opprettNy(fagsak1);
        fagsakRepository.opprettNy(fagsak2);
        fagsakRepository.opprettNy(fagsak3);
        fagsakRepository.opprettNy(fagsak4);

        List<AktørId> aktørList = new ArrayList<>(personinfoList);
        var resultat = sorteringRepository.hentEksisterendeAktørIderMedSak(aktørList);

        assertThat(resultat)
            .hasSize(2)
            .contains(navBrukerMedÅpenSak.getAktørId())
            .contains(navBrukerMedÅpenOgAvsluttetSak.getAktørId());
    }

    @Test
    void skal_publisere_videre_hendelser_på_saker_om_engangsstønad() {
        // Arrange
        var personinfoList = genererPersonInfo(1);
        var navBruker = NavBruker.opprettNyNB(personinfoList.get(0));
        var fagsak = opprettFagsak(navBruker, FagsakYtelseType.ENGANGSTØNAD);

        navBrukerRepository.lagre(navBruker);
        fagsakRepository.opprettNy(fagsak);

        var finnAktørIder = List.of(navBruker.getAktørId());

        // Act
        var resultat = sorteringRepository.hentEksisterendeAktørIderMedSak(finnAktørIder);

        // Assert
        assertThat(resultat).contains(navBruker.getAktørId());
    }

    @Test
    void skal_publisere_videre_hendelser_på_saker_om_svangerskapspenger() {
        // Arrange
        var personinfoList = genererPersonInfo(1);
        var navBruker = NavBruker.opprettNyNB(personinfoList.get(0));
        var fagsak = opprettFagsak(navBruker, FagsakYtelseType.SVANGERSKAPSPENGER);

        navBrukerRepository.lagre(navBruker);
        fagsakRepository.opprettNy(fagsak);

        var finnAktørIder = List.of(navBruker.getAktørId());

        // Act
        var resultat = sorteringRepository.hentEksisterendeAktørIderMedSak(finnAktørIder);

        // Assert
        assertThat(resultat).contains(navBruker.getAktørId());
    }

    @Test
    void skal_finne_match_på_både_mor_og_barn_i_behandlingsgrunnlaget() {
        // Arrange
        var barnAktørId = AktørId.dummy();
        var fødselsdato = LocalDate.now();

        var personinfoList = genererPersonInfo(1);
        var navBruker = NavBruker.opprettNyNB(personinfoList.get(0));
        var morAktørId = navBruker.getAktørId();
        var fagsak = opprettFagsak(navBruker, FagsakYtelseType.FORELDREPENGER);
        navBrukerRepository.lagre(navBruker);
        fagsakRepository.opprettNy(fagsak);

        var behandlingBuilder = Behandling.forFørstegangssøknad(fagsak);
        var behandling = behandlingBuilder.build();
        lagreBehandling(behandling);

        var behandlingId = behandling.getId();
        var informasjonBuilder = personopplysningRepository.opprettBuilderForRegisterdata(behandlingId);
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
        var resultat1 = sorteringRepository.hentEksisterendeAktørIderMedSak(singletonList(morAktørId));
        var resultat2 = sorteringRepository.hentEksisterendeAktørIderMedSak(singletonList(barnAktørId));

        // Assert
        assertThat(resultat1)
            .hasSize(1)
            .contains(morAktørId);
        assertThat(resultat2)
            .hasSize(1)
            .contains(barnAktørId);
    }

    private void lagreBehandling(Behandling behandling) {
        behandlingRepository.lagre(behandling, new BehandlingLåsRepository(getEntityManager()).taLås(behandling.getId()));
    }

    @Test
    void skal_ikke_finne_match_på_barn_i_behandlingsgrunnlaget_når_relasjonen_mangler() {
        // Arrange
        var barnAktørId = AktørId.dummy();
        var fødselsdato = LocalDate.now();

        var personinfoList = genererPersonInfo(1);
        var navBruker = NavBruker.opprettNyNB(personinfoList.get(0));
        var morAktørId = navBruker.getAktørId();

        var fagsak = opprettFagsak(navBruker, FagsakYtelseType.FORELDREPENGER);
        navBrukerRepository.lagre(navBruker);
        fagsakRepository.opprettNy(fagsak);

        var behandlingBuilder = Behandling.forFørstegangssøknad(fagsak);
        var behandling = behandlingBuilder.build();
        lagreBehandling(behandling);

        var behandlingId = behandling.getId();
        var informasjonBuilder = personopplysningRepository.opprettBuilderForRegisterdata(behandlingId);
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
        var resultat1 = sorteringRepository.hentEksisterendeAktørIderMedSak(singletonList(morAktørId));
        var resultat2 = sorteringRepository.hentEksisterendeAktørIderMedSak(singletonList(barnAktørId));

        // Assert
        assertThat(resultat1)
            .hasSize(1)
            .contains(morAktørId);
        assertThat(resultat2).isEmpty();
    }

    private Fagsak opprettFagsak(NavBruker bruker, FagsakYtelseType fagsakYtelseType) {
        return Fagsak.opprettNy(fagsakYtelseType, bruker);
    }

    private List<AktørId> genererFagsaker(int antall) {
        var personinfoList = genererPersonInfo(antall);

        List<Fagsak> fagsaker = new ArrayList<>();
        List<NavBruker> navBrukere = new ArrayList<>();

        for (var pInfo : personinfoList) {
            var bruker = NavBruker.opprettNyNB(pInfo);
            navBrukere.add(bruker);

            fagsaker.add(opprettFagsak(bruker, FagsakYtelseType.FORELDREPENGER));
        }

        if (!fagsaker.isEmpty()) {
            navBrukere.forEach(b -> navBrukerRepository.lagre(b));
            fagsaker.forEach(f -> fagsakRepository.opprettNy(f));
        }

        return personinfoList;
    }

    private List<AktørId> genererPersonInfo(int antall) {
        List<AktørId> personinfoList = new ArrayList<>();
        for (var i = 0; i < antall; i++) {
            personinfoList.add(AktørId.dummy());
        }
        return personinfoList;
    }
}
