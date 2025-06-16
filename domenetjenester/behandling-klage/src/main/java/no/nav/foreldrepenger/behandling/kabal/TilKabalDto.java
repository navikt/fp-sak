package no.nav.foreldrepenger.behandling.kabal;

import java.time.LocalDate;
import java.util.List;

import jakarta.validation.constraints.NotNull;

import no.nav.foreldrepenger.behandlingslager.behandling.Behandling;
import no.nav.foreldrepenger.behandlingslager.kodeverk.Fagsystem;

/**
 * Tilpasset versjon av V4 https://kabal-api.intern.dev.nav.no/swagger-ui/index.html?urls.primaryName=external#/kabal-api-external/sendInnSakV4
 *
 * Har utelatt klager, frist, sakMottattKaTidspunkt, hindreAutomatiskSvarbrev, saksbehandlerIdentForTildeling
 */
public record TilKabalDto(@NotNull KlageAnke type,
                          @NotNull Klager sakenGjelder,
                          Fullmektig prosessfullmektig,
                          @NotNull Sak fagsak,
                          @NotNull String kildeReferanse,
                          String dvhReferanse,
                          @NotNull List<String> hjemler,
                          @NotNull String forrigeBehandlendeEnhet, // Førsteinstans
                          @NotNull List<DokumentReferanse> tilknyttedeJournalposter,
                          LocalDate brukersKlageMottattVedtaksinstans, // Required Mottattdato?
                          @NotNull KabalYtelse ytelse,
                          String kommentar) {

    public static TilKabalDto klage(Behandling behandling,
                                    @NotNull Klager sakenGjelder,
                                    Fullmektig prosessfullmektig,
                                    @NotNull String forrigeBehandlendeEnhet, // Førsteinstans
                                    @NotNull List<DokumentReferanse> tilknyttedeJournalposter,
                                    @NotNull LocalDate brukersKlageMottattVedtaksinstans, // Required Mottattdato?
                                    List<String> hjemler,
                                    String kommentar) {
        return new TilKabalDto(KlageAnke.KLAGE,
            sakenGjelder, prosessfullmektig,
            new Sak(behandling.getSaksnummer().getVerdi(), Fagsystem.FPSAK.getOffisiellKode()),
            behandling.getUuid().toString(), behandling.getUuid().toString(),
            hjemler, forrigeBehandlendeEnhet, tilknyttedeJournalposter,
            brukersKlageMottattVedtaksinstans, mapYtelseType(behandling), kommentar);
    }

    public record Sak(@NotNull String fagsakId, @NotNull String fagsystem) {}

    public record PartId(@NotNull PartsType type, @NotNull String verdi) {}

    public record Fullmektig(@NotNull PartId id, String navn) {}

    public record Klager(@NotNull PartId id) {}

    public record DokumentReferanse(@NotNull String journalpostId, @NotNull DokumentReferanseType type) {}

    public enum DokumentReferanseType {
        ANNET, BRUKERS_KLAGE, BRUKERS_SOEKNAD, KLAGE_VEDTAK, OPPRINNELIG_VEDTAK, OVERSENDELSESBREV
    }

    public enum PartsType {
        PERSON, VIRKSOMHET
    }

    public enum KlageAnke {
        ANKE, KLAGE
    }

    public enum KabalYtelse {
        FOR_ENG, FOR_FOR, FOR_SVA
    }

    private static KabalYtelse mapYtelseType(Behandling behandling) {
        return switch (behandling.getFagsakYtelseType()) {
            case ENGANGSTØNAD -> TilKabalDto.KabalYtelse.FOR_ENG;
            case FORELDREPENGER -> TilKabalDto.KabalYtelse.FOR_FOR;
            case SVANGERSKAPSPENGER -> TilKabalDto.KabalYtelse.FOR_SVA;
            default -> throw new IllegalArgumentException("Mangler ytelsetype");
        };
    }

}
