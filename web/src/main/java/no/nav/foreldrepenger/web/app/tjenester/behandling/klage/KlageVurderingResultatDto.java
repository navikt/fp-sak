package no.nav.foreldrepenger.web.app.tjenester.behandling.klage;

import jakarta.validation.constraints.NotNull;

import no.nav.foreldrepenger.behandlingslager.behandling.klage.KlageHjemmel;
import no.nav.foreldrepenger.behandlingslager.behandling.klage.KlageMedholdÅrsak;
import no.nav.foreldrepenger.behandlingslager.behandling.klage.KlageVurdering;
import no.nav.foreldrepenger.behandlingslager.behandling.klage.KlageVurderingOmgjør;

public record KlageVurderingResultatDto(@NotNull String klageVurdertAv,
                                        KlageVurdering klageVurdering,
                                        String begrunnelse,
                                        KlageMedholdÅrsak klageMedholdArsak,
                                        KlageMedholdÅrsak klageMedholdÅrsak,
                                        KlageVurderingOmgjør klageVurderingOmgjoer,
                                        KlageVurderingOmgjør klageVurderingOmgjør,
                                        KlageHjemmel klageHjemmel,
                                        String fritekstTilBrev) {
}
