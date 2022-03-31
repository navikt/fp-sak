package no.nav.foreldrepenger.domene.uttak.fastsetteperioder.validering;

import static no.nav.foreldrepenger.regler.uttak.felles.grunnlag.Stønadskontotype.FEDREKVOTE;
import static no.nav.foreldrepenger.regler.uttak.felles.grunnlag.Stønadskontotype.FELLESPERIODE;
import static no.nav.foreldrepenger.regler.uttak.felles.grunnlag.Stønadskontotype.FORELDREPENGER;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.List;

import org.junit.jupiter.api.Test;

import no.nav.foreldrepenger.regler.uttak.fastsetteperiode.Trekkdager;
import no.nav.foreldrepenger.regler.uttak.fastsetteperiode.saldo.SaldoUtregning;
import no.nav.vedtak.exception.TekniskException;

public class SaldoValideringTest {

    @Test
    public void annenpart_ikke_berørt_skal_ikke_godkjennes_hvis_negativ_saldo_og_ikke_nok_dager_å_frigi() {
        var saldoUtregning = mock(SaldoUtregning.class);
        when(saldoUtregning.negativSaldo(FELLESPERIODE)).thenReturn(true);
        when(saldoUtregning.nokDagerÅFrigiPåAnnenpart(FELLESPERIODE)).thenReturn(false);
        var validering = new SaldoValidering(saldoUtregning, true, false);

        assertThat(validering.valider(FELLESPERIODE).isGyldig()).isFalse();
    }

    @Test
    public void annenpart_ikke_berørt_skal_godkjennes_hvis_negativ_saldo_og_nok_dager_å_frigi() {
        var saldoUtregning = mock(SaldoUtregning.class);
        when(saldoUtregning.negativSaldo(FELLESPERIODE)).thenReturn(true);
        when(saldoUtregning.nokDagerÅFrigiPåAnnenpart(FELLESPERIODE)).thenReturn(true);
        var validering = new SaldoValidering(saldoUtregning, true, false);

        assertThat(validering.valider(FELLESPERIODE).isGyldig()).isTrue();
    }

    @Test
    public void berørt_skal_godkjennes_hvis_ikke_negativ_saldo() {
        var saldoUtregning = mock(SaldoUtregning.class);
        when(saldoUtregning.negativSaldo(FELLESPERIODE)).thenReturn(false);
        when(saldoUtregning.restSaldoDagerUtenAktivitetskrav()).thenReturn(Trekkdager.ZERO);
        var validering = new SaldoValidering(saldoUtregning, true, true);

        assertThat(validering.valider(FELLESPERIODE).isGyldig()).isTrue();
    }

    @Test
    public void berørt_skal_ikke_godkjennes_hvis_negativ_saldo() {
        var saldoUtregning = mock(SaldoUtregning.class);
        when(saldoUtregning.negativSaldo(FELLESPERIODE)).thenReturn(true);
        var validering = new SaldoValidering(saldoUtregning, true, true);

        assertThat(validering.valider(FELLESPERIODE).isGyldig()).isFalse();
    }

    @Test
    public void ikke_annenpart_skal_ikke_godkjennes_hvis_negativ_saldo() {
        var saldoUtregning = mock(SaldoUtregning.class);
        when(saldoUtregning.negativSaldo(FELLESPERIODE)).thenReturn(true);
        var validering = new SaldoValidering(saldoUtregning, false, false);

        assertThat(validering.valider(FELLESPERIODE).isGyldig()).isFalse();
    }

    @Test
    public void ikke_annenpart_skal_godkjennes_hvis_ikke_negativ_saldo() {
        var saldoUtregning = mock(SaldoUtregning.class);
        when(saldoUtregning.negativSaldo(FELLESPERIODE)).thenReturn(false);
        when(saldoUtregning.restSaldoDagerUtenAktivitetskrav()).thenReturn(Trekkdager.ZERO);
        var validering = new SaldoValidering(saldoUtregning, false, false);

        assertThat(validering.valider(FELLESPERIODE).isGyldig()).isTrue();
    }

    @Test
    public void skal_kunne_gå_negativ_hvis_samtidig_uttak_ikke_berørt() {
        var saldoUtregning = mock(SaldoUtregning.class);
        when(saldoUtregning.negativSaldo(FELLESPERIODE)).thenReturn(true);
        when(saldoUtregning.søktSamtidigUttak(FELLESPERIODE)).thenReturn(true);
        var validering = new SaldoValidering(saldoUtregning, true, false);

        assertThat(validering.valider(FELLESPERIODE).isGyldig()).isTrue();
        assertThat(validering.valider(FELLESPERIODE).isNegativPgaSamtidigUttak()).isTrue();
    }

    @Test
    public void skal_ikke_kunne_gå_negativ_hvis_samtidig_på_annen_kontotype() {
        var saldoUtregning = mock(SaldoUtregning.class);
        when(saldoUtregning.negativSaldo(FEDREKVOTE)).thenReturn(true);
        when(saldoUtregning.søktSamtidigUttak(FELLESPERIODE)).thenReturn(true);
        var validering = new SaldoValidering(saldoUtregning, true, true);

        assertThat(validering.valider(FEDREKVOTE).isGyldig()).isFalse();
        assertThat(validering.valider(FEDREKVOTE).isNegativPgaSamtidigUttak()).isFalse();
    }

    @Test
    public void skal_ikke_kunne_gå_negativ_hvis_samtidig_uttak_berørt() {
        var saldoUtregning = mock(SaldoUtregning.class);
        when(saldoUtregning.negativSaldo(FELLESPERIODE)).thenReturn(true);
        when(saldoUtregning.søktSamtidigUttak(FELLESPERIODE)).thenReturn(true);
        var validering = new SaldoValidering(saldoUtregning, true, true);

        assertThat(validering.valider(FELLESPERIODE).isGyldig()).isFalse();
        assertThat(validering.valider(FELLESPERIODE).isNegativPgaSamtidigUttak()).isFalse();
    }

    @Test
    public void ikke_kunne_gå_negativ_på_dager_uten_aktivitetskrav() {
        var saldoUtregning = mock(SaldoUtregning.class);
        when(saldoUtregning.negativSaldo(FORELDREPENGER)).thenReturn(false);
        when(saldoUtregning.getMaxDagerUtenAktivitetskrav()).thenReturn(new Trekkdager(10));
        when(saldoUtregning.getMaxDagerMinsterett()).thenReturn(Trekkdager.ZERO);
        when(saldoUtregning.restSaldoDagerUtenAktivitetskrav()).thenReturn(new Trekkdager(-5));
        var validering = new SaldoValidering(saldoUtregning, false, false);

        assertThat(validering.valider(FORELDREPENGER).isGyldig()).isTrue();
        assertThatExceptionOfType(TekniskException.class).isThrownBy(() -> validering.utfør(List.of()));
    }

    @Test
    public void ikke_kunne_gå_negativ_på_minsterett() {
        var saldoUtregning = mock(SaldoUtregning.class);
        when(saldoUtregning.negativSaldo(FORELDREPENGER)).thenReturn(false);
        when(saldoUtregning.getMaxDagerUtenAktivitetskrav()).thenReturn(Trekkdager.ZERO);
        when(saldoUtregning.getMaxDagerMinsterett()).thenReturn(new Trekkdager(10));
        when(saldoUtregning.restSaldoMinsterett()).thenReturn(new Trekkdager(-5));
        var validering = new SaldoValidering(saldoUtregning, false, false);

        assertThat(validering.valider(FORELDREPENGER).isGyldig()).isTrue();
        assertThatExceptionOfType(TekniskException.class).isThrownBy(() -> validering.utfør(List.of()));
    }
}

