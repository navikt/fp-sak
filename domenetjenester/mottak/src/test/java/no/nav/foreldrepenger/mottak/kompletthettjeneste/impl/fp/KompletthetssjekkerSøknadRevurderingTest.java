package no.nav.foreldrepenger.mottak.kompletthettjeneste.impl.fp;

import static java.util.Collections.singleton;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.LocalDate;
import java.time.Period;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import no.nav.foreldrepenger.behandling.BehandlingReferanse;
import no.nav.foreldrepenger.behandlingslager.behandling.Behandling;
import no.nav.foreldrepenger.behandlingslager.behandling.DokumentTypeId;
import no.nav.foreldrepenger.behandlingslager.behandling.MottattDokument;
import no.nav.foreldrepenger.behandlingslager.behandling.repository.BehandlingRepositoryProvider;
import no.nav.foreldrepenger.behandlingslager.behandling.søknad.SøknadVedleggEntitet;
import no.nav.foreldrepenger.behandlingslager.behandling.ytelsefordeling.årsak.UtsettelseÅrsak;
import no.nav.foreldrepenger.dbstoette.EntityManagerAwareTest;
import no.nav.foreldrepenger.dokumentarkiv.DokumentArkivTjeneste;
import no.nav.foreldrepenger.domene.typer.Saksnummer;
import no.nav.foreldrepenger.mottak.kompletthettjeneste.impl.KompletthetssjekkerTestUtil;

class KompletthetssjekkerSøknadRevurderingTest extends EntityManagerAwareTest {

    private static final String TERMINBEKREFTELSE = "I000041";
    private static final String DOK_INNLEGGELSE = "I000037";

    private BehandlingRepositoryProvider repositoryProvider;

    private KompletthetssjekkerTestUtil testUtil;

    private final DokumentArkivTjeneste dokumentArkivTjeneste = mock(DokumentArkivTjeneste.class);
    private KompletthetssjekkerSøknadRevurderingImpl kompletthetssjekker;

    @BeforeEach
    void setUp() {
        repositoryProvider = new BehandlingRepositoryProvider(getEntityManager());
        testUtil = new KompletthetssjekkerTestUtil(repositoryProvider);
        kompletthetssjekker = new KompletthetssjekkerSøknadRevurderingImpl(dokumentArkivTjeneste, repositoryProvider, Period.parse("P4W"));
    }

    @Test
    void skal_utlede_at_et_påkrevd_vedlegg_finnes_i_journal() {
        // Arrange
        var scenario = testUtil.opprettRevurderingsscenarioForMor();
        scenario.medSøknad()
            .medElektroniskRegistrert(true)
            .medSøknadsdato(LocalDate.now())
            .leggTilVedlegg(new SøknadVedleggEntitet.Builder().medSkjemanummer(TERMINBEKREFTELSE).medErPåkrevdISøknadsdialog(true).build())
            .build();
        var behandling = scenario.lagre(repositoryProvider);

        // Matcher med søknad:
        var dokumentListe = singleton(DokumentTypeId.DOKUMENTASJON_AV_TERMIN_ELLER_FØDSEL);
        when(dokumentArkivTjeneste.hentDokumentTypeIdForSak(any(Saksnummer.class), any())).thenReturn(dokumentListe);

        // Act
        var manglendeVedlegg = kompletthetssjekker.utledManglendeVedleggForSøknad(lagRef(behandling));

        // Assert
        assertThat(manglendeVedlegg).isEmpty();
    }

    @Test
    void skal_utlede_at_et_påkrevd_vedlegg_ikke_finnes_i_journal() {
        // Arrange
        var scenario = testUtil.opprettRevurderingsscenarioForMor();
        scenario.medSøknad()
            .medElektroniskRegistrert(true)
            .medSøknadsdato(LocalDate.now())
            .leggTilVedlegg(new SøknadVedleggEntitet.Builder().medSkjemanummer(TERMINBEKREFTELSE).medErPåkrevdISøknadsdialog(true).build())
            .build();
        var behandling = scenario.lagre(repositoryProvider);

        // Matcher ikke med søknad:
        var dokumentListe = singleton(DokumentTypeId.LEGEERKLÆRING);
        when(dokumentArkivTjeneste.hentDokumentTypeIdForSak(any(Saksnummer.class), any())).thenReturn(dokumentListe);

        // Act
        var manglendeVedlegg = kompletthetssjekker.utledManglendeVedleggForSøknad(lagRef(behandling));

        // Assert
        assertThat(manglendeVedlegg).hasSize(1);
        assertThat(manglendeVedlegg.get(0).getDokumentType().getOffisiellKode()).isEqualTo(TERMINBEKREFTELSE);
    }

    @Test
    void skal_utlede_at_et_påkrevd_vedlegg_ikke_finnes_i_journal_når_det_ble_mottatt_før_gjeldende_vedtak() {
        // Arrange
        var scenario = testUtil.opprettRevurderingsscenarioForMor();
        scenario.medSøknad()
            .medElektroniskRegistrert(true)
            .medSøknadsdato(LocalDate.now())
            .leggTilVedlegg(new SøknadVedleggEntitet.Builder().medSkjemanummer(TERMINBEKREFTELSE).medErPåkrevdISøknadsdialog(true).build())
            .build();
        var revurdering = scenario.lagre(repositoryProvider);

        // Matcher med søknad, men er mottatt ifbm førstegangsbehandlingen:
        Set<DokumentTypeId> dokumentListe = new HashSet<>();
        dokumentListe.add(DokumentTypeId.LEGEERKLÆRING);
        when(dokumentArkivTjeneste.hentDokumentTypeIdForSak(any(Saksnummer.class), any())).thenReturn(dokumentListe);

        // Act
        var manglendeVedlegg = kompletthetssjekker.utledManglendeVedleggForSøknad(lagRef(revurdering));

        // Assert
        assertThat(manglendeVedlegg).hasSize(1);
        assertThat(manglendeVedlegg.get(0).getDokumentType().getOffisiellKode()).isEqualTo(TERMINBEKREFTELSE);
    }

    @Test
    void skal_utlede_at_et_påkrevd_vedlegg_som_finnes_i_mottatte_dokumenter_ikke_mangler_selv_om_vedlegget_fra_journal_har_mottatt_dato_null() {
        // Arrange
        var scenario = testUtil.opprettRevurderingsscenarioForMor();
        scenario.medSøknad()
            .medElektroniskRegistrert(true)
            .medSøknadsdato(LocalDate.now())
            .leggTilVedlegg(new SøknadVedleggEntitet.Builder().medSkjemanummer(TERMINBEKREFTELSE).medErPåkrevdISøknadsdialog(true).build())
            .build();
        var revurdering = scenario.lagre(repositoryProvider);

        // Matcher med søknad, men mangler mottatt dato:
        var dokumentListe = singleton(DokumentTypeId.DOKUMENTASJON_AV_TERMIN_ELLER_FØDSEL);
        when(dokumentArkivTjeneste.hentDokumentTypeIdForSak(any(Saksnummer.class), any())).thenReturn(dokumentListe);

        var mottattDokument = new MottattDokument.Builder()
            .medFagsakId(revurdering.getFagsakId())
            .medBehandlingId(revurdering.getId())
            .medDokumentType(DokumentTypeId.DOKUMENTASJON_AV_TERMIN_ELLER_FØDSEL)
            .medMottattDato(LocalDate.now())
            .build();
        repositoryProvider.getMottatteDokumentRepository().lagre(mottattDokument);

        // Act
        var manglendeVedlegg = kompletthetssjekker.utledManglendeVedleggForSøknad(lagRef(revurdering));

        // Assert
        assertThat(manglendeVedlegg).isEmpty();
    }

    @Test
    void skal_utlede_at_et_påkrevd_vedlegg_som_ikke_finnes_i_mottatte_dokumenter_mangler_når_vedlegget_fra_journal_har_mottatt_dato_null() {
        // Arrange
        var scenario = testUtil.opprettRevurderingsscenarioForMor();
        scenario.medSøknad()
            .medElektroniskRegistrert(true)
            .medSøknadsdato(LocalDate.now())
            .leggTilVedlegg(new SøknadVedleggEntitet.Builder().medSkjemanummer(TERMINBEKREFTELSE).medErPåkrevdISøknadsdialog(true).build())
            .build();
        var revurdering = scenario.lagre(repositoryProvider);

        // Matcher med søknad, men mangler mottatt dato:
        when(dokumentArkivTjeneste.hentDokumentTypeIdForSak(any(Saksnummer.class), any())).thenReturn(Collections.emptySet());

        // Act
        var manglendeVedlegg = kompletthetssjekker.utledManglendeVedleggForSøknad(lagRef(revurdering));

        // Assert
        assertThat(manglendeVedlegg).hasSize(1);
        assertThat(manglendeVedlegg.get(0).getDokumentType().getOffisiellKode()).isEqualTo(TERMINBEKREFTELSE);
    }

    @Test
    void skal_utlede_at_et_dokument_som_er_påkrevd_som_følger_av_utsettelse_ikke_finnes_i_journal() {
        // Arrange
        var scenario = testUtil.opprettRevurderingsscenarioForMor();
        var behandling = scenario.lagre(repositoryProvider);
        testUtil.byggOppgittFordelingMedUtsettelse(behandling, UtsettelseÅrsak.INSTITUSJON_SØKER);
        testUtil.byggOgLagreSøknadMedEksisterendeOppgittFordeling(behandling, false);

        // Matcher ikke med utsettelse:
        var dokumentListe = singleton(DokumentTypeId.LEGEERKLÆRING);
        when(dokumentArkivTjeneste.hentDokumentTypeIdForSak(any(Saksnummer.class), any())).thenReturn(dokumentListe);

        // Act
        var manglendeVedlegg = kompletthetssjekker.utledManglendeVedleggForSøknad(lagRef(behandling));

        // Assert
        assertThat(manglendeVedlegg).hasSize(1);
        assertThat(manglendeVedlegg.get(0).getDokumentType().getOffisiellKode()).isEqualTo(DOK_INNLEGGELSE);
    }

    @Test
    void skal_utlede_at_et_dokument_som_er_påkrevd_som_følger_av_utsettelse_finnes_i_journal() {
        // Arrange
        var scenario = testUtil.opprettRevurderingsscenarioForMor();
        var behandling = scenario.lagre(repositoryProvider);
        testUtil.byggOppgittFordelingMedUtsettelse(behandling, UtsettelseÅrsak.INSTITUSJON_SØKER);
        testUtil.byggOgLagreSøknadMedEksisterendeOppgittFordeling(behandling, false);

        // Matcher med utsettelse:
        var dokumentListe = singleton(DokumentTypeId.DOK_INNLEGGELSE);
        when(dokumentArkivTjeneste.hentDokumentTypeIdForSak(any(Saksnummer.class), any())).thenReturn(dokumentListe);

        // Act
        var manglendeVedlegg = kompletthetssjekker.utledManglendeVedleggForSøknad(lagRef(behandling));

        // Assert
        assertThat(manglendeVedlegg).isEmpty();
    }

    @Test
    void skal_utlede_at_et_dokument_som_er_påkrevd_finnes_ved_vedtak_samme_dag() {
        // Arrange
        var søknadsDato = LocalDate.now().minusWeeks(2);
        var scenario = testUtil.opprettRevurderingsscenarioForMor();
        var behandling = scenario.lagre(repositoryProvider);
        testUtil.byggOppgittFordelingMedUtsettelse(behandling, UtsettelseÅrsak.INSTITUSJON_SØKER);
        testUtil.byggOgLagreSøknadMedEksisterendeOppgittFordeling(behandling, false, søknadsDato);

        // Matcher med utsettelse:
        var dokumentListe = singleton(DokumentTypeId.DOK_INNLEGGELSE);
        when(dokumentArkivTjeneste.hentDokumentTypeIdForSak(eq(behandling.getSaksnummer()), eq(søknadsDato))).thenReturn(dokumentListe);

        // Act
        var manglendeVedlegg = kompletthetssjekker.utledManglendeVedleggForSøknad(lagRef(behandling));

        // Assert
        assertThat(manglendeVedlegg).isEmpty();
        verify(dokumentArkivTjeneste).hentDokumentTypeIdForSak(eq(behandling.getSaksnummer()), eq(søknadsDato));
    }

    private BehandlingReferanse lagRef(Behandling behandling) {
        return BehandlingReferanse.fra(behandling);
    }

}
