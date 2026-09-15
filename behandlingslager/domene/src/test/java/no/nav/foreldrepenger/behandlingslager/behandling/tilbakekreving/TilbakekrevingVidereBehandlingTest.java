package no.nav.foreldrepenger.behandlingslager.behandling.tilbakekreving;

import static no.nav.foreldrepenger.behandlingslager.behandling.tilbakekreving.TilbakekrevingVidereBehandling.navnForHistorikk;
import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class TilbakekrevingVidereBehandlingTest {

    @Test
    void skal_gi_navn_med_varsel_når_varseltekst_er_satt() {
        var navn = navnForHistorikk(TilbakekrevingVidereBehandling.OPPRETT_TILBAKEKREVING, "Du har fått for mye utbetalt");

        assertThat(navn).isEqualTo("Opprett tilbakekreving, send varsel");
    }

    @Test
    void skal_gi_navn_uten_varsel_når_varseltekst_mangler() {
        var navn = navnForHistorikk(TilbakekrevingVidereBehandling.OPPRETT_TILBAKEKREVING, null);

        assertThat(navn).isEqualTo("Opprett tilbakekreving, ikke send varsel");
    }

    @Test
    void skal_gi_navn_uten_varsel_når_varseltekst_kun_er_blanke_tegn() {
        var navn = navnForHistorikk(TilbakekrevingVidereBehandling.OPPRETT_TILBAKEKREVING, "   ");

        assertThat(navn).isEqualTo("Opprett tilbakekreving, ikke send varsel");
    }

    @Test
    void skal_bruke_kodeverdiens_navn_for_andre_valg_enn_opprett_tilbakekreving() {
        assertThat(navnForHistorikk(TilbakekrevingVidereBehandling.INNTREKK, null)).isEqualTo(
            TilbakekrevingVidereBehandling.INNTREKK.getNavn());
        assertThat(navnForHistorikk(TilbakekrevingVidereBehandling.IGNORER_TILBAKEKREVING, null)).isEqualTo(
            TilbakekrevingVidereBehandling.IGNORER_TILBAKEKREVING.getNavn());
        assertThat(navnForHistorikk(TilbakekrevingVidereBehandling.TILBAKEKR_OPPDATER, null)).isEqualTo(
            TilbakekrevingVidereBehandling.TILBAKEKR_OPPDATER.getNavn());
    }

    @Test
    void skal_ikke_påvirkes_av_varseltekst_for_andre_valg_enn_opprett_tilbakekreving() {
        var navn = navnForHistorikk(TilbakekrevingVidereBehandling.INNTREKK, "En varseltekst");

        assertThat(navn).isEqualTo(TilbakekrevingVidereBehandling.INNTREKK.getNavn());
    }

    @Test
    void skal_gi_null_når_videre_behandling_mangler() {
        assertThat(navnForHistorikk(null, "En varseltekst")).isNull();
        assertThat(navnForHistorikk(null, null)).isNull();
    }
}
