package no.nav.foreldrepenger.behandling.steg.registrersøknad;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.LocalDate;
import java.time.Period;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

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
import no.nav.foreldrepenger.behandlingslager.behandling.repository.BehandlingRepository;
import no.nav.foreldrepenger.behandlingslager.behandling.repository.BehandlingRepositoryProvider;
import no.nav.foreldrepenger.behandlingslager.behandling.repository.MottatteDokumentRepository;
import no.nav.foreldrepenger.behandlingslager.fagsak.Fagsak;
import no.nav.foreldrepenger.behandlingslager.fagsak.FagsakRepository;
import no.nav.foreldrepenger.behandlingslager.fagsak.FagsakYtelseType;
import no.nav.foreldrepenger.behandlingslager.geografisk.Språkkode;
import no.nav.foreldrepenger.dbstoette.FPsakEntityManagerAwareExtension;
import no.nav.foreldrepenger.domene.typer.AktørId;
import no.nav.foreldrepenger.domene.typer.PersonIdent;
import no.nav.foreldrepenger.domene.typer.Saksnummer;
import no.nav.foreldrepenger.mottak.dokumentmottak.MottatteDokumentTjeneste;
import no.nav.vedtak.felles.testutilities.db.EntityManagerAwareTest;

@ExtendWith(FPsakEntityManagerAwareExtension.class)
public class RegistrerSøknadStegTest extends EntityManagerAwareTest {


    private MottatteDokumentTjeneste mottatteDokumentTjeneste;

    private BehandlingRepository behandlingRepository;
    private FagsakRepository fagsakRepository;
    private MottatteDokumentRepository mottatteDokumentRepository;
    private RegistrerSøknadSteg steg;

    @BeforeEach
    public void setUp() {
        behandlingRepository = new BehandlingRepository(getEntityManager());;
        fagsakRepository = new FagsakRepository(getEntityManager());
        mottatteDokumentRepository = new MottatteDokumentRepository(getEntityManager());
        mottatteDokumentTjeneste = new MottatteDokumentTjeneste(Period.ofWeeks(6), null, mottatteDokumentRepository, new BehandlingRepositoryProvider(getEntityManager()));
        steg = new RegistrerSøknadSteg(behandlingRepository, mottatteDokumentTjeneste, null);
    }

    @Test
    public void opprette_registrer_endringssøknad_aksjonspunkt_hvis_mottatt_førstegangssøknad_i_en_revurdering() {

        AktørId aktørId = AktørId.dummy();
        Personinfo personinfo = new Personinfo.Builder()
            .medNavBrukerKjønn(NavBrukerKjønn.KVINNE)
            .medPersonIdent(PersonIdent.fra("123"))
            .medAktørId(aktørId)
            .medNavn("Navn Navnesen")
            .medFødselsdato(LocalDate.now().minusYears(30))
            .medForetrukketSpråk(Språkkode.NB)
            .build();
        Long fagsakId = fagsakRepository
            .opprettNy(new Fagsak(FagsakYtelseType.FORELDREPENGER, NavBruker.opprettNy(personinfo), RelasjonsRolleType.MORA, new Saksnummer("123")));

        Fagsak fagsak = fagsakRepository.finnEksaktFagsak(fagsakId);
        Behandling forrigeBehandling = Behandling.forFørstegangssøknad(fagsak)
            .build();

        Behandling revurdering = Behandling.fraTidligereBehandling(forrigeBehandling, BehandlingType.REVURDERING).build();
        BehandlingLås lås = behandlingRepository.taSkriveLås(revurdering.getId());
        behandlingRepository.lagre(revurdering, lås);

        MottattDokument mottattDokument = new MottattDokument.Builder()
            .medFagsakId(revurdering.getFagsakId())
            .medBehandlingId(revurdering.getId())
            .medDokumentType(DokumentTypeId.SØKNAD_FORELDREPENGER_FØDSEL)
            .medMottattDato(LocalDate.now())
            .medDokumentKategori(DokumentKategori.SØKNAD)
            .build();
        mottatteDokumentRepository.lagre(mottattDokument);

        BehandlingskontrollKontekst kontekst = new BehandlingskontrollKontekst(fagsakId, aktørId, lås);
        BehandleStegResultat resultat = steg.utførSteg(kontekst);

        assertThat(resultat.getAksjonspunktListe()).containsExactly(AksjonspunktDefinisjon.REGISTRER_PAPIR_ENDRINGSØKNAD_FORELDREPENGER);
    }

    @Test
    public void opprett_registrer_papirsøknad_svangerskapspenger_hvis_fagsaktype_er_svp() {

        var aktørId = AktørId.dummy();
        var personinfo = new Personinfo.Builder()
            .medNavBrukerKjønn(NavBrukerKjønn.KVINNE)
            .medPersonIdent(PersonIdent.fra("123"))
            .medAktørId(aktørId)
            .medNavn("Navn Navnesen")
            .medFødselsdato(LocalDate.now().minusYears(30))
            .medForetrukketSpråk(Språkkode.NB)
            .build();
        Long fagsakId = fagsakRepository
            .opprettNy(new Fagsak(FagsakYtelseType.SVANGERSKAPSPENGER, NavBruker.opprettNy(personinfo), RelasjonsRolleType.MORA, new Saksnummer("124")));

        Fagsak fagsak = fagsakRepository.finnEksaktFagsak(fagsakId);
        Behandling forrigeBehandling = Behandling.forFørstegangssøknad(fagsak)
            .build();

        Behandling revurdering = Behandling.fraTidligereBehandling(forrigeBehandling, BehandlingType.REVURDERING).build();
        BehandlingLås lås = behandlingRepository.taSkriveLås(revurdering.getId());
        behandlingRepository.lagre(revurdering, lås);

        MottattDokument mottattDokument = new MottattDokument.Builder()
            .medFagsakId(revurdering.getFagsakId())
            .medBehandlingId(revurdering.getId())
            .medDokumentType(DokumentTypeId.SØKNAD_SVANGERSKAPSPENGER)
            .medMottattDato(LocalDate.now())
            .medDokumentKategori(DokumentKategori.SØKNAD)
            .build();
        mottatteDokumentRepository.lagre(mottattDokument);

        BehandlingskontrollKontekst kontekst = new BehandlingskontrollKontekst(fagsakId, aktørId, lås);
        BehandleStegResultat resultat = steg.utførSteg(kontekst);

        assertThat(resultat.getAksjonspunktListe()).containsExactly(AksjonspunktDefinisjon.REGISTRER_PAPIRSØKNAD_SVANGERSKAPSPENGER);
    }



}
