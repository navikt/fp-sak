package no.nav.foreldrepenger.web.app.tjenester.historikk.dto;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.stream.Stream;

import org.junit.jupiter.api.Test;

import no.nav.foreldrepenger.behandlingslager.behandling.historikk.HistorikkinnslagType;
import no.nav.foreldrepenger.historikk.dto.HistorikkinnslagDto;

class HistorikkinnslagDtoTest {
    private static final LocalDateTime NOW = LocalDateTime.now();

    @Test
    void skal_sortere_basert_på_tidligste_opprettetDato_first() {
        var historikkInnslagDtos = Stream.of(
                lagHistorikkInnslagDtos(NOW.plusMonths(2)),
                lagHistorikkInnslagDtos(NOW.minusWeeks(8)),
                lagHistorikkInnslagDtos(NOW.plusDays(3)),
                lagHistorikkInnslagDtos(NOW),
                lagHistorikkInnslagDtos(NOW.plusSeconds(4)),
                lagHistorikkInnslagDtos(NOW.plusYears(1)),
                lagHistorikkInnslagDtos(NOW.minusMinutes(6)),
                lagHistorikkInnslagDtos(NOW.minusHours(7)),
                lagHistorikkInnslagDtos(NOW.minusYears(9)))
            .sorted(Comparator.naturalOrder())
            .toList();

        assertThat(historikkInnslagDtos.get(0).getOpprettetTidspunkt()).isEqualTo(NOW.plusYears(1));
        assertThat(historikkInnslagDtos.get(1).getOpprettetTidspunkt()).isEqualTo(NOW.plusMonths(2));
        assertThat(historikkInnslagDtos.get(2).getOpprettetTidspunkt()).isEqualTo(NOW.plusDays(3));
        assertThat(historikkInnslagDtos.get(3).getOpprettetTidspunkt()).isEqualTo(NOW.plusSeconds(4));
        assertThat(historikkInnslagDtos.get(4).getOpprettetTidspunkt()).isEqualTo(NOW);
        assertThat(historikkInnslagDtos.get(5).getOpprettetTidspunkt()).isEqualTo(NOW.minusMinutes(6));
        assertThat(historikkInnslagDtos.get(6).getOpprettetTidspunkt()).isEqualTo(NOW.minusHours(7));
        assertThat(historikkInnslagDtos.get(7).getOpprettetTidspunkt()).isEqualTo(NOW.minusWeeks(8));
        assertThat(historikkInnslagDtos.get(8).getOpprettetTidspunkt()).isEqualTo(NOW.minusYears(9));
    }

    @Test
    void skal_revurdOpprettet_kommer_før_brev_sent_og_brev_vent_hvis_samme_tids_punkt() {
        var historikkInnslagDtos = Stream.of(
                lagHistorikkInnslagDtos(NOW, HistorikkinnslagType.REVURD_OPPR),
                lagHistorikkInnslagDtos(NOW, HistorikkinnslagType.BEH_VENT),
                lagHistorikkInnslagDtos(NOW, HistorikkinnslagType.BREV_SENT),
                lagHistorikkInnslagDtos(NOW.minusWeeks(1), HistorikkinnslagType.BEH_STARTET))
            .sorted(Comparator.naturalOrder())
            .toList();

        assertThat(historikkInnslagDtos.get(2).getType()).isEqualTo(HistorikkinnslagType.REVURD_OPPR);
    }

    private HistorikkinnslagDto lagHistorikkInnslagDtos(LocalDateTime periodeFraDato, HistorikkinnslagType innslagType) {
        var historikkInnslagDto = new HistorikkinnslagDto();
        historikkInnslagDto.setOpprettetTidspunkt(periodeFraDato);
        historikkInnslagDto.setType(innslagType);
        return historikkInnslagDto;
    }

    private HistorikkinnslagDto lagHistorikkInnslagDtos(LocalDateTime periodeFraDato) {
        var historikkInnslagDto = new HistorikkinnslagDto();
        historikkInnslagDto.setOpprettetTidspunkt(periodeFraDato);
        return historikkInnslagDto;
    }
}
