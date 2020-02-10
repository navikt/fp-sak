package no.nav.foreldrepenger.domene.uttak.fastsetteperioder.validering;

import static no.nav.foreldrepenger.regler.uttak.felles.grunnlag.Stønadskontotype.FEDREKVOTE;
import static no.nav.foreldrepenger.regler.uttak.felles.grunnlag.Stønadskontotype.FELLESPERIODE;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import org.junit.Test;

import no.nav.foreldrepenger.regler.uttak.fastsetteperiode.saldo.SaldoUtregning;

public class SaldoValideringTest {

    @Test
    public void annenpart_ikke_tapende_skal_ikke_godkjennes_hvis_negativ_saldo_og_ikke_nok_dager_å_frigi() {
        SaldoUtregning saldoUtregning = mock(SaldoUtregning.class);
        when(saldoUtregning.negativSaldo(FELLESPERIODE)).thenReturn(true);
        when(saldoUtregning.nokDagerÅFrigiPåAnnenpart(FELLESPERIODE)).thenReturn(false);
        SaldoValidering validering = new SaldoValidering(saldoUtregning, true, false);

        assertThat(validering.valider(FELLESPERIODE).isGyldig()).isFalse();
    }

    @Test
    public void annenpart_ikke_tapende_skal_godkjennes_hvis_negativ_saldo_og_nok_dager_å_frigi() {
        SaldoUtregning saldoUtregning = mock(SaldoUtregning.class);
        when(saldoUtregning.negativSaldo(FELLESPERIODE)).thenReturn(true);
        when(saldoUtregning.nokDagerÅFrigiPåAnnenpart(FELLESPERIODE)).thenReturn(true);
        SaldoValidering validering = new SaldoValidering(saldoUtregning, true, false);

        assertThat(validering.valider(FELLESPERIODE).isGyldig()).isTrue();
    }

    @Test
    public void tapende_skal_godkjennes_hvis_ikke_negativ_saldo() {
        SaldoUtregning saldoUtregning = mock(SaldoUtregning.class);
        when(saldoUtregning.negativSaldo(FELLESPERIODE)).thenReturn(false);
        SaldoValidering validering = new SaldoValidering(saldoUtregning, true, true);

        assertThat(validering.valider(FELLESPERIODE).isGyldig()).isTrue();
    }

    @Test
    public void tapende_skal_ikke_godkjennes_hvis_negativ_saldo() {
        SaldoUtregning saldoUtregning = mock(SaldoUtregning.class);
        when(saldoUtregning.negativSaldo(FELLESPERIODE)).thenReturn(true);
        SaldoValidering validering = new SaldoValidering(saldoUtregning, true, true);

        assertThat(validering.valider(FELLESPERIODE).isGyldig()).isFalse();
    }

    @Test
    public void ikke_annenpart_skal_ikke_godkjennes_hvis_negativ_saldo() {
        SaldoUtregning saldoUtregning = mock(SaldoUtregning.class);
        when(saldoUtregning.negativSaldo(FELLESPERIODE)).thenReturn(true);
        SaldoValidering validering = new SaldoValidering(saldoUtregning, false, false);

        assertThat(validering.valider(FELLESPERIODE).isGyldig()).isFalse();
    }

    @Test
    public void ikke_annenpart_skal_godkjennes_hvis_ikke_negativ_saldo() {
        SaldoUtregning saldoUtregning = mock(SaldoUtregning.class);
        when(saldoUtregning.negativSaldo(FELLESPERIODE)).thenReturn(false);
        SaldoValidering validering = new SaldoValidering(saldoUtregning, false, false);

        assertThat(validering.valider(FELLESPERIODE).isGyldig()).isTrue();
    }

    @Test
    public void skal_kunne_gå_negativ_hvis_samtidig_uttak_ikke_tapende() {
        SaldoUtregning saldoUtregning = mock(SaldoUtregning.class);
        when(saldoUtregning.negativSaldo(FELLESPERIODE)).thenReturn(true);
        when(saldoUtregning.søktSamtidigUttak(FELLESPERIODE)).thenReturn(true);
        SaldoValidering validering = new SaldoValidering(saldoUtregning, true, false);

        assertThat(validering.valider(FELLESPERIODE).isGyldig()).isTrue();
        assertThat(validering.valider(FELLESPERIODE).isNegativPgaSamtidigUttak()).isTrue();
    }

    @Test
    public void skal_ikke_kunne_gå_negativ_hvis_samtidig_på_annen_kontotype() {
        SaldoUtregning saldoUtregning = mock(SaldoUtregning.class);
        when(saldoUtregning.negativSaldo(FEDREKVOTE)).thenReturn(true);
        when(saldoUtregning.søktSamtidigUttak(FELLESPERIODE)).thenReturn(true);
        SaldoValidering validering = new SaldoValidering(saldoUtregning, true, true);

        assertThat(validering.valider(FEDREKVOTE).isGyldig()).isFalse();
        assertThat(validering.valider(FEDREKVOTE).isNegativPgaSamtidigUttak()).isFalse();
    }

    @Test
    public void skal_ikke_kunne_gå_negativ_hvis_samtidig_uttak_tapende() {
        SaldoUtregning saldoUtregning = mock(SaldoUtregning.class);
        when(saldoUtregning.negativSaldo(FELLESPERIODE)).thenReturn(true);
        when(saldoUtregning.søktSamtidigUttak(FELLESPERIODE)).thenReturn(true);
        SaldoValidering validering = new SaldoValidering(saldoUtregning, true, true);

        assertThat(validering.valider(FELLESPERIODE).isGyldig()).isFalse();
        assertThat(validering.valider(FELLESPERIODE).isNegativPgaSamtidigUttak()).isFalse();
    }
}

