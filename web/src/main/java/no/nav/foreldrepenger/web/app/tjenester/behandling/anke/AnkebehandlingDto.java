package no.nav.foreldrepenger.web.app.tjenester.behandling.anke;

public record AnkebehandlingDto(AnkeVurderingResultatDto ankeVurderingResultat,
                                boolean underBehandlingKabal,
                                boolean underBehandlingKabalTrygderett,
                                boolean behandletAvKabal) {

}
