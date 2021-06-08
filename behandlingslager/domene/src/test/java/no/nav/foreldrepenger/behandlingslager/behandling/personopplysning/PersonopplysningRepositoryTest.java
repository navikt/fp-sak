package no.nav.foreldrepenger.behandlingslager.behandling.personopplysning;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.LocalDate;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import no.nav.foreldrepenger.behandlingslager.aktør.NavBruker;
import no.nav.foreldrepenger.behandlingslager.aktør.NavBrukerKjønn;
import no.nav.foreldrepenger.behandlingslager.aktør.Personinfo;
import no.nav.foreldrepenger.behandlingslager.behandling.Behandling;
import no.nav.foreldrepenger.behandlingslager.behandling.repository.BehandlingRepository;
import no.nav.foreldrepenger.behandlingslager.fagsak.Fagsak;
import no.nav.foreldrepenger.behandlingslager.fagsak.FagsakRepository;
import no.nav.foreldrepenger.behandlingslager.fagsak.FagsakYtelseType;
import no.nav.foreldrepenger.behandlingslager.geografisk.Landkoder;
import no.nav.foreldrepenger.dbstoette.EntityManagerAwareTest;
import no.nav.foreldrepenger.domene.typer.AktørId;
import no.nav.foreldrepenger.domene.typer.PersonIdent;

public class PersonopplysningRepositoryTest extends EntityManagerAwareTest {

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
    public void skal_hente_eldste_versjon_av_aggregat() {
        final var personinfo = lagPerson();
        final var fagsak = Fagsak.opprettNy(FagsakYtelseType.ENGANGSTØNAD, NavBruker.opprettNyNB(personinfo.getAktørId()));
        fagsakRepository.opprettNy(fagsak);
        final var builder = Behandling.forFørstegangssøknad(fagsak);
        final var behandling = builder.build();
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
        assertThat(personopplysningerAggregat.getSøker()).isEqualToComparingOnlyGivenFields(førsteVersjonPersonopplysningerAggregat.getSøker(), "aktørId", "navn", "fødselsdato", "region", "sivilstand", "brukerKjønn");
        assertThat(personopplysningerAggregat.getSøker()).isNotEqualTo(førsteVersjonPersonopplysningerAggregat.getSøker());
    }

    private PersonopplysningerAggregat tilAggregat(Behandling behandling, PersonopplysningGrunnlagEntitet grunnlag) {
        return new PersonopplysningerAggregat(grunnlag, behandling.getAktørId(),LocalDate.now(), LocalDate.now());
    }

    private Personinfo lagPerson() {
        return new Personinfo.Builder()
            .medNavn("Navn navnesen")
            .medAktørId(AktørId.dummy())
            .medSivilstandType(SivilstandType.SAMBOER)
            .medFødselsdato(LocalDate.now().minusYears(20))
            .medLandkoder(List.of(Landkoder.NOR))
            .medNavBrukerKjønn(NavBrukerKjønn.KVINNE)
            .medPersonIdent(new PersonIdent("12345678901"))
            .build();
    }
}
