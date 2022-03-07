package no.nav.foreldrepenger.mottak.kabal;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

public record KabalHendelse(UUID eventId,
                            String kildeReferanse,
                            String kilde,
                            String kabalReferanse,
                            BehandlingEventType type,
                            BehandlingDetaljer detaljer) {

    public enum BehandlingEventType {
        KLAGEBEHANDLING_AVSLUTTET, ANKEBEHANDLING_OPPRETTET, ANKEBEHANDLING_AVSLUTTET
    }

    public record BehandlingDetaljer(KlagebehandlingAvsluttetDetaljer klagebehandlingAvsluttet,
                                     AnkebehandlingOpprettetDetaljer ankebehandlingOpprettet,
                                     AnkebehandlingAvsluttetDetaljer ankebehandlingAvsluttet) {}

    public record KlagebehandlingAvsluttetDetaljer(LocalDateTime avsluttet, KabalUtfall utfall, List<String> journalpostReferanser) {}


    public record AnkebehandlingOpprettetDetaljer(LocalDateTime mottattKlageinstans) {}

    public record AnkebehandlingAvsluttetDetaljer(LocalDateTime avsluttet, KabalUtfall utfall, List<String> journalpostReferanser) {}
}


