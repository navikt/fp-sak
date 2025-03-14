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
    private final String ORG_NR = no.nav.foreldrepenger.behandlingslager.virksomhet.OrgNummer.KUNSTIG_ORG;
    private final LocalDate FRA = LocalDate.now();
    private final LocalDate TIL = FRA.plusWeeks(2);
    private final BigDecimal STILLINGSPROSENT = BigDecimal.valueOf(85);
    private AktivitetskravArbeidRepository repository;
    private BasicBehandlingBuilder basicBehandlingBuilder;

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
        var grunnlagFraDato = FRA.minusWeeks(2);
        var grunnlagTilDato = TIL.plusWeeks(2);
        var perioder = List.of(lagAktvitetskravArbeidPeriode(FRA, TIL, STILLINGSPROSENT));
        var aktvitetskravPerioder = lagPerioderBUilder(perioder);
        repository.lagreAktivitetskravArbeidPerioder(behandling.getId(), aktvitetskravPerioder, grunnlagFraDato, grunnlagTilDato);

        var lagretGrunnlag = repository.hentGrunnlag(behandling.getId()).orElseThrow();

        assertThat(lagretGrunnlag.getAktivitetskravPerioderMedArbeidEnitet()
            .map(AktivitetskravArbeidPerioderEntitet::getAktivitetskravArbeidPeriodeListe)
            .orElse(List.of())).hasSize(1);
        assertThat(lagretGrunnlag.getPeriode().getFomDato()).isEqualTo(grunnlagFraDato);
        assertThat(lagretGrunnlag.getPeriode().getTomDato()).isEqualTo(grunnlagTilDato);
        assertThat(
            lagretGrunnlag.getAktivitetskravPerioderMedArbeidEnitet().get().getAktivitetskravArbeidPeriodeListe().getFirst().getPeriode()).isEqualTo(
            DatoIntervallEntitet.fraOgMedTilOgMed(FRA, TIL));
        assertThat(lagretGrunnlag.getAktivitetskravPerioderMedArbeidEnitet()
            .get()
            .getAktivitetskravArbeidPeriodeListe()
            .getFirst()
            .getOrgNummer()
            .getId()).isEqualTo(ORG_NR);
        assertThat(lagretGrunnlag.getAktivitetskravPerioderMedArbeidEnitet()
            .get()
            .getAktivitetskravArbeidPeriodeListe()
            .getFirst()
            .getSumStillingsprosent()
            .getVerdi()).isEqualTo(STILLINGSPROSENT);
        assertThat(lagretGrunnlag.getAktivitetskravPerioderMedArbeidEnitet()
            .get()
            .getAktivitetskravArbeidPeriodeListe()
            .getFirst()
            .getSumPermisjonsprosent()
            .getVerdi()).isEqualTo(BigDecimal.ZERO);
    }

    @Test
    void skal_oppdatere_eksisterende_med_nytt_grunnlag() {
        var fagsak = basicBehandlingBuilder.opprettFagsak(FagsakYtelseType.FORELDREPENGER);
        var behandling = basicBehandlingBuilder.opprettOgLagreFørstegangssøknad(fagsak);
        var perioderBuilder = lagAktvitetskravArbeidPeriode(FRA, TIL, STILLINGSPROSENT);
        var aktvitetskravPerioder = lagPerioderBUilder(List.of(perioderBuilder));
        repository.lagreAktivitetskravArbeidPerioder(behandling.getId(), aktvitetskravPerioder, FRA, TIL);
        var stillingsprosentDesimaler = BigDecimal.valueOf(74.9853124843);

        var nyFra = FRA.plusWeeks(1);
        var nyTil = TIL.plusWeeks(1);
        var periodeBuilder2 = lagAktvitetskravArbeidPeriode(nyFra, nyTil, stillingsprosentDesimaler);

        var aktivitetskravPerioder2 = lagPerioderBUilder(List.of(perioderBuilder, periodeBuilder2));
        repository.lagreAktivitetskravArbeidPerioder(behandling.getId(), aktivitetskravPerioder2, FRA, TIL.plusWeeks(2));

        var oppdatertGrunnlag = repository.hentGrunnlag(behandling.getId()).orElseThrow();

        assertThat(oppdatertGrunnlag.getAktivitetskravPerioderMedArbeidEnitet()
            .map(AktivitetskravArbeidPerioderEntitet::getAktivitetskravArbeidPeriodeListe)
            .orElse(List.of())).hasSize(2);

        assertThat(oppdatertGrunnlag.getPeriode().getFomDato()).isEqualTo(FRA);
        assertThat(oppdatertGrunnlag.getPeriode().getTomDato()).isEqualTo(TIL.plusWeeks(2));

        assertThat(oppdatertGrunnlag.getAktivitetskravPerioderMedArbeidEnitet()
            .get()
            .getAktivitetskravArbeidPeriodeListe()
            .getFirst()
            .getPeriode()).isEqualTo(DatoIntervallEntitet.fraOgMedTilOgMed(FRA, TIL));
        assertThat(oppdatertGrunnlag.getAktivitetskravPerioderMedArbeidEnitet()
            .get()
            .getAktivitetskravArbeidPeriodeListe()
            .getFirst()
            .getOrgNummer()
            .getId()).isEqualTo(ORG_NR);
        assertThat(oppdatertGrunnlag.getAktivitetskravPerioderMedArbeidEnitet()
            .get()
            .getAktivitetskravArbeidPeriodeListe()
            .getFirst()
            .getSumStillingsprosent()
            .getVerdi()).isEqualTo(STILLINGSPROSENT);
        assertThat(oppdatertGrunnlag.getAktivitetskravPerioderMedArbeidEnitet()
            .get()
            .getAktivitetskravArbeidPeriodeListe()
            .getFirst()
            .getSumPermisjonsprosent()
            .getVerdi()).isEqualTo(BigDecimal.ZERO);

        assertThat(
            oppdatertGrunnlag.getAktivitetskravPerioderMedArbeidEnitet().get().getAktivitetskravArbeidPeriodeListe().get(1).getPeriode()).isEqualTo(
            DatoIntervallEntitet.fraOgMedTilOgMed(nyFra, nyTil));
        assertThat(oppdatertGrunnlag.getAktivitetskravPerioderMedArbeidEnitet()
            .get()
            .getAktivitetskravArbeidPeriodeListe()
            .get(1)
            .getOrgNummer()
            .getId()).isEqualTo(ORG_NR);
        assertThat(oppdatertGrunnlag.getAktivitetskravPerioderMedArbeidEnitet()
            .get()
            .getAktivitetskravArbeidPeriodeListe()
            .get(1)
            .getSumStillingsprosent()
            .getVerdi()).isEqualTo(stillingsprosentDesimaler);
        assertThat(oppdatertGrunnlag.getAktivitetskravPerioderMedArbeidEnitet()
            .get()
            .getAktivitetskravArbeidPeriodeListe()
            .get(1)
            .getSumPermisjonsprosent()
            .getVerdi()).isEqualTo(BigDecimal.ZERO);
    }

    @Test
    void skal_oppdatere_grunnlag_hvis_endret_innhentingsperiode() {
        var fagsak = basicBehandlingBuilder.opprettFagsak(FagsakYtelseType.FORELDREPENGER);
        var behandling = basicBehandlingBuilder.opprettOgLagreFørstegangssøknad(fagsak);
        var perioderBuilder = lagAktvitetskravArbeidPeriode(FRA, TIL, STILLINGSPROSENT);
        var aktvitetskravPerioder = lagPerioderBUilder(List.of(perioderBuilder));
        repository.lagreAktivitetskravArbeidPerioder(behandling.getId(), aktvitetskravPerioder, FRA, TIL);


        repository.lagreAktivitetskravArbeidPerioder(behandling.getId(), aktvitetskravPerioder, FRA, TIL.plusWeeks(2));

        var oppdatertGrunnlag = repository.hentGrunnlag(behandling.getId()).orElseThrow();

        assertThat(oppdatertGrunnlag.getPeriode().getFomDato()).isEqualTo(FRA);
        assertThat(oppdatertGrunnlag.getPeriode().getTomDato()).isEqualTo(TIL.plusWeeks(2));

        assertThat(oppdatertGrunnlag.getAktivitetskravPerioderMedArbeidEnitet()
            .map(AktivitetskravArbeidPerioderEntitet::getAktivitetskravArbeidPeriodeListe)
            .orElse(List.of())).hasSize(1);
        assertThat(oppdatertGrunnlag.getAktivitetskravPerioderMedArbeidEnitet()
            .get()
            .getAktivitetskravArbeidPeriodeListe()
            .getFirst()
            .getPeriode()).isEqualTo(DatoIntervallEntitet.fraOgMedTilOgMed(FRA, TIL));
        assertThat(oppdatertGrunnlag.getAktivitetskravPerioderMedArbeidEnitet()
            .get()
            .getAktivitetskravArbeidPeriodeListe()
            .getFirst()
            .getOrgNummer()
            .getId()).isEqualTo(ORG_NR);
        assertThat(oppdatertGrunnlag.getAktivitetskravPerioderMedArbeidEnitet()
            .get()
            .getAktivitetskravArbeidPeriodeListe()
            .getFirst()
            .getSumStillingsprosent()
            .getVerdi()).isEqualTo(STILLINGSPROSENT);
        assertThat(oppdatertGrunnlag.getAktivitetskravPerioderMedArbeidEnitet()
            .get()
            .getAktivitetskravArbeidPeriodeListe()
            .getFirst()
            .getSumPermisjonsprosent()
            .getVerdi()).isEqualTo(BigDecimal.ZERO);
    }

    private AktivitetskravArbeidPerioderEntitet lagPerioderBUilder(List<AktivitetskravArbeidPeriodeEntitet.Builder> perioder) {
        var builder = new AktivitetskravArbeidPerioderEntitet.Builder();
        perioder.forEach(builder::leggTil);
        return builder.build();
    }

    private AktivitetskravArbeidPeriodeEntitet.Builder lagAktvitetskravArbeidPeriode(LocalDate fra, LocalDate til, BigDecimal stillingsprosent) {
        return new AktivitetskravArbeidPeriodeEntitet.Builder().medPeriode(fra, til)
            .medOrgNummer(ORG_NR)
            .medPermisjon(BigDecimal.ZERO, AktivitetskravPermisjonType.UDEFINERT)
            .medSumStillingsprosent(stillingsprosent);
    }
}
