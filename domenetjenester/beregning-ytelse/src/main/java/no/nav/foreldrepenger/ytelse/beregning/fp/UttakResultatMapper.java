package no.nav.foreldrepenger.ytelse.beregning.fp;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import no.nav.foreldrepenger.behandlingskontroll.FagsakYtelseTypeRef;
import no.nav.foreldrepenger.domene.uttak.ForeldrepengerUttak;
import no.nav.foreldrepenger.domene.uttak.ForeldrepengerUttakTjeneste;
import no.nav.foreldrepenger.domene.uttak.input.UttakInput;
import no.nav.foreldrepenger.ytelse.beregning.UttakResultatRepoMapper;
import no.nav.foreldrepenger.ytelse.beregning.regelmodell.UttakResultat;

@FagsakYtelseTypeRef("FP")
@ApplicationScoped
public class UttakResultatMapper implements UttakResultatRepoMapper {

    private ForeldrepengerUttakTjeneste uttakTjeneste;
    private MapUttakResultatFraVLTilRegel mapper;

    UttakResultatMapper() {
        //for proxy
    }

    @Inject
    public UttakResultatMapper(ForeldrepengerUttakTjeneste uttakTjeneste, MapUttakResultatFraVLTilRegel mapUttakResultatFraVLTilRegelFP) {
        this.uttakTjeneste = uttakTjeneste;
        this.mapper = mapUttakResultatFraVLTilRegelFP;
    }

    @Override
    public UttakResultat hentOgMapUttakResultat(UttakInput input) {
        var ref = input.getBehandlingReferanse();
        ForeldrepengerUttak uttakResultat = uttakTjeneste.hentUttak(ref.getBehandlingId());
        return mapper.mapFra(uttakResultat, input);
    }
}
