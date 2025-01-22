package no.nav.foreldrepenger.behandlingslager.hendelser.håndtering;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.LocalDate;

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
import no.nav.foreldrepenger.behandlingslager.hendelser.HendelseHåndteringRepository;
import no.nav.foreldrepenger.dbstoette.EntityManagerAwareTest;
import no.nav.foreldrepenger.domene.typer.AktørId;

class HendelseHåndteringRepositoryTest extends EntityManagerAwareTest {

    private HendelseHåndteringRepository hendelseHåndteringRepository;
    private PersonopplysningRepository personopplysningRepository;
    private FagsakRepository fagsakRepository;
    private NavBrukerRepository navBrukerRepository;
    private BehandlingRepository behandlingRepository;

    @BeforeEach
    void setUp() {
        var entityManager = getEntityManager();
        hendelseHåndteringRepository = new HendelseHåndteringRepository(entityManager);
        personopplysningRepository = new PersonopplysningRepository(entityManager);
        fagsakRepository = new FagsakRepository(entityManager);
        navBrukerRepository = new NavBrukerRepository(entityManager);
        behandlingRepository = new BehandlingRepository(entityManager);
    }

    @Test
    void skal_finne_fagsak_på_barnets_aktørId_i_behandlingsgrunnlaget() {
        // Arrange
        var morAktørId = AktørId.dummy();
        var barnAktørId = AktørId.dummy();
        var fødselsdato = LocalDate.now();

        var navBruker = NavBruker.opprettNyNB(morAktørId);
        navBrukerRepository.lagre(navBruker);
        var fagsak = Fagsak.opprettNy(FagsakYtelseType.FORELDREPENGER, navBruker);
        fagsakRepository.opprettNy(fagsak);

        var behandlingBuilder = Behandling.forFørstegangssøknad(fagsak);
        var behandling = behandlingBuilder.build();
        behandlingRepository.lagre(behandling, new BehandlingLåsRepository(getEntityManager()).taLås(behandling.getId()));

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
        var resultat = hendelseHåndteringRepository.hentFagsakerSomHarAktørIdSomBarn(barnAktørId);

        // Assert
        assertThat(resultat)
            .hasSize(1)
            .contains(fagsak);
    }
}
