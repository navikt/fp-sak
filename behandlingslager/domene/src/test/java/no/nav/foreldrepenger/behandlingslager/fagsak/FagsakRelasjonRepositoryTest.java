package no.nav.foreldrepenger.behandlingslager.fagsak;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.time.LocalDate;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import no.nav.foreldrepenger.behandlingslager.aktør.NavBruker;
import no.nav.foreldrepenger.behandlingslager.behandling.Behandling;
import no.nav.foreldrepenger.behandlingslager.behandling.BehandlingType;
import no.nav.foreldrepenger.behandlingslager.behandling.personopplysning.RelasjonsRolleType;
import no.nav.foreldrepenger.behandlingslager.behandling.ytelsefordeling.YtelsesFordelingRepository;
import no.nav.foreldrepenger.dbstoette.EntityManagerAwareTest;
import no.nav.foreldrepenger.domene.typer.AktørId;
import no.nav.foreldrepenger.domene.typer.Saksnummer;
import no.nav.vedtak.exception.VLException;

public class FagsakRelasjonRepositoryTest extends EntityManagerAwareTest {

    private FagsakRepository fagsakRepository;
    private FagsakRelasjonRepository relasjonRepository;

    @BeforeEach
    void setUp() {
        var entityManager = getEntityManager();
        fagsakRepository = new FagsakRepository(entityManager);
        relasjonRepository = new FagsakRelasjonRepository(entityManager, new YtelsesFordelingRepository(entityManager),
            new FagsakLåsRepository(entityManager));
    }

    @Test
    public void skal_ikke_kunne_kobles_med_seg_selv() {
        final Fagsak fagsak = Fagsak.opprettNy(FagsakYtelseType.FORELDREPENGER, NavBruker.opprettNyNB(AktørId.dummy()));
        fagsakRepository.opprettNy(fagsak);
        relasjonRepository.opprettRelasjon(fagsak, Dekningsgrad._100);
        assertThrows(VLException.class, () -> relasjonRepository.kobleFagsaker(fagsak, fagsak, null));
    }

    @Test
    public void skal_ikke_kunne_kobles_med_fagsak_med_identisk_aktørid() {
        final NavBruker bruker = NavBruker.opprettNyNB(AktørId.dummy());
        final Fagsak fagsak = Fagsak.opprettNy(FagsakYtelseType.FORELDREPENGER, bruker);
        fagsakRepository.opprettNy(fagsak);
        final Fagsak fagsak2 = Fagsak.opprettNy(FagsakYtelseType.FORELDREPENGER, bruker);
        fagsakRepository.opprettNy(fagsak2);

        relasjonRepository.opprettRelasjon(fagsak, Dekningsgrad._100);
        assertThrows(VLException.class, () -> relasjonRepository.kobleFagsaker(fagsak, fagsak2, null));
    }

    @Test
    public void skal_ikke_kunne_kobles_med_fagsak_med_ulik_ytelse() {
        final Fagsak fagsak = Fagsak.opprettNy(FagsakYtelseType.FORELDREPENGER, NavBruker.opprettNyNB(AktørId.dummy()));
        fagsakRepository.opprettNy(fagsak);
        final Fagsak fagsak2 = Fagsak.opprettNy(FagsakYtelseType.ENGANGSTØNAD, NavBruker.opprettNyNB(AktørId.dummy()));
        fagsakRepository.opprettNy(fagsak2);

        relasjonRepository.opprettRelasjon(fagsak, Dekningsgrad._100);
        assertThrows(VLException.class, () -> relasjonRepository.kobleFagsaker(fagsak, fagsak2, null));
    }

    @Test
    public void skal_koble_sammen_fagsak_med_lik_ytelse_type_og_ulik_aktør() {
        final Fagsak fagsak = Fagsak.opprettNy(FagsakYtelseType.FORELDREPENGER, NavBruker.opprettNyNB(AktørId.dummy()));
        fagsakRepository.opprettNy(fagsak);
        final Fagsak fagsak2 = Fagsak.opprettNy(FagsakYtelseType.FORELDREPENGER, NavBruker.opprettNyNB(AktørId.dummy()));
        fagsakRepository.opprettNy(fagsak2);

        relasjonRepository.opprettRelasjon(fagsak, Dekningsgrad._100);
        relasjonRepository.kobleFagsaker(fagsak, fagsak2, null);
        final FagsakRelasjon fagsakRelasjon = relasjonRepository.finnRelasjonFor(fagsak);
        final FagsakRelasjon fagsakRelasjon1 = relasjonRepository.finnRelasjonFor(fagsak2);

        assertThat(fagsakRelasjon).isEqualTo(fagsakRelasjon1);
        assertThat(fagsakRelasjon.getFagsakNrEn()).isEqualTo(fagsak);
        assertThat(fagsakRelasjon.getFagsakNrTo()).hasValueSatisfying(fag -> assertThat(fag).isEqualTo(fagsak2));
    }

    @Test
    public void skal_lage_relasjon_når_mangler() {
        final Fagsak fagsak = Fagsak.opprettNy(FagsakYtelseType.FORELDREPENGER, NavBruker.opprettNyNB(AktørId.dummy()));
        fagsakRepository.opprettNy(fagsak);
        relasjonRepository.opprettEllerOppdaterRelasjon(fagsak, Optional.empty(), Dekningsgrad._100);
        final FagsakRelasjon fagsakRelasjon = relasjonRepository.finnRelasjonFor(fagsak);

        assertThat(fagsakRelasjon.getFagsakNrEn()).isEqualTo(fagsak);
        assertThat(fagsakRelasjon.getDekningsgrad()).isEqualTo(Dekningsgrad._100);
    }

    @Test
    public void skal_oppdatere_relasjon_når_1gang() {

        final Fagsak fagsak = Fagsak.opprettNy(FagsakYtelseType.FORELDREPENGER, NavBruker.opprettNyNB(AktørId.dummy()));
        fagsakRepository.opprettNy(fagsak);
        Behandling.nyBehandlingFor(fagsak, BehandlingType.FØRSTEGANGSSØKNAD).build();

        relasjonRepository.opprettEllerOppdaterRelasjon(fagsak, Optional.empty(), Dekningsgrad._80);

        FagsakRelasjon fagsakRelasjon = relasjonRepository.finnRelasjonFor(fagsak);
        assertThat(fagsakRelasjon.getFagsakNrEn()).isEqualTo(fagsak);
        assertThat(fagsakRelasjon.getDekningsgrad()).isEqualTo(Dekningsgrad._80);

        Behandling.nyBehandlingFor(fagsak, BehandlingType.FØRSTEGANGSSØKNAD).build();

        relasjonRepository.opprettEllerOppdaterRelasjon(fagsak, Optional.of(fagsakRelasjon), Dekningsgrad._100);

        fagsakRelasjon = relasjonRepository.finnRelasjonFor(fagsak);
        assertThat(fagsakRelasjon.getFagsakNrEn()).isEqualTo(fagsak);
        assertThat(fagsakRelasjon.getDekningsgrad()).isEqualTo(Dekningsgrad._100);
    }

    @Test
    public void skal_overstyre_dekningsgrad_eier_av_relasjon(){
        // Arrange
        final Fagsak fagsak1 = Fagsak.opprettNy(FagsakYtelseType.FORELDREPENGER, NavBruker.opprettNyNB(AktørId.dummy()));
        final Fagsak fagsak2 = Fagsak.opprettNy(FagsakYtelseType.FORELDREPENGER, NavBruker.opprettNyNB(AktørId.dummy()));
        fagsakRepository.opprettNy(fagsak1);
        fagsakRepository.opprettNy(fagsak2);
        relasjonRepository.opprettRelasjon(fagsak1, Dekningsgrad._80);
        relasjonRepository.kobleFagsaker(fagsak1, fagsak2, null);
        // Act
        relasjonRepository.overstyrDekningsgrad(fagsak1, Dekningsgrad._100);
        final FagsakRelasjon fagsakRelasjon1 = relasjonRepository.finnRelasjonFor(fagsak1);
        final FagsakRelasjon fagsakRelasjon2 = relasjonRepository.finnRelasjonFor(fagsak2);
        // Assert
        assertThat(fagsakRelasjon1).isEqualTo(fagsakRelasjon2);
        assertThat(fagsakRelasjon1.getFagsakNrEn()).isEqualTo(fagsak1);
        assertThat(fagsakRelasjon1.getDekningsgrad().getVerdi()).isEqualTo(80);
        assertThat(fagsakRelasjon1.getOverstyrtDekningsgrad().get().getVerdi()).isEqualTo(100);
        assertThat(fagsakRelasjon1.getFagsakNrTo()).hasValueSatisfying(fagsakOpt -> assertThat(fagsakOpt).isEqualTo(fagsak2));
        assertThat(fagsakRelasjon2.getFagsakNrEn()).isEqualTo(fagsak1);
        assertThat(fagsakRelasjon2.getDekningsgrad().getVerdi()).isEqualTo(80);
        assertThat(fagsakRelasjon1.getOverstyrtDekningsgrad().get().getVerdi()).isEqualTo(100);
        assertThat(fagsakRelasjon2.getFagsakNrTo()).hasValueSatisfying(fagsakOpt -> assertThat(fagsakOpt).isEqualTo(fagsak2));
    }

    @Test
    public void skal_overstyre_dekningsgrad_ikke_eier_av_relasjon(){
        // Arrange
        final Fagsak fagsak1 = Fagsak.opprettNy(FagsakYtelseType.FORELDREPENGER, NavBruker.opprettNyNB(AktørId.dummy()));
        final Fagsak fagsak2 = Fagsak.opprettNy(FagsakYtelseType.FORELDREPENGER, NavBruker.opprettNyNB(AktørId.dummy()));
        fagsakRepository.opprettNy(fagsak1);
        fagsakRepository.opprettNy(fagsak2);
        relasjonRepository.opprettRelasjon(fagsak1, Dekningsgrad._80);
        relasjonRepository.kobleFagsaker(fagsak1, fagsak2, null);
        // Act
        relasjonRepository.overstyrDekningsgrad(fagsak2, Dekningsgrad._100);
        final FagsakRelasjon fagsakRelasjon1 = relasjonRepository.finnRelasjonFor(fagsak1);
        final FagsakRelasjon fagsakRelasjon2 = relasjonRepository.finnRelasjonFor(fagsak2);
        // Assert
        assertThat(fagsakRelasjon1).isEqualTo(fagsakRelasjon2);
        assertThat(fagsakRelasjon1.getFagsakNrEn()).isEqualTo(fagsak1);
        assertThat(fagsakRelasjon1.getDekningsgrad().getVerdi()).isEqualTo(80);
        assertThat(fagsakRelasjon1.getOverstyrtDekningsgrad().get().getVerdi()).isEqualTo(100);
        assertThat(fagsakRelasjon1.getFagsakNrTo()).hasValueSatisfying(fagsakOpt -> assertThat(fagsakOpt).isEqualTo(fagsak2));
        assertThat(fagsakRelasjon2.getFagsakNrEn()).isEqualTo(fagsak1);
        assertThat(fagsakRelasjon2.getDekningsgrad().getVerdi()).isEqualTo(80);
        assertThat(fagsakRelasjon1.getOverstyrtDekningsgrad().get().getVerdi()).isEqualTo(100);
        assertThat(fagsakRelasjon2.getFagsakNrTo()).hasValueSatisfying(fagsakOpt -> assertThat(fagsakOpt).isEqualTo(fagsak2));
    }

    @Test
    public void skal_finne_relasjon_med_saksnummer(){
        // Arrange
        Saksnummer saksnummer = new Saksnummer("1337");
        final Fagsak fagsak = Fagsak.opprettNy(FagsakYtelseType.FORELDREPENGER, NavBruker.opprettNyNB(AktørId.dummy()), RelasjonsRolleType.MORA, saksnummer);
        fagsakRepository.opprettNy(fagsak);
        relasjonRepository.opprettRelasjon(fagsak, Dekningsgrad._80);

        // Act
        final FagsakRelasjon fagsakRelasjon = relasjonRepository.finnRelasjonFor(saksnummer);

        // Assert
        assertThat(fagsakRelasjon.getDekningsgrad().getVerdi()).isEqualTo(80);
        assertThat(fagsakRelasjon.getFagsakNrEn()).isEqualTo(fagsak);
    }

    @Test
    public void skal_oppdatere_med_avslutningsdato(){
        // Arrange
        final Fagsak fagsak1 = Fagsak.opprettNy(FagsakYtelseType.FORELDREPENGER, NavBruker.opprettNyNB(AktørId.dummy()));
        final Fagsak fagsak2 = Fagsak.opprettNy(FagsakYtelseType.FORELDREPENGER, NavBruker.opprettNyNB(AktørId.dummy()));
        fagsakRepository.opprettNy(fagsak1);
        fagsakRepository.opprettNy(fagsak2);
        relasjonRepository.opprettRelasjon(fagsak1, Dekningsgrad._100);
        relasjonRepository.kobleFagsaker(fagsak1, fagsak2, null);
        // Act
        var fagsakRealasjon = relasjonRepository.finnRelasjonFor(fagsak1);
        relasjonRepository.oppdaterMedAvsluttningsdato(fagsakRealasjon, LocalDate.now(), null, Optional.empty(), Optional.empty());
        final FagsakRelasjon fagsakRelasjon1 = relasjonRepository.finnRelasjonFor(fagsak1);
        final FagsakRelasjon fagsakRelasjon2 = relasjonRepository.finnRelasjonFor(fagsak2);
        // Assert
        assertThat(fagsakRelasjon1).isEqualTo(fagsakRelasjon2);
        assertThat(fagsakRelasjon1.getAvsluttningsdato()).isNotNull();
        assertThat(fagsakRelasjon2.getAvsluttningsdato()).isNotNull();
        assertThat(fagsakRelasjon1.getAvsluttningsdato()).isEqualTo(LocalDate.now());
        assertThat(fagsakRelasjon2.getAvsluttningsdato()).isEqualTo(LocalDate.now());
    }

}
