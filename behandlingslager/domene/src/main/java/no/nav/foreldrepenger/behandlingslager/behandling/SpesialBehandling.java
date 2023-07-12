package no.nav.foreldrepenger.behandlingslager.behandling;

import java.util.Set;

/*
 * Metoder knyttet til tekniske behandlinger pga vedtak i relatert fagsak eller senerelagt uttak
 * Disse skal typisk ikke gjennom vilkår eller beregning - men oppdatere uttak eller tilkjent ytelse.
 */
public final class SpesialBehandling {

    private static final Set<BehandlingÅrsakType> BEHOLD_GRUNNLAG = BehandlingÅrsakType.alleTekniskeÅrsaker();

    private static final Set<BehandlingÅrsakType> BEHOLD_UTTAK = Set.of(BehandlingÅrsakType.REBEREGN_FERIEPENGER, BehandlingÅrsakType.RE_UTSATT_START);

    private SpesialBehandling() {
    }

    public static boolean erSpesialBehandling(Behandling behandling) {
        return behandling.harNoenBehandlingÅrsaker(BEHOLD_GRUNNLAG);
    }

    public static boolean erIkkeSpesialBehandling(Behandling behandling) {
        return !behandling.harNoenBehandlingÅrsaker(BEHOLD_GRUNNLAG);
    }

    public static boolean erBerørtBehandling(Behandling behandling) {
        return behandling.harBehandlingÅrsak(BehandlingÅrsakType.BERØRT_BEHANDLING);
    }

    public static boolean erJusterFeriepenger(Behandling behandling) {
        return behandling.harBehandlingÅrsak(BehandlingÅrsakType.REBEREGN_FERIEPENGER);
    }

    public static boolean erOppsagtUttak(Behandling behandling) {
        return behandling.harBehandlingÅrsak(BehandlingÅrsakType.RE_UTSATT_START);
    }

    public static boolean skalGrunnlagBeholdes(Behandling behandling) {
        return behandling.harNoenBehandlingÅrsaker(BEHOLD_GRUNNLAG);
    }

    public static boolean skalUttakVurderes(Behandling behandling) {
        return !behandling.harNoenBehandlingÅrsaker(BEHOLD_UTTAK);
    }

    public static boolean skalKøes(Behandling behandling) {
        return !behandling.harNoenBehandlingÅrsaker(BEHOLD_GRUNNLAG);
    }

    public static boolean skalIkkeKøes(Behandling behandling) {
        return behandling.harNoenBehandlingÅrsaker(BEHOLD_GRUNNLAG);
    }

    public static boolean kanIkkeOverstyres(Behandling behandling) {
        return behandling.harNoenBehandlingÅrsaker(BEHOLD_UTTAK);
    }

    public static boolean kanHenlegges(Behandling behandling) {
        return !behandling.harNoenBehandlingÅrsaker(BEHOLD_UTTAK);
    }

}
