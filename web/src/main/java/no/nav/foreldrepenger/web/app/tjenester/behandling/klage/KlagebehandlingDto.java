package no.nav.foreldrepenger.web.app.tjenester.behandling.klage;

import java.time.LocalDate;
import java.util.List;

import no.nav.foreldrepenger.behandlingslager.behandling.klage.KlageHjemmel;

public record KlagebehandlingDto(KlageFormkravResultatDto klageFormkravResultatNFP,
                                 KlageVurderingResultatDto klageVurderingResultatNFP,
                                 KlageFormkravResultatDto klageFormkravResultatKA,
                                 KlageVurderingResultatDto klageVurderingResultatNK,
                                 List<KlageHjemmel> aktuelleHjemler,
                                 boolean underBehandlingKabal,
                                 boolean behandletAvKabal,
                                 LocalDate mottattDato) {
}
