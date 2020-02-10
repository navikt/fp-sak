package no.nav.foreldrepenger.ytelse.beregning.fp;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import no.nav.foreldrepenger.behandlingskontroll.FagsakYtelseTypeRef;
import no.nav.foreldrepenger.behandlingslager.behandling.repository.BehandlingRepositoryProvider;
import no.nav.foreldrepenger.behandlingslager.uttak.UttakRepository;
import no.nav.foreldrepenger.behandlingslager.uttak.UttakResultatEntitet;
import no.nav.foreldrepenger.domene.uttak.input.UttakInput;
import no.nav.foreldrepenger.ytelse.beregning.UttakResultatRepoMapper;
import no.nav.foreldrepenger.ytelse.beregning.regelmodell.UttakResultat;

@FagsakYtelseTypeRef("FP")
@ApplicationScoped
public class UttakResultatMapper implements UttakResultatRepoMapper {

    private UttakRepository uttakRepository;
    private MapUttakResultatFraVLTilRegel mapper;

    UttakResultatMapper() {
        //for proxy
    }

    @Inject
    public UttakResultatMapper(BehandlingRepositoryProvider repositoryProvider, MapUttakResultatFraVLTilRegel mapUttakResultatFraVLTilRegelFP) {
        this.uttakRepository = repositoryProvider.getUttakRepository();
        this.mapper = mapUttakResultatFraVLTilRegelFP;
    }

    @Override
    public UttakResultat hentOgMapUttakResultat(UttakInput input) {
        var ref = input.getBehandlingReferanse();
        UttakResultatEntitet uttakResultat = uttakRepository.hentUttakResultat(ref.getBehandlingId());
        return mapper.mapFra(uttakResultat, input);
    }
}
