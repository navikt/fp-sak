package no.nav.foreldrepenger.behandlingslager.hendelser.håndtering;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.LocalDate;
import java.util.List;

import org.junit.Rule;
import org.junit.Test;

import no.nav.foreldrepenger.behandlingslager.aktør.NavBruker;
import no.nav.foreldrepenger.behandlingslager.aktør.NavBrukerKjønn;
import no.nav.foreldrepenger.behandlingslager.behandling.Behandling;
import no.nav.foreldrepenger.behandlingslager.behandling.personopplysning.PersonInformasjonBuilder;
import no.nav.foreldrepenger.behandlingslager.behandling.personopplysning.PersonopplysningRepository;
import no.nav.foreldrepenger.behandlingslager.behandling.personopplysning.RelasjonsRolleType;
import no.nav.foreldrepenger.behandlingslager.behandling.repository.BehandlingRepositoryProvider;
import no.nav.foreldrepenger.behandlingslager.fagsak.Fagsak;
import no.nav.foreldrepenger.behandlingslager.fagsak.FagsakYtelseType;
import no.nav.foreldrepenger.behandlingslager.geografisk.Språkkode;
import no.nav.foreldrepenger.behandlingslager.hendelser.HendelseHåndteringRepository;
import no.nav.foreldrepenger.dbstoette.UnittestRepositoryRule;
import no.nav.foreldrepenger.domene.typer.AktørId;
import no.nav.vedtak.felles.testutilities.db.Repository;

public class HendelseHåndteringRepositoryImplTest {

    @Rule
    public UnittestRepositoryRule repositoryRule = new UnittestRepositoryRule();
    public Repository repository = repositoryRule.getRepository();

    private HendelseHåndteringRepository hendelseHåndteringRepository = new HendelseHåndteringRepository(repositoryRule.getEntityManager());
    private BehandlingRepositoryProvider repositoryProvider = new BehandlingRepositoryProvider(repositoryRule.getEntityManager());
    private PersonopplysningRepository personopplysningRepository = repositoryProvider.getPersonopplysningRepository();

    @Test
    public void skal_finne_fagsak_på_barnets_aktørId_i_behandlingsgrunnlaget() {
        // Arrange
        AktørId morAktørId = AktørId.dummy();
        AktørId barnAktørId = AktørId.dummy();
        LocalDate fødselsdato = LocalDate.now();

        NavBruker navBruker = NavBruker.opprettNy(morAktørId, Språkkode.NB);
        Fagsak fagsak = Fagsak.opprettNy(FagsakYtelseType.FORELDREPENGER, navBruker);
        repository.lagre(navBruker);
        repository.lagre(fagsak);

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
        List<Fagsak> resultat = hendelseHåndteringRepository.hentFagsakerSomHarAktørIdSomBarn(barnAktørId);

        // Assert
        assertThat(resultat).hasSize(1);
        assertThat(resultat).contains(fagsak);
    }
}
