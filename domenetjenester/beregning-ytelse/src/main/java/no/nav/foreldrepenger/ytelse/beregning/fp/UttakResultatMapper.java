package no.nav.foreldrepenger.ytelse.beregning.fp;

import java.util.List;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import no.nav.foreldrepenger.behandlingskontroll.FagsakYtelseTypeRef;
import no.nav.foreldrepenger.behandlingslager.behandling.BehandlingÅrsakType;
import no.nav.foreldrepenger.behandlingslager.fagsak.FagsakYtelseType;
import no.nav.foreldrepenger.domene.uttak.ForeldrepengerUttak;
import no.nav.foreldrepenger.domene.uttak.ForeldrepengerUttakTjeneste;
import no.nav.foreldrepenger.domene.uttak.input.UttakInput;
import no.nav.foreldrepenger.ytelse.beregning.UttakResultatRepoMapper;
import no.nav.foreldrepenger.ytelse.beregning.regelmodell.uttakresultat.UttakResultat;

@FagsakYtelseTypeRef(FagsakYtelseType.FORELDREPENGER)
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
        var uttakResultat = uttakTjeneste.hentHvisEksisterer(ref.behandlingId());
        if (uttakResultat.isEmpty() && !input.harBehandlingÅrsak(BehandlingÅrsakType.RE_UTSATT_START)) {
            throw new IllegalStateException("Utviklerfeil mangler uttakresultat");
        }
        return mapper.mapFra(uttakResultat.map(ForeldrepengerUttak::getGjeldendePerioder).orElse(List.of()), input);
    }
}
