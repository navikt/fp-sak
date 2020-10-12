package no.nav.foreldrepenger.behandling.impl;

import static java.time.Month.JANUARY;
import static no.nav.foreldrepenger.behandlingslager.aktør.NavBrukerKjønn.MANN;
import static org.assertj.core.api.Assertions.assertThat;

import java.time.LocalDate;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;

import no.nav.foreldrepenger.behandling.FagsakTjeneste;
import no.nav.foreldrepenger.behandlingslager.aktør.BrukerTjeneste;
import no.nav.foreldrepenger.behandlingslager.aktør.NavBruker;
import no.nav.foreldrepenger.behandlingslager.aktør.NavBrukerRepository;
import no.nav.foreldrepenger.behandlingslager.behandling.Behandling;
import no.nav.foreldrepenger.behandlingslager.behandling.personopplysning.PersonInformasjonBuilder;
import no.nav.foreldrepenger.behandlingslager.behandling.personopplysning.PersonopplysningGrunnlagEntitet;
import no.nav.foreldrepenger.behandlingslager.behandling.personopplysning.PersonopplysningRepository;
import no.nav.foreldrepenger.behandlingslager.behandling.personopplysning.PersonopplysningerAggregat;
import no.nav.foreldrepenger.behandlingslager.behandling.personopplysning.RelasjonsRolleType;
import no.nav.foreldrepenger.behandlingslager.behandling.personopplysning.SivilstandType;
import no.nav.foreldrepenger.behandlingslager.behandling.repository.BehandlingLås;
import no.nav.foreldrepenger.behandlingslager.behandling.repository.BehandlingRepository;
import no.nav.foreldrepenger.behandlingslager.behandling.søknad.SøknadRepository;
import no.nav.foreldrepenger.behandlingslager.fagsak.Fagsak;
import no.nav.foreldrepenger.behandlingslager.fagsak.FagsakRepository;
import no.nav.foreldrepenger.behandlingslager.fagsak.FagsakStatus;
import no.nav.foreldrepenger.behandlingslager.fagsak.FagsakYtelseType;
import no.nav.foreldrepenger.behandlingslager.geografisk.Region;
import no.nav.foreldrepenger.behandlingslager.geografisk.Språkkode;
import no.nav.foreldrepenger.dbstoette.FPsakEntityManagerAwareExtension;
import no.nav.foreldrepenger.domene.tid.DatoIntervallEntitet;
import no.nav.foreldrepenger.domene.typer.AktørId;
import no.nav.vedtak.felles.testutilities.Whitebox;
import no.nav.vedtak.felles.testutilities.db.EntityManagerAwareTest;

@ExtendWith(MockitoExtension.class)
@ExtendWith(FPsakEntityManagerAwareExtension.class)
public class FagsakTjenesteTest extends EntityManagerAwareTest {

    private FagsakTjeneste tjeneste;
    private BrukerTjeneste brukerTjeneste;

    private BehandlingRepository behandlingRepository;
    private PersonopplysningRepository personopplysningRepository;

    private Fagsak fagsak;

    private final AktørId forelderAktørId = AktørId.dummy();
    private LocalDate forelderFødselsdato = LocalDate.of(1990, JANUARY, 1);

    @BeforeEach
    public void oppsett() {
        behandlingRepository = new BehandlingRepository(getEntityManager());
        personopplysningRepository = new PersonopplysningRepository(getEntityManager());
        tjeneste = new FagsakTjeneste(new FagsakRepository(getEntityManager()),
                new SøknadRepository(getEntityManager(), behandlingRepository), null);

        brukerTjeneste = new BrukerTjeneste(new NavBrukerRepository(getEntityManager()));

    }

    private Fagsak lagNyFagsak() {
        NavBruker søker = NavBruker.opprettNy(forelderAktørId, Språkkode.NB);
        Fagsak fagsak = Fagsak.opprettNy(FagsakYtelseType.ENGANGSTØNAD, søker);
        tjeneste.opprettFagsak(fagsak);
        return fagsak;
    }

    @Test
    public void skal_oppdatere_fagsakrelasjon_med_barn_og_endret_kjønn() {
        fagsak = lagNyFagsak();
        LocalDate barnsFødselsdato = LocalDate.of(2017, JANUARY, 1);
        AktørId barnAktørId = AktørId.dummy();

        // Arrange
        Behandling.Builder behandlingBuilder = Behandling.forFørstegangssøknad(fagsak);
        Behandling behandling = behandlingBuilder.build();
        BehandlingLås lås = behandlingRepository.taSkriveLås(behandling);
        behandlingRepository.lagre(behandling, lås);

        // TODO opplegg for å opprette PersonInformasjon og PersonopplysningerAggregat
        // på en enklere måte
        Long behandlingId = behandling.getId();
        final PersonInformasjonBuilder medBarnOgOppdatertKjønn = personopplysningRepository.opprettBuilderForRegisterdata(behandlingId);
        medBarnOgOppdatertKjønn
                .leggTil(
                        medBarnOgOppdatertKjønn.getPersonopplysningBuilder(barnAktørId)
                                .medKjønn(MANN)
                                .medNavn("Baby Nordmann")
                                .medFødselsdato(barnsFødselsdato)
                                .medSivilstand(SivilstandType.UGIFT)
                                .medRegion(Region.NORDEN))
                .leggTil(
                        medBarnOgOppdatertKjønn.getPersonopplysningBuilder(forelderAktørId)
                                .medKjønn(MANN)
                                .medSivilstand(SivilstandType.UGIFT)
                                .medFødselsdato(forelderFødselsdato)
                                .medRegion(Region.NORDEN)
                                .medNavn("Kari Nordmann"))
                .leggTil(
                        medBarnOgOppdatertKjønn
                                .getRelasjonBuilder(forelderAktørId, barnAktørId, RelasjonsRolleType.BARN)
                                .harSammeBosted(true))
                .leggTil(
                        medBarnOgOppdatertKjønn
                                .getRelasjonBuilder(barnAktørId, forelderAktørId, RelasjonsRolleType.FARA)
                                .harSammeBosted(true));

        Whitebox.setInternalState(fagsak, "fagsakStatus", FagsakStatus.LØPENDE); // dirty, men eksponerer ikke status nå
        personopplysningRepository.lagre(behandlingId, medBarnOgOppdatertKjønn);
        final PersonopplysningGrunnlagEntitet personopplysningGrunnlag = personopplysningRepository.hentPersonopplysninger(behandlingId);

        PersonopplysningerAggregat personopplysningerAggregat = new PersonopplysningerAggregat(personopplysningGrunnlag,
                forelderAktørId, DatoIntervallEntitet.fraOgMedTilOgMed(LocalDate.now(), LocalDate.now()));

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
        Whitebox.setInternalState(fagsak, "fagsakStatus", FagsakStatus.LØPENDE); // dirty, men eksponerer ikke status nå

        // Ifølgeregler i mottak skal vi opprette en nyTerminbekreftelse sak hvis vi
        // ikke har sak nyere enn 10 mnd:
        NavBruker søker = brukerTjeneste.hentEllerOpprettFraAktorId(forelderAktørId, Språkkode.NB);
        Fagsak fagsakNy = Fagsak.opprettNy(FagsakYtelseType.ENGANGSTØNAD, søker);
        tjeneste.opprettFagsak(fagsakNy);
        assertThat(fagsak.getNavBruker().getId()).as("Forventer at fagsakene peker til samme bruker")
                .isEqualTo(fagsakNy.getNavBruker().getId());
    }

}
