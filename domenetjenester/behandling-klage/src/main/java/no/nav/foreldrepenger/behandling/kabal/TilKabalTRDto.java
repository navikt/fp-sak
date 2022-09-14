package no.nav.foreldrepenger.behandling.kabal;

import java.time.LocalDateTime;
import java.util.List;

import javax.validation.constraints.NotNull;

import no.nav.foreldrepenger.behandlingslager.behandling.Behandling;
import no.nav.foreldrepenger.behandlingslager.kodeverk.Fagsystem;

/**
 * Tilpasset versjon av V1 https://kabal-api.dev.intern.nav.no/swagger-ui/index.html#/kabal-api-external/sendInnAnkeITrygderettenV1
 *
 * Har utelatt dvhReferanse (=kildereferanse=behandlinguuid), frist, innsynUrl, sakMottattKaDato, sakenGjelder
 */
public record TilKabalTRDto(@NotNull TilKabalDto.Klager klager,
                            @NotNull List<TilKabalDto.DokumentReferanse> tilknyttedeJournalposter,
                            @NotNull String kildeReferanse, // Typisk BehandlingUUID
                            @NotNull String dvhReferanse,
                            @NotNull TilKabalDto.KabalYtelse ytelse,
                            @NotNull LocalDateTime sakMottattKaTidspunkt,// Tidspunkt KA skal ha fått vite om behandlingen
                            @NotNull LocalDateTime sendtTilTrygderetten,
                            @NotNull KabalUtfall utfall,
                            TilKabalDto.Sak fagsak,
                            List<String> hjemler) {

    public static TilKabalTRDto anke(Behandling behandling,
                                     String kildereferanse,
                                     @NotNull TilKabalDto.Klager klager,
                                     @NotNull List<TilKabalDto.DokumentReferanse> tilknyttedeJournalposter,
                                     @NotNull LocalDateTime sakMottattKaTidspunkt, // Tidspunkt KA skal ha fått vite om behandlingen
                                     @NotNull LocalDateTime sendtTilTrygderetten,
                                     @NotNull KabalUtfall utfall,
                                     List<String> hjemler ) {
        return new TilKabalTRDto(klager, tilknyttedeJournalposter,
            kildereferanse, kildereferanse,
            mapYtelseType(behandling), sakMottattKaTidspunkt,
            sendtTilTrygderetten, utfall,
            new TilKabalDto.Sak(behandling.getFagsak().getSaksnummer().getVerdi(), Fagsystem.FPSAK.getOffisiellKode()),
            hjemler);
    }

    private static TilKabalDto.KabalYtelse mapYtelseType(Behandling behandling) {
        return switch (behandling.getFagsakYtelseType()) {
            case ENGANGSTØNAD -> TilKabalDto.KabalYtelse.FOR_ENG;
            case FORELDREPENGER -> TilKabalDto.KabalYtelse.FOR_FOR;
            case SVANGERSKAPSPENGER -> TilKabalDto.KabalYtelse.FOR_SVA;
            default -> throw new IllegalArgumentException("Mangler ytelsetype");
        };
    }

}
