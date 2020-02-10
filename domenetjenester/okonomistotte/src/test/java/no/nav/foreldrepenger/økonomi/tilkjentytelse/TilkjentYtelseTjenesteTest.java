package no.nav.foreldrepenger.økonomi.tilkjentytelse;

import static org.assertj.core.api.Assertions.assertThat;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Collection;

import javax.inject.Inject;

import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;

import no.nav.foreldrepenger.behandlingslager.behandling.Behandling;
import no.nav.foreldrepenger.behandlingslager.behandling.BehandlingResultatType;
import no.nav.foreldrepenger.behandlingslager.behandling.Behandlingsresultat;
import no.nav.foreldrepenger.behandlingslager.behandling.beregning.BeregningsresultatEntitet;
import no.nav.foreldrepenger.behandlingslager.behandling.beregning.BeregningsresultatRepository;
import no.nav.foreldrepenger.behandlingslager.behandling.repository.BehandlingLås;
import no.nav.foreldrepenger.behandlingslager.behandling.repository.BehandlingRepository;
import no.nav.foreldrepenger.behandlingslager.behandling.repository.BehandlingRepositoryProvider;
import no.nav.foreldrepenger.behandlingslager.behandling.vedtak.BehandlingVedtak;
import no.nav.foreldrepenger.behandlingslager.behandling.vedtak.BehandlingVedtakRepository;
import no.nav.foreldrepenger.behandlingslager.behandling.vedtak.VedtakResultatType;
import no.nav.foreldrepenger.dbstoette.UnittestRepositoryRule;
import no.nav.foreldrepenger.behandlingslager.testutilities.behandling.ScenarioMorSøkerForeldrepenger;
import no.nav.foreldrepenger.kontrakter.tilkjentytelse.TilkjentYtelse;
import no.nav.foreldrepenger.kontrakter.tilkjentytelse.v1.TilkjentYtelseAndelV1;
import no.nav.foreldrepenger.kontrakter.tilkjentytelse.v1.TilkjentYtelseBehandlingInfoV1;
import no.nav.foreldrepenger.kontrakter.tilkjentytelse.v1.TilkjentYtelsePeriodeV1;
import no.nav.foreldrepenger.kontrakter.tilkjentytelse.v1.TilkjentYtelseV1;
import no.nav.foreldrepenger.økonomi.tilkjentytelse.TilkjentYtelseTjeneste;
import no.nav.vedtak.felles.testutilities.cdi.CdiRunner;

@RunWith(CdiRunner.class)
public class TilkjentYtelseTjenesteTest {

    @Rule
    public final UnittestRepositoryRule repoRule = new UnittestRepositoryRule();

    @Inject
    TilkjentYtelseTjeneste tjeneste;
    @Inject
    BehandlingRepositoryProvider behandlingRepositoryProvider;
    @Inject
    BehandlingRepository behandlingRepository;
    @Inject
    BeregningsresultatRepository beregningsresultatRepository;
    @Inject
    BehandlingVedtakRepository behandlingVedtakRepository;

    @Test
    public void skal_hente_beregningsresultat_og_mappe_om() {
        //arrange
        Behandling behandling = ScenarioMorSøkerForeldrepenger.forFødsel().lagre(behandlingRepositoryProvider);
        BeregningsresultatEntitet beregningsresultat = MapperForTilkjentYtelseTest.lagTilkjentYtelseTilBruker();
        lagreResultat(behandling, beregningsresultat);

        //act
        TilkjentYtelse resultat = tjeneste.hentilkjentYtelse(behandling.getId());

        //assert

        assertThat(resultat).isInstanceOf(TilkjentYtelseV1.class);
        TilkjentYtelseV1 ty = (TilkjentYtelseV1) resultat;

        assertThat(ty.getErOpphør()).isFalse();
        assertThat(ty.getErOpphørEtterSkjæringstidspunkt()).isNull();

        Collection<TilkjentYtelsePeriodeV1> typerioder = ty.getPerioder();
        assertThat(typerioder).hasSize(1);
        TilkjentYtelsePeriodeV1 periode = typerioder.iterator().next();
        assertThat(periode.getFom()).isEqualTo(LocalDate.of(2018, 3, 1));
        assertThat(periode.getTom()).isEqualTo(LocalDate.of(2018, 3, 31));
        assertThat(periode.getAndeler()).hasSize(1);
        TilkjentYtelseAndelV1 andel = periode.getAndeler().iterator().next();
        assertThat(andel.getUtbetalesTilBruker()).isTrue();
        assertThat(andel.getSatsBeløp()).isEqualTo(1000);
        assertThat(andel.getSatsType()).isEqualTo(TilkjentYtelseV1.SatsType.DAGSATS);
        assertThat(andel.getUtbetalingsgrad()).isEqualTo(BigDecimal.valueOf(100));
    }

    @Test
    public void skal_mappe_til_tomt_tilkjent_ytelse_perioder_hvis_TY_ikke_finnes() {
        //arrange
        Behandling behandling = ScenarioMorSøkerForeldrepenger.forFødsel().lagre(behandlingRepositoryProvider);
        lagreResultat(behandling, null);

        //act
        TilkjentYtelse resultat = tjeneste.hentilkjentYtelse(behandling.getId());

        //assert
        assertThat(resultat).isInstanceOf(TilkjentYtelseV1.class);
        TilkjentYtelseV1 ty = (TilkjentYtelseV1) resultat;

        assertThat(ty.getErOpphør()).isFalse();
        assertThat(ty.getErOpphørEtterSkjæringstidspunkt()).isNull();

        Collection<TilkjentYtelsePeriodeV1> typerioder = ty.getPerioder();
        assertThat(typerioder).isEmpty();

        TilkjentYtelseBehandlingInfoV1 behandlingInfoV1 = ty.getBehandingsinfo();
        assertThat(behandlingInfoV1.getBehandlingId()).isEqualTo(behandling.getId());
        assertThat(behandlingInfoV1.getSaksnummer()).isEqualTo(behandling.getFagsak().getSaksnummer().getVerdi());
    }

    private void lagreResultat(Behandling behandling, BeregningsresultatEntitet beregningsresultat) {
        Behandlingsresultat behandlingsresultat = Behandlingsresultat.builderForBeregningResultat()
            .medBehandlingResultatType(BehandlingResultatType.INNVILGET)
            .buildFor(behandling);

        repoRule.getRepository().lagre(behandlingsresultat.getBeregningResultat());
        repoRule.getRepository().lagre(behandlingsresultat);

        BehandlingLås lås = new BehandlingLås(behandling.getId());
        behandlingRepository.lagre(behandling, lås);
        if (beregningsresultat != null) {
            beregningsresultatRepository.lagre(behandling, beregningsresultat);
        }
        behandlingVedtakRepository.lagre(BehandlingVedtak.builder()
            .medAnsvarligSaksbehandler("Z111111")
            .medVedtakstidspunkt(LocalDate.of(2018, 3, 1).atStartOfDay())
            .medVedtakResultatType(VedtakResultatType.INNVILGET)
            .medBehandlingsresultat(behandlingsresultat)
            .build(), lås);
    }
}
