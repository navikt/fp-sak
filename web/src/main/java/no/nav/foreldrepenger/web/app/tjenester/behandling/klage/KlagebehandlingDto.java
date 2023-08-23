package no.nav.foreldrepenger.web.app.tjenester.behandling.klage;

import no.nav.foreldrepenger.behandlingslager.behandling.klage.KlageHjemmel;

import java.util.List;

public record KlagebehandlingDto(KlageFormkravResultatDto klageFormkravResultatNFP,
                                 KlageVurderingResultatDto klageVurderingResultatNFP,
                                 KlageFormkravResultatDto klageFormkravResultatKA,
                                 KlageVurderingResultatDto klageVurderingResultatNK,
                                 List<KlageHjemmel> aktuelleHjemler,
                                 boolean underBehandlingKabal,
                                 boolean behandletAvKabal) {
}
