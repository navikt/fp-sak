package no.nav.foreldrepenger.jsonfeed;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import no.nav.foreldrepenger.domene.feed.FeedRepository;
import no.nav.foreldrepenger.domene.feed.FpVedtakUtgåendeHendelse;
import no.nav.foreldrepenger.domene.feed.HendelseCriteria;
import no.nav.foreldrepenger.domene.feed.SvpVedtakUtgåendeHendelse;
import no.nav.foreldrepenger.domene.person.PersoninfoAdapter;
import no.nav.foreldrepenger.domene.typer.AktørId;
import no.nav.foreldrepenger.kontrakter.feed.vedtak.v1.ForeldrepengerInnvilget;
import no.nav.foreldrepenger.kontrakter.feed.vedtak.v1.Meldingstype;

@ExtendWith(MockitoExtension.class)
class VedtakFattetTjenesteTest {

    private static final String PAYLOAD = "{}";
    private static final AktørId AKTØR_ID = AktørId.dummy();
    private static final String HENDELSE_TYPE = Meldingstype.FORELDREPENGER_INNVILGET.getType();
    private static final long MAX_ANTALL = 2L;
    private static final long SIST_LEST_SEKVENSID = 1L;
    private VedtakFattetTjeneste tjeneste;
    @Mock
    private FeedRepository feedRepository;
    @Mock
    private PersoninfoAdapter personinfoAdapter;

    @BeforeEach
    void setUp() {
        tjeneste = new VedtakFattetTjeneste(feedRepository, personinfoAdapter);
    }

    @Test
    void skal_delegere_til_repository_metode_for_søk_fp_hendelser() {
        var captor = ArgumentCaptor.forClass(HendelseCriteria.class);
        var hendelse = mockFpHendelse(SIST_LEST_SEKVENSID + 1);

        when(feedRepository.hentUtgåendeHendelser(eq(FpVedtakUtgåendeHendelse.class), any(HendelseCriteria.class))).thenReturn(List.of(hendelse));

        var dto = tjeneste.hentFpVedtak(SIST_LEST_SEKVENSID, MAX_ANTALL, HENDELSE_TYPE, Optional.of(AKTØR_ID));

        verify(feedRepository).hentUtgåendeHendelser(eq(FpVedtakUtgåendeHendelse.class), captor.capture());
        var criteria = captor.getValue();
        assertThat(criteria.getAktørId()).isEqualTo(AKTØR_ID.getId());
        assertThat(criteria.getMaxAntall()).isEqualTo(MAX_ANTALL + 1);
        assertThat(criteria.getSisteLestSekvensId()).isEqualTo(SIST_LEST_SEKVENSID);
        assertThat(criteria.getType()).isEqualTo(HENDELSE_TYPE);
        assertThat(dto.isHarFlereElementer()).isFalse();
        assertThat(dto.getElementer()).hasSize(1);
        assertThat(dto.getElementer().get(0).getType()).isEqualTo(HENDELSE_TYPE);
        assertThat(dto.getElementer().get(0).getInnhold()).isInstanceOf(ForeldrepengerInnvilget.class);
        assertThat(dto.getElementer().get(0).getSekvensId()).isEqualTo(SIST_LEST_SEKVENSID + 1);
    }

    @Test
    void skal_delegere_til_repository_metode_for_søk_svp_hendelser() {
        var captor = ArgumentCaptor.forClass(HendelseCriteria.class);
        var hendelse = mockSvpHendelse(SIST_LEST_SEKVENSID + 1);

        when(feedRepository.hentUtgåendeHendelser(eq(SvpVedtakUtgåendeHendelse.class), any(HendelseCriteria.class))).thenReturn(List.of(hendelse));

        var dto = tjeneste.hentSvpVedtak(SIST_LEST_SEKVENSID, MAX_ANTALL, HENDELSE_TYPE, Optional.of(AKTØR_ID));

        verify(feedRepository).hentUtgåendeHendelser(eq(SvpVedtakUtgåendeHendelse.class), captor.capture());
        var criteria = captor.getValue();
        assertThat(criteria.getAktørId()).isEqualTo(AKTØR_ID.getId());
        assertThat(criteria.getMaxAntall()).isEqualTo(MAX_ANTALL + 1);
        assertThat(criteria.getSisteLestSekvensId()).isEqualTo(SIST_LEST_SEKVENSID);
        assertThat(criteria.getType()).isEqualTo(HENDELSE_TYPE);
        assertThat(dto.isHarFlereElementer()).isFalse();
        assertThat(dto.getElementer()).hasSize(1);
        assertThat(dto.getElementer().get(0).getType()).isEqualTo(HENDELSE_TYPE);
        assertThat(dto.getElementer().get(0).getInnhold()).isInstanceOf(ForeldrepengerInnvilget.class);
        assertThat(dto.getElementer().get(0).getSekvensId()).isEqualTo(SIST_LEST_SEKVENSID + 1);
    }

    @Test
    void hent_hendelser_skal_returnere_at_det_er_flere_hendelser_å_lese() {
        var hendelse = mockFpHendelse(SIST_LEST_SEKVENSID + 1);
        var hendelse2 = mockFpHendelse(SIST_LEST_SEKVENSID + 2);
        when(feedRepository.hentUtgåendeHendelser(eq(FpVedtakUtgåendeHendelse.class), any(HendelseCriteria.class)))
                .thenReturn(List.of(hendelse, hendelse2));

        var dto = tjeneste.hentFpVedtak(SIST_LEST_SEKVENSID, 1L, HENDELSE_TYPE, Optional.of(AKTØR_ID));

        assertThat(dto.isHarFlereElementer()).isTrue();
    }

    private FpVedtakUtgåendeHendelse mockFpHendelse(Long sekvensenummer) {
        var hendelse = mock(FpVedtakUtgåendeHendelse.class);
        when(hendelse.getType()).thenReturn(HENDELSE_TYPE);
        when(hendelse.getSekvensnummer()).thenReturn(sekvensenummer);
        when(hendelse.getPayload()).thenReturn(PAYLOAD);
        when(hendelse.getOpprettetTidspunkt()).thenReturn(LocalDateTime.now());
        return hendelse;
    }

    private SvpVedtakUtgåendeHendelse mockSvpHendelse(Long sekvensenummer) {
        var hendelse = mock(SvpVedtakUtgåendeHendelse.class);
        when(hendelse.getType()).thenReturn(HENDELSE_TYPE);
        when(hendelse.getSekvensnummer()).thenReturn(sekvensenummer);
        when(hendelse.getPayload()).thenReturn(PAYLOAD);
        when(hendelse.getOpprettetTidspunkt()).thenReturn(LocalDateTime.now());
        return hendelse;
    }
}
