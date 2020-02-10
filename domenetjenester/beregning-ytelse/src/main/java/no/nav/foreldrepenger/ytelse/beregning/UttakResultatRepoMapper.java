package no.nav.foreldrepenger.ytelse.beregning;

import no.nav.foreldrepenger.domene.uttak.input.UttakInput;
import no.nav.foreldrepenger.ytelse.beregning.regelmodell.UttakResultat;

@FunctionalInterface
public interface UttakResultatRepoMapper {

    /**
     * Henter uttaksresultat fra repository og mapper til en felles regelmodell for
     * uttaksresultat ved hjelp av en mapper. Repository og mapper brukt avhenger
     * av fagsak-typen (foreldrepenger eller svangerskapspenger).
     *
     * @param input UttakInput
     * @return Regelmodell for uttakResultat
     */

    UttakResultat hentOgMapUttakResultat(UttakInput input);
}
