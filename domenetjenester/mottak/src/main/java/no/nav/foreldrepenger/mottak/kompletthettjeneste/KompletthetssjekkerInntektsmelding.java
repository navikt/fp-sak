package no.nav.foreldrepenger.mottak.kompletthettjeneste;

import java.util.List;

import no.nav.foreldrepenger.behandling.BehandlingReferanse;
import no.nav.foreldrepenger.kompletthet.ManglendeVedlegg;

public interface KompletthetssjekkerInntektsmelding {

    /**
     * Utleder manglende inntektsmeldinger
     *
     * @return Manglende påkrevde inntektsmeldinger som ennå ikke er mottatt
     */
    List<ManglendeVedlegg> utledManglendeInntektsmeldinger(BehandlingReferanse ref);

    /**
     * Henter alle påkrevde inntektsmeldinger fra grunnlaget, og filterer ut alle
     * motatte.
     *
     * @return Manglende påkrevde inntektsmeldinger som ennå ikke er motatt
     */
    List<ManglendeVedlegg> utledManglendeInntektsmeldingerFraGrunnlag(BehandlingReferanse ref);

}
