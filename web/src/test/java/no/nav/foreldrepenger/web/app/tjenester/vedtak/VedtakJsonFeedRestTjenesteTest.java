package no.nav.foreldrepenger.web.app.tjenester.vedtak;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.ArrayList;
import java.util.Optional;

import org.junit.Before;
import org.junit.Test;
import no.nav.foreldrepenger.domene.typer.AktørId;
import no.nav.foreldrepenger.jsonfeed.VedtakFattetTjeneste;
import no.nav.foreldrepenger.jsonfeed.dto.VedtakDto;
import no.nav.foreldrepenger.kontrakter.feed.vedtak.v1.FeedDto;
import no.nav.foreldrepenger.kontrakter.feed.vedtak.v1.Meldingstype;
import no.nav.foreldrepenger.web.app.tjenester.vedtak.vedtakfattet.VedtakJsonFeedRestTjeneste;
import no.nav.foreldrepenger.web.app.tjenester.vedtak.vedtakfattet.dto.AktørParam;
import no.nav.foreldrepenger.web.app.tjenester.vedtak.vedtakfattet.dto.HendelseTypeParam;
import no.nav.foreldrepenger.web.app.tjenester.vedtak.vedtakfattet.dto.MaxAntallParam;
import no.nav.foreldrepenger.web.app.tjenester.vedtak.vedtakfattet.dto.SekvensIdParam;
import no.nav.vedtak.sikkerhet.ContextPathHolder;

public class VedtakJsonFeedRestTjenesteTest {

    private static final AktørId AKTØR_ID = AktørId.dummy();
    private VedtakJsonFeedRestTjeneste tjeneste;
    private VedtakFattetTjeneste vedtakFattetTjeneste;

    @Before
    public void setUp() {
        ContextPathHolder.instance("/fpsak");
        vedtakFattetTjeneste = mock(VedtakFattetTjeneste.class);
        tjeneste = new VedtakJsonFeedRestTjeneste(vedtakFattetTjeneste);
    }   

    @Test
    public void skal_delegere_til_hent_fp_vedtak_tjeneste() {
        SekvensIdParam sisteLestSekvensIdParam = new SekvensIdParam("1");
        MaxAntallParam maxAntallParam = new MaxAntallParam("100");
        HendelseTypeParam hendelseTypeParam = new HendelseTypeParam(Meldingstype.FORELDREPENGER_ENDRET.getType());
        AktørParam aktørParam = new AktørParam(AKTØR_ID.getId());
        when(vedtakFattetTjeneste.hentFpVedtak(1L, 100L, "ForeldrepengerEndret_v1", Optional.of(AKTØR_ID))).thenReturn(new VedtakDto(true, new ArrayList<>()));

        FeedDto feed = tjeneste.fpVedtakHendelser(sisteLestSekvensIdParam, maxAntallParam, hendelseTypeParam, aktørParam);
        assertThat(feed.getTittel()).isEqualTo("ForeldrepengerVedtak_v1");
        assertThat(feed.getInneholderFlereElementer()).isEqualTo(true);
        assertThat(feed.getElementer()).isEqualTo(new ArrayList<>());
    }

    @Test
    public void skal_delegere_til_hent_svp_vedtak_tjeneste() {
        SekvensIdParam sisteLestSekvensIdParam = new SekvensIdParam("1");
        MaxAntallParam maxAntallParam = new MaxAntallParam("100");
        HendelseTypeParam hendelseTypeParam = new HendelseTypeParam(Meldingstype.SVANGERSKAPSPENGER_ENDRET.getType());
        AktørParam aktørParam = new AktørParam(AKTØR_ID.getId());
        when(vedtakFattetTjeneste.hentSvpVedtak(1L, 100L, "SVPEndret_v1", Optional.of(AKTØR_ID))).thenReturn(new VedtakDto(true, new ArrayList<>()));

        FeedDto feed = tjeneste.svpVedtakHendelser(sisteLestSekvensIdParam, maxAntallParam, hendelseTypeParam, aktørParam);
        assertThat(feed.getTittel()).isEqualTo("SVPVedtak_v1");
        assertThat(feed.getInneholderFlereElementer()).isEqualTo(true);
        assertThat(feed.getElementer()).isEqualTo(new ArrayList<>());
    }
    
    @Test
    public void skal_delegere_til_hent_fp_vedtak_tjeneste_med_default_params() {
        SekvensIdParam sisteLestSekvensIdParam = new SekvensIdParam("1");
        MaxAntallParam maxAntallParam = new MaxAntallParam("100");
        HendelseTypeParam hendelseTypeParam = new HendelseTypeParam("");
        AktørParam aktørParam = new AktørParam("");
        
        Optional<AktørId> emptyAktørParam = Optional.empty();
        when(vedtakFattetTjeneste.hentFpVedtak(1L, 100L, null, emptyAktørParam)).thenReturn(new VedtakDto(true, new ArrayList<>()));

        FeedDto feed = tjeneste.fpVedtakHendelser(sisteLestSekvensIdParam, maxAntallParam, hendelseTypeParam, aktørParam);
        assertThat(feed.getTittel()).isEqualTo("ForeldrepengerVedtak_v1");
        assertThat(feed.getInneholderFlereElementer()).isEqualTo(true);
        assertThat(feed.getElementer()).isEqualTo(new ArrayList<>());
    }

    @Test
    public void skal_delegere_til_hent_svp_vedtak_tjeneste_med_default_params() {
        SekvensIdParam sisteLestSekvensIdParam = new SekvensIdParam("1");
        MaxAntallParam maxAntallParam = new MaxAntallParam("100");
        HendelseTypeParam hendelseTypeParam = new HendelseTypeParam("");
        AktørParam aktørParam = new AktørParam("");

        Optional<AktørId> emptyAktørParam = Optional.empty();
        when(vedtakFattetTjeneste.hentSvpVedtak(1L, 100L, null, emptyAktørParam)).thenReturn(new VedtakDto(true, new ArrayList<>()));

        FeedDto feed = tjeneste.svpVedtakHendelser(sisteLestSekvensIdParam, maxAntallParam, hendelseTypeParam, aktørParam);
        assertThat(feed.getTittel()).isEqualTo("SVPVedtak_v1");
        assertThat(feed.getInneholderFlereElementer()).isEqualTo(true);
        assertThat(feed.getElementer()).isEqualTo(new ArrayList<>());
    }
}
