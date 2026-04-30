package no.nav.foreldrepenger.web.app.tjenester.mellomlagring;

import static no.nav.foreldrepenger.behandlingslager.behandling.dokument.MellomlagringType.INNHENT_OPPLYSNINGER;
import static no.nav.foreldrepenger.behandlingslager.behandling.dokument.MellomlagringType.VARSEL_REVURDERING;
import static no.nav.foreldrepenger.behandlingslager.behandling.dokument.MellomlagringType.VEDTAKSBREV;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;

import jakarta.inject.Inject;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;

import no.nav.foreldrepenger.behandlingslager.behandling.Behandling;
import no.nav.foreldrepenger.behandlingslager.behandling.dokument.MellomlagringRepository;
import no.nav.foreldrepenger.behandlingslager.behandling.repository.BehandlingRepository;
import no.nav.foreldrepenger.behandlingslager.behandling.repository.BehandlingRepositoryProvider;
import no.nav.foreldrepenger.behandlingslager.testutilities.behandling.ScenarioMorSøkerForeldrepenger;
import no.nav.foreldrepenger.dbstoette.CdiDbAwareTest;
import no.nav.foreldrepenger.dokumentbestiller.DokumentBehandlingTjeneste;
import no.nav.foreldrepenger.web.app.tjenester.behandling.dto.UuidDto;

@CdiDbAwareTest
class MellomlagringRestTjenesteTest {

    @Inject
    private BehandlingRepositoryProvider repositoryProvider;
    @Inject
    private BehandlingRepository behandlingRepository;
    @Inject
    private MellomlagringRepository mellomlagringRepository;
    @Mock
    private DokumentBehandlingTjeneste dokumentBehandlingTjeneste;

    private MellomlagringRestTjeneste tjeneste;
    private Behandling behandling;

    @BeforeEach
    void setUp() {
        behandling = ScenarioMorSøkerForeldrepenger.forFødsel().lagre(repositoryProvider);
        tjeneste = new MellomlagringRestTjeneste(mellomlagringRepository, behandlingRepository, dokumentBehandlingTjeneste);
    }

    // --- GET: hentMellomlagring ---

    @Test
    void hent_returnerer_innhold_når_det_finnes() {
        mellomlagringRepository.lagreEllerOppdater(behandling.getId(), VARSEL_REVURDERING, "<p>Mellomlagret varsel</p>");

        var respons = tjeneste.hentMellomlagring(new UuidDto(behandling.getUuid()), VARSEL_REVURDERING, null);

        assertThat(respons.getStatus()).isEqualTo(200);
        var dto = (MellomlagringRestTjeneste.MellomlagringResultatDto) respons.getEntity();
        assertThat(dto.innhold()).isEqualTo("<p>Mellomlagret varsel</p>");
    }

    @Test
    void hent_returnerer_204_når_innhold_ikke_finnes() {
        var respons = tjeneste.hentMellomlagring(new UuidDto(behandling.getUuid()), VARSEL_REVURDERING, null);

        assertThat(respons.getStatus()).isEqualTo(204);
        assertThat(respons.getEntity()).isNull();
    }

    @Test
    void hent_med_dokumentMal_resolver_type() {
        mellomlagringRepository.lagreEllerOppdater(behandling.getId(), INNHENT_OPPLYSNINGER, "<p>Innhent opplysninger</p>");

        var respons = tjeneste.hentMellomlagring(new UuidDto(behandling.getUuid()), null, "INNOPP");

        assertThat(respons.getStatus()).isEqualTo(200);
        var dto = (MellomlagringRestTjeneste.MellomlagringResultatDto) respons.getEntity();
        assertThat(dto.innhold()).isEqualTo("<p>Innhent opplysninger</p>");
    }

    @Test
    void hent_uten_type_og_dokumentMal_gir_vedtaksbrev() {
        mellomlagringRepository.lagreEllerOppdater(behandling.getId(), VEDTAKSBREV, "<p>Vedtaksbrev</p>");

        var respons = tjeneste.hentMellomlagring(new UuidDto(behandling.getUuid()), null, null);

        assertThat(respons.getStatus()).isEqualTo(200);
        var dto = (MellomlagringRestTjeneste.MellomlagringResultatDto) respons.getEntity();
        assertThat(dto.innhold()).isEqualTo("<p>Vedtaksbrev</p>");
    }

    // --- POST: lagreMellomlagring ---

    @Test
    void lagre_med_innhold_persisterer_til_database() {
        var dto = new MellomlagringRestTjeneste.MellomlagringDto(behandling.getUuid(), VARSEL_REVURDERING, null, "<p>Nytt varsel</p>");

        var respons = tjeneste.lagreMellomlagring(dto);

        assertThat(respons.getStatus()).isEqualTo(200);
        var lagret = mellomlagringRepository.hentMellomlagring(behandling.getId(), VARSEL_REVURDERING);
        assertThat(lagret).isPresent();
        assertThat(lagret.get().getInnhold()).isEqualTo("<p>Nytt varsel</p>");
    }

    @Test
    void lagre_oppdaterer_eksisterende_innhold() {
        mellomlagringRepository.lagreEllerOppdater(behandling.getId(), VARSEL_REVURDERING, "<p>Gammel</p>");

        var dto = new MellomlagringRestTjeneste.MellomlagringDto(behandling.getUuid(), VARSEL_REVURDERING, null, "<p>Ny</p>");
        tjeneste.lagreMellomlagring(dto);

        var lagret = mellomlagringRepository.hentMellomlagring(behandling.getId(), VARSEL_REVURDERING);
        assertThat(lagret).isPresent();
        assertThat(lagret.get().getInnhold()).isEqualTo("<p>Ny</p>");
    }

    @Test
    void lagre_med_null_innhold_sletter_fra_database() {
        mellomlagringRepository.lagreEllerOppdater(behandling.getId(), VARSEL_REVURDERING, "<p>Varsel</p>");

        var dto = new MellomlagringRestTjeneste.MellomlagringDto(behandling.getUuid(), VARSEL_REVURDERING, null, null);
        tjeneste.lagreMellomlagring(dto);

        assertThat(mellomlagringRepository.hentMellomlagring(behandling.getId(), VARSEL_REVURDERING)).isEmpty();
    }

    @Test
    void lagre_med_dokumentMal_VARREV_resolver_til_VARSEL_REVURDERING() {
        var dto = new MellomlagringRestTjeneste.MellomlagringDto(behandling.getUuid(), null, "VARREV", "<p>Varsel</p>");

        tjeneste.lagreMellomlagring(dto);

        var lagret = mellomlagringRepository.hentMellomlagring(behandling.getId(), VARSEL_REVURDERING);
        assertThat(lagret).isPresent();
        assertThat(lagret.get().getInnhold()).isEqualTo("<p>Varsel</p>");
    }

    @Test
    void lagre_med_dokumentMal_INNOPP_resolver_til_INNHENT_OPPLYSNINGER() {
        var dto = new MellomlagringRestTjeneste.MellomlagringDto(behandling.getUuid(), null, "INNOPP", "<p>Innhent</p>");

        tjeneste.lagreMellomlagring(dto);

        var lagret = mellomlagringRepository.hentMellomlagring(behandling.getId(), INNHENT_OPPLYSNINGER);
        assertThat(lagret).isPresent();
        assertThat(lagret.get().getInnhold()).isEqualTo("<p>Innhent</p>");
    }

    @Test
    void lagre_uten_type_og_dokumentMal_delegerer_til_dokumentBehandlingTjeneste() {
        var dto = new MellomlagringRestTjeneste.MellomlagringDto(behandling.getUuid(), null, null, "<p>Vedtak</p>");

        tjeneste.lagreMellomlagring(dto);

        verify(dokumentBehandlingTjeneste).lagreOverstyrtBrev(behandling, "<p>Vedtak</p>");
    }

    @Test
    void lagre_vedtaksbrev_med_null_innhold_fjerner_overstyring() {
        var dto = new MellomlagringRestTjeneste.MellomlagringDto(behandling.getUuid(), null, null, null);

        tjeneste.lagreMellomlagring(dto);

        verify(dokumentBehandlingTjeneste).fjernOverstyringAvBrev(behandling);
    }

    // --- resolveType ---

    @Test
    void type_parameter_har_prioritet_over_dokumentMal() {
        var dto = new MellomlagringRestTjeneste.MellomlagringDto(behandling.getUuid(), VARSEL_REVURDERING, "INNOPP", "<p>Innhold</p>");

        tjeneste.lagreMellomlagring(dto);

        assertThat(mellomlagringRepository.hentMellomlagring(behandling.getId(), VARSEL_REVURDERING)).isPresent();
        assertThat(mellomlagringRepository.hentMellomlagring(behandling.getId(), INNHENT_OPPLYSNINGER)).isEmpty();
    }
}
