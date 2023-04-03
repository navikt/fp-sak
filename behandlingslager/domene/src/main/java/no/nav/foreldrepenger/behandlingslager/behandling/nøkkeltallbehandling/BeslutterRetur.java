package no.nav.foreldrepenger.behandlingslager.behandling.nøkkeltallbehandling;

import no.nav.foreldrepenger.behandlingslager.behandling.aksjonspunkt.VurderÅrsak;

import java.time.LocalDate;

public record BeslutterRetur(LocalDate dato, Long totaltAntallTotrinnsretur, VurderÅrsak vurderÅrsakType, Long antall) {
}
