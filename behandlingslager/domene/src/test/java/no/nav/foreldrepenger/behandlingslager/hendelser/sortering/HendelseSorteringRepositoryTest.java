package no.nav.foreldrepenger.behandlingslager.hendelser.sortering;

import static java.util.Collections.singletonList;
import static org.assertj.core.api.Assertions.assertThat;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import no.nav.foreldrepenger.behandlingslager.aktør.NavBruker;
import no.nav.foreldrepenger.behandlingslager.aktør.NavBrukerKjønn;
import no.nav.foreldrepenger.behandlingslager.aktør.NavBrukerRepository;
import no.nav.foreldrepenger.behandlingslager.behandling.Behandling;
import no.nav.foreldrepenger.behandlingslager.behandling.personopplysning.PersonInformasjonBuilder;
import no.nav.foreldrepenger.behandlingslager.behandling.personopplysning.PersonopplysningRepository;
import no.nav.foreldrepenger.behandlingslager.behandling.personopplysning.RelasjonsRolleType;
import no.nav.foreldrepenger.behandlingslager.behandling.repository.BehandlingLåsRepository;
import no.nav.foreldrepenger.behandlingslager.behandling.repository.BehandlingRepository;
import no.nav.foreldrepenger.behandlingslager.fagsak.Fagsak;
import no.nav.foreldrepenger.behandlingslager.fagsak.FagsakRepository;
import no.nav.foreldrepenger.behandlingslager.fagsak.FagsakYtelseType;
import no.nav.foreldrepenger.behandlingslager.hendelser.HendelseSorteringRepository;
import no.nav.foreldrepenger.dbstoette.FPsakEntityManagerAwareExtension;
import no.nav.foreldrepenger.domene.typer.AktørId;
import no.nav.vedtak.felles.testutilities.db.EntityManagerAwareTest;

@ExtendWith(FPsakEntityManagerAwareExtension.class)
public class HendelseSorteringRepositoryTest extends EntityManagerAwareTest {

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
    public void skal_hente_1_aktørId_fra_fagsak() {
        var personer = genererFagsaker(1);

        AktørId aktørId = personer.get(0);

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

        List<AktørId> finnAktørIder = personer.stream().limit(4).collect(Collectors.toList());

        List<AktørId> resultat = sorteringRepository.hentEksisterendeAktørIderMedSak(finnAktørIder);
        assertThat(resultat).hasSize(4);
    }

    @Test
    public void skal_ikke_publisere_videre_hendelser_på_avsluttede_saker() {
        var personinfoList = genererPersonInfo(3);

        NavBruker navBrukerMedAvsluttetSak = NavBruker.opprettNyNB(personinfoList.get(0));
        Fagsak fagsak1 = opprettFagsak(navBrukerMedAvsluttetSak, FagsakYtelseType.FORELDREPENGER);
        fagsak1.setAvsluttet();

        NavBruker navBrukerMedÅpenSak = NavBruker.opprettNyNB(personinfoList.get(1));
        Fagsak fagsak2 = opprettFagsak(navBrukerMedÅpenSak, FagsakYtelseType.FORELDREPENGER);

        NavBruker navBrukerMedÅpenOgAvsluttetSak = NavBruker.opprettNyNB(personinfoList.get(2));
        Fagsak fagsak3 = opprettFagsak(navBrukerMedÅpenOgAvsluttetSak, FagsakYtelseType.FORELDREPENGER);
        Fagsak fagsak4 = opprettFagsak(navBrukerMedÅpenOgAvsluttetSak, FagsakYtelseType.FORELDREPENGER);
        fagsak4.setAvsluttet();

        navBrukerRepository.lagre(navBrukerMedAvsluttetSak);
        navBrukerRepository.lagre(navBrukerMedÅpenSak);
        navBrukerRepository.lagre(navBrukerMedÅpenOgAvsluttetSak);

        fagsakRepository.opprettNy(fagsak1);
        fagsakRepository.opprettNy(fagsak2);
        fagsakRepository.opprettNy(fagsak3);
        fagsakRepository.opprettNy(fagsak4);

        List<AktørId> aktørList = new ArrayList<>(personinfoList);
        List<AktørId> resultat = sorteringRepository.hentEksisterendeAktørIderMedSak(aktørList);

        assertThat(resultat).hasSize(2);
        assertThat(resultat).contains(navBrukerMedÅpenSak.getAktørId());
        assertThat(resultat).contains(navBrukerMedÅpenOgAvsluttetSak.getAktørId());
    }

    @Test
    public void skal_publisere_videre_hendelser_på_saker_om_engangsstønad() {
        // Arrange
        var personinfoList = genererPersonInfo(1);
        NavBruker navBruker = NavBruker.opprettNyNB(personinfoList.get(0));
        Fagsak fagsak = opprettFagsak(navBruker, FagsakYtelseType.ENGANGSTØNAD);

        navBrukerRepository.lagre(navBruker);
        fagsakRepository.opprettNy(fagsak);

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
        NavBruker navBruker = NavBruker.opprettNyNB(personinfoList.get(0));
        Fagsak fagsak = opprettFagsak(navBruker, FagsakYtelseType.SVANGERSKAPSPENGER);

        navBrukerRepository.lagre(navBruker);
        fagsakRepository.opprettNy(fagsak);

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
        NavBruker navBruker = NavBruker.opprettNyNB(personinfoList.get(0));
        AktørId morAktørId = navBruker.getAktørId();
        Fagsak fagsak = opprettFagsak(navBruker, FagsakYtelseType.FORELDREPENGER);
        navBrukerRepository.lagre(navBruker);
        fagsakRepository.opprettNy(fagsak);

        Behandling.Builder behandlingBuilder = Behandling.forFørstegangssøknad(fagsak);
        Behandling behandling = behandlingBuilder.build();
        lagreBehandling(behandling);

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

    private Long lagreBehandling(Behandling behandling) {
        return behandlingRepository.lagre(behandling, new BehandlingLåsRepository(getEntityManager()).taLås(behandling.getId()));
    }

    @Test
    public void skal_ikke_finne_match_på_barn_i_behandlingsgrunnlaget_når_relasjonen_mangler() {
        // Arrange
        AktørId barnAktørId = AktørId.dummy();
        LocalDate fødselsdato = LocalDate.now();

        var personinfoList = genererPersonInfo(1);
        NavBruker navBruker = NavBruker.opprettNyNB(personinfoList.get(0));
        AktørId morAktørId = navBruker.getAktørId();

        Fagsak fagsak = opprettFagsak(navBruker, FagsakYtelseType.FORELDREPENGER);
        navBrukerRepository.lagre(navBruker);
        fagsakRepository.opprettNy(fagsak);

        Behandling.Builder behandlingBuilder = Behandling.forFørstegangssøknad(fagsak);
        Behandling behandling = behandlingBuilder.build();
        lagreBehandling(behandling);

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

    private List<AktørId> genererFagsaker(int antall) {
        var personinfoList = genererPersonInfo(antall);

        List<Fagsak> fagsaker = new ArrayList<>();
        List<NavBruker> navBrukere = new ArrayList<>();

        for (AktørId pInfo : personinfoList) {
            NavBruker bruker = NavBruker.opprettNyNB(pInfo);
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
        for (int i = 0; i < antall; i++) {
            personinfoList.add(AktørId.dummy());
        }
        return personinfoList;
    }
}
