package no.nav.foreldrepenger.behandling.steg.registrersøknad;

import no.nav.foreldrepenger.behandlingskontroll.BehandlingskontrollKontekst;
import no.nav.foreldrepenger.behandlingslager.aktør.NavBruker;
import no.nav.foreldrepenger.behandlingslager.behandling.*;
import no.nav.foreldrepenger.behandlingslager.behandling.aksjonspunkt.AksjonspunktDefinisjon;
import no.nav.foreldrepenger.behandlingslager.behandling.personopplysning.RelasjonsRolleType;
import no.nav.foreldrepenger.behandlingslager.behandling.repository.BehandlingRepository;
import no.nav.foreldrepenger.behandlingslager.behandling.repository.BehandlingRepositoryProvider;
import no.nav.foreldrepenger.behandlingslager.behandling.repository.MottatteDokumentRepository;
import no.nav.foreldrepenger.behandlingslager.fagsak.Fagsak;
import no.nav.foreldrepenger.behandlingslager.fagsak.FagsakRepository;
import no.nav.foreldrepenger.behandlingslager.fagsak.FagsakYtelseType;
import no.nav.foreldrepenger.dbstoette.EntityManagerAwareTest;
import no.nav.foreldrepenger.domene.typer.AktørId;
import no.nav.foreldrepenger.domene.typer.Saksnummer;
import no.nav.foreldrepenger.mottak.dokumentmottak.MottatteDokumentTjeneste;
import no.nav.foreldrepenger.produksjonsstyring.behandlingenhet.RegistrerFagsakEgenskaper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.time.Period;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

class RegistrerSøknadStegTest extends EntityManagerAwareTest {

    private MottatteDokumentTjeneste mottatteDokumentTjeneste;

    private BehandlingRepository behandlingRepository;
    private FagsakRepository fagsakRepository;
    private MottatteDokumentRepository mottatteDokumentRepository;
    private RegistrerSøknadSteg steg;

    @BeforeEach
    public void setUp() {
        behandlingRepository = new BehandlingRepository(getEntityManager());
        fagsakRepository = new FagsakRepository(getEntityManager());
        mottatteDokumentRepository = new MottatteDokumentRepository(getEntityManager());
        mottatteDokumentTjeneste = new MottatteDokumentTjeneste(Period.ofWeeks(6), null, mottatteDokumentRepository,
                new BehandlingRepositoryProvider(getEntityManager()));
        steg = new RegistrerSøknadSteg(behandlingRepository, mottatteDokumentTjeneste, mock(RegistrerFagsakEgenskaper.class), null);
    }

    @Test
    void opprette_registrer_endringssøknad_aksjonspunkt_hvis_mottatt_førstegangssøknad_i_en_revurdering() {

        var aktørId = AktørId.dummy();
        var fagsakId = fagsakRepository
                .opprettNy(
                        new Fagsak(FagsakYtelseType.FORELDREPENGER, NavBruker.opprettNyNB(aktørId), RelasjonsRolleType.MORA, new Saksnummer("9999")));

        var fagsak = fagsakRepository.finnEksaktFagsak(fagsakId);
        var forrigeBehandling = Behandling.forFørstegangssøknad(fagsak)
                .build();

        var revurdering = Behandling.fraTidligereBehandling(forrigeBehandling, BehandlingType.REVURDERING).build();
        var lås = behandlingRepository.taSkriveLås(revurdering.getId());
        behandlingRepository.lagre(revurdering, lås);

        var mottattDokument = new MottattDokument.Builder()
                .medFagsakId(revurdering.getFagsakId())
                .medBehandlingId(revurdering.getId())
                .medDokumentType(DokumentTypeId.SØKNAD_FORELDREPENGER_FØDSEL)
                .medMottattDato(LocalDate.now())
                .medDokumentKategori(DokumentKategori.SØKNAD)
                .build();
        mottatteDokumentRepository.lagre(mottattDokument);

        var kontekst = new BehandlingskontrollKontekst(fagsakId, aktørId, lås);
        var resultat = steg.utførSteg(kontekst);

        assertThat(resultat.getAksjonspunktListe()).containsExactly(AksjonspunktDefinisjon.REGISTRER_PAPIR_ENDRINGSØKNAD_FORELDREPENGER);
    }

    @Test
    void opprett_registrer_papirsøknad_svangerskapspenger_hvis_fagsaktype_er_svp() {

        var aktørId = AktørId.dummy();
        var fagsakId = fagsakRepository
                .opprettNy(new Fagsak(FagsakYtelseType.SVANGERSKAPSPENGER, NavBruker.opprettNyNB(aktørId), RelasjonsRolleType.MORA,
                        new Saksnummer("9999")));

        var fagsak = fagsakRepository.finnEksaktFagsak(fagsakId);
        var forrigeBehandling = Behandling.forFørstegangssøknad(fagsak)
                .build();

        var revurdering = Behandling.fraTidligereBehandling(forrigeBehandling, BehandlingType.REVURDERING).build();
        var lås = behandlingRepository.taSkriveLås(revurdering.getId());
        behandlingRepository.lagre(revurdering, lås);

        var mottattDokument = new MottattDokument.Builder()
                .medFagsakId(revurdering.getFagsakId())
                .medBehandlingId(revurdering.getId())
                .medDokumentType(DokumentTypeId.SØKNAD_SVANGERSKAPSPENGER)
                .medMottattDato(LocalDate.now())
                .medDokumentKategori(DokumentKategori.SØKNAD)
                .build();
        mottatteDokumentRepository.lagre(mottattDokument);

        var kontekst = new BehandlingskontrollKontekst(fagsakId, aktørId, lås);
        var resultat = steg.utførSteg(kontekst);

        assertThat(resultat.getAksjonspunktListe()).containsExactly(AksjonspunktDefinisjon.REGISTRER_PAPIRSØKNAD_SVANGERSKAPSPENGER);
    }

}
