package no.nav.foreldrepenger.behandlingslager.behandling.repository;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.LocalDate;
import java.time.LocalDateTime;

import org.junit.Rule;
import org.junit.Test;

import no.nav.foreldrepenger.behandlingslager.aktør.NavBruker;
import no.nav.foreldrepenger.behandlingslager.aktør.NavBrukerKjønn;
import no.nav.foreldrepenger.behandlingslager.aktør.Personinfo;
import no.nav.foreldrepenger.behandlingslager.behandling.Behandling;
import no.nav.foreldrepenger.behandlingslager.behandling.BehandlingResultatType;
import no.nav.foreldrepenger.behandlingslager.behandling.Behandlingsresultat;
import no.nav.foreldrepenger.behandlingslager.behandling.BehandlingsresultatRepository;
import no.nav.foreldrepenger.behandlingslager.behandling.vedtak.BehandlingVedtak;
import no.nav.foreldrepenger.behandlingslager.behandling.vedtak.BehandlingVedtakRepository;
import no.nav.foreldrepenger.behandlingslager.behandling.vedtak.VedtakResultatType;
import no.nav.foreldrepenger.behandlingslager.fagsak.Fagsak;
import no.nav.foreldrepenger.behandlingslager.fagsak.FagsakRepository;
import no.nav.foreldrepenger.behandlingslager.fagsak.FagsakYtelseType;
import no.nav.foreldrepenger.behandlingslager.geografisk.Landkoder;
import no.nav.foreldrepenger.behandlingslager.geografisk.Språkkode;
import no.nav.foreldrepenger.dbstoette.UnittestRepositoryRule;
import no.nav.foreldrepenger.domene.typer.AktørId;
import no.nav.foreldrepenger.domene.typer.PersonIdent;

public class BehandlingsresultatRepositoryTest {

    @Rule
    public final UnittestRepositoryRule repoRule = new UnittestRepositoryRule();
    private final BehandlingRepositoryProvider repositoryProvider = new BehandlingRepositoryProvider(repoRule.getEntityManager());

    private final BehandlingsresultatRepository behandlingsresultatRepository = repositoryProvider.getBehandlingsresultatRepository();
    private final FagsakRepository fagsakRepository = repositoryProvider.getFagsakRepository();
    private final BehandlingRepository behandlingRepository = repositoryProvider.getBehandlingRepository();
    private final BehandlingVedtakRepository behandlingVedtakRepository = repositoryProvider.getBehandlingVedtakRepository();

    @Test
    public void behandlingsresultat_som_ikke_finnes__gir_optional_empty() {
        var behandlingsresultat = behandlingsresultatRepository.hentHvisEksisterer(1234L);

        assertThat(behandlingsresultat).isEmpty();
    }

    @Test
    public void skal_hente_behandlingsresultat_fra_vedtatt_behandling() {
        var behandling = opprettVedtattBehandling();

        var behandlingsresultat = behandlingsresultatRepository.hentHvisEksisterer(behandling.getId());

        assertThat(behandlingsresultat).isPresent();
        assertThat(behandlingsresultat.get().getBehandlingResultatType()).isEqualTo(BehandlingResultatType.INNVILGET);
    }

    private Behandling opprettVedtattBehandling() {
        var fagsak = Fagsak.opprettNy(FagsakYtelseType.FORELDREPENGER, NavBruker.opprettNy(lagPerson()));
        fagsakRepository.opprettNy(fagsak);
        var behandling = Behandling.forFørstegangssøknad(fagsak).build();
        behandlingRepository.lagre(behandling, behandlingRepository.taSkriveLås(behandling));
        var behandlingsresultat = Behandlingsresultat.builder()
            .medBehandlingResultatType(BehandlingResultatType.INNVILGET).buildFor(behandling);
        var behandlingVedtak = BehandlingVedtak.builder().medVedtakstidspunkt(LocalDateTime.now()).medBehandlingsresultat(behandlingsresultat)
            .medVedtakResultatType(VedtakResultatType.INNVILGET).medAnsvarligSaksbehandler("asdf").build();
        behandling.avsluttBehandling();
        behandlingRepository.lagre(behandling, behandlingRepository.taSkriveLås(behandling));
        behandlingVedtakRepository.lagre(behandlingVedtak, behandlingRepository.taSkriveLås(behandling));
        return behandling;
    }

    private Personinfo lagPerson() {
        return new Personinfo.Builder()
            .medNavn("Navn navnesen")
            .medAktørId(AktørId.dummy())
            .medFødselsdato(LocalDate.now().minusYears(20))
            .medLandkode(Landkoder.NOR)
            .medNavBrukerKjønn(NavBrukerKjønn.KVINNE)
            .medPersonIdent(new PersonIdent("12345678901"))
            .medForetrukketSpråk(Språkkode.NB)
            .build();
    }

}
