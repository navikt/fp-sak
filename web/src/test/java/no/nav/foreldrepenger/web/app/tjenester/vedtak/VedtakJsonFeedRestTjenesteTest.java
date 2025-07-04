package no.nav.foreldrepenger.web.app.tjenester.vedtak;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

import java.util.ArrayList;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import no.nav.foreldrepenger.domene.typer.AktørId;
import no.nav.foreldrepenger.jsonfeed.VedtakFattetTjeneste;
import no.nav.foreldrepenger.jsonfeed.dto.VedtakDto;
import no.nav.foreldrepenger.kontrakter.feed.vedtak.v1.Meldingstype;
import no.nav.foreldrepenger.web.app.tjenester.vedtak.vedtakfattet.VedtakJsonFeedRestTjeneste;
import no.nav.foreldrepenger.web.app.tjenester.vedtak.vedtakfattet.dto.AktørParam;
import no.nav.foreldrepenger.web.app.tjenester.vedtak.vedtakfattet.dto.HendelseTypeParam;
import no.nav.foreldrepenger.web.app.tjenester.vedtak.vedtakfattet.dto.MaxAntallParam;
import no.nav.foreldrepenger.web.app.tjenester.vedtak.vedtakfattet.dto.SekvensIdParam;

@ExtendWith(MockitoExtension.class)
class VedtakJsonFeedRestTjenesteTest {

    private static final AktørId AKTØR_ID = AktørId.dummy();
    private VedtakJsonFeedRestTjeneste tjeneste;
    @Mock
    private VedtakFattetTjeneste vedtakFattetTjeneste;

    @BeforeEach
    void setUp() {
        tjeneste = new VedtakJsonFeedRestTjeneste(vedtakFattetTjeneste);
    }

    @Test
    void skal_delegere_til_hent_fp_vedtak_tjeneste() {
        var sisteLestSekvensIdParam = new SekvensIdParam("1");
        var maxAntallParam = new MaxAntallParam("100");
        var hendelseTypeParam = new HendelseTypeParam(Meldingstype.FORELDREPENGER_ENDRET.getType());
        var aktørParam = new AktørParam(AKTØR_ID.getId());
        when(vedtakFattetTjeneste.hentFpVedtak(1L, 100L, "ForeldrepengerEndret_v1", Optional.of(AKTØR_ID)))
                .thenReturn(new VedtakDto(true, new ArrayList<>()));

        var feed = tjeneste.fpVedtakHendelser(sisteLestSekvensIdParam, maxAntallParam, hendelseTypeParam, aktørParam);
        assertThat(feed.getTittel()).isEqualTo("ForeldrepengerVedtak_v1");
        assertThat(feed.getInneholderFlereElementer()).isTrue();
        assertThat(feed.getElementer()).isEqualTo(new ArrayList<>());
    }

    @Test
    void skal_delegere_til_hent_svp_vedtak_tjeneste() {
        var sisteLestSekvensIdParam = new SekvensIdParam("1");
        var maxAntallParam = new MaxAntallParam("100");
        var hendelseTypeParam = new HendelseTypeParam(Meldingstype.SVANGERSKAPSPENGER_ENDRET.getType());
        var aktørParam = new AktørParam(AKTØR_ID.getId());
        when(vedtakFattetTjeneste.hentSvpVedtak(1L, 100L, "SVPEndret_v1", Optional.of(AKTØR_ID))).thenReturn(new VedtakDto(true, new ArrayList<>()));

        var feed = tjeneste.svpVedtakHendelser(sisteLestSekvensIdParam, maxAntallParam, hendelseTypeParam, aktørParam);
        assertThat(feed.getTittel()).isEqualTo("SVPVedtak_v1");
        assertThat(feed.getInneholderFlereElementer()).isTrue();
        assertThat(feed.getElementer()).isEqualTo(new ArrayList<>());
    }

    @Test
    void skal_delegere_til_hent_fp_vedtak_tjeneste_med_default_params() {
        var sisteLestSekvensIdParam = new SekvensIdParam("1");
        var maxAntallParam = new MaxAntallParam("100");
        var hendelseTypeParam = new HendelseTypeParam("");
        var aktørParam = new AktørParam("");

        Optional<AktørId> emptyAktørParam = Optional.empty();
        when(vedtakFattetTjeneste.hentFpVedtak(1L, 100L, null, emptyAktørParam)).thenReturn(new VedtakDto(true, new ArrayList<>()));

        var feed = tjeneste.fpVedtakHendelser(sisteLestSekvensIdParam, maxAntallParam, hendelseTypeParam, aktørParam);
        assertThat(feed.getTittel()).isEqualTo("ForeldrepengerVedtak_v1");
        assertThat(feed.getInneholderFlereElementer()).isTrue();
        assertThat(feed.getElementer()).isEqualTo(new ArrayList<>());
    }

    @Test
    void skal_delegere_til_hent_svp_vedtak_tjeneste_med_default_params() {
        var sisteLestSekvensIdParam = new SekvensIdParam("1");
        var maxAntallParam = new MaxAntallParam("100");
        var hendelseTypeParam = new HendelseTypeParam("");
        var aktørParam = new AktørParam("");

        Optional<AktørId> emptyAktørParam = Optional.empty();
        when(vedtakFattetTjeneste.hentSvpVedtak(1L, 100L, null, emptyAktørParam)).thenReturn(new VedtakDto(true, new ArrayList<>()));

        var feed = tjeneste.svpVedtakHendelser(sisteLestSekvensIdParam, maxAntallParam, hendelseTypeParam, aktørParam);
        assertThat(feed.getTittel()).isEqualTo("SVPVedtak_v1");
        assertThat(feed.getInneholderFlereElementer()).isTrue();
        assertThat(feed.getElementer()).isEqualTo(new ArrayList<>());
    }
}
