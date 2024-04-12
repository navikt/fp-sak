package no.nav.foreldrepenger.behandlingslager.behandling.aktivitetskrav;


import static org.assertj.core.api.Assertions.assertThat;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import no.nav.foreldrepenger.behandlingslager.behandling.BasicBehandlingBuilder;
import no.nav.foreldrepenger.behandlingslager.fagsak.FagsakYtelseType;
import no.nav.foreldrepenger.dbstoette.EntityManagerAwareTest;
import no.nav.foreldrepenger.domene.tid.DatoIntervallEntitet;

class AktivitetskravArbeidRepositoryTest extends EntityManagerAwareTest {
    private AktivitetskravArbeidRepository repository;
    private BasicBehandlingBuilder basicBehandlingBuilder;
    private final String ORG_NR = "55555555";
    private final LocalDate FRA = LocalDate.now();
    private final LocalDate TIL = FRA.plusWeeks(2);
    private final BigDecimal STILLINGSPROSENT = BigDecimal.valueOf(85);
    @BeforeEach
    void before() {
        var entityManager = getEntityManager();
        basicBehandlingBuilder = new BasicBehandlingBuilder(entityManager);
        repository = new AktivitetskravArbeidRepository(entityManager);
    }

    @Test
    void skal_returnere_empty_ved_manglende_grunnlag() {
        assertThat(repository.hentGrunnlag(999L)).isEmpty();
    }

    @Test
    void skal_lagre_og_finne_grunnlag() {
        var fagsak = basicBehandlingBuilder.opprettFagsak(FagsakYtelseType.FORELDREPENGER);
        var behandling = basicBehandlingBuilder.opprettOgLagreFørstegangssøknad(fagsak);
        var perioder = List.of(lagAktvitetskravArbeidPeriode(FRA, TIL));
        var aktvitetskravPerioder = lagPerioderBUilder(perioder);
        repository.lagreAktivitetskravArbeidPerioder(behandling.getId(), aktvitetskravPerioder);

        var hentet = repository.hentGrunnlag(behandling.getId()).orElseThrow();

        assertThat(hentet.getAktivitetskravPerioderMedArbeidEnitet().map(AktivitetskravArbeidPerioderEntitet::getAktivitetskravArbeidPeriodeListe).orElse(List.of())).hasSize(1);
        assertThat(hentet.getAktivitetskravPerioderMedArbeidEnitet().get().getAktivitetskravArbeidPeriodeListe().getFirst().getPeriode()).isEqualTo(DatoIntervallEntitet.fraOgMedTilOgMed(FRA, TIL));
        assertThat(hentet.getAktivitetskravPerioderMedArbeidEnitet().get().getAktivitetskravArbeidPeriodeListe().getFirst().getOrganisasjonsnummer()).isEqualTo(ORG_NR);
        assertThat(hentet.getAktivitetskravPerioderMedArbeidEnitet().get().getAktivitetskravArbeidPeriodeListe().getFirst().getSumStillingsprosent()).isEqualTo(STILLINGSPROSENT);
        assertThat(hentet.getAktivitetskravPerioderMedArbeidEnitet().get().getAktivitetskravArbeidPeriodeListe().getFirst().getSumPermisjonsprosent()).isEqualTo(BigDecimal.ZERO);
    }

    @Test
    void skal_oppdatere_eksisterende_med_nytt_grunnlag() {
        var fagsak = basicBehandlingBuilder.opprettFagsak(FagsakYtelseType.FORELDREPENGER);
        var behandling = basicBehandlingBuilder.opprettOgLagreFørstegangssøknad(fagsak);
        var perioderBuilder = lagAktvitetskravArbeidPeriode(FRA, TIL);
        var aktvitetskravPerioder = lagPerioderBUilder(List.of(perioderBuilder));
        repository.lagreAktivitetskravArbeidPerioder(behandling.getId(), aktvitetskravPerioder);

        var nyFra = FRA.plusWeeks(1);
        var nyTil = TIL.plusWeeks(1);
        var periodeBuilder2 = lagAktvitetskravArbeidPeriode(nyFra, nyTil);

        var aktivitetskravPerioder2 = lagPerioderBUilder(List.of(perioderBuilder, periodeBuilder2));
        repository.lagreAktivitetskravArbeidPerioder(behandling.getId(), aktivitetskravPerioder2);

        var hentet = repository.hentGrunnlag(behandling.getId()).orElseThrow();

        assertThat(hentet.getAktivitetskravPerioderMedArbeidEnitet().map(AktivitetskravArbeidPerioderEntitet::getAktivitetskravArbeidPeriodeListe).orElse(List.of())).hasSize(2);

        assertThat(hentet.getAktivitetskravPerioderMedArbeidEnitet().get().getAktivitetskravArbeidPeriodeListe().getFirst().getPeriode()).isEqualTo(DatoIntervallEntitet.fraOgMedTilOgMed(FRA, TIL));
        assertThat(hentet.getAktivitetskravPerioderMedArbeidEnitet().get().getAktivitetskravArbeidPeriodeListe().getFirst().getOrganisasjonsnummer()).isEqualTo(ORG_NR);
        assertThat(hentet.getAktivitetskravPerioderMedArbeidEnitet().get().getAktivitetskravArbeidPeriodeListe().getFirst().getSumStillingsprosent()).isEqualTo(STILLINGSPROSENT);
        assertThat(hentet.getAktivitetskravPerioderMedArbeidEnitet().get().getAktivitetskravArbeidPeriodeListe().getFirst().getSumPermisjonsprosent()).isEqualTo(BigDecimal.ZERO);

        assertThat(hentet.getAktivitetskravPerioderMedArbeidEnitet().get().getAktivitetskravArbeidPeriodeListe().get(1).getPeriode()).isEqualTo(DatoIntervallEntitet.fraOgMedTilOgMed(nyFra, nyTil));
        assertThat(hentet.getAktivitetskravPerioderMedArbeidEnitet().get().getAktivitetskravArbeidPeriodeListe().get(1).getOrganisasjonsnummer()).isEqualTo(ORG_NR);
        assertThat(hentet.getAktivitetskravPerioderMedArbeidEnitet().get().getAktivitetskravArbeidPeriodeListe().get(1).getSumStillingsprosent()).isEqualTo(STILLINGSPROSENT);
        assertThat(hentet.getAktivitetskravPerioderMedArbeidEnitet().get().getAktivitetskravArbeidPeriodeListe().get(1).getSumPermisjonsprosent()).isEqualTo(BigDecimal.ZERO);
    }

    private AktivitetskravArbeidPerioderEntitet.Builder lagPerioderBUilder(List<AktivitetskravArbeidPeriodeEntitet.Builder> perioder) {
        var builder = new AktivitetskravArbeidPerioderEntitet.Builder();
        perioder.forEach(builder::leggTil);
        return builder;
    }

    private AktivitetskravArbeidPeriodeEntitet.Builder lagAktvitetskravArbeidPeriode(LocalDate fra, LocalDate til) {
        return new AktivitetskravArbeidPeriodeEntitet.Builder()
            .medPeriode(fra, til)
            .medOrganisasjonsnummer(ORG_NR)
            .medSumPermisjonsprosent(BigDecimal.ZERO)
            .medSumStillingsprosent(STILLINGSPROSENT);
    }
}
