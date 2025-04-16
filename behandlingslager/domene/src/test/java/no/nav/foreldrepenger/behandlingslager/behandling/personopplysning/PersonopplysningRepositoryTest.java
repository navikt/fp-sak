package no.nav.foreldrepenger.behandlingslager.behandling.personopplysning;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.LocalDate;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import no.nav.foreldrepenger.behandlingslager.aktør.NavBruker;
import no.nav.foreldrepenger.behandlingslager.aktør.NavBrukerKjønn;
import no.nav.foreldrepenger.behandlingslager.aktør.Personinfo;
import no.nav.foreldrepenger.behandlingslager.aktør.historikk.Gyldighetsperiode;
import no.nav.foreldrepenger.behandlingslager.aktør.historikk.StatsborgerskapPeriode;
import no.nav.foreldrepenger.behandlingslager.behandling.BasicBehandlingBuilder;
import no.nav.foreldrepenger.behandlingslager.behandling.Behandling;
import no.nav.foreldrepenger.behandlingslager.behandling.repository.BehandlingRepository;
import no.nav.foreldrepenger.behandlingslager.behandling.søknad.SøknadAnnenPartType;
import no.nav.foreldrepenger.behandlingslager.fagsak.Fagsak;
import no.nav.foreldrepenger.behandlingslager.fagsak.FagsakRepository;
import no.nav.foreldrepenger.behandlingslager.fagsak.FagsakYtelseType;
import no.nav.foreldrepenger.behandlingslager.geografisk.Landkoder;
import no.nav.foreldrepenger.dbstoette.EntityManagerAwareTest;
import no.nav.foreldrepenger.domene.typer.AktørId;
import no.nav.foreldrepenger.domene.typer.PersonIdent;

class PersonopplysningRepositoryTest extends EntityManagerAwareTest {

    private PersonopplysningRepository repository;
    private FagsakRepository fagsakRepository;
    private BehandlingRepository behandlingRepository;

    @BeforeEach
    void setUp() {
        var entityManager = getEntityManager();
        repository = new PersonopplysningRepository(entityManager);
        fagsakRepository = new FagsakRepository(entityManager);
        behandlingRepository = new BehandlingRepository(entityManager);
    }

    @Test
    void skal_hente_eldste_versjon_av_aggregat() {
        var personinfo = lagPerson();
        var fagsak = Fagsak.opprettNy(FagsakYtelseType.ENGANGSTØNAD, NavBruker.opprettNyNB(personinfo.getAktørId()));
        fagsakRepository.opprettNy(fagsak);
        var builder = Behandling.forFørstegangssøknad(fagsak);
        var behandling = builder.build();
        behandlingRepository.lagre(behandling, behandlingRepository.taSkriveLås(behandling));

        var behandlingId = behandling.getId();
        var informasjonBuilder = repository.opprettBuilderForRegisterdata(behandlingId);
        var personopplysningBuilder = informasjonBuilder.getPersonopplysningBuilder(personinfo.getAktørId());
        personopplysningBuilder.medNavn(personinfo.getNavn())
            .medKjønn(personinfo.getKjønn())
            .medFødselsdato(personinfo.getFødselsdato())
            .medSivilstand(personinfo.getSivilstandType());
        informasjonBuilder.leggTil(personopplysningBuilder);
        repository.lagre(behandlingId, informasjonBuilder);

        informasjonBuilder = repository.opprettBuilderForRegisterdata(behandlingId);
        personopplysningBuilder = informasjonBuilder.getPersonopplysningBuilder(personinfo.getAktørId());
        personopplysningBuilder.medNavn(personinfo.getNavn())
            .medKjønn(personinfo.getKjønn())
            .medFødselsdato(personinfo.getFødselsdato())
            .medSivilstand(personinfo.getSivilstandType())
            .medDødsdato(LocalDate.now());
        informasjonBuilder.leggTil(personopplysningBuilder);
        repository.lagre(behandlingId, informasjonBuilder);

        var personopplysningerAggregat = tilAggregat(behandling, repository.hentPersonopplysninger(behandlingId));
        var førsteVersjonPersonopplysningerAggregat = tilAggregat(behandling, repository.hentFørsteVersjonAvPersonopplysninger(behandlingId));

        assertThat(personopplysningerAggregat).isNotEqualTo(førsteVersjonPersonopplysningerAggregat);
        assertThat(personopplysningerAggregat.getSøker()).isEqualToComparingOnlyGivenFields(førsteVersjonPersonopplysningerAggregat.getSøker(), "aktørId", "navn", "fødselsdato", "sivilstand", "brukerKjønn");
        assertThat(personopplysningerAggregat.getSøker()).isNotEqualTo(førsteVersjonPersonopplysningerAggregat.getSøker());
    }


    @Test
    void skal_finne_aktoerId_for_saksnummer_kun_sak() {
        var fagsak = new BasicBehandlingBuilder(getEntityManager()).opprettFagsak(FagsakYtelseType.FORELDREPENGER, AktørId.dummy());

        var aktørIder = repository.hentAktørIdKnyttetTilSaksnummer(fagsak.getSaksnummer().getVerdi());
        assertThat(aktørIder).containsOnly(fagsak.getAktørId());
    }

    @Test
    void skal_finne_aktoerId_for_saksnummer_behandling_med_oppgitt_annenpart() {
        var fagsak = new BasicBehandlingBuilder(getEntityManager()).opprettFagsak(FagsakYtelseType.FORELDREPENGER, AktørId.dummy());
        var builder = Behandling.forFørstegangssøknad(fagsak);
        var behandling = builder.build();
        behandlingRepository.lagre(behandling, behandlingRepository.taSkriveLås(behandling));
        var annenPart = AktørId.dummy();
        repository.lagre(behandling.getId(), new OppgittAnnenPartBuilder().medType(SøknadAnnenPartType.FAR).medAktørId(annenPart).build());

        var aktørIder = repository.hentAktørIdKnyttetTilSaksnummer(fagsak.getSaksnummer().getVerdi());
        assertThat(aktørIder).containsOnly(fagsak.getAktørId(), annenPart);
    }

    @Test
    void skal_finne_aktoerId_for_saksnummer_behandling_med_annenpart_oppgitt_og_register() {
        var fagsak = new BasicBehandlingBuilder(getEntityManager()).opprettFagsak(FagsakYtelseType.FORELDREPENGER, AktørId.dummy());
        var builder = Behandling.forFørstegangssøknad(fagsak);
        var behandling = builder.build();
        behandlingRepository.lagre(behandling, behandlingRepository.taSkriveLås(behandling));
        var annenPart = AktørId.dummy();
        repository.lagre(behandling.getId(), new OppgittAnnenPartBuilder().medType(SøknadAnnenPartType.FAR).medAktørId(annenPart).build());

        var informasjonBuilder = repository.opprettBuilderForRegisterdata(behandling.getId());
        var personopplysningBuilder = informasjonBuilder.getPersonopplysningBuilder(annenPart)
            .medNavn("Annen part")
            .medKjønn(NavBrukerKjønn.MANN)
            .medFødselsdato(LocalDate.now().minusYears(25))
            .medSivilstand(SivilstandType.GIFT);
        informasjonBuilder.leggTil(personopplysningBuilder);
        repository.lagre(behandling.getId(), informasjonBuilder);

        var aktørIder = repository.hentAktørIdKnyttetTilSaksnummer(fagsak.getSaksnummer().getVerdi());
        assertThat(aktørIder).containsOnly(fagsak.getAktørId(), annenPart);
    }

    @Test
    void skal_finne_aktoerId_for_saksnummer_behandling_med_annenpart_og_barn() {
        var fagsak = new BasicBehandlingBuilder(getEntityManager()).opprettFagsak(FagsakYtelseType.FORELDREPENGER, AktørId.dummy());
        var builder = Behandling.forFørstegangssøknad(fagsak);
        var behandling = builder.build();
        behandlingRepository.lagre(behandling, behandlingRepository.taSkriveLås(behandling));
        var annenPart = AktørId.dummy();
        repository.lagre(behandling.getId(), new OppgittAnnenPartBuilder().medType(SøknadAnnenPartType.FAR).medAktørId(annenPart).build());

        var informasjonBuilder = repository.opprettBuilderForRegisterdata(behandling.getId());
        var personopplysningBuilder = informasjonBuilder.getPersonopplysningBuilder(annenPart)
            .medNavn("Annen part")
            .medKjønn(NavBrukerKjønn.MANN)
            .medFødselsdato(LocalDate.now().minusYears(25))
            .medSivilstand(SivilstandType.GIFT);
        informasjonBuilder.leggTil(personopplysningBuilder);


        var barn = AktørId.dummy();
        var personopplysningBuilderBarn = informasjonBuilder.getPersonopplysningBuilder(barn)
            .medNavn("Barn Bruker")
            .medKjønn(NavBrukerKjønn.KVINNE)
            .medFødselsdato(LocalDate.now().minusDays(2))
            .medSivilstand(SivilstandType.UGIFT);
        informasjonBuilder.leggTil(personopplysningBuilderBarn);

        repository.lagre(behandling.getId(), informasjonBuilder);

        var aktørIder = repository.hentAktørIdKnyttetTilSaksnummer(fagsak.getSaksnummer().getVerdi());
        assertThat(aktørIder).containsOnly(fagsak.getAktørId(), annenPart, barn);
    }

    @Test
    void skal_finne_aktoerId_for_saksnummer_behandling_med_barn() {
        var fagsak = new BasicBehandlingBuilder(getEntityManager()).opprettFagsak(FagsakYtelseType.FORELDREPENGER, AktørId.dummy());
        var builder = Behandling.forFørstegangssøknad(fagsak);
        var behandling = builder.build();
        behandlingRepository.lagre(behandling, behandlingRepository.taSkriveLås(behandling));

        var informasjonBuilder = repository.opprettBuilderForRegisterdata(behandling.getId());
        var barn = AktørId.dummy();
        var personopplysningBuilderBarn = informasjonBuilder.getPersonopplysningBuilder(barn)
            .medNavn("Barn Bruker")
            .medKjønn(NavBrukerKjønn.KVINNE)
            .medFødselsdato(LocalDate.now().minusDays(2))
            .medSivilstand(SivilstandType.UGIFT);
        informasjonBuilder.leggTil(personopplysningBuilderBarn);

        repository.lagre(behandling.getId(), informasjonBuilder);

        var aktørIder = repository.hentAktørIdKnyttetTilSaksnummer(fagsak.getSaksnummer().getVerdi());
        assertThat(aktørIder).containsOnly(fagsak.getAktørId(), barn);
    }

    private PersonopplysningerAggregat tilAggregat(Behandling behandling, PersonopplysningGrunnlagEntitet grunnlag) {
        return new PersonopplysningerAggregat(grunnlag, behandling.getAktørId());
    }

    private Personinfo lagPerson() {
        return new Personinfo.Builder()
            .medNavn("Navn navnesen")
            .medAktørId(AktørId.dummy())
            .medSivilstandType(SivilstandType.SAMBOER)
            .medFødselsdato(LocalDate.now().minusYears(20))
            .medLandkoder(List.of(new StatsborgerskapPeriode(Gyldighetsperiode.innenfor(null, null), Landkoder.NOR)))
            .medNavBrukerKjønn(NavBrukerKjønn.KVINNE)
            .medPersonIdent(new PersonIdent("12345678901"))
            .build();
    }
}
