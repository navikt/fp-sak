package no.nav.foreldrepenger.behandling.impl;

import static java.time.Month.JANUARY;
import static no.nav.foreldrepenger.behandlingslager.aktør.NavBrukerKjønn.MANN;
import static org.assertj.core.api.Assertions.assertThat;

import java.time.LocalDate;

import javax.persistence.EntityManager;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;

import no.nav.foreldrepenger.behandling.FagsakTjeneste;
import no.nav.foreldrepenger.behandlingslager.aktør.NavBruker;
import no.nav.foreldrepenger.behandlingslager.aktør.NavBrukerRepository;
import no.nav.foreldrepenger.behandlingslager.behandling.Behandling;
import no.nav.foreldrepenger.behandlingslager.behandling.personopplysning.PersonopplysningRepository;
import no.nav.foreldrepenger.behandlingslager.behandling.personopplysning.PersonopplysningerAggregat;
import no.nav.foreldrepenger.behandlingslager.behandling.personopplysning.RelasjonsRolleType;
import no.nav.foreldrepenger.behandlingslager.behandling.personopplysning.SivilstandType;
import no.nav.foreldrepenger.behandlingslager.behandling.repository.BehandlingRepository;
import no.nav.foreldrepenger.behandlingslager.behandling.søknad.SøknadRepository;
import no.nav.foreldrepenger.behandlingslager.fagsak.Fagsak;
import no.nav.foreldrepenger.behandlingslager.fagsak.FagsakRepository;
import no.nav.foreldrepenger.behandlingslager.fagsak.FagsakStatus;
import no.nav.foreldrepenger.behandlingslager.fagsak.FagsakYtelseType;
import no.nav.foreldrepenger.behandlingslager.geografisk.Språkkode;
import no.nav.foreldrepenger.dbstoette.FPsakEntityManagerAwareExtension;
import no.nav.foreldrepenger.domene.typer.AktørId;

@ExtendWith(MockitoExtension.class)
@ExtendWith(FPsakEntityManagerAwareExtension.class)
public class FagsakTjenesteTest {

    private FagsakTjeneste tjeneste;

    private BehandlingRepository behandlingRepository;
    private PersonopplysningRepository personopplysningRepository;
    private NavBrukerRepository brukerRepository;

    private Fagsak fagsak;

    private final AktørId forelderAktørId = AktørId.dummy();
    private final LocalDate forelderFødselsdato = LocalDate.of(1990, JANUARY, 1);

    @BeforeEach
    public void oppsett(EntityManager entityManager) {
        brukerRepository = new NavBrukerRepository(entityManager);
        behandlingRepository = new BehandlingRepository(entityManager);
        personopplysningRepository = new PersonopplysningRepository(entityManager);
        tjeneste = new FagsakTjeneste(new FagsakRepository(entityManager),
                new SøknadRepository(entityManager, behandlingRepository), null);

    }

    private Fagsak lagNyFagsak() {
        var søker = NavBruker.opprettNyNB(forelderAktørId);
        return tjeneste.opprettFagsak(FagsakYtelseType.ENGANGSTØNAD, søker);
    }

    @Test
    public void skal_oppdatere_fagsakrelasjon_med_barn_og_endret_kjønn() {
        fagsak = lagNyFagsak();
        var barnsFødselsdato = LocalDate.of(2017, JANUARY, 1);
        var barnAktørId = AktørId.dummy();

        // Arrange
        var behandlingBuilder = Behandling.forFørstegangssøknad(fagsak);
        var behandling = behandlingBuilder.build();
        var lås = behandlingRepository.taSkriveLås(behandling);
        behandlingRepository.lagre(behandling, lås);

        // TODO opplegg for å opprette PersonInformasjon og PersonopplysningerAggregat
        // på en enklere måte
        var behandlingId = behandling.getId();
        final var medBarnOgOppdatertKjønn = personopplysningRepository.opprettBuilderForRegisterdata(behandlingId);
        medBarnOgOppdatertKjønn
                .leggTil(
                        medBarnOgOppdatertKjønn.getPersonopplysningBuilder(barnAktørId)
                                .medKjønn(MANN)
                                .medNavn("Baby Nordmann")
                                .medFødselsdato(barnsFødselsdato)
                                .medSivilstand(SivilstandType.UGIFT))
                .leggTil(
                        medBarnOgOppdatertKjønn.getPersonopplysningBuilder(forelderAktørId)
                                .medKjønn(MANN)
                                .medSivilstand(SivilstandType.UGIFT)
                                .medFødselsdato(forelderFødselsdato)
                                .medNavn("Kari Nordmann"))
                .leggTil(
                        medBarnOgOppdatertKjønn
                                .getRelasjonBuilder(forelderAktørId, barnAktørId, RelasjonsRolleType.BARN)
                                .harSammeBosted(true))
                .leggTil(
                        medBarnOgOppdatertKjønn
                                .getRelasjonBuilder(barnAktørId, forelderAktørId, RelasjonsRolleType.FARA)
                                .harSammeBosted(true));

        // Whitebox.setInternalState(fagsak, "fagsakStatus", FagsakStatus.LØPENDE); //
        // dirty, men eksponerer ikke status nå
        fagsak.setStatus(FagsakStatus.LØPENDE);
        personopplysningRepository.lagre(behandlingId, medBarnOgOppdatertKjønn);
        final var personopplysningGrunnlag = personopplysningRepository.hentPersonopplysninger(behandlingId);

        var personopplysningerAggregat = new PersonopplysningerAggregat(personopplysningGrunnlag,
                forelderAktørId, LocalDate.now(), LocalDate.now());

        // Act
        tjeneste.oppdaterFagsak(behandling, personopplysningerAggregat, personopplysningerAggregat.getBarna());

        // Assert
        var oppdatertFagsak = tjeneste.finnFagsakerForAktør(forelderAktørId);
        assertThat(oppdatertFagsak).hasSize(1);
        assertThat(oppdatertFagsak.get(0).getRelasjonsRolleType().getKode()).isEqualTo(RelasjonsRolleType.FARA.getKode());
    }

    @Test
    public void opprettFlereFagsakerSammeBruker() {
        // Opprett en fagsak i systemet
        fagsak = lagNyFagsak();
        fagsak.setStatus(FagsakStatus.LØPENDE);

        // Whitebox.setInternalState(fagsak, "fagsakStatus", FagsakStatus.LØPENDE); //
        // dirty, men eksponerer ikke status nå

        // Ifølgeregler i mottak skal vi opprette en nyTerminbekreftelse sak hvis vi
        // ikke har sak nyere enn 10 mnd:
        var søker = brukerRepository.hent(forelderAktørId).orElseGet(() -> NavBruker.opprettNy(forelderAktørId, Språkkode.NB));
        var fagsakNy = tjeneste.opprettFagsak(FagsakYtelseType.ENGANGSTØNAD, søker);
        assertThat(fagsak.getNavBruker().getId()).as("Forventer at fagsakene peker til samme bruker")
                .isEqualTo(fagsakNy.getNavBruker().getId());
        assertThat(fagsak.getSaksnummer().getVerdi().compareTo("152001000") > 0).isTrue();
    }

}
