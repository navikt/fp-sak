package no.nav.foreldrepenger.web.app.tjenester.behandling.anke;

import java.util.UUID;

import jakarta.validation.constraints.NotNull;

import no.nav.foreldrepenger.behandlingslager.behandling.anke.AnkeOmgjørÅrsak;
import no.nav.foreldrepenger.behandlingslager.behandling.anke.AnkeVurdering;
import no.nav.foreldrepenger.behandlingslager.behandling.anke.AnkeVurderingOmgjør;

public record AnkeVurderingResultatDto(AnkeVurdering ankeVurdering,
                                       @NotNull String begrunnelse,
                                       String fritekstTilBrev,
                                       AnkeOmgjørÅrsak ankeOmgjoerArsak,
                                       AnkeOmgjørÅrsak ankeOmgjørÅrsak,
                                       AnkeVurderingOmgjør ankeVurderingOmgjoer,
                                       AnkeVurderingOmgjør ankeVurderingOmgjør,
                                       @NotNull boolean erAnkerIkkePart,
                                       @NotNull boolean erFristIkkeOverholdt,
                                       @NotNull boolean erIkkeKonkret,
                                       @NotNull boolean erIkkeSignert,
                                       @NotNull boolean erSubsidiartRealitetsbehandles,
                                       boolean erMerknaderMottatt,
                                       String merknadKommentar,
                                       UUID påAnketKlageBehandlingUuid,
                                       AnkeVurdering trygderettVurdering,
                                       AnkeOmgjørÅrsak trygderettOmgjoerArsak,
                                       AnkeOmgjørÅrsak trygderettOmgjørÅrsak,
                                       AnkeVurderingOmgjør trygderettVurderingOmgjoer,
                                       AnkeVurderingOmgjør trygderettVurderingOmgjør
                                       ) {
}
