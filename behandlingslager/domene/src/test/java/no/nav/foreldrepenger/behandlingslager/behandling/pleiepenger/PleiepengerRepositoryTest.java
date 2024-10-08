package no.nav.foreldrepenger.behandlingslager.behandling.pleiepenger;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.LocalDate;
import java.util.Arrays;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import no.nav.foreldrepenger.behandlingslager.behandling.BasicBehandlingBuilder;
import no.nav.foreldrepenger.behandlingslager.fagsak.FagsakYtelseType;
import no.nav.foreldrepenger.dbstoette.EntityManagerAwareTest;
import no.nav.foreldrepenger.domene.tid.DatoIntervallEntitet;
import no.nav.foreldrepenger.domene.typer.AktørId;
import no.nav.foreldrepenger.domene.typer.Saksnummer;

class PleiepengerRepositoryTest extends EntityManagerAwareTest {

    private static final LocalDate I_GÅR = LocalDate.now().minusDays(1);
    private static final AktørId PLEIETRENGENDE = AktørId.dummy();
    private static final Saksnummer PSB_SAK = new Saksnummer("Dummy");

    private BasicBehandlingBuilder basicBehandlingBuilder;
    private PleiepengerRepository repository;

    @BeforeEach
    void before() {
        var entityManager = getEntityManager();
        basicBehandlingBuilder = new BasicBehandlingBuilder(entityManager);
        repository = new PleiepengerRepository(entityManager);
    }

    @Test
    void skal_returnere_empty_ved_manglende_grunnlag() {
        assertThat(repository.hentGrunnlag(999L)).isEmpty();
    }

    @Test
    void skal_lagre_og_finne_grunnlag() {
        var fagsak = basicBehandlingBuilder.opprettFagsak(FagsakYtelseType.FORELDREPENGER);
        var behandling = basicBehandlingBuilder.opprettOgLagreFørstegangssøknad(fagsak);
        var perioder = lagInnleggelsePerioder(I_GÅR);
        repository.lagrePerioder(behandling.getId(), perioder);

        var hentet = repository.hentGrunnlag(behandling.getId()).orElseThrow();

        assertThat(hentet.getPerioderMedInnleggelse().map(PleiepengerPerioderEntitet::getInnleggelser).orElse(List.of())).hasSize(1);
        assertThat(hentet.getPerioderMedInnleggelse().get().getInnleggelser().get(0).getPleietrengendeAktørId()).isEqualTo(PLEIETRENGENDE);
    }

    @Test
    void skal_beholde_grunnlag_hvis_uendret() {
        var fagsak = basicBehandlingBuilder.opprettFagsak(FagsakYtelseType.FORELDREPENGER);
        var behandling = basicBehandlingBuilder.opprettOgLagreFørstegangssøknad(fagsak);
        var perioder = lagInnleggelsePerioder(I_GÅR);
        repository.lagrePerioder(behandling.getId(), perioder);

        var hentet = repository.hentGrunnlag(behandling.getId()).orElseThrow();

        var periodeBuilder2 = lagInnleggelsePerioder(I_GÅR);
        repository.lagrePerioder(behandling.getId(), periodeBuilder2);

        var hentet2 = repository.hentGrunnlag(behandling.getId()).orElseThrow();

        assertThat(hentet2.getId()).isEqualTo(hentet.getId());
    }

    @Test
    void skal_lagre_nytt_grunnlag_hvis_endret() {
        var fagsak = basicBehandlingBuilder.opprettFagsak(FagsakYtelseType.FORELDREPENGER);
        var behandling = basicBehandlingBuilder.opprettOgLagreFørstegangssøknad(fagsak);
        var perioder = lagInnleggelsePerioder(I_GÅR);
        repository.lagrePerioder(behandling.getId(), perioder);

        var hentet = repository.hentGrunnlag(behandling.getId()).orElseThrow();

        var periodeBuilder2 = lagInnleggelsePerioder(I_GÅR, I_GÅR.plusWeeks(2));
        repository.lagrePerioder(behandling.getId(), periodeBuilder2);

        var hentet2 = repository.hentGrunnlag(behandling.getId()).orElseThrow();

        assertThat(hentet2.getId()).isNotEqualTo(hentet.getId());
        assertThat(hentet2.getBehandlingId()).isEqualTo(hentet.getBehandlingId());
        assertThat(hentet2.getPerioderMedInnleggelse().map(PleiepengerPerioderEntitet::getInnleggelser).orElse(List.of())).hasSize(2);
    }

    @Test
    void skal_kopiere_grunnlaget_og_sette_kopiert_flagget() {
        // Arrange
        var fagsak = basicBehandlingBuilder.opprettFagsak(FagsakYtelseType.FORELDREPENGER);
        var gammelBehandling = basicBehandlingBuilder.opprettOgLagreFørstegangssøknad(fagsak);
        var nyBehandling = basicBehandlingBuilder.opprettOgLagreFørstegangssøknad(fagsak);

        var perioder =  lagInnleggelsePerioder(I_GÅR);
        repository.lagrePerioder(gammelBehandling.getId(), perioder);
        var hentet = repository.hentGrunnlag(gammelBehandling.getId()).orElseThrow();

        // Act
        repository.kopierGrunnlagFraEksisterendeBehandling(gammelBehandling.getId(), nyBehandling.getId());

        // Assert
        var kopiertGrunnlag = repository.hentGrunnlag(nyBehandling.getId()).orElseThrow();
        assertThat(kopiertGrunnlag.getId()).isNotEqualTo(hentet.getId());
        assertThat(kopiertGrunnlag.getPerioderMedInnleggelse()).isEqualTo(hentet.getPerioderMedInnleggelse());
        assertThat(kopiertGrunnlag.getBehandlingId()).isEqualTo(nyBehandling.getId());
        assertThat(kopiertGrunnlag.getPerioderMedInnleggelse().map(PleiepengerPerioderEntitet::getInnleggelser).orElse(List.of())).hasSize(1);
        assertThat(kopiertGrunnlag.getPerioderMedInnleggelse().get().getInnleggelser().get(0).getPleietrengendeAktørId()).isEqualTo(PLEIETRENGENDE);
    }

    private PleiepengerPerioderEntitet.Builder lagInnleggelsePerioder(LocalDate... datoer) {
        var builder = new PleiepengerPerioderEntitet.Builder();
        Arrays.stream(datoer)
            .map(d -> new PleiepengerInnleggelseEntitet.Builder()
                .medPleietrengendeAktørId(PLEIETRENGENDE)
                .medPeriode(DatoIntervallEntitet.fraOgMedTilOgMed(d, d.plusWeeks(2).minusDays(1)))
                .medPleiepengerSaksnummer(PSB_SAK))
            .forEach(builder::leggTil);
        return builder;
    }

}
