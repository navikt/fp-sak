package no.nav.foreldrepenger.behandling.steg.registrersøknad;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.LocalDate;

import javax.persistence.EntityManager;

import org.junit.Rule;
import org.junit.Test;

import no.nav.foreldrepenger.behandlingskontroll.BehandleStegResultat;
import no.nav.foreldrepenger.behandlingskontroll.BehandlingskontrollKontekst;
import no.nav.foreldrepenger.behandlingslager.aktør.NavBruker;
import no.nav.foreldrepenger.behandlingslager.aktør.NavBrukerKjønn;
import no.nav.foreldrepenger.behandlingslager.aktør.Personinfo;
import no.nav.foreldrepenger.behandlingslager.behandling.Behandling;
import no.nav.foreldrepenger.behandlingslager.behandling.BehandlingType;
import no.nav.foreldrepenger.behandlingslager.behandling.DokumentKategori;
import no.nav.foreldrepenger.behandlingslager.behandling.DokumentTypeId;
import no.nav.foreldrepenger.behandlingslager.behandling.MottattDokument;
import no.nav.foreldrepenger.behandlingslager.behandling.aksjonspunkt.AksjonspunktDefinisjon;
import no.nav.foreldrepenger.behandlingslager.behandling.personopplysning.RelasjonsRolleType;
import no.nav.foreldrepenger.behandlingslager.behandling.repository.BehandlingLås;
import no.nav.foreldrepenger.behandlingslager.behandling.repository.BehandlingRepositoryProvider;
import no.nav.foreldrepenger.behandlingslager.fagsak.Fagsak;
import no.nav.foreldrepenger.behandlingslager.fagsak.FagsakYtelseType;
import no.nav.foreldrepenger.behandlingslager.geografisk.Språkkode;
import no.nav.foreldrepenger.dbstoette.UnittestRepositoryRule;
import no.nav.foreldrepenger.domene.typer.AktørId;
import no.nav.foreldrepenger.domene.typer.PersonIdent;
import no.nav.foreldrepenger.domene.typer.Saksnummer;

public class RegistrerSøknadStegTest {

    @Rule
    public UnittestRepositoryRule repoRule = new UnittestRepositoryRule();
    private EntityManager entityManager = repoRule.getEntityManager();
    private BehandlingRepositoryProvider repositoryProvider = new BehandlingRepositoryProvider(entityManager);

    @Test
    public void opprette_registrer_endringssøknad_aksjonspunkt_hvis_mottatt_førstegangssøknad_i_en_revurdering() {
        RegistrerSøknadSteg steg = new RegistrerSøknadSteg(repositoryProvider, null);

        AktørId aktørId = AktørId.dummy();
        Personinfo personinfo = new Personinfo.Builder()
            .medNavBrukerKjønn(NavBrukerKjønn.KVINNE)
            .medPersonIdent(PersonIdent.fra("123"))
            .medAktørId(aktørId)
            .medNavn("Navn Navnesen")
            .medFødselsdato(LocalDate.now().minusYears(30))
            .medForetrukketSpråk(Språkkode.nb)
            .build();
        Long fagsakId = repositoryProvider.getFagsakRepository()
            .opprettNy(new Fagsak(FagsakYtelseType.FORELDREPENGER, NavBruker.opprettNy(personinfo), RelasjonsRolleType.MORA, new Saksnummer("123")));

        Fagsak fagsak = repositoryProvider.getFagsakRepository().finnEksaktFagsak(fagsakId);
        Behandling forrigeBehandling = Behandling.forFørstegangssøknad(fagsak)
            .build();

        Behandling revurdering = Behandling.fraTidligereBehandling(forrigeBehandling, BehandlingType.REVURDERING).build();
        BehandlingLås lås = repositoryProvider.getBehandlingLåsRepository().taLås(revurdering.getId());
        repositoryProvider.getBehandlingRepository().lagre(revurdering, lås);

        MottattDokument mottattDokument = new MottattDokument.Builder()
            .medFagsakId(revurdering.getFagsakId())
            .medBehandlingId(revurdering.getId())
            .medDokumentType(DokumentTypeId.SØKNAD_FORELDREPENGER_FØDSEL)
            .medMottattDato(LocalDate.now())
            .medDokumentKategori(DokumentKategori.SØKNAD)
            .build();
        repositoryProvider.getMottatteDokumentRepository().lagre(mottattDokument);

        BehandlingskontrollKontekst kontekst = new BehandlingskontrollKontekst(fagsakId, aktørId, lås);
        BehandleStegResultat resultat = steg.utførSteg(kontekst);

        assertThat(resultat.getAksjonspunktListe()).containsExactly(AksjonspunktDefinisjon.REGISTRER_PAPIR_ENDRINGSØKNAD_FORELDREPENGER);
    }

    @Test
    public void opprett_registrer_papirsøknad_svangerskapspenger_hvis_fagsaktype_er_svp() {

        var steg = new RegistrerSøknadSteg(repositoryProvider, null);

        var aktørId = AktørId.dummy();
        var personinfo = new Personinfo.Builder()
            .medNavBrukerKjønn(NavBrukerKjønn.KVINNE)
            .medPersonIdent(PersonIdent.fra("123"))
            .medAktørId(aktørId)
            .medNavn("Navn Navnesen")
            .medFødselsdato(LocalDate.now().minusYears(30))
            .medForetrukketSpråk(Språkkode.nb)
            .build();
        Long fagsakId = repositoryProvider.getFagsakRepository()
            .opprettNy(new Fagsak(FagsakYtelseType.SVANGERSKAPSPENGER, NavBruker.opprettNy(personinfo), RelasjonsRolleType.MORA, new Saksnummer("124")));

        Fagsak fagsak = repositoryProvider.getFagsakRepository().finnEksaktFagsak(fagsakId);
        Behandling forrigeBehandling = Behandling.forFørstegangssøknad(fagsak)
            .build();

        Behandling revurdering = Behandling.fraTidligereBehandling(forrigeBehandling, BehandlingType.REVURDERING).build();
        BehandlingLås lås = repositoryProvider.getBehandlingLåsRepository().taLås(revurdering.getId());
        repositoryProvider.getBehandlingRepository().lagre(revurdering, lås);

        MottattDokument mottattDokument = new MottattDokument.Builder()
            .medFagsakId(revurdering.getFagsakId())
            .medBehandlingId(revurdering.getId())
            .medDokumentType(DokumentTypeId.SØKNAD_SVANGERSKAPSPENGER)
            .medMottattDato(LocalDate.now())
            .medDokumentKategori(DokumentKategori.SØKNAD)
            .build();
        repositoryProvider.getMottatteDokumentRepository().lagre(mottattDokument);

        BehandlingskontrollKontekst kontekst = new BehandlingskontrollKontekst(fagsakId, aktørId, lås);
        BehandleStegResultat resultat = steg.utførSteg(kontekst);

        assertThat(resultat.getAksjonspunktListe()).containsExactly(AksjonspunktDefinisjon.REGISTRER_PAPIRSØKNAD_SVANGERSKAPSPENGER);
    }



}
