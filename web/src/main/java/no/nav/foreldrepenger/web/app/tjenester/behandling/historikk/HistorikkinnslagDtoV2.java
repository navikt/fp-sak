package no.nav.foreldrepenger.web.app.tjenester.behandling.historikk;

import java.time.LocalDateTime;
import java.util.UUID;

import no.nav.foreldrepenger.behandlingslager.behandling.historikk.HistorikkAktør;
import no.nav.foreldrepenger.behandlingslager.behandling.skjermlenke.SkjermlenkeType;
import no.nav.foreldrepenger.historikk.dto.HistorikkInnslagDokumentLinkDto;

public record HistorikkinnslagDtoV2(UUID behandlingUuid,
                                    HistorikkAktør historikkAktør,
                                    SkjermlenkeType skjermlenke,
                                    LocalDateTime opprettetTidspunkt,
                                    HistorikkInnslagDokumentLinkDto dokumenter,
                                    String saksbehandler,
                                    String tittel,
                                    String body) {
}
